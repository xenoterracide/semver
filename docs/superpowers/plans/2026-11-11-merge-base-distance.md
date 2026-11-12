<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Merge-Base Distance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Topic-branch build metadata reports the merge-base distance, not the tag distance.

**Architecture:** Add `distanceFromMergeBase()` to the `GitMetadata` interface (default returns `distance()`), compute it in `GitMetadataImpl` using `MergeBaseFinder` plus `DistanceCalculator.distanceBetween`, and read it in `VersionCalculator.toGitContext` instead of copying `distance()`. A blackbox integration test then recreates the `SpecIntegrationTest` scenario end to end through `GitMetadataFactory` and `VersionCalculator`.

**Tech Stack:** Java 17 (main) / 25 (tests), JGit, vavr, JUnit 5 + AssertJ, Maven.

**Spec:** `module/semver-calculator/README.md` (§ "Version Calculation Algorithm", § "Distance Calculation"). The distance rules: prerelease uses distance from tag for both branch types; build metadata uses tag distance on the HEAD branch and merge-base distance on a topic branch.

## Global Constraints

- Main code compiles to Java release 17; tests to release 25 (`maven.compiler.release` = 17).
- NullAway runs in JSpecify mode; annotate nullable returns/params with `@Nullable`.
- Java sources carry SPDX `GPL-3.0-or-later` headers; the plan doc is `CC-BY-NC-SA-4.0`.
- Conventional commits; valid types are `ci feat fix perf refactor style test build ops docs chore merge revert`; `commitScopes` is empty, so use no scope.
- `GitMetadata` is a public interface. The new method must be a `default` method so existing implementers (the test stub `SimpleGitMetadata`) stay source-compatible.
- Follow existing JGit test patterns: `CommitTools.commit(git)` for commits, on-disk `@TempDir(cleanup = CleanupMode.ON_SUCCESS)` repos, `MergeBaseFinderTest.setupRemote()` for bare-remote + `set-head` setup.
- The integration test is blackbox: it uses JGit only to build its fixture and touches the SUT through public API (`GitMetadataFactory.create`, `VersionCalculator.calculate`). Test code is patched into its module under JPMS, so the module descriptor must declare JGit readable: add a `test`-scope `org.eclipse.jgit` dependency plus `requires static org.eclipse.jgit` in `semver-calculator`'s `module-info.java` (jgit is already on the compile module path transitively via `git-metadata`).
- Run `./mvnw -B -q --no-transfer-progress verify` before claiming done.
- Module-scoped test commands use `-pl module/<name> -am` (also-make) because `semver-calculator` depends on `git-metadata`; `git-metadata` alone needs only `-pl`. Add `-DfailIfNoTests=false` when `-Dtest` targets a test that exists in only one module.

## Review Focus

The inputs most likely to bite a user, in order, with the test that pins each:

1. **Topic branch with no commits of its own** (merge base == HEAD) must report `git.0`, not the tag distance. → Task 1, `distanceFromMergeBaseOnHeadBranchIsZero`, and Task 3, `topicBranchWithNoOwnCommitsReportsZero`.
2. **Merge-base distance larger than tag distance** (a tag lands on the topic branch after it diverged) must report the larger value: `1.0.1-alpha.0.5+branch.feature-x.git.11.abc123`. → Task 2, `mergeBaseDistanceLargerThanTagDistance`.
3. **No origin / no remote HEAD / detached HEAD** must fall back to `distance()` so existing output is unchanged. → Task 1, `distanceFromMergeBaseFallsBackWithoutOrigin`.
4. **Exact tag on a topic branch** must still emit `git.0` (the `OnExactTagTopicStrategy` path is unaffected). → already covered by `OnExactTagTopicStrategyTest`; verify in Task 2.
5. **Empty repository (HEAD unresolved)** must fall back rather than throw. → Task 1 implementation routes through `distance()` via `Try`; `distance()`'s empty-repo behavior (`0`) is already pinned by `GitMetadataImplTest.getCommitDistance`.

---

### Task 1: Expose and compute merge-base distance in git-metadata

**Files:**

- Modify: `module/git-metadata/src/main/java/com/xenoterracide/git/GitMetadata.java` (add default method after `distance()`, ~line 35)
- Modify: `module/git-metadata/src/main/java/com/xenoterracide/git/GitMetadataImpl.java` (add override after `distance()`, ~line 150)
- Test: `module/git-metadata/src/test/java/com/xenoterracide/git/GitMetadataImplTest.java` (add three tests)

**Interfaces:**

