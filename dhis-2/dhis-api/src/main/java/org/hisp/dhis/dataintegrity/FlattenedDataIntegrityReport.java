package org.hisp.dhis.dataintegrity;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.IdentifiableObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Flattened, easily serializable object derivable from the more complex DataIntegrityReport.
 * Use an instance of this object to serialize and deliver a DataIntegrityReport.
 *
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
public class FlattenedDataIntegrityReport
{
    @JsonProperty
    private List<String> dataElementsWithoutDataSet;

    @JsonProperty
    private List<String> dataElementsWithoutGroups;

    @JsonProperty
    private Map<String, Collection<String>> dataElementsAssignedToDataSetsWithDifferentPeriodTypes;

    @JsonProperty
    private SortedMap<String, Collection<String>> dataElementsViolatingExclusiveGroupSets;

    @JsonProperty
    private SortedMap<String, Collection<String>> dataElementsInDataSetNotInForm;
    
    @JsonProperty
    private List<String> invalidCategoryCombos;

    @JsonProperty
    private List<String> dataSetsNotAssignedToOrganisationUnits;

    @JsonProperty
    private Collection<Collection<String>> indicatorsWithIdenticalFormulas;

    @JsonProperty
    private List<String> indicatorsWithoutGroups;

    @JsonProperty
    private Map<String, String> invalidIndicatorNumerators;

    @JsonProperty
    private Map<String, String> invalidIndicatorDenominators;

    @JsonProperty
    private SortedMap<String, Collection<String>> indicatorsViolatingExclusiveGroupSets;

    @JsonProperty
    private List<String> duplicatePeriods;

    @JsonProperty
    private List<String> organisationUnitsWithCyclicReferences;

    @JsonProperty
    private List<String> orphanedOrganisationUnits;

    @JsonProperty
    private List<String> organisationUnitsWithoutGroups;

    @JsonProperty
    private SortedMap<String, Collection<String>> organisationUnitsViolatingExclusiveGroupSets;

    @JsonProperty
    private List<String> organisationUnitGroupsWithoutGroupSets;

    @JsonProperty
    private List<String> validationRulesWithoutGroups;

    @JsonProperty
    private Map<String, String> invalidValidationRuleLeftSideExpressions;

    @JsonProperty
    private Map<String, String> invalidValidationRuleRightSideExpressions;

    public FlattenedDataIntegrityReport( org.hisp.dhis.dataintegrity.DataIntegrityReport report )
    {
        dataElementsWithoutDataSet = transformCollection( report.getDataElementsWithoutDataSet() );

        dataElementsWithoutGroups = transformCollection( report.getDataElementsWithoutGroups() );

        dataElementsAssignedToDataSetsWithDifferentPeriodTypes = transformMapOfCollections( report.getDataElementsAssignedToDataSetsWithDifferentPeriodTypes() );

        dataElementsViolatingExclusiveGroupSets = transformSortedMap( report.getDataElementsViolatingExclusiveGroupSets() );

        dataElementsInDataSetNotInForm = transformSortedMap( report.getDataElementsInDataSetNotInForm() );
        
        invalidCategoryCombos = transformCollection( report.getInvalidCategoryCombos() );

        dataSetsNotAssignedToOrganisationUnits = transformCollection( report.getDataSetsNotAssignedToOrganisationUnits() );

        indicatorsWithIdenticalFormulas = transformCollectionOfCollections( report.getIndicatorsWithIdenticalFormulas() );

        indicatorsWithoutGroups = transformCollection( report.getIndicatorsWithoutGroups() );

        invalidIndicatorNumerators = transformMapOfStrings( report.getInvalidIndicatorNumerators() );

        invalidIndicatorDenominators = transformMapOfStrings( report.getInvalidIndicatorDenominators() );

        indicatorsViolatingExclusiveGroupSets = transformSortedMap( report.getIndicatorsViolatingExclusiveGroupSets() );

        duplicatePeriods = transformCollection( report.getDuplicatePeriods() );

        organisationUnitsWithCyclicReferences = transformCollection( report.getOrganisationUnitsWithCyclicReferences() );

        orphanedOrganisationUnits = transformCollection( report.getOrphanedOrganisationUnits() );

        organisationUnitsWithoutGroups = transformCollection( report.getOrganisationUnitsWithoutGroups() );

        organisationUnitsViolatingExclusiveGroupSets = transformSortedMap( report.getOrganisationUnitsViolatingExclusiveGroupSets() );

        organisationUnitGroupsWithoutGroupSets = transformCollection( report.getOrganisationUnitGroupsWithoutGroupSets() );

        validationRulesWithoutGroups = transformCollection( report.getValidationRulesWithoutGroups() );

        invalidValidationRuleLeftSideExpressions = transformMapOfStrings( report.getInvalidValidationRuleLeftSideExpressions() );

        invalidValidationRuleRightSideExpressions = transformMapOfStrings( report.getInvalidValidationRuleRightSideExpressions() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Collection<Collection<String>> transformCollectionOfCollections( Collection<? extends Collection<? extends IdentifiableObject>> collection )
    {
        Collection<Collection<String>> newCollection = new HashSet<>();

        for ( Collection<? extends IdentifiableObject> c : collection )
        {
            newCollection.add( transformCollection( c ) );
        }

        return newCollection;
    }

    private Map<String, String> transformMapOfStrings( Map<? extends IdentifiableObject, String> map )
    {
        HashMap<String, String> newMap = new HashMap<>( map.size() );

        for ( Map.Entry<? extends IdentifiableObject, String> entry : map.entrySet() )
        {
            newMap.put( entry.getKey().getDisplayName(), entry.getValue() );
        }

        return newMap;
    }

    private Map<String, Collection<String>> transformMapOfCollections( Map<? extends IdentifiableObject, ? extends Collection<? extends IdentifiableObject>> map )
    {
        HashMap<String, Collection<String>> newMap = new HashMap<>();

        for ( Map.Entry<? extends IdentifiableObject, ? extends Collection<? extends IdentifiableObject>> entry : map.entrySet() )
        {
            Collection<String> value = new HashSet<>();
            value.addAll( transformCollection( entry.getValue() ) );

            newMap.put( entry.getKey().getDisplayName(), value );
        }

        return newMap;
    }

    private List<String> transformCollection( Collection<? extends IdentifiableObject> collection )
    {
        List<String> newCollection = new ArrayList<>( collection.size() );

        for ( IdentifiableObject o : collection )
        {
            newCollection.add( o.getDisplayName() );
        }

        return newCollection;
    }

    private SortedMap<String, Collection<String>> transformSortedMap( SortedMap<? extends IdentifiableObject, ? extends Collection<? extends IdentifiableObject>> map )
    {
        SortedMap<String, Collection<String>> newMap = new TreeMap<>();

        for ( SortedMap.Entry<? extends IdentifiableObject, ? extends Collection<? extends IdentifiableObject>> entry : map.entrySet() )
        {
            newMap.put( entry.getKey().getDisplayName(), transformCollection( entry.getValue() ) );
        }

        return newMap;
    }
}
