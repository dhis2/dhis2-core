package org.hisp.dhis.dxf2.events.event;

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

import java.sql.SQLException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.postgresql.util.PGobject;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Luciano Fiandesio
 */
public class EventUtils
{
    public final static String FALLBACK_USERNAME = "[Unknown]";

    public static String getValidUsername( String userName, ImportOptions importOptions )
    {
        String validUsername = userName;
        String fallBack = importOptions.getUser() != null ? importOptions.getUser().getUsername() : FALLBACK_USERNAME;

        if ( StringUtils.isEmpty( validUsername ) )
        {
            validUsername = User.getSafeUsername( fallBack );
        }
        else if ( validUsername.length() > UserCredentials.USERNAME_MAX_LENGTH )
        {
            validUsername = User.getSafeUsername( fallBack );
        }

        return validUsername;
    }

    /**
     * Converts a Set of {@see EventDataValue} into a JSON string using the provided
     * Jackson {@see ObjectMapper} This method, before serializing to JSON, if first
     * transforms the Set into a Map, where the Map key is the EventDataValue
     * DataElement UID and the Map value is the actual {@see EventDataValue}.
     * 
     * @param dataValues a Set of {@see EventDataValue}
     * @param mapper a configured Jackson {@see ObjectMapper}
     * @return a String containing the serialized Set
     * @throws JsonProcessingException if the JSON serialization fails
     */
    public static PGobject eventDataValuesToJson( Set<EventDataValue> dataValues, ObjectMapper mapper )
        throws JsonProcessingException, SQLException
    {
        PGobject jsonbObj = new PGobject();
        jsonbObj.setType( "json" );
        jsonbObj.setValue( mapper.writeValueAsString(
            dataValues.stream().collect( Collectors.toMap( EventDataValue::getDataElement, Function.identity() ) ) ) );
        return jsonbObj;
    }
}
