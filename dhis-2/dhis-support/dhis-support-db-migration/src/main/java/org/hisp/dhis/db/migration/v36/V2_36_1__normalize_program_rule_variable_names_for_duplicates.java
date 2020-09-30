package org.hisp.dhis.db.migration.v36;

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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Slf4j
public class V2_36_1__normalize_program_rule_variable_names_for_duplicates
    extends BaseJavaMigration
{

    @Override
    public void migrate( Context context )
        throws Exception
    {
        getCandidates( context.getConnection() )
            .forEach( candidate -> renameOccurrencesWithPrefix( candidate, context.getConnection() ) );
    }

    private List<Pair<Long, String>> getCandidates( Connection connection )
        throws SQLException
    {

        final String candidateDetectionSql = "SELECT programid, name" +
            " FROM programrulevariable " +
            " group by programid, name " +
            " having count(*) > 1";

        List<Pair<Long, String>> candidates = new ArrayList<>();

        try (final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery( candidateDetectionSql ))
        {
            while ( rs.next() )
            {
                long programId = rs.getLong( "programid" );
                String programRuleVariableName = rs.getString( "name" );

                candidates.add( Pair.of( programId, programRuleVariableName ) );
            }
        }
        return candidates;
    }

    @SneakyThrows
    private void renameOccurrencesWithPrefix( Pair<Long, String> candidate, Connection connection )
    {
        Long programId = candidate.getLeft();
        String variableName = candidate.getRight();

        final String programRulesToRenameSql = "SELECT uid, name" +
            " FROM programrulevariable where programid = " + programId +
            " AND name like '" + variableName + "%'";

        Map<String, String> uidWithNewNames = new HashMap<>();

        try (final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery( programRulesToRenameSql ))
        {
            while ( rs.next() )
            {

                uidWithNewNames.put( rs.getString( "uid" ), rs.getString( "name" ) );

            }
        }
        renameAll( variableName, uidWithNewNames, new HashSet<>( uidWithNewNames.values() ), connection );
    }

    private void renameAll( String variableName, Map<String, String> uidWithNewNames, Set<String> existingNames,
        Connection connection )
    {
        uidWithNewNames.entrySet().stream()
            .filter( entry -> entry.getValue().equals( variableName ) )
            .skip( 1 )
            .map( entry -> renameOne( entry, existingNames, connection ) )
            .forEach( updateQuery -> executeUpdate( updateQuery, connection ) );
    }

    @SneakyThrows
    private void executeUpdate( String updateQuery, Connection connection )
    {
        try (final Statement stmt = connection.createStatement())
        {
            stmt.executeUpdate( updateQuery );
        }
    }

    @SneakyThrows
    private String renameOne( Map.Entry<String, String> uidNameEntry, Set<String> existingNames, Connection connection )
    {
        return "UPDATE programrulevariable SET name='" + findAvailableName( uidNameEntry.getValue(), existingNames )
            + "' WHERE uid= '" + uidNameEntry.getKey() + "'";
    }

    private String findAvailableName( String originalVariableName, Set<String> existingNames )
    {
        int i = 2;
        while ( i < 99 )
        {
            String proposedName = originalVariableName + "-" + i;
            if ( !existingNames.contains( proposedName ) )
            {
                existingNames.add( proposedName );
                return proposedName;
            }
            i++;
        }
        throw new IllegalStateException( "Unable to detect a unique name for rule variable " + originalVariableName );
    }
}
