/*
 * Copyright (c) 2004-2021, University of Oslo
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
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.webmessage.WebMessageResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Flattened, easily serializable object derivable from the more complex
 * DataIntegrityReport. Use an instance of this object to serialize and deliver
 * a DataIntegrityReport.
 *
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
public class FlattenedDataIntegrityReport implements WebMessageResponse
{
    @JsonProperty
    private final List<String> dataElementsWithoutDataSet;

    @JsonProperty
    private final List<String> dataElementsWithoutGroups;

    @JsonProperty
    private final Map<String, Collection<String>> dataElementsAssignedToDataSetsWithDifferentPeriodTypes;

    @JsonProperty
    private final SortedMap<String, Collection<String>> dataElementsViolatingExclusiveGroupSets;

    @JsonProperty
    private final SortedMap<String, Collection<String>> dataElementsInDataSetNotInForm;

    @JsonProperty
    private final List<String> invalidCategoryCombos;

    @JsonProperty
    private final List<String> dataSetsNotAssignedToOrganisationUnits;

    @JsonProperty
    private final List<List<String>> indicatorsWithIdenticalFormulas;

    @JsonProperty
    private final List<String> indicatorsWithoutGroups;

    @JsonProperty
    private final Map<String, String> invalidIndicatorNumerators;

    @JsonProperty
    private final Map<String, String> invalidIndicatorDenominators;

    @JsonProperty
    private final SortedMap<String, Collection<String>> indicatorsViolatingExclusiveGroupSets;

    @JsonProperty
    private final List<String> duplicatePeriods;

    @JsonProperty
    private final List<String> organisationUnitsWithCyclicReferences;

    @JsonProperty
    private final List<String> orphanedOrganisationUnits;

    @JsonProperty
    private final List<String> organisationUnitsWithoutGroups;

    @JsonProperty
    private final SortedMap<String, Collection<String>> organisationUnitsViolatingExclusiveGroupSets;

    @JsonProperty
    private final List<String> organisationUnitGroupsWithoutGroupSets;

    @JsonProperty
    private final List<String> validationRulesWithoutGroups;

    @JsonProperty
    private final Map<String, String> invalidValidationRuleLeftSideExpressions;

    @JsonProperty
    private final Map<String, String> invalidValidationRuleRightSideExpressions;

    @JsonProperty
    private final Map<String, String> invalidProgramIndicatorExpressions;

    @JsonProperty
    private final List<String> programIndicatorsWithNoExpression;

    @JsonProperty
    private final Map<String, String> invalidProgramIndicatorFilters;

    @JsonProperty
    private final Map<String, Collection<String>> programRulesWithNoCondition;

    @JsonProperty
    private final Map<String, Collection<String>> programRulesWithNoPriority;

    @JsonProperty
    private final Map<String, Collection<String>> programRulesWithNoAction;

    @JsonProperty
    private final Map<String, Collection<String>> programRuleVariablesWithNoDataElement;

    @JsonProperty
    private final Map<String, Collection<String>> programRuleVariablesWithNoAttribute;

    @JsonProperty
    private final Map<String, Collection<String>> programRuleActionsWithNoDataObject;

    @JsonProperty
    private final Map<String, Collection<String>> programRuleActionsWithNoNotification;

    @JsonProperty
    private final Map<String, Collection<String>> programRuleActionsWithNoSectionId;

    @JsonProperty
    private final Map<String, Collection<String>> programRuleActionsWithNoStageId;

    public FlattenedDataIntegrityReport( org.hisp.dhis.dataintegrity.DataIntegrityReport report )
    {
        dataElementsWithoutDataSet = mapToListOfDisplayNameOrUid( report.getDataElementsWithoutDataSet() );

        dataElementsWithoutGroups = mapToListOfDisplayNameOrUid( report.getDataElementsWithoutGroups() );

        dataElementsAssignedToDataSetsWithDifferentPeriodTypes = mapToDisplayNameOrId(
            report.getDataElementsAssignedToDataSetsWithDifferentPeriodTypes() );

        dataElementsViolatingExclusiveGroupSets = mapToSortedDisplayNameOrUid(
            report.getDataElementsViolatingExclusiveGroupSets() );

        dataElementsInDataSetNotInForm = mapToSortedDisplayNameOrUid( report.getDataElementsInDataSetNotInForm() );

        invalidCategoryCombos = mapToListOfDisplayNameOrUid( report.getInvalidCategoryCombos() );

        dataSetsNotAssignedToOrganisationUnits = mapToListOfDisplayNameOrUid(
            report.getDataSetsNotAssignedToOrganisationUnits() );

        indicatorsWithIdenticalFormulas = mapToListOfListOfDisplayNameOrUid(
            report.getIndicatorsWithIdenticalFormulas() );

        indicatorsWithoutGroups = mapToListOfDisplayNameOrUid( report.getIndicatorsWithoutGroups() );

        invalidIndicatorNumerators = mapKeyToDisplayNameOrUid( report.getInvalidIndicatorNumerators() );

        invalidIndicatorDenominators = mapKeyToDisplayNameOrUid( report.getInvalidIndicatorDenominators() );

        indicatorsViolatingExclusiveGroupSets = mapToSortedDisplayNameOrUid(
            report.getIndicatorsViolatingExclusiveGroupSets() );

        duplicatePeriods = mapToListOfDisplayNameOrUid( report.getDuplicatePeriods() );

        organisationUnitsWithCyclicReferences = mapToListOfDisplayNameOrUid(
            report.getOrganisationUnitsWithCyclicReferences() );

        orphanedOrganisationUnits = mapToListOfDisplayNameOrUid( report.getOrphanedOrganisationUnits() );

        organisationUnitsWithoutGroups = mapToListOfDisplayNameOrUid(
            report.getOrganisationUnitsWithoutGroups() );

        organisationUnitsViolatingExclusiveGroupSets = mapToSortedDisplayNameOrUid(
            report.getOrganisationUnitsViolatingExclusiveGroupSets() );

        organisationUnitGroupsWithoutGroupSets = mapToListOfDisplayNameOrUid(
            report.getOrganisationUnitGroupsWithoutGroupSets() );

        validationRulesWithoutGroups = mapToListOfDisplayNameOrUid(
            report.getValidationRulesWithoutGroups() );

        invalidValidationRuleLeftSideExpressions = mapKeyToDisplayNameOrUid(
            report.getInvalidValidationRuleLeftSideExpressions() );

        invalidValidationRuleRightSideExpressions = mapKeyToDisplayNameOrUid(
            report.getInvalidValidationRuleRightSideExpressions() );

        programIndicatorsWithNoExpression = mapToListOfDisplayNameOrUid(
            report.getProgramIndicatorsWithNoExpression() );

        invalidProgramIndicatorExpressions = mapKeyToDisplayNameOrUid( report.getInvalidProgramIndicatorExpressions() );

        invalidProgramIndicatorFilters = mapKeyToDisplayNameOrUid( report.getInvalidProgramIndicatorFilters() );

        programRulesWithNoCondition = mapToDisplayNameOrId( report.getProgramRulesWithoutCondition() );

        programRulesWithNoPriority = mapToDisplayNameOrId( report.getProgramRulesWithNoPriority() );

        programRulesWithNoAction = mapToDisplayNameOrId( report.getProgramRulesWithNoAction() );

        programRuleVariablesWithNoDataElement = mapToDisplayNameOrId(
            report.getProgramRuleVariablesWithNoDataElement() );

        programRuleVariablesWithNoAttribute = mapToDisplayNameOrId(
            report.getProgramRuleVariablesWithNoAttribute() );

        programRuleActionsWithNoDataObject = mapToDisplayNameOrId(
            report.getProgramRuleActionsWithNoDataObject() );

        programRuleActionsWithNoNotification = mapToDisplayNameOrId(
            report.getProgramRuleActionsWithNoNotification() );

        programRuleActionsWithNoSectionId = mapToDisplayNameOrId( report.getProgramRuleActionsWithNoSectionId() );

        programRuleActionsWithNoStageId = mapToDisplayNameOrId( report.getProgramRuleActionsWithNoStageId() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<List<String>> mapToListOfListOfDisplayNameOrUid(
        Collection<? extends Collection<? extends IdentifiableObject>> collection )
    {
        return collection == null
            ? null
            : collection.stream().map( this::mapToListOfDisplayNameOrUid ).collect( toUnmodifiableList() );
    }

    private Map<String, String> mapKeyToDisplayNameOrUid( Map<? extends IdentifiableObject, String> map )
    {
        return map == null
            ? null
            : map.entrySet().stream().collect( toUnmodifiableMap(
                e -> defaultIfNull( e.getKey() ), Entry::getValue ) );
    }

    private Map<String, Collection<String>> mapToDisplayNameOrId(
        Map<? extends IdentifiableObject, ? extends Collection<? extends IdentifiableObject>> map )
    {
        return map == null
            ? null
            : map.entrySet().stream().collect( toUnmodifiableMap(
                e -> defaultIfNull( e.getKey() ),
                e -> mapToListOfDisplayNameOrUid( e.getValue() ) ) );
    }

    private List<String> mapToListOfDisplayNameOrUid( Collection<? extends IdentifiableObject> collection )
    {
        return collection == null
            ? null
            : collection.stream()
                .map( o -> defaultIfBlank( o.getDisplayName(), o.getUid() ) )
                .collect( toUnmodifiableList() );
    }

    private SortedMap<String, Collection<String>> mapToSortedDisplayNameOrUid(
        SortedMap<? extends IdentifiableObject, ? extends Collection<? extends IdentifiableObject>> map )
    {
        return map == null
            ? null
            : map.entrySet().stream().collect( toMap(
                e -> defaultIfNull( e.getKey() ),
                e -> mapToListOfDisplayNameOrUid( e.getValue() ),
                ( v1, v2 ) -> {
                    Collection<String> both = new ArrayList<>();
                    both.addAll( v1 );
                    both.addAll( v2 );
                    return both;
                },
                TreeMap::new ) );
    }

    private String defaultIfNull( IdentifiableObject object )
    {
        if ( object.getDisplayName() == null )
        {
            return object.getUid();
        }

        return object.getDisplayName() + ":" + object.getUid();
    }
}
