// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: PolyForm-Small-Business-1.0.0 OR PolyForm-Noncommercial-1.0.0

/**
 * Git metadata extraction module.
 */
module com.xenoterracide.git {
  requires org.eclipse.jgit;
  requires io.vavr;
  requires org.apache.commons.lang3;
  requires com.google.common;
  requires org.slf4j;
  requires static org.jspecify;

  exports com.xenoterracide.git;
}
