<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Contributing to semver

## Bootstrap

    git clone <repo-url>
    cd semver
    pnpm contribute

## Linting and testing

    pnpm lint
    ./mvnw -q verify

## Updating from the template

    copier update --trust

Conflicts appear as `.rej` files. Resolve them and commit.

## License headers

The template renders no SPDX headers. `reuse annotate`, run by the
pre-commit hook through lint-staged, stamps each file's license.
