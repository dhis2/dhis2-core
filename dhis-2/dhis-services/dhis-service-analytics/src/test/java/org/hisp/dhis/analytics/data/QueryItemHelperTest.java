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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.common.QueryOperator.IN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.QueryItemHelper;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;

/**
 * @author Dusan Bernat
 */
class QueryItemHelperTest extends DhisConvenienceTest
{
    private String UID_A = CodeGenerator.generateUid();

    private String UID_B = CodeGenerator.generateUid();

    private String OPTION_NAME_A = "OptionA";

    private String OPTION_NAME_B = "OptionB";

    private String LEGEND_NAME_A = "LegendA";

    private String LEGEND_NAME_B = "LegendB";

    private String LEGEND_CODE_A = "LegendCodeA";

    private String LEGEND_CODE_B = "LegendCodeB";

    @Test
    void testGeItemOptionValueWithIdSchemeNAME()
    {
        Option opA = createOption( 'A' );
        Option opB = createOption( 'B' );
        opA.setUid( UID_A );
        opB.setUid( UID_B );
        OptionSet os = createOptionSet( 'A', opA, opB );
        QueryItem queryItem = new QueryItem( null, null, null, null, os );
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem )
            .withOutputIdScheme( IdScheme.NAME ).build();

        String optionValueA = QueryItemHelper.getItemOptionValue( OPTION_NAME_A, params );
        String optionValueB = QueryItemHelper.getItemOptionValue( OPTION_NAME_B, params );

