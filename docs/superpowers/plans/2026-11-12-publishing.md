<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-4.0
SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Publishing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every push publishes GPG-signed jars to GitHub Packages, with the version computed by this project's own CLI, and tag pushes additionally create GitHub releases.

**Architecture:** Fix four inherited version-calculation bugs first (they produce wrong versions under CI checkouts), then add build artifacts (sources, javadoc, runnable manifest), publish plumbing (sign profile, distributionManagement), and finally a `publish` job in `build.yml` that dogfoods the tree's own CLI for the version and verifies the published artifact after deploy.

**Tech Stack:** Maven 4 wrapper, jgit 7.8.1, JBang, maven-gpg-plugin 3.2.8, maven-source-plugin 3.4.0, versions-maven-plugin 2.25.1, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-12-publishing-design.md`

## Global Constraints

- Quote versions in every shell step: `-DnewVersion="$VERSION"`,
  `"com.xenoterracide:semver-cli:$VERSION"`. A `+` unquoted becomes a glob.
- Maven runs: `-B -q --no-transfer-progress`. Git: `--quiet`, `status --porcelain`. pnpm: `--reporter=silent`.
- Every new or changed file carries its SPDX header; `pnpm lint` (prettier + REUSE) passes before each commit.
- Conventional commits; types from `git-conventional-commits.yaml`.
- Java: main compiles to release 17, tests to 25. Nullable types use JSpecify `@Nullable`.
- No new runtime dependencies. Test deps only where named.
- Targeted test runs use `-pl <module> -am` with
  `-Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false`.
- `./mvnw -B -q --no-transfer-progress verify` and `pnpm --reporter=silent lint` green before every commit.
- Editing `.github/workflows/build.yml` may require a token with `workflow` scope. If the push is rejected for scope, hand that step to the human; do not force.

## Review Focus

Five inputs the spec implies but ordinary happy-path tests miss. Each is pinned by a task below.

1. A human cuts a lightweight tag (`git tag v1.0.0`, not `-a`) — release must still be `1.0.0`. Pinned: Task 1 Step 1, Task 3 Step 5.
2. A tag-push checkout is detached with `origin/HEAD` present — version must be clean `1.0.0`, not `1.0.0+branch.<sha>...`. Pinned: Task 3 Step 3.
3. A contributor's plain local clone has no `origin/HEAD` — HEAD-branch semantics, no `+branch.main...` while on `main`. Pinned: Task 2 Step 1, Task 3 Step 4.
4. A computed version (`0.0.1-alpha.0.5+git.5.abc123`) passes through `versions:set` unquoted — must not glob-expand. Pinned: Task 7 steps quote every use (verified working with plugin 2.25.1).
5. The just-deployed artifact resolves from GitHub Packages — exact version and `0+` latest must both return the deployed version. Pinned: Task 7 Step 12.
6. A shallow clone (`fetch-depth: 1`, GitHub's default checkout) — versions computed from it look plausible and are wrong; must fail loudly, not publish. Pinned: Task 1 Steps 1–3, Task 2 Steps 1–3.

---

### Task 1: git-metadata — lightweight tags, detached HEAD, shallow detection

**Files:**

- Modify: `module/git-metadata/src/main/java/com/xenoterracide/git/GitMetadata.java`
- Modify: `module/git-metadata/src/main/java/com/xenoterracide/git/GitMetadataImpl.java`
- Test: `module/git-metadata/src/test/java/com/xenoterracide/git/GitMetadataImplTest.java`

**Interfaces:**

- Consumes: nothing new.
- Produces: `tag()` returns lightweight `v*` tags; `branch()` returns `null` when HEAD is detached; `boolean GitMetadata.isShallowClone()` (default `false`, overridden by the impl). Task 2's calculator and Task 3's ITs depend on all three.

- [ ] **Step 1: Write failing tests**

Add to `GitMetadataImplTest` (pattern: `@TempDir` + `Git.init()` + `new GitMetadataImpl(() -> git)`):

- `tagFindsLightweightTag`: commit, `git.tag().setName("v1.0.0").setAnnotated(false).call()`, assert `pg.tag()` is `"v1.0.0"`.
- `branchIsNullWhenDetached`: commit on `main`, checkout by commit SHA (`git.checkout().setName(<sha>).call()`), assert `pg.branch()` is null.
- `isShallowCloneFalseForFreshInit`: after init + commit, assert `pg.isShallowClone()` is false.
- `isShallowCloneTrueWhenShallowFileExists`: after init + commit, write the commit SHA into `new File(git.getRepository().getDirectory(), "shallow")`, assert `pg.isShallowClone()` is true. (This is exactly how git records shallowness.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -B -q --no-transfer-progress -pl module/git-metadata -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — `tagFindsLightweightTag` gets `null`; `branchIsNullWhenDetached` gets the 40-char SHA; the `isShallowClone` tests fail to compile (method absent).

- [ ] **Step 3: Implement**

- `GitMetadata`: add `default boolean isShallowClone() { return false; }` (same pattern as `distanceFromMergeBase`).
- `GitMetadataImpl.tag()`: chain `.setTags(true)` onto the existing `describe()` call (mirrors `Describer`).
- `GitMetadataImpl.branch()`: resolve `Repository::getFullBranch`; when it does not start with `Constants.R_HEADS` (detached), return null; otherwise strip the `refs/heads/` prefix. Same guard style as `distanceFromMergeBase()`.
- `GitMetadataImpl.isShallowClone()`: override — `gitRepository().mapTry(r -> { var dir = r.getDirectory(); return dir != null && new File(dir, "shallow").length() > 0; }).getOrElse(false)`. Null `getDirectory()` (in-memory repo) means not shallow.
- `distance()`: delete the `shortCount < 4` warning block, `shortCount()`, and `SHALLOW_CLONE_DETECTED` — `shortCount` has no other callers.

- [ ] **Step 4: Run module tests**

Run: `./mvnw -B -q --no-transfer-progress -pl module/git-metadata -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS.

