#!/usr/bin/env bash
set -euo pipefail

# Build, sign, tag and publish a Cassetto release APK to GitHub Releases.
#
# Usage: scripts/publish-release.sh [--dry-run] [version]
#   version is M.m or M.m.p; omit it to auto-bump the current versionName.
#   First release: ./scripts/publish-release.sh 0.1.0
#
# Prerequisites (outside the repo):
#   ~/keystores/cassetto.properties   storeFile/storePassword/keyAlias/keyPassword
#   ~/.config/cassetto/github.token   GitHub PAT with repo access

# ── helpers ─────────────────────────────────────────────────────────────
die() { echo "ERROR: $*" >&2; exit 1; }
warn() { echo "WARNING: $*" >&2; }
info() { echo "• $*"; }

# ── global temp dir (cleaned up on exit) ────────────────────────────────
SCRIPT_TEMP="$(mktemp -d)" || die "Cannot create temporary directory"
trap 'rm -rf "$SCRIPT_TEMP"' EXIT

# ── parse arguments ─────────────────────────────────────────────────────
dry_run=false
version_arg=""
args=()
for a in "$@"; do
    case "$a" in
        --dry-run) dry_run=true ;;
        *)         args+=("$a") ;;
    esac
done
if [[ ${#args[@]} -gt 1 ]]; then
    die "Usage: $0 [--dry-run] [version]"
fi
version_arg="${args[0]:-}"

# ── resolve repo root ───────────────────────────────────────────────────
cd "$(git rev-parse --show-toplevel)" || die "Not inside a git repository"

REPO_ROOT="$(pwd)"
info "Repository root: $REPO_ROOT"

# ── current branch check ────────────────────────────────────────────────
current_branch="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$current_branch" != "master" ]]; then
    die "Current branch is '$current_branch', not 'master'. Switch to master first."
fi
info "Branch: master ✓"

# ── working tree check ──────────────────────────────────────────────────
dirty=false
if [[ -n "$(git status --porcelain)" ]]; then
    dirty=true
    if $dry_run; then
        warn "Working tree is dirty — real run requires a clean tree."
    else
        die "Working tree is dirty. Commit or stash changes first."
    fi
fi
if ! $dirty; then
    info "Working tree: clean ✓"
fi

# ── token file check ────────────────────────────────────────────────────
token_file="${CASSETTO_GITHUB_TOKEN_FILE:-$HOME/.config/cassetto/github.token}"
if [[ ! -f "$token_file" || ! -s "$token_file" ]]; then
    fallback_token="$HOME/.config/gainsgoblin/github.token"
    if [[ -f "$fallback_token" && -s "$fallback_token" ]]; then
        token_file="$fallback_token"
        warn "Cassetto token file missing; using fallback $token_file"
    fi
fi
if [[ -f "$token_file" && -s "$token_file" ]]; then
    token="$(tr -d '[:space:]' < "$token_file")"
    if [[ -z "$token" ]]; then
        if $dry_run; then
            warn "Token file $token_file is empty — real run will fail."
        else
            die "Token file $token_file is empty."
        fi
    else
        info "GitHub token: $token_file (non-empty) ✓"
    fi
else
    if $dry_run; then
        warn "Token file $token_file not found or empty — real run will fail."
    else
        die "Token file $token_file not found or empty. Please create ~/.config/cassetto/github.token with a GitHub Personal Access Token."
    fi
fi

# ── keystore props check ────────────────────────────────────────────────
keystore_props="$HOME/keystores/cassetto.properties"
if [[ -f "$keystore_props" ]]; then
    info "Keystore props: $keystore_props ✓"
else
    if $dry_run; then
        warn "Keystore props $keystore_props not found — real run will fail."
    else
        die "Keystore props $keystore_props not found."
    fi
fi

# ── version math ────────────────────────────────────────────────────────
current_version="$(
    sed -n 's/.*versionName "\([^"]*\)".*/\1/p' app/build.gradle | head -n1
)"
if [[ -z "$current_version" ]]; then
    die "Could not parse versionName from app/build.gradle"
fi

