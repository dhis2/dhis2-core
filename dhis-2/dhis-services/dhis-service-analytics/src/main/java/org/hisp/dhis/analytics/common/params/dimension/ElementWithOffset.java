/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.params.dimension;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Objects;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UidObject;

/**
 * Encapsulates and element T with its offset.
 *
 * @param <T> the dimension type.
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class ElementWithOffset<T extends UidObject> {
  @SuppressWarnings("rawtypes")
  private static final ElementWithOffset EMPTY_ELEMENT_WITH_OFFSET =
      ElementWithOffset.of(null, null);

  private final T element;

  private final Integer offset;

  public static <T extends UidObject> ElementWithOffset<T> of(T element) {
    return ElementWithOffset.of(element, null);
  }

  public boolean hasOffset() {
    return Objects.nonNull(offset);
  }

  @SuppressWarnings("unchecked")
  public static <R extends UidObject> ElementWithOffset<R> emptyElementWithOffset() {
    return EMPTY_ELEMENT_WITH_OFFSET;
  }

  /**
   * Returns the current offset. If there is none, the number "0" (zero) is returned.
   *
   * @return the current offset, or "0" (zero) as default value.
   */
  public Integer getOffsetWithDefault() {
    return hasOffset() ? offset : 0;
  }

  public boolean isPresent() {
    return Objects.nonNull(element);
  }

  @Override
  public String toString() {
    if (isPresent()) {
      if (hasOffset()) {
        return element.getUid() + "[" + offset + "]";
      }
      return element.getUid();
    }
    return EMPTY;
  }
}
