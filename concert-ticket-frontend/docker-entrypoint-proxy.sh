#!/bin/sh
set -eu

# Validate before envsubst inserts the secret into Nginx configuration.
if [ "${RENDER:-false}" = "true" ]; then
    case "${PROXY_SHARED_SECRET:-}" in
        ''|*[!0-9a-fA-F]*) echo 'PROXY_SHARED_SECRET must be at least 64 hexadecimal characters.' >&2; exit 1 ;;
    esac
    if [ "${#PROXY_SHARED_SECRET}" -lt 64 ]; then
        echo 'PROXY_SHARED_SECRET must be at least 64 hexadecimal characters.' >&2
        exit 1
    fi
else
    export RENDER=false
    export PROXY_SHARED_SECRET=
fi
