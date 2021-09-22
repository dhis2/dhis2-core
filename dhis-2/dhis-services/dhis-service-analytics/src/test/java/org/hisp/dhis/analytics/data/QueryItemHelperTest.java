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
package org.hisp.dhis.analytics.data;

import static org.junit.Assert.*;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.QueryItemHelper;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dusan Bernat
 */
public class QueryItemHelperTest extends DhisConvenienceTest
{
    private final String UID_A = "aaaaaaaa";

    private final String UID_B = "bbbbbbbb";

    private final String OPTION_NAME_A = "OptionA";

    private final String OPTION_NAME_B = "OptionB";

    private final String LEGEND_NAME_A = "LegendA";

    private final String LEGEND_NAME_B = "LegendB";

    private final String LEGEND_CODE_A = "LegendCodeA";

    private final String LEGEND_CODE_B = "LegendCodeB";

    private QueryItem queryItem;

    @Before
    public void setUp()
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
    public void geItemOptionNameValueTest()
    {
        /// arrange
        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( queryItem )
            .withOutputIdScheme( IdScheme.NAME )
            .build();
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
    public void geItemOptionCodeTest()
    {
        // arrange
        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( queryItem )
            .withOutputIdScheme( IdScheme.CODE )
            .build();
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
    public void geItemOptionUidTest()
    {
        // arrange
        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( queryItem )
            .withOutputIdScheme( IdScheme.UID )
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
}
