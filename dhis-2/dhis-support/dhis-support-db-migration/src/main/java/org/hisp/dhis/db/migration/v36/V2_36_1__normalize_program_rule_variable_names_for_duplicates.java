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

import static org.hisp.dhis.db.migration.v36.V2_36_1__normalize_program_rule_variable_names_for_duplicates.ProgramRuleMigrationUtils.findAvailableName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
        Collection<String> affectedRules = getCandidates( context.getConnection() ).stream()
            .map( candidate -> renameOccurrencesWithPrefix( candidate, context.getConnection() ) )
            .collect( Collectors.toSet() )
            .stream()
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );

        if ( !affectedRules.isEmpty() )
        {
            log.warn(
                "The following rules have variables whose names were formerly duplicated by some other variables. " +
                    "Some of the following rules might not work as expected after this migration, please review them: "
                    + affectedRules );
        }

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
    private Collection<String> renameOccurrencesWithPrefix( Pair<Long, String> candidate, Connection connection )
    {
        Long programId = candidate.getLeft();
        String variableName = candidate.getRight();

        final String programRulesVariableToRenameSql = "SELECT uid, name" +
            " FROM programrulevariable where programid = " + programId +
            " AND name like '" + variableName + "%'";

        Map<String, String> uidWithNewNames = new HashMap<>();

        try (final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery( programRulesVariableToRenameSql ))
        {
            while ( rs.next() )
            {

                uidWithNewNames.put( rs.getString( "uid" ), rs.getString( "name" ) );

            }
        }

        Collection<String> renamedVariableNames = ProgramRuleMigrationUtils.renameAll( variableName, uidWithNewNames,
            connection, this::getUpdateQuery );

        return getAffectedRules( renamedVariableNames, connection );

    }

    @SneakyThrows
    private Collection<String> getAffectedRules( Collection<String> renamedVariableNames, Connection connection )
    {
        if ( !renamedVariableNames.isEmpty() )
        {
            String affectedRulesSql = "SELECT name FROM programrule WHERE " + renamedVariableNames.stream()
                .map( variableName -> "rulecondition LIKE '%{" + variableName + "}%'" )
                .collect( Collectors.joining( " OR " ) );

            try (final Statement stmt = connection.createStatement();
                ResultSet resultSet = stmt.executeQuery( affectedRulesSql ))
            {
                Collection<String> rules = new HashSet<>();

                while ( resultSet.next() )
                {
                    String ruleName = resultSet.getString( "name" );
                    rules.add( ruleName );
                }
                return rules;
            }
        }
        return Collections.emptySet();
    }

    @SneakyThrows
    private String getUpdateQuery( Map.Entry<String, String> uidNameEntry, Set<String> existingNames )
    {
        return "UPDATE programrulevariable SET name='" + findAvailableName( uidNameEntry.getValue(), existingNames )
            + "' WHERE uid= '" + uidNameEntry.getKey() + "'";
    }

    static class ProgramRuleMigrationUtils
    {

        static String findAvailableName( String originalVariableName, Set<String> existingNames )
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
            throw new IllegalStateException(
                "Unable to detect a unique name for rule variable " + originalVariableName );
        }

        @SneakyThrows
        static void executeUpdate( String updateQuery, Connection connection )
        {
            try (final Statement stmt = connection.createStatement())
            {
                stmt.executeUpdate( updateQuery );
            }
        }

        static Collection<String> renameAll( String variableName, Map<String, String> uidWithNewNames,
            Connection connection,
            BiFunction<Map.Entry<String, String>, Set<String>, String> updateQuerySupplier )
        {
            Collection<String> renamedVariableNames = new HashSet<>();

            Set<String> existingNames = new HashSet<>( uidWithNewNames.values() );

            uidWithNewNames.entrySet().stream()
                .filter( entry -> entry.getValue().equals( variableName ) )
                .skip( 1 )
                .peek( entry -> renamedVariableNames.add( entry.getValue() ) )
                .map( entry -> updateQuerySupplier.apply( entry, existingNames ) )
                .forEach( updateQuery -> executeUpdate( updateQuery, connection ) );

            return renamedVariableNames;
        }
    }
}
