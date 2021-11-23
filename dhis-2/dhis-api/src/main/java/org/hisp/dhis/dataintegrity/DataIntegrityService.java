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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetDataIntegrityProvider;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitDataIntegrityProvider;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupDataIntegrityProvider;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleDataIntegrityProvider;

/**
 * @author Fredrik Fjeld
 */
public interface DataIntegrityService
    extends OrganisationUnitDataIntegrityProvider, OrganisationUnitGroupDataIntegrityProvider,
    ValidationRuleDataIntegrityProvider, DataSetDataIntegrityProvider
{
    String ID = DataIntegrityService.class.getName();

    // -------------------------------------------------------------------------
    // DataIntegrityService
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    /**
     * Gets all data elements which are not assigned to any data set.
     */
    List<DataElement> getDataElementsWithoutDataSet();

    /**
     * Gets all data elements which are not members of any groups.
     */
    List<DataElement> getDataElementsWithoutGroups();

    /**
     * Gets all data elements units which are members of more than one group
     * which enter into an exclusive group set.
     */
    SortedMap<DataElement, Collection<DataElementGroup>> getDataElementsViolatingExclusiveGroupSets();

    /**
     * Returns all data elements which are members of data sets with different
     * period types.
     */
    SortedMap<DataElement, Collection<DataSet>> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes();

    /**
     * Returns all data elements which are member of a data set but not part of
     * either the custom form or sections of the data set.
     */
    SortedMap<DataSet, Collection<DataElement>> getDataElementsInDataSetNotInForm();

    /**
     * Returns all invalid category combinations.
     */
    List<CategoryCombo> getInvalidCategoryCombos();

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------

    /**
     * Gets all indicators with identical numerator and denominator.
     */
    Set<Set<Indicator>> getIndicatorsWithIdenticalFormulas();

    /**
     * Gets all indicators which are not assigned to any groups.
     */
    List<Indicator> getIndicatorsWithoutGroups();

    /**
     * Gets all indicators with invalid indicator numerators.
     */
    SortedMap<Indicator, String> getInvalidIndicatorNumerators();

    /**
     * Gets all indicators with invalid indicator denominators.
     */
    SortedMap<Indicator, String> getInvalidIndicatorDenominators();

    /**
     * Gets all indicators units which are members of more than one group which
     * enter into an exclusive group set.
     */
    SortedMap<Indicator, Collection<IndicatorGroup>> getIndicatorsViolatingExclusiveGroupSets();

    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------

    /**
     * Lists all Periods which are duplicates, based on the period type and
     * start date.
     */
    List<Period> getDuplicatePeriods();

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    /**
     * Gets all ValidationRules with invalid left side expressions.
     */
    SortedMap<ValidationRule, String> getInvalidValidationRuleLeftSideExpressions();

    /**
     * Gets all ValidationRules with invalid right side expressions.
     */
    SortedMap<ValidationRule, String> getInvalidValidationRuleRightSideExpressions();

    // -------------------------------------------------------------------------
    // DataIntegrityReport
    // -------------------------------------------------------------------------

    /**
     * Returns a DataIntegrityReport.
     */
    DataIntegrityReport getDataIntegrityReport();

    /**
     * Returns a FlattenedDataIntegrityReport.
     */
    default FlattenedDataIntegrityReport getFlattenedDataIntegrityReport()
    {
        return new FlattenedDataIntegrityReport( getDataIntegrityReport() );
    }

}
