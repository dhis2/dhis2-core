/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.fieldfiltering;

import java.util.List;
import java.util.Set;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

public class FieldPathConverter implements ConditionalGenericConverter {

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return targetType.getResolvableType().getGenerics().length == 1
        && FieldPath.class.equals(targetType.getResolvableType().getGeneric(0).resolve());
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Set.of(
        new ConvertiblePair(String.class, List.class),
        new ConvertiblePair(String[].class, List.class));
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
      return FieldFilterParser.parse(String.join(",", (String[]) source));
    }
    return FieldFilterParser.parse((String) source);
  }
}
