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
package org.hisp.dhis.tracker.importer.databuilder;

import java.time.Instant;
import java.util.List;

import org.hisp.dhis.helpers.JsonObjectBuilder;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventDataBuilder implements TrackerImporterDataBuilder
{
    private JsonObjectBuilder builder;

    public EventDataBuilder()
    {
        this.builder = new JsonObjectBuilder();
        setEventDate( Instant.now().toString() );
        // setStatus( "ACTIVE" );
    }

    public EventDataBuilder setId( String id )
    {
        this.builder.addProperty( "event", id );
        return this;
    }

    public EventDataBuilder setProgramStage( String programStage )
    {
        this.builder.addProperty( "programStage", programStage );

        return this;
    }

    public EventDataBuilder setAssignedUser( String assignedUserId )
    {
        JsonObject assignedUser = new JsonObjectBuilder().addProperty( "uid", assignedUserId ).build();
        this.builder.addObject( "assignedUser", assignedUser );

        return this;
    }

    public EventDataBuilder setProgram( String program )
    {
        this.builder.addProperty( "program", program );

        return this;
    }

    public EventDataBuilder setAttributeCategoryOptions( List<String> categoryOptions )
    {
        this.builder.addProperty( "attributeCategoryOptions", String.join( ";", categoryOptions ) );

        return this;
    }

    public EventDataBuilder setStatus( String status )
    {
        this.builder.addProperty( "status", status );
        return this;
    }

    public EventDataBuilder setScheduledDate( String date )
    {
        builder.addProperty( "scheduledAt", date );
        return this;
    }

    public EventDataBuilder setEnrollment( String enrollment )
    {
        builder.addProperty( "enrollment", enrollment );
        return this;
    }

    public EventDataBuilder setTei( String tei )
    {
        builder.addProperty( "trackedEntity", tei );

        return this;
    }

    public EventDataBuilder addDataValue( String dataElementId, String value )
    {
        JsonObject dataValue = new JsonObjectBuilder()
            .addProperty( "dataElement", dataElementId )
            .addProperty( "value", value ).build();

        this.builder.addOrAppendToArray( "dataValues", dataValue );
        return this;
    }

    public EventDataBuilder setOu( String ou )
    {
        this.builder.addProperty( "orgUnit", ou );
        return this;
    }

    public EventDataBuilder setEventDate( String eventDate )
    {
        this.builder.addProperty( "occurredAt", eventDate );

        return this;
    }

    public EventDataBuilder addNote( String value )
    {
        JsonObject note = new JsonObjectBuilder()
            .addProperty( "value", value )
            .build();

        this.builder.addOrAppendToArray( "notes", note );

        return this;
    }

    public JsonObject array( String ou, String program, String programStage )
    {
        setOu( ou ).setProgram( program ).setProgramStage( programStage );

        return array();
    }

    public JsonObject array( String ou, String program, String programStage, String status )
    {
        setOu( ou ).setProgram( program ).setProgramStage( programStage ).setStatus( status );

        return array();
    }

    @Override
    public JsonObject single()
    {
        return this.builder.build();
    }

    @Override
    public JsonObject array()
    {
        return this.builder.wrapIntoArray( "events" );
    }
}
