/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.eventhook;

import java.util.Map;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobStatus;

import com.google.common.base.CaseFormat;

/**
 * @author Morten Olav Hansen
 */
public final class EventUtils
{
    public static Event metadataCreate( BaseIdentifiableObject object )
    {
        return metadata( object, "create" );
    }

    public static Event metadataUpdate( BaseIdentifiableObject object )
    {
        return metadata( object, "update" );
    }

    public static Event metadataDelete( Class<?> type, String uid )
    {
        String name = camelCase( type.getSimpleName() );

        return Event.builder()
            .path( String.format( "metadata.%s.%s", name, uid ) )
            .meta( Map.of( "op", "delete" ) )
            .object( Map.of( "id", uid ) )
            .build();
    }

    private static Event metadata( BaseIdentifiableObject object, String op )
    {
        String name = camelCase( object.getClass().getSimpleName() );

        return Event.builder()
            .path( String.format( "metadata.%s.%s", name, object.getUid() ) )
            .meta( Map.of( "op", op ) )
            .object( object )
            .build();
    }

    public static Event schedulerStart( JobConfiguration object )
    {
        return Event.builder()
            .path( String.format( "scheduler.%s.%s", object.getJobType(), object.getUid() ) )
            .meta( Map.of( "op", JobStatus.RUNNING ) )
            .object( object )
            .build();
    }

    public static Event schedulerCompleted( JobConfiguration object )
    {
        return Event.builder()
            .path( String.format( "scheduler.%s.%s", object.getJobType(), object.getUid() ) )
            .meta( Map.of( "op", JobStatus.COMPLETED ) )
            .object( object )
            .build();
    }

    public static Event schedulerFailed( JobConfiguration object )
    {
        return Event.builder()
            .path( String.format( "scheduler.%s.%s", object.getJobType(), object.getUid() ) )
            .meta( Map.of( "op", JobStatus.FAILED ) )
            .object( object )
            .build();
    }

    public static String camelCase( String name )
    {
        return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_CAMEL, name );
    }

    private EventUtils()
    {
    }
}
