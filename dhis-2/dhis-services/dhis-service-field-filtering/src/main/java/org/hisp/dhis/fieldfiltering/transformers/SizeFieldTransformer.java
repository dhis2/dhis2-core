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
package org.hisp.dhis.fieldfiltering.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hisp.dhis.fieldfiltering.FieldTransformer;

/**
 * Field transformer that returns the size of the value of a given key.
 *
 * <p>Usage: "?fields=id,name::size,dataElementGroups::size"
 *
 * @author Morten Olav Hansen
 */
public class SizeFieldTransformer implements FieldTransformer {
  public static final SizeFieldTransformer INSTANCE = new SizeFieldTransformer();

  @Override
  public JsonNode apply(String path, JsonNode value, JsonNode parent) {
    if (!parent.isObject()) {
      return value;
    }

    String fieldName = getFieldName(path);

    if (value.isArray()) {
      ((ObjectNode) parent).put(fieldName, value.size());
    } else if (value.isTextual()) {
      ((ObjectNode) parent).put(fieldName, value.asText().length());
    } else if (value.isInt()) {
      ((ObjectNode) parent).put(fieldName, value.asInt());
    } else if (value.isLong()) {
      ((ObjectNode) parent).put(fieldName, value.asLong());
    } else if (value.isDouble() || value.isFloat()) {
      ((ObjectNode) parent).put(fieldName, value.asDouble());
    }

    return value;
  }
}
