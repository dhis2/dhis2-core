#!/usr/bin/env bash
# AI Assisted
# @author Morten Svanæs <msvanaes@dhis2.org>
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
export DHIS2_IMAGE="${DHIS2_IMAGE:-dhis2/core-dev:esignet-e2e}"
# Unique project avoids touching any other developer's containers or volumes.
project="dhis2-esignet-$(date +%s)-$$"
compose=(docker compose -p "$project" -f docker-compose.e2e-esignet.yml)
mkdir -p "$PWD/target/esignet"
output="$(mktemp -d "$PWD/target/esignet/run.XXXXXX")"
export ESIGNET_RUN_DIR="$output"
mkdir -p "$output/runtime" "$output/reports"
echo "Compose project: $project; evidence: $output"
cleanup() {
  status=$?
  trap - EXIT
  "${compose[@]}" logs --no-color > "$output/compose.log" 2>&1 || true
  "${compose[@]}" down --volumes --remove-orphans || status=1
  rm -f "$output/runtime/client.p12"
  echo "eSignet E2E exit=$status; evidence: $output"
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# Build this checkout, never a possibly stale core-pr tag. Opt out only with an explicitly built image.
if [[ "${ESIGNET_SKIP_BUILD:-false}" != true ]]; then
  mvn package --threads "${MAVEN_THREADS:-2}" --batch-mode --no-transfer-progress \
    -DskipTests -Dpackaging.type=jar -Dmdep.analyze.skip --file ../pom.xml \
    --projects dhis-web-server --also-make --activate-profiles embedded,jibDockerBuild \
    -Djib.to.image="$DHIS2_IMAGE" 2>&1 | tee "$output/build.log"
fi
upstream=https://raw.githubusercontent.com/mosip/esignet/db528423dac7898ad546cb7b2f5f867a949676a7/docker-compose
curl --fail --location --retry 3 "$upstream/init.sql" -o "$output/runtime/init.sql"
curl --fail --location --retry 3 "$upstream/nginx.conf" -o "$output/runtime/nginx.conf"
printf '%s  %s\n' \
  1823da061189046dd6329c87576426d39c268118b9aa90f8d3498f9d9276e1a9 "$output/runtime/init.sql" \
  c0e82870b0fcb8adb7f53ece43ed7951b64bbd60ccdf82021c01aeb7a4468084 "$output/runtime/nginx.conf" | sha256sum --check
keytool -genkeypair -alias esignet -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
  -validity 2 -dname 'CN=DHIS2 eSignet E2E' -storetype PKCS12 \
  -keystore "$output/runtime/client.p12" -storepass e2e-password -keypass e2e-password -noprompt
"${compose[@]}" up --build --abort-on-container-exit --exit-code-from test 2>&1 | tee "$output/run.log"