- [ ] **Step 5: Full verify and commit**

Run: `./mvnw -B -q --no-transfer-progress verify` then `pnpm --reporter=silent lint`. Expected: both green.

```bash
git add module/git-metadata
git commit -m "fix: detect lightweight tags, detached HEAD, and shallow clones"
```

### Task 2: semver-calculator — head-branch fallback and shallow fail-fast

**Files:**

- Modify: `module/semver-calculator/src/main/java/com/xenoterracide/semver/VersionCalculator.java` (`toGitContext` and `calculate`)
- Modify: `module/semver-cli/src/main/java/com/xenoterracide/semver/cli/SemverCommand.java`
- Test: `module/semver-calculator/src/test/java/com/xenoterracide/semver/VersionCalculatorTest.java`
- Test: `module/semver-cli/src/test/java/com/xenoterracide/semver/cli/SemverCommandTest.java` (new)

**Interfaces:**

- Consumes: Task 1's null-branch-on-detached semantics and `isShallowClone()`.
- Produces: `isHeadBranch = branch == null || headBranch == null || Objects.equals(branch, headBranch)` (gradle-semver parity — `SemverExtension`: "When there's no HEAD branch configured, treat current branch as HEAD"; a detached HEAD is not a topic branch). `VersionCalculator.calculate` throws `IllegalStateException` when `metadata.isShallowClone()` — a version from a shallow clone looks plausible and is wrong. The CLI preempts it with a clean error.

- [ ] **Step 1: Write failing tests**

`VersionCalculatorTest`, using the existing `createMetadata(...)` helper (extend the `SimpleGitMetadata` record with an `isShallowClone` component):

