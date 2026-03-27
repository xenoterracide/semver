// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import org.apache.commons.lang3.Strings;
import org.eclipse.jgit.lib.Constants;
import org.jspecify.annotations.Nullable;

/**
 * Git Remote information.
 */
public interface GitRemote {
  /**
   * Gets the remote HEAD branch. This removes the {@code refs/heads/} prefix.
   *
   * @return HEAD branch
   */
  @Nullable
  default String headBranch() {
    var prefix = Constants.R_REMOTES + this.name() + "/";
    var refName = this.headBranchRefName();
    return refName != null ? Strings.CI.removeStart(refName, prefix) : null;
  }

  /**
   * Gets the remote HEAD branch ref name. This will look something like {@code refs/remotes/origin/main}.
   *
   * @return HEAD branch ref name
   */
  @Nullable
  String headBranchRefName();

  /**
   * Gets the remote name; a common example is origin.
   *
   * @return remote name
   */
  String name();
}
