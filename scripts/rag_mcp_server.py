#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10,<3.15"
# dependencies = [
#     "mcp>=1.3.0,<2",
#     "fastembed>=0.5.0",
#     "numpy>=1.26.0",
# ]
# ///
"""
rag_mcp_server.py — Stdio MCP server exposing local documentation RAG retrieval tools.

Exposes:
  - search_project_knowledge(query, top_k): Hybrid dense + BM25 search over SplitTrip docs.
  - get_doc_section(file_path, section): Retrieve specific section or full document text.
  - reindex_project_knowledge(force): Re-indexes markdown docs and updates the in-memory vector index.
"""

from __future__ import annotations

import os
from pathlib import Path
import re
import sqlite3
from typing import Any
import numpy as np
from fastembed import TextEmbedding
from mcp.server.fastmcp import FastMCP

import sys

_scripts_dir = Path(__file__).resolve().parent
if str(_scripts_dir) not in sys.path:
    sys.path.insert(0, str(_scripts_dir))

from rag_indexer import RagDatabase, index_documentation


class HybridSearchEngine:
    """Hybrid dense vector + SQLite FTS5 BM25 search engine using Reciprocal Rank Fusion (RRF)."""

    def __init__(
        self,
        db_path: Path,
        repo_root: Path,
        model_name: str = "BAAI/bge-small-en-v1.5",
    ):
        self.db_path = db_path
        self.repo_root = repo_root
        self.model_name = model_name
        self.embedding_model = TextEmbedding(model_name=model_name)

        self.chunk_ids: list[str] = []
        self.chunk_metadata: dict[str, dict[str, Any]] = {}
        self.embedding_matrix: np.ndarray = np.zeros((0, 384), dtype=np.float32)

        self.reload_index()

    def reload_index(self) -> None:
        """Load all chunks and embeddings into memory for sub-millisecond dense search."""
        if not self.db_path.exists():
            return

        conn = sqlite3.connect(str(self.db_path))
        cursor = conn.execute(
            """
            SELECT chunk_id, file_path, section_title, breadcrumbs, header_level, content, embedding
            FROM chunks
            """
        )
        rows = cursor.fetchall()
        conn.close()

        chunk_ids: list[str] = []
        chunk_metadata: dict[str, dict[str, Any]] = {}
        embeddings_list: list[np.ndarray] = []

        for row in rows:
            c_id, f_path, s_title, b_crumbs, h_level, content, emb_bytes = row
            chunk_ids.append(c_id)
            chunk_metadata[c_id] = {
                "chunk_id": c_id,
                "file_path": f_path,
                "section_title": s_title,
                "breadcrumbs": b_crumbs,
                "header_level": h_level,
                "content": content,
            }
            emb = np.frombuffer(emb_bytes, dtype=np.float32)
            embeddings_list.append(emb)

        if embeddings_list:
            matrix = np.vstack(embeddings_list)
            # Normalize embedding rows for cosine similarity via dot product
            norms = np.linalg.norm(matrix, axis=1, keepdims=True)
            norms[norms == 0] = 1.0
            matrix = matrix / norms
            self.embedding_matrix = matrix
        else:
            self.embedding_matrix = np.zeros((0, 384), dtype=np.float32)

        self.chunk_ids = chunk_ids
        self.chunk_metadata = chunk_metadata

    def dense_search(self, query_vec: np.ndarray, top_k: int = 20) -> list[tuple[str, float]]:
        """Dense cosine similarity search against in-memory embedding matrix."""
        if self.embedding_matrix.shape[0] == 0:
            return []

        norm = np.linalg.norm(query_vec)
        if norm > 0:
            query_vec = query_vec / norm

        scores = np.dot(self.embedding_matrix, query_vec)
        top_indices = np.argsort(scores)[::-1][:top_k]

        results = []
        for idx in top_indices:
            c_id = self.chunk_ids[idx]
            score = float(scores[idx])
            results.append((c_id, score))
        return results

    def sparse_search(self, query: str, top_k: int = 20) -> list[tuple[str, float]]:
        """Sparse BM25 search via SQLite FTS5."""
        if not self.db_path.exists():
            return []

        tokens = re.findall(r"\w+", query)
        if not tokens:
            return []

        fts_query = " OR ".join(f'"{t}"' for t in tokens)
        try:
            conn = sqlite3.connect(str(self.db_path))
            cursor = conn.execute(
                """
                SELECT chunk_id, rank
                FROM chunks_fts
                WHERE chunks_fts MATCH ?
                ORDER BY rank ASC
                LIMIT ?
                """,
                (fts_query, top_k),
            )
            rows = cursor.fetchall()
            conn.close()
            # In SQLite FTS5, lower rank is better (more negative)
            return [(row[0], float(row[1])) for row in rows]
        except Exception:
            return []

    def hybrid_search(self, query: str, top_k: int = 5) -> list[dict[str, Any]]:
        """Combine dense and sparse search rankings using Reciprocal Rank Fusion (RRF)."""
        # 1. Dense search
        query_embeddings = list(self.embedding_model.embed([query]))
        dense_results: list[tuple[str, float]] = []
        if query_embeddings:
            dense_results = self.dense_search(query_embeddings[0], top_k=max(20, top_k * 4))

        # 2. Sparse search
        sparse_results = self.sparse_search(query, top_k=max(20, top_k * 4))

        # 3. Reciprocal Rank Fusion (k = 60)
        k = 60.0
        rrf_scores: dict[str, float] = {}

        for rank, (c_id, _) in enumerate(dense_results, start=1):
            rrf_scores[c_id] = rrf_scores.get(c_id, 0.0) + (1.0 / (k + rank))

        for rank, (c_id, _) in enumerate(sparse_results, start=1):
            rrf_scores[c_id] = rrf_scores.get(c_id, 0.0) + (1.0 / (k + rank))

        sorted_chunks = sorted(rrf_scores.items(), key=lambda x: x[1], reverse=True)[:top_k]

        results = []
        for c_id, score in sorted_chunks:
            meta = self.chunk_metadata.get(c_id)
            if meta:
                item = dict(meta)
                item["rrf_score"] = round(score, 5)
                results.append(item)

        return results