- `noRemoteHeadTreatsCurrentBranchAsHead`: `createMetadata("v1.0.0", 0, "main", null, "abc123", GitStatus.CLEAN)` → version `1.0.0`, build metadata empty.
- `detachedHeadOnTagReturnsCleanVersion`: `createMetadata("v1.0.0", 0, null, "develop", "abc123", GitStatus.CLEAN)` → `1.0.0`, build empty.
- `detachedHeadAfterTagUsesHeadStyleMetadata`: `createMetadata("v1.0.0", 2, null, "develop", "abc123", GitStatus.CLEAN)` → `1.0.1-alpha.0.2`, build `git.2.abc123` (no `branch.` prefix).
- `shallowCloneThrows`: metadata with `isShallowClone = true` → `calculate` throws `IllegalStateException` with message containing `shallow`, `git fetch --all --filter=blob:none`, and `fetch-depth: 0`.

`SemverCommandTest` (new, pattern: `@TempDir` + `Git.init()` + commit, then `new CommandLine(new SemverCommand()).execute("--path", dir)`):

- `shallowClonePrintsErrorAndReturnsOne`: write the commit SHA into `<gitdir>/shallow`, execute returns `1`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -B -q --no-transfer-progress -pl module/semver-calculator,module/semver-cli -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — the three branch tests get topic-branch output (`+branch.main...` etc.); `shallowCloneThrows` and the CLI test fail (no such behavior).

- [ ] **Step 3: Implement**

- `toGitContext`: `var isHeadBranch = branch == null || headBranch == null || Objects.equals(branch, headBranch);` and `.isShallowClone(metadata.isShallowClone())` replacing the hardcoded `false`.
- `calculate`: after the null check, `if (metadata.isShallowClone()) { throw new IllegalStateException("cannot calculate version from a shallow clone; run: git fetch --all --filter=blob:none, or set fetch-depth: 0 on actions/checkout"); }` — both remedies as documented in gradle-semver's README FAQ.
- `SemverCommand.call()`: after the NO_REPO check, `if (metadata.isShallowClone()) { System.err.println("Error: shallow clone; run: git fetch --all --filter=blob:none, or set fetch-depth: 0 on actions/checkout"); return 1; }` — keeps the existing error-reporting pattern.

- [ ] **Step 4: Run module tests**

