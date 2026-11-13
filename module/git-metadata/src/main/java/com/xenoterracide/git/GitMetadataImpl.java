// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;

import io.vavr.control.Try;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of GitMetadata.
 */
final class GitMetadataImpl implements GitMetadata {

  /** Glob pattern for matching version tags (not a regex). */
  private static final String VERSION_GLOB = "v[0-9]*.[0-9]*.[0-9]*";

  /** Separator used in Git ref paths. */
  private static final String GIT_SEPARATOR = "/";

  /** Logger instance for this class. */
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  /** The TryGit instance for Git operations. */
  private final TryGit git;

  GitMetadataImpl(TryGit tryGit) {
    this.git = tryGit;
  }

  static <T> Function<? super Throwable, ? extends T> allWith(@Nullable T value) {
    return e ->
      Match(e)
        .option(
          Case($(instanceOf(RefNotFoundException.class)), value),
          Case($(instanceOf(RepositoryNotFoundException.class)), value)
        )
        .getOrElseThrow(() -> Exceptions.toRuntime(e));
  }

  Try<Repository> gitRepository() {
    return Try.of(this.git::get)
      .filter(Objects::nonNull)
      .map(Git::getRepository)
      .onFailure(e -> this.log.error("failed to get repository", e));
  }

  /**
   * Gets branch.
   *
   * @return the branch, or null when HEAD is detached
   */
  @Override
  public @Nullable String branch() {
    return this.gitRepository()
      .mapTry(Repository::getFullBranch)
      .filter(Objects::nonNull)
      .filter(fullBranch -> fullBranch.startsWith(Constants.R_HEADS))
      .map(fullBranch -> fullBranch.substring(Constants.R_HEADS.length()))
      .recover(NoSuchElementException.class, e -> null)
      .getOrNull();
  }

  /**
   * Gets object id for.
   *
   * @param shalike
   *   the shalike
   * @return the object id for
   */
  public Try<ObjectId> getObjectIdFor(@NonNull String shalike) {
    return this.gitRepository().mapTry(r -> r.resolve(Objects.requireNonNull(shalike)));
  }

  /**
   * Gets the full revision SHA for the given SHA-like string.
   *
   * @param shalike the SHA-like string to resolve
   * @return the full 40-character SHA, or null if not found
   */
  public @Nullable String getRev(@NonNull String shalike) {
    return this.getObjectIdFor(shalike).map(AnyObjectId::getName).getOrNull();
  }

  @Override
  public @Nullable String uniqueShort() {
    return this.gitRepository()
      .mapTry(Repository::newObjectReader)
      .mapTry(reader -> reader.abbreviate(this.getObjectIdFor(Constants.HEAD).get()))
      .map(AbbreviatedObjectId::name)
      .recover(NoSuchElementException.class, e -> null)
      .onFailure(e -> this.log.error("failed to get unique short", e))
      .getOrNull();
  }

  @Override
  public @Nullable String tag() {
    return this.git
      .tryCommand(g -> g.describe().setMatch(VERSION_GLOB).setAbbrev(0).setTags(true))
      .recover(NoSuchElementException.class, e -> null)
      .recover(GitMetadataImpl.allWith(null))
      .onFailure(e -> this.log.error("failed to get tag", e))
      .getOrNull();
  }

  @Override
  public boolean isShallowClone() {
    return this.gitRepository()
      .mapTry(r -> {
        var dir = r.getCommonDirectory();
        return dir != null && new File(dir, "shallow").length() > 0;
      })
      .getOrElse(false);
  }

  @Override
  public long distance() {
    return Try.of(() -> new DistanceCalculator(this.git).apply(Constants.HEAD))
      .recover(NoSuchElementException.class, 0L)
      .recover(RepositoryNotFoundException.class, 0L)
      .getOrElseThrow(Exceptions::toRuntime);
  }

  @Override
  public long distanceFromMergeBase() {
    var onBranch = this.gitRepository()
      .mapTry(Repository::getFullBranch)
      .map(fullBranch -> fullBranch != null && fullBranch.startsWith(Constants.R_HEADS))
      .getOrElse(false);
    if (!onBranch) {
      return this.distance();
    }
    return this.remotes()
      .stream()
      .filter(remote -> "origin".equals(remote.name()))
      .findFirst()
      .flatMap(remote ->
        this.gitRepository()
          .map(repo -> new MergeBaseFinder(repo).find(remote))
          .onFailure(e -> this.log.error("failed to find merge base", e))
          .getOrElse(Optional.empty())
      )
      .map(mergeBase ->
        this.getObjectIdFor(Constants.HEAD)
          .map(head -> new DistanceCalculator(this.git).distanceBetween(mergeBase, head))
          .getOrElse(this::distance)
      )
      .orElseGet(this::distance);
  }

  @Override
  public GitStatus status() {
    return this.git
      .tryCommand(Git::status)
      .filter(Objects::nonNull)
      .map(status -> status.isClean() ? GitStatus.CLEAN : GitStatus.DIRTY)
      .recover(NoSuchElementException.class, e -> GitStatus.NO_REPO)
      .recover(RepositoryNotFoundException.class, e -> GitStatus.NO_REPO)
      .onFailure(e -> this.log.error("failed to get status", e))
      .getOrElseThrow(Exceptions::toRuntime);
  }

  /**
   * Full 40 character commit SHA.
   *
   * @return sha
   */
  @Override
  public @Nullable String commit() {
    return this.getRev(Constants.HEAD);
  }

  @Override
  public List<GitRemote> remotes() {
    return this.git
      .tryCommand(Git::remoteList)
      .map(Collection::stream)
      .map(s -> s.map(RemoteConfig::getName))
      .map(s -> s.filter(Objects::nonNull))
      .map(s -> s.map(name -> RemoteImpl.nullCheck(name, this.headBranch(name))))
      .map(s -> s.collect(Collectors.<GitRemote>toList()))
      .onFailure(e -> this.log.error("failed to get remotes", e))
      .getOrElse(ArrayList::new);
  }

  @Nullable
  String headBranch(String remote) {
    var remoteRef = Constants.R_REMOTES + remote + GIT_SEPARATOR + Constants.HEAD;
    return this.gitRepository()
      .mapTry(r -> r.findRef(remoteRef))
      .filter(Objects::nonNull)
      .filter(Ref::isSymbolic)
      .map(Ref::getLeaf)
      .map(Ref::getName)
      .recover(NoSuchElementException.class, e -> null)
      .onFailure(e -> this.log.error("failed to get HEAD branch", e))
      .getOrNull();
  }

  private static class RemoteImpl implements GitRemote {

    private final String name;
    private final @Nullable String headBranchRefName;

    RemoteImpl(String name, @Nullable String headBranchRefName) {
      this.name = name;
      this.headBranchRefName = headBranchRefName;
    }

    static GitRemote nullCheck(String name, @Nullable String headBranchRefName) {
      return new RemoteImpl(Objects.requireNonNull(name), headBranchRefName);
    }

    @Override
    public @Nullable String headBranchRefName() {
      return this.headBranchRefName;
    }

    @Override
    public String name() {
      return this.name;
    }

    @Override
    public String toString() {
      return "RemoteImpl{name=" + this.name + ", headBranchRefName=" + this.headBranchRefName + "}";
    }
  }
}
