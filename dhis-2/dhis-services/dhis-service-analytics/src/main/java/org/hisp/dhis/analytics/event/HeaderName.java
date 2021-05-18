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
package org.hisp.dhis.analytics.event;

/**
 * Represents the analytics events headers.
 *
 * @author maikel arabori
 */
public enum HeaderName
{
    NAME_EVENT( "Event" ),
    NAME_TRACKED_ENTITY_INSTANCE( "Tracked entity instance" ),
    NAME_PROGRAM_INSTANCE( "Program instance" ),
    NAME_PROGRAM_STAGE( "Program stage" ),
    NAME_EVENT_DATE( "Event date" ),
    NAME_ENROLLMENT_DATE( "Enrollment date" ),
    NAME_INCIDENT_DATE( "Incident date" ),
    NAME_GEOMETRY( "Geometry" ),
    NAME_LONGITUDE( "Longitude" ),
    NAME_LATITUDE( "Latitude" ),
    NAME_ORG_UNIT_NAME( "Organisation unit name" ),
    NAME_ORG_UNIT_CODE( "Organisation unit code" ),
    NAME_COUNT( "Count" ),
    NAME_CENTER( "Center" ),
    NAME_EXTENT( "Extent" ),
    NAME_POINTS( "Points" );

    private final String value;

    private HeaderName( final String value )
    {
        this.value = value;
    }

    public static HeaderName from( final String value )
    {
        for ( final HeaderName names : HeaderName.values() )
        {
            if ( names.value().equalsIgnoreCase( value ) )
            {
                return names;
            }
        }

        return null;
    }

    public String value()
    {
        return value;
    }
}
