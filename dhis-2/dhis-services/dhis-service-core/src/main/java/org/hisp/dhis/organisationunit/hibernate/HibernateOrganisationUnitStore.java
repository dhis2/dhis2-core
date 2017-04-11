package org.hisp.dhis.organisationunit.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hibernate.Query;
import org.hibernate.Session;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitHierarchy;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.system.objectmapper.OrganisationUnitRelationshipRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kristian Nordal
 */
public class HibernateOrganisationUnitStore
    extends HibernateIdentifiableObjectStore<OrganisationUnit>
    implements OrganisationUnitStore
{
    @Autowired
    private DbmsManager dbmsManager;

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    @Override
    public List<OrganisationUnit> getAllOrganisationUnitsByLastUpdated( Date lastUpdated )
    {
        return getAllGeLastUpdated( lastUpdated );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<OrganisationUnit> getRootOrganisationUnits()
    {
        return getQuery( "from OrganisationUnit o where o.parent is null" ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<OrganisationUnit> getOrganisationUnitsWithoutGroups()
    {
        return getQuery( "from OrganisationUnit o where size(o.groups) = 0" ).list();
    }

    @Override
    public Long getOrganisationUnitHierarchyMemberCount( OrganisationUnit parent, Object member, String collectionName )
    {
        final String hql = 
            "select count(*) from OrganisationUnit o " +
            "where o.path like :path " +
            "and :object in elements(o." + collectionName + ")";
        
        return (Long) getQuery( hql )
            .setString( "path", parent.getPath() + "%" )
            .setEntity( "object", member )
            .uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<OrganisationUnit> getOrganisationUnits( OrganisationUnitQueryParams params )
    {
        SqlHelper hlp = new SqlHelper();

        String hql = "select distinct o from OrganisationUnit o ";

        if ( params.hasGroups() )
        {
            hql += "join o.groups og ";
        }
        
        if ( params.hasQuery() )
        {
            hql += hlp.whereAnd() + " (lower(o.name) like :queryLower or o.code = :query or o.uid = :query) ";
        }

        if ( params.hasParents() )
        {
            hql += hlp.whereAnd() + " (";

            for ( OrganisationUnit parent : params.getParents() )
            {
                hql += "o.path like :" + parent.getUid() + " or ";
            }

            hql = TextUtils.removeLastOr( hql ) + ") ";
        }

        if ( params.hasGroups() )
        {
            hql += hlp.whereAnd() + " og.id in (:groupIds) ";
        }

        if ( params.hasLevels() )
        {
            hql += hlp.whereAnd() + " o.hierarchyLevel in (:levels) ";
        }

        if ( params.getMaxLevels() != null )
        {
            hql += hlp.whereAnd() + " o.hierarchyLevel <= :maxLevels ";
        }

        hql += "order by o.name";

        Query query = getQuery( hql );

        if ( params.hasQuery() )
        {
            query.setString( "queryLower", "%" + params.getQuery().toLowerCase() + "%" );
            query.setString( "query", params.getQuery() );
        }

        if ( params.hasParents() )
        {
            for ( OrganisationUnit parent : params.getParents() )
            {
                query.setString( parent.getUid(), parent.getPath() + "%" );
            }
        }

        if ( params.hasGroups() )
        {
            query.setParameterList( "groupIds", IdentifiableObjectUtils.getIdentifiers( params.getGroups() ) );
        }
        
        if ( params.hasLevels() )
        {
            query.setParameterList( "levels", params.getLevels() );
        }

        if ( params.getMaxLevels() != null )
        {
            query.setInteger( "maxLevels", params.getMaxLevels() );
        }

        if ( params.getFirst() != null )
        {
            query.setFirstResult( params.getFirst() );
        }

        if ( params.getMax() != null )
        {
            query.setMaxResults( params.getMax() ).list();
        }

        return query.list();
    }

    @Override
    public Map<String, Set<String>> getOrganisationUnitDataSetAssocationMap()
    {
        final String sql = "select ds.uid as ds_uid, ou.uid as ou_uid from datasetsource d " +
            "left join organisationunit ou on ou.organisationunitid=d.sourceid " +
            "left join dataset ds on ds.datasetid=d.datasetid";

        final SetMap<String, String> map = new SetMap<>();

        jdbcTemplate.query( sql, new RowCallbackHandler()
        {
            @Override
            public void processRow( ResultSet rs ) throws SQLException
            {
                String dataSetId = rs.getString( "ds_uid" );
                String organisationUnitId = rs.getString( "ou_uid" );
                map.putValue( organisationUnitId, dataSetId );
            }
        } );

        return map;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<OrganisationUnit> getWithinCoordinateArea( double[] box )
    {
        final String sql = "from OrganisationUnit o " +
            "where o.featureType='Point' " +
            "and o.coordinates is not null " +
            "and cast( substring(o.coordinates, 2, locate(',', o.coordinates) - 2) AS big_decimal ) >= " + box[3] + " " +
            "and cast( substring(o.coordinates, 2, locate(',', o.coordinates) - 2) AS big_decimal ) <= " + box[1] + " " +
            "and cast( substring(coordinates, locate(',', o.coordinates) + 1, locate(']', o.coordinates) - locate(',', o.coordinates) - 1 ) AS big_decimal ) >= " + box[2] + " " +
            "and cast( substring(coordinates, locate(',', o.coordinates) + 1, locate(']', o.coordinates) - locate(',', o.coordinates) - 1 ) AS big_decimal ) <= " + box[0];
        
        return getQuery( sql ).list();
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitHierarchy
    // -------------------------------------------------------------------------

    @Override
    public OrganisationUnitHierarchy getOrganisationUnitHierarchy()
    {
        final String sql = "select organisationunitid, parentid from organisationunit";

        return new OrganisationUnitHierarchy( jdbcTemplate.query( sql, new OrganisationUnitRelationshipRowMapper() ) );
    }

    @Override
    public void updateOrganisationUnitParent( int organisationUnitId, int parentId )
    {
        Timestamp now = new Timestamp( new Date().getTime() );

        final String sql = "update organisationunit " + "set parentid=" + parentId + ", lastupdated='"
            + now + "' " + "where organisationunitid=" + organisationUnitId;

        jdbcTemplate.execute( sql );
    }

    @Override
    public void updatePaths()
    {
        getQuery( "from OrganisationUnit ou where ou.path is null or ou.hierarchyLevel is null" ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void forceUpdatePaths()
    {
        List<OrganisationUnit> organisationUnits = new ArrayList<>( getQuery( "from OrganisationUnit" ).list() );
        updatePaths( organisationUnits );
    }

    @Override
    public int getMaxLevel()
    {
        String hql = "select max(ou.hierarchyLevel) from OrganisationUnit ou";

        Integer maxLength = (Integer) getQuery( hql ).uniqueResult();

        return maxLength != null ? maxLength.intValue() : 0;
    }

    private void updatePaths( List<OrganisationUnit> organisationUnits )
    {
        Session session = getSession();
        int counter = 0;

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            session.update( organisationUnit );

            if ( (counter % 400) == 0 )
            {
                dbmsManager.clearSession();
            }

            counter++;
        }
    }
}
