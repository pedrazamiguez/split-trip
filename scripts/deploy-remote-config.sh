#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# SplitTrip — Firebase Remote Config Deployment & Operational Tooling
#
# Commands:
#   validate         Validate consistency between XML defaults and JSON template
#   diff             Diff local template against live cloud configuration
#   deploy           Validate and deploy Remote Config template to Firebase
#   rollback <v>     Roll back Remote Config template to a specific version
#   versions         List published Remote Config versions
#   get [file]       Fetch live Remote Config template (to stdout or file)
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
TEMPLATE_FILE="${ROOT_DIR}/firebase/remoteconfig.template.json"
VALIDATOR_SCRIPT="${ROOT_DIR}/scripts/validate_remote_config.py"
MERGE_SCRIPT="${ROOT_DIR}/scripts/merge_remote_config.py"
FIREBASERC_FILE="${ROOT_DIR}/.firebaserc"

RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
CYAN="\033[0;36m"
BOLD="\033[1m"
NC="\033[0m"

usage() {
  echo -e "${BOLD}Usage:${NC} $0 <command> [arguments]"
  echo ""
  echo -e "${YELLOW}Commands:${NC}"
  echo -e "  ${CYAN}validate${NC}         Validate consistency between XML defaults and JSON template"
  echo -e "  ${CYAN}diff${NC}             Diff local template against live cloud configuration"
  echo -e "  ${CYAN}deploy [--overwrite]${NC} Validate and deploy Remote Config (additive merge by default)"
  echo -e "  ${CYAN}rollback <v>${NC}     Roll back Remote Config template to a specific version"
  echo -e "  ${CYAN}versions${NC}         List published Remote Config versions"
  echo -e "  ${CYAN}get [file]${NC}       Fetch live Remote Config template (to stdout or file)"
  echo ""
  exit 1
}

cmd_validate() {
  echo -e "${CYAN}🔍 Step 1: Validating Remote Config template consistency...${NC}"
  python3 "${VALIDATOR_SCRIPT}"
}

cmd_diff() {
  echo -e "${CYAN}🔍 Fetching live Remote Config template from Firebase...${NC}"
  TMP_LIVE="$(mktemp /tmp/rc_live_XXXXXX.json)"
  TMP_LOCAL="$(mktemp /tmp/rc_local_XXXXXX.json)"
  trap 'rm -f "${TMP_LIVE:-}" "${TMP_LOCAL:-}"' EXIT

  if ! npx firebase-tools remoteconfig:get -o "${TMP_LIVE}" > /dev/null 2>&1; then
    echo -e "${RED}❌ Failed to fetch live Remote Config template. Check Firebase authentication and project configuration.${NC}"
    exit 1
  fi

  # Normalize live and local templates by stripping ephemeral version metadata and sorting keys
  python3 -c "
import json

with open('${TMP_LIVE}') as f:
    live = json.load(f)
live.pop('version', None)
if not live.get('parameters'):
    live.pop('parameters', None)

with open('${TEMPLATE_FILE}') as f:
    local = json.load(f)
local.pop('version', None)
if not local.get('parameters'):
    local.pop('parameters', None)

with open('${TMP_LIVE}', 'w') as f:
    json.dump(live, f, indent=2, sort_keys=True)
    f.write('\n')

with open('${TMP_LOCAL}', 'w') as f:
    json.dump(local, f, indent=2, sort_keys=True)
    f.write('\n')
"

  echo -e "${YELLOW}📊 Unified Diff (Live Cloud [---] vs Local Template [+++]):${NC}"
  echo ""
  if diff -u --color=auto "${TMP_LIVE}" "${TMP_LOCAL}"; then
    echo -e "${GREEN}✅ Local template matches live cloud configuration exactly.${NC}"
  else
    echo ""
    echo -e "${YELLOW}ℹ️  Differences found above. Local template has unapplied changes.${NC}"
  fi
}

