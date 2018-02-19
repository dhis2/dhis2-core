package org.hisp.dhis.dataanalysis;

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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Lars Helge Overland
 */
public interface DataAnalysisStore
{
    String ID = DataAnalysisStore.class.getName();

    /**
     * Calculates the average and standard deviation measures of the DataValues
     * registered for a given data element, set of category option combos,
     * and organisation unit parents.
     *
     * @param dataElement the DataElement.
     * @param parentPaths the parent OrganisationUnits' paths.
     * @param from the from date for which to include data values.
     * @return a mapping between OrganisationUnit identifier and its standard deviation.
     */
    List<DataAnalysisMeasures> getDataAnalysisMeasures( DataElement dataElement,
        Collection<DataElementCategoryOptionCombo> categoryOptionCombos,
        Collection<String> parentPaths, Date from );

    /**
     * Generates a collection of data value violations of min-max predefined values.
     * 
     * @param dataElements the data elements.
     * @param categoryOptionCombos the category option combos.
     * @param periods the periods.
     * @param parents the parent OrganisationUnit units.
     * @param limit the max limit of violations to return.
     * @return a list of data value violations.
     */
    List<DeflatedDataValue> getMinMaxViolations( Collection<DataElement> dataElements, Collection<DataElementCategoryOptionCombo> categoryOptionCombos,
        Collection<Period> periods, Collection<OrganisationUnit> parents, int limit );
    
    /**
     * Returns a collection of DeflatedDataValues for the given input.
     * 
     * @param dataElement the DataElement.
     * @param categoryOptionCombo the DataElementCategoryOptionCombo.
     * @param periods the collection of Periods.
     * @param lowerBoundMap the lower bound for the registered MinMaxDataElement.
     * @param upperBoundMap the upper bound for the registered MinMaxDataElement.
     * @return a list of DeflatedDataValues.
     */
    List<DeflatedDataValue> getDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Map<Integer, Integer> lowerBoundMap, Map<Integer, Integer> upperBoundMap );

    /**
     * Returns a collection of DeflatedDataValues which are marked for followup and
     * whose source OrganisationUnit is equal or subordinate to the given OrganisationUnit.
     *
     * @param organisationUnit the source OrganisationUnit.
     * @param limit the maximum number of DeflatedDataValues to return.
     * @return a list of DeflatedDataValues.
     */
    List<DeflatedDataValue> getFollowupDataValues( OrganisationUnit organisationUnit, int limit );
}
