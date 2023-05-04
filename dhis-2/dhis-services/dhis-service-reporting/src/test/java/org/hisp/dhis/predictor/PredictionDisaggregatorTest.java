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
import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.category.CategoryOption.DEFAULT_NAME;
import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapOf;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Tests {@link PredictionDisaggregator}.
 *
 * @author Jim Grace
 */
class PredictionDisaggregatorTest
    extends DhisConvenienceTest
{

    // Data Element and Category structure:
    //
    // Data Element A -> Combo A -> Cat A (options A, B)
    // Data Element B -> Combo AC -> Cat A (options A, B), Cat C (options C, D)
    // Data Element C -> Combo BC -> Cat B (options A, B), Cat C (options C, D)
    // Data Element D -> Combo D -> Cat D (options E, F)
    // Data Element E -> Combo E -> Cat E (option A)
    // Data Element X -> Default combo -> Default category -> Default option

    // Category Options

    private final CategoryOption coA = createCategoryOption( "coA", "coAaaaaaaaa" );

    private final CategoryOption coB = createCategoryOption( "coB", "coBbbbbbbbb" );

    private final CategoryOption coC = createCategoryOption( "coC", "coCcccccccc" );

    private final CategoryOption coD = createCategoryOption( "coD", "coDdddddddd" );

    private final CategoryOption coE = createCategoryOption( "coE", "coEeeeeeeee" );

    private final CategoryOption coF = createCategoryOption( "coF", "coFffffffff" );

    private final CategoryOption coDefault = createCategoryOption( DEFAULT_NAME, "coIsDefault" );

    // Categories

    private final Category catA = createCategory( "catA", "catAabababa", coA, coB );

    private final Category catB = createCategory( "catB", "catBabababa", coA, coB );

    private final Category catC = createCategory( "catC", "catCcdcdcdc", coC, coD );

    private final Category catD = createCategory( "catD", "catDefefefe", coE, coF );

    private final Category catE = createCategory( "catE", "catEaaaaaaa", coA );

    private final Category catDefault = createCategory( DEFAULT_NAME, "caIsDefault", coDefault );

    // Category Combos

    private final CategoryCombo ccA = createCategoryCombo( "ccA", "ccAabababab", catA );

    private final CategoryCombo ccAC = createCategoryCombo( "ccAC", "ccACabcdabc", catA, catC );

    private final CategoryCombo ccBC = createCategoryCombo( "ccBC", "ccBCabcdabc", catB, catC );

    private final CategoryCombo ccD = createCategoryCombo( "ccD", "ccDefefefefe", catD );

    private final CategoryCombo ccE = createCategoryCombo( "ccE", "ccEaaaaaaaa", catE );

    private final CategoryCombo ccDefault = new CategoryCombo( DEFAULT_CATEGORY_COMBO_NAME, DISAGGREGATION,
        List.of( catDefault ) );

    // Category Option Combos

    private final CategoryOptionCombo cocAa = newCoc( 1L, "cocAa", "cocAccAaaaa", ccA, coA );

    private final CategoryOptionCombo cocAb = newCoc( 2L, "cocAb", "cocAccAbbbb", ccA, coB );

    private final CategoryOptionCombo cocACac = newCoc( 3L, "cocACac", "cocACccACac", ccAC, coA, coC );

    private final CategoryOptionCombo cocACad = newCoc( 4L, "cocACad", "cocACccACad", ccAC, coA, coD );

    private final CategoryOptionCombo cocACbc = newCoc( 5L, "cocACbc", "cocACccACbc", ccAC, coB, coC );

    private final CategoryOptionCombo cocACbd = newCoc( 6L, "cocACbd", "cocACccACbd", ccAC, coB, coD );

    private final CategoryOptionCombo cocBCac = newCoc( 7L, "cocBCac", "cocBCccACbd", ccBC, coA, coC );

    private final CategoryOptionCombo cocBCad = newCoc( 8L, "cocBCad", "cocAccBCada", ccBC, coA, coD );

    private final CategoryOptionCombo cocBCbc = newCoc( 9L, "cocBCbc", "cocAccBCaca", ccBC, coB, coC );

    private final CategoryOptionCombo cocBCbd = newCoc( 10L, "cocBCbd", "cocBCccBCbd", ccBC, coB, coD );

    private final CategoryOptionCombo cocDe = newCoc( 11L, "cocDe", "cocDccDeeee", ccD, coE );

    private final CategoryOptionCombo cocDf = newCoc( 12L, "cocDf", "cocDccDffff", ccD, coF );

    private final CategoryOptionCombo cocEa = newCoc( 13L, "cocEa", "cocEccAaaaa", ccA, coA );

    private final CategoryOptionCombo cocEb = newCoc( 14L, "cocEa", "cocEccAaaaa", ccA, coB );

    private final CategoryOptionCombo cocDefault = newCoc( 15L, DEFAULT_NAME, "cocDefault", ccDefault, coDefault );

    // Data Elements

    private final DataElement deA = createDataElement( 'A', ccA );

    private final DataElement deB = createDataElement( 'B', ccAC );

    private final DataElement deC = createDataElement( 'C', ccBC );

    private final DataElement deD = createDataElement( 'D', ccD );

    private final DataElement deE = createDataElement( 'E', ccE );

    private final DataElement deX = createDataElement( 'X', ccDefault );

    // Data Element Operands

    private final DataElementOperand deoAa = new DataElementOperand( deA, cocAa );

    private final DataElementOperand deoAb = new DataElementOperand( deA, cocAb );

    private final DataElementOperand deoBac = new DataElementOperand( deB, cocACac );

    private final DataElementOperand deoBad = new DataElementOperand( deB, cocACad );

    private final DataElementOperand deoBbc = new DataElementOperand( deB, cocACbc );

    private final DataElementOperand deoBbd = new DataElementOperand( deB, cocACbd );

    private final DataElementOperand deoCac = new DataElementOperand( deC, cocBCac );

    private final DataElementOperand deoCad = new DataElementOperand( deC, cocBCad );

    private final DataElementOperand deoCbc = new DataElementOperand( deC, cocBCbc );

    private final DataElementOperand deoCbd = new DataElementOperand( deC, cocBCbd );

    private final DataElementOperand deoDe = new DataElementOperand( deD, cocDe );

    private final DataElementOperand deoDf = new DataElementOperand( deD, cocDf );

    private final DataElementOperand deoEa = new DataElementOperand( deE, cocEa );

    private final DataElementOperand deoAX = new DataElementOperand( deA, null );

    private final DataElementOperand deoBX = new DataElementOperand( deB, null );

    private final DataElementOperand deoCX = new DataElementOperand( deC, null );

    private final DataElementOperand deoXX = new DataElementOperand( deX, null );

    // Items as if parsed from an expression

    private final List<DimensionalItemObject> expressionItems = List.of( deA, deB, deC, deX,
        deoAa, deoAb, deoBac, deoBad, deoBbc, deoBbd, deoCac, deoDe, deoDf, deoEa );

    // Organisation unit level

    private final OrganisationUnitLevel ouLevelA = new OrganisationUnitLevel( 1, "Top" );

    // Expression

    private final Expression expA = new Expression( "1", "Description" );

    // Predictors

    private final Predictor pWithoutDisag = createPredictor( deA, cocAa, "A", expA, null,
        new MonthlyPeriodType(), ouLevelA, 0, 0, 0 );

    private final Predictor pWithDisag = createPredictor( deA, null, "A", expA, null,
        new MonthlyPeriodType(), ouLevelA, 0, 0, 0 );

    // Periods

    private final Period per1 = getPeriodFromIsoString( "202201" );

    private final Period per2 = getPeriodFromIsoString( "202202" );

    // Context values before disaggregating

    // Period 1 values found in data:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap1 = MapMap.ofEntries(
        immutableEntry( per1, mapOf(
            deoAa, 1.0, // deA: catA=coA
            deoAb, 2.0, // deA: catA=coB
            deoBac, 3.0, // deB: catA=coA, catC=coC
            deoBad, 4.0, // deB: catA=coA, catC=coD
            deoBbc, 5.0, // deB: catA=coB, catC=coC
            deoBbd, 6.0, // deB: catA=coB, catC=coD
            deoCac, 7.0, // deC: catB=coA, catC=coC
            deoCad, 8.0, // deC: catB=coA, catC=coD
            deoCbc, 9.0, // deC: catB=coB, catC=coC
            deoCbd, 10.0, // deC: catB=coB, catC=coD
            deoDe, 11.0, // deD: catD=coE
            deoDf, 12.0, // deD: catD=coF
            deoEa, 13.0 ) ) ); // deE: catA=coA

    // Period 2 values found in data:
    private final MapMap<Period, DimensionalItemObject, Object> pvMap2 = MapMap.ofEntries(
        immutableEntry( per2, Map.of(
            deoAa, 14.0 ) ) ); // deA: catA=coA

    // Context values after disaggregating

    // Period 1 expression values for coA (removing coB):
    private final MapMap<Period, DimensionalItemObject, Object> pvMap1a = MapMap.ofEntries(
        immutableEntry( per1, mapOf(
            deoAa, 1.0, // deA: catA=coA
            deoBac, 3.0, // deB: catA=coA, catC=coC
            deoBad, 4.0, // deB: catA=coA, catC=coD
            deoCac, 7.0, // deC: catB=coA, catC=coC
            deoCad, 8.0, // deC: catB=coA, catC=coD
            deoDe, 11.0, // deD: catD=coE
            deoDf, 12.0, // deD: catD=coF
            deoEa, 13.0, // deE: catA=coA
            deA, 1.0, // deA sum
            deB, 7.0, // deB sum
            deC, 15.0, // deC sum
            deD, 23.0, // deD sum
            deE, 13.0 ) ) ); // deE sum

    // Period 1 expression values for coB (removing coA):
    private final MapMap<Period, DimensionalItemObject, Object> pvMap1b = MapMap.ofEntries(
        immutableEntry( per1, mapOf(
            deoAb, 2.0, // deA: catA=coB
            deoBbc, 5.0, // deB: catA=coB, catC=coC
            deoBbd, 6.0, // deB: catA=coB, catC=coD
            deoCbc, 9.0, // deC: catB=coB, catC=coC
            deoCbd, 10.0, // deC: catB=coB, catC=coD
            deoDe, 11.0, // deD: catD=coE
            deoDf, 12.0, // deD: catD=coF
            deA, 2.0, // deA sum
            deB, 11.0, // deB sum
            deC, 19.0, // deC sum
            deD, 23.0 ) ) ); // deE sum

    // Period 2 expression values for coA (removing coB):
    private final MapMap<Period, DimensionalItemObject, Object> pvMap2a = MapMap.ofEntries(
        immutableEntry( per2, Map.of(
            deoAa, 14.0, // deA: catA=coA
            deA, 14.0 ) ) ); // deA sum

    // Period 2 expression values for coB (removing coA):
    private final MapMap<Period, DimensionalItemObject, Object> pvMap2b = new MapMap<>();

    // Prediction contexts without disaggregating

    private final PredictionContext ctx1 = new PredictionContext( cocAa, cocDefault, per1, pvMap1 );

    private final PredictionContext ctx2 = new PredictionContext( cocAa, cocDefault, per2, pvMap2 );

    // Prediction contexts after disaggregating

    private final PredictionContext ctx1a = new PredictionContext( cocAa, cocDefault, per1, pvMap1a );

    private final PredictionContext ctx1b = new PredictionContext( cocAb, cocDefault, per1, pvMap1b );

    private final PredictionContext ctx2a = new PredictionContext( cocAa, cocDefault, per2, pvMap2a );

    private final PredictionContext ctx2b = new PredictionContext( cocAb, cocDefault, per2, pvMap2b );

    // List of starting (undisaggregated) prediction contexts

    private final List<PredictionContext> startingContexts = List.of( ctx1, ctx2 );

    // Target for tests

    private PredictionDisaggregator target;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void testGetDisaggregatedItemsWithoutDisaggregation()
    {
        setUpWithoutDisag();

        assertContainsOnly(
            expressionItems,
            target.getDisaggregatedItems() );
    }

    @Test
    void testGetDisaggregatedItemsWithDisaggregation()
    {
        setUpWithDisag();

        assertContainsOnly(
            Set.of( deoAX, deoBX, deoCX, deoXX,
                deoAa, deoAb, deoBac, deoBad, deoBbc, deoBbd, deoCac, deoDe, deoDf, deoEa ),
            target.getDisaggregatedItems() );
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

    private CategoryOptionCombo newCoc( Long id, String name, String uid, CategoryCombo categoryCombo,
        CategoryOption... categoryOptions )
    {
        CategoryOptionCombo coc = createCategoryOptionCombo( name, uid, categoryCombo, categoryOptions );
        coc.setId( id );
        categoryCombo.getOptionCombos().add( coc );

        return coc;
    }

    private void setUpWithoutDisag()
    {
        target = new PredictionDisaggregator( pWithoutDisag, expressionItems, cocDefault );
    }

    private void setUpWithDisag()
    {
        target = new PredictionDisaggregator( pWithDisag, expressionItems, cocDefault );
    }

    private void assertContextInList( PredictionContext ctx, List<PredictionContext> list )
    {
        assertTrue( list.contains( ctx ), String.format( "\n %s not found in [\n %s ]",
            formatPredictionContext( ctx ), formatPredictionContextList( list ) ) );
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
