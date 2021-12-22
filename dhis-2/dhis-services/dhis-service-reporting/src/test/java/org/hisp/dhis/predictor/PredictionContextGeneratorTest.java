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
package org.hisp.dhis.predictor;

import static com.google.common.collect.Maps.immutableEntry;
import static org.hisp.dhis.predictor.PredictionContextGenerator.getContexts;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Tests PredictionContextGenerator.
 *
 * @author Jim Grace
 */
public class PredictionContextGeneratorTest
    extends DhisConvenienceTest
{
    private final OrganisationUnit ouA = createOrganisationUnit( 'A' );

    private final Period periodA = createPeriod( "202201" );

    private final Period periodB = createPeriod( "202202" );

    private final Period periodC = createPeriod( "202203" );

    private final DataElement deA = createDataElement( 'A' );

    private final DataElementOperand deaA = new DataElementOperand( deA, createCategoryOptionCombo( 'C' ) );

    private final ProgramIndicator piA = createProgramIndicator( 'A',
        createProgram( 'A' ), "Expression A", "Filter A" );

    private final TrackedEntityAttribute teaA = createTrackedEntityAttribute( 'A' );

    private final ProgramTrackedEntityAttributeDimensionItem paA = new ProgramTrackedEntityAttributeDimensionItem(
        createProgram( 'B' ), teaA );

    private final ProgramTrackedEntityAttributeDimensionItem paB = new ProgramTrackedEntityAttributeDimensionItem(
        createProgram( 'C' ), teaA );

    private final List<Period> outputPeriods = Lists.newArrayList( periodB, periodC );

    private final CategoryOptionCombo aocA = createCategoryOptionCombo( createCategoryCombo( 'A' ) );

    private final CategoryOptionCombo aocB = createCategoryOptionCombo( createCategoryCombo( 'B' ) );

    private final CategoryOptionCombo aocX = createCategoryOptionCombo( createCategoryCombo( 'X' ) );

    private final FoundDimensionItemValue aocVal1 = new FoundDimensionItemValue( ouA, periodA, aocA, deA, 1.0 );

    private final FoundDimensionItemValue aocVal2 = new FoundDimensionItemValue( ouA, periodB, aocA, deaA, 2.0 );

    private final FoundDimensionItemValue aocVal3 = new FoundDimensionItemValue( ouA, periodB, aocA, piA, 3.0 );

    private final FoundDimensionItemValue aocVal4 = new FoundDimensionItemValue( ouA, periodC, aocB, teaA, 4.0 );

    private final CategoryOptionCombo aocNone = null;

    private final FoundDimensionItemValue nonAocVal5 = new FoundDimensionItemValue( ouA, periodB, aocNone, paA, 5.0 );

    private final FoundDimensionItemValue nonAocVal6 = new FoundDimensionItemValue( ouA, periodB, aocNone, paB, 6.0 );

    private final List<FoundDimensionItemValue> aocValues = Lists.newArrayList( aocVal1, aocVal2, aocVal3, aocVal4 );

    private final List<FoundDimensionItemValue> nonAocValues = Lists.newArrayList( nonAocVal5, nonAocVal6 );

    private final List<FoundDimensionItemValue> allValues = Lists.newArrayList( aocVal1, aocVal2, aocVal3, aocVal4,
        nonAocVal5, nonAocVal6 );

    private final List<FoundDimensionItemValue> noValues = Collections.emptyList();

    private final Map<DimensionalItemObject, Object> emptyValueMap = new HashMap<>();

    private final MapMap<Period, DimensionalItemObject, Object> emptyPeriodValueMap = new MapMap<>();

    // -------------------------------------------------------------------------
    // Format prediction contexts for ease of reading.
    // -------------------------------------------------------------------------

    // This may seem like a lot of work, but it makes it extremely easier to
    // read any test failures.

    private final Map<DimensionalItemObject, String> itemName = ImmutableMap.of(
        deA, "deA",
        deaA, "deaA",
        piA, "piA",
        teaA, "teaA",
        paA, "paA",
        paB, "paB" );

    private final Map<Period, String> periodName = ImmutableMap.of(
        periodA, "periodA",
        periodB, "periodB",
        periodC, "periodC" );

    private final Map<CategoryOptionCombo, String> aocName = ImmutableMap.of(
        aocA, "aocA",
        aocB, "aocB",
        aocX, "aocX" );

    private final Map<FoundDimensionItemValue, String> valueName = ImmutableMap.of(
        aocVal1, "aocVal1",
        aocVal2, "aocVal2",
        aocVal3, "aocVal3",
        aocVal4, "aocVal4",
        nonAocVal5, "nonAocVal5",
        nonAocVal6, "nonAocVal6" );

    private List<String> formatPredictionContextList( List<PredictionContext> contexts )
    {
        return contexts.stream()
            .map( this::formatPredictionContext )
            .collect( Collectors.toList() );
    }

    private String formatPredictionContext( PredictionContext ctx )
    {
        return "( aoc:" + aocName.get( ctx.getAttributeOptionCombo() )
            + ", period:" + periodName.get( ctx.getOutputPeriod() )
            + ", periodValueMap:" + formatPeriodValueMap( ctx.getPeriodValueMap() )
            + ", valueMap:" + formatValueMap( ctx.getValueMap() ) + ")";
    }

    private String formatPeriodValueMap( MapMap<Period, DimensionalItemObject, Object> periodValueMap )
    {
        String format = "{";

        for ( Map.Entry<Period, Map<DimensionalItemObject, Object>> e : sort3( periodValueMap.entrySet() ) )
        {
            format += periodName.get( e.getKey() ) + ":" + formatValueMap( e.getValue() ) + ",";
        }

        return stripTrailingComma( format ) + "}";
    }

    private String formatValueMap( Map<DimensionalItemObject, Object> valueMap )
    {
        String format = "{";

        for ( Map.Entry<DimensionalItemObject, Object> e : sort2( valueMap.entrySet() ) )
        {
            format += itemName.get( e.getKey() ) + ":" + e.getValue() + ",";
        }

        return stripTrailingComma( format ) + "}";
    }

    private String stripTrailingComma( String s )
    {
        return s.lastIndexOf( ',' ) == s.length() - 1
            ? s.substring( 0, s.length() - 1 )
            : s;
    }

    private List<Map.Entry<Period, Map<DimensionalItemObject, Object>>> sort3(
        Set<Map.Entry<Period, Map<DimensionalItemObject, Object>>> entrySet )
    {
        List<Map.Entry<Period, Map<DimensionalItemObject, Object>>> entries = new ArrayList<>( entrySet );

        entries.sort( Comparator.comparing( e -> e.getKey() ) );

        return entries;
    }

    private List<Map.Entry<DimensionalItemObject, Object>> sort2(
        Set<Map.Entry<DimensionalItemObject, Object>> entrySet )
    {
        List<Map.Entry<DimensionalItemObject, Object>> entries = new ArrayList<>( entrySet );

        entries.sort( Comparator.comparing( e -> (Double) e.getValue() ) );

        return entries;
    }

    // -------------------------------------------------------------------------
    // Check order
    // -------------------------------------------------------------------------

    /**
     * For each attribute option combination, earlier periods must be evaluated
     * before later periods. This is because an earlier predicted value may be
     * used as input to a later prediction.
     * <p>
     * However, the ordering between contexts of attribute option combinations
     * does not matter. That order depends on the order they are pulled out of a
     * Map within the context generator. This only checks the order of periods
     * within contexts of the same attribute option combination.
     */
    private void checkOrder( List<PredictionContext> contexts )
    {
        List<CategoryOptionCombo> aocs = contexts.stream()
            .map( PredictionContext::getAttributeOptionCombo )
            .collect( Collectors.toList() );

        for ( CategoryOptionCombo aoc : aocs )
        {
            List<Period> outputPeriods = contexts.stream()
                .filter( c -> c.getAttributeOptionCombo().equals( aoc ) )
                .map( PredictionContext::getOutputPeriod )
                .collect( Collectors.toList() );

            List<Period> sortedPeriods = new ArrayList<>( outputPeriods );

            sortedPeriods.sort( Comparator.comparing( Period::getStartDate ) );

            assertEquals( sortedPeriods, outputPeriods );
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetContextsWithAocData()
    {
        // valueMap for attributeOptionCombo A, period B:
        Map<DimensionalItemObject, Object> aocAPerBValueMap = ImmutableMap.of(
            deaA, 2.0,
            piA, 3.0 );

        // valueMap for attributeOptionCombo B, period C:
        Map<DimensionalItemObject, Object> aocBPerCValueMap = ImmutableMap.of(
            teaA, 4.0 );

        // periodValueMap for attributeOptionCombo A:
        MapMap<Period, DimensionalItemObject, Object> aocAPeriodValueMap = MapMap.ofEntries(
            immutableEntry( periodA, ImmutableMap.of(
                deA, 1.0 ) ),
            immutableEntry( periodB, aocAPerBValueMap ) );

        // periodValueMap for attributeOptionCombo B:
        MapMap<Period, DimensionalItemObject, Object> aocBPeriodValueMap = MapMap.ofEntries(
            immutableEntry( periodC, aocBPerCValueMap ) );

        PredictionContext expected1 = new PredictionContext( aocA, periodB, aocAPeriodValueMap, aocAPerBValueMap );
        PredictionContext expected2 = new PredictionContext( aocA, periodC, aocAPeriodValueMap, emptyValueMap );
        PredictionContext expected3 = new PredictionContext( aocB, periodB, aocBPeriodValueMap, emptyValueMap );
        PredictionContext expected4 = new PredictionContext( aocB, periodC, aocBPeriodValueMap, aocBPerCValueMap );

        String formatted1 = formatPredictionContext( expected1 );
        String formatted2 = formatPredictionContext( expected2 );
        String formatted3 = formatPredictionContext( expected3 );
        String formatted4 = formatPredictionContext( expected4 );

        List<PredictionContext> actual = getContexts( outputPeriods, aocValues, aocX );

        List<String> actualFormatted = formatPredictionContextList( actual );

        assertContainsOnly( actualFormatted, formatted1, formatted2, formatted3, formatted4 );

        checkOrder( actual );
    }

    @Test
    public void testGetContextsWithNonAocData()
    {
        // valueMap for attributeOptionCombo X, period B:
        Map<DimensionalItemObject, Object> aocXPerBValueMap = ImmutableMap.of(
            paA, 5.0,
            paB, 6.0 );

        // periodValueMap for attributeOptionCombo X (default AOC):
        MapMap<Period, DimensionalItemObject, Object> aocXPeriodValueMap = MapMap.ofEntries(
            immutableEntry( periodB, aocXPerBValueMap ) );

        PredictionContext expected1 = new PredictionContext( aocX, periodB, aocXPeriodValueMap, aocXPerBValueMap );
        PredictionContext expected2 = new PredictionContext( aocX, periodC, aocXPeriodValueMap, emptyValueMap );

        String formatted1 = formatPredictionContext( expected1 );
        String formatted2 = formatPredictionContext( expected2 );

        List<PredictionContext> actual = getContexts( outputPeriods, nonAocValues, aocX );

        List<String> actualFormatted = formatPredictionContextList( actual );

        assertContainsOnly( actualFormatted, formatted1, formatted2 );

        checkOrder( actual );
    }

    @Test
    public void testGetContextsWithAocAndNonAocData()
    {
        // valueMap for attributeOptionCombo A, period B:
        Map<DimensionalItemObject, Object> aocAPerBValueMap = ImmutableMap.of(
            deaA, 2.0,
            piA, 3.0,
            paA, 5.0,
            paB, 6.0 );

        // valueMap for attributeOptionCombo A, period B:
        Map<DimensionalItemObject, Object> aocBPerBValueMap = ImmutableMap.of(
            paA, 5.0,
            paB, 6.0 );

        // valueMap for attributeOptionCombo B, period C:
        Map<DimensionalItemObject, Object> aocBPerCValueMap = ImmutableMap.of(
            teaA, 4.0 );

        // periodValueMap for attributeOptionCombo A:
        MapMap<Period, DimensionalItemObject, Object> aocAPeriodValueMap = MapMap.ofEntries(
            immutableEntry( periodA, ImmutableMap.of(
                deA, 1.0 ) ),
            immutableEntry( periodB, aocAPerBValueMap ) );

        // periodValueMap for attributeOptionCombo B:
        MapMap<Period, DimensionalItemObject, Object> aocBPeriodValueMap = MapMap.ofEntries(
            immutableEntry( periodB, aocBPerBValueMap ),
            immutableEntry( periodC, aocBPerCValueMap ) );

        PredictionContext expected1 = new PredictionContext( aocA, periodB, aocAPeriodValueMap, aocAPerBValueMap );
        PredictionContext expected2 = new PredictionContext( aocA, periodC, aocAPeriodValueMap, emptyValueMap );
        PredictionContext expected3 = new PredictionContext( aocB, periodB, aocBPeriodValueMap, aocBPerBValueMap );
        PredictionContext expected4 = new PredictionContext( aocB, periodC, aocBPeriodValueMap, aocBPerCValueMap );

        String formatted1 = formatPredictionContext( expected1 );
        String formatted2 = formatPredictionContext( expected2 );
        String formatted3 = formatPredictionContext( expected3 );
        String formatted4 = formatPredictionContext( expected4 );

        List<PredictionContext> actual = getContexts( outputPeriods, allValues, aocX );

        List<String> actualFormatted = formatPredictionContextList( actual );

        assertContainsOnly( actualFormatted, formatted1, formatted2, formatted3, formatted4 );

        checkOrder( actual );
    }

    @Test
    public void testGetContextsWithNoData()
    {
        PredictionContext expected1 = new PredictionContext( aocX, periodB, emptyPeriodValueMap, emptyValueMap );
        PredictionContext expected2 = new PredictionContext( aocX, periodC, emptyPeriodValueMap, emptyValueMap );

        String formatted1 = formatPredictionContext( expected1 );
        String formatted2 = formatPredictionContext( expected2 );

        List<PredictionContext> actual = getContexts( outputPeriods, noValues, aocX );

        List<String> actualFormatted = formatPredictionContextList( actual );

        assertContainsOnly( actualFormatted, formatted1, formatted2 );

        checkOrder( actual );
    }
}
