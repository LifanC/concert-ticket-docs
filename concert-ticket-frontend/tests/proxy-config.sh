#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
sh -n docker-entrypoint-proxy.sh
RENDER=false sh docker-entrypoint-proxy.sh
RENDER=true PROXY_SHARED_SECRET=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa sh docker-entrypoint-proxy.sh
if RENDER=true PROXY_SHARED_SECRET=invalid sh docker-entrypoint-proxy.sh 2>/dev/null; then
    echo 'Invalid secret was accepted' >&2
    exit 1
fi
if RENDER=true PROXY_SHARED_SECRET= sh docker-entrypoint-proxy.sh 2>/dev/null; then
    echo 'Empty secret was accepted' >&2
    exit 1
fi
if RENDER=true PROXY_SHARED_SECRET='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;' sh docker-entrypoint-proxy.sh 2>/dev/null; then
    echo 'Unsafe secret was accepted' >&2
    exit 1
fi
echo 'Proxy startup checks passed'
