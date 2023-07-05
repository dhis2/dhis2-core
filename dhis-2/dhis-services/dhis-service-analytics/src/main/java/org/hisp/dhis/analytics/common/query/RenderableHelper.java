/*
 * Copyright (c) 2004-2004, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.common.query;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Helper class that provides methods responsible for assisting with specific tasks related to
 * {@link Renderable} objects.
 */
@NoArgsConstructor(access = PRIVATE)
public class RenderableHelper {

  /** A {@link Renderable} object that always returns "false". */
  public static final Renderable FALSE_CONDITION = () -> "false";

  /**
   * Joins the given {@link Renderable} objects using the given delimiter.
   *
   * @param renderables a collection of {@link Renderable} objects.
   * @param delimiter a delimiter string.
   * @return a string of rendered objects joined by the delimiter.
   */
  public static String join(Collection<? extends Renderable> renderables, String delimiter) {
    return join(renderables, delimiter, EMPTY);
  }

  /**
   * Joins the given {@link Renderable} objects using the given delimiter and prefix.
   *
   * @param renderables a collection of {@link Renderable} objects.
   * @param delimiter a delimiter string.
   * @param prefix a prefix string.
   * @return a string of rendered objects joined by the delimiter and prefixed by the given prefix.
   */
  public static String join(
      Collection<? extends Renderable> renderables, String delimiter, String prefix) {
    return join(renderables, delimiter, prefix, EMPTY);
  }

  /**
   * Renders the given {@link Renderable} objects. Returns an empty list if the given collection is
   * null or contains only null/blank values.
   *
   * @param renderables the collection of {@link Renderable}.
   * @return a list of rendered strings.
   */
  public static List<String> renderCollection(Collection<? extends Renderable> renderables) {
    return emptyIfNull(renderables).stream()
        .filter(Objects::nonNull)
        .map(Renderable::render)
        .filter(StringUtils::isNotBlank)
        .collect(toList());
  }

  /**
   * Joins the given {@link Renderable} objects using the given delimiter, prefix and suffix.
   *
   * @param renderables a collection of {@link Renderable} objects.
   * @param delimiter a delimiter string.
   * @param prefix a prefix string.
   * @param suffix a suffix string.
   * @return a string of rendered objects joined by the delimiter and prefixed by the given prefix
   *     and suffixed by the given suffix.
   */
  public static String join(
      Collection<? extends Renderable> renderables,
      String delimiter,
      String prefix,
      String suffix) {
    List<String> renderedList = renderCollection(renderables);

    if (isNotEmpty(renderedList)) {
      return renderedList.stream().collect(joining(delimiter, prefix, suffix));
    }

    return EMPTY;
  }
}
