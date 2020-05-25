package org.hisp.dhis.dxf2.events.importer.context;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class EventDataValueAggregatorTest
{
    private EventDataValueAggregator subject;

    @Before
    public void setUp()
    {
        this.subject = new EventDataValueAggregator();
    }
    private final ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
    @Test
    public void allNew()
    {
        Event event1 = new Event();
        event1.setUid( CodeGenerator.generateUid() );
        event1.setDataValues( Sets.newHashSet( createDataValue( "abcd", "val1" ),
            createDataValue( "efgh", "val2" ), createDataValue( "ilmn", "val3" ) ) );

        Map<String, Set<EventDataValue>> dataValues = subject.aggregateDataValues( Lists.newArrayList( event1 ),
            new HashMap<>(), importOptions );
        assertThat( dataValues, is( notNullValue() ) );
        assertThat( dataValues.keySet(), hasSize( 1 ) );
        assertThat( dataValues.get( event1.getUid() ), hasSize( 3 ) );
        // TODO assert on single elements
    }

    @Test
    public void allExisting()
    {
        Event event1 = new Event();
        event1.setUid( CodeGenerator.generateUid() );
        event1.setDataValues( Sets.newHashSet(
            createDataValue( "abcd", "val1" ),
            createDataValue( "efgh", "val2" ),
            createDataValue( "ilmn", "val3" ) ) );

        Map<String, ProgramStageInstance> programStageInstanceMap = new HashMap<>();
        programStageInstanceMap.put( event1.getUid(), createPsi( event1.getUid(),
            createEventDataValue( "abcd", "val1" ),
            createEventDataValue( "efgh", "val2" ),
            createEventDataValue( "ilmn", "val3" ) ) );

        Map<String, Set<EventDataValue>> dataValues = subject.aggregateDataValues( Lists.newArrayList( event1 ),
            programStageInstanceMap, importOptions );
        assertThat( dataValues, is( notNullValue() ) );
        assertThat( dataValues.keySet(), hasSize( 1 ) );
        assertThat( dataValues.get( event1.getUid() ), hasSize( 3 ) );
        // TODO assert on single elements
    }

    @Test
    public void payloadHasLess()
    {
        Event event1 = new Event();
        event1.setUid( CodeGenerator.generateUid() );
        event1.setDataValues( Sets.newHashSet( createDataValue( "abcd", "val1" ) ) );

        Map<String, ProgramStageInstance> programStageInstanceMap = new HashMap<>();
        programStageInstanceMap.put( event1.getUid(), createPsi( event1.getUid(),
            createEventDataValue( "abcd", "val1" ), createEventDataValue( "efgh", "val2" ),
            createEventDataValue( "ilmn", "val3" ) ) );

        Map<String, Set<EventDataValue>> dataValues = subject.aggregateDataValues( Lists.newArrayList( event1 ),
            programStageInstanceMap, importOptions );
        assertThat( dataValues, is( notNullValue() ) );
        assertThat( dataValues.keySet(), hasSize( 1 ) );
        assertThat( dataValues.get( event1.getUid() ), hasSize( 1 ) );

        EventDataValue eventDataValue = dataValues.get( event1.getUid() ).iterator().next();
        assertThat( eventDataValue.getValue(), is( "val1" ) );
        assertThat( eventDataValue.getDataElement(), is( "abcd" ) );
    }

    @Test
    public void payloadHasMore()
    {
        Event event1 = new Event();
        event1.setUid( CodeGenerator.generateUid() );
        // @formatter:off
        event1.setDataValues( Sets.newHashSet(
            createDataValue( "abcd", "val1" ),
            createDataValue( "efgh", "val2" ),
            createDataValue( "ilmn", "val3" ),
            createDataValue( "gggg", "val4" ) ) );
        // @formatter:on
        Map<String, ProgramStageInstance> programStageInstanceMap = new HashMap<>();
        programStageInstanceMap.put( event1.getUid(),
        // @formatter:off
            createPsi( event1.getUid(),
            createEventDataValue( "abcd", "val1" ),
            createEventDataValue( "efgh", "val2" ),
            createEventDataValue( "ilmn", "val3" ) ) );
            // @formatter:on
        Map<String, Set<EventDataValue>> dataValues = subject.aggregateDataValues( Lists.newArrayList( event1 ),
            programStageInstanceMap, importOptions );
        assertThat( dataValues, is( notNullValue() ) );
        assertThat( dataValues.keySet(), hasSize( 1 ) );
        assertThat( dataValues.get( event1.getUid() ), hasSize( 4 ) );
    }

    @Test
    public void payloadIsEmpty()
    {
        Event event1 = new Event();
        event1.setUid( CodeGenerator.generateUid() );
        event1.setDataValues( Sets.newHashSet() );

        Map<String, ProgramStageInstance> programStageInstanceMap = new HashMap<>();
        programStageInstanceMap.put( event1.getUid(),
        // @formatter:off
            createPsi( event1.getUid(),
                createEventDataValue( "abcd", "val1" ),
                createEventDataValue( "efgh", "val2" ),
                createEventDataValue( "ilmn", "val3" ) ) );
            // @formatter:on
        Map<String, Set<EventDataValue>> dataValues = subject.aggregateDataValues( Lists.newArrayList( event1 ),
            programStageInstanceMap, importOptions );
        assertThat( dataValues, is( notNullValue() ) );
        assertThat( dataValues.keySet(), hasSize( 1 ) );
        assertThat( dataValues.get( event1.getUid() ), hasSize( 0 ) );
    }

    @Test
    public void allExistingButNoValue()
    {
        Event event1 = new Event();
        event1.setUid( CodeGenerator.generateUid() );
        event1.setDataValues( Sets.newHashSet(
        // @formatter:off
            createDataValue( "abcd", "" ),
            createDataValue( "efgh", "" ),
            createDataValue( "ilmn", "" ) ) );
            // @formatter:on
        Map<String, ProgramStageInstance> programStageInstanceMap = new HashMap<>();
        programStageInstanceMap.put( event1.getUid(), createPsi( event1.getUid(),
            createEventDataValue( "abcd", "val1" ),
            createEventDataValue( "efgh", "val2" ),
            createEventDataValue( "ilmn", "val3" ) ) );

        Map<String, Set<EventDataValue>> dataValues = subject.aggregateDataValues( Lists.newArrayList( event1 ),
            programStageInstanceMap, importOptions );
        assertThat( dataValues, is( notNullValue() ) );
        assertThat( dataValues.keySet(), hasSize( 1 ) );
        assertThat( dataValues.get( event1.getUid() ), hasSize( 0 ) );
        // TODO assert on single elements
    }

    public DataValue createDataValue( String dataElement, String value )
    {
        return new DataValue( dataElement, value );
    }

    public ProgramStageInstance createPsi( String uid, EventDataValue... eventDataValues )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( uid );
        programStageInstance.setEventDataValues( Sets.newHashSet( eventDataValues ) );
        return programStageInstance;
    }

    public EventDataValue createEventDataValue( String dataElement, String value )
    {
        return new EventDataValue( dataElement, value );
    }
}