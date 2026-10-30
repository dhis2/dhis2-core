#!/usr/bin/env bash

set -euo pipefail

CURL_VERSION='8.20.0'
CURL_SHA256_AMD64='3cf20eb1bca2726d74a39fcda2b758a08a23a3dce73ad6f3f468d7e4d49d215b'
CURL_SHA256_ARM64='061b624119f128038bdfa96f975980d6c35ed502130c4825d59b3143efeafa0e'

dest_dir="${1:?Usage: fetch-curl-binary.sh <dest-dir>}"

case "$(uname -m)" in
  x86_64)        curl_arch='x86_64';  sha256="$CURL_SHA256_AMD64" ;;
  aarch64|arm64) curl_arch='aarch64'; sha256="$CURL_SHA256_ARM64" ;;
  *) echo "Unsupported architecture: $(uname -m)"; exit 1 ;;
esac

dest="${dest_dir}/curl"

if [ -f "$dest" ]; then
  echo "curl binary already present at ${dest}, skipping download."
  exit 0
fi

tarball="curl-linux-${curl_arch}-glibc-${CURL_VERSION}.tar.xz"
url="https://github.com/stunnel/static-curl/releases/download/${CURL_VERSION}/${tarball}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

echo "Downloading static curl ${CURL_VERSION} for ${curl_arch} ..."
curl -fsSL -o "${tmp}/${tarball}" "$url"
echo "${sha256}  ${tmp}/${tarball}" | sha256sum -c -

mkdir -p "$dest_dir"
tar -C "$dest_dir" -xJf "${tmp}/${tarball}" curl
chmod +x "$dest"
