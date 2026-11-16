#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10,<3.15"
# dependencies = [
#     "fastembed>=0.5.0",
#     "numpy>=1.26.0",
# ]
# ///
"""
rag_indexer.py — Semantic markdown chunker and local vector indexer for SplitTrip documentation.

Parses /docs/**/*.md and AGENTS.md, extracts hierarchical breadcrumbs, preserves code fences,
generates dense embeddings using BAAI/bge-small-en-v1.5 locally, and indexes chunks into SQLite
(dense vector storage + FTS5 full-text search).
"""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import sqlite3
import sys
import time
from dataclasses import dataclass
import numpy as np


@dataclass
class MarkdownChunk:
    chunk_id: str
    file_path: str
    section_title: str
    breadcrumbs: str
    header_level: int
    content: str
    enriched_text: str


class MarkdownChunker:
    """Header-aware semantic markdown chunker that preserves code fences and heading hierarchy."""

    HEADER_RE = re.compile(r"^(#{1,6})\s+(.+)$")
    CODE_FENCE_RE = re.compile(r"^\s*(```|~~~)")

    def __init__(self, max_words: int = 400):
        self.max_words = max_words

    def chunk_file(self, file_path: Path, repo_root: Path | None = None) -> list[MarkdownChunk]:
        """Chunk a markdown file into semantic sections with breadcrumb metadata."""
        if not file_path.is_file():
            return []

        rel_path = str(file_path.relative_to(repo_root)) if repo_root else file_path.name
        text = file_path.read_text(encoding="utf-8")
        return self.chunk_text(text, rel_path)

    def chunk_text(self, text: str, rel_path: str) -> list[MarkdownChunk]:
        """Chunk markdown text into semantic sections with breadcrumb metadata."""
        lines = text.splitlines()
        chunks: list[MarkdownChunk] = []

        heading_stack: list[tuple[int, str]] = []
        current_section_title = Path(rel_path).stem.replace("-", " ").title()
        current_header_level = 0
        current_buffer: list[str] = []
        in_code_block = False

        def flush_section() -> None:
            nonlocal current_buffer
            section_content = "\n".join(current_buffer).strip()
            if not section_content:
                current_buffer = []
                return

            breadcrumbs = " > ".join(title for _, title in heading_stack) if heading_stack else current_section_title
            section_chunks = self._split_section_content(
                rel_path=rel_path,
                section_title=current_section_title,
                breadcrumbs=breadcrumbs,
                header_level=current_header_level,
                content=section_content,
                base_chunk_index=len(chunks),
            )
            chunks.extend(section_chunks)
            current_buffer = []

        for line in lines:
            if self.CODE_FENCE_RE.match(line):
                in_code_block = not in_code_block
                current_buffer.append(line)
                continue

            if not in_code_block:
                header_match = self.HEADER_RE.match(line)
                if header_match:
                    flush_section()
                    level = len(header_match.group(1))
                    title = header_match.group(2).strip()

                    while heading_stack and heading_stack[-1][0] >= level:
                        heading_stack.pop()
                    heading_stack.append((level, title))

                    current_section_title = title
                    current_header_level = level
                    continue

            current_buffer.append(line)

        flush_section()
        return chunks

    def _split_section_content(
        self,
        rel_path: str,
        section_title: str,
        breadcrumbs: str,
        header_level: int,
        content: str,
        base_chunk_index: int,
    ) -> list[MarkdownChunk]:
        words = content.split()
        if len(words) <= self.max_words:
            chunk_id = f"{rel_path}#{base_chunk_index}"
            enriched_text = f"{breadcrumbs}\n\n{content}"
            return [
                MarkdownChunk(
                    chunk_id=chunk_id,
                    file_path=rel_path,
                    section_title=section_title,
                    breadcrumbs=breadcrumbs,
                    header_level=header_level,
                    content=content,
                    enriched_text=enriched_text,
                )
            ]

        paragraphs = self._split_paragraphs_preserving_code(content)
        result: list[MarkdownChunk] = []
        current_paragraphs: list[str] = []
        current_word_count = 0
        part = 1

        def emit_chunk() -> None:
            nonlocal current_paragraphs, current_word_count, part
            if not current_paragraphs:
                return
            chunk_content = "\n\n".join(current_paragraphs).strip()
            part_suffix = f" (Part {part})" if part > 1 or len(paragraphs) > 1 else ""
            eff_section = f"{section_title}{part_suffix}"
            eff_breadcrumbs = f"{breadcrumbs}{part_suffix}"
            chunk_id = f"{rel_path}#{base_chunk_index + len(result)}"
            enriched_text = f"{eff_breadcrumbs}\n\n{chunk_content}"

            result.append(
                MarkdownChunk(
                    chunk_id=chunk_id,
                    file_path=rel_path,
                    section_title=eff_section,
                    breadcrumbs=eff_breadcrumbs,
                    header_level=header_level,
                    content=chunk_content,
                    enriched_text=enriched_text,
                )
            )
            part += 1
            current_paragraphs = []
            current_word_count = 0

        for p in paragraphs:
            p_words = len(p.split())
            if current_paragraphs and (current_word_count + p_words > self.max_words):
                emit_chunk()
            current_paragraphs.append(p)
            current_word_count += p_words

        emit_chunk()
        return result

    def _split_paragraphs_preserving_code(self, text: str) -> list[str]:
        lines = text.splitlines()
        paragraphs: list[str] = []
        current: list[str] = []
        in_code = False

        for line in lines:
            if self.CODE_FENCE_RE.match(line):
                in_code = not in_code
                current.append(line)
                continue

            if not in_code and not line.strip():
                if current:
                    paragraphs.append("\n".join(current).strip())
                    current = []
                continue

            current.append(line)

        if current:
            paragraphs.append("\n".join(current).strip())

        return [p for p in paragraphs if p]


