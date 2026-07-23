/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.jsonpatch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import java.util.Set;

/**
 * PropertyFilter that omits properties named in {@code excluded} without invoking their getter, via
 * {@link PropertyWriter#serializeAsOmittedField}. Mirrors {@code
 * org.hisp.dhis.fieldfiltering.FieldFilterSimpleBeanPropertyFilter}'s shape, simplified to a flat
 * excluded-name set (no path-context tracking needed here).
 *
 * @author Jason Pickering
 */
public class JsonPatchExcludedPropertyFilter extends SimpleBeanPropertyFilter {

  /**
   * Single source of truth for the filter id -- referenced by both {@link JsonPatchFilterMixin}'s
   * {@code @JsonFilter} annotation and {@link JsonPatchManager}'s {@code addFilter} call. Declared
   * here, on a concrete class, rather than on the mixin interface itself: an interface holding only
   * a constant is exactly the "constant interface" anti-pattern (SonarQube java:S1214) --
   * implementing classes would inherit the constant into their own namespace for no reason. {@code
   * JsonPatchFilterMixin} stays a truly empty marker interface, matching its precedent, {@code
   * org.hisp.dhis.fieldfiltering.FieldFilterMixin}.
   */
  public static final String ID = "json-patch-collection-filter";

  private final Set<String> excluded;

  public JsonPatchExcludedPropertyFilter(Set<String> excluded) {
    this.excluded = excluded;
  }

  @Override
  protected boolean include(final BeanPropertyWriter writer) {
    return true;
  }

  @Override
  protected boolean include(final PropertyWriter writer) {
    return true;
  }

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
      throws Exception {
    if (excluded.contains(writer.getName())) {
      if (!jgen.canOmitFields()) {
        writer.serializeAsOmittedField(pojo, jgen, provider);
      }
    } else {
      writer.serializeAsField(pojo, jgen, provider);
    }
  }
}
