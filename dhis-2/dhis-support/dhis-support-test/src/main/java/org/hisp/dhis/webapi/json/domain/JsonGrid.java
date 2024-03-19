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
package org.hisp.dhis.webapi.json.domain;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;

/**
 * Web API equivalent of a {@link org.hisp.dhis.common.Grid}.
 *
 * @author Jan Bernitt
 */
public interface JsonGrid extends JsonObject {

  default JsonList<JsonGridHeader> getHeaders() {
    return getList("headers", JsonGridHeader.class);
  }

  default JsonArray getRows() {
    return getArray("rows");
  }

  default int getWidth() {
    return getNumber("width").intValue();
  }

  default int getHeight() {
    return getNumber("height").intValue();
  }

  default int getHeaderWidth() {
    return getNumber("headerWidth").intValue();
  }

  default JsonGridMetadata getMetaData() {
    return get("metaData", JsonGridMetadata.class);
  }

  interface JsonGridHeader extends JsonObject {
    default String getName() {
      return getString("name").string();
    }

    default String getColumn() {
      return getString("column").string();
    }

    default ValueType getValueType() {
      return getString("valueType").parsed(ValueType::valueOf);
    }

    default Class<?> getType() {
      return getString("type").parsedClass();
    }

    default boolean isHidden() {
      return getBoolean("hidden").booleanValue();
    }

    default boolean isMeta() {
      return getBoolean("meta").booleanValue();
    }
  }

  interface JsonGridMetadata extends JsonObject {
    default JsonMap<JsonObject> getItems() {
      return getMap("items", JsonObject.class);
    }

    default JsonMap<JsonArray> getDimensions() {
      return getMap("dimensions", JsonArray.class);
    }
  }
}
