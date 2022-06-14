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

import static java.util.AbstractMap.SimpleImmutableEntry;
import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.category.CategoryOption.DEFAULT_NAME;
import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.Test;

/**
 * Tests {@see PredictionDisaggregator}.
 *
 * @author Jim Grace
 */
class PredictionDisaggregatorTest
    extends DhisConvenienceTest
{
    private final CategoryOption coA = createCategoryOption( "coA", "coAaaaaaaaa" );

    private final CategoryOption coB = createCategoryOption( "coB", "coBbbbbbbbb" );

    private final CategoryOption coDefault = createCategoryOption( DEFAULT_NAME, "coIsDefault" );

    private final Category catA = createCategory( "catA", "catAaaaaaaa", coA, coB );

    private final Category catDefault = createCategory( DEFAULT_NAME, "cataDefault", coDefault );

    private final CategoryCombo ccA = createCategoryCombo( "ccA", "ccAaaaaaaaa", catA );

    private final CategoryCombo ccDefault = new CategoryCombo( DEFAULT_CATEGORY_COMBO_NAME, DISAGGREGATION,
        List.of( catDefault ) );

    private final CategoryOptionCombo cocA = newCategoryOptionCombo( "cocA", "cocAaaaaaaa", ccA, coA );

    private final CategoryOptionCombo cocB = newCategoryOptionCombo( "cocB", "cocBbbbbbbb", ccA, coB );

    private final CategoryOptionCombo cocDefault = newCategoryOptionCombo( DEFAULT_NAME, "cocDefault", ccDefault,
        coDefault );

    private final DataElement deA = createDataElement( 'A', ccA );

    private final DataElement deB = createDataElement( 'B', ccA );

    private final DataElement deC = createDataElement( 'C', ccDefault );

    private final List<DimensionalItemObject> expressionItems = List.of( deA, deB, deC );

    private final DataElementOperand deoA = new DataElementOperand( deA, cocA );

    private final DataElementOperand deoB = new DataElementOperand( deA, cocB );

    private final DataElementOperand deoC = new DataElementOperand( deB, cocA );

    private final DataElementOperand deoD = new DataElementOperand( deB, cocB );

    private final DataElementOperand deoX = new DataElementOperand( deA, null );

    private final OrganisationUnitLevel ouLevelA = new OrganisationUnitLevel( 1, "Top" );

    private final Expression expA = new Expression( "1", "Description" );

    private final Predictor pWithoutDisag = createPredictor( deA, cocA, "A", expA, null,
        new MonthlyPeriodType(), ouLevelA, 0, 0, 0 );

    private final Predictor pWithDisag = createPredictor( deA, null, "A", expA, null,
        new MonthlyPeriodType(), ouLevelA, 0, 0, 0 );

    private final Period perA = getPeriodFromIsoString( "202201" );

    private final Period perB = getPeriodFromIsoString( "202202" );

    // Period A values found:
    private final MapMap<Period, DimensionalItemObject, Object> pvMapA = MapMap.ofEntries(
        new SimpleImmutableEntry<>( perA, Map.of( deoA, 1.0, deoB, 2.0 ) ) );

    // Period A with disaggregated values where deA -> deoA:
    private final MapMap<Period, DimensionalItemObject, Object> pvMapAA = MapMap.ofEntries(
        new SimpleImmutableEntry<>( perA, Map.of( deoA, 1.0, deoB, 2.0, deA, 1.0 ) ) );

    // Period A with disaggregated values where deA -> deoB:
    private final MapMap<Period, DimensionalItemObject, Object> pvMapAB = MapMap.ofEntries(
        new SimpleImmutableEntry<>( perA, Map.of( deoA, 1.0, deoB, 2.0, deA, 2.0 ) ) );

    // Period B values found:
    private final MapMap<Period, DimensionalItemObject, Object> pvMapB = MapMap.ofEntries(
        new SimpleImmutableEntry<>( perB, Map.of( deoA, 4.0 ) ) );

    // Period B disaggregated values where deA -> deoA:
    private final MapMap<Period, DimensionalItemObject, Object> pvMapBA = MapMap.ofEntries(
        new SimpleImmutableEntry<>( perB, Map.of( deoA, 4.0, deA, 4.0 ) ) );

    // Period B disaggregated values where deA -> deoB (which is missing):
    private final MapMap<Period, DimensionalItemObject, Object> pvMapBB = MapMap.ofEntries(
        new SimpleImmutableEntry<>( perB, Map.of( deoA, 4.0 ) ) );

    // Undisaggregated prediction contexts:
    private final PredictionContext ctxA = new PredictionContext( cocA, cocA, perA, pvMapA );

    private final PredictionContext ctxB = new PredictionContext( cocA, cocA, perB, pvMapB );

    // Disaggregated prediction contexts:
    private final PredictionContext ctxAA = new PredictionContext( cocA, cocA, perA, pvMapAA );

    private final PredictionContext ctxAB = new PredictionContext( cocB, cocA, perA, pvMapAB );

    private final PredictionContext ctxBA = new PredictionContext( cocA, cocA, perB, pvMapBA );

    private final PredictionContext ctxBB = new PredictionContext( cocB, cocA, perB, pvMapBB );

    // List of starting (undisaggregated) prediction contexts:
    private final List<PredictionContext> startingContexts = List.of( ctxA, ctxB );

    private PredictionDisaggregator target;

    @Test
    void testGetDisaggregatedItemsWithoutDisaggregation()
    {
        setUpWithoutDisag();

        assertEquals( Set.of( deA, deB, deC, deoA ), target.getDisaggregatedItems() );
    }

    @Test
    void testGetDisaggregatedItemsWithDisaggregation()
    {
        setUpWithDisag();

        assertEquals( Set.of( deC, deoA, deoB, deoC, deoD ), target.getDisaggregatedItems() );
    }

    @Test
    void testGetOutputCocWithoutDisaggregation()
    {
        setUpWithoutDisag();

        assertEquals( cocA, target.getOutputCoc() );
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

        assertEquals( deoA, target.getOutputDataElementOperand() );
    }

    @Test
    void testGetOutputDataElementOperandWithDisaggregation()
    {
        setUpWithDisag();

        assertEquals( deoX, target.getOutputDataElementOperand() );
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

        assertContainsOnly( actual, ctxAA, ctxAB, ctxBA, ctxBB );

        assertPeriodsInOrder( actual );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private CategoryOptionCombo newCategoryOptionCombo( String name, String uid, CategoryCombo categoryCombo,
        CategoryOption... categoryOptions )
    {
        CategoryOptionCombo coc = createCategoryOptionCombo( name, uid, categoryCombo, categoryOptions );
        categoryCombo.getOptionCombos().add( coc );

        return coc;
    }

    private void setUpWithoutDisag()
    {
        target = new PredictionDisaggregator( pWithoutDisag, cocDefault, expressionItems );
    }

    private void setUpWithDisag()
    {
        target = new PredictionDisaggregator( pWithDisag, cocDefault, expressionItems );
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
