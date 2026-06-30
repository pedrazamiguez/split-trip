#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# ai-setup.sh — Idempotent setup for AI Code Intelligence MCP tools
#
# Installs codebase-memory-mcp + Graphify, builds indexes, merges MCP configs.
# Safe to re-run (idempotent) — will skip installed components and only
# update indexes if stale.
#
# Usage:
#   ./scripts/ai-setup.sh
#   make ai-setup
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { printf "${CYAN}⏳  %s${NC}\n" "$1"; }
log_ok()    { printf "${GREEN}✅  %s${NC}\n" "$1"; }
log_warn()  { printf "${YELLOW}⚠️   %s${NC}\n" "$1"; }
log_err()   { printf "${RED}✗   %s${NC}\n" "$1"; }

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CBM_BIN="${HOME}/.local/bin/codebase-memory-mcp"
UV_BIN="${HOME}/.local/bin/uv"
GEMINI_MCP="${HOME}/.gemini/config/mcp_config.json"
OPENCODE_CONFIG="${HOME}/.config/opencode/opencode.jsonc"

errors=0

# ─── Step 1: Install codebase-memory-mcp ────────────────────────────────────
install_codebase_memory() {
  if [ -x "$CBM_BIN" ]; then
    log_ok "codebase-memory-mcp already installed at ${CBM_BIN}"
    return 0
  fi
  log_info "Installing codebase-memory-mcp..."
  if curl -LsSf https://raw.githubusercontent.com/DeusData/codebase-memory-mcp/main/install.sh | bash; then
    log_ok "codebase-memory-mcp installed"
  else
    log_err "Failed to install codebase-memory-mcp"
    return 1
  fi
}

# ─── Step 2: Build codebase-memory-mcp index ─────────────────────────────────
index_codebase_memory() {
  if [ ! -x "$CBM_BIN" ]; then
    log_err "codebase-memory-mcp not found — cannot index"
    return 1
  fi
  local cache_dir="${HOME}/.cache/codebase-memory-mcp"
  if [ -d "$cache_dir" ]; then
    log_ok "codebase-memory-mcp index already exists at ${cache_dir}"
    return 0
  fi
  log_info "Building codebase-memory-mcp index (this may take a minute)..."
  if "$CBM_BIN" init && "$CBM_BIN" index; then
    log_ok "codebase-memory-mcp index built"
  else
    log_err "Failed to build codebase-memory-mcp index"
    return 1
  fi
}

# ─── Step 3: Install uv (if missing) ─────────────────────────────────────────
install_uv() {
  if command -v uv &>/dev/null; then
    log_ok "uv already installed"
    return 0
  fi
  if [ -x "$UV_BIN" ]; then
    log_ok "uv already installed at ${UV_BIN}"
    return 0
  fi
  log_info "Installing uv..."
  if curl -LsSf https://astral.sh/uv/install.sh | sh; then
    log_ok "uv installed"
  else
    log_err "Failed to install uv"
    return 1
  fi
}

# ─── Step 4: Install Graphify ────────────────────────────────────────────────
install_graphify() {
  if command -v graphify &>/dev/null; then
    log_ok "graphify already installed"
    return 0
  fi
  if ! command -v uv &>/dev/null && [ ! -x "$UV_BIN" ]; then
    log_err "uv not found — cannot install graphify"
    return 1
  fi
  log_info "Installing graphify..."
  if uv tool install graphifyy; then
    log_ok "graphify installed"
  else
    log_err "Failed to install graphify"
    return 1
  fi
}

# ─── Step 5: Build Graphify index ────────────────────────────────────────────
index_graphify() {
  local gdir="${PROJECT_ROOT}/graphify-out"
  if [ -f "${gdir}/graph.json" ]; then
    log_ok "Graphify index already exists at ${gdir}"
    return 0
  fi
  if ! command -v graphify &>/dev/null; then
    log_err "graphify not found — cannot index"
    return 1
  fi
  log_info "Building Graphify index..."
  mkdir -p "$gdir"
  if graphify index . --output "$gdir"; then
    log_ok "Graphify index built at ${gdir}"
  else
    log_err "Failed to build Graphify index"
    return 1
  fi
}

# ─── Step 6: Install Graphify platform hooks ────────────────────────────────
install_graphify_hooks() {
  if ! command -v graphify &>/dev/null; then
    log_err "graphify not found — cannot install hooks"
    return 1
  fi
  log_info "Installing Graphify platform hooks..."
  if graphify install --platform opencode --project; then
    log_ok "Graphify platform hooks installed"
  else
    log_warn "graphify install failed (may already be installed)"
  fi
}

