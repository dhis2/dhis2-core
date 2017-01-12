package org.hisp.dhis.dxf2.events.event.csv;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.hisp.dhis.dxf2.events.event.Coordinate;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.event.EventStatus;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultCsvEventService implements CsvEventService
{
    private static final CsvMapper CSV_MAPPER = new CsvMapper().enable( CsvParser.Feature.WRAP_AS_ARRAY );

    private static final CsvSchema CSV_SCHEMA = CSV_MAPPER.schemaFor( CsvEventDataValue.class ).withLineSeparator( "\n" );

    @Override
    public void writeEvents( OutputStream outputStream, Events events, boolean withHeader ) throws IOException
    {
        ObjectWriter writer = CSV_MAPPER.writer( CSV_SCHEMA.withUseHeader( withHeader ) );

        List<CsvEventDataValue> dataValues = new ArrayList<>();

        for ( Event event : events.getEvents() )
        {
            CsvEventDataValue templateDataValue = new CsvEventDataValue();
            templateDataValue.setEvent( event.getEvent() );
            templateDataValue.setStatus( event.getStatus() != null ? event.getStatus().name() : null );
            templateDataValue.setProgram( event.getProgram() );
            templateDataValue.setProgramStage( event.getProgramStage() );
            templateDataValue.setEnrollment( event.getEnrollment() );
            templateDataValue.setOrgUnit( event.getOrgUnit() );
            templateDataValue.setEventDate( event.getEventDate() );
            templateDataValue.setDueDate( event.getDueDate() );
            templateDataValue.setStoredBy( event.getStoredBy() );

            if ( event.getCoordinate() != null )
            {
                templateDataValue.setLatitude( event.getCoordinate().getLatitude() );
                templateDataValue.setLongitude( event.getCoordinate().getLongitude() );
            }

            for ( DataValue value : event.getDataValues() )
            {
                CsvEventDataValue dataValue = new CsvEventDataValue( templateDataValue );
                dataValue.setDataElement( value.getDataElement() );
                dataValue.setValue( value.getValue() );
                dataValue.setProvidedElsewhere( value.getProvidedElsewhere() );

                if ( value.getStoredBy() != null )
                {
                    dataValue.setStoredBy( value.getStoredBy() );
                }

                dataValues.add( dataValue );
            }
        }

        writer.writeValue( outputStream, dataValues );
    }

    @Override
    public Events readEvents( InputStream inputStream, boolean skipFirst ) throws IOException
    {
        Events events = new Events();

        ObjectReader reader = CSV_MAPPER.readerFor( CsvEventDataValue.class )
            .with( CSV_SCHEMA.withSkipFirstDataRow( skipFirst ) );

        MappingIterator<CsvEventDataValue> iterator = reader.readValues( inputStream );
        Event event = new Event();
        event.setEvent( "not_valid" );

        while ( iterator.hasNext() )
        {
            CsvEventDataValue dataValue = iterator.next();

            if ( !event.getEvent().equals( dataValue.getEvent() ) )
            {
                event = new Event();
                event.setEvent( dataValue.getEvent() );
                event.setStatus( StringUtils.isEmpty( dataValue.getStatus() )
                    ? EventStatus.ACTIVE : Enum.valueOf( EventStatus.class, dataValue.getStatus() ) );
                event.setProgram( dataValue.getProgram() );
                event.setProgramStage( dataValue.getProgramStage() );
                event.setEnrollment( dataValue.getEnrollment() );
                event.setOrgUnit( dataValue.getOrgUnit() );
                event.setEventDate( dataValue.getEventDate() );
                event.setDueDate( dataValue.getDueDate() );

                if ( dataValue.getLongitude() != null && dataValue.getLatitude() != null )
                {
                    event.setCoordinate( new Coordinate( dataValue.getLongitude(), dataValue.getLatitude() ) );
                }

                events.getEvents().add( event );
            }

            DataValue value = new DataValue( dataValue.getDataElement(), dataValue.getValue() );
            value.setStoredBy( dataValue.getStoredBy() );
            value.setProvidedElsewhere( dataValue.getProvidedElsewhere() );

            event.getDataValues().add( value );
        }

        return events;
    }
}
