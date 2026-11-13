/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.security.acl;

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * The attribute option combos (AOCs) a user may read data of, as resolved by {@link
 * AccessControlStore}, and the one way data queries filter on them.
 *
 * <p>A query filters with {@link #condition(String)} and binds {@link #ids()} as {@link
 * #PARAMETER}. When the user is not restricted, {@link #ids()} is null: queries built with {@code
 * QueryBuilder#eraseNullParameterLines} then drop the condition line, other queries leave it out
 * ({@link #isUnrestricted()}). When the user may read no AOC at all ({@link #isNone()}), there is
 * no data to query.
 */
public final class DataReadableAocs {

  /** Name of the array parameter holding the internal ids of the readable AOCs. */
  public static final String PARAMETER = "dataReadableAocs";

  private static final DataReadableAocs UNRESTRICTED = new DataReadableAocs(null);

  @CheckForNull private final long[] ids;

  private DataReadableAocs(@CheckForNull long[] ids) {
    this.ids = ids;
  }

  /** A user who may read the data of all AOCs, a superuser. */
  @Nonnull
  public static DataReadableAocs unrestricted() {
    return UNRESTRICTED;
  }

  /**
   * @param ids internal ids of the AOCs the user may read data of, empty when none
   */
  @Nonnull
  public static DataReadableAocs of(@Nonnull long[] ids) {
    return new DataReadableAocs(ids.clone());
  }

  /**
   * A condition on one line limiting the AOC column to the AOCs bound as {@link #PARAMETER}: a hash
   * semi join over the bound array, which PostgreSQL estimates like no filter at all. Joining a
   * sharing-filtered set of category option combos instead gives the planner a result size it
   * cannot estimate (DHIS2-22400).
   *
   * @param aocColumn qualified AOC id column, e.g. {@code dv.attributeoptioncomboid}
   */
  @Nonnull
  public static String condition(@Nonnull String aocColumn) {
    return "EXISTS (SELECT 1 FROM unnest(CAST(:%s AS bigint[])) data_readable_aoc(id) WHERE data_readable_aoc.id = %s)"
        .formatted(PARAMETER, aocColumn);
  }

  /** True when the user may read the data of all AOCs: queries do not filter on AOCs. */
  public boolean isUnrestricted() {
    return ids == null;
  }

  /** True when the user may read the data of no AOC: there is no data to query. */
  public boolean isNone() {
    return ids != null && ids.length == 0;
  }

  /**
   * @return internal ids of the readable AOCs to bind as {@link #PARAMETER}, null when unrestricted
   */
  @CheckForNull
  public long[] ids() {
    return ids == null ? null : ids.clone();
  }

  @Override
  public String toString() {
    return ids == null
        ? "DataReadableAocs[unrestricted]"
        : "DataReadableAocs" + Arrays.toString(ids);
  }
}