# ─── Step 7: Merge MCP configs into Gemini mcp_config.json ──────────────────
merge_gemini_config() {
  log_info "Merging MCP entries into ${GEMINI_MCP}..."
  python3 -c "
import json, os, sys

path = os.path.expanduser('${GEMINI_MCP}')
cbm_bin = os.path.expanduser('${CBM_BIN}')

# Read existing config or start fresh
if os.path.exists(path):
    with open(path) as f:
        config = json.load(f)
else:
    config = {'mcpServers': {}}

servers = config.setdefault('mcpServers', {})

# Add codebase-memory-mcp if not present
if 'codebase-memory-mcp' not in servers:
    servers['codebase-memory-mcp'] = {
        'command': cbm_bin,
        'args': []
    }
    print('  Added codebase-memory-mcp to Gemini MCP config')
else:
    print('  codebase-memory-mcp already in Gemini MCP config')

# Add graphify if not present
if 'graphify' not in servers:
    servers['graphify'] = {
        'command': 'graphify',
        'args': ['serve']
    }
    print('  Added graphify to Gemini MCP config')
else:
    print('  graphify already in Gemini MCP config')

with open(path, 'w') as f:
    json.dump(config, f, indent=2)
    f.write('\n')
print('  Gemini MCP config updated')
"
  log_ok "Gemini MCP config merged"
}

# ─── Step 8: Merge MCP configs into opencode.jsonc ──────────────────────────
merge_opencode_config() {
  log_info "Merging MCP entry into ${OPENCODE_CONFIG}..."
  python3 -c "
import json, os

path = os.path.expanduser('${OPENCODE_CONFIG}')
cbm_bin = os.path.expanduser('${CBM_BIN}')

# opencode.jsonc may have trailing comma — use json5-like tolerance by
# treating it as plain text merge. We do a structural insert via json then
# re-serialize (single-line command arrays to keep it compact).
if os.path.exists(path):
    with open(path) as f:
        raw = f.read()

# Simplified: parse with json, add entry, rewrite
# We strip comments/commas by using a basic approach
import re
# Remove single-line comments
content = re.sub(r'//.*', '', raw)
content = re.sub(r',\s*}', '}', content)
content = re.sub(r',\s*]', ']', content)

config = json.loads(content)
mcp_block = config.setdefault('mcp', {})

if 'codebase-memory-mcp' not in mcp_block:
    mcp_block['codebase-memory-mcp'] = {
        'type': 'local',
        'command': [cbm_bin],
        'enabled': True
    }
    print('  Added codebase-memory-mcp to opencode.jsonc')
else:
    print('  codebase-memory-mcp already in opencode.jsonc')

with open(path, 'w') as f:
    json.dump(config, f, indent=2)
    f.write('\n')
print('  opencode.jsonc updated')
"
  log_ok "Opencode config merged"
}

# ─── Summary ─────────────────────────────────────────────────────────────────
print_summary() {
  echo ""
  printf "${YELLOW}═══════════════════════════════════════${NC}\n"
  if [ "$errors" -eq 0 ]; then
    printf "${GREEN}  ✅  AI setup complete — all steps passed${NC}\n"
  else
    printf "${RED}  ✗   AI setup completed with ${errors} error(s)${NC}\n"
    printf "${YELLOW}  Review messages above and re-run after fixing${NC}\n"
  fi
  printf "${YELLOW}═══════════════════════════════════════${NC}\n"
  echo ""
  printf "  Run ${CYAN}make doctor${NC} to verify the installation.\n"
}

# ─── Main ────────────────────────────────────────────────────────────────────
main() {
  echo ""
  printf "${CYAN}═══════════════════════════════════════${NC}\n"
  printf "${CYAN}  SplitTrip — AI Code Intelligence Setup${NC}\n"
  printf "${CYAN}═══════════════════════════════════════${NC}\n"
  echo ""

  install_codebase_memory || ((errors++))
  echo ""
  index_codebase_memory || ((errors++))
  echo ""
  install_uv || ((errors++))
  echo ""
  install_graphify || ((errors++))
  echo ""
  index_graphify || ((errors++))
  echo ""
  install_graphify_hooks || ((errors++))
  echo ""
  merge_gemini_config || ((errors++))
  echo ""
  merge_opencode_config || ((errors++))
  echo ""

  print_summary
  return "$errors"
}

main "$@"
