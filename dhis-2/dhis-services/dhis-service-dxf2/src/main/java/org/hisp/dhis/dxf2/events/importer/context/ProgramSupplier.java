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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramSupplier" )
public class ProgramSupplier extends AbstractSupplier<Map<String, Program>>
{
    private final static String PROGRAM_CACHE_KEY = "000P";

    private final static String ATTRIBUTESCHEME_COL = "attributevalues";

    private final Cache<Map<String, Program>> programsCache;

    public ProgramSupplier( NamedParameterJdbcTemplate jdbcTemplate, CacheProvider cacheProvider )
    {
        super( jdbcTemplate );
        programsCache = cacheProvider.createProgramCache();
    }

    public Map<String, Program> get( ImportOptions importOptions, Set<String> uids )
    {
        if ( isEmpty( uids ) )
        {
            return new HashMap<>();
        }

        return getProgramByUid( importOptions.getIdSchemes().getProgramIdScheme(),
            uids );
    }

    private Map<String, Program> getProgramByUid( IdScheme idScheme,
        Set<String> uids )
    {
        if ( isEmpty( uids ) )
        {
            return new HashMap<>();
        }

        String sql = "select p.programid as programid, p.uid as program_uid, p.onlyenrollonce, p.displayincidentdate, p.featuretype, p.selectenrollmentdatesinfuture, p.selectincidentdatesinfuture, p.type, p.sharing ";

        if ( idScheme.isAttribute() )
        {
            sql += ",p.attributevalues->'" + idScheme.getAttribute()
                + "'->>'value' as " + ATTRIBUTESCHEME_COL;
        }

        sql += "from program p left join program_attributes pa on p.programid = pa.programid where p.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", uids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, Program> results = new HashMap<>();

            while ( rs.next() )
            {
                Program pi = mapFromResultSet( rs, idScheme );
                results.put( pi.getUid(), pi );
            }
            return results;
        } );

    }

    private Program mapFromResultSet( ResultSet rs, IdScheme idScheme )
        throws SQLException
    {
        Program pi = new Program();
        pi.setId( rs.getLong( "programid" ) );
        pi.setUid( rs.getString( "program_uid" ) );
        pi.setOnlyEnrollOnce( rs.getBoolean( "onlyenrollonce" ) );
        pi.setDisplayIncidentDate( rs.getBoolean( "displayincidentdate" ) );
        pi.setFeatureType(
            Optional.ofNullable( rs.getString( "featuretype" ) ).map( FeatureType::valueOf ).orElse( null ) );
        pi.setSelectEnrollmentDatesInFuture( rs.getBoolean( "selectenrollmentdatesinfuture" ) );
        pi.setSelectIncidentDatesInFuture( rs.getBoolean( "selectincidentdatesinfuture" ) );
        pi.setProgramType( Optional.ofNullable( rs.getString( "type" ) ).map( ProgramType::valueOf ).orElse( null ) );
        pi.setSharing( toSharing( rs.getString( "sharing" ) ) );

        if ( idScheme.isAttribute() )
        {
            pi.setAttributeValues(
                new HashSet<>(
                    Collections.singletonList( new AttributeValue( rs.getString( ATTRIBUTESCHEME_COL ) ) ) ) );
        }

        return pi;
    }
}
