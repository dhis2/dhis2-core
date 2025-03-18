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

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;

/**
 * @author David Mackessy
 */
public interface JsonProgram extends JsonObject, JsonNameableObject {

  default JsonList<JsonProgramStage> getProgramStages() {
    return getList("programStages", JsonProgramStage.class);
  }

  default JsonList<JsonProgramSection> getProgramSections() {
    return getList("programSections", JsonProgramSection.class);
  }

  default JsonList<JsonProgramIndicator> getProgramIndicators() {
    return getList("programIndicators", JsonProgramIndicator.class);
  }

  default JsonList<JsonProgramRuleVariable> getProgramRuleVariables() {
    return getList("programRuleVariables", JsonProgramRuleVariable.class);
  }

  default JsonList<JsonProgramTrackedEntityAttribute> getProgramTrackedEntityAttributes() {
    return getList("programTrackedEntityAttributes", JsonProgramTrackedEntityAttribute.class);
  }

  default JsonString getEnrollmentDateLabel() {
    return getString("enrollmentDateLabel");
  }

  default JsonString getEnrollmentLabel() {
    return getString("enrollmentLabel");
  }

  default JsonString getFollowUpLabel() {
    return getString("followUpLabel");
  }

  default JsonString getOrUnitLabel() {
    return getString("orgUnitLabel");
  }

  default JsonString getRelationshipLabel() {
    return getString("relationshipLabel");
  }

  default JsonString getNoteLabel() {
    return getString("noteLabel");
  }

  default JsonString getTrackedEntityAttributeLabel() {
    return getString("trackedEntityAttributeLabel");
  }

  default JsonString getProgramStageLabel() {
    return getString("programStageLabel");
  }

  default JsonString getEventLabel() {
    return getString("eventLabel");
  }
}
