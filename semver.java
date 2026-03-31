// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

//JAVA 17+
//REPOS mavencentral,mavenlocal
//DEPS info.picocli:picocli:4.7.7
//DEPS org.eclipse.jgit:org.eclipse.jgit:7.6.0.202603022253-r
//DEPS org.semver4j:semver4j:6.0.0
//DEPS io.vavr:vavr:1.0.1
//DEPS com.google.guava:guava:33.5.0-jre
//DEPS org.apache.commons:commons-lang3:3.20.0
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS org.jspecify:jspecify:1.0.0

// Local modules (requires 'mvn install' to be run first)
//DEPS com.xenoterracide:git-metadata:0.0.0
//DEPS com.xenoterracide:semver-calculator:0.0.0

import com.xenoterracide.git.GitMetadata;
import com.xenoterracide.git.GitMetadataFactory;
import com.xenoterracide.git.GitStatus;
import com.xenoterracide.semver.VersionCalculator;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

/**
 * Semver CLI - Calculate semantic versions from Git repository metadata.
 *
 * <p>Usage examples:</p>
 * <pre>
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
 * </pre>
 */
@Command(
  name = "semver",
  mixinStandardHelpOptions = true,
  versionProvider = SemverCommand.VersionProvider.class,
  description = "Calculate semantic versions from Git repository metadata."
)
class SemverCommand implements Callable<Integer> {

  @Option(
    names = { "--path", "-p" },
    description = "Path to the Git repository (default: current directory)",
    defaultValue = "."
  )
  private File path;

  @Option(names = { "--debug", "-d" }, description = "Enable debug logging")
  private boolean debug;

  @Override
  public Integer call() {
    if (this.debug) {
      System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    } else {
      System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    var metadata = createMetadata(this.path);
    if (metadata == null) {
      System.err.println("Error: Not a valid Git repository: " + this.path);
      return 1;
    }

    if (metadata.status() == GitStatus.NO_REPO) {
      System.err.println("Error: Not a valid Git repository: " + this.path);
      return 1;
    }

    var version = VersionCalculator.calculate(metadata);
    System.out.println(version);

    return 0;
  }

  private static GitMetadata createMetadata(File path) {
    return GitMetadataFactory.create(path);
  }

  static class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
      return new String[] { "semver 0.0.0" };
    }
  }

  public static void main(String[] args) {
    var cmd = new CommandLine(new SemverCommand());
    var exitCode = cmd.execute(args);
    System.exit(exitCode);
  }
}
