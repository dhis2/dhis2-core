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

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMultiMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;

/**
 * JSON equivalent of the {@link org.hisp.dhis.dataintegrity.FlattenedDataIntegrityReport}.
 *
 * @author Jan Bernitt
 */
public interface JsonDataIntegrityReport extends JsonObject {

  default JsonList<JsonString> getDataElementsWithoutDataSet() {
    return getList("dataElementsWithoutDataSet", JsonString.class);
  }

  default JsonList<JsonString> getDataElementsWithoutGroups() {
    return getList("dataElementsWithoutGroups", JsonString.class);
  }

  default JsonMultiMap<JsonString> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes() {
    return getMultiMap("dataElementsAssignedToDataSetsWithDifferentPeriodTypes", JsonString.class);
  }

  default JsonMultiMap<JsonString> getDataElementsViolatingExclusiveGroupSets() {
    return getMultiMap("dataElementsViolatingExclusiveGroupSets", JsonString.class);
  }

  default JsonMultiMap<JsonString> getDataElementsInDataSetNotInForm() {
    return getMultiMap("dataElementsInDataSetNotInForm", JsonString.class);
  }

  default JsonList<JsonString> getInvalidCategoryCombos() {
    return getList("invalidCategoryCombos", JsonString.class);
  }

  default JsonList<JsonString> getDataSetsNotAssignedToOrganisationUnits() {
    return getList("dataSetsNotAssignedToOrganisationUnits", JsonString.class);
  }

  default JsonList<JsonArray> getIndicatorsWithIdenticalFormulas() {
    return getList("indicatorsWithIdenticalFormulas", JsonArray.class);
  }

  default JsonList<JsonString> getIndicatorsWithoutGroups() {
    return getList("indicatorsWithoutGroups", JsonString.class);
  }

  default JsonMap<JsonString> getInvalidIndicatorNumerators() {
    return getMap("invalidIndicatorNumerators", JsonString.class);
  }

  default JsonMap<JsonString> getInvalidIndicatorDenominators() {
    return getMap("invalidIndicatorDenominators", JsonString.class);
  }

  default JsonMultiMap<JsonString> getIndicatorsViolatingExclusiveGroupSets() {
    return getMultiMap("indicatorsViolatingExclusiveGroupSets", JsonString.class);
  }

  default JsonList<JsonString> getDuplicatePeriods() {
    return getList("duplicatePeriods", JsonString.class);
  }

  default JsonList<JsonString> getOrganisationUnitsWithCyclicReferences() {
    return getList("organisationUnitsWithCyclicReferences", JsonString.class);
  }

  default JsonList<JsonString> getOrphanedOrganisationUnits() {
    return getList("orphanedOrganisationUnits", JsonString.class);
  }

  default JsonList<JsonString> getOrganisationUnitsWithoutGroups() {
    return getList("organisationUnitsWithoutGroups", JsonString.class);
  }

  default JsonMultiMap<JsonString> getOrganisationUnitsViolatingExclusiveGroupSets() {
    return getMultiMap("organisationUnitsViolatingExclusiveGroupSets", JsonString.class);
  }

  default JsonList<JsonString> getOrganisationUnitGroupsWithoutGroupSets() {
    return getList("organisationUnitGroupsWithoutGroupSets", JsonString.class);
  }

  default JsonList<JsonString> getValidationRulesWithoutGroups() {
    return getList("validationRulesWithoutGroups", JsonString.class);
  }

  default JsonMap<JsonString> getInvalidValidationRuleLeftSideExpressions() {
    return getMap("invalidValidationRuleLeftSideExpressions", JsonString.class);
  }

  default JsonMap<JsonString> getInvalidValidationRuleRightSideExpressions() {
    return getMap("invalidValidationRuleRightSideExpressions", JsonString.class);
  }

  default JsonMap<JsonString> getInvalidProgramIndicatorExpressions() {
    return getMap("invalidProgramIndicatorExpressions", JsonString.class);
  }

  default JsonList<JsonString> getProgramIndicatorsWithNoExpression() {
    return getList("programIndicatorsWithNoExpression", JsonString.class);
  }

  default JsonMap<JsonString> getInvalidProgramIndicatorFilters() {
    return getMap("invalidProgramIndicatorFilters", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRulesWithNoCondition() {
    return getMultiMap("programRulesWithNoCondition", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRulesWithNoPriority() {
    return getMultiMap("programRulesWithNoPriority", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRulesWithNoAction() {
    return getMultiMap("programRulesWithNoAction", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRuleVariablesWithNoDataElement() {
    return getMultiMap("programRuleVariablesWithNoDataElement", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRuleVariablesWithNoAttribute() {
    return getMultiMap("programRuleVariablesWithNoAttribute", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRuleActionsWithNoDataObject() {
    return getMultiMap("programRuleActionsWithNoDataObject", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRuleActionsWithNoNotification() {
    return getMultiMap("programRuleActionsWithNoNotification", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRuleActionsWithNoSectionId() {
    return getMultiMap("programRuleActionsWithNoSectionId", JsonString.class);
  }

  default JsonMultiMap<JsonString> getProgramRuleActionsWithNoStageId() {
    return getMultiMap("programRuleActionsWithNoStageId", JsonString.class);
  }
}
