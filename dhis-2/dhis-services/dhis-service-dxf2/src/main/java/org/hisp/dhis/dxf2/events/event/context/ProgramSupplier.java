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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramsSupplier" )
public class ProgramSupplier extends AbstractSupplier<Map<String, Program>>
{
    private final static String PROGRAM_CACHE_KEY = "000P";

    // @formatter:off
    private final Cache<String, Map<String, Program>> programsCache = new Cache2kBuilder<String, Map<String, Program>>() {}
        .expireAfterWrite( 30, TimeUnit.MINUTES ) // expire/refresh after 30 minutes
        .build();
    // @formatter:on

    public ProgramSupplier( NamedParameterJdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    public Map<String, Program> get( List<Event> eventList )
    {
        Map<String, Program> programMap = programsCache.get( PROGRAM_CACHE_KEY );
        if ( programMap == null )
        {
            programMap = load();
            programsCache.put( PROGRAM_CACHE_KEY, programMap );
        }
        return programMap;
    }

    private Map<String, Program> load()
    {
        final String sql = "select p.programid, p.uid, p.name, p.type, c.uid as catcombo_uid, c.name as catcombo_name, "
            + "ps.programstageid as ps_id, ps.uid as ps_uid, ps.featuretype as ps_feature_type, ps.sort_order, string_agg(ou.uid, ', ') ous "
            + "from program p LEFT JOIN categorycombo c on p.categorycomboid = c.categorycomboid "
            + "        LEFT JOIN programstage ps on p.programid = ps.programid "
            + "        LEFT JOIN program_organisationunits pou on p.programid = pou.programid "
            + "        LEFT JOIN organisationunit ou on pou.organisationunitid = ou.organisationunitid "
            + "group by p.programid, p.uid, p.name, p.type, c.uid, c.name, ps.programstageid, ps.uid , ps.featuretype, ps.sort_order "
            + "order by p.programid, ps.sort_order";

        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<String, Program> results = new HashMap<>();
            long programId = 0;
            while ( rs.next() )
            {
                if ( programId != rs.getLong( "programid" ) )
                {
                    Set<ProgramStage> programStages = new HashSet<>();
                    Program program = new Program();
                    program.setId( rs.getLong( "programid" ) );
                    program.setUid( rs.getString( "uid" ) );
                    program.setName( rs.getString( "name" ) );
                    program.setProgramType( ProgramType.fromValue( rs.getString( "type" ) ) );

                    programStages.add( toProgramStage( rs ) );

                    CategoryCombo categoryCombo = new CategoryCombo();
                    categoryCombo.setUid( rs.getString( "catcombo_uid" ) );
                    categoryCombo.setName( rs.getString( "catcombo_name" ) );
                    program.setCategoryCombo( categoryCombo );

                    program.setProgramStages( programStages );
                    results.put( rs.getString( "uid" ), program );
                    String ous = rs.getString( "ous" );
                    if ( StringUtils.isNotEmpty( ous ) )
                    {
                        program.setOrganisationUnits( Arrays.stream( ous.split( "," ) ).map( s -> {
                            OrganisationUnit ou = new OrganisationUnit();
                            ou.setUid( s );
                            return ou;
                        } ).collect( Collectors.toSet() ) );
                    }

                    programId = program.getId();
                }
                else
                {
                    results.get( rs.getString( "uid" ) ).getProgramStages().add( toProgramStage( rs ) );
                }
            }
            return results;
        } );
    }

    private ProgramStage toProgramStage( ResultSet rs )
        throws SQLException
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setId( rs.getLong( "ps_id" ) );
        programStage.setUid( rs.getString( "ps_uid" ) );
        programStage.setSortOrder( rs.getInt( "sort_order" ) );
        programStage.setFeatureType(
            rs.getString( "ps_feature_type" ) != null ? FeatureType.getTypeFromName( rs.getString( "ps_feature_type" ) )
                : FeatureType.NONE );

        return programStage;
    }
}
