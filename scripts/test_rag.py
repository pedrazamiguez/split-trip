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
test_rag.py — Automated test suite for the SplitTrip documentation RAG pipeline.

Tests:
  - test_chunking_hierarchy
  - test_code_fence_preservation
  - test_incremental_indexing
  - test_dense_vector_search
  - test_sparse_fts5_search
  - test_hybrid_rrf_scoring
  - test_mcp_tool_execution
"""

from __future__ import annotations

import os
from pathlib import Path
import sys
import tempfile
import unittest
import numpy as np

_scripts_dir = Path(__file__).resolve().parent
if str(_scripts_dir) not in sys.path:
    sys.path.insert(0, str(_scripts_dir))

from rag_indexer import MarkdownChunker, RagDatabase, compute_file_hash
from rag_mcp_server import (
    HybridSearchEngine,
    get_doc_section,
    search_engine,
    search_project_knowledge,
)


class TestRagPipeline(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parent.parent

    def test_chunking_hierarchy(self) -> None:
        """Validates that Markdown headers are parsed into hierarchical breadcrumbs."""
        sample_md = """# Root Document
Introductory summary here.

## Section Alpha
Content inside alpha section.

### Alpha Subsection One
Deeply nested details.

## Section Beta
Content inside beta section.
"""
        chunker = MarkdownChunker(max_words=200)
        chunks = chunker.chunk_text(sample_md, "docs/sample.md")

        self.assertEqual(len(chunks), 4)

        # Chunk 0: Root Document
        self.assertEqual(chunks[0].section_title, "Root Document")
        self.assertEqual(chunks[0].breadcrumbs, "Root Document")
        self.assertIn("Introductory summary", chunks[0].content)

        # Chunk 1: Section Alpha
        self.assertEqual(chunks[1].section_title, "Section Alpha")
        self.assertEqual(chunks[1].breadcrumbs, "Root Document > Section Alpha")
        self.assertIn("Content inside alpha", chunks[1].content)

        # Chunk 2: Alpha Subsection One
        self.assertEqual(chunks[2].section_title, "Alpha Subsection One")
        self.assertEqual(chunks[2].breadcrumbs, "Root Document > Section Alpha > Alpha Subsection One")
        self.assertIn("Deeply nested details", chunks[2].content)

        # Chunk 3: Section Beta
        self.assertEqual(chunks[3].section_title, "Section Beta")
        self.assertEqual(chunks[3].breadcrumbs, "Root Document > Section Beta")
        self.assertIn("Content inside beta", chunks[3].content)

    def test_code_fence_preservation(self) -> None:
        """Ensures code fences are preserved intact and hash headers inside fences are ignored."""
        sample_md = """# Architecture Notes

Here is a Kotlin code block with comments and internal hashes:

```kotlin
// # This is a comment, not a markdown heading
fun calculateTotal(): BigDecimal {
    // ## Another comment
    return BigDecimal.TEN
}
```

