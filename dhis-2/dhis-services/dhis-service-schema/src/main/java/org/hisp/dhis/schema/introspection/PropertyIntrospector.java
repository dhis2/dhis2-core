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

import java.util.Map;
import org.hisp.dhis.schema.Property;

/**
 * A {@link PropertyIntrospector} is a function to extract or modify {@link Property} values from a
 * {@link Class} or additional information associated with that {@link Class} to build a {@link
 * org.hisp.dhis.schema.Schema}.
 *
 * <p>The main sources of information are Hibernate, provided by {@link
 * HibernatePropertyIntrospector} and Jackson, provided by {@link JacksonPropertyIntrospector}.
 *
 * <p>The baseline created by the main sources is enriched by the {@link
 * PropertyPropertyIntrospector} and {@link TranslatablePropertyIntrospector} which look for
 * optional annotations.
 *
 * @author Jan Bernitt
 */
@FunctionalInterface
public interface PropertyIntrospector {

  /**
   * Adds or modifies the provided {@link Property} map for the provided {@link Class}.
   *
   * @param klass The type to introspect
   * @param properties the intermediate and result state. {@link PropertyIntrospector} running
   *     before this one might already have added to the provided map. This {@link
   *     PropertyIntrospector} can add or modify the {@link Property} entries further.
   */
  void introspect(Class<?> klass, Map<String, Property> properties);

  /**
   * Allows to chain multiple {@link PropertyIntrospector}s into a sequence.
   *
   * @param next the {@link PropertyIntrospector} called after this one
   * @return A {@link PropertyIntrospector} that first calls this instance and then the next
   *     instance
   */
  default PropertyIntrospector then(PropertyIntrospector next) {
    return (klass, properties) -> {
      introspect(klass, properties);
      next.introspect(klass, properties);
    };
  }
}