info "Current versionName: $current_version"

parse_version_parts() {
    local v="$1"
    local IFS='.'
    read -ra parts <<< "$v"
    if [[ ${#parts[@]} -lt 2 || ${#parts[@]} -gt 3 ]]; then
        return 1
    fi
    for p in "${parts[@]}"; do
        if ! [[ "$p" =~ ^[0-9]+$ ]]; then
            return 1
        fi
    done
    echo "${parts[@]}"
}

if [[ -n "$version_arg" ]]; then
    # validate explicit version
    if ! [[ "$version_arg" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
        die "Invalid version '$version_arg'. Expected format: M.m or M.m.p (e.g. 0.01 or 1.0.14)"
    fi
    new_version="$version_arg"
else
    # auto-bump
    parts_str="$(parse_version_parts "$current_version")" || die "Unparseable versionName '$current_version' — expected 2 or 3 dot-separated numeric components"
    read -ra parts <<< "$parts_str"
    if [[ ${#parts[@]} -eq 2 ]]; then
        new_version="${parts[0]}.$((parts[1] + 1))"
    else
        new_version="${parts[0]}.${parts[1]}.$((parts[2] + 1))"
    fi
fi

# parse new version parts for versionCode computation
parts_str="$(parse_version_parts "$new_version")" || die "Internal error: unparseable new version '$new_version'"
read -ra new_parts <<< "$parts_str"
major="${new_parts[0]}"
minor="${new_parts[1]}"
patch="${new_parts[2]:-}"

if [[ -z "$patch" ]]; then
    version_code=$(( major * 100 + minor ))
else
    version_code=$(( major * 10000 + minor * 100 + patch ))
fi

info "New version:  $new_version"
info "VersionCode:  $version_code"

# ── resolv apksigner path ───────────────────────────────────────────────
find_apksigner() {
    # 1) via ANDROID_HOME
    if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME/build-tools" ]]; then
        local candidate
        candidate="$(find "$ANDROID_HOME/build-tools" -name apksigner -type f 2>/dev/null | sort -V | tail -1)"
        if [[ -n "$candidate" ]]; then
            echo "$candidate"
            return 0
        fi
    fi
    # 2) hardcoded default
    local default="/home/b/Android/Sdk/build-tools/37.0.0/apksigner"
    if [[ -x "$default" ]]; then
        echo "$default"
        return 0
    fi
    # 3) fallback search
    local fb
    fb="$(find /home/b/Android/Sdk/build-tools -name apksigner -type f 2>/dev/null | sort -V | tail -1)"
    if [[ -n "$fb" ]]; then
        echo "$fb"
        return 0
    fi
    return 1
}

APKSIGNER="$(find_apksigner)" || die "apksigner not found anywhere (tried ANDROID_HOME, default paths)"
info "apksigner: $APKSIGNER ✓"

# ── resolve <since> for release notes ───────────────────────────────────
resolve_since() {
    local since
    # First try a v*-prefixed tag
    since="$(git describe --tags --match 'v*' --abbrev=0 2>/dev/null)" && { echo "$since"; return 0; }
    # Fall back to merge-base with upstream/master
    if git rev-parse --verify upstream/master &>/dev/null; then
        since="$(git merge-base HEAD upstream/master 2>/dev/null)"
        if [[ -n "$since" ]]; then
            echo "$since"
            return 0
        fi
    fi
    # First release: no v* tag and no upstream remote yet.
    since="$(git rev-list --max-parents=0 HEAD 2>/dev/null | tail -n1)"
    if [[ -n "$since" ]]; then
        echo "$since"
        return 0
    fi
    # No fallback works
    return 1
}

since_ref="$(resolve_since)" || die "No previous release tag (v*) and no upstream/master remote to derive fork point. Either tag a baseline or pass explicit notes."
info "Release notes since: ${since_ref:0:10}..."

# ── build release notes ─────────────────────────────────────────────────
build_release_notes() {
    local since="$1" ver="$2" notes_dir="$SCRIPT_TEMP/release-notes"
    mkdir -p "$notes_dir"
    local notes_file="$notes_dir/release-notes.md"

    # Gather commits
    local commits
    commits="$(git log "$since..HEAD" --no-merges --pretty=format:'%H%x09%s' 2>/dev/null)" || {
        rm -rf "$notes_dir"
        die "Failed to list commits since $since"
    }

    if [[ -z "$commits" ]]; then
        echo "No commits since previous release." > "$notes_file"
        echo "$notes_file"
        return 0
    fi

    # Temporary files for groups (all under notes_dir, cleaned via SCRIPT_TEMP)
    local feat_file="$notes_dir/feat.txt"
    local fix_file="$notes_dir/fix.txt"
    local perf_file="$notes_dir/perf.txt"
    local docs_file="$notes_dir/docs.txt"
    local refactor_file="$notes_dir/refactor.txt"
    local chore_file="$notes_dir/chore.txt"
    local other_file="$notes_dir/other.txt"

    rm -f "$feat_file" "$fix_file" "$perf_file" "$docs_file" "$refactor_file" "$chore_file" "$other_file"

    local re_feat='^(feat|ui|reader|auth|lists|api|data)(\([^)]*\))?:'
    local re_fix='^fix(\([^)]*\))?:'
    local re_perf='^perf(\([^)]*\))?:'
    local re_docs='^docs(\([^)]*\))?:'
    local re_refactor='^refactor(\([^)]*\))?:'
    local re_chore='^(chore|build|ci)(\([^)]*\))?:'

    while IFS=$'\t' read -r hash subject; do
        [[ -z "$hash" ]] && continue
        local short_hash="${hash:0:7}"
        local subject_lower="${subject,,}"

        if [[ "$subject_lower" =~ $re_feat ]]; then
            echo "- $subject ($short_hash)" >> "$feat_file"
        elif [[ "$subject_lower" =~ $re_fix ]]; then
            echo "- $subject ($short_hash)" >> "$fix_file"
        elif [[ "$subject_lower" =~ $re_perf ]]; then
            echo "- $subject ($short_hash)" >> "$perf_file"
        elif [[ "$subject_lower" =~ $re_docs ]]; then
            echo "- $subject ($short_hash)" >> "$docs_file"
        elif [[ "$subject_lower" =~ $re_refactor ]]; then
            echo "- $subject ($short_hash)" >> "$refactor_file"
        elif [[ "$subject_lower" =~ $re_chore ]]; then
            echo "- $subject ($short_hash)" >> "$chore_file"
        else
            echo "- $subject ($short_hash)" >> "$other_file"
        fi
    done <<< "$commits"

    {
        echo "## What's changed"

        has_content=

        if [[ -s "$feat_file" ]]; then
            echo ""; echo "### Features"
            cat "$feat_file"; has_content=true
        fi
        if [[ -s "$fix_file" ]]; then
            echo ""; echo "### Fixes"
            cat "$fix_file"; has_content=true
        fi
        if [[ -s "$perf_file" ]]; then
            echo ""; echo "### Performance"
            cat "$perf_file"; has_content=true
        fi
        if [[ -s "$docs_file" ]]; then
            echo ""; echo "### Documentation"
            cat "$docs_file"; has_content=true
        fi
        if [[ -s "$refactor_file" ]]; then
            echo ""; echo "### Refactoring"
            cat "$refactor_file"; has_content=true
        fi
        if [[ -s "$chore_file" ]]; then
            echo ""; echo "### Chores"
            cat "$chore_file"; has_content=true
        fi
        if [[ -s "$other_file" ]]; then
            echo ""; echo "### Other"
            cat "$other_file"; has_content=true
        fi

        if [[ -z "${has_content}" ]] && [[ ! -s "$feat_file" && ! -s "$fix_file" && ! -s "$perf_file" && ! -s "$docs_file" && ! -s "$refactor_file" && ! -s "$chore_file" && ! -s "$other_file" ]]; then
            echo ""; echo "*No changes detected*"
        fi

        echo ""
        echo "---"
        echo ""
        echo "Full changelog: https://github.com/READYPLAYERAI/cassetto/compare/${since}...v${ver}"
    } > "$notes_file"

    echo "$notes_file"
}

notes_file="$(build_release_notes "$since_ref" "$new_version")"
info "Release notes: $(wc -l < "$notes_file") lines"

# ── dry-run summary and exit ────────────────────────────────────────────
if $dry_run; then
    echo ""
    echo "═══════════════════════════════════════════════════════════════════"
    echo "                        DRY RUN SUMMARY"
    echo "═══════════════════════════════════════════════════════════════════"
    echo ""
    echo "Planned tag:           v${new_version}"
    echo "Planned release title: Cassetto v${new_version}"
    echo "Planned asset:         Cassetto-v${new_version}.apk"
    echo "Version code:          ${version_code}"
    echo "Release notes since:   ${since_ref}"
    echo ""
    echo "Preconditions:"
    if $dirty;  then echo "  • Working tree:   ⚠️  DIRTY (real run requires clean)"; else echo "  • Working tree:   ✓ CLEAN"; fi
    if [[ -f "$keystore_props" ]]; then echo "  • Keystore:       ✓ FOUND"; else echo "  • Keystore:       ✗ MISSING"; fi
    if [[ -f "$token_file" && -s "$token_file" ]]; then echo "  • GitHub token:   ✓ FOUND"; else echo "  • GitHub token:   ✗ MISSING"; fi
    echo ""
    echo "Would run:"
    echo "  1. git add app/build.gradle && git commit -m \"Release v${new_version}\""
    echo "  2. ./gradlew :app:assembleRelease"
    echo "  3. apksigner verify ... app-release.apk"
    echo "  4. cp app-release.apk → (temp)/Cassetto-v${new_version}.apk"
    echo "  5. git tag -a v${new_version} -m \"Cassetto v${new_version}\""
    echo "  6. git push origin master && git push origin v${new_version}"
    echo "  7. Create GitHub Release via API"
    echo "  8. Upload APK asset via API"
    echo ""
    echo "Release notes preview:"
    echo "───────────────────────────────────────────────────────────────────"
    cat "$notes_file"
    echo "───────────────────────────────────────────────────────────────────"
    echo ""
    echo "DRY RUN — no changes made."
    exit 0
fi

# ═══════════════════════════════════════════════════════════════════════
#  REAL RUN
# ═══════════════════════════════════════════════════════════════════════

info "══ Starting real release v${new_version} ══"

EXPECTED_CERT_CN="${EXPECTED_CERT_CN:-Gains Goblin}"

# 1. Commit version bump ────────────────────────────────────────────────
sed -i "s/^\(.*versionCode \)[0-9]\+/\1${version_code}/" app/build.gradle
sed -i 's/^\(.*versionName "\)[^"]*\(".*\)/\1'"${new_version}"'\2/' app/build.gradle

git add app/build.gradle
git commit -m "Release v${new_version}"
info "Committed version bump → v${new_version} (code ${version_code})"

# 2. Build release APK ───────────────────────────────────────────────────
info "Running './gradlew :app:assembleRelease'..."
if ! ./gradlew :app:assembleRelease 2>&1; then
    die "Gradle build failed. See output above."
fi
info "Build successful."

# 3. Signature gate ──────────────────────────────────────────────────────
APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK_PATH" ]]; then
    die "APK not found at $APK_PATH — build may have failed or path changed."
fi

verify_output="$("$APKSIGNER" verify --print-certs "$APK_PATH" 2>&1)" || die "apksigner verification FAILED for $APK_PATH. Output:\n$verify_output"
if ! grep -q "CN=${EXPECTED_CERT_CN}" <<< "$verify_output"; then
    die "apksigner verification passed but certificate does NOT contain 'CN=${EXPECTED_CERT_CN}'.\nOutput:\n$verify_output"
fi
info "APK signature verified (CN=${EXPECTED_CERT_CN}) ✓"

# 4. Stage artifact for upload ────────────────────────────────────────────
STAGING_DIR="$SCRIPT_TEMP/artifact"
mkdir -p "$STAGING_DIR"
ASSET_NAME="Cassetto-v${new_version}.apk"
cp "$APK_PATH" "$STAGING_DIR/$ASSET_NAME"
info "Artifact staged: $STAGING_DIR/$ASSET_NAME"

# 5. Tag + push ──────────────────────────────────────────────────────────
git tag -a "v${new_version}" -m "Cassetto v${new_version}"
info "Tagged v${new_version}"

git push origin master
info "Pushed master"

git push origin "v${new_version}"
info "Pushed tag v${new_version}"

# 6. Create GitHub Release ───────────────────────────────────────────────
GITHUB_API="https://api.github.com/repos/READYPLAYERAI/cassetto/releases"
UPLOAD_API="https://uploads.github.com/repos/READYPLAYERAI/cassetto/releases"

json_payload="$(
    python3 -c '
import json, sys
tag, title, notesfile = sys.argv[1], sys.argv[2], sys.argv[3]
with open(notesfile) as f:
    notes = f.read()
print(json.dumps({"tag_name": tag, "name": title, "body": notes}))
' "v${new_version}" "Cassetto v${new_version}" "$notes_file"
)"

info "Creating GitHub Release..."
set +e
release_response="$(curl --fail-with-body -sS -X POST \
    -H "Authorization: Bearer $token" \
    -H "Accept: application/vnd.github+json" \
    -w '\n%{http_code}' \
    "$GITHUB_API" \
    -d "$json_payload" 2>&1)"
curl_rc=$?
set -e

http_code="$(tail -1 <<< "$release_response")"
release_body="$(sed '$d' <<< "$release_response")"

if [[ $curl_rc -ne 0 || "$http_code" != 2?? ]]; then
    die "GitHub Release creation failed (HTTP $http_code, curl exit $curl_rc). Response:\n$release_body"
fi

release_id="$(python3 -c "import json,sys; data=json.load(sys.stdin); print(data.get('id',''))" <<< "$release_body")"
release_url="$(python3 -c "import json,sys; data=json.load(sys.stdin); print(data.get('html_url',''))" <<< "$release_body")"

if [[ -z "$release_id" || "$release_id" == "None" ]]; then
    die "GitHub Release creation succeeded but no 'id' in response. Response:\n$release_body"
fi
if python3 -c "import json,sys; data=json.load(sys.stdin); sys.exit(0 if 'message' in data else 1)" <<< "$release_body" 2>/dev/null; then
    err_msg="$(python3 -c "import json,sys; data=json.load(sys.stdin); print(data['message'])" <<< "$release_body")"
    die "GitHub API returned an error message: $err_msg\nFull response:\n$release_body"
fi

info "GitHub Release created: ${release_url:-<no URL>} (ID: $release_id)"

# 7. Upload APK asset ────────────────────────────────────────────────────
info "Uploading APK asset..."
set +e
upload_response="$(curl --fail-with-body -sS -X POST \
    -H "Authorization: Bearer $token" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/vnd.android.package-archive" \
    --data-binary @"$STAGING_DIR/$ASSET_NAME" \
    -w '\n%{http_code}' \
    "${UPLOAD_API}/${release_id}/assets?name=${ASSET_NAME}" 2>&1)"
curl_rc=$?
set -e

upload_http_code="$(tail -1 <<< "$upload_response")"
upload_body="$(sed '$d' <<< "$upload_response")"

if [[ $curl_rc -ne 0 || "$upload_http_code" != "201" ]]; then
    die "APK upload failed (HTTP $upload_http_code, curl exit $curl_rc). Response:\n$upload_body"
fi

upload_state="$(python3 -c "import json,sys; data=json.load(sys.stdin); print(data.get('state',''))" <<< "$upload_body")"
if [[ "$upload_state" != "uploaded" ]]; then
    die "APK upload completed but state is '$upload_state' (expected 'uploaded'). Response:\n$upload_body"
fi

info "APK asset uploaded successfully ✓"

# 8. Done ────────────────────────────────────────────────────────────────
echo ""
echo "RELEASE URL: ${release_url}"
echo "Done."