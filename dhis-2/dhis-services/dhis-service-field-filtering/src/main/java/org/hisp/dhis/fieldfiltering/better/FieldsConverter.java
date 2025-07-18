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
package org.hisp.dhis.fieldfiltering.better;

import java.util.Set;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Spring converter that converts field filter strings to Fields. This allows controller methods to
 * directly accept Fields parameters from @RequestParam fields values.
 */
public class FieldsConverter implements ConditionalGenericConverter {
  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return Fields.class.equals(targetType.getResolvableType().resolve());
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Set.of(
        new ConvertiblePair(String.class, Fields.class),
        new ConvertiblePair(String[].class, Fields.class));
  }

  @Override
  public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (sourceType.isArray()) {
      /*
       * Undo Spring's splitting of
       * {@code fields=attributes[attribute,value],deleted} into
       * <ul>
       * <li>0 = "attributes[attribute"</li>
       * <li>1 = "value]"</li>
       * <li>2 = "deleted"</li>
       * </ul>
       * separating nested fields attribute and value.
       */
      return FieldsParser.parse(String.join(",", (String[]) source));
    }
    return FieldsParser.parse((String) source);
  }
}
