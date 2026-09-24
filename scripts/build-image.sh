#!/usr/bin/env bash
# Builds the kabuproxy image from the host's Maven build (no Maven inside Docker, so it also works on a bad connection
# once ~/.m2 is filled). Extra args are passed to `docker compose build`.
set -euo pipefail
cd "$(dirname "$0")/.."

mvn -B -q clean package
rm -rf target/docker-lib
mvn -B -q -f docker/hibernate-downloader/pom.xml dependency:copy-dependencies -DoutputDirectory="$(pwd)/target/docker-lib"
docker compose build "$@" kabuproxy