class RagDatabase:
    """SQLite vector and full-text search database."""

    def __init__(self, db_path: Path):
        self.db_path = db_path
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self.conn = sqlite3.connect(str(self.db_path))
        self.conn.execute("PRAGMA foreign_keys = ON;")
        self.conn.execute("PRAGMA journal_mode = WAL;")
        self._init_schema()

    def _init_schema(self) -> None:
        with self.conn:
            self.conn.execute(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    file_path TEXT PRIMARY KEY,
                    content_hash TEXT NOT NULL,
                    updated_at REAL NOT NULL
                );
                """
            )
            self.conn.execute(
                """
                CREATE TABLE IF NOT EXISTS chunks (
                    chunk_id TEXT PRIMARY KEY,
                    file_path TEXT NOT NULL,
                    section_title TEXT NOT NULL,
                    breadcrumbs TEXT NOT NULL,
                    header_level INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    embedding BLOB NOT NULL,
                    FOREIGN KEY (file_path) REFERENCES documents(file_path) ON DELETE CASCADE
                );
                """
            )
            self.conn.execute(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS chunks_fts USING fts5(
                    chunk_id UNINDEXED,
                    file_path,
                    breadcrumbs,
                    content,
                    tokenize = 'porter unicode61'
                );
                """
            )

    def needs_reindex(self, file_path: str, current_hash: str) -> bool:
        cursor = self.conn.execute("SELECT content_hash FROM documents WHERE file_path = ?", (file_path,))
        row = cursor.fetchone()
        if not row:
            return True
        return row[0] != current_hash

    def save_document(
        self,
        file_path: str,
        content_hash: str,
        chunks: list[MarkdownChunk],
        embeddings: list[np.ndarray],
    ) -> None:
        with self.conn:
            self.delete_document(file_path)
            self.conn.execute(
                "INSERT INTO documents (file_path, content_hash, updated_at) VALUES (?, ?, ?)",
                (file_path, content_hash, time.time()),
            )
            for chunk, emb in zip(chunks, embeddings):
                emb_bytes = emb.astype(np.float32).tobytes()
                self.conn.execute(
                    """
                    INSERT INTO chunks (chunk_id, file_path, section_title, breadcrumbs, header_level, content, embedding)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        chunk.chunk_id,
                        chunk.file_path,
                        chunk.section_title,
                        chunk.breadcrumbs,
                        chunk.header_level,
                        chunk.content,
                        emb_bytes,
                    ),
                )
                self.conn.execute(
                    """
                    INSERT INTO chunks_fts (chunk_id, file_path, breadcrumbs, content)
                    VALUES (?, ?, ?, ?)
                    """,
                    (chunk.chunk_id, chunk.file_path, chunk.breadcrumbs, chunk.content),
                )

    def delete_document(self, file_path: str) -> None:
        with self.conn:
            self.conn.execute(
                "DELETE FROM chunks_fts WHERE chunk_id IN (SELECT chunk_id FROM chunks WHERE file_path = ?)",
                (file_path,),
            )
            self.conn.execute("DELETE FROM chunks WHERE file_path = ?", (file_path,))
            self.conn.execute("DELETE FROM documents WHERE file_path = ?", (file_path,))

    def get_indexed_documents(self) -> dict[str, str]:
        cursor = self.conn.execute("SELECT file_path, content_hash FROM documents")
        return {row[0]: row[1] for row in cursor.fetchall()}

    def get_stats(self) -> dict[str, int | float]:
        doc_count = self.conn.execute("SELECT COUNT(*) FROM documents").fetchone()[0]
        chunk_count = self.conn.execute("SELECT COUNT(*) FROM chunks").fetchone()[0]
        size_bytes = self.db_path.stat().st_size if self.db_path.exists() else 0
        return {
            "documents": doc_count,
            "chunks": chunk_count,
            "size_bytes": size_bytes,
            "size_mb": round(size_bytes / (1024 * 1024), 2),
        }

    def close(self) -> None:
        self.conn.close()


def compute_file_hash(path: Path) -> str:
    sha = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest()


def discover_markdown_files(repo_root: Path) -> list[Path]:
    files: list[Path] = []
    docs_dir = repo_root / "docs"
    if docs_dir.is_dir():
        for p in docs_dir.rglob("*.md"):
            if p.is_file() and not any(part.startswith(".") for part in p.parts):
                files.append(p)

    agents_md = repo_root / "AGENTS.md"
    if agents_md.is_file():
        files.append(agents_md)

    return sorted(files)


def index_documentation(
    repo_root: Path,
    db_path: Path,
    force: bool = False,
    model_name: str = "BAAI/bge-small-en-v1.5",
) -> dict:
    db = RagDatabase(db_path)
    chunker = MarkdownChunker()
    files = discover_markdown_files(repo_root)

    current_rel_paths = {str(p.relative_to(repo_root)): p for p in files}
    indexed_docs = db.get_indexed_documents()

    # Clean up deleted documents
    for indexed_path in list(indexed_docs.keys()):
        if indexed_path not in current_rel_paths:
            db.delete_document(indexed_path)
            print(f"  Removed obsolete index for {indexed_path}")

    files_to_index: list[tuple[str, Path, str, list[MarkdownChunk]]] = []
    for rel_path, file_path in current_rel_paths.items():
        file_hash = compute_file_hash(file_path)
        if force or db.needs_reindex(rel_path, file_hash):
            chunks = chunker.chunk_file(file_path, repo_root=repo_root)
            if chunks:
                files_to_index.append((rel_path, file_path, file_hash, chunks))

    if not files_to_index:
        stats = db.get_stats()
        print(f"  All {stats['documents']} documents ({stats['chunks']} chunks) up to date.")
        db.close()
        return stats

    print(f"  Found {len(files_to_index)} document(s) requiring indexing.")
    from fastembed import TextEmbedding

    embedding_model = TextEmbedding(model_name=model_name)

    all_chunks_to_embed: list[MarkdownChunk] = []
    for _, _, _, chunks in files_to_index:
        all_chunks_to_embed.extend(chunks)

    texts = [c.enriched_text for c in all_chunks_to_embed]
    embeddings_gen = embedding_model.embed(texts)
    embeddings = list(embeddings_gen)

    emb_idx = 0
    for rel_path, _, file_hash, chunks in files_to_index:
        chunk_embeddings = embeddings[emb_idx : emb_idx + len(chunks)]
        emb_idx += len(chunks)
        db.save_document(rel_path, file_hash, chunks, chunk_embeddings)
        print(f"  Indexed {rel_path} ({len(chunks)} chunks)")

    stats = db.get_stats()
    print(f"  Indexing complete: {stats['documents']} docs, {stats['chunks']} chunks ({stats['size_mb']} MB).")
    db.close()
    return stats


def main() -> None:
    parser = argparse.ArgumentParser(description="Index SplitTrip documentation for local RAG.")
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--db-path", type=Path, default=None)
    parser.add_argument("--force", action="store_true", help="Force reindexing all documents.")
    parser.add_argument("--check", action="store_true", help="Check if any document requires indexing.")
    args = parser.parse_args()

    repo_root = args.repo_root.resolve()
    db_path = args.db_path or (repo_root / ".cache" / "splittrip_rag.db")
    db_path = db_path.resolve()

    if args.check:
        db = RagDatabase(db_path)
        files = discover_markdown_files(repo_root)
        dirty = []
        for file_path in files:
            rel = str(file_path.relative_to(repo_root))
            f_hash = compute_file_hash(file_path)
            if db.needs_reindex(rel, f_hash):
                dirty.append(rel)
        db.close()
        if dirty:
            print(f"Index stale. {len(dirty)} file(s) need reindexing: {', '.join(dirty)}")
            sys.exit(1)
        else:
            print("Index is fully up to date.")
            sys.exit(0)

    index_documentation(repo_root, db_path, force=args.force)


if __name__ == "__main__":
    main()
