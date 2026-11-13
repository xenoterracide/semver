// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

// JAVA 17+
// REPOS mavencentral,mavenlocal,gh=https://maven.pkg.github.com/xenoterracide/semver

// The 0.0.0 pin resolves from mavenlocal for development ('mvn install' first).
// Consumers run jbang com.xenoterracide:semver-cli:<version>, resolved from the
// gh repo, which matches the settings.xml server id gh.
//DEPS com.xenoterracide:semver-cli:0.0.0
//DEPS org.slf4j:slf4j-simple:2.0.17

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
class main {

  public static void main(String[] args) {
    SemverCommand.main(args);
  }
}
