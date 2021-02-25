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
package org.hisp.dhis.program.jdbc;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.association.IdentifiableObjectAssociations;
import org.hisp.dhis.association.ProgramOrganisationUnitAssociationsQueryBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JdbcProgramOrgUnitAssociationsStore
{

    private final CurrentUserService currentUserService;

    private final JdbcTemplate jdbcTemplate;

    private final ProgramOrganisationUnitAssociationsQueryBuilder queryBuilder;

    public IdentifiableObjectAssociations getProgramOrganisationUnitsAssociations( Set<String> programUids )
    {

        Set<String> userOrgUnitPaths = getUserOrgUnitPaths();

        return jdbcTemplate.query(
            queryBuilder.buildSqlQuery( programUids, userOrgUnitPaths, currentUserService.getCurrentUser() ),
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

    private Set<String> getUserOrgUnitPaths()
    {
        return currentUserService.getCurrentUserOrganisationUnits().stream()
            .map( OrganisationUnit::getPath )
            .collect( Collectors.toSet() );
    }

}
