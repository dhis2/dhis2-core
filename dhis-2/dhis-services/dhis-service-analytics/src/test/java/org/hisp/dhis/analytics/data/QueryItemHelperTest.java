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

import java.util.List;

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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dusan Bernat
 */
class QueryItemHelperTest extends DhisConvenienceTest
{

    private final String UID_A = CodeGenerator.generateUid();

    private final String UID_B = CodeGenerator.generateUid();

    private final String OPTION_NAME_A = "OptionA";

    private final String OPTION_NAME_B = "OptionB";

    private final String LEGEND_NAME_A = "LegendA";

    private final String LEGEND_NAME_B = "LegendB";

    private final String LEGEND_CODE_A = "LegendCodeA";

    private final String LEGEND_CODE_B = "LegendCodeB";

    private QueryItem queryItem;

    @BeforeEach
    void setUp()
    {
        DataElement deA = createDataElement( 'A' );
        Option opA = createOption( 'A' );
        Option opB = createOption( 'B' );
        opA.setUid( UID_A );
        opB.setUid( UID_B );
        OptionSet os = createOptionSet( 'A', opA, opB );
        Legend lpA = createLegend( 'A', 0.0, 1.0 );
        lpA.setCode( LEGEND_CODE_A );
        Legend lpB = createLegend( 'B', 0.0, 1.0 );
        lpB.setCode( LEGEND_CODE_B );
        lpA.setUid( UID_A );
        lpB.setUid( UID_B );
        LegendSet ls = createLegendSet( 'A', lpA, lpB );
        queryItem = new QueryItem( deA, ls, deA.getValueType(), deA.getAggregationType(), os );
    }

    @Test
    void geItemOptionNameValueTest()
    {
        // / arrange
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem )
            .withOutputIdScheme( IdScheme.NAME ).build();
        // act
        String optionValueA = QueryItemHelper.getItemOptionValue( OPTION_NAME_A, params );
        String optionValueB = QueryItemHelper.getItemOptionValue( OPTION_NAME_B, params );
        String legendValueA = QueryItemHelper.getItemLegendValue( LEGEND_NAME_A, params );
        String legendValueB = QueryItemHelper.getItemLegendValue( LEGEND_NAME_B, params );
        // assert
        assertEquals( OPTION_NAME_A, optionValueA );
        assertEquals( OPTION_NAME_B, optionValueB );
        assertEquals( LEGEND_NAME_A, legendValueA );
        assertEquals( LEGEND_NAME_B, legendValueB );
    }

    @Test
    void geItemOptionCodeTest()
    {
        // arrange
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem )
            .withOutputIdScheme( IdScheme.CODE ).build();
        // act
        String optionValueA = QueryItemHelper.getItemOptionValue( OPTION_NAME_A, params );
        String optionValueB = QueryItemHelper.getItemOptionValue( OPTION_NAME_B, params );
        String legendValueA = QueryItemHelper.getItemLegendValue( LEGEND_NAME_A, params );
        String legendValueB = QueryItemHelper.getItemLegendValue( LEGEND_NAME_B, params );
        // assert
        assertEquals( "OptionCodeA", optionValueA );
        assertEquals( "OptionCodeB", optionValueB );
        assertEquals( LEGEND_CODE_A, legendValueA );
        assertEquals( LEGEND_CODE_B, legendValueB );
    }

    @Test
    void geItemOptionUidTest()
    {
        // arrange
        EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem ).withOutputIdScheme( IdScheme.UID )
            .build();
        // act
        String optionValueA = QueryItemHelper.getItemOptionValue( OPTION_NAME_A, params );
        String optionValueB = QueryItemHelper.getItemOptionValue( OPTION_NAME_B, params );
        String legendValueA = QueryItemHelper.getItemLegendValue( LEGEND_NAME_A, params );
        String legendValueB = QueryItemHelper.getItemLegendValue( LEGEND_NAME_B, params );
        // assert
        assertEquals( UID_A, optionValueA );
        assertEquals( UID_B, optionValueB );
        assertEquals( UID_A, legendValueA );
        assertEquals( UID_B, legendValueB );
    }

    @Test
    void testGetItemOptionsWhenRowsArePresent()
    {
        // Given
        final Option option = new Option( "Opt-A", "Code-A" );
        final OptionSet optionSet = new OptionSet();
        optionSet.addOption( option );

        final Grid grid = stubGridWithRowsAndOptionSet( optionSet );
        final QueryItem queryItem = new QueryItem( null, null, null, null, optionSet );
        queryItem.addFilter( new QueryFilter( IN, "Code-A" ) );

        final EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem ).build();
        params.getItems().add( queryItem );

        // When
        final List<Option> options = QueryItemHelper.getItemOptions( grid, params );

        // Then
        assertTrue( options.contains( option ) );
    }

    @Test
    void testGetItemOptionsWhenRowsAreEmpty()
    {
        // Given
        final Option option = new Option( "Opt-A", "Code-A" );
        final OptionSet optionSet = new OptionSet();
        optionSet.addOption( option );

        final Grid grid = stubGridWithEmptyRowsAndOptionSet( optionSet );
        final QueryItem queryItem = new QueryItem( null, null, null, null, optionSet );
        queryItem.addFilter( new QueryFilter( IN, "Code-A" ) );

        final EventQueryParams params = new EventQueryParams.Builder().addItem( queryItem ).build();
        params.getItems().add( queryItem );

        // When
        final List<Option> options = QueryItemHelper.getItemOptions( grid, params );

        // Then
        assertTrue( options.contains( option ) );
    }

    private Grid stubGridWithRowsAndOptionSet( final OptionSet optionSet )
    {
        final Grid grid = new ListGrid();
        final GridHeader headerA = new GridHeader( "ColA", "colA", ValueType.TEXT, false, true,
            optionSet, null, "programStage", new RepeatableStageParams() );
        final GridHeader headerB = new GridHeader( "ColB", "colB", ValueType.TEXT, false, true );

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

    private Grid stubGridWithEmptyRowsAndOptionSet( final OptionSet optionSet )
    {
        final Grid grid = new ListGrid();
        final GridHeader headerA = new GridHeader( "ColA", "colA", ValueType.TEXT, false, true,
            optionSet, null, "programStage", new RepeatableStageParams() );
        final GridHeader headerB = new GridHeader( "ColB", "colB", ValueType.TEXT, false, true );

        grid.addHeader( headerA );
        grid.addHeader( headerB );

        return grid;
    }
}
