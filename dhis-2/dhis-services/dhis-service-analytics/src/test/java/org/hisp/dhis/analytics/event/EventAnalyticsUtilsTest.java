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
package org.hisp.dhis.analytics.event;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.EventAnalyticsDimensionalItem;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;

/**
 * @author Henning Håkonsen
 */
class EventAnalyticsUtilsTest extends DhisConvenienceTest
{
    @Test
    void testGetAggregatedDataValueMapping()
    {
        Grid grid = new ListGrid();
        grid.addRow();
        grid.addValue( "de1" );
        grid.addValue( "ou2" );
        grid.addValue( "pe1" );
        grid.addValue( 3 );
        grid.addRow();
        grid.addValue( "de2" );
        grid.addValue( "ou3" );
        grid.addValue( "pe2" );
        grid.addValue( 5 );
        Map<String, Object> map = EventAnalyticsUtils.getAggregatedEventDataMapping( grid );
        assertEquals( 3, map.get( "de1" + DIMENSION_SEP + "ou2" + DIMENSION_SEP + "pe1" ) );
        assertEquals( 5, map.get( "de2" + DIMENSION_SEP + "ou3" + DIMENSION_SEP + "pe2" ) );
    }

    @Test
    void testGenerateEventDataPermutations()
    {
        Map<String, List<EventAnalyticsDimensionalItem>> tableRows = new LinkedHashMap<>();
        Grid grid = new ListGrid();
        DataElement deA = createDataElement( 'A' );
        deA.setValueType( ValueType.BOOLEAN );
        grid.addMetaData( deA.getUid(), deA );
        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'B' );
        OptionSet optionSet = new OptionSet();
        optionSet.addOption( new Option( "name", "code" ) );
        trackedEntityAttribute.setOptionSet( optionSet );
        grid.addMetaData( trackedEntityAttribute.getUid(), trackedEntityAttribute );
        List<EventAnalyticsDimensionalItem> objects = new ArrayList<>();
        Option t = new Option();
        t.setCode( "1" );
        t.setName( "Yes" );
        Option f = new Option();
        f.setCode( "0" );
        f.setName( "No" );
        objects.add( new EventAnalyticsDimensionalItem( t, deA.getUid() ) );
        objects.add( new EventAnalyticsDimensionalItem( f, deA.getUid() ) );
        objects
            .add( new EventAnalyticsDimensionalItem( new Option( "name", "code" ), trackedEntityAttribute.getUid() ) );
        tableRows.put( deA.getUid(), objects );
        tableRows.put( trackedEntityAttribute.getDimensionItem(), objects );
        List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations = EventAnalyticsUtils
            .generateEventDataPermutations( tableRows );
        assertEquals( 9, rowPermutations.size() );
    }
}
