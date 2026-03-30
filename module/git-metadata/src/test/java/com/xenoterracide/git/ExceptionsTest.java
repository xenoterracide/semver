// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Exceptions}.
 */
class ExceptionsTest {

  @Test
  void toRuntimeWithRuntimeExceptionReturnsSame() {
    var original = new IllegalStateException("test");

    var result = Exceptions.toRuntime(original);

    assertThat(result).isSameAs(original);
  }

  @Test
  void toRuntimeWithIOExceptionWrapsInUnchecked() {
    var original = new IOException("test");

    var result = Exceptions.toRuntime(original);

    assertThat(result).isInstanceOf(UncheckedIOException.class).hasCause(original);
  }

  @Test
  void toRuntimeWithCheckedExceptionWrapsInRuntime() {
    var original = new Exception("test");

    var result = Exceptions.toRuntime(original);

    assertThat(result)
      .isInstanceOf(RuntimeException.class)
      .isNotInstanceOf(UncheckedIOException.class)
      .hasCause(original);
  }

  @Test
  void toRuntimeWithErrorWrapsInRuntime() {
    var original = new Error("test");

    var result = Exceptions.toRuntime(original);

    assertThat(result).isInstanceOf(RuntimeException.class).hasCause(original);
  }
}
