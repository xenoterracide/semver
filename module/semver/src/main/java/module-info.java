// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: MIT

/**
 * Semantic versioning calculation module.
 */
module com.xenoterracide.semver {
  requires com.xenoterracide.git;
  requires org.semver4j;
  requires com.google.common;
  requires org.jspecify;

  exports com.xenoterracide.semver;

  opens com.xenoterracide.semver to org.immutables.builder;
}
