#!/usr/bin/env bash
set -euo pipefail

source_root=$(cd "${KENT_WORKTREE_SOURCE_WORKSPACE_ROOT:-${1:?missing source workspace}}" && pwd -P)
target_root=$(cd "${KENT_WORKTREE_ROOT:-${3:?missing worktree}}" && pwd -P)
if [[ "$source_root" == "$target_root" ]]; then
    echo "Refusing to provision the source workspace itself." >&2
    exit 1
fi

for path in .java-version local.properties certificates/keystore.jks scripts/composeReport2Html.jar; do
    source_file="$source_root/$path"
    target_file="$target_root/$path"
    [[ -f "$source_file" ]] || continue
    git -C "$target_root" check-ignore -q -- "$path" || continue
    if [[ -L "$target_file" && "$(readlink "$target_file")" == "$source_file" ]]; then
        continue
    fi
    if [[ -e "$target_file" || -L "$target_file" ]]; then
        echo "Refusing to overwrite existing worktree file: $path" >&2
        exit 1
    fi
    mkdir -p "$(dirname "$target_file")"
    ln -s "$source_file" "$target_file"
done
