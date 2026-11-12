// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;

import com.xenoterracide.git.GitMetadataFactory;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.semver4j.Semver;

/**
 * Verifies that the versions the calculator produces sort in the order Maven resolves them.
 *
 * <p>Ordering is checked with Maven's {@link ComparableVersion}, not Semver4j: Maven treats build
 * metadata as significant, so a topic branch's {@code +branch.<name>.git.N} metadata orders it
 * relative to the HEAD branch's {@code +git.N}. This is the ordering that decides dependency
 * resolution when the version is used in Maven coordinates.</p>
 */
class VersionOrderingIT {

  static final String MAIN = "main";
  static final String ORIGIN = "origin";

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File bareRemote;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File devDir;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File workDir;

  @Test
  void versionProgressionFromTag() throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = setupRepository()) {
      git.tag().setName("v1.0.0").call();
      var onTag = version();

      git.commit().setMessage("commit on main").call();
      git.push().setPushAll().call();
      var onMain = version();

      git.checkout().setCreateBranch(true).setName("feature-x").call();
      git.commit().setMessage("commit on topic").call();
      var onTopic = version();

      assertThat(new ComparableVersion(onTag.toString()))
        .isLessThan(new ComparableVersion(onMain.toString()))
        .isLessThan(new ComparableVersion(onTopic.toString()));
    }
  }

  @Test
  void topicBranchSortsHigherThanHeadBranch()
    throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = setupRepository()) {
      git.tag().setName("v1.0.0").call();
      git.commit().setMessage("first").call();
      git.commit().setMessage("second").call();
      git.push().setPushAll().call();
      var headVersion = version();

      git.checkout().setCreateBranch(true).setName("feature-x").call();
      git.commit().setMessage("topic commit").call();
      var topicVersion = version();

      assertThat(new ComparableVersion(topicVersion.toString())).isGreaterThan(
        new ComparableVersion(headVersion.toString())
      );
    }
  }

  private Semver version() {
    var metadata = GitMetadataFactory.create(workDir);
    assertThat(metadata).isNotNull();
    return VersionCalculator.calculate(metadata);
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
    return git;
  }
}
