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

import org.hisp.dhis.dataintegrity.DataIntegritySeverity;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.Required;

/**
 * JSON API equivalent of the {@link org.hisp.dhis.dataintegrity.DataIntegrityCheck}.
 *
 * @author Jan Bernitt
 */
public interface JsonDataIntegrityCheck extends JsonObject {
  @Required
  default String getName() {
    return getString("name").string();
  }

  @Required
  default String getDisplayName() {
    return getString("displayName").string();
  }

  default String getSection() {
    return getString("section").string();
  }

  default DataIntegritySeverity getSeverity() {
    return getString("severity").parsed(DataIntegritySeverity::valueOf);
  }

  default String getDescription() {
    return getString("description").string();
  }

  default String getIntroduction() {
    return getString("introduction").string();
  }

  default String getRecommendation() {
    return getString("recommendation").string();
  }

  default String getIssuesIdType() {
    return getString("issuesIdType").string();
  }

  default boolean getIsSlow() {
    return getBoolean("isSlow").booleanValue();
  }

  default boolean getIsProgrammatic() {
    return getBoolean("isProgrammatic").booleanValue();
  }

  default String getCode() {
    return getString("code").string();
  }
}