Run: `./mvnw -B -q --no-transfer-progress -pl module/semver-calculator,module/semver-cli -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS, including the existing topic-branch tests (they pass non-null distinct branch/headBranch).

- [ ] **Step 5: Full verify and commit**

Run: `./mvnw -B -q --no-transfer-progress verify` then `pnpm --reporter=silent lint`. Expected: both green.

```bash
git add module/semver-calculator/src module/semver-cli/src
git commit -m "fix: head-branch fallback and shallow-clone fail-fast"
```

### Task 3: Integration tests — CI checkout scenarios

**Files:**

- Test: `module/semver-calculator/src/testIntegration/java/com/xenoterracide/semver/VersionCalculatorIT.java`

**Interfaces:**

- Consumes: Task 1 (`tag()`, `branch()`) and Task 2 (`isHeadBranch`) via the public API (`GitMetadataFactory.create` + `VersionCalculator.calculate`).
- Produces: blackbox regression net for every checkout shape the publish job creates. Existing `setupRepository()` (bare remote + clone + `git remote set-head --auto origin`) and `version()` helpers are reused as-is.

- [ ] **Step 1: Write the tests**

Three new `@Test` methods:

- `tagPushCheckoutReturnsCleanVersion`: `setupRepository()`, tag `v1.0.0` (jgit default = annotated), push, `git.checkout().setName("v1.0.0").call()` (detached, as `actions/checkout` with a tag ref does), `version()` hasToString `"1.0.0"`.
- `localRepositoryWithoutOriginTreatsBranchAsHead`: `Git.init()` only (no remote), commit, tag `v1.0.0` → `"1.0.0"`; commit again → starts with `"1.0.1-alpha.0.1+git.1."`.
- `lightweightTagFromGitCliIsDetected`: `setupRepository()`, run `git tag v1.0.0` via `ProcessBuilder` in `workDir`, `version()` hasToString `"1.0.0"`.

- [ ] **Step 2: Run to verify they fail against the pre-fix code (if run before Tasks 1–2, otherwise PASS)**

Run: `./mvnw -B -q --no-transfer-progress -pl module/semver-calculator -am -Dit.test=VersionCalculatorIT -Dfailsafe.failIfNoSpecifiedTests=false verify`
Expected: after Tasks 1–2, PASS. Each assertion documents a real failure observed against the unfixed code.

- [ ] **Step 3: Full verify and commit**

Run: `./mvnw -B -q --no-transfer-progress verify` then `pnpm --reporter=silent lint`. Expected: both green.

```bash
git add module/semver-calculator/src/testIntegration
git commit -m "test: cover tag-push and local-repo checkout scenarios"
```

### Task 4: Source and javadoc jars, runnable CLI manifest

**Files:**

- Modify: `pom.xml` (property + `pluginManagement` pin for maven-source-plugin 3.4.0)
- Modify: `module/git-metadata/pom.xml`, `module/semver-calculator/pom.xml`, `module/semver-cli/pom.xml` (bind `source:jar-no-fork` and `javadoc:jar` at `package`)
- Modify: `module/semver-cli/pom.xml` (maven-jar-plugin archive `Main-Class: com.xenoterracide.semver.cli.SemverCommand`)

**Interfaces:**

- Consumes: maven-javadoc-plugin 3.12.0 already pinned and configured (`doclint all,-missing`) in root `pluginManagement`.
- Produces: `module/*/target/*-sources.jar` and `*-javadoc.jar` on `package` (release assets); a `semver-cli` jar runnable as `jbang <GAV>` without `--main`.

- [ ] **Step 1: Pin maven-source-plugin**

`pom.xml`: property `<maven-source-plugin.version>3.4.0</maven-source-plugin.version>` in the Plugin versions block; `pluginManagement` entry with that version.

- [ ] **Step 2: Bind jars in the three module poms**

Each module's `<build><plugins>` (where compiler/surefire already live, per house convention — the parent is `pom` packaging and must emit no artifacts): source-plugin `jar-no-fork` at `package`, javadoc-plugin `jar` at `package`.

- [ ] **Step 3: Add the Main-Class manifest**

`module/semver-cli/pom.xml`: maven-jar-plugin with `<archive><manifest><mainClass>com.xenoterracide.semver.cli.SemverCommand</mainClass></manifest></archive>`.

- [ ] **Step 4: Verify artifacts**

Run: `./mvnw -B -q --no-transfer-progress -DskipTests -DskipITs package`
Expected: six jars exist (`ls module/*/target/*-sources.jar module/*/target/*-javadoc.jar`), no `semver-parent-*-sources.jar`; `unzip -p module/semver-cli/target/semver-cli-0.0.0.jar META-INF/MANIFEST.MF` contains `Main-Class: com.xenoterracide.semver.cli.SemverCommand`.

- [ ] **Step 5: Full verify and commit**

Run: `./mvnw -B -q --no-transfer-progress verify` then `pnpm --reporter=silent lint`. Expected: both green.

```bash
git add pom.xml module/*/pom.xml
git commit -m "build: publish source and javadoc jars with runnable cli manifest"
```

### Task 5: Sign profile and distributionManagement

**Files:**

- Modify: `pom.xml`

**Interfaces:**

- Consumes: artifacts from Task 4.
- Produces: profile `sign` (maven-gpg-plugin 3.2.8, `sign` goal at `verify`, `--pinentry-mode loopback`, passphrase via `-Dgpg.passphrase`); `distributionManagement` repository id `gh` → `https://maven.pkg.github.com/xenoterracide/semver`; versions-maven-plugin 2.25.1 pinned in `pluginManagement`. Inactive by default — local builds never sign.

