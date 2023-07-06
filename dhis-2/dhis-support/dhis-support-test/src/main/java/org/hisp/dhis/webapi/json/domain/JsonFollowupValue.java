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

import java.time.LocalDateTime;
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonObject;

/**
 * Web API equivalent of a {@link org.hisp.dhis.dataanalysis.FollowupValue}.
 *
 * @author Jan Bernitt
 */
public interface JsonFollowupValue extends JsonObject {
  default String getDe() {
    return getString("de").string();
  }

  default String getDeName() {
    return getString("deName").string();
  }

  default String getOu() {
    return getString("ou").string();
  }

  default String getOuName() {
    return getString("ouName").string();
  }

  default String getOuPath() {
    return getString("ouPath").string();
  }

  default String getPe() {
    return getString("pe").string();
  }

  default String getPeName() {
    return getString("peName").string();
  }

  default String getPeType() {
    return getString("peType").string();
  }

  default LocalDateTime getPeStartDate() {
    return get("peStartDate", JsonDate.class).date();
  }

  default LocalDateTime getPeEndDate() {
    return get("peEndDate", JsonDate.class).date();
  }

  default String getCoc() {
    return getString("coc").string();
  }

  default String getCocName() {
    return getString("cocName").string();
  }

  default String getAoc() {
    return getString("aoc").string();
  }

  default String getAocName() {
    return getString("aocName").string();
  }

  default String getValue() {
    return getString("value").string();
  }

  default Double getValueAsNumber() {
    return getString("value").parsed(Double::parseDouble);
  }

  default String getStoredBy() {
    return getString("storedBy").string();
  }

  default String getComment() {
    return getString("comment").string();
  }

  default LocalDateTime getLastUpdated() {
    return get("lastUpdated", JsonDate.class).date();
  }

  default LocalDateTime getCreated() {
    return get("created", JsonDate.class).date();
  }
}