# Resolve paths relative to repository root
REPO_ROOT = Path(__file__).resolve().parent.parent
DB_PATH = REPO_ROOT / ".cache" / "splittrip_rag.db"

# Initialize search engine
search_engine = HybridSearchEngine(db_path=DB_PATH, repo_root=REPO_ROOT)

# MCP Server definition
mcp = FastMCP(
    "splittrip-rag",
    instructions="Local documentation RAG retrieval tools for SplitTrip domain, architecture, and design rules.",
)


@mcp.tool()
def search_project_knowledge(query: str, top_k: int = 5) -> str:
    """Search SplitTrip domain invariants, architectural decisions, and design guidelines.

    Uses local hybrid retrieval (dense vectors + SQLite BM25 full-text search) with zero external
    API calls. Returns ranked markdown sections with breadcrumbs and relevance scores.

    Args:
        query: Semantic or keyword search query (e.g. 'FIFO cash tranche rules', 'BigDecimal precision').
        top_k: Number of ranked chunks to return (default 5, max 20).
    """
    top_k = min(max(1, top_k), 20)
    results = search_engine.hybrid_search(query, top_k=top_k)

    if not results:
        return f"No documentation chunks found matching query: '{query}'."

    output = [f"## Knowledge Search Results for: `{query}`\n"]
    for i, res in enumerate(results, start=1):
        output.append(f"### {i}. `{res['file_path']}`")
        output.append(f"**Section:** {res['breadcrumbs']}")
        output.append(f"**Relevance Score (RRF):** {res['rrf_score']}\n")
        output.append(res["content"])
        output.append("\n---\n")

    return "\n".join(output)


@mcp.tool()
def get_doc_section(file_path: str, section: str = "") -> str:
    """Retrieve full text of a specific document section or an entire documentation file.

    Args:
        file_path: Relative path to documentation file (e.g. 'docs/domain/cash-tranches-and-fifo.md').
        section: Optional section title or breadcrumb substring to filter.
    """
    clean_path = file_path.strip().lstrip("/")
    matched_chunks = [
        meta
        for meta in search_engine.chunk_metadata.values()
        if meta["file_path"].lower() == clean_path.lower()
        or meta["file_path"].lower().endswith(clean_path.lower())
    ]

    if not matched_chunks:
        # Fallback to direct file read if present on disk
        target_file = (REPO_ROOT / clean_path).resolve()
        if target_file.is_file():
            return target_file.read_text(encoding="utf-8")
        return f"Document not found: {file_path}"

    if section.strip():
        sec_clean = section.strip().lower()
        filtered = [
            c
            for c in matched_chunks
            if sec_clean in c["section_title"].lower() or sec_clean in c["breadcrumbs"].lower()
        ]
        if filtered:
            matched_chunks = filtered
        else:
            return f"Section '{section}' not found in {file_path}. Available sections:\n" + "\n".join(
                f"- {c['breadcrumbs']}" for c in matched_chunks
            )

    output = [f"# `{matched_chunks[0]['file_path']}`\n"]
    for c in matched_chunks:
        output.append(f"## {c['breadcrumbs']}\n\n{c['content']}\n")

    return "\n".join(output)


@mcp.tool()
def reindex_project_knowledge(force: bool = False) -> str:
    """Trigger incremental or forced re-indexing of documentation into the local RAG database.

    Args:
        force: If True, re-indexes all markdown files even if unchanged.
    """
    stats = index_documentation(REPO_ROOT, DB_PATH, force=force)
    search_engine.reload_index()
    return (
        f"Re-indexing complete: {stats['documents']} documents, {stats['chunks']} chunks "
        f"({stats['size_mb']} MB in {DB_PATH.name}). In-memory vector matrix refreshed."
    )


def main() -> None:
    mcp.run()


if __name__ == "__main__":
    main()
