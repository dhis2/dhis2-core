/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.gist;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.PrimaryKeyObject;

import static java.util.Objects.requireNonNull;

/**
 * Data types to process a Gist API object list request.
 *
 * @author Jan Bernitt
 * @since 2.43
 * @param pager for the contained page of values
 * @param paths
 * @param values the objects matching the query given as an array of values for the requested
 *     fields/properties
 */
public record GistObjectList(
    @Nonnull GistPager pager,
    @Nonnull List<String> paths,
    @Nonnull List<Type> valueTypes,
    @Nonnull Stream<Object[]> values) {

  public record Type(@Nonnull Class<?> rawType, @CheckForNull Class<?> elementType) {
    Type(@Nonnull Class<?> rawType) {
      this(rawType, null);
    }
  }

  /**
   * Input to the query.
   *
   * @param elementType
   * @param autoDefault
   * @param contextRoot
   * @param requestURL
   * @param pager
   * @param params
   */
  public record Input(
      @Nonnull Class<? extends PrimaryKeyObject> elementType,
      @Nonnull GistAutoType autoDefault,
      @CheckForNull String contextRoot,
      @CheckForNull String requestURL,
      @Nonnull GistParams params) {
    public Input {
      requireNonNull(elementType);
      requireNonNull(autoDefault);
      requireNonNull(params);
    }
  }

  /**
   * Data output of the query before it gets serialized.
   *
   * @param headless true, to not wrap the result list in an object with pager
   * @param pager the pager information to render
   * @param collectionName name of the array of values in the wrapper object
   * @param paths the fields/properties each value object has
   * @param valueTypes the types of the fields/properties in Java
   * @param values a stream of value object of the result list
   */
  public record Output(
      boolean headless,
      @CheckForNull GistPager pager,
      @Nonnull String collectionName,
      @Nonnull List<String> paths,
      @Nonnull List<Type> valueTypes,
      @Nonnull Stream<Object[]> values) {

    public Output {
      requireNonNull(collectionName);
      requireNonNull(paths);
      requireNonNull(valueTypes);
      requireNonNull(values);
      if (paths.size() != valueTypes.size())
        throw new IllegalArgumentException("path and valueType list must be of equal length");
    }
  }
}