- [ ] **Step 1: Add the profile and repository**

- Property `<maven-gpg-plugin.version>3.2.8</maven-gpg-plugin.version>`; `pluginManagement` entry with `<gpgArguments><arg>--pinentry-mode</arg><arg>loopback</arg></gpgArguments>`.
- `<profiles><profile><id>sign</id>` binding gpg `sign` at `verify`.
- `<distributionManagement><repository><id>gh</id><url>https://maven.pkg.github.com/xenoterracide/semver</url></repository></distributionManagement>`.
- Property `<versions-maven-plugin.version>2.25.1</versions-maven-plugin.version>` + `pluginManagement` pin.

- [ ] **Step 2: Verify inactive by default, config parses**

Run: `./mvnw -B --no-transfer-progress help:active-profiles | tail -5` → `sign` not listed.
Run: `./mvnw -B --no-transfer-progress help:effective-pom | grep -B2 -A3 "maven.pkg.github.com/xenoterracide/semver"` → the repository appears.
Run: `./mvnw -B -q --no-transfer-progress -DskipTests -DskipITs package` → BUILD SUCCESS, no signing attempted, no passphrase prompt.

- [ ] **Step 3: Full verify and commit**

Run: `./mvnw -B -q --no-transfer-progress verify` then `pnpm --reporter=silent lint`. Expected: both green.

```bash
git add pom.xml
git commit -m "build: add sign profile and GitHub Packages distribution"
```

### Task 6: main.java — resolve from GitHub Packages

**Files:**

- Modify: `main.java`

**Interfaces:**

- Consumes: nothing new.
- Produces: `//REPOS mavencentral,mavenlocal,gh=https://maven.pkg.github.com/xenoterracide/semver`. The `gh` name is the settings.xml server id used by both JBang resolution and Maven deploy. The `0.0.0` pin stays — it forces the local install (mavenlocal) during dogfooding; consumers run the GAV directly.

- [ ] **Step 1: Edit directives and comments**

Update `//REPOS`; replace the stale "requires 'mvn install'" comment with: pin `0.0.0` resolves from mavenlocal for development (`mvn install` first); consumers run `jbang com.xenoterracide:semver-cli:<version>` resolved from `gh` with a settings.xml server id `gh`.

- [ ] **Step 2: Verify locally**

Run: `./mvnw -B -q --no-transfer-progress -DskipTests -DskipITs install` then `jbang main.java --path .`
Expected: prints a version (e.g. `0.0.1-alpha.0.N+git.N.<sha>`), exit 0.

- [ ] **Step 3: Lint and commit**

Run: `pnpm --reporter=silent lint`. Expected: green.

```bash
git add main.java
git commit -m "build: resolve semver-cli from GitHub Packages in jbang wrapper"
```

### Task 7: Publish workflow and consumer docs

**Files:**

- Modify: `.github/workflows/build.yml` (add `publish` job; delete the `jbang` job)
- Modify: `README.md` (Consuming section)

**Interfaces:**

- Consumes: Tasks 1–3 (correct versions under every checkout shape), Task 4 (release assets, bare GAV run), Task 5 (`sign` profile, `gh` repository, `versions:set` pin), Task 6 (`gh` repo alias).
- Produces: every push deploys signed jars to GHP; tag pushes create GitHub releases; post-publish checks prove consumability. Secrets: `GPG_SECRET_KEY`, `GPG_PASSPHRASE` (already used by the Gradle repos).

- [ ] **Step 1: Replace the `jbang` job with a `publish` job**

`needs: build`; `runs-on: ubuntu-latest`; `timeout-minutes: 10`; `permissions: { contents: write, packages: write }`; `concurrency: { group: publish }`. Steps, in order:

