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
package org.hisp.dhis.dataintegrity;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.hisp.dhis.webmessage.WebMessageResponse;

/**
 * Flattened, easily serializable object derivable from the more complex DataIntegrityReport. Use an
 * instance of this object to serialize and deliver a DataIntegrityReport.
 *
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
public class FlattenedDataIntegrityReport implements WebMessageResponse {
  @JsonProperty private final List<String> dataElementsWithoutDataSet;

  @JsonProperty private final List<String> dataElementsWithoutGroups;

  @JsonProperty
  private final Map<String, List<String>> dataElementsAssignedToDataSetsWithDifferentPeriodTypes;

  @JsonProperty private final Map<String, List<String>> dataElementsViolatingExclusiveGroupSets;

  @JsonProperty private final Map<String, List<String>> dataElementsInDataSetNotInForm;

  @JsonProperty private final List<String> invalidCategoryCombos;

  @JsonProperty private final List<String> dataSetsNotAssignedToOrganisationUnits;

  @JsonProperty private final List<List<String>> indicatorsWithIdenticalFormulas;

  @JsonProperty private final List<String> indicatorsWithoutGroups;

  @JsonProperty private final Map<String, String> invalidIndicatorNumerators;

  @JsonProperty private final Map<String, String> invalidIndicatorDenominators;

  @JsonProperty private final Map<String, List<String>> indicatorsViolatingExclusiveGroupSets;

  @JsonProperty private final List<String> duplicatePeriods;

  @JsonProperty private final List<String> organisationUnitsWithCyclicReferences;

  @JsonProperty private final List<String> orphanedOrganisationUnits;

  @JsonProperty private final List<String> organisationUnitsWithoutGroups;

  @JsonProperty
  private final Map<String, List<String>> organisationUnitsViolatingExclusiveGroupSets;

  @JsonProperty private final List<String> organisationUnitGroupsWithoutGroupSets;

  @JsonProperty private final List<String> validationRulesWithoutGroups;

  @JsonProperty private final Map<String, String> invalidValidationRuleLeftSideExpressions;

  @JsonProperty private final Map<String, String> invalidValidationRuleRightSideExpressions;

  @JsonProperty private final Map<String, String> invalidProgramIndicatorExpressions;

  @JsonProperty private final List<String> programIndicatorsWithNoExpression;

  @JsonProperty private final Map<String, String> invalidProgramIndicatorFilters;

  @JsonProperty private final Map<String, List<String>> programRulesWithNoCondition;

  @JsonProperty private final Map<String, List<String>> programRulesWithNoPriority;

  @JsonProperty private final Map<String, List<String>> programRulesWithNoAction;

  @JsonProperty private final Map<String, List<String>> programRuleVariablesWithNoDataElement;

  @JsonProperty private final Map<String, List<String>> programRuleVariablesWithNoAttribute;

  @JsonProperty private final Map<String, List<String>> programRuleActionsWithNoDataObject;

  @JsonProperty private final Map<String, List<String>> programRuleActionsWithNoNotification;

  @JsonProperty private final Map<String, List<String>> programRuleActionsWithNoSectionId;

  @JsonProperty private final Map<String, List<String>> programRuleActionsWithNoStageId;

  public FlattenedDataIntegrityReport(Map<String, DataIntegrityDetails> detailsByName) {
    // name/UID only
    this.dataElementsWithoutDataSet =
        listOfDisplayNameWithUid(detailsByName.get("data_elements_without_datasets"));
    this.dataElementsWithoutGroups =
        listOfDisplayNameWithUid(detailsByName.get("data_elements_aggregate_no_groups"));
    this.invalidCategoryCombos =
        listOfDisplayNameWithUid(detailsByName.get("invalid_category_combos"));
    this.dataSetsNotAssignedToOrganisationUnits =
        listOfDisplayNameOrUid(detailsByName.get("datasets_not_assigned_to_org_units"));
    this.indicatorsWithoutGroups =
        listOfDisplayNameOrUid(detailsByName.get("indicators_not_grouped"));
    this.duplicatePeriods =
        listOfDisplayNameOrUid(detailsByName.get("periods_same_start_date_period_type"));
    this.organisationUnitsWithCyclicReferences =
        listOfDisplayNameOrUid(
            detailsByName.get(DataIntegrityCheckType.ORG_UNITS_WITH_CYCLIC_REFERENCES.getName()));
    this.orphanedOrganisationUnits =
        listOfDisplayNameWithUid(detailsByName.get("orgunits_orphaned"));
    // Replaced with SQL based equivalent
    this.organisationUnitsWithoutGroups =
        listOfDisplayNameWithUid(detailsByName.get("organisation_units_without_groups"));
    this.organisationUnitGroupsWithoutGroupSets =
        listOfDisplayNameOrUid(detailsByName.get("org_units_not_in_compulsory_group_sets"));
    this.validationRulesWithoutGroups =
        listOfDisplayNameOrUid(detailsByName.get("validation_rules_without_groups"));
    this.programIndicatorsWithNoExpression =
        listOfDisplayNameOrUid(detailsByName.get("program_indicators_without_expression"));

    // grouped name/UID
    this.indicatorsWithIdenticalFormulas =
        groupedListOfDisplayNameOrUid(
            detailsByName.get(DataIntegrityCheckType.INDICATORS_WITH_IDENTICAL_FORMULAS.getName()));

    // comments ny name/UID
    this.invalidIndicatorNumerators =
        mapOfCommentByDisplayNameOrUid(
            detailsByName.get(DataIntegrityCheckType.INDICATORS_WITH_INVALID_NUMERATOR.getName()));
    this.invalidIndicatorDenominators =
        mapOfCommentByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.INDICATORS_WITH_INVALID_DENOMINATOR.getName()));
    this.invalidValidationRuleLeftSideExpressions =
        mapOfCommentByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_LEFT_SIDE_EXPRESSION
                    .getName()));
    this.invalidValidationRuleRightSideExpressions =
        mapOfCommentByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_RIGHT_SIDE_EXPRESSION
                    .getName()));
    this.invalidProgramIndicatorExpressions =
        mapOfCommentByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_EXPRESSIONS.getName()));
    this.invalidProgramIndicatorFilters =
        mapOfCommentByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_FILTERS.getName()));
    this.dataElementsAssignedToDataSetsWithDifferentPeriodTypes =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get("data_elements_aggregate_with_different_period_types"));
    this.dataElementsViolatingExclusiveGroupSets =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get("data_elements_violating_exclusive_group_sets"));
    this.dataElementsInDataSetNotInForm =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.DATA_ELEMENTS_IN_DATA_SET_NOT_IN_FORM.getName()));
    this.indicatorsViolatingExclusiveGroupSets =
        mapOfRefsByDisplayNameOrUid(detailsByName.get("indicators_violating_exclusive_group_sets"));
    this.organisationUnitsViolatingExclusiveGroupSets =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get("organisation_units_violating_exclusive_group_sets"));
    this.programRulesWithNoCondition =
        mapOfRefsByDisplayNameOrUid(detailsByName.get("program_rules_without_condition"));
    this.programRulesWithNoPriority =
        mapOfRefsByDisplayNameOrUid(detailsByName.get("program_rules_no_priority"));
    this.programRulesWithNoAction =
        mapOfRefsByDisplayNameOrUid(detailsByName.get("program_rules_no_action"));
    this.programRuleVariablesWithNoDataElement =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_DATA_ELEMENT.getName()));
    this.programRuleVariablesWithNoAttribute =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_ATTRIBUTE.getName()));
    this.programRuleActionsWithNoDataObject =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_DATA_OBJECT.getName()));
    this.programRuleActionsWithNoNotification =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_NOTIFICATION.getName()));
    this.programRuleActionsWithNoSectionId =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_SECTION.getName()));
    this.programRuleActionsWithNoStageId =
        mapOfRefsByDisplayNameOrUid(
            detailsByName.get(
                DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_STAGE_ID.getName()));
  }

  @Nonnull
  @Override
  public Class<? extends WebMessageResponse> getResponseClassType() {
    return FlattenedDataIntegrityReport.class;
  }

  private List<String> listOfDisplayNameOrUid(DataIntegrityDetails details) {
    return details == null
        ? null
        : details.getIssues().stream()
            .map(DataIntegrityIssue::getName)
            .collect(toUnmodifiableList());
  }

  private List<String> listOfDisplayNameWithUid(DataIntegrityDetails details) {
    return details == null
        ? null
        : details.getIssues().stream()
            .map(issue -> issue.getName() + ":" + issue.getId())
            .collect(toUnmodifiableList());
  }

  private List<List<String>> groupedListOfDisplayNameOrUid(DataIntegrityDetails details) {
    return details == null
        ? null
        : details.getIssues().stream()
            .map(DataIntegrityIssue::getRefs)
            .collect(toUnmodifiableList());
  }

  private static Map<String, String> mapOfCommentByDisplayNameOrUid(DataIntegrityDetails details) {
    return details == null
        ? null
        : details.getIssues().stream()
            .collect(
                toUnmodifiableMap(DataIntegrityIssue::getName, DataIntegrityIssue::getComment));
  }

  private static SortedMap<String, List<String>> mapOfRefsByDisplayNameOrUid(
      DataIntegrityDetails details) {
    return details == null
        ? null
        : details.getIssues().stream()
            .collect(
                toMap(
                    DataIntegrityIssue::getName,
                    DataIntegrityIssue::getRefs,
                    FlattenedDataIntegrityReport::concatLists,
                    TreeMap::new));
  }

  /**
   * On a "collision" (key already exists/has a value) we do combine both lists. At this point we
   * have to assume the passed {@link Collection}s are read-only.
   */
  private static List<String> concatLists(List<String> a, List<String> b) {
    List<String> both = new ArrayList<>();
    both.addAll(a);
    both.addAll(b);
    return both;
  }
}
