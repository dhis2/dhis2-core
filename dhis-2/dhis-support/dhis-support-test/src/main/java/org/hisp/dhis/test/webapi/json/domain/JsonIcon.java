/*
 * Copyright (c) 2004-2023, University of Oslo
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
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonObject;

/**
 * @author Zubair Asghar
 */
public interface JsonIcon extends JsonObject {

  default String getKey() {
    return getString("key").string();
  }

  default String getUid() {
    return getString("id").string();
  }

  default String getHref() {
    return getString("href").string();
  }

  default LocalDateTime getCreated() {
    return get("created", JsonDate.class).date();
  }

  default LocalDateTime getLastUpdated() {
    return get("lastUpdated", JsonDate.class).date();
  }

  default JsonObject getUserUid() {
    return getObject("createdBy");
  }

  default JsonObject getFileResource() {
    return getObject("fileResource");
  }

  default Set<String> getKeywords() {
    return new HashSet<>(getArray("keywords").stringValues());
  }

  default String getDescription() {
    return getString("description").string();
  }
}
