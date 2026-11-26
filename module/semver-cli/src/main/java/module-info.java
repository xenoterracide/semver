// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: PolyForm-Small-Business-1.0.0 OR PolyForm-Noncommercial-1.0.0

/**
 * Semantic versioning CLI module.
 */
module com.xenoterracide.semver.cli {
  requires com.xenoterracide.git;
  requires com.xenoterracide.semver;
  requires info.picocli;
  requires static org.eclipse.jgit;
  requires static org.jspecify;
  requires org.slf4j;

  exports com.xenoterracide.semver.cli;

  opens com.xenoterracide.semver.cli to info.picocli;
}
