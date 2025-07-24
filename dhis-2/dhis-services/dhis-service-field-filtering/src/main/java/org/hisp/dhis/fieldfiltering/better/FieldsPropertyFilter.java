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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

public class FieldsPropertyFilter extends SimpleBeanPropertyFilter {

  public static final String FILTER_ID = "better-fields-filter";

  /** Key under which fields are stored for filtering during serialization. */
  public static final String FIELDS_ATTRIBUTE = "fields";

  public FieldsPropertyFilter() {
    // Stateless filter - relies on provider attributes
  }

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
      throws Exception {
    Fields current = (Fields) provider.getAttribute(FIELDS_ATTRIBUTE);

    if (current == null) {
      throw new IllegalStateException(
          "No fields attribute found in SerializerProvider. "
              + "Make sure to set it via ObjectWriter.withAttribute()");
    }

    // TODO(ivo) clean this up. and is it expensive to setAttribute?
    if (current.test(writer.getName())) {
      //      if (current.containsChild(writer.getName())) {
      // Set the child for nested serialization
      Fields children = current.getChild(writer.getName());
      provider.setAttribute(FIELDS_ATTRIBUTE, children);
      //      }

      writer.serializeAsField(pojo, jgen, provider);

      // Restore the current predicate after serialization
      provider.setAttribute(FIELDS_ATTRIBUTE, current);
      // TODO(ivo) is this needed? its in the default implementation but feels odd to have an else
      // on "do not includes the property", is this to make sure arrays/objects are closed properly?
    } else if (!jgen.canOmitFields()) { // since 2.3
      writer.serializeAsOmittedField(pojo, jgen, provider);
    }
  }
}
