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
import org.semver4j.Semver;

/**
 * Blackbox integration test of the version calculation algorithm against a real repository.
 *
 * <p>Recreates the {@code SpecIntegrationTest.headBranch()} scenario from the archived
 * {@code xenoterracide/gradle-semver} project: a single run walks commit, tag, and branch in sequence,
 * asserting the calculated version and its monotonic ordering at each step through
 * {@link GitMetadataFactory} and {@link VersionCalculator} only.</p>
 */
class VersionCalculatorIT {

  static final String MAIN = "main";
  static final String ORIGIN = "origin";
  static final String TOPIC = "topic/foo";
  static final String PR_TOPIC = "topic/prerelease-test";

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File bareRemote;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File devDir;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File workDir;

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File localDir;

  @Test
  @SuppressWarnings({ "checkstyle:JavaNCSS", "checkstyle:ExecutableStatementCount" })
  void headBranch() throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = setupRepository()) {
      var v001Alpha01 = version();
      assertThat(v001Alpha01).asString().startsWith("0.0.1-alpha.0.1+git.1.");

      git.commit().setMessage("second commit").call();
      var v001Alpha02 = version();
      assertThat(v001Alpha02).asString().startsWith("0.0.1-alpha.0.2+git.2.");

      git.tag().setName("v0.1.0").call();
      var v010 = version();
      assertThat(v010).hasToString("0.1.0").isGreaterThan(v001Alpha01);

      git.commit().setMessage("after v0.1.0").call();
      var v010BldV2 = version();
      assertThat(v010BldV2)
        .isGreaterThan(v001Alpha01)
        .isGreaterThan(v010)
        .asString()
        .startsWith("0.1.1-alpha.0.1+git.1.");

      git.commit().setMessage("after v0.1.0 again").call();
      var v010BldV3 = version();
      assertThat(v010BldV3).isGreaterThan(v010BldV2).asString().startsWith("0.1.1-alpha.0.2+git.2.");

      git.tag().setName("v0.1.1-rc.1").call();
      var v011Rc1 = version();
      assertThat(v011Rc1).hasToString("0.1.1-rc.1").isGreaterThan(v010BldV3);

      git.commit().setMessage("after rc.1").call();
      var v011Rc1BldV1 = version();
      assertThat(v011Rc1BldV1).asString().startsWith("0.1.1-rc.1.1+git.1.");

      git.tag().setName("v0.1.1").call();
      var v011 = version();
      assertThat(v011).hasToString("0.1.1").isGreaterThan(v011Rc1BldV1);

      git.commit().setMessage("A").call();
      git.checkout().setCreateBranch(true).setName(TOPIC).call();
      git.push().setPushAll().call();
      var vTopic0 = version();
      assertThat(vTopic0).isGreaterThan(v011).asString().startsWith("0.1.2-alpha.0.1+branch.topic-foo.git.0.");

      git.commit().setMessage("B").call();
      git.commit().setMessage("C").call();
      git.push().setPushAll().call();
      git.checkout().setName(MAIN).call();

      git.commit().setMessage("D").call();
      git.push().setPushAll().call();
      var vMain2 = version();
      assertThat(vMain2).isGreaterThan(v011).asString().startsWith("0.1.2-alpha.0.2+git.2.");

      git.checkout().setName(TOPIC).call();
      var vTopic2 = version();
      assertThat(vTopic2).isGreaterThan(v011).asString().startsWith("0.1.2-alpha.0.3+branch.topic-foo.git.2.");

      git.checkout().setName(MAIN).call();
      git.commit().setMessage("prerelease base").call();
      git.tag().setName("v0.2.0-rc.1").call();
      git.push().setPushAll().call();
      var v020Rc1 = version();
      assertThat(v020Rc1).hasToString("0.2.0-rc.1");

      git.checkout().setCreateBranch(true).setName(PR_TOPIC).call();
      git.commit().setMessage("prerelease topic").call();
      git.push().setPushAll().call();
      var v020Rc1Topic = version();
      assertThat(v020Rc1Topic).asString().startsWith("0.2.0-rc.1.1+branch.topic-prerelease-test.git.1.");
    }
  }

  @Test
  void tagPushCheckoutReturnsCleanVersion()
    throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = setupRepository()) {
      git.tag().setName("v1.0.0").call();
      git.push().setPushTags().call();
      git.checkout().setName("v1.0.0").call();
      assertThat(version()).hasToString("1.0.0");
    }
  }

  @Test
  void localRepositoryWithoutOriginTreatsBranchAsHead() throws GitAPIException, IOException {
    try (var git = Git.init().setDirectory(localDir).setInitialBranch(MAIN).call()) {
      git.commit().setMessage("initial commit").call();
      git.tag().setName("v1.0.0").call();
      assertThat(version(localDir)).hasToString("1.0.0");

      git.commit().setMessage("after v1.0.0").call();
      assertThat(version(localDir)).asString().startsWith("1.0.1-alpha.0.1+git.1.");
    }
  }

  @Test
  void lightweightTagFromGitCliIsDetected()
    throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var ignored = setupRepository()) {
      var tag = new ProcessBuilder("git", "tag", "v1.0.0")
        .directory(workDir)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start();
      tag.waitFor();
      assertThat(version()).hasToString("1.0.0");
    }
  }

  private Semver version() {
    return version(workDir);
  }

  private static Semver version(File directory) {
    var metadata = GitMetadataFactory.create(directory);
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
