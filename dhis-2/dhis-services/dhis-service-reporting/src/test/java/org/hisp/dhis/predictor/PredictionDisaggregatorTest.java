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

import static com.google.common.collect.Maps.immutableEntry;
import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapOf;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;
import static org.hisp.dhis.predictor.PredictionDisaggregatorUtils.createPredictionDisaggregator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PredictionDisaggregator}.
 *
 * @author Jim Grace
 */
class PredictionDisaggregatorTest
    extends PredictionDisaggregatorAbstractTest
{
    // Periods

    private final Period per1 = getPeriodFromIsoString( "202201" );

    private final Period per2 = getPeriodFromIsoString( "202202" );

    // Context values before disaggregating (powers of 2 are used so if a test
    // fails you can tell the difference between the wrong value and adding two
    // values)

    // Period 1 values found in data:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap1 = MapMap.ofEntries(
        immutableEntry( per1, Map.of(
            deoAa, 1.0,
            deoAb, 2.0,
            deoBa, 4.0,
            deoBb, 8.0,
            deoCa, 16.0,
            deoCb, 32.0,
            deoDa, 64.0,
            deoDc, 128.0 ) ) );

    // Period 2 values found in data:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap2 = MapMap.ofEntries(
        immutableEntry( per2, Map.of(
            deoAa, 256.0 ) ) );

    // Context values after disaggregating

    // Period 1 where Data Elements A, B, and C have option A:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap1a = MapMap.ofEntries(
        immutableEntry( per1, mapOf(
            deoAa, 1.0,
            deoAb, 2.0,
            deoBa, 4.0,
            deoBb, 8.0,
            deoCa, 16.0,
            deoCb, 32.0,
            deoDa, 64.0,
            deoDc, 128.0,
            deA, 1.0,
            deB, 4.0,
            deC, 16.0 ) ) );

    // Period 1 where Data Elements A, B, and C have option B:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap1b = MapMap.ofEntries(
        immutableEntry( per1, mapOf(
            deoAa, 1.0,
            deoAb, 2.0,
            deoBa, 4.0,
            deoBb, 8.0,
            deoCa, 16.0,
            deoCb, 32.0,
            deoDa, 64.0,
            deoDc, 128.0,
            deA, 2.0,
            deB, 8.0,
            deC, 32.0 ) ) );

    // Period 2 where Data Element A has option A:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap2a = MapMap.ofEntries(
        immutableEntry( per1, Map.of(
            deoAa, 256.0,
            deA, 256.0 ) ) );

    // Period 2 where Data Element A is missing option B:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap2b = MapMap.ofEntries(
        immutableEntry( per2, Map.of(
            deoAa, 256.0 ) ) );

    // Prediction contexts without disaggregating

    private final PredictionContext ctx1 = new PredictionContext( cocAa, cocDefault, per1, pvMap1 );

    private final PredictionContext ctx2 = new PredictionContext( cocAa, cocDefault, per2, pvMap2 );

    // Prediction contexts after disaggregating

    private final PredictionContext ctx1a = new PredictionContext( cocAa, cocDefault, per1, pvMap1a );

    private final PredictionContext ctx1b = new PredictionContext( cocAa, cocDefault, per1, pvMap1b );

    private final PredictionContext ctx2a = new PredictionContext( cocAa, cocDefault, per2, pvMap2a );

    private final PredictionContext ctx2b = new PredictionContext( cocAa, cocDefault, per2, pvMap2b );

    // List of starting (undisaggregated) prediction contexts

    private final List<PredictionContext> startingContexts = List.of( ctx1, ctx2 );

    // Target for tests

    private PredictionDisaggregator target;

    @Test
    void testCcAOptionCombos()
    {
        // Double-check to be sure that Cat Combo C does not contain cocCb
        assertEquals( Set.of( cocCa ), ccC.getOptionCombos() );
    }

    @Test
    void testGetDisaggregatedItemsWithoutDisaggregation()
    {
        setUpWithoutDisag();

        assertEquals(
            Set.of( deA, deB, deC, deD, deoAa, deoAb, deoBa, deoBb, deoCa, deoCb, deoDa, deoDc ),
            target.getDisaggregatedItems() );
    }

    @Test
    void testGetDisaggregatedItemsWithDisaggregation()
    {
        setUpWithDisag();

        assertEquals(
            Set.of( deoAX, deoBX, deoCX, deD, deoAa, deoAb, deoBa, deoBb, deoCa, deoCb, deoDa, deoDc ),
            target.getDisaggregatedItems() );
    }

    @Test
    void testGetOutputCocWithoutDisaggregation()
    {
        setUpWithoutDisag();

        assertEquals( cocAa, target.getOutputCoc() );
    }

    @Test
    void testGetOutputCocWithDisaggregation()
    {
        setUpWithDisag();

        assertNull( target.getOutputCoc() );
    }

    @Test
    void testGetOutputDataElementOperandWithoutDisaggregation()
    {
        setUpWithoutDisag();

        assertEquals( deoAa, target.getOutputDataElementOperand() );
    }

    @Test
    void testGetOutputDataElementOperandWithDisaggregation()
    {
        setUpWithDisag();

        assertEquals( deoAX, target.getOutputDataElementOperand() );
    }

    @Test
    void testGetDisaggregateContextsWithoutDisaggregation()
    {
        setUpWithoutDisag();

        assertEquals( startingContexts, target.getDisaggregateContexts( startingContexts ) );
    }

    @Test
    void testGetDisaggregateContextsWithDisaggregation()
    {
        setUpWithDisag();

        List<PredictionContext> actual = target.getDisaggregateContexts( startingContexts );

        assertContextInList( ctx1a, actual );
        assertContextInList( ctx1b, actual );
        assertContextInList( ctx2a, actual );
        assertContextInList( ctx2b, actual );

        assertEquals( 4, actual.size() );

        assertPeriodsInOrder( actual );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private DataElementOperand deo( DataElement dataElement, CategoryOptionCombo coc )
    {
        return new DataElementOperand( dataElement, coc );
    }

    private void setUpWithoutDisag()
    {
        target = createPredictionDisaggregator( pWithoutDisag, cocDefault, expressionItems );
    }

    private void setUpWithDisag()
    {
        target = createPredictionDisaggregator( pWithDisag, cocDefault, expressionItems );
    }

    private void assertContextInList( PredictionContext ctx, List<PredictionContext> list )
    {
        assertTrue( list.contains( ctx1a ), String.format( "\n %s not found in [\n %s ]",
            formatPredictionContext( ctx1a ), formatPredictionContextList( list ) ) );
    }

    private String formatPredictionContextList( List<PredictionContext> list )
    {
        return list.stream()
            .map( this::formatPredictionContext )
            .collect( joining( ",\n " ) );
    }

    private String formatPredictionContext( PredictionContext ctx )
    {
        return "OutputPeriod: " + ctx.getOutputPeriod().getName()
            + ", OutputCOC: " + ctx.getCategoryOptionCombo().getName()
            + ", AOC: " + ctx.getAttributeOptionCombo().getName()
            + ", PVM: {" + formatPeriodValueMap( ctx.getPeriodValueMap() ) + "}";
    }

    private String formatPeriodValueMap( MapMap<Period, DimensionalItemObject, Object> pvm )
    {
        return pvm.entrySet().stream()
            .map( e -> e.getKey().getName() + ": {" + formatValueMap( e.getValue() ) + "}" )
            .sorted()
            .collect( joining( ", " ) );
    }

    private String formatValueMap( Map<DimensionalItemObject, Object> vm )
    {
        return vm.entrySet().stream()
            .map( e -> e.getKey().getName() + ": " + e.getValue() )
            .sorted()
            .collect( joining( ", " ) );
    }

    /**
     * Asserts that context periods are in order (never going back in time).
     */
    private void assertPeriodsInOrder( List<PredictionContext> cList )
    {
        for ( int i = 0; i < cList.size() - 1; i++ )
        {
            String period = cList.get( i ).getOutputPeriod().getIsoDate();
            String nextPeriod = cList.get( i + 1 ).getOutputPeriod().getIsoDate();

            assertTrue( period.compareTo( nextPeriod ) <= 0,
                "Expected period " + period + " [" + i + "] <= " + nextPeriod );
        }
    }
}
