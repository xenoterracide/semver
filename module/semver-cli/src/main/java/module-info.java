// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * Semantic versioning CLI module.
 */
module com.xenoterracide.semver.cli {
  requires com.xenoterracide.git;
  requires com.xenoterracide.semver;
  requires info.picocli;
  requires static org.jspecify;
  requires org.slf4j;

  exports com.xenoterracide.semver.cli;
}
