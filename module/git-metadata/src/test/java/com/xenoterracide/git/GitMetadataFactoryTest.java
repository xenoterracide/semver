// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import static org.assertj.core.api.Assertions.assertThat;

import com.xenoterracide.git.fixtures.CommitTools;
import java.io.File;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link GitMetadataFactory}.
 */
class GitMetadataFactoryTest {

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  File tempDir;

  @Test
  void createWithValidRepoReturnsMetadata() throws Exception {
    var git = Git.init().setDirectory(this.tempDir).call();
    CommitTools.commit(git);

    var metadata = GitMetadataFactory.create(this.tempDir);

    assertThat(metadata).isNotNull();
    assertThat(metadata.status()).isEqualTo(GitStatus.CLEAN);
  }

  @Test
  void createWithStringPathReturnsMetadata() throws Exception {
    var git = Git.init().setDirectory(this.tempDir).call();
    CommitTools.commit(git);

    var metadata = GitMetadataFactory.create(this.tempDir.getAbsolutePath());

    assertThat(metadata).isNotNull();
    assertThat(metadata.status()).isEqualTo(GitStatus.CLEAN);
  }

  @Test
  void createWithInvalidRepoReturnsNull() {
    var notARepo = new File(this.tempDir, "not-a-repo");
    notARepo.mkdirs();

    var metadata = GitMetadataFactory.create(notARepo);

    assertThat(metadata).isNull();
  }

  @Test
  void createWithNonExistentPathReturnsNull() {
    var nonExistent = new File("/does/not/exist");

    var metadata = GitMetadataFactory.create(nonExistent);

    assertThat(metadata).isNull();
  }
}
