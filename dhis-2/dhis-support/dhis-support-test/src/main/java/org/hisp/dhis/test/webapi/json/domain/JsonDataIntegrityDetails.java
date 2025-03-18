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
package org.hisp.dhis.test.webapi.json.domain;

import java.time.LocalDateTime;
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.Required;

/**
 * JSON API equivalent of the {@link org.hisp.dhis.dataintegrity.DataIntegrityDetails}.
 *
 * @author Jan Bernitt
 */
public interface JsonDataIntegrityDetails extends JsonDataIntegrityCheck {
  @Required
  default LocalDateTime getFinishedTime() {
    return get("finishedTime", JsonDate.class).date();
  }

  @Required
  default LocalDateTime getStartTime() {
    return get("startTime", JsonDate.class).date();
  }

  default String getError() {
    return getString("error").string(null);
  }

  @Required
  default JsonList<JsonDataIntegrityIssue> getIssues() {
    return getList("issues", JsonDataIntegrityIssue.class);
  }

  interface JsonDataIntegrityIssue extends JsonObject {
    @Required
    default String getId() {
      return getString("id").string();
    }

    @Required
    default String getName() {
      return getString("name").string();
    }

    default JsonString getComment() {
      return getString("comment");
    }

    default JsonList<JsonString> getRefs() {
      return getList("refs", JsonString.class);
    }
  }
}
