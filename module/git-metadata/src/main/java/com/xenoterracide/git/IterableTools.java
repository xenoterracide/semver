// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Internal utility for working with iterables.
 */
final class IterableTools {

  private IterableTools() {}

  /**
   * Converts an iterable to a stream.
   *
   * @param iterable the iterable
   * @param <T> the element type
   * @return a stream
   */
  static <T> Stream<T> of(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
