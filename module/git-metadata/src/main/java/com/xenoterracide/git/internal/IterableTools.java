// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: GPL-3.0-or-later WITH Classpath-exception-2.0

package com.xenoterracide.git.internal;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Internal utility for working with iterables.
 */
public final class IterableTools {

  private IterableTools() {}

  /**
   * Converts an iterable to a stream.
   *
   * @param iterable the iterable
   * @param <T> the element type
   * @return a stream
   */
  public static <T> Stream<T> of(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
