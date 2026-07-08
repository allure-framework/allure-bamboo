#!/usr/bin/env bash
set -euo pipefail

# Publishes a built plugin jar as a new public version of the Atlassian
# Marketplace listing, using the Marketplace REST API v3 (the v2 API sunset
# on June 30, 2026). Compatibility range, license, payment model, and the
# listing content all carry over from the latest published version.
# API reference: https://developer.atlassian.com/platform/marketplace/rest/v4/
#
# Flow:
#   1. upload the jar                        POST /rest/3/artifacts
#   2. resolve the app software id           GET  /rest/3/app-software/app-key/{key}
#   3. read the latest version + listing     GET  .../versions, .../versions/{build}/listing
#   4. create the new version                POST .../versions
#   5. publish the new version's listing     POST .../versions/{build}/listing
#
# Required environment:
#   MARKETPLACE_EMAIL  Atlassian account email of the vendor contact
#   MARKETPLACE_TOKEN  API token from https://id.atlassian.com/manage-profile/security/api-tokens
#   VERSION            version being released, e.g. 1.22.0
#   ARTIFACT           path to the plugin jar
#   RELEASE_URL        GitHub release page linked from the Marketplace release notes
# Optional:
#   DRY_RUN            when "true", upload the artifact and print the version and
#                      listing payloads, but do not create or publish anything

APP_KEY="io.qameta.allure.allure-bamboo"
BASE="${MARKETPLACE_BASE:-https://api.atlassian.com/marketplace}/rest/3"
# Accepted on every publish — the same Marketplace Partner Agreement the
# publish dialog in the Marketplace UI asks to confirm.
AGREEMENT_URL="https://www.atlassian.com/licensing/marketplace/partneragreement"

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
upload=$(api -F "file=@${ARTIFACT}" "${BASE}/artifacts")
artifact_id=$(jq -re '._links.self.href | split("/") | last' <<<"$upload")
jar_version=$(jq -r '.details.version // empty' <<<"$upload")
echo "Uploaded artifact ${artifact_id} (plugin version: ${jar_version:-unknown})"

if [[ -n "$jar_version" && "$jar_version" != "$VERSION" ]]; then
  echo "error: jar declares version ${jar_version}, expected ${VERSION}" >&2
  exit 1
fi

app_software_id=$(api "${BASE}/app-software/app-key/${APP_KEY}" \
  | jq -re 'map(select(.hosting == "datacenter")) | first | .appSoftwareId')
echo "App software id: ${app_software_id}"

latest=$(api "${BASE}/app-software/${app_software_id}/versions?limit=50" \
  | jq -e '.versions | max_by(.buildNumber)')
prev_build=$(jq -re '.buildNumber' <<<"$latest")
new_build=$((prev_build + 100))
echo "Latest version: $(jq -r '.versionNumber' <<<"$latest") (build ${prev_build}); new build: ${new_build}"

version_payload=$(jq -e \
  --arg version "$VERSION" \
  --arg artifactId "$artifact_id" \
  --arg agreementUrl "$AGREEMENT_URL" \
  --arg releaseUrl "$RELEASE_URL" \
  --argjson buildNumber "$new_build" \
  '{
    buildNumber: $buildNumber,
    versionNumber: $version,
    compatibilities: [.compatibilities[] | {parentSoftwareId, minBuildNumber, maxBuildNumber}],
    supportedPaymentModel: (.supportedPaymentModel // "free"),
    frameworkDetails: {
      frameworkId: "plugin",
      attributes: {artifactId: $artifactId, pluginFrameworkType: "P2"}
    },
    supported: true,
    beta: false,
    acceptedAgreements: [{agreementUrl: $agreementUrl}],
    changelog: {
      releaseSummary: "See the GitHub release notes for details.",
      releaseNotes: "See the GitHub release notes: \($releaseUrl)"
    }
  }
  + (if .licenseType.id then {licenseType: {id: .licenseType.id}} else {} end)
  | if (.compatibilities | length) == 0
      or any(.compatibilities[]; .parentSoftwareId == null or .minBuildNumber == null or .maxBuildNumber == null)
    then error("cannot carry compatibilities over from the latest version") else . end' \
  <<<"$latest")

prev_listing=$(api "${BASE}/app-software/${app_software_id}/versions/${prev_build}/listing")
listing_payload=$(jq -e '{
    state: "PUBLIC",
    revision: 1,
    screenshots: (if .screenshots then [.screenshots[] | {imageId, caption}] else null end),
    highlights: (if .highlights then [.highlights[]
      | {title, caption, summary, thumbnail, screenshot: (.screenshot | {imageId, caption})}] else null end),
    moreDetails: .moreDetails,
    heroImage: .heroImage,
    youtubeId: .youtubeId,
    deploymentInstructions: (if .deploymentInstructions then [.deploymentInstructions[]
      | {body} + (if .screenshot then {screenshot: (.screenshot | {imageId, caption})} else {} end)] else null end),
    developerLinks: (.developerLinks
      | if . then {documentation, learnMore, eula, purchase, bonTermsSupported, partnerSpecificTerms} else null end)
  } | del(.. | select(. == null))' <<<"$prev_listing")

if [[ "${DRY_RUN:-}" == "true" ]]; then
  echo "DRY_RUN: would create version ${VERSION} (build ${new_build}) with payload:"
  echo "$version_payload"
  echo "DRY_RUN: would publish its listing with payload:"
  echo "$listing_payload"
  exit 0
fi

echo "Creating version ${VERSION} (build ${new_build}) ..."
created=$(api -H 'Content-Type: application/json' \
  --data-binary "$version_payload" \
  "${BASE}/app-software/${app_software_id}/versions")
jq '{versionNumber, buildNumber, state}' <<<"$created"

echo "Publishing the version listing ..."
if ! listing=$(api -H 'Content-Type: application/json' \
  --data-binary "$listing_payload" \
  "${BASE}/app-software/${app_software_id}/versions/${new_build}/listing"); then
  echo "error: version ${VERSION} (build ${new_build}) was created, but publishing its" >&2
  echo "listing failed — finish the release in the Marketplace developer console." >&2
  exit 1
fi
jq '{state, approvalStatus, revision}' <<<"$listing"

echo "Published ${APP_KEY} ${VERSION} to Atlassian Marketplace."
