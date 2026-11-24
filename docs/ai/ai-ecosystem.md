# AI Development Ecosystem

This document describes SplitTrip's AI development architecture, configuration hierarchy, Model Context Protocol (MCP) tool integrations, and specialized multi-agent workflow taxonomy.

---

## 1. Taxonomy & Structure

The AI tooling in SplitTrip is organized into distinct, modular layers:

```
.agents/
├── hooks.json              → PreToolUse hooks (e.g. github-mcp-guard blocking broken/git-mutating MCP writes)
├── rules/                  → Passive, always-on constraints (`trigger: always_on`: graphify.md, github-operations.md)
├── scripts/                → Hook handler scripts (e.g. github-mcp-guard.py)
└── skills/                 → Active, phase-gated multi-step workflows (e.g. sp-start-issue, sp-scaffold-usecase)
.opencode/
├── commands/               → Slash-command bindings exposed to developer chat interfaces
└── opencode.json           → Project-level environment and plugin registration
docs/
├── domain/                 → Living business invariants and ubiquitous language grounding
├── architecture/           → Formal MADRs and structural pattern documentation
├── design-system/          → Horizon narrative visual tokens and UI guidelines
├── engineering/            → Quality standards, static analysis, and validation rules
└── ai/                     → Agent taxonomy, MCP catalogs, and intelligence benchmarks
```

### 1.1 Rules (`.agents/rules/` & `AGENTS.md`)
* **Nature:** Passive, always-on constraints and boundary enforcements.
* **Role:** Read at session initialization to prevent architectural regressions (e.g. BigDecimal math, no FQNs, <600 lines file limit, debounced clicks).

### 1.2 Skills (`.agents/skills/`)
* **Nature:** Active, executable workflows invoked via slash commands or subagents.
* **Structure:** Each skill is a directory containing a `SKILL.md` with YAML frontmatter specifying name, description, tools, and arguments, followed by markdown instructions.

### 1.3 Grounding Context (`/docs/`)
* **Nature:** Authoritative, machine-readable documentation providing deep domain, architectural, and design knowledge to AI agents during phased execution.

---

## 2. MCP Server Integrations

SplitTrip leverages several Model Context Protocol (MCP) servers to provide live, structured access to external systems and code graphs:

| MCP Server | Primary Capabilities | Primary Consumer |
|---|---|---|
| **`codebase-memory-mcp`** | Knowledge graph discovery, AST symbol navigation (`search_graph`, `trace_path`, `get_code_snippet`) | Architect Agent, Domain Implementer |
| **`graphify`** | Community detection, dependency graphs, god node identification, architectural blast-radius queries | Architect Agent, Specification Agent |
| **`splittrip-rag`** | Local semantic & BM25 documentation retrieval (`search_project_knowledge`, `get_doc_section`) | All Agent Personas (Domain, Architect, Spec) |
| **`github-mcp-server`** | Read-only issue/PR lookups and PR reviews (issue writes route through `gh` CLI; `issue_write` and autonomous git-mutating tools are blocked by `.agents/hooks.json` + `.agents/scripts/github-mcp-guard.py`) | Specification Agent, Auditor / QA Agent |
| **`firebase-mcp-server`** | Firestore schema introspection, security rule evaluation, cloud resource verification | Infra & UI Implementer |
| **`StitchMCP`** | UI screen scaffolding, design token synchronization, visual component generation | Infra & UI Implementer |
| **`sonarqube`** | Quality gate status, code smell detection, security hotspot inspection | Auditor / QA Agent |

---

## 3. Configuration Hierarchy

1. **Global Configuration:** `~/.gemini/antigravity/` and `~/.config/opencode/opencode.jsonc` configure MCP server endpoints, global agent presets, and default token limits.
2. **Project Configuration:** `.opencode/opencode.json` registers project-specific plugins and command mappings.
3. **Repository Setup:** Run `scripts/ai-setup.sh` to initialize, update, and link local MCP servers and tools.
