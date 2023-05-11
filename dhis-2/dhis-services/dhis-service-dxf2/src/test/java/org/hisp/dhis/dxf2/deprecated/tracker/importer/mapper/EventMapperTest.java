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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;

class EventMapperTest
{

    private final ProgramStageInstanceMapper programStageInstanceMapper;

    private org.hisp.dhis.dxf2.deprecated.tracker.event.Event event;

    private final static String DATA_ELEMENT_UID = "ABC12345678";

    private final static String EVENT_UID = "ABC23456789";

    EventMapperTest()
    {
        this.programStageInstanceMapper = setup( Collections.emptyMap() );
    }

    ProgramStageInstanceMapper setup( Map<String, Event> programStageInstanceMap )
    {
        // Identifiers
        String dataElementCode = "DE_CODE";
        String eventUid = "ABC23456789";

        // Set up DataElement
        DataElement de = new DataElement();
        de.setUid( DATA_ELEMENT_UID );
        de.setCode( dataElementCode );

        // Set up DataValue; identifier is CODE.
        DataValue dv = new DataValue();
        dv.setDataElement( de.getCode() );
        dv.setValue( "VALUE" );

        // Set up Event
        org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
        event.setUid( eventUid );
        event.setEvent( eventUid );
        event.setDataValues( Set.of( dv ) );

        // Prepare WorkContext collections
        Map<String, DataElement> dataElementMap = new HashMap<>();
        Map<String, Set<EventDataValue>> dataValuesMap = new HashMap<>();

        // populate dataElementMap. Identifier is CODE, value is the DataElement
        dataElementMap.put( de.getCode(), de );

        // convert DataValues to EventDataValues
        dataValuesMap.put( event.getUid(), event.getDataValues().stream().map( r -> {
            EventDataValue edv = new EventDataValue();
            edv.setDataElement( r.getDataElement() );
            edv.setValue( r.getValue() );
            return edv;
        } ).collect( Collectors.toSet() ) );

        this.event = event;

        // Initialize workContext, mapper and event.
        return new ProgramStageInstanceMapper(
            getWorkContext( programStageInstanceMap, dataElementMap, dataValuesMap ) );
    }

    private static WorkContext getWorkContext( Map<String, Event> programStageInstanceMap,
        Map<String, DataElement> dataElementMap, Map<String, Set<EventDataValue>> dataValuesMap )
    {
        return WorkContext.builder()
            .dataElementMap( dataElementMap )
            .programStageInstanceMap( programStageInstanceMap )
            .programInstanceMap( new HashMap<>() )
            .programsMap( new HashMap<>() )
            .organisationUnitMap( new HashMap<>() )
            .categoryOptionComboMap( new HashMap<>() )
            .eventDataValueMap( dataValuesMap )
            .importOptions( ImportOptions.getDefaultImportOptions().setIdScheme( "CODE" ) )
            .build();
    }

    @Test
    void mapShouldChangeIdentifierFromCodeToUid()
    {
        Event psi = programStageInstanceMapper.map( event );

        assertTrue(
            psi.getEventDataValues().stream().anyMatch( dv -> dv.getDataElement().equals( DATA_ELEMENT_UID ) ) );
    }

    @Test
    void mapShouldUseCreatedAtClientAndLastUpdatedAtClientIfNew()
    {
        Event psi = programStageInstanceMapper.map( event );
        assertEquals( psi.getCreatedAtClient(), DateUtils.parseDate( event.getCreatedAtClient() ) );
        assertEquals( psi.getLastUpdatedAtClient(), DateUtils.parseDate( event.getLastUpdatedAtClient() ) );
    }

    @Test
    void mapShouldNotUseCreatedAtClientIfUpdate()
    {
        Event existingPsi = mockProgramStageInstance( EVENT_UID,
            "2020-01-01T00:00:00.000",
            "2020-01-02T00:00:00.000" );

        ProgramStageInstanceMapper tested = setup( Map.of( EVENT_UID, existingPsi ) );

        Event psi = tested.map( event );

        assertEquals( psi.getCreatedAtClient(), existingPsi.getCreatedAtClient() );
        assertEquals( psi.getLastUpdatedAtClient(), DateUtils.parseDate( event.getLastUpdatedAtClient() ) );
    }

    private Event mockProgramStageInstance( String uid, String createdAtClient, String updatedAtClient )
    {
        Event psi = new Event();
        psi.setUid( uid );
        psi.setCreatedAtClient( DateUtils.parseDate( createdAtClient ) );
        psi.setLastUpdatedAtClient( DateUtils.parseDate( updatedAtClient ) );
        return psi;
    }
}
