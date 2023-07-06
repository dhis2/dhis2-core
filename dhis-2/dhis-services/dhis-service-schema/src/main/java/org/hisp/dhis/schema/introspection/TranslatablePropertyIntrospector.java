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
package org.hisp.dhis.schema.introspection;

import com.google.common.base.CaseFormat;
import java.util.Map;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.system.util.AnnotationUtils;

/**
 * A {@link PropertyIntrospector} that adds information to existing {@link Property} values if they
 * are annotated with {@link org.hisp.dhis.translation.Translatable}.
 *
 * @author Jan Bernitt (extracted from {@link JacksonPropertyIntrospector})
 */
public class TranslatablePropertyIntrospector implements PropertyIntrospector {
  @Override
  public void introspect(Class<?> klass, Map<String, Property> properties) {
    Property translationsProperty = properties.get(BaseIdentifiableObject_.TRANSLATIONS);

    if (translationsProperty == null || !translationsProperty.isPersisted()) {
      return;
    }

    Map<String, String> translatableFields = AnnotationUtils.getTranslatableAnnotatedFields(klass);

    for (Property property : properties.values()) {
      if (property.isPersisted() && translatableFields.containsKey(property.getFieldName())) {
        property.setTranslatable(true);
        property.setTranslationKey(translatableFields.get(property.getFieldName()));
        String i18nKey =
            CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, property.getFieldName());
        property.setI18nTranslationKey(i18nKey);
      }

      /** Embedded objects can have their own translatable properties. */
      if (property.isEmbeddedObject()
          && !AnnotationUtils.getTranslatableAnnotatedFields(property.getKlass()).isEmpty()) {
        property.setTranslatable(true);
      }
    }
  }
}
