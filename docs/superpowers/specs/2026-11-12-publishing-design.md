<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-4.0
SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Publishing Design

## Intent

Every push publishes signed jars to GitHub Packages. The version comes from
the tree's own build, run through JBang. Built so the generic parts can move
to `xenoterracide/github` as a reusable workflow. This is the flow the
project exists to enable.

## Goals

- Every push publishes `git-metadata`, `semver-calculator`, `semver-cli` to
  `https://maven.pkg.github.com/xenoterracide/semver`.
- Every publish is GPG-signed. One code path; local builds never sign.
- Version computed by the tree's own build — catches bugs before they are
  published.
- After publishing, the published artifact is resolved from GitHub Packages
  and run; output must match. Consumability proof on every push.
- Tag pushes also create a GitHub release with the jars attached.

## Non-goals

- Maven Central. Cheap later in Maven: one `distributionManagement` stanza
  plus the existing sign profile, landed once in the shared workflow.
  JReleaser stays out unless it earns a place (it failed on version catalogs;
  nothing here needs orchestrating).
- `${revision}` / flatten-maven-plugin. `versions:set` writes literal poms.
- Snapshot handling.

## Approaches Considered

- **Self-host via JBang range** (resolve the published CLI, like
  gradle-semver's plugin self-host) — rejected. A range resolves the highest
  version across all repos, so the stale published artifact wins over the
  tree. The Gradle `-1` bootstrap was a hack for this same problem.
- **Version from the tree via local install** — picked. `mvn install` puts
  `semver-cli` at `0.0.0` in `~/.m2`; `main.java` pins `0.0.0` and lists
  `mavenlocal`, so JBang runs the tree's code. Exactly what `main.java` and
  the current `jbang` CI job already do.
- **JReleaser** — rejected for now. One destination, plain jars.
- **flatten-maven-plugin / `${revision}`** — rejected. Verified empirically:
  `versions:set` accepts `+` build-metadata versions
  (`0.0.1-alpha.0.5+git.5.abc123`, plugin 2.25.1, all four poms updated).
  `versions:set` writes literal poms, so consumers never resolve a property.

## Prerequisites: Version Correctness Fixes

Simulating the CI publish checkout against the current code exposed four
inherited bugs. All four are in the migration lineage from gradle-semver and
must land before the publish workflow, or it publishes wrong versions.

1. **`tag()` misses lightweight tags.** It calls jgit `describe()` without
   `setTags(true)`; `Describer` (the distance path) has it. A human running
   `git tag v1.0.0` gets the no-tag strategy — `0.0.1-alpha...` on a release.
   gradle-semver has the identical bug and survived only because every tag it
   ever published was annotated (`gh release create` / `git tag -a`). Our
   fixtures never caught it because jgit's `TagCommand` creates annotated
   tags by default. Fix: add `setTags(true)` to `tag()`.
2. **`isHeadBranch` lost the no-remote-HEAD fallback.** gradle-semver's
   `SemverExtension` documented: when `origin/HEAD` is absent, treat the
   current branch as the HEAD branch. The port kept only the strict equality,
   so any repo without `origin/HEAD` (a plain local clone) always gets
   topic-branch semantics — `+branch.main...` while on `main`.
3. **Detached HEAD puts the full SHA in `branch()`.** A tag-push checkout
   (`ref: github.ref_name`) is always detached, so a release would publish as
   `1.0.0+branch.<40-hex>.git.0.<short>`. Fix: `branch()` returns null when
   HEAD is detached (same `refs/heads` guard `distanceFromMergeBase` uses);
   a null branch or null remote HEAD means HEAD-branch semantics. Tag pushes
   then produce the clean `1.0.0` the gradle-semver portal shows.
4. **Shallow detection is a heuristic that lies both ways.** The
   `shortCount < 4` warning false-positives on small repos and
   false-negatives on `fetch-depth: 10`. Git already marks shallow clones
   with a non-empty `.git/shallow` file — detect that instead, expose
   `GitMetadata.isShallowClone()`, wire it into `GitContext.isShallowClone`
   (hardcoded `false` since the port; gradle-semver left it TODO), and fail
   fast: a version computed from a shallow clone looks plausible and is
   wrong. The publish workflow's `fetch-depth: 0` means CI never trips it.
   GitHub's default checkout is shallow (`fetch-depth: 1`) — the
   gradle-semver publish job grew `fetch-depth: 0` + `fetch-tags: true`
   (b609d50) for exactly this reason. The recovery command is
   `git fetch --all --unshallow`; in GitHub Actions, use `fetch-depth: 0`.

## Constraints

- JBang `--repos` adds repositories; it cannot replace them. Only the `0.0.0`
  pin forces local resolution — keep it.
- GHP stores `.asc` files; resolution ignores them. Signing is uniform
  anyway: one path, ready for Central.
- A 409 on publish means semver produced a duplicate version — a bug,
  surfacing as a failed build. Re-running a publish on the same commit also
  409s; accepted.
- Existing secrets work as-is: `GPG_SECRET_KEY`, `GPG_PASSPHRASE`.
- Signing must not run on local builds (the Gradle pain). The sign profile
  activates only when CI invokes it.
- Agent token may lack `workflow` scope for `build.yml`; if the push is
  rejected, the workflow edit is human-required.

## Flow

`publish` job in `build.yml` (`needs: build`, `packages: write`,
concurrency-serialized):

1. Full clone (`fetch-depth: 0`, `fetch-tags: true`),
   `git remote set-head --auto origin`.
2. Write `~/.m2/settings.xml`: server `gh` with `github.actor` /
   `github.token`.
3. Import the GPG key from `GPG_SECRET_KEY`.
4. `./mvnw -DskipTests -DskipITs install` — the tree's CLI lands at `0.0.0`
   in `~/.m2`.
5. `VERSION=$(jbang main.java)` — `main.java` unchanged: pin `0.0.0`, repos
   `mavencentral,mavenlocal`. Runs the tree's code.
6. `./mvnw versions:set -DnewVersion="$VERSION" -DprocessAllModules
-DgenerateBackupPoms=false`.
7. `./mvnw -DskipTests -DskipITs -Psign deploy`.
8. Post-publish, same job — resolve and run what was just deployed:
   - `jbang com.xenoterracide:semver-cli:"$VERSION"` — output must equal
     `VERSION`.
   - `jbang com.xenoterracide:semver-cli:0+` — latest-resolution check (the
     metadata consumers rely on); output must equal `VERSION`.
9. Tag pushes only: `gh release create --verify-tag --generate-notes` with
   the module jars attached.

The `jbang` job is absorbed by step 8 — delete it.

Self-sufficient from the first push: the version comes from the local
install, and the post-publish checks resolve artifacts this same job
deployed. No bootstrap step.

## Follow-ups

1. Extract the generic parts (settings.xml, sign, `versions:set`, deploy,
   release, post-publish check) into a reusable workflow in
   `xenoterracide/github`, with the version command as an input. Default:
   the `semver-cli` GAV range — how every other repo gets its version. This
   repo passes the local-install dogfood variant.
2. `java-commons` adopts the shared workflow.
3. `spring-app-commons` adopts it when it moves to Maven (Gradle today).
4. Central, if wanted: one `distributionManagement` stanza in the shared
   workflow; signing is already uniform.

## Left to the Builder

- `versions:set` must accept `+` build-metadata versions — verified working
  (2.25.1); quote `-DnewVersion="$VERSION"` in every shell step.
- `Main-Class` manifest on `semver-cli` (maven-jar-plugin) so the GAV runs
  bare; else pass `--main`.
- GHP auth for the GAV runs: named repo `gh=…` must match the settings.xml
  server id — verify JBang honors it.
- Sign profile activation: `-Psign` passed only by CI, never locally, so no
  passphrase prompts on local builds. maven-gpg-plugin needs
  `--pinentry-mode loopback` for headless signing.
- versions-maven-plugin pin in `pluginManagement`.
- Release asset globbing.
