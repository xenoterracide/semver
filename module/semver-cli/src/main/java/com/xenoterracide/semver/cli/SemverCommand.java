// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver.cli;

import com.xenoterracide.git.GitMetadataFactory;
import com.xenoterracide.git.GitStatus;
import com.xenoterracide.semver.VersionCalculator;
import java.io.File;
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

    var metadata = this.path == null ? null : GitMetadataFactory.create(this.path);
    if (metadata == null || metadata.status() == GitStatus.NO_REPO) {
      this.reportError();
      return 1;
    }

    System.out.println(VersionCalculator.calculate(metadata));

    return 0;
  }

  private void reportError() {
    if (this.path == null) {
      System.err.println("Error: Path must not be null");
    } else {
      System.err.println("Error: Not a valid Git repository: " + this.path);
    }
  }

  private void configureLogging() {
    String level = this.debug ? "debug" : "warn";
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
  }

  // CHECKSTYLE.OFF: UncommentedMain
  /**
   * Main entry point for the CLI.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    var cmd = new CommandLine(new SemverCommand());
    var exitCode = cmd.execute(args);
    System.exit(exitCode);
  }

  // CHECKSTYLE.ON: UncommentedMain

  /**
   * Provides version information for the CLI.
   */
  public static final class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
      return new String[] { "semver 0.0.0" };
    }
  }
}