Trailing notes after code block.
"""
        chunker = MarkdownChunker(max_words=200)
        chunks = chunker.chunk_text(sample_md, "docs/code.md")

        self.assertEqual(len(chunks), 1)
        self.assertEqual(chunks[0].breadcrumbs, "Architecture Notes")
        self.assertIn("fun calculateTotal(): BigDecimal", chunks[0].content)
        self.assertIn("// # This is a comment, not a markdown heading", chunks[0].content)
        self.assertIn("Trailing notes after code block.", chunks[0].content)

    def test_incremental_indexing(self) -> None:
        """Ensures unchanged files are skipped and modified files trigger re-indexing."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            db_path = Path(tmp_dir) / "test_rag.db"
            db = RagDatabase(db_path)

            doc_file = Path(tmp_dir) / "test_doc.md"
            doc_file.write_text("# Test\nInitial content", encoding="utf-8")
            h1 = compute_file_hash(doc_file)

            # Initially needs indexing
            self.assertTrue(db.needs_reindex("test_doc.md", h1))

            # Chunk and save
            chunker = MarkdownChunker()
            chunks = chunker.chunk_file(doc_file, repo_root=Path(tmp_dir))
            dummy_emb = [np.ones(384, dtype=np.float32)]
            db.save_document("test_doc.md", h1, chunks, dummy_emb)

            # Should no longer need indexing with same hash
            self.assertFalse(db.needs_reindex("test_doc.md", h1))

            # Modify file
            doc_file.write_text("# Test\nUpdated content with changes", encoding="utf-8")
            h2 = compute_file_hash(doc_file)
            self.assertNotEqual(h1, h2)

            # Now needs reindex
            self.assertTrue(db.needs_reindex("test_doc.md", h2))
            db.close()

    def test_dense_vector_search(self) -> None:
        """Validates dense cosine similarity ranking using synthetic vectors."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            db_path = Path(tmp_dir) / "test_vector.db"
            db = RagDatabase(db_path)

            chunker = MarkdownChunker()
            doc_file = Path(tmp_dir) / "doc1.md"
            doc_file.write_text("# Item 1\nAlpha", encoding="utf-8")
            chunks1 = chunker.chunk_file(doc_file, repo_root=Path(tmp_dir))

            doc_file2 = Path(tmp_dir) / "doc2.md"
            doc_file2.write_text("# Item 2\nBeta", encoding="utf-8")
            chunks2 = chunker.chunk_file(doc_file2, repo_root=Path(tmp_dir))

            # Vector 1 points along axis 0, Vector 2 points along axis 1
            v1 = np.zeros(384, dtype=np.float32)
            v1[0] = 1.0
            v2 = np.zeros(384, dtype=np.float32)
            v2[1] = 1.0

            db.save_document("doc1.md", "h1", chunks1, [v1])
            db.save_document("doc2.md", "h2", chunks2, [v2])
            db.close()

            engine = HybridSearchEngine(db_path=db_path, repo_root=Path(tmp_dir))

            # Search with query vector pointing along axis 0
            query_vec = np.zeros(384, dtype=np.float32)
            query_vec[0] = 1.0

            dense_results = engine.dense_search(query_vec, top_k=2)
            self.assertEqual(len(dense_results), 2)
            # doc1 should be top match with cosine similarity ~ 1.0
            self.assertEqual(dense_results[0][0], chunks1[0].chunk_id)
            self.assertAlmostEqual(dense_results[0][1], 1.0, places=4)

    def test_sparse_fts5_search(self) -> None:
        """Validates exact keyword retrieval using SQLite FTS5 BM25."""
        # Query project database for domain keyword
        sparse_res = search_engine.sparse_search("RoundingMode", top_k=5)
        self.assertGreater(len(sparse_res), 0)

        chunk_id = sparse_res[0][0]
        meta = search_engine.chunk_metadata.get(chunk_id)
        self.assertIsNotNone(meta)
        self.assertTrue(
            "RoundingMode" in meta["content"]
            or "RoundingMode" in meta["breadcrumbs"]
            or "0002-bigdecimal-math.md" in meta["file_path"]
        )

    def test_hybrid_rrf_scoring(self) -> None:
        """Validates Reciprocal Rank Fusion calculation."""
        # Query something with both dense and sparse presence
        results = search_engine.hybrid_search("BigDecimal precision rules", top_k=3)
        self.assertGreater(len(results), 0)

        top_chunk = results[0]
        self.assertIn("docs/architecture/adrs/0002-bigdecimal-math.md", top_chunk["file_path"])
        self.assertGreater(top_chunk["rrf_score"], 0.0)

    def test_mcp_tool_execution(self) -> None:
        """Executes search_project_knowledge and get_doc_section for authoritative domain invariants."""
        # 1. FIFO cash tranche query
        fifo_output = search_project_knowledge("FIFO cash tranche rules", top_k=3)
        self.assertIn("docs/domain/cash-tranches-and-fifo.md", fifo_output)
        self.assertIn("FIFO", fifo_output)

        # 2. BigDecimal precision query
        bigdecimal_output = search_project_knowledge("BigDecimal precision rules", top_k=3)
        self.assertIn("docs/architecture/adrs/0002-bigdecimal-math.md", bigdecimal_output)
        self.assertIn("RoundingMode", bigdecimal_output)

        # 3. Subunit composition rules query
        subunit_output = search_project_knowledge("Subunit composition rules", top_k=3)
        self.assertIn("docs/domain/subunits-and-composition.md", subunit_output)

        # 4. get_doc_section query
        section_output = get_doc_section(
            "docs/architecture/adrs/0002-bigdecimal-math.md",
            section="Decision Drivers",
        )
        self.assertIn("ADR-0002: Strict BigDecimal Math", section_output)
        self.assertIn("Decision Drivers", section_output)


if __name__ == "__main__":
    unittest.main()
