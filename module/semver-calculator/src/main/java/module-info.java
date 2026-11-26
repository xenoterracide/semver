// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: PolyForm-Small-Business-1.0.0 OR PolyForm-Noncommercial-1.0.0

/**
 * Semantic versioning calculation module.
 */
module com.xenoterracide.semver {
  requires com.xenoterracide.git;
  requires org.semver4j;
  requires com.google.common;
  requires org.jspecify;
  requires static org.immutables.value.annotations;
  requires static org.immutables.datatype;
  requires static com.google.errorprone.annotations;
  requires static org.eclipse.jgit;

  exports com.xenoterracide.semver;
}
