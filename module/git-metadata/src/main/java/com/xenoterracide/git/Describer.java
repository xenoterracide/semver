// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import com.google.common.base.Splitter;
import io.vavr.CheckedFunction1;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.InvalidPatternException;
import org.eclipse.jgit.lib.ObjectId;
import org.jspecify.annotations.Nullable;

/**
 * Describes a git commit using git describe command.
 */
class Describer implements CheckedFunction1<Git, Describer.Described> {

  private static final long serialVersionUID = 1L;

  /** Splitter for parsing git describe output. */
  private static final Splitter DESCRIBE_SPLITTER = Splitter.on('-');

  /** Glob pattern for matching version tags. */
  private static final String VERSION_GLOB = "v[0-9]*.[0-9]*.[0-9]*";

  /** The object ID to describe. */
  private final ObjectId oid;

  /**
   * Creates a new Describer for the given object ID.
   *
   * @param objectId the object ID to describe
   */
  Describer(ObjectId objectId) {
    this.oid = objectId;
  }

  /**
   * Creates a describe function for the given object ID.
   *
   * @param objectId the object ID to describe
   * @return a function that describes the object
   */
  static CheckedFunction1<Git, Describer.Described> describe(ObjectId objectId) {
    return new Describer(objectId);
  }

  @Override
  public @Nullable Described apply(Git git) throws InvalidPatternException, IOException, GitAPIException {
    var cmd = git.describe().setMatch(VERSION_GLOB).setLong(true).setTags(true).setTarget(this.oid);
    var desc = cmd.call();
    return desc != null ? new Described(DESCRIBE_SPLITTER.splitToList(desc)) : null;
  }

  /** Value object representing parsed git describe output. */
  static class Described {

    /** The parsed parts of the describe output. */
    private final List<String> parts;

    /**
     * Creates a new Described instance.
     *
     * @param partsList the parsed parts of the describe output
     */
    Described(List<String> partsList) {
      this.parts = partsList;
    }

    /**
     * Gets the distance from the nearest tag.
     *
     * @return the distance in commits
     */
    long distance() {
      return this.parts.size() > 2 ? Long.parseLong(this.parts.get(this.parts.size() - 2)) : 0;
    }
  }
}