- Consumes: existing `GitMetadata.distance()`, `GitMetadata.remotes()`, `GitRemote.name()`, `MergeBaseFinder.find(GitRemote) -> Optional<ObjectId>`, `DistanceCalculator.distanceBetween(ObjectId, ObjectId) -> long`, `GitMetadataImpl.gitRepository() -> Try<Repository>`, `GitMetadataImpl.getObjectIdFor(String) -> Try<ObjectId>`.
- Produces: `GitMetadata.distanceFromMergeBase() -> long` (default returns `distance()`), overridden in `GitMetadataImpl`. Task 2 consumes it.

- [ ] **Step 1: Write the failing test `distanceFromMergeBaseFallsBackWithoutOrigin`**

In `GitMetadataImplTest`, a plain repo with no remote must fall back to `distance()`:

```java
@Test
void distanceFromMergeBaseFallsBackWithoutOrigin() throws Exception {
  try (var git = Git.init().setDirectory(projectDir).call()) {
    commit(git);
    var pg = new GitMetadataImpl(() -> git);
    assertThat(pg.distanceFromMergeBase()).isEqualTo(pg.distance());
  }
}
```

`projectDir` is the existing `@TempDir` field; `commit` is the static import `com.xenoterracide.git.fixtures.CommitTools.commit`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -B -q --no-transfer-progress -pl module/git-metadata -Dtest=GitMetadataImplTest#distanceFromMergeBaseFallsBackWithoutOrigin test`
Expected: compile error — `distanceFromMergeBase()` does not exist on `GitMetadata`.

- [ ] **Step 3: Add the default method `distanceFromMergeBase()` to `GitMetadata`**

Signature: `default long distanceFromMergeBase() { return this.distance(); }`, with Javadoc stating it returns the commits from the merge base (between HEAD and the origin HEAD branch) to HEAD, and falls back to `distance()` when no merge base exists. No `@Nullable`.

- [ ] **Step 4: Run the test to verify it passes**

Run: same as Step 2.
Expected: PASS (default returns `distance()`, so fallback path holds).

- [ ] **Step 5: Write the failing tests for the real distance**

Add to `GitMetadataImplTest` two tests that set up an origin with a HEAD ref (mirror `MergeBaseFinderTest.setupRemote()`: bare remote, clone, `git remote set-head origin --auto`), then branch:

```java
@Test
void distanceFromMergeBaseOnHeadBranchIsZero() throws Exception {
  // clone + set-head origin (merge base == HEAD, no topic commits)
  var pg = new GitMetadataImpl(() -> git);
  assertThat(pg.distanceFromMergeBase()).isEqualTo(0L);
}

@Test
void distanceFromMergeBaseTopicBranchCountsOwnCommits() throws Exception {
  // clone + set-head origin, then checkout -b topic/foo and commit twice
  var pg = new GitMetadataImpl(() -> git);
  assertThat(pg.distanceFromMergeBase()).isEqualTo(2L);
}
```

Use method-level `@TempDir` parameters (`File bareRemote`, `File workDir`) or a dedicated setup mirroring `MergeBaseFinderTest.setupRemote()`; both tests need `refs/remotes/origin/HEAD` to exist so `MergeBaseFinder.find(origin)` returns a value.

- [ ] **Step 6: Run the tests to verify they fail**

Run: `./mvnw -B -q --no-transfer-progress -pl module/git-metadata -Dtest=GitMetadataImplTest#distanceFromMergeBaseOnHeadBranchIsZero+distanceFromMergeBaseTopicBranchCountsOwnCommits test`
Expected: both FAIL — the default method returns `distance()` (tag distance), not the merge-base distance.

- [ ] **Step 7: Override `distanceFromMergeBase()` in `GitMetadataImpl`**

Signature: `@Override public long distanceFromMergeBase()`. Body approach:

1. `this.remotes().stream().filter(r -> "origin".equals(r.name())).findFirst()` — the same origin filter `VersionCalculator.extractHeadBranch` uses.
2. `.flatMap(remote -> this.gitRepository().map(repo -> new MergeBaseFinder(repo).find(remote)).getOrElse(Optional.empty()))` to get the merge base.
3. `.map(mergeBase -> this.getObjectIdFor(Constants.HEAD).map(head -> new DistanceCalculator(this.git).distanceBetween(mergeBase, head)).getOrElseGet(this::distance))`.
4. `.orElseGet(this::distance)`.

Return the long. Import `java.util.Optional` if not already present. Name private helpers to avoid colliding with the public method.

- [ ] **Step 8: Run the tests to verify they pass**

