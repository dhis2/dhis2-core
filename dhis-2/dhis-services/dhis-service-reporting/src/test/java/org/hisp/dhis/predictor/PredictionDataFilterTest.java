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
package org.hisp.dhis.predictor;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.predictor.PredictionDataFilter.filter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PredictionDataFilter}.
 *
 * @author Jim Grace
 */

class PredictionDataFilterTest
    extends DhisConvenienceTest
{
    private final OrganisationUnit ouA = createOrganisationUnit( 'A' );

    private final Period periodA = createPeriod( "202201" );

    private final Period periodB = createPeriod( "202202" );

    private final Period periodC = createPeriod( "202203" );

    private final CategoryCombo ccA = createCategoryCombo( 'A' );

    private final DataElement deA = createDataElement( 'A', ccA );

    private final CategoryOption coA = createCategoryOption( 'A' );

    private final CategoryOptionCombo cocA = createCategoryOptionCombo( ccA, coA );

    private final FoundDimensionItemValue valueA = new FoundDimensionItemValue( ouA, periodA, cocA, deA, 1.0 );

    private final FoundDimensionItemValue valueB = new FoundDimensionItemValue( ouA, periodB, cocA, deA, 2.0 );

    private final FoundDimensionItemValue valueC = new FoundDimensionItemValue( ouA, periodC, cocA, deA, 3.0 );

    private final List<FoundDimensionItemValue> values = List.of( valueA, valueB, valueC );

    private final List<DataValue> oldPredictions = emptyList();

    private final PredictionData dataABC = new PredictionData( ouA, List.of( valueA, valueB, valueC ), oldPredictions );

    private final PredictionData dataAB = new PredictionData( ouA, List.of( valueA, valueB ), oldPredictions );

    private final PredictionData dataBC = new PredictionData( ouA, List.of( valueB, valueC ), oldPredictions );

    private final PredictionData dataB = new PredictionData( ouA, List.of( valueB ), oldPredictions );

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void testFilterMinDate()
    {
        deA.setQueryMods( QueryModifiers.builder()
            .minDate( periodB.getStartDate() ).build() );

        assertEquals( dataBC, filter( dataABC ) );
    }

    @Test
    void testFilterMaxDate()
    {
        deA.setQueryMods( QueryModifiers.builder()
            .maxDate( periodB.getEndDate() ).build() );

        assertEquals( dataAB, filter( dataABC ) );
    }

    @Test
    void testFilterMinAndMaxDate()
    {
        deA.setQueryMods( QueryModifiers.builder()
            .minDate( periodB.getStartDate() )
            .maxDate( periodB.getEndDate() ).build() );

        assertEquals( dataB, filter( dataABC ) );
    }

    @Test
    void testFilterNoMinOrMaxDate()
    {
        deA.setQueryMods( QueryModifiers.builder().build() );

        assertEquals( dataABC, filter( dataABC ) );
    }

    @Test
    void testFilterNoQueryMods()
    {
        deA.setQueryMods( null );

        assertEquals( dataABC, filter( dataABC ) );
    }

    @Test
    void testFilterNoData()
    {
        assertNull( filter( null ) );
    }
}