cmd_deploy() {
  local OVERWRITE=false
  while [ $# -gt 0 ]; do
    case "$1" in
      --overwrite)
        OVERWRITE=true
        shift
        ;;
      *)
        echo -e "${RED}❌ Unknown deploy option: $1${NC}"
        usage
        ;;
    esac
  done

  echo -e "${BOLD}🚀 Deploying Firebase Remote Config...${NC}"
  cmd_validate

  if [ "${OVERWRITE}" = "true" ]; then
    echo ""
    echo -e "${YELLOW}⚠️  --overwrite enabled: replacing live Remote Config values with firebase/remoteconfig.template.json.${NC}"
    echo -e "${CYAN}📤 Publishing full template to Firebase...${NC}"
    npx firebase-tools deploy --only remoteconfig --force
  else
    TMP_DIR="$(mktemp -d /tmp/rc_deploy_XXXXXX)"
    trap 'rm -rf "${TMP_DIR:-}"' EXIT

    echo ""
    echo -e "${CYAN}📥 Fetching live Remote Config template from Firebase...${NC}"
    if ! npx firebase-tools remoteconfig:get -o "${TMP_DIR}/live.json"; then
      echo -e "${RED}❌ Failed to fetch live Remote Config template. Aborting to prevent destructive overwrite.${NC}"
      exit 1
    fi

    echo ""
    echo -e "${CYAN}🔀 Merging git template into live Remote Config template...${NC}"
    python3 "${MERGE_SCRIPT}" \
      --live "${TMP_DIR}/live.json" \
      --repo "${TEMPLATE_FILE}" \
      --output "${TMP_DIR}/remoteconfig.merged.json"

    cat > "${TMP_DIR}/firebase.json" <<EOF
{
  "remoteconfig": {
    "template": "${TMP_DIR}/remoteconfig.merged.json"
  }
}
EOF

    if [ -f "${FIREBASERC_FILE}" ]; then
      cp "${FIREBASERC_FILE}" "${TMP_DIR}/.firebaserc"
    fi

    echo ""
    echo -e "${CYAN}📤 Publishing merged template to Firebase...${NC}"
    npx firebase-tools deploy --only remoteconfig --config "${TMP_DIR}/firebase.json" --force
  fi

  echo ""
  echo -e "${GREEN}✅ Remote Config template deployed successfully!${NC}"
}

cmd_rollback() {
  if [ -z "${1:-}" ]; then
    echo -e "${RED}❌ Error: Missing version number to roll back to.${NC}"
    echo -e "Usage: $0 rollback <versionNumber>"
    exit 1
  fi
  VERSION_NUMBER="$1"

  echo -e "${YELLOW}⚠️  Rolling back Remote Config template to version ${VERSION_NUMBER}...${NC}"
  npx firebase-tools remoteconfig:rollback --version-number "${VERSION_NUMBER}" --force

  echo ""
  echo -e "${GREEN}✅ Rolled back Remote Config template to version ${VERSION_NUMBER}.${NC}"
}

cmd_versions() {
  echo -e "${CYAN}📜 Fetching Remote Config published versions...${NC}"
  npx firebase-tools remoteconfig:versions:list
}

cmd_get() {
  if [ -n "${1:-}" ]; then
    TARGET_FILE="$1"
    echo -e "${CYAN}📥 Fetching Remote Config template into ${TARGET_FILE}...${NC}"
    npx firebase-tools remoteconfig:get -o "${TARGET_FILE}"
    echo -e "${GREEN}✅ Saved live template to ${TARGET_FILE}.${NC}"
  else
    npx firebase-tools remoteconfig:get
  fi
}

# ─── Main Dispatcher ─────────────────────────────────────────────────────────

if [ $# -lt 1 ]; then
  usage
fi

COMMAND="$1"
shift

case "${COMMAND}" in
  validate)
    cmd_validate
    ;;
  diff)
    cmd_diff
    ;;
  deploy)
    cmd_deploy "$@"
    ;;
  rollback)
    cmd_rollback "$@"
    ;;
  versions)
    cmd_versions
    ;;
  get)
    cmd_get "$@"
    ;;
  *)
    echo -e "${RED}❌ Unknown command: ${COMMAND}${NC}"
    usage
    ;;
esac