1. `actions/checkout` with `ref: ${{ github.ref_name }}`, `filter: "blob:none"`, `fetch-depth: 0`, `fetch-tags: true` (the gradle-semver-proven pattern).
2. `run: git remote set-head --auto origin`.
3. `actions/setup-java` (same pinned SHA/version/distribution as the current `jbang` job).
4. `jbangdev/setup-jbang` (same pinned SHA as the current job).
5. Write `~/.m2/settings.xml` with server id `gh`, username `${{ github.actor }}`, password `${{ github.token }}`.
6. `run: echo "${{ secrets.GPG_SECRET_KEY }}" | gpg --batch --import`.
7. `run: ./mvnw -B -q --no-transfer-progress -DskipTests -DskipITs install`.
8. `run: echo "VERSION=$(jbang --quiet main.java)" >> "$GITHUB_ENV"` — if jbang banner text pollutes stdout, keep `--quiet` (global flag) and confirm stderr separation.
9. `run: ./mvnw -B -q --no-transfer-progress versions:set -DnewVersion="$VERSION" -DprocessAllModules -DgenerateBackupPoms=false` (env: `VERSION` from step 8).
10. `run: ./mvnw -B -q --no-transfer-progress -DskipTests -DskipITs -Psign deploy -Dgpg.passphrase="$GPG_PASSPHRASE"` with `env: GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}`.
11. Post-publish consumability checks (both must hold, else the job fails). GAV runs don't read `main.java`'s directives, so each names the repo explicitly (`gh` matches the settings.xml server id; JBang adds it to its defaults):
    - `test "$(jbang --quiet --repos=gh=https://maven.pkg.github.com/xenoterracide/semver "com.xenoterracide:semver-cli:$VERSION")" = "$VERSION"`
    - `test "$(jbang --quiet --repos=gh=https://maven.pkg.github.com/xenoterracide/semver "com.xenoterracide:semver-cli:0+")" = "$VERSION"` (latest-resolution check; runner `~/.m2` holds only this build's `0.0.0` and `$VERSION` sorts higher, so highest = `$VERSION`)
12. On tag push only (`startsWith(github.ref, 'refs/tags/')`): `gh release create "$GITHUB_REF_NAME" --generate-notes --verify-tag module/*/target/*.jar` with `GH_TOKEN: ${{ github.token }}`.

Delete the standalone `jbang` job — step 11 absorbs it.

- [ ] **Step 2: README — Consuming section and FAQ**

After "Build and test", add:

- A **Consuming** section: GHP repo URL, `settings.xml` server snippet (`gh` id, GitHub username, PAT with `read:packages`), and `jbang com.xenoterracide:semver-cli:<version>` / Maven dependency coordinates.
- An **FAQ** section ported from gradle-semver's README, adapted to this project:
  - **Shallow clones** — version distance is wrong from a shallow clone (the CLI fails fast). Locally: `git fetch --all --filter blob:none`. In GitHub Actions: `actions/checkout` with `filter: "blob:none"` and `fetch-depth: 0`.
  - **Annotated tags on tag pushes** — [actions/checkout#882](https://github.com/actions/checkout/issues/882): tag-push checkouts need an explicit `ref: ${{ github.ref_name }}` and `fetch-tags: true`, or the tag may not resolve.

- [ ] **Step 3: Verify formatting**

Run: `pnpm exec prettier --check .github/workflows/build.yml README.md` and `pnpm --reporter=silent lint`.
Expected: clean. If `actionlint` is on PATH, run it on the workflow too.

- [ ] **Step 4: Full verify and commit**

Run: `./mvnw -B -q --no-transfer-progress verify`. Expected: green.

```bash
git add .github/workflows/build.yml README.md
git commit -m "ci: publish signed jars to GitHub Packages on every push"
```

- [ ] **Step 5: Push (may need human help)**

The commit touches `.github/workflows/`; the agent token may lack `workflow` scope. Push the branch; if rejected for scope, hand the push to the human. The first push of the branch runs the publish job end-to-end — its green run is the proof the whole design works.