Run: same as Step 6, plus the Step 1 fallback test.
Expected: all three PASS.

- [ ] **Step 9: Run the module build and commit**

Run: `./mvnw -B -q --no-transfer-progress -pl module/git-metadata verify`
Then:

```bash
git add module/git-metadata
git commit -m "fix: compute merge-base distance in git-metadata"
```

---

### Task 2: Use merge-base distance in VersionCalculator

**Files:**

- Modify: `module/semver-calculator/src/main/java/com/xenoterracide/semver/VersionCalculator.java:65` (`.distanceFromMergeBase(distance)` → `.distanceFromMergeBase(metadata.distanceFromMergeBase())`)
- Test: `module/semver-calculator/src/test/java/com/xenoterracide/semver/VersionCalculatorTest.java` (extend `SimpleGitMetadata` and add two tests)

**Interfaces:**

- Consumes: `GitMetadata.distanceFromMergeBase() -> long` from Task 1; existing `GitContext.builder()`, `VersionStrategyFactory.determineStrategy`.
- Produces: nothing new — `VersionCalculator.calculate(GitMetadata) -> Semver` keeps its signature; only its internal `toGitContext` wiring changes.

- [ ] **Step 1: Extend the `SimpleGitMetadata` stub to expose a distinct merge-base distance**

In `VersionCalculatorTest`, add a `long mergeBaseDistance` record component after `distance`, and override the interface default:

```java
private record SimpleGitMetadata(
  @Nullable String tag,
  long distance,
  long mergeBaseDistance,
  @Nullable String branch,
  @Nullable String commit,
  @Nullable String uniqueShort,
  GitStatus status,
  List<GitRemote> remotes
) implements GitMetadata {
  @Override
  public long distanceFromMergeBase() {
    return this.mergeBaseDistance;
  }
}
```

Update the existing `createMetadata` helper (lines 168-179) to keep current call sites compiling by delegating to a new overload that takes `mergeBaseDistance`, passing `distance` for it. Add the new overload:

```java
private static GitMetadata createMetadata(
  @Nullable String tag,
  long distance,
  long mergeBaseDistance,
  @Nullable String branch,
  @Nullable String headBranch,
  String shortSha,
  GitStatus status
) { ... }
```

- [ ] **Step 2: Write the failing test `afterTagTopicBranchUsesMergeBaseDistance`**

```java
@Test
void afterTagTopicBranchUsesMergeBaseDistance() {
  var metadata = createMetadata("v1.0.0", 5, 3, "feature-x", "main", "abc123", GitStatus.CLEAN);

  var version = VersionCalculator.calculate(metadata);

  assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
  assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "3", "abc123");
}
```

- [ ] **Step 3: Write the failing test `mergeBaseDistanceLargerThanTagDistance`**

Pins the case where the tag is on the topic branch (merge-base distance exceeds tag distance):

```java
@Test
void mergeBaseDistanceLargerThanTagDistance() {
  var metadata = createMetadata("v1.0.0", 5, 11, "feature-x", "main", "abc123", GitStatus.CLEAN);

  var version = VersionCalculator.calculate(metadata);

  assertThat(version).hasToString("1.0.1-alpha.0.5+branch.feature-x.git.11.abc123");
}
```

- [ ] **Step 4: Run the tests to verify they fail**

Run: `./mvnw -B -q --no-transfer-progress -pl module/semver-calculator -am -Dtest=VersionCalculatorTest#afterTagTopicBranchUsesMergeBaseDistance+mergeBaseDistanceLargerThanTagDistance -DfailIfNoTests=false test`
Expected: FAIL — `toGitContext` still writes `distance` (5) into `distanceFromMergeBase`, so build metadata reads `git.5`, not `git.3` / `git.11`.

- [ ] **Step 5: Change `VersionCalculator.toGitContext` line 65**

