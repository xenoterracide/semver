// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: PolyForm-Small-Business-1.0.0 OR PolyForm-Noncommercial-1.0.0

package com.xenoterracide.git;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitStatusTest {

  @Test
  void toStringReturnsCamelCase() {
    assertThat(GitStatus.CLEAN.toString()).isEqualTo("clean");
    assertThat(GitStatus.DIRTY.toString()).isEqualTo("dirty");
    assertThat(GitStatus.NO_REPO.toString()).isEqualTo("noRepo");
  }
}
