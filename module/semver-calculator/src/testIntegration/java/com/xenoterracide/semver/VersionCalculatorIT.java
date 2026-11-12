// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;

import com.xenoterracide.git.GitMetadataFactory;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class VersionCalculatorIT {

  static final String MAIN = "main";
  static final String ORIGIN = "origin";
  static final String TOPIC = "topic/foo";

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File bareRemote;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File devDir;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File workDir;

  @Test
  void topicBranchReportsMergeBaseDistance()
    throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = setupRepository()) {
      git.checkout().setCreateBranch(true).setName(TOPIC).call();
      git.commit().setMessage("B").call();
      git.commit().setMessage("C").call();
      git.checkout().setName(MAIN).call();
      git.commit().setMessage("D").call();
      git.push().call();
      git.checkout().setName(TOPIC).call();
    }

    var metadata = GitMetadataFactory.create(workDir);
    assertThat(metadata).isNotNull();
    var version = VersionCalculator.calculate(metadata);

    assertThat(version.toString()).startsWith("0.1.2-alpha.0.3+branch.topic-foo.git.2.");
  }

  @Test
  void topicBranchWithNoOwnCommitsReportsZero()
    throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = setupRepository()) {
      git.checkout().setCreateBranch(true).setName(TOPIC).call();
      git.checkout().setName(MAIN).call();
      git.commit().setMessage("D").call();
      git.push().call();
      git.checkout().setName(TOPIC).call();
    }

    var metadata = GitMetadataFactory.create(workDir);
    assertThat(metadata).isNotNull();
    var version = VersionCalculator.calculate(metadata);

    assertThat(version.toString()).startsWith("0.1.2-alpha.0.1+branch.topic-foo.git.0.");
  }

  private Git setupRepository() throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    var uri = bareRemote.toURI().toASCIIString();
    Git.init().setDirectory(bareRemote).setInitialBranch(MAIN).setBare(true).call().close();
    try (var dev = Git.init().setDirectory(devDir).setInitialBranch(MAIN).call()) {
      dev.remoteAdd().setName(ORIGIN).setUri(new URIish(uri)).call();
      dev.commit().setMessage("initial commit").call();
      dev.push().call();
    }
    var git = Git.cloneRepository().setDirectory(workDir).setURI(uri).call();
    var setHead = new ProcessBuilder("git", "remote", "set-head", "--auto", ORIGIN)
      .directory(workDir)
      .redirectError(ProcessBuilder.Redirect.DISCARD)
      .start();
    setHead.waitFor();
    git.tag().setName("v0.1.1").call();
    git.commit().setMessage("A").call();
    git.push().call();
    return git;
  }
}
