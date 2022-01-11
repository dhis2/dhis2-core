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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifierBasedOnIdScheme;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.UnrecoverableImportException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextOrgUnitsSupplier" )
public class OrganisationUnitSupplier extends AbstractSupplier
{
    private final static String ATTRIBUTESCHEME_COL = "attributevalues";

    public OrganisationUnitSupplier( NamedParameterJdbcTemplate jdbcTemplate, Environment environment )
    {
        super( jdbcTemplate, environment );
    }

    public Map<String, OrganisationUnit> get( ImportOptions importOptions, Set<String> uids,
        Multimap<String, String> orgUnitToEntity )
    {
        if ( isEmpty( uids ) )
        {
            return new HashMap<>();
        }

        return fetchOu( importOptions.getIdSchemes().getOrgUnitIdScheme(), uids, orgUnitToEntity );
    }

    private Map<String, OrganisationUnit> fetchOu( IdScheme idScheme, Set<String> orgUnitUids,
        Multimap<String, String> orgUnitToEntity )
    {
        String sql = "select ou.organisationunitid, ou.uid, ou.code, ou.name, ou.path, ou.hierarchylevel ";

        if ( idScheme.isAttribute() )
        {
            //
            // Attribute IdScheme handling: use Postgres JSONB custom clauses to
            // query the
            // "attributvalues" column
            //
            // The column is expected to contain a JSON structure like so:
            //
            // {"ie9wfkGw8GX": {"value": "Some value", "attribute": {"id":
            // "ie9wfkGw8GX"}}}
            //
            // The 'ie9wfkGw8GX' uid is the attribute identifier
            //

            final String attribute = idScheme.getAttribute();

            sql += ",attributevalues->'" + attribute
                + "'->>'value' as " + ATTRIBUTESCHEME_COL + " from organisationunit ou where ou.attributevalues#>>'{"
                + attribute
                + ",value}' in (:ids)";
        }
        else
        {
            sql += "from organisationunit ou where ou."
                + IdSchemeUtils.getColumnNameByScheme( idScheme, "organisationunitid" ) + " in (:ids)";
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", orgUnitUids );

        Map<String, OrganisationUnit> organisationUnitMap = new HashMap<>();

        return jdbcTemplate.query( sql, parameters, rs -> {

            while ( rs.next() )
            {
                try
                {
                    OrganisationUnit organisationUnit = mapFromResultSet( rs );

                    for ( String entityIdentifier : orgUnitToEntity
                        .get( idScheme.isAttribute() ? rs.getString( ATTRIBUTESCHEME_COL )
                            : getIdentifierBasedOnIdScheme( organisationUnit, idScheme ) ) )
                    {
                        organisationUnitMap.put( entityIdentifier, organisationUnit );
                    }

                }
                catch ( Exception e )
                {
                    throw new UnrecoverableImportException( e );
                }
            }
            return organisationUnitMap;

        } );
    }

    private OrganisationUnit mapFromResultSet( ResultSet rs )
        throws SQLException
    {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setId( rs.getLong( "organisationunitid" ) );
        organisationUnit.setUid( rs.getString( "uid" ) );
        organisationUnit.setCode( rs.getString( "code" ) );
        organisationUnit.setName( rs.getString( "name" ) );
        String path = rs.getString( "path" );
        organisationUnit.setPath( path );
        organisationUnit.setHierarchyLevel( rs.getInt( "hierarchylevel" ) );
        organisationUnit.setParent( SupplierUtils.getParentHierarchy( organisationUnit, path ) );

        return organisationUnit;
    }
}
