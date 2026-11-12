<!--
SPDX-FileCopyrightText: Copyright © 2025, 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Semver Calculator

Calculates a [semantic version](https://semver.org/) from git metadata. It turns the metadata read by the `git-metadata` module into a version.

## Usage

### Java API

```java
var metadata = GitMetadataFactory.create(new File("."));

var version = VersionCalculator.calculate(metadata);

System.out.println(version); // e.g. 1.0.1-alpha.0.5+branch.feature.git.3.abc123
```

`GitMetadataFactory` and `GitMetadata` come from `git-metadata`. `VersionCalculator.calculate` returns an [`org.semver4j.Semver`](https://javadoc.io/doc/org.semver4j/semver4j/latest/index.html).

### CLI

The `semver-cli` module, wrapped by the root `main.java` JBang script, prints the version for a repository:

```sh
jbang main.java             # current directory
jbang main.java --path ../repo
```

## Version Calculation Algorithm

The calculator selects one of six strategies from two dimensions of the git context: the tag relationship (`ON_EXACT_TAG`, `AFTER_TAG`, `NO_TAG`) and the branch type (`HEAD_BRANCH`, `TOPIC_BRANCH`).

### Scenarios

| Scenario                                        | HEAD Branch Output             | Topic Branch Output                           |
| ----------------------------------------------- | ------------------------------ | --------------------------------------------- |
| **On exact tag**                                | `1.0.0`                        | `1.0.0+branch.feature.git.0.abc123`           |
| **After stable tag** (e.g., `v1.0.0`)           | `1.0.1-alpha.0.5+git.5.abc123` | `1.0.1-alpha.0.5+branch.feature.git.3.abc123` |
| **After pre-release tag** (e.g., `v1.0.0-rc.1`) | `1.0.0-rc.1.5+git.5.abc123`    | `1.0.0-rc.1.5+branch.feature.git.3.abc123`    |
| **No tags in repo**                             | `0.0.1-alpha.0.5+git.5.abc123` | `0.0.1-alpha.0.5+branch.feature.git.3.abc123` |

### Terminology

- **HEAD branch**: The default branch of the remote (e.g., `main`, `develop`, `master`).
  See [`git remote set-head`](https://git-scm.com/docs/git-remote#Documentation/git-remote.txt-emset-headem).
- **Merge base**: The best common ancestor between two commits (where the topic branch diverged).
  See [`git merge-base`](https://git-scm.com/docs/git-merge-base).
- **Distance**: Number of commits between two points.

### Key Behaviors

1. **HEAD Branch vs Topic Branch**:
   - On the HEAD branch, versions include `+git.<distance>.<sha>` [build metadata](https://semver.org/#spec-item-10)
   - On topic branches, versions include `+branch.<name>.git.<distance>.<sha>` build metadata
   - On exact tags (any branch): Clean version without build metadata

2. **Distance Calculation**:
   - **Prerelease distance**: Distance from tag (same for HEAD and topic branches)
   - **Build metadata distance**: For HEAD branch, distance from tag; for topic branch, distance from merge base

3. **Stable vs Pre-release Tags**:
   - After a stable tag (e.g., `v1.0.0`): Patch is incremented (`1.0.1-alpha...`)
   - After a pre-release tag (e.g., `v1.0.0-rc.1`): Distance appended (`1.0.0-rc.1.3`)

4. **Dirty Working Tree**:
   - `.dirty` is appended to metadata when the working tree has uncommitted changes
   - Example: `0.1.1-alpha.0.3+branch.feature.git.3.abc123.dirty`

5. **No Tags**:
   - Starts from `0.0.0` base, resulting in `0.0.1-alpha...`

### Merge-Base Distance Fallback

When a topic branch has no merge base to measure from, the merge-base distance falls back to the tag distance. This happens when there is:

- no `origin` remote,
- no remote `HEAD` (`refs/remotes/origin/HEAD`),
- no common ancestor (unrelated histories), or
- a detached HEAD.

In these cases the topic branch reports the same build-metadata distance the HEAD branch would.

### Version Components

```
<major>.<minor>.<patch>[-<prerelease>][+<metadata>]
```

- **major.minor.patch**: From the nearest tag (or `0.0.0` if no tags)
- **prerelease**: `alpha.0.<distance>` for stable tags, or `<tag-prerelease>.<distance>` for pre-release tags
- **metadata**:
  - HEAD branch: `git.<distance>.<short-sha>`
  - Topic branch: `branch.<branch-name>.git.<distance>.<short-sha>`
  - Dirty: `.dirty` appended to metadata
