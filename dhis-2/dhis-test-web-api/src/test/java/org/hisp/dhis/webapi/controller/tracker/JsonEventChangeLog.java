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
package org.hisp.dhis.webapi.controller.tracker;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.controller.tracker.view.EventChangeLog;

/** Representation of {@link EventChangeLog}. */
public interface JsonEventChangeLog extends JsonObject {
  default JsonUser getCreatedBy() {
    return get("createdBy").as(JsonUser.class);
  }

  default String getType() {
    return getString("type").string();
  }

  default JsonChange getChange() {
    return get("change").as(JsonChange.class);
  }

  interface JsonChange extends JsonObject {
    default JsonDataValue getDataValue() {
      return get("dataValue").as(JsonDataValue.class);
    }

    default JsonEventField getEventField() {
      return get("eventField").as(JsonEventField.class);
    }
  }

  interface JsonDataValue extends JsonObject {
    default String getDataElement() {
      return getString("dataElement").string();
    }

    default String getPreviousValue() {
      return getString("previousValue").string();
    }

    default String getCurrentValue() {
      return getString("currentValue").string();
    }
  }

  interface JsonEventField extends JsonObject {
    default String getField() {
      return getString("field").string();
    }

    default String getPreviousValue() {
      return getString("previousValue").string();
    }

    default String getCurrentValue() {
      return getString("currentValue").string();
    }
  }
}
