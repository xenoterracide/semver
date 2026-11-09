<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# semver

Java library for semantic version calculation from Git metadata. Three modules:

- **git-metadata** — reads tags, commits, branches, remotes, and status.
- **semver-calculator** — turns that metadata into a version.
- **semver-cli** — command-line entry point. `main.java` is a JBang wrapper.

## Source of Truth

Read these files instead of trusting a copy of their contents here.

| For                       | See                                |
| ------------------------- | ---------------------------------- |
| Tool versions             | `.tool-versions`                   |
| pnpm version              | `packageManager` in `package.json` |
| Java dependencies         | `pom.xml`                          |
| Python dependencies       | `pyproject.toml`, `uv.lock`        |
| Formatting rules          | `.editorconfig`, `.prettierrc.cjs` |
| Conventional commit types | `git-conventional-commits.yaml`    |
| Pre-commit rules          | `.lintstagedrc.cjs`                |
| License headers           | `REUSE.toml`, `.lintstagedrc.cjs`  |
| Git hooks                 | `.config/git/hooks`                |
| Template answers          | `.copier-answers.yml`              |

## Build and Test

```bash
pnpm contribute # bootstrap once after clone: tools, deps, hooks
pnpm lint # prettier + REUSE
./mvnw -q verify # compile, test, checkstyle, spotbugs, jacoco
./mvnw -q test # unit tests only
```

`pnpm contribute` heals the toolchain through
`.config/git/hooks/heal.sh`. A failed heal never breaks a checkout.

## Module Architecture

### git-metadata

Exports `com.xenoterracide.git`:

- `GitMetadata` — interface for Git metadata access.
- `GitMetadataImpl` — JGit implementation.
- `GitStatus` — repository status: `CLEAN`, `DIRTY`, `NO_REPO`.
- `GitRemote` — interface for remote repository info.
- `DistanceCalculator` — commit distance from tags.
- `MergeBaseFinder` — merge base between branches.
- `TryGit` — functional wrapper for JGit operations.

Test fixtures live in `src/testFixtures/java`:

- `CommitTools` — commits with auto-incrementing messages.
- `GitRemotes` — runs `git remote set-head --auto`.

### semver-calculator

Exports `com.xenoterracide.semver`:

- `VersionCalculator` — version calculation entry point.
- `GitContext` — immutable context built with Immutables.
- `VersionStrategy` — strategy interface.

Strategies in the `internal` package are selected by tag relationship
(`ON_EXACT_TAG`, `AFTER_TAG`, `NO_TAG`) and branch type (`HEAD_BRANCH`,
`TOPIC_BRANCH`):

- `OnExactTagHeadStrategy`, `OnExactTagTopicStrategy`
- `AfterTagHeadStrategy`, `AfterTagTopicStrategy`
- `NoTagHeadStrategy`, `NoTagTopicStrategy`
- `VersionStrategyFactory` — picks the strategy.

## Code Style

- Read `.editorconfig` before editing. Every text file ends with a newline.
- Prettier formats Java, XML, YAML, TOML, JSON, and Markdown.
- Checkstyle runs one config per source set, under `.config/checkstyle/`:
  `main.xml`, `test.xml`, `testFixtures.xml`, `testIntegration.xml`.
  Fixtures follow main's size caps plus stricter design checks.
- Java compiles to release 17. Tests compile to release 25.
- Annotate nullable types with JSpecify `@Nullable`. NullAway enforces the
  rest.
- Use Immutables `@Value.Immutable` for value objects with Jakarta
  annotations.
- Prefer package-private visibility over `private` for methods.

## Testing

- JUnit 5 (Jupiter) and AssertJ.
- JGit in-memory repositories for Git tests.
- `@TempDir` with `CleanupMode.ON_SUCCESS` for temporary directories.
- Integration tests live in `src/testIntegration/java` and run during
  `verify`.

## Git Workflow

Conventional commits. Types come from `git-conventional-commits.yaml`.
Release tags follow `v[0-9]*.[0-9]*.[0-9]*`.

Hooks live in `.config/git/hooks`:

- `pre-commit` — runs lint-staged: formatting and SPDX headers.
- `commit-msg` — validates the conventional commit format.
- `post-checkout` and `post-merge` — heal the toolchain when lockfiles
  change.

## Template Updates

Shared tooling comes from
[xenoterracide/base-template](https://github.com/xenoterracide/base-template)
through copier. Refresh it with:

```bash
copier update --trust
```

Conflicts land as `.rej` files. Resolve them by hand. Renovate opens an
update PR whenever the template tags a release.

Staged workflows live in `staged/.github/workflows/`. The active copies in
`.github/workflows/` are consumer-owned. Copy them with credentials that
carry the `workflow` scope:

```bash
cp -r staged/.github/workflows .github/workflows
```

## Licensing

All files carry SPDX headers. REUSE enforces them.

- **GPL-3.0-or-later** — source code.
- **MIT** — scripts, configuration, workflows.
- **CC0-1.0** — configuration and generated files.
- **CC-BY-NC-SA-4.0** — documentation.
