// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git.fixtures;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test fixtures for configuring git remotes.
 */
public final class GitRemotes {

  private static final Logger LOG = LoggerFactory.getLogger(GitRemotes.class);

  private GitRemotes() {}

  /**
   * Runs {@code git remote set-head --auto origin} in the project directory.
   *
   * @param git the git repository
   * @param projectDir the project directory
   * @return the git repository
   * @throws IOException if the process cannot start
   * @throws InterruptedException if the process is interrupted
   */
  public static Git setHeadAuto(Git git, File projectDir) throws IOException, InterruptedException {
    var process = new ProcessBuilder()
      .directory(projectDir)
      .command("git", "remote", "set-head", "--auto", "origin")
      .start();
    try (var input = process.inputReader()) {
      LOG.warn("set-head: {}", input.lines().toList());
    }
    process.waitFor();
    var remoteRef = Constants.R_REMOTES + "origin" + "/" + Constants.HEAD;
    var file = git.getRepository().getDirectory().toPath().resolve(remoteRef).toFile();
    LOG.warn("project: '{}' with remoteRef: '{}' exists: {}", projectDir, file, file.exists());
    return git;
  }
}
