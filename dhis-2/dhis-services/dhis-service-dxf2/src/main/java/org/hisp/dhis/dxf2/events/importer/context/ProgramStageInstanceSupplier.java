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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramStageInstancesSupplier" )
@Slf4j
public class ProgramStageInstanceSupplier extends AbstractSupplier<Map<String, ProgramStageInstance>>
{
    private final ObjectMapper jsonMapper;

    private final MapType mapType;

    public ProgramStageInstanceSupplier( NamedParameterJdbcTemplate jdbcTemplate,
        @Qualifier( "dataValueJsonMapper" ) ObjectMapper jsonMapper )
    {
        super( jdbcTemplate );
        this.jsonMapper = jsonMapper;

        TypeFactory typeFactory = jsonMapper.getTypeFactory();
        mapType = typeFactory.constructMapType( HashMap.class, String.class, EventDataValue.class );
    }

    @Override
    public Map<String, ProgramStageInstance> get( ImportOptions importOptions, List<Event> events )
    {
        if ( events == null )
        {
            return new HashMap<>();
        }

        Set<String> psiUid = events.stream().map( Event::getUid ).collect( Collectors.toSet() );

        if ( isEmpty( psiUid ) )
        {
            return new HashMap<>();
        }

        final String sql = "select psi.programinstanceid, psi.programstageinstanceid, psi.uid, psi.status, psi.deleted, "
            + "psi.eventdatavalues from programstageinstance psi where psi.uid in (:ids)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", psiUid );

        return jdbcTemplate.query( sql, parameters, rs -> {
            Map<String, ProgramStageInstance> results = new HashMap<>();

            while ( rs.next() )
            {
                ProgramStageInstance psi = new ProgramStageInstance();
                psi.setId( rs.getLong( "programstageinstanceid" ) );
                psi.setUid( rs.getString( "uid" ) );
                psi.setStatus( EventStatus.valueOf( rs.getString( "status" ) ) );
                psi.setDeleted( rs.getBoolean( "deleted" ) );

                try
                {
                    psi.setEventDataValues( jsonToEventDataValues( rs.getString(
                        "eventdatavalues" ) ) );
                }
                catch ( JsonProcessingException e )
                {
                    log.error(
                        "Invalid Data Element Values payload, skipping Program Stage Instance with id: " + psi.getId(),
                        e );
                }
                results.put( psi.getUid(), psi );

            }
            return results;
        } );
    }

    /**
     * Converts the Event Data Value json payload into a Set of EventDataValue
     * 
     * Note that the EventDataValue payload is stored as a map: {dataelementid:{
     * ...}, {dataelementid:{ ...} }
     * 
     * Therefore, the conversion is a bit convoluted, since the payload has to be
     * converted into a Map and then into a Set
     */
    private Set<EventDataValue> jsonToEventDataValues( String eventsDataValues )
        throws JsonProcessingException
    {
        Set<EventDataValue> dataValues = new HashSet<>();
        if ( !StringUtils.isEmpty( eventsDataValues ) )
        {
            Map<String, EventDataValue> parsed = jsonMapper.readValue( eventsDataValues, mapType );
            for ( String dataElementId : parsed.keySet() )
            {
                EventDataValue edv = parsed.get( dataElementId );
                edv.setDataElement( dataElementId );
                dataValues.add( edv );
            }
        }

        return dataValues;
    }
}