Replace `.distanceFromMergeBase(distance)` with `.distanceFromMergeBase(metadata.distanceFromMergeBase())`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./mvnw -B -q --no-transfer-progress -pl module/semver-calculator -am test`
Expected: PASS — the two new tests plus all existing `VersionCalculatorTest` cases (existing cases pass `distance` as merge-base distance, so their assertions are unchanged).

- [ ] **Step 7: Verify the exact-tag topic path is unaffected and commit**

Run: `./mvnw -B -q --no-transfer-progress -pl module/semver-calculator -am verify` (this runs `OnExactTagTopicStrategyTest`, which still expects `git.0`).
Then:

```bash
git add module/semver-calculator
git commit -m "fix: use merge-base distance for topic branch metadata"
```

---

### Task 3: Blackbox end-to-end integration test in semver-calculator

Recreates the `SpecIntegrationTest` "one step after another" scenario through public API only.

**Files:**

- Modify: `module/semver-calculator/pom.xml` (add `org.eclipse.jgit` test-scope dependency)
- Modify: `module/semver-calculator/src/main/java/module-info.java` (add `requires static org.eclipse.jgit;`)
- Test: `module/semver-calculator/src/testIntegration/java/com/xenoterracide/semver/VersionCalculatorIT.java` (create)

**Interfaces:**

- Consumes: `GitMetadataFactory.create(File) -> @Nullable GitMetadata` (public), `VersionCalculator.calculate(GitMetadata) -> Semver` (public); transitively `GitMetadata.distanceFromMergeBase()` from Task 1.
- Produces: a blackbox regression test for the full `GitMetadataImpl → VersionCalculator` path. No new API.

- [ ] **Step 1: Grant JGit to the test harness**

Add to `semver-calculator/pom.xml` (version inherited from the parent's `dependencyManagement`):

```xml
<dependency>
  <groupId>org.eclipse.jgit</groupId>
  <artifactId>org.eclipse.jgit</artifactId>
  <scope>test</scope>
</dependency>
```

Add to `module-info.java`'s `requires static` group:

```java
requires static org.eclipse.jgit;
```

`requires static` (not `requires`) keeps jgit an optional runtime edge, since main code never uses it. It resolves at compile time because jgit is already on the compile module path transitively via `git-metadata`.

- [ ] **Step 2: Write the blackbox test**

Create `VersionCalculatorIT` in `src/testIntegration/java` (failsafe picks up `*IT.java`). A private `setupRepository()` helper mirrors `MergeBaseFinderTest.setupRemote()`: bare remote, work repo on `main` with `origin`, initial commit, push, `git remote set-head origin --auto` via `ProcessBuilder`, tag `v0.1.1`, commit A, push — so `refs/remotes/origin/HEAD` exists and `origin/main` = A (the merge base). Close the JGit handle before calling `GitMetadataFactory.create`.

```java
@Test
void topicBranchReportsMergeBaseDistance() throws Exception {
  try (var git = setupRepository()) {
    git.checkout().setCreateBranch(true).setName("topic/foo").call();
    git.commit().setMessage("B").call();
    git.commit().setMessage("C").call();
    git.checkout().setName("main").call();
    git.commit().setMessage("D").call();
    git.push().call();
    git.checkout().setName("topic/foo").call();
  } // handle closed

  var metadata = GitMetadataFactory.create(workDir);
  assertThat(metadata).isNotNull();
  var version = VersionCalculator.calculate(metadata);

  assertThat(version.toString()).startsWith("0.1.2-alpha.0.3+branch.topic-foo.git.2.");
}

@Test
void topicBranchWithNoOwnCommitsReportsZero() throws Exception {
  try (var git = setupRepository()) {
    git.checkout().setCreateBranch(true).setName("topic/foo").call();
    git.checkout().setName("main").call();
    git.commit().setMessage("D").call();
    git.push().call();
    git.checkout().setName("topic/foo").call();
  }

  var metadata = GitMetadataFactory.create(workDir);
  assertThat(metadata).isNotNull();
  var version = VersionCalculator.calculate(metadata);

  assertThat(version.toString()).startsWith("0.1.2-alpha.0.1+branch.topic-foo.git.0.");
}
```

The first pins the core case: tag distance 3 (`A,B,C`), merge-base distance 2 (`B,C`). The second pins a topic branch with no commits of its own (`git.0`). This is a regression test — after Tasks 1-2 it passes on first run; to prove it bites, temporarily revert Task 2's line-65 change and watch it emit `git.3`.

- [ ] **Step 3: Run the integration test**

Run: `./mvnw -B -q --no-transfer-progress -pl module/semver-calculator -am -Dit.test=VersionCalculatorIT verify`
Expected: PASS (failsafe runs `VersionCalculatorIT`; the two assertions hold).

- [ ] **Step 4: Commit**

```bash
git add module/semver-calculator
git commit -m "test: add blackbox merge-base distance integration test"
```

---

### Task 4: Full-build verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full build and lint**

Run: `./mvnw -B -q --no-transfer-progress verify` then `pnpm lint`
Expected: BUILD SUCCESS and lint clean (checkstyle, spotbugs, jacoco ≥ 0.90, REUSE, prettier all pass).

- [ ] **Step 2: Report completion**

Confirm the merge-base distance now flows into topic-branch metadata, and hand off per the chosen execution method.
