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
package org.hisp.dhis.dataanalysis;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.jdbc.batchhandler.MinMaxDataElementBatchHandler;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.dataanalysis.MinMaxOutlierAnalysisService" )
public class MinMaxOutlierAnalysisService
    implements MinMaxDataAnalysisService
{
    private final DataAnalysisStore dataAnalysisStore;

    private final MinMaxDataElementService minMaxDataElementService;

    private final BatchHandlerFactory batchHandlerFactory;

    // -------------------------------------------------------------------------
    // DataAnalysisService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<DeflatedDataValue> analyse( Collection<OrganisationUnit> parents,
        Collection<DataElement> dataElements, Collection<Period> periods, Double stdDevFactor, Date from )
    {
        Set<DataElement> elements = dataElements.stream()
            .filter( de -> de.getValueType().isNumeric() )
            .collect( Collectors.toSet() );
        Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();

        for ( DataElement dataElement : elements )
        {
            categoryOptionCombos.addAll( dataElement.getCategoryOptionCombos() );
        }

        log.debug( "Starting min-max analysis, no of data elements: " + elements.size() + ", no of parent org units: "
            + parents.size() );

        return dataAnalysisStore.getMinMaxViolations( elements, categoryOptionCombos, periods, parents, MAX_OUTLIERS );
    }

    @Override
    public void generateMinMaxValues( OrganisationUnit parent, Collection<DataElement> dataElements,
        Double stdDevFactor )
    {
        log.info( "Starting min-max value generation, no of data elements: " + dataElements.size() + ", parent: "
            + parent.getUid() );

        Date from = new DateTime( 1, 1, 1, 1, 1 ).toDate();

        minMaxDataElementService.removeMinMaxDataElements( dataElements, parent );

        log.debug( "Deleted existing min-max values" );

        List<String> parentPaths = Lists.newArrayList( parent.getPath() );

        BatchHandler<MinMaxDataElement> batchHandler = batchHandlerFactory
            .createBatchHandler( MinMaxDataElementBatchHandler.class ).init();

        for ( DataElement dataElement : dataElements )
        {
            if ( dataElement.getValueType().isNumeric() )
            {
                Set<CategoryOptionCombo> categoryOptionCombos = dataElement.getCategoryOptionCombos();

                List<DataAnalysisMeasures> measuresList = dataAnalysisStore.getDataAnalysisMeasures( dataElement,
                    categoryOptionCombos, parentPaths, from );

                for ( DataAnalysisMeasures measures : measuresList )
                {
                    int min = (int) Math.round(
                        MathUtils.getLowBound( measures.getStandardDeviation(), stdDevFactor, measures.getAverage() ) );
                    int max = (int) Math.round( MathUtils.getHighBound( measures.getStandardDeviation(), stdDevFactor,
                        measures.getAverage() ) );

                    switch ( dataElement.getValueType() )
                    {
                        case INTEGER_POSITIVE:
                        case INTEGER_ZERO_OR_POSITIVE:
                            min = Math.max( 0, min ); // Cannot be < 0
                            break;
                        case INTEGER_NEGATIVE:
                            max = Math.min( 0, max ); // Cannot be > 0
                            break;
                    }

                    OrganisationUnit orgUnit = new OrganisationUnit();
                    orgUnit.setId( measures.getOrgUnitId() );

                    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
                    categoryOptionCombo.setId( measures.getCategoryOptionComboId() );

                    batchHandler.addObject(
                        new MinMaxDataElement( dataElement, orgUnit, categoryOptionCombo, min, max, true ) );
                }
            }
        }

        log.info( "Min-max value generation done" );

        batchHandler.flush();
    }
}
