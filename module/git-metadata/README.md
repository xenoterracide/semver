<!--
SPDX-FileCopyrightText: Copyright © 2025, 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Git Metadata

Reads git metadata — tags, commits, branches, remotes, and status. This is the `git-metadata` module of the `semver` project: it supplies the metadata the `semver-calculator` module turns into a version.

## Usage

```java
var metadata = GitMetadataFactory.create(new File("."));

var tag = metadata.tag();

var distance = metadata.distance();

var status = metadata.status();
```

`GitMetadataFactory.create` returns `null` when the directory is not a git repository. Values that have no meaning without commits (a tag, a commit SHA) return `null`.

## API

The `com.xenoterracide.git` package exports:

- `GitMetadataFactory` — opens a repository. `create(File)` and `create(String)`.
- `GitMetadata` — the values read from the repository:
  - `tag()` — the nearest version tag, or `null` when there is none.
  - `distance()` — commits from the nearest tag to HEAD, or total commits when there is no tag.
  - `distanceFromMergeBase()` — commits from the merge base (HEAD vs the `origin` HEAD branch) to HEAD; falls back to `distance()` when there is no merge base.
  - `branch()` — the current branch name, or `null` when HEAD is detached.
  - `commit()` — the full commit SHA.
  - `uniqueShort()` — the abbreviated commit SHA.
  - `status()` — `CLEAN`, `DIRTY`, or `NO_REPO`.
  - `remotes()` — the configured remotes.
- `GitStatus` — repository status: `CLEAN`, `DIRTY`, `NO_REPO`.
- `GitRemote` — a remote's name and HEAD branch.
