#!/usr/bin/env bash
set -euo pipefail

PROPS_FILE="$(dirname "$0")/../local.properties"

if [[ ! -f "$PROPS_FILE" ]]; then
  echo "ERROR: local.properties not found. Copy local.properties.example and fill in values."
  exit 1
fi

# Load properties as env vars (skip comments and blank lines)
while IFS='=' read -r key value; do
  [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
  export "$key=$value"
done < "$PROPS_FILE"

ROOT="$(dirname "$0")/.."

# Patch agent files — replace <PLACEHOLDER> style
sed -i '' \
  "s|<AWS_ACCOUNT_ID>|${AWS_ACCOUNT_ID}|g" \
  "$ROOT/.claude/agents/aws-solutions-architect.md" \
  "$ROOT/.claude/agents/devops-engineer.md"

# Patch index.html — replace __PLACEHOLDER__ style
sed -i '' \
  -e "s|__COGNITO_DOMAIN__|${COGNITO_DOMAIN}|g" \
  -e "s|__COGNITO_CLIENT_ID__|${COGNITO_CLIENT_ID}|g" \
  "$ROOT/frontend/src/wasmJsMain/resources/index.html"

echo "Done. Local placeholders replaced. Do NOT commit the patched files."
echo "Run 'git restore' to reset them before committing."
