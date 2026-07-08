#!/usr/bin/env bash
set -euo pipefail

# Publishes a built plugin jar as a new public version of the Atlassian
# Marketplace listing, carrying the listing content over from the latest
# published version.
# https://developer.atlassian.com/platform/marketplace/listing-an-app-version-using-rest/
#
# Required environment:
#   MARKETPLACE_EMAIL  Atlassian account email of the vendor contact
#   MARKETPLACE_TOKEN  API token from https://id.atlassian.com/manage-profile/security/api-tokens
#   VERSION            version being released, e.g. 1.22.0
#   ARTIFACT           path to the plugin jar
#   RELEASE_URL        GitHub release page linked from the Marketplace release notes
# Optional:
#   DRY_RUN            when "true", upload the artifact and print the version
#                      payload, but do not create the version

APP_KEY="io.qameta.allure.allure-bamboo"
MARKETPLACE_URL="${MARKETPLACE_URL:-https://marketplace.atlassian.com}"

for name in MARKETPLACE_EMAIL MARKETPLACE_TOKEN VERSION ARTIFACT RELEASE_URL; do
  if [[ -z "${!name:-}" ]]; then
    echo "error: $name is not set" >&2
    exit 1
  fi
done

if [[ ! -f "$ARTIFACT" ]]; then
  echo "error: artifact not found: $ARTIFACT" >&2
  exit 1
fi

# On failure the Marketplace API explains itself in the response body, which
# curl --fail would discard, so check the status code by hand.
api() {
  local out status
  out=$(mktemp)
  status=$(curl -sS -o "$out" -w '%{http_code}' \
    --user "${MARKETPLACE_EMAIL}:${MARKETPLACE_TOKEN}" "$@")
  if [[ "$status" != 2* ]]; then
    echo "error: HTTP $status from ${*: -1}" >&2
    cat "$out" >&2
    rm -f "$out"
    return 1
  fi
  cat "$out"
  rm -f "$out"
}

echo "Uploading $(basename "$ARTIFACT") ..."
artifact_href=$(api -H 'Content-Type: application/octet-stream' \
  --data-binary "@${ARTIFACT}" \
  "${MARKETPLACE_URL}/rest/2/assets/artifact?file=$(basename "$ARTIFACT")" \
  | jq -re '._links.self.href')
echo "Uploaded artifact: ${artifact_href}"

# buildNumber is omitted so Marketplace assigns the next one, and
# compatibilities are omitted so the previous version's range is reused.
payload=$(curl -fsS "${MARKETPLACE_URL}/rest/2/addons/${APP_KEY}/versions/latest" | jq \
  --arg version "$VERSION" \
  --arg artifactHref "$artifact_href" \
  --arg date "$(date -u +%Y-%m-%d)" \
  --arg releaseUrl "$RELEASE_URL" \
  '{
    _links: ({artifact: {href: $artifactHref}}
      + (if ._links.license.href then {license: {href: ._links.license.href}} else {} end)),
    _embedded: {
      highlights: [(._embedded.highlights // [])[]
        | {_links: (._links | with_entries(.value |= {href})), title, body, explanation}],
      screenshots: [(._embedded.screenshots // [])[]
        | {_links: (._links | with_entries(.value |= {href})), caption}]
    },
    name: $version,
    status: "public",
    paymentModel: .paymentModel,
    release: {date: $date, beta: false, supported: true},
    vendorLinks: (.vendorLinks // {}),
    text: {
      moreDetails: .text.moreDetails,
      releaseSummary: "See the GitHub release notes for details.",
      releaseNotes: "<p>See the <a href=\"\($releaseUrl)\">GitHub release notes</a>.</p>"
    }
  } | del(.. | select(. == null))')

if [[ "${DRY_RUN:-}" == "true" ]]; then
  echo "DRY_RUN: would create version ${VERSION} with payload:"
  echo "$payload"
  exit 0
fi

echo "Creating version ${VERSION} ..."
api -H 'Content-Type: application/json' \
  --data-binary "$payload" \
  "${MARKETPLACE_URL}/rest/2/addons/${APP_KEY}/versions" \
  | jq '{name, buildNumber, status}'

echo "Published ${APP_KEY} ${VERSION} to Atlassian Marketplace."