        assertEquals( OPTION_NAME_A, optionValueA );
        assertEquals( OPTION_NAME_B, optionValueB );
    }

    @Test
    void testGeItemOptionValueWithIdSchemeUID()
    {
        Option opA = createOption( 'A' );
        Option opB = createOption( 'B' );
        opA.setUid( UID_A );
        opB.setUid( UID_B );
        OptionSet os = createOptionSet( 'A', opA, opB );
        QueryItem queryItem = new QueryItem( null, null, null, null, os );
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem ).withOutputIdScheme( IdScheme.UID )
            .build();

        String optionValueA = QueryItemHelper.getItemOptionValue( OPTION_NAME_A, params );
        String optionValueB = QueryItemHelper.getItemOptionValue( OPTION_NAME_B, params );

        assertEquals( UID_A, optionValueA );
        assertEquals( UID_B, optionValueB );
    }

    @Test
    void testGeItemOptionValueWithIdSchemeCODE()
    {
        Option opA = createOption( 'A' );
        Option opB = createOption( 'B' );
        opA.setUid( UID_A );
        opB.setUid( UID_B );
        OptionSet os = createOptionSet( 'A', opA, opB );
        QueryItem queryItem = new QueryItem( null, null, null, null, os );
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem )
            .withOutputIdScheme( IdScheme.CODE )
            .build();

        String optionValueA = QueryItemHelper.getItemOptionValue( OPTION_NAME_A, params );
        String optionValueB = QueryItemHelper.getItemOptionValue( OPTION_NAME_B, params );

        assertEquals( "OptionCodeA", optionValueA );
        assertEquals( "OptionCodeB", optionValueB );
    }

    @Test
    void testGeItemLegendValueWithIdSchemeNAME()
    {
        Legend lpA = createLegend( 'A', 0.0, 1.0 );
        lpA.setCode( LEGEND_CODE_A );
        Legend lpB = createLegend( 'B', 0.0, 1.0 );
        lpB.setCode( LEGEND_CODE_B );
        lpA.setUid( UID_A );
        lpB.setUid( UID_B );
        LegendSet ls = createLegendSet( 'A', lpA, lpB );

        QueryItem queryItem = new QueryItem( null, ls, null, null, null );
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem )
            .withOutputIdScheme( IdScheme.NAME ).build();

        String legendValueA = QueryItemHelper.getItemLegendValue( LEGEND_NAME_A, params );
        String legendValueB = QueryItemHelper.getItemLegendValue( LEGEND_NAME_B, params );

        assertEquals( LEGEND_NAME_A, legendValueA );
        assertEquals( LEGEND_NAME_B, legendValueB );
    }

    @Test
    void testGeItemLegendValueWithIdSchemeUID()
    {
        Legend lpA = createLegend( 'A', 0.0, 1.0 );
        lpA.setCode( LEGEND_CODE_A );
        Legend lpB = createLegend( 'B', 0.0, 1.0 );
        lpB.setCode( LEGEND_CODE_B );
        lpA.setUid( UID_A );
        lpB.setUid( UID_B );
        LegendSet ls = createLegendSet( 'A', lpA, lpB );
        QueryItem queryItem = new QueryItem( null, ls, null, null, null );
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem ).withOutputIdScheme( IdScheme.UID )
            .build();

        String legendValueA = QueryItemHelper.getItemLegendValue( LEGEND_NAME_A, params );
        String legendValueB = QueryItemHelper.getItemLegendValue( LEGEND_NAME_B, params );

        assertEquals( UID_A, legendValueA );
        assertEquals( UID_B, legendValueB );
    }

    @Test
    void testGeItemLegendValueWithIdSchemeCODE()
    {
        Legend lpA = createLegend( 'A', 0.0, 1.0 );
        lpA.setCode( LEGEND_CODE_A );
        Legend lpB = createLegend( 'B', 0.0, 1.0 );
        lpB.setCode( LEGEND_CODE_B );
        lpA.setUid( UID_A );
        lpB.setUid( UID_B );
        LegendSet ls = createLegendSet( 'A', lpA, lpB );

        QueryItem queryItem = new QueryItem( null, ls, null, null, null );
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem )
            .withOutputIdScheme( IdScheme.CODE ).build();

        String legendValueA = QueryItemHelper.getItemLegendValue( LEGEND_NAME_A, params );
        String legendValueB = QueryItemHelper.getItemLegendValue( LEGEND_NAME_B, params );

        assertEquals( LEGEND_CODE_A, legendValueA );
        assertEquals( LEGEND_CODE_B, legendValueB );
    }

    @Test
    void testGetItemOptionsThatAreReferencedByQueryItems()
    {
        Option option1 = new Option( "Opt-A", "Code-A" );
        Option option2 = new Option( "Opt-B", "Code-B" );
        Option option3 = new Option( "Opt-C", "Code-C" );
        OptionSet optionSet = createOptionSet( 'A', option1, option2, option3 );

        Set<Option> options = Set.of( option1, option2, option3 );
        List<QueryItem> queryItems = new ArrayList<>();

        QueryItem queryItem = new QueryItem( null, null, null, null, optionSet );
        queryItem.addFilter( new QueryFilter( IN, "Code-A;Code-B" ) );
        queryItems.add( queryItem );

        Set<Option> actualOptions = QueryItemHelper.getItemOptions( options, queryItems );

        assertEquals( 2, actualOptions.size(), "Should have size of 2: actualOptions" );
    }

    @Test
    void testGetItemOptionsThatAreReferencedByQueryItemsDifferentCases()
    {
        Option option1 = new Option( "Opt-A", "Code-A" );
        Option option2 = new Option( "Opt-B", "Code-B" );
        Option option3 = new Option( "Opt-C", "Code-C" );
        OptionSet optionSet = createOptionSet( 'A', option1, option2, option3 );

        Set<Option> options = Set.of( option1, option2, option3 );
        List<QueryItem> queryItems = new ArrayList<>();

        QueryItem queryItem = new QueryItem( null, null, null, null, optionSet );
        queryItem.addFilter( new QueryFilter( IN, "CODE-A;CODE-C" ) );
        queryItems.add( queryItem );

        Set<Option> actualOptions = QueryItemHelper.getItemOptions( options, queryItems );

        // Then
        assertEquals( 2, actualOptions.size(), "Should have size of 2: actualOptions" );
    }

    @Test
    void testGetItemOptionsThatHaveNoFilters()
    {
        Option option1 = new Option( "Opt-A", "Code-A" );
        Option option2 = new Option( "Opt-B", "Code-B" );
        Option option3 = new Option( "Opt-C", "Code-C" );
        OptionSet optionSet = createOptionSet( 'A', option1, option2, option3 );

        Set<Option> options = Set.of( option1, option2, option3 );
        QueryItem queryItem = new QueryItem( null, null, null, null, optionSet );
        List<QueryItem> queryItems = new ArrayList<>();
        queryItems.add( queryItem );

        Set<Option> actualOptions = QueryItemHelper.getItemOptions( options, queryItems );

        assertEquals( 0, actualOptions.size(), "Should have size of 0: actualOptions" );
    }

    @Test
    void testGetItemOptionsWhenRowsArePresent()
    {
        Option option = new Option( "Opt-A", "Code-A" );
        OptionSet optionSet = createOptionSet( 'A', option );

        Grid grid = stubGridWithRowsAndOptionSet( optionSet );
        QueryItem queryItem = new QueryItem( null, null, null, null, optionSet );
        queryItem.addFilter( new QueryFilter( IN, "Code-A" ) );

        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem ).build();
        params.getItems().add( queryItem );

        Map<String, List<Option>> options = QueryItemHelper.getItemOptions( grid, params );

        assertTrue(
            options.values().stream().flatMap( Collection::stream ).collect( Collectors.toList() ).contains( option ) );
    }

    @Test
    void testGetItemOptionsWhenRowsAreEmpty()
    {
        Option option = new Option( "Opt-A", "Code-A" );
        OptionSet optionSet = createOptionSet( 'A', option );

        Grid grid = stubGridWithEmptyRowsAndOptionSet( optionSet );
        QueryItem queryItem = new QueryItem( null, null, null, null, optionSet );
        queryItem.addFilter( new QueryFilter( IN, "Code-A" ) );

        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem ).build();
        params.getItems().add( queryItem );

        Map<String, List<Option>> options = QueryItemHelper.getItemOptions( grid, params );

        assertTrue( options.values().stream()
            .flatMap( Collection::stream )
            .collect( Collectors.toList() )
            .contains( option ) );
    }

    @Test
    void testisItemOptionEqualToRowContentForText()
    {
        assertTrue( QueryItemHelper.isItemOptionEqualToRowContent( "abc", "AbC" ) );
    }

    @Test
    void testisItemOptionEqualToRowContentForDouble()
    {
        assertTrue( QueryItemHelper.isItemOptionEqualToRowContent( "1.0", Double.parseDouble( "1.0" ) ) );
    }

    @Test
    void testisItemOptionEqualToRowContentForInteger()
    {
        assertTrue( QueryItemHelper.isItemOptionEqualToRowContent( "1", Integer.parseInt( "1" ) ) );
    }

    @Test
    void testisItemOptionEqualToRowContentForBoolean()
    {
        assertTrue( QueryItemHelper.isItemOptionEqualToRowContent( "TruE", Boolean.parseBoolean( "true" ) ) );
    }

    @Test
    void testisItemOptionEqualToRowContentForLocalDate()
    {
        LocalDate localDate = LocalDate.now();

        assertTrue( QueryItemHelper.isItemOptionEqualToRowContent( localDate.toString(), localDate ) );
    }

    @Test
    void testisItemOptionEqualToRowContentForLocalDateTime()
    {
        LocalDateTime localDateTime = LocalDateTime.now();

        assertTrue( QueryItemHelper.isItemOptionEqualToRowContent( localDateTime.toString(), localDateTime ) );
    }

    private Grid stubGridWithRowsAndOptionSet( OptionSet optionSet )
    {
        Grid grid = new ListGrid();
        GridHeader headerA = new GridHeader( "ColA", "colA", ValueType.TEXT, false, true,
            optionSet, null, "programStage", new RepeatableStageParams() );
        GridHeader headerB = new GridHeader( "ColB", "colB", ValueType.TEXT, false, true );

        grid.addHeader( headerA );
        grid.addHeader( headerB );
        grid.addRow();
        grid.addValue( "Code-A" );
        grid.addValue( 11 );
        grid.addRow();
        grid.addValue( 21 );
        grid.addValue( 22 );
        grid.addRow();
        grid.addValue( 31 );
        grid.addValue( 32 );
        grid.addRow();
        grid.addValue( 41 );
        grid.addValue( 42 );

        return grid;
    }

    private Grid stubGridWithEmptyRowsAndOptionSet( OptionSet optionSet )
    {
        Grid grid = new ListGrid();
        GridHeader headerA = new GridHeader( "ColA", "colA", ValueType.TEXT, false, true,
            optionSet, null, "programStage", new RepeatableStageParams() );
        GridHeader headerB = new GridHeader( "ColB", "colB", ValueType.TEXT, false, true );

        grid.addHeader( headerA );
        grid.addHeader( headerB );

        return grid;
    }
}
