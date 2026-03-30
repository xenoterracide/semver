// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Objects}.
 */
class ObjectsTest {

  @Test
  void nonNullValueReturnsValue() {
    var value = "test";

    var result = Objects.requireNonNullElseThrow(value, "should not throw");

    assertThat(result).isEqualTo(value);
  }

  @Test
  void nonNullIntegerReturnsValue() {
    var value = 42;

    var result = Objects.requireNonNullElseThrow(value, "should not throw");

    assertThat(result).isEqualTo(value);
  }

  @Test
  void nullValueThrowsWithMessage() {
    String value = null;

    assertThatThrownBy(() -> Objects.requireNonNullElseThrow(value, "custom error message"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("custom error message");
  }

  @Test
  void nullValueEmptyMessage() {
    String value = null;

    assertThatThrownBy(() -> Objects.requireNonNullElseThrow(value, ""))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("");
  }
}
