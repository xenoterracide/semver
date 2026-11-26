// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: PolyForm-Small-Business-1.0.0 OR PolyForm-Noncommercial-1.0.0

package com.xenoterracide.semver.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying the packaged CLI jar reports its implementation version.
 */
class PackagedSemverCommandTest {

  @Test
  void packagedJarReportsImplementationVersion() throws IOException, InterruptedException {
    var jar = findPackagedJar();

    var process = new ProcessBuilder("java", "-jar", jar.toString(), "--version")
      .redirectError(ProcessBuilder.Redirect.DISCARD)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .start();

    var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    var exitCode = process.waitFor();

    assertThat(exitCode).as("java -jar exited successfully").isEqualTo(0);
    assertThat(output).contains("0.0.0").doesNotContain("(development)");
  }

  private static Path findPackagedJar() {
    var target = Path.of("target");
    try (Stream<Path> paths = Files.list(target)) {
      var jars = paths
        .filter(p -> p.getFileName().toString().startsWith("semver-cli-"))
        .filter(p -> p.getFileName().toString().endsWith(".jar"))
        .filter(p -> !p.getFileName().toString().endsWith("-sources.jar"))
        .filter(p -> !p.getFileName().toString().endsWith("-javadoc.jar"))
        .filter(p -> !p.getFileName().toString().endsWith("-test-fixtures.jar"))
        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
        .toList();

      assertThat(jars).as("expected packaged semver-cli jar in %s", target.toAbsolutePath()).isNotEmpty();

      return jars.getFirst();
    } catch (IOException e) {
      throw new AssertionError("failed to list packaged jars in " + target.toAbsolutePath(), e);
    }
  }
}
