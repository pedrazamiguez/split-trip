# Code Intelligence MCP Tools

## Motivation

Every AI coding session on SplitTrip spends a significant portion of its token budget on structural re-discovery: re-grepping the same codebase, re-reading the same files, and re-resolving the same architectural relationships. Code-intelligence MCP tools address this by providing pre-indexed, queryable knowledge graphs of the codebase, reducing token consumption by 80–99% for structural queries.

## Architecture Overview

```
Agent (OpenCode / Antigravity)
  │
  ├── MCP Runtime
  │     ├── GitHub MCP Server        (existing)
  │     ├── SonarQube MCP Server     (existing)
  │     ├── StitchMCP                (existing)
  │     ├── Firebase MCP Server      (existing)
  │     │
  │     ├── codebase-memory-mcp      [Phase 1] — Structural graph, static C binary
  │     └── graphify                 [Phase 1] — Semantic graph + HTML viz
  │
  └── Local Indexes (persistent)
        ├── ~/.cache/codebase-memory-mcp/  (SQLite, auto-updated)
        └── graphify-out/                  (JSON + HTML, git-committed)
```

## Tool Catalog

| Tool | Repo | Purpose | Token Savings | Install Method | OpenCode | Antigravity | Phase |
|------|------|---------|--------------|---------------|----------|-------------|-------|
| **codebase-memory-mcp** | [DeusData/codebase-memory-mcp](https://github.com/DeusData/codebase-memory-mcp) | Structural code graph (tree-sitter + LSP), 14 MCP tools, impact analysis | 120× fewer tokens | Static C binary (`curl \| bash`) | ✅ Auto | ✅ Auto | **Phase 1 — Active** |
| **Graphify** | [safishamsi/graphify](https://github.com/safishamsi/graphify) | Multi-modal knowledge graph (AST + LLM), HTML report, team-shareable artifacts | 71.5× fewer tokens | `uv tool install graphifyy` | ✅ Native `--platform opencode` | ✅ Dedicated `antigravity install` | **Phase 1 — Active** |
| **Gentle-AI** | [Gentleman-Programming/gentle-ai](https://github.com/Gentleman-Programming/gentle-ai) | Cross-session memory (Engram), SDD workflow, provider switching | Indirect (memory reuse) | `brew install gentle-ai` | ✅ Native SDD profiles | ✅ Native | Phase 2 — On Radar |
| **token-savior** | [Mibayy/token-savior](https://github.com/Mibayy/token-savior) | Structural index + Bash output compaction, 53 tools | 80–99.9% | `pip install token-savior-recall[mcp]` | ⚠️ Manual | ⚠️ Manual | Phase 2 — On Radar |
| **CodeGraph** | [codegraph-ai/CodeGraph](https://github.com/codegraph-ai/CodeGraph) | Rust semantic graph, 38 languages | 94% fewer tool calls | Manual binary | ❌ Not listed | ❌ Not listed | Skipped |
| **ai-token-optimizer** | [ooples/token-optimizer-mcp](https://github.com/ooples/token-optimizer-mcp) | Surgical file reading, output compression, token audit | 50–95% | `npm install` | ❌ Not listed | ❌ Not listed | Skipped |

## Phase 1 Tools (Active)

### codebase-memory-mcp

- **Binary:** Single static C binary (~15MB), zero dependencies
- **Indexing:** Tree-sitter (158 languages) + Hybrid LSP → SQLite cache at `~/.cache/codebase-memory-mcp/`
- **Tools:** 14 MCP tools including `get-entity`, `get-call-graph`, `impact-analysis`, `find-implementations`, etc.
- **OpenCode:** Auto-detected by `install` script — configures `opencode.jsonc`, `AGENTS.md`, and pre-tool hooks
- **Antigravity:** Auto-detected — configures MCP entries in `antigravity-cli/` + `SessionStart` hook

### Graphify

- **Runtime:** Python with `uv` (isolated), tree-sitter AST + optional LLM semantic extraction
- **Output:** `graphify-out/graph.json` (structured graph) + `graphify-out/report.html` (interactive visualization)
- **OpenCode:** `graphify install --platform opencode` writes `.agents/skills/graphify/SKILL.md` + `references/`
- **Antigravity:** `graphify antigravity install` writes `.agents/rules` + `.agents/workflows`
- **Git integration:** `graphify-out/` should be committed to share the graph artifact across the team

## Phase 2 Tools (On Radar)

### Gentle-AI

- **Purpose:** Not a code graph tool — an ecosystem configurator with cross-session persistent memory (Engram), SDD workflow profiles, and per-phase model routing
- **Trigger for adoption:** Observed cross-session context loss during development
- **Setup:** `brew install gentle-ai` + `gentle-ai install` (auto-detects OpenCode)

### token-savior

- **Purpose:** Structural code indexing + persistent memory engine + Bash output compaction (unique feature)
- **Trigger for adoption:** Bash output bloat degrading agent cost
- **Setup:** `pip install token-savior-recall[mcp]` + manual `opencode.jsonc` config
- **Note:** No OpenCode auto-detect — requires manual MCP config

## Workspace Setup

### Quick start

```bash
make ai-setup
```

This idempotent command:
1. Installs codebase-memory-mcp (if missing) and builds the index
2. Installs `uv` (if missing)
3. Installs Graphify via `uv tool install graphifyy`
4. Builds Graphify index to `graphify-out/`
5. Runs `graphify install --platform opencode --project`
6. Merges MCP entries into `~/.gemini/config/mcp_config.json`
7. Verifies all steps completed

### Verification

```bash
make doctor
```

Extended checks include:
- `codebase-memory-mcp` binary present
- `uv` present
- `graphify` present
- codebase-memory-mcp index exists
- Graphify index (`graphify-out/graph.json`) exists
- `opencode.jsonc` has codebase-memory-mcp entry
- Gemini MCP config has codebase-memory-mcp + graphify entries

## Sensitive Data Map

| Config File | Structural Fields (script-managed) | Secret Fields (env-var / manual) |
|-------------|-----------------------------------|----------------------------------|
| `~/.config/opencode/opencode.jsonc` | `mcp.codebase-memory-mcp.command`, `mcp.graphify.command` | `mcp.github.headers.Authorization` (GitHub PAT), `mcp.sonarqube.environment.SONARQUBE_TOKEN`, `mcp.stitch.command[...].X-Goog-Api-Key` |
| `~/.gemini/config/mcp_config.json` | `mcpServers.codebase-memory-mcp.args`, `mcpServers.graphify.args` | `mcpServers.github-mcp-server.env.GITHUB_PERSONAL_ACCESS_TOKEN`, `mcpServers.sonarqube.env.SONARQUBE_TOKEN`, `mcpServers.StitchMCP.args[...].X-Goog-Api-Key` |
| `~/.gemini/config/config.json` | N/A | Gemini API keys (user-managed) |

**Principle:** The `make ai-setup` script only writes structural MCP entries (command + args) — it never writes, overwrites, or reads secret fields. Secrets are provisioned via environment variables or manual placement.

## Backward Compatibility

All existing workflows are unaffected:

| Check | Status |
|-------|--------|
| `make check` | 0 failures (no app code changes) |
| Existing MCP servers (GitHub, SonarQube, Stitch, Firebase) | Continue working |
| `sp-start-issue` | Unchanged |
| `sp-review-pr` | Unchanged |
| `ktlint` / `detekt` / `konsist` | Unchanged |
| CI/CD pipelines | Unchanged |
