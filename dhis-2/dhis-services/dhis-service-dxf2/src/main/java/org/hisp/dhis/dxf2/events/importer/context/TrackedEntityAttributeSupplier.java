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
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifierBasedOnIdScheme;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component( "workContextTrackedEntityAttributeSupplier" )
public class TrackedEntityAttributeSupplier extends AbstractSupplier
{

    private static final String TEA_CACHE_KEY = "000TEA";

    private static final String ATTRIBUTESCHEME_COL = "attributevalues";

    private final Cache<Map<String, TrackedEntityAttribute>> trackedEntityAttributeCache;

    public TrackedEntityAttributeSupplier( NamedParameterJdbcTemplate jdbcTemplate, CacheProvider cacheProvider,
        Environment environment )
    {
        super( jdbcTemplate, environment );
        this.trackedEntityAttributeCache = cacheProvider.createTeiAttributesCache();
    }

    public Map<String, TrackedEntityAttribute> get( ImportOptions importOptions, Set<String> uids )
    {
        if ( isEmpty( uids ) )
        {
            return new HashMap<>();
        }

        if ( importOptions.isSkipCache() )
        {
            trackedEntityAttributeCache.invalidateAll();
        }

        Map<String, TrackedEntityAttribute> trackedEntityAttributeMap = trackedEntityAttributeCache.get( TEA_CACHE_KEY )
            .orElse( new HashMap<>() );

        if ( requiresCacheReload( uids, trackedEntityAttributeMap ) )
        {
            trackedEntityAttributeMap = getTrackedEntityAttributeMap(
                importOptions.getIdSchemes().getTrackedEntityAttributeIdScheme(),
                uids );

            trackedEntityAttributeCache.put( TEA_CACHE_KEY, trackedEntityAttributeMap );
        }

        return trackedEntityAttributeMap;
    }

    private boolean requiresCacheReload( Set<String> uids,
        Map<String, TrackedEntityAttribute> trackedEntityAttributeMap )
    {
        final Set<String> teaInCache = trackedEntityAttributeMap.keySet();

        for ( String program : uids )
        {
            if ( !teaInCache.contains( program ) )
            {
                return true;
            }
        }
        return false;
    }

    public Map<String, TrackedEntityAttribute> getTrackedEntityAttributeMap( IdScheme idScheme,
        Set<String> uids )
    {
        if ( isEmpty( uids ) )
        {
            return new HashMap<>();
        }

        String sql = "select trackedentityattributeid , name , uid, code, description, sharing, valuetype ";

        if ( idScheme.isAttribute() )
        {
            sql += ",t.attributevalues->'" + idScheme.getAttribute()
                + "'->>'value' as " + ATTRIBUTESCHEME_COL;
        }

        sql += "from trackedentityattribute t where t.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", uids );

        Map<String, TrackedEntityAttribute> trackedEntityAttributeMap = new HashMap<>();

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            while ( rs.next() )
            {
                TrackedEntityAttribute trackedEntityAttribute = mapFromResultSet( rs );
                trackedEntityAttributeMap.put( idScheme.isAttribute() ? rs.getString( ATTRIBUTESCHEME_COL )
                    : getIdentifierBasedOnIdScheme( trackedEntityAttribute, idScheme ), trackedEntityAttribute );
            }
            return trackedEntityAttributeMap;
        } );

    }

    private TrackedEntityAttribute mapFromResultSet( ResultSet rs )
        throws SQLException
    {
        TrackedEntityAttribute pi = new TrackedEntityAttribute();
        pi.setId( rs.getLong( "trackedentityattributeid" ) );
        pi.setUid( rs.getString( "uid" ) );
        pi.setName( rs.getString( "name" ) );
        pi.setDescription( rs.getString( "description" ) );
        pi.setValueType( ValueType.valueOf( rs.getString( "valuetype" ) ) );

        return pi;
    }
}
