// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver.cli;

import com.xenoterracide.git.GitMetadata;
import com.xenoterracide.git.GitMetadataFactory;
import com.xenoterracide.git.GitStatus;
import com.xenoterracide.semver.VersionCalculator;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

/**
 * Semver CLI - Calculate semantic versions from Git repository metadata.
 *
 * <p>Usage examples:</p>
 * {@snippet lang="sh" :
 * # Print version for current directory
 * jbang semver.java
 *
 * # Print version for specific repository
 * jbang semver.java --path /path/to/repo
 *
 * # Enable debug logging
 * jbang semver.java --debug
 *
 * # Use in shell scripts
 * VERSION=$(jbang semver.java)
 * mvn versions:set -DnewVersion="$VERSION"
 * }
 */
@Command(
  name = "semver",
  mixinStandardHelpOptions = true,
  versionProvider = SemverCommand.VersionProvider.class,
  description = "Calculate semantic versions from Git repository metadata."
)
public final class SemverCommand implements Callable<Integer> {

  /** Path to the Git repository. */
  @Option(
    names = { "--path", "-p" },
    description = "Path to the Git repository (default: current directory)",
    defaultValue = "."
  )
  private @Nullable File path;

  /** Enable debug logging flag. */
  @Option(names = { "--debug", "-d" }, description = "Enable debug logging")
  private boolean debug;

  @Override
  public Integer call() {
    this.configureLogging();

    if (this.path == null) {
      System.err.println("Error: Path must not be null");
      return 1;
    }

    var metadata = createMetadata(this.path);
    var error = validateMetadata(this.path, metadata);
    if (error != null) {
      System.err.println(error);
      return 1;
    }

    return calculateVersion(Objects.requireNonNull(metadata));
  }

  private static int calculateVersion(GitMetadata metadata) {
    try {
      var version = VersionCalculator.calculate(metadata);
      System.out.println(version);
      return 0;
    } catch (IllegalStateException e) {
      System.err.println("Error: " + e.getMessage());
      return 1;
    }
  }

  private static @Nullable String validateMetadata(File path, @Nullable GitMetadata metadata) {
    if (metadata == null || metadata.status() == GitStatus.NO_REPO) {
      return "Error: Not a valid Git repository: " + path;
    }
    return null;
  }

  private void configureLogging() {
    String level = this.debug ? "debug" : "warn";
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
  }

  private static @Nullable GitMetadata createMetadata(File path) {
    return GitMetadataFactory.create(path);
  }

  // CHECKSTYLE.OFF: UncommentedMain - Main method is the entry point for CLI
  /**
   * Main entry point for the CLI.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    System.exit(run(args));
  }

  static int run(String[] args) {
    return new CommandLine(new SemverCommand()).execute(args);
  }

  // CHECKSTYLE.ON: UncommentedMain

  /**
   * Provides version information for the CLI.
   */
  public static final class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
      var version = SemverCommand.class.getPackage().getImplementationVersion();
      return new String[] { "semver " + (version != null ? version : "0.0.0 (development)") };
    }
  }
}
