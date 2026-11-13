// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import static com.xenoterracide.git.fixtures.CommitTools.commit;
import static com.xenoterracide.git.fixtures.CommitTools.supplies;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class GitMetadataImplTest {

  static final String MAIN = "main";
  static final String ORIGIN = "origin";

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File projectDir;

  @Test
  void getBranchName() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();
      git.branchCreate().setName("topic/test").call();
      git.checkout().setName("topic/test").call();

      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.branch()).isEqualTo("topic/test");
    }
  }

  @Test
  void getObjectIdFor() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();
      git.branchCreate().setName("topic/test").call();
      git.checkout().setName("topic/test").call();

      var pg = new GitMetadataImpl(() -> git);
      var main = pg.getObjectIdFor("topic/test").get().getName();
      var head = pg.getObjectIdFor("HEAD").get().getName();
      assertThat(main).hasSize(40);
      assertThat(head).hasSize(40);
      assertThat(main).isEqualTo(head);
    }
  }

  @Test
  void getSha() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();
      git.branchCreate().setName("topic/test").call();
      git.checkout().setName("topic/test").call();

      var pg = new GitMetadataImpl(() -> git);
      var main = pg.getRev("topic/test");
      var head = pg.getRev("HEAD");
      assertThat(main).hasSize(40);
      assertThat(head).hasSize(40);
      assertThat(main).isEqualTo(head);
    }
  }

  @Test
  void getHeadSha() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();
      git.branchCreate().setName("topic/test").call();
      git.checkout().setName("topic/test").call();

      var pg = new GitMetadataImpl(() -> git);
      var main = pg.getRev("topic/test");
      var head = pg.commit();
      assertThat(main).hasSize(40);
      assertThat(head).hasSize(40);
      assertThat(main).isEqualTo(head);
    }
  }

  @Test
  void getHeadShortSha() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();
      git.branchCreate().setName("topic/test").call();
      git.checkout().setName("topic/test").call();

      var pg = new GitMetadataImpl(() -> git);
      var main = pg.getRev("topic/test");
      var head = pg.uniqueShort();
      assertThat(main).isNotNull();
      assertThat(main).hasSize(40);
      assertThat(head).hasSize(7);
      assertThat(main.substring(0, 7)).isEqualTo(head);
    }
  }

  @Test
  void getLastTag() throws Exception {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.tag()).isNull();

      git.commit().setMessage("first commit").call();

      assertThat(pg.tag()).isNull();

      git.commit().setMessage("second commit").call();

      assertThat(pg.tag()).isNull();

      git.tag().setName("v0.1.0").call();
      assertThat(pg.tag()).isEqualTo("v0.1.0");

      git.commit().setMessage("second commit").call();

      assertThat(pg.tag()).isEqualTo("v0.1.0");

      git.tag().setName("v0.1.1").call();
      assertThat(pg.tag()).isEqualTo("v0.1.1");
    }
  }

  @Test
  void getCommitDistance() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      var pg = new GitMetadataImpl(() -> git);
      Supplier<Long> distance = pg::distance;
      assertThat(distance.get()).isEqualTo(0L);

      assertThat(supplies(commit(git), distance)).isEqualTo(1L);

      assertThat(supplies(commit(git), distance)).isEqualTo(2L);

      git.tag().setName("v0.1.0").call();

      assertThat(distance.get()).isEqualTo(0L);

      assertThat(supplies(commit(git), distance)).isEqualTo(1L);

      assertThat(supplies(commit(git), distance)).isEqualTo(2L);

      git.tag().setName("v0.1.1").call();
      assertThat(distance.get()).isEqualTo(0L);

      assertThat(supplies(commit(git), distance)).isEqualTo(1L);

      git.tag().setName("v0.1.2-beta.0").call();

      assertThat(distance.get()).isEqualTo(0L);
      assertThat(supplies(commit(git), distance)).isEqualTo(1L);
      assertThat(supplies(commit(git), distance)).isEqualTo(2L);
    }
  }

  @Test
  void getStatus() throws Exception {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();
      git.tag().setName("v0.1.0").call();

      var pg = new GitMetadataImpl(() -> git);

      assertThat(pg.status()).isEqualTo(GitStatus.CLEAN);

      Files.createFile(projectDir.toPath().resolve("test.txt"));

      assertThat(pg.status()).isEqualTo(GitStatus.DIRTY);
    }
  }

  @Test
  void getRemotesEmpty() throws Exception {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.remotes()).isEmpty();
    }
  }

  @Test
  void remoteImplToString() throws GitAPIException, URISyntaxException, IOException, InterruptedException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.remoteAdd().setUri(new URIish("https://example.org/repo.git")).setName(ORIGIN).call();

      commit(git);

      assertThat(new GitMetadataImpl(() -> git).remotes()).allSatisfy(r ->
        assertThat(r.toString()).isNotEmpty().startsWith("RemoteImpl{")
      );
    }
  }

  @Test
  void distanceFromMergeBaseFallsBackWithoutOrigin() throws Exception {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      commit(git);
      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.distanceFromMergeBase()).isEqualTo(pg.distance());
    }
  }

  @Test
  void distanceFromMergeBaseOnHeadBranchIsZero(
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File bareRemote,
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File devDir,
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File workDir
  ) throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = cloneWithOriginHead(bareRemote, devDir, workDir)) {
      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.distanceFromMergeBase()).isEqualTo(0L);
    }
  }

  @Test
  void distanceFromMergeBaseTopicBranchCountsOwnCommits(
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File bareRemote,
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File devDir,
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File workDir
  ) throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = cloneWithOriginHead(bareRemote, devDir, workDir)) {
      git.checkout().setCreateBranch(true).setName("topic/foo").call();
      commit(git);
      commit(git);
      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.distanceFromMergeBase()).isEqualTo(2L);
    }
  }

  @Test
  void distanceFromMergeBaseDetachedHeadFallsBackToDistance(
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File bareRemote,
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File devDir,
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) File workDir
  ) throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    try (var git = cloneWithOriginHead(bareRemote, devDir, workDir)) {
      git.tag().setName("v0.1.1").call();
      var commitA = git.commit().setMessage("A").call();
      git.push().call();
      git.checkout().setName(commitA.getId().getName()).call();
      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.distanceFromMergeBase()).isEqualTo(pg.distance());
    }
  }

  @Test
  void tagFindsLightweightTag() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();
      git.tag().setName("v1.0.0").setAnnotated(false).call();

      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.tag()).isEqualTo("v1.0.0");
    }
  }

  @Test
  void branchIsNullWhenDetached() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      var initial = git.commit().setMessage("initial commit").call();
      git.checkout().setName(initial.getId().getName()).call();

      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.branch()).isNull();
    }
  }

  @Test
  void isShallowCloneFalseForFreshInit() throws GitAPIException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      git.commit().setMessage("initial commit").call();

      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.isShallowClone()).isFalse();
    }
  }

  @Test
  void isShallowCloneTrueWhenShallowFileExists() throws Exception {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      var initial = git.commit().setMessage("initial commit").call();
      var sha = initial.getId().getName();
      Files.writeString(new File(git.getRepository().getDirectory(), "shallow").toPath(), sha, StandardCharsets.UTF_8);

      var pg = new GitMetadataImpl(() -> git);
      assertThat(pg.isShallowClone()).isTrue();
    }
  }

  @Test
  void isShallowCloneTrueForLinkedWorktree(@TempDir(cleanup = CleanupMode.ON_SUCCESS) File worktreeDir)
    throws GitAPIException, IOException, InterruptedException {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      var initial = git.commit().setMessage("initial commit").call();
      var sha = initial.getId().getName();

      var worktree = new ProcessBuilder("git", "worktree", "add", worktreeDir.getAbsolutePath(), "-b", "wt")
        .directory(projectDir)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start();
      var worktreeExit = worktree.waitFor();
      assertThat(worktreeExit).isEqualTo(0);

      Files.writeString(
        new File(git.getRepository().getCommonDirectory(), "shallow").toPath(),
        sha,
        StandardCharsets.UTF_8
      );

      try (var wtGit = Git.open(worktreeDir)) {
        var pg = new GitMetadataImpl(() -> wtGit);
        assertThat(pg.isShallowClone()).isTrue();
      }
    }
  }

  private static Git cloneWithOriginHead(File bareRemote, File devDir, File workDir)
    throws GitAPIException, IOException, InterruptedException, URISyntaxException {
    var uri = bareRemote.toURI().toASCIIString();
    Git.init().setDirectory(bareRemote).setBare(true).call().close();
    try (var dev = Git.init().setDirectory(devDir).call()) {
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
