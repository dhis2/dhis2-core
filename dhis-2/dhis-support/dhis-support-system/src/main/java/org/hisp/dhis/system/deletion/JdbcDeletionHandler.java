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
package org.hisp.dhis.system.deletion;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public abstract class JdbcDeletionHandler extends DeletionHandler
{
    private NamedParameterJdbcTemplate npTemplate;

    @Autowired
    public void setNamedParameterJdbcTemplate( NamedParameterJdbcTemplate npTemplate )
    {
        this.npTemplate = npTemplate;
    }

    protected final int count( String sql, Map<String, Object> parameters )
    {
        // OBS! Need to use queryForList to allow no result rows
        List<Integer> count = npTemplate.queryForList( sql, new MapSqlParameterSource( parameters ), Integer.class );
        return count.isEmpty() ? 0 : count.get( 0 );
    }

    protected final boolean exists( String sql, Map<String, Object> parameters )
    {
        return count( sql, parameters ) > 0;
    }

    protected final DeletionVeto vetoIfExists( DeletionVeto veto, String sql, Map<String, Object> parameters )
    {
        return exists( sql, parameters ) ? veto : DeletionVeto.ACCEPT;
    }

    protected final int delete( String sql, Map<String, Object> parameters )
    {
        return npTemplate.update( sql, parameters );
    }

    protected final String firstMatch( String sql, Map<String, Object> parameters )
    {
        if ( !sql.toLowerCase().contains( "limit 1" ) )
        {
            sql = sql + " limit 1";
        }
        List<String> names = npTemplate.queryForList( sql, new MapSqlParameterSource( parameters ), String.class );
        return names.isEmpty() ? null : names.get( 0 );
    }
}
