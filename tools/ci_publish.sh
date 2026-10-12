#!/usr/bin/env bash
# CI helper: publishes a directory into a sub-folder of the `ci-artifacts` branch (created on first use), so
# API dumps and in-game screenshots can be fetched with plain git. Needs GITHUB_TOKEN with contents: write.
# Usage: tools/ci_publish.sh <source-dir> <destination-subfolder>
set -euo pipefail
src="$(cd "$1" && pwd)"
dest="$2"
url="https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
for attempt in 1 2 3; do
  work="$(mktemp -d)"
  if git ls-remote --exit-code --heads "$url" ci-artifacts > /dev/null 2>&1; then
    git clone -q --depth 1 --branch ci-artifacts "$url" "$work"
  else
    git init -q "$work"
    git -C "$work" checkout -q -b ci-artifacts
  fi
  rm -rf "${work:?}/$dest"
  mkdir -p "$work/$dest"
  cp -r "$src"/. "$work/$dest"/
  git -C "$work" config user.name "github-actions[bot]"
  git -C "$work" config user.email "41898282+github-actions[bot]@users.noreply.github.com"
  git -C "$work" add -A
  if git -C "$work" diff --cached --quiet; then
    echo "nothing new to publish"
    exit 0
  fi
  git -C "$work" commit -q -m "CI artifacts: $dest from ${GITHUB_SHA:0:7}"
  if git -C "$work" push -q "$url" HEAD:ci-artifacts; then
    echo "published $dest to ci-artifacts"
    exit 0
  fi
  echo "push failed (attempt $attempt), retrying"
  sleep $((attempt * 5))
done
exit 1
