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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * PropertyFilter that supports filtering using FieldPaths, also supports skipping of all fields
 * related to sharing.
 *
 * <p>The filter _must_ be set on the ObjectMapper before serialising an object.
 *
 * @author Morten Olav Hansen
 */
@RequiredArgsConstructor
public class FieldFilterSimpleBeanPropertyFilter extends SimpleBeanPropertyFilter {
  private final List<FieldPath> fieldPaths;

  private final boolean skipSharing;

  @Override
  protected boolean include(final BeanPropertyWriter writer) {
    return true;
  }

  @Override
  protected boolean include(final PropertyWriter writer) {
    return true;
  }

  protected boolean include(final PropertyWriter writer, final JsonGenerator jgen) {
    PathValue pathValue = getPath(writer, jgen);
    String path = pathValue.getPath();

    if (pathValue.getValue() == null) {
      return false;
    }

    if (skipSharing
        && StringUtils.equalsAny(
            path,
            "user",
            "publicAccess",
            "externalAccess",
            "userGroupAccesses",
            "userAccesses",
            "sharing")) {
      return false;
    }

    if (pathValue.isInsideMap()) {
      return true;
    }

    for (FieldPath fieldPath : fieldPaths) {
      if (fieldPath.toFullPath().equals(path)) {
        return true;
      }
    }

    return false;
  }

  private PathValue getPath(PropertyWriter writer, JsonGenerator jgen) {
    StringBuilder nestedPath = new StringBuilder();
    JsonStreamContext sc = jgen.getOutputContext();
    Object value = null;
    boolean isInsideMap = false;

    if (sc != null) {
      nestedPath.append(writer.getName());
      value = sc.getCurrentValue();
      sc = sc.getParent();
    }

    while (sc != null) {
      if (sc.getCurrentValue() != null
          && (Map.class.isAssignableFrom(sc.getCurrentValue().getClass())
              || JobParameters.class.isAssignableFrom(sc.getCurrentValue().getClass()))) {
        sc = sc.getParent();
        isInsideMap = true;
        continue;
      }

      if (sc.getCurrentName() != null && sc.getCurrentValue() != null) {
        nestedPath.insert(0, ".");
        nestedPath.insert(0, sc.getCurrentName());
      }

      sc = sc.getParent();
    }

    if (value != null
        && (Map.class.isAssignableFrom(value.getClass())
            || JobParameters.class.isAssignableFrom(value.getClass()))) {
      isInsideMap = true;
    }

    return new PathValue(nestedPath.toString(), value, isInsideMap);
  }

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
      throws Exception {
    if (include(writer, jgen)) {
      writer.serializeAsField(pojo, jgen, provider);
    } else if (!jgen.canOmitFields()) { // since 2.3
      writer.serializeAsOmittedField(pojo, jgen, provider);
    }
  }
}

/** Simple container class used by getPath to handle Maps. */
@Data
@RequiredArgsConstructor
class PathValue {
  private final String path;

  private final Object value;

  private final boolean insideMap;
}
