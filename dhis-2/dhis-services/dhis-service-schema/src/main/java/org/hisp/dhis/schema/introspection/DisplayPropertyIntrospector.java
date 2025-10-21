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
package org.hisp.dhis.schema.introspection;

import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.schema.Property;

/**
 * A {@link PropertyIntrospector} that sets translation keys for display properties (displayName,
 * displayDescription, displayShortName) so they can be used for database-level filtering and
 * sorting via JSONB functions.
 *
 * <p>Note: This introspector does NOT mark these properties as persisted. The persisted flag is
 * dynamically set by {@link org.hisp.dhis.query.planner.DefaultQueryPlanner} when needed.
 *
 * @author Viet Nguyen
 */
public class DisplayPropertyIntrospector implements PropertyIntrospector {

  private static final Set<String> DISPLAY_PROPERTIES =
      Set.of("displayName", "displayDescription", "displayShortName");

  private static final Map<String, String> DISPLAY_TO_TRANSLATION_KEY =
      Map.of(
          "displayName", "NAME",
          "displayDescription", "DESCRIPTION",
          "displayShortName", "SHORT_NAME");

  @Override
  public void introspect(Class<?> klass, Map<String, Property> properties) {
    // Only process classes that have a translations property
    if (!hasTranslationsProperty(properties)) {
      return;
    }

    for (String displayPropertyName : DISPLAY_PROPERTIES) {
      Property displayProperty = properties.get(displayPropertyName);
      if (displayProperty != null) {
        // Mark as translatable and set translation key so JpaCriteriaQueryEngine knows which key to use
        displayProperty.setTranslatable(true);
        displayProperty.setTranslationKey(DISPLAY_TO_TRANSLATION_KEY.get(displayPropertyName));
      }
    }
  }

  /**
   * Checks if the given properties map contains a translations property.
   *
   * @param properties the properties map to check.
   * @return true if a translations property is present, false otherwise.
   */
  private boolean hasTranslationsProperty(Map<String, Property> properties) {
    Property property = properties.get(BaseIdentifiableObject_.TRANSLATIONS);
    return property != null && property.isPersisted();
  }
}
