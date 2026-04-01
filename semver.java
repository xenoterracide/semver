// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

// JAVA 17+
// REPOS mavencentral,mavenlocal

// Dependencies will be resolved from Maven Central once published
// For now, requires 'mvn install' to be run first
// DEPS com.xenoterracide:semver-cli:0.0.0

import com.xenoterracide.semver.cli.SemverCommand;

/**
 * JBang entry point for the semver CLI.
 *
 * <p>This script is a thin wrapper around {@link SemverCommand}.
 * The actual CLI implementation lives in the semver-cli module.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * jbang semver.java [options]
 * </pre>
 */
class semver {

  public static void main(String[] args) {
    SemverCommand.main(args);
  }
}
