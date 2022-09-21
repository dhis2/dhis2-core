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

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.commons.util.SystemUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JdbcProgramOrgUnitAssociationsStore
{

    private static final String SHARING_OUTER_QUERY_BEGIN = "select " +
        "    prg.uid, " +
        "    prg.agg_ou_uid " +
        "from (";

    private static final String SHARING_OUTER_QUERY_END = ") as prg";

    private static final String INNER_SQL_QUERY = "select " +
        "    pr.uid, " +
        "    array_agg(ou.uid) agg_ou_uid " +
        "from program pr " +
        "    left join program_organisationunits po on pr.programid = po.programid " +
        "    left join organisationunit ou on po.organisationunitid = ou.organisationunitid " +
        "where";

    private static final String INNER_QUERY_GROUPING_BY = "group by pr.uid";

    private final JdbcTemplate jdbcTemplate;

    private final CacheProvider cacheProvider;

    private final Environment env;

    private Cache<Set<String>> programOrgUnitAssociationCache;

    @PostConstruct
    public void init()
    {
        programOrgUnitAssociationCache = cacheProvider.newCacheBuilderForSet( String.class )
            .forRegion( "pgmOrgUnitAssocCache" )
            .expireAfterWrite( 1, TimeUnit.HOURS )
            .withInitialCapacity( 100 )
            .withMaximumSize( SystemUtils.isTestRun( env.getActiveProfiles() ) ? 0 : 1000 )
            .build();
    }

    @EventListener
    public void handleApplicationCachesCleared( ApplicationCacheClearedEvent event )
    {
        programOrgUnitAssociationCache.invalidateAll();
    }

    public SetValuedMap<String, String> getOrganisationUnitsAssociations( Set<String> uids )
    {
        SetValuedMap<String, String> setValuedMap = new HashSetValuedHashMap<String, String>();
        boolean cached = true;
        for ( String uid : uids )
        {
            Optional<Set<String>> orgUnitUids = programOrgUnitAssociationCache.get( uid );
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
                buildSqlQueryForRawAssociation( uids ),
                resultSet -> {
                    while ( resultSet.next() )
                    {
                        setValuedMap.putAll(
                            resultSet.getString( 1 ),
                            Arrays.asList( (String[]) resultSet.getArray( 2 ).getArray() ) );
                        programOrgUnitAssociationCache.put( resultSet.getString( 1 ),
                            new HashSet<String>( setValuedMap.get( resultSet.getString( 1 ) ) ) );
                    }
                    return setValuedMap;
                } );

            return setValuedMap;
        }
    }

    private String buildSqlQueryForRawAssociation( Set<String> uids )
    {
        Stream<String> queryParts = Stream.of(
            SHARING_OUTER_QUERY_BEGIN,
            innerQueryProvider( uids ),
            SHARING_OUTER_QUERY_END );

        return queryParts.collect( joining( " " ) );
    }

    private String innerQueryProvider( Set<String> programUids )
    {
        Stream<String> queryParts = Stream.of(
            INNER_SQL_QUERY,
            getProgramUidsFilter( programUids ) );

        queryParts = Stream.concat( queryParts, Stream.of( INNER_QUERY_GROUPING_BY ) );

        return queryParts.collect( joining( " " ) );
    }

    private String getProgramUidsFilter( Set<String> programUids )
    {
        return "pr.uid in (" +
            programUids.stream()
                .map( this::withQuotes )
                .collect( joining( "," ) )
            + ")";
    }

    private String withQuotes( String programUid )
    {
        return String.join( "", "'", programUid, "'" );
    }
}