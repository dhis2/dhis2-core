/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.program.jdbc;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.association.IdentifiableObjectAssociations;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JdbcProgramOrgUnitAssociationsStore
{

    public static final String BASE_SQL_QUERY = "select pr.uid, array_agg(ou.uid) " +
        "from program pr " +
        "inner join program_organisationunits po on pr.programid = po.programid " +
        "inner join organisationunit ou on po.organisationunitid = ou.organisationunitid " +
        "where ";

    private final CurrentUserService currentUserService;

    private final JdbcTemplate jdbcTemplate;

    public IdentifiableObjectAssociations getProgramOrganisationUnitsAssociations( Set<String> programUids )
    {

        Set<String> userOrgUnitPaths = currentUserService.getCurrentUserOrganisationUnits().stream()
            .map( OrganisationUnit::getPath )
            .collect( Collectors.toSet() );

        return jdbcTemplate.query(
            buildSqlQuery( programUids, userOrgUnitPaths, currentUserService.getCurrentUser() ),
            resultSet -> {
                IdentifiableObjectAssociations identifiableObjectAssociations = new IdentifiableObjectAssociations();
                while ( resultSet.next() )
                {
                    identifiableObjectAssociations.addAllAssociations(
                        resultSet.getString( 1 ),
                        Arrays.asList( (String[]) resultSet.getArray( 2 ).getArray() ) );

                }
                return identifiableObjectAssociations;
            } );
    }

    private String buildSqlQuery( Set<String> programUids, Set<String> userOrgUnitPaths, User currentUser )
    {
        // TODO: Restrict programs to current user sharing access
        String sql = BASE_SQL_QUERY + getProgramUidsFilter( programUids );

        if ( Objects.nonNull( currentUser ) && !currentUser.isSuper() )
        {
            sql += getUserOrgUnitPathsFilter( userOrgUnitPaths );
        }

        sql += " group by pr.uid";

        sql += " union all " + getUnionQuery( programUids );

        return sql;
    }

    private String getUnionQuery( Set<String> programUids )
    {
        return "select pr.uid, '{}' from program pr" +
            " where " + getProgramUidsFilter( programUids ) + " and not exists(" +
            "    select 1 from program_organisationunits po" +
            "        where po.programid = pr.programid" +
            "    ) ";
    }

    private String getProgramUidsFilter( Set<String> programUids )
    {
        return "pr.uid in ( " +
            programUids.stream()
                .map( this::withQuotes )
                .collect( joining( "," ) )
            + " )";
    }

    private String withQuotes( String programUid )
    {
        return "'" + programUid + "'";
    }

    private String getUserOrgUnitPathsFilter( Set<String> userOrgUnitPaths )
    {
        if ( userOrgUnitPaths != null && userOrgUnitPaths.size() > 0 )
        {
            return " and " +
                userOrgUnitPaths.stream()
                    .map( s -> "ou.path like '" + s + "%'" )
                    .collect( joining( " or ", "(", ")" ) );
        }
        return "";
    }
}
