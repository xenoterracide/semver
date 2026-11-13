// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.IntSupplier;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class SemverCommandTest {

  private static final ThreadLocal<String> STDERR = new ThreadLocal<>();

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File projectDir;

  @AfterEach
  void clearStdErr() {
    STDERR.remove();
  }

  @Test
  void shallowClonePrintsErrorAndReturnsOne() throws Exception {
    var sha = initRepo();

    var shallowFile = new File(projectDir, ".git" + File.separator + "shallow");
    Files.writeString(shallowFile.toPath(), sha, StandardCharsets.UTF_8);

    var exitCode = captureStdErr(() ->
      new CommandLine(new SemverCommand()).execute("--path", projectDir.getAbsolutePath())
    );

    assertThat(exitCode).isEqualTo(1);
    assertThat(capturedErr())
      .contains("shallow clone")
      .contains("git fetch --all --unshallow")
      .contains("fetch-depth: 0");
  }

  @Test
  void nonGitDirectoryPrintsErrorAndReturnsOne() throws Exception {
    var notAGitDir = new File(projectDir, "not-a-repo");
    Files.createDirectories(notAGitDir.toPath());

    var exitCode = captureStdErr(() ->
      new CommandLine(new SemverCommand()).execute("--path", notAGitDir.getAbsolutePath())
    );

    assertThat(exitCode).isEqualTo(1);
    assertThat(capturedErr()).contains("Not a valid Git repository");
  }

  @Test
  void debugFlagCalculatesVersion() throws Exception {
    initRepo();

    var exitCode = new CommandLine(new SemverCommand()).execute("--path", projectDir.getAbsolutePath(), "--debug");

    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  void versionProviderReturnsVersion() throws Exception {
    var version = new SemverCommand.VersionProvider().getVersion();

    assertThat(version).containsExactly("semver 0.0.0");
  }

  @Test
  void runEntryPointCalculatesVersion() throws Exception {
    initRepo();

    var exitCode = SemverCommand.run(new String[] { "--path", projectDir.getAbsolutePath() });

    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  void nullPathPrintsErrorAndReturnsOne() throws Exception {
    var command = new SemverCommand();
    var pathField = SemverCommand.class.getDeclaredField("path");
    pathField.setAccessible(true);
    pathField.set(command, null);

    var exitCode = captureStdErr(command::call);

    assertThat(exitCode).isEqualTo(1);
    assertThat(capturedErr()).contains("Path must not be null");
  }

  private String initRepo() throws Exception {
    try (var git = Git.init().setDirectory(projectDir).call()) {
      var commit = git.commit().setMessage("initial commit").call();
      return commit.getId().getName();
    }
  }

  private static int captureStdErr(IntSupplier command) {
    var err = new ByteArrayOutputStream();
    var originalErr = System.err;
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    try {
      var exitCode = command.getAsInt();
      STDERR.set(err.toString(StandardCharsets.UTF_8));
      return exitCode;
    } finally {
      System.setErr(originalErr);
    }
  }

  private static String capturedErr() {
    return STDERR.get();
  }
}
