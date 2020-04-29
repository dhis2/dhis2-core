package org.hisp.dhis.dxf2.events.event.context;

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

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextAssignedUsersSupplier" )
public class AssignedUserSupplier extends AbstractSupplier<Map<String, User>>
{
    public AssignedUserSupplier( NamedParameterJdbcTemplate namedParameterJdbcTemplate )
    {
        super( namedParameterJdbcTemplate );
    }

    @Override
    public Map<String, User> get( List<Event> events )
    {
        // @formatter:off
        // Collect all the org unit uids to pass as SQL query argument
        Set<String> userUids = events.stream()
                .filter( e -> StringUtils.isNotEmpty(e.getAssignedUser()))
                .map( Event::getAssignedUser )
                .collect( Collectors.toSet() );

        // Create a map user -> event
        Map<String, String> userToEvent = events.stream()
                .filter( e -> e.getAssignedUser() != null )
                .collect( Collectors.toMap( Event::getAssignedUser, Event::getUid  ) );
        // @formatter:on

        if ( !userUids.isEmpty() )
        {
            final String sql = "select u.userid, u.uid, u.disabled from users u where u.uid in (:ids);";

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue( "ids", userUids );

            return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
                Map<String, User> results = new HashMap<>();

                while ( rs.next() )
                {
                    User user = new User();
                    user.setId( rs.getLong( "organisationunitid" ) );
                    user.setUid( rs.getString( "uid" ) );

                    results.put( userToEvent.get( user.getUid() ), user );
                }
                return results;
            } );
        }
        return new HashMap<>();
    }
}
