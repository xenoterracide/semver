// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: MIT

/**
 * Git metadata extraction module.
 */
module com.xenoterracide.git {
  requires org.eclipse.jgit;
  requires io.vavr;
  requires org.apache.commons.lang3;
  requires com.google.common;
  requires org.jspecify;
  requires org.slf4j;

  exports com.xenoterracide.git;
  exports com.xenoterracide.git.internal;
}
