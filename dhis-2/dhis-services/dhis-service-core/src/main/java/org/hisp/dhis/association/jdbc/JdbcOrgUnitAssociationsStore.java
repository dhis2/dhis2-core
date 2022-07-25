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
package org.hisp.dhis.association.jdbc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.hisp.dhis.association.AbstractOrganisationUnitAssociationsQueryBuilder;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class JdbcOrgUnitAssociationsStore
{
    private final CurrentUserService currentUserService;

    private final JdbcTemplate jdbcTemplate;

    private final AbstractOrganisationUnitAssociationsQueryBuilder queryBuilder;

    private final Cache<Set<String>> orgUnitAssociationCache;

    public SetValuedMap<String, String> getOrganisationUnitsAssociationsForCurrentUser( Set<String> objectUids )
    {
        Set<String> userOrgUnitPaths = getUserOrgUnitPaths();

        return jdbcTemplate.query(
            queryBuilder.buildSqlQuery( objectUids, userOrgUnitPaths, currentUserService.getCurrentUser() ),
            resultSet -> {
                SetValuedMap<String, String> setValuedMap = new HashSetValuedHashMap<>();
                while ( resultSet.next() )
                {
                    setValuedMap.putAll(
                        resultSet.getString( 1 ),
                        Arrays.asList( (String[]) resultSet.getArray( 2 ).getArray() ) );

                }
                return setValuedMap;
            } );
    }

    public SetValuedMap<String, String> getOrganisationUnitsAssociations( Set<String> uids )
    {
        SetValuedMap<String, String> setValuedMap = new HashSetValuedHashMap<>();

        boolean cached = true;
        for ( String uid : uids )
        {
            Optional<Set<String>> orgUnitUids = orgUnitAssociationCache.get( uid );
            if ( !orgUnitUids.isPresent() )
            {
                cached = false;
                break;
            }
            else
            {
                setValuedMap.putAll( uid, orgUnitUids.get() );
            }
        }

        if ( cached )
        {
            return setValuedMap;
        }
        else
        {
            setValuedMap.clear();
            jdbcTemplate.query(
                queryBuilder.buildSqlQueryForRawAssociation( uids ),
                resultSet -> {
                    while ( resultSet.next() )
                    {
                        setValuedMap.putAll(
                            resultSet.getString( 1 ),
                            Arrays.asList( (String[]) resultSet.getArray( 2 ).getArray() ) );
                        orgUnitAssociationCache.put( resultSet.getString( 1 ),
                            new HashSet<>( setValuedMap.get( resultSet.getString( 1 ) ) ) );

                    }
                    return setValuedMap;
                } );
            return setValuedMap;
        }
    }

    private Set<String> getUserOrgUnitPaths()
    {
        Set<String> allUserOrgUnitPaths = currentUserService.getCurrentUserOrganisationUnits().stream()
            .map( OrganisationUnit::getPath )
            .collect( Collectors.toSet() );

        return allUserOrgUnitPaths.stream()
            .filter( orgUnitPath -> !existsAnchestor( orgUnitPath, allUserOrgUnitPaths ) )
            .collect( Collectors.toSet() );
    }

    private boolean existsAnchestor( String orgUnitPath, Set<String> allUserOrgUnitPaths )
    {
        return allUserOrgUnitPaths.stream()
            .anyMatch( path -> !path.equals( orgUnitPath ) && orgUnitPath.startsWith( path ) );
    }
}
