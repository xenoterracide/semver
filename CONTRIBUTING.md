<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Contributing to semver

## Bootstrap

    git clone <repo-url>
    cd semver
    npm run contribute

`npm run contribute` installs tools and dependencies. It heals the toolchain
through `.config/git/hooks/heal.sh`. Node 25+ drops corepack, so bootstrap
starts from npm. With direnv, run `direnv allow` to load the environment.

<<<<<<< before updating
    pnpm lint
    ./mvnw -q verify

`pnpm check` runs `pnpm lint`. `git push` runs `pnpm lint` first through
the pre-push hook; `git push --no-verify` bypasses it. The first commit's
pre-commit hook stamps headers and seeds `LICENSES`; reuse checks fail
before it runs.
=======
## Verify

    pnpm verify

`pnpm verify` runs the checks and tests. Java runs Maven verify. TypeScript
builds before checking. Config runs the checks.

`git push` runs `pnpm lint` first (pre-push hook), reuse included;
`git push --no-verify` bypasses it. The first commit's pre-commit hook stamps
headers and seeds `LICENSES`; reuse checks fail before it runs.
>>>>>>> after updating

## Updating from the template

    uvx copier update --trust

<<<<<<< before updating
Conflicts appear as `.rej` files. Resolve them and commit.

## License headers

The template renders no SPDX headers. `reuse annotate`, run by the
pre-commit hook through lint-staged, stamps each file's license.
=======
Renovate opens an update PR whenever
[base-template](https://github.com/xenoterracide/base-template) tags a release.
Merge it like any other dependency update. Conflicts appear as `.rej` files;
resolve and commit.

## License headers

Most renders carry no SPDX headers. `reuse annotate`, run at the first
commit, stamps each file's license.
>>>>>>> after updating
