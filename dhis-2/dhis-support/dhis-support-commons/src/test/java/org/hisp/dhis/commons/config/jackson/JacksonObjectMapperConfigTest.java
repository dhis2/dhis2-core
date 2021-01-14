package org.hisp.dhis.commons.config.jackson;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.commons.config.JacksonObjectMapperConfig;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class JacksonObjectMapperConfigTest
{
    private ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

    @Test
    public void testIsoDateSupport()
        throws JsonProcessingException
    {
        Map<String, Date> yearTest = jsonMapper.readValue( createDateTest( "2019" ), new DateMapTypeReference() );
        assertEquals( yearTest.get( "date" ), DateUtils.parseDate( "2019" ) );
        Map<String, Date> yearMonthTest = jsonMapper.readValue( createDateTest( "2019-01" ),
            new DateMapTypeReference() );
        assertEquals( yearMonthTest.get( "date" ), DateUtils.parseDate( "2019-01" ) );
    }

    @Test
    public void testUnixEpochTimestamp()
        throws JsonProcessingException
    {
        Map<String, Date> unixEpochDateString = jsonMapper.readValue( createDateTest( "1575118800000" ),
            new DateMapTypeReference() );
        assertEquals( unixEpochDateString.get( "date" ), new Date( 1575118800000L ) );

        Map<String, Date> unixEpochDateLong = jsonMapper.readValue( createUnixEpochTest( 1575118800000L ),
            new DateMapTypeReference() );
        assertEquals( unixEpochDateLong.get( "date" ), new Date( 1575118800000L ) );
    }

    @Test
    public void testNullDate()
        throws JsonProcessingException
    {
        Map<String, Date> yearTest = jsonMapper.readValue( createDateTest( null ), new DateMapTypeReference() );
        assertNull( yearTest.get( "date" ) );
    }

    @Test // DHIS2-8582
    public void testSerializerUserWithUser()
        throws JsonProcessingException
    {
        User user = new User();
        user.setAutoFields();
        user.setUser( user );
        user.setLastUpdatedBy( user );

        String payload = jsonMapper.writeValueAsString( user );
        User testUser = jsonMapper.readValue( payload, User.class );

        assertNotNull( user.getUser() );
        assertNotNull( user.getLastUpdatedBy() );

        assertEquals( user.getUid(), testUser.getUid() );
        assertEquals( user.getUid(), user.getUser().getUid() );
        assertEquals( user.getUid(), user.getLastUpdatedBy().getUid() );
    }

    private String createDateTest( String str )
    {
        if ( str == null )
        {
            return "{\"date\": null }";
        }

        return String.format( "{\"date\": \"%s\"}", str );
    }

    private String createUnixEpochTest( long ts )
    {
        return String.format( "{\"date\": %d}", ts );
    }

    private static class DateMapTypeReference
        extends
        TypeReference<Map<String, Date>>
    {
    }
}
