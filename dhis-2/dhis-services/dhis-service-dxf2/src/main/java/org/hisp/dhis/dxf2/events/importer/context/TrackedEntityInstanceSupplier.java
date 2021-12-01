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

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * @author Cambi Luca
 */
@Component( "workContextTrackedEntityInstancesSupplier" )
public class TrackedEntityInstanceSupplier extends AbstractSupplier<Set<TrackedEntityInstance>>
{

    public TrackedEntityInstanceSupplier( NamedParameterJdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    public Set<TrackedEntityInstance> get( Set<String> teiUids )
    {
        if ( isEmpty( teiUids ) )
        {
            return new HashSet<>();
        }

        return getTrackedEntityInstances( teiUids );
    }

    private Set<TrackedEntityInstance> getTrackedEntityInstances( Set<String> teiUids )
    {
        final String sql = "select tei.trackedentityinstanceid, tei.uid, tei.code, tei.deleted, tei.created, tei.createdatclient, tei.lastupdatedatclient, tei.lastupdated"
            +
            ", tet.trackedentitytypeid, tet.uid as tet_uid, tet.code as tet_code, tet.name as tet_name, tet.description as tet_description, ou.organisationunitid as ou_id, ou.uid as ou_uid, ou.path as ou_path "
            +
            "from trackedentityinstance tei  join organisationunit ou on tei.organisationunitid = ou.organisationunitid "
            +
            "left join trackedentitytype tet on tei.trackedentitytypeid = tet.trackedentitytypeid where tei.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", teiUids );

        Set<TrackedEntityInstance> trackedEntityInstances = new HashSet<>();
        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {

            while ( rs.next() )
            {
                TrackedEntityInstance tei = new TrackedEntityInstance();
                tei.setId( rs.getLong( "trackedentityinstanceid" ) );
                tei.setUid( rs.getString( "uid" ) );
                tei.setCode( rs.getString( "code" ) );
                tei.setDeleted( Boolean.parseBoolean( rs.getString( "deleted" ) ) );
                tei.setCreated( rs.getTimestamp( "created" ) );
                tei.setCreatedAtClient( rs.getTimestamp( "createdatclient" ) );
                tei.setLastUpdatedAtClient( rs.getTimestamp( "lastupdatedatclient" ) );
                tei.setLastUpdated( rs.getTimestamp( "lastupdated" ) );

                TrackedEntityType trackedEntityType = new TrackedEntityType();
                trackedEntityType.setId( rs.getLong( "trackedentitytypeid" ) );
                trackedEntityType.setUid( rs.getString( "tet_uid" ) );
                trackedEntityType.setCode( rs.getString( "tet_code" ) );
                trackedEntityType.setName( rs.getString( "tet_name" ) );
                trackedEntityType.setDescription( rs.getString( "tet_description" ) );

                tei.setTrackedEntityType( trackedEntityType );
                String teiOuUid = rs.getString( "ou_uid" );
                if ( teiOuUid != null )
                {
                    OrganisationUnit organisationUnit = new OrganisationUnit();
                    organisationUnit.setId( rs.getLong( "ou_id" ) );
                    organisationUnit.setUid( teiOuUid );
                    organisationUnit
                        .setParent(
                            SupplierUtils.getParentHierarchy( organisationUnit, rs.getString( "ou_path" ) ) );
                    tei.setOrganisationUnit( organisationUnit );
                }

                tei.setTrackedEntityAttributeValues( getTrackedEntityAttrValue( tei.getId() ) );
                trackedEntityInstances.add( tei );
            }
            return trackedEntityInstances;
        } );
    }

    private Set<TrackedEntityAttributeValue> getTrackedEntityAttrValue(Long teiId )
    {
        final String sql = "select teav.trackedentityinstanceid, teav.trackedentityattributeid, teav.value, tea.uid as tea_uid, tea.code as tea_code, tea.name as tea_name, tea.description as tea_description, tea.valuetype  from trackedentityattributevalue teav "
            +
            "join trackedentityattribute tea on teav.trackedentityattributeid  = tea.trackedentityattributeid where trackedentityinstanceid = :id";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "id", teiId );

        Set<TrackedEntityAttributeValue> trackedEntityInstances = new HashSet<>();
        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {

            while ( rs.next() )
            {
                TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
                trackedEntityAttributeValue.setValue( rs.getString( "tea_uid" ) );
                TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
                trackedEntityInstance.setId( rs.getLong( "trackedentityinstanceid" ) );

                trackedEntityAttributeValue.setEntityInstance( trackedEntityInstance );

                TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
                trackedEntityAttribute.setId( rs.getLong( "trackedentityattributeid" ) );
                trackedEntityAttribute.setUid( rs.getString( "tea_uid" ) );
                trackedEntityAttribute.setCode( rs.getString( "tea_code" ) );
                trackedEntityAttribute.setName( rs.getString( "tea_name" ) );
                trackedEntityAttribute.setDescription( rs.getString( "tea_description" ) );
                trackedEntityAttribute.setValueType( ValueType.valueOf( rs.getString( "valuetype" ) ) );

                trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );

                trackedEntityInstances.add( trackedEntityAttributeValue );
            }
            return trackedEntityInstances;
        } );
    }
}
