package org.hisp.dhis.trackedentity.hibernate;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.*;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.*;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceService.ERROR_DUPLICATE_IDENTIFIER;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceService.SEPARATOR;

/**
 * @author Abyot Asalefew Gizaw
 */
@Transactional
public class HibernateTrackedEntityInstanceStore
    extends HibernateIdentifiableObjectStore<TrackedEntityInstance>
    implements TrackedEntityInstanceStore
{
    private static final Log log = LogFactory.getLog( HibernateTrackedEntityInstanceStore.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int countTrackedEntityInstances( TrackedEntityInstanceQueryParams params )
    {
        String hql = buildTrackedEntityInstanceCountHql( params );
        Query query = getQuery( hql );

        return ((Number) query.iterate().next()).intValue();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params )
    {
        String hql = buildTrackedEntityInstanceHql( params );
        Query query = getQuery( hql );

        if ( params.isPaging() )
        {
            query.setFirstResult( params.getOffset() );
            query.setMaxResults( params.getPageSizeWithDefault() );
        }

        return query.list();
    }

    private String buildTrackedEntityInstanceCountHql( TrackedEntityInstanceQueryParams params )
    {
        return buildTrackedEntityInstanceHql( params ).replaceFirst( "select distinct tei from", "select count(distinct tei) from" );
    }

    private String buildTrackedEntityInstanceHql( TrackedEntityInstanceQueryParams params )
    {
        String hql = "select distinct tei from TrackedEntityInstance tei left join tei.trackedEntityAttributeValues";
        SqlHelper hlp = new SqlHelper( true );

        if ( params.hasTrackedEntity() )
        {
            hql += hlp.whereAnd() + "tei.trackedEntity.uid='" + params.getTrackedEntity().getUid() + "'";
        }

        if ( params.hasOrganisationUnits() )
        {
            params.handleOrganisationUnits();

            if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS ) )
            {
                String ouClause = "(";

                SqlHelper orHlp = new SqlHelper( true );

                for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
                {
                    ouClause += orHlp.or() + "tei.organisationUnit.path LIKE '" + organisationUnit.getPath() + "%'";
                }

                ouClause += ")";

                hql += hlp.whereAnd() + ouClause;
            }
            else
            {
                hql += hlp.whereAnd() + "tei.organisationUnit.uid in (" + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnits() ) ) + ")";
            }
        }

        if ( params.hasQuery() )
        {
            QueryFilter queryFilter = params.getQuery();

            String filter = queryFilter.getSqlFilter( queryFilter.getFilter() );

            hql += hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

            hql += " and teav.plainValue " + queryFilter.getSqlOperator() + filter + ")";
        }

        if ( params.hasFilters() )
        {
            for ( QueryItem queryItem : params.getFilters() )
            {
                for ( QueryFilter queryFilter : queryItem.getFilters() )
                {
                    String filter = queryFilter.getSqlFilter( StringUtils.lowerCase( queryFilter.getFilter() ) );

                    hql += hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

                    hql += " and teav.attribute.uid='" + queryItem.getItemId() + "'";

                    if ( queryItem.isNumeric() )
                    {
                        hql += " and teav.plainValue " + queryFilter.getSqlOperator() + filter + ")";
                    }
                    else
                    {
                        hql += " and lower(teav.plainValue) " + queryFilter.getSqlOperator() + filter + ")";
                    }
                }
            }
        }

        if ( params.hasProgram() )
        {
            hql += hlp.whereAnd() + "exists (from ProgramInstance pi where pi.entityInstance=tei";

            hql += " and pi.program.uid = '" + params.getProgram().getUid() + "'";

            if ( params.hasProgramStatus() )
            {
                hql += hlp.whereAnd() + "pi.status = " + params.getProgramStatus();
            }

            if ( params.hasFollowUp() )
            {
                hql += hlp.whereAnd() + "pi.followup = " + params.getFollowUp();
            }

            if ( params.hasProgramEnrollmentStartDate() )
            {
                hql += hlp.whereAnd() + "pi.enrollmentDate >= '" + getMediumDateString( params.getProgramEnrollmentStartDate() ) + "'";
            }

            if ( params.hasProgramEnrollmentEndDate() )
            {
                hql += hlp.whereAnd() + "pi.enrollmentDate < '" + getMediumDateString( params.getProgramEnrollmentEndDate() ) + "'";
            }

            if ( params.hasProgramIncidentStartDate() )
            {
                hql += hlp.whereAnd() + "pi.incidentDate >= '" + getMediumDateString( params.getProgramIncidentStartDate() ) + "'";
            }

            if ( params.hasProgramIncidentEndDate() )
            {
                hql += hlp.whereAnd() + "pi.incidentDate < '" + getMediumDateString( params.getProgramIncidentEndDate() ) + "'";
            }

            hql += ")";
        }

        return hql;
    }

    @Override
    public List<Map<String, String>> getTrackedEntityInstancesGrid( TrackedEntityInstanceQueryParams params )
    {
        SqlHelper hlp = new SqlHelper();

        // ---------------------------------------------------------------------
        // Select clause
        // ---------------------------------------------------------------------

        String sql =
            "select tei.uid as " + TRACKED_ENTITY_INSTANCE_ID + ", " +
                "tei.created as " + CREATED_ID + ", " +
                "tei.lastupdated as " + LAST_UPDATED_ID + ", " +
                "ou.uid as " + ORG_UNIT_ID + ", " +
                "ou.name as " + ORG_UNIT_NAME + ", " +
                "te.uid as " + TRACKED_ENTITY_ID + ", " +
                "tei.inactive as " + INACTIVE_ID + ", ";

        for ( QueryItem item : params.getAttributes() )
        {
            String col = statementBuilder.columnQuote( item.getItemId() );

            sql += col + ".value as " + col + ", ";
        }

        sql = removeLastComma( sql ) + " ";

        // ---------------------------------------------------------------------
        // From and where clause
        // ---------------------------------------------------------------------

        sql += getFromWhereClause( params, hlp );

        // ---------------------------------------------------------------------
        // Paging clause
        // ---------------------------------------------------------------------

        if ( params.isPaging() )
        {
            sql += "limit " + params.getPageSizeWithDefault() + " offset " + params.getOffset();
        }

        // ---------------------------------------------------------------------
        // Query
        // ---------------------------------------------------------------------

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        log.debug( "Tracked entity instance query SQL: " + sql );

        List<Map<String, String>> list = new ArrayList<>();

        while ( rowSet.next() )
        {
            final Map<String, String> map = new HashMap<>();

            map.put( TRACKED_ENTITY_INSTANCE_ID, rowSet.getString( TRACKED_ENTITY_INSTANCE_ID ) );
            map.put( CREATED_ID, rowSet.getString( CREATED_ID ) );
            map.put( LAST_UPDATED_ID, rowSet.getString( LAST_UPDATED_ID ) );
            map.put( ORG_UNIT_ID, rowSet.getString( ORG_UNIT_ID ) );
            map.put( ORG_UNIT_NAME, rowSet.getString( ORG_UNIT_NAME ) );
            map.put( TRACKED_ENTITY_ID, rowSet.getString( TRACKED_ENTITY_ID ) );
            map.put( INACTIVE_ID, rowSet.getString( INACTIVE_ID ) );

            for ( QueryItem item : params.getAttributes() )
            {
                map.put( item.getItemId(), rowSet.getString( item.getItemId() ) );
            }

            list.add( map );
        }

        return list;
    }

    @Override
    public int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params )
    {
        SqlHelper hlp = new SqlHelper();

        // ---------------------------------------------------------------------
        // Select clause
        // ---------------------------------------------------------------------

        String sql = "select count(tei.uid) as " + TRACKED_ENTITY_INSTANCE_ID + " ";

        // ---------------------------------------------------------------------
        // From and where clause
        // ---------------------------------------------------------------------

        sql += getFromWhereClause( params, hlp );

        // ---------------------------------------------------------------------
        // Query
        // ---------------------------------------------------------------------

        Integer count = jdbcTemplate.queryForObject( sql, Integer.class );

        log.debug( "Tracked entity instance count SQL: " + sql );

        return count;
    }

    /**
     * From, join and where clause. For attribute params, restriction is set in
     * inner join. For query params, restriction is set in where clause.
     */
    private String getFromWhereClause( TrackedEntityInstanceQueryParams params, SqlHelper hlp )
    {
        final String regexp = statementBuilder.getRegexpMatch();
        final String wordStart = statementBuilder.getRegexpWordStart();
        final String wordEnd = statementBuilder.getRegexpWordEnd();
        final String anyChar = "\\.*?";

        String sql = "from trackedentityinstance tei "
            + "inner join trackedentity te on tei.trackedentityid = te.trackedentityid "
            + "inner join organisationunit ou on tei.organisationunitid = ou.organisationunitid ";

        for ( QueryItem item : params.getAttributesAndFilters() )
        {
            final String col = statementBuilder.columnQuote( item.getItemId() );

            final String joinClause = item.hasFilter() ? "inner join" : "left join";

            sql += joinClause + " " + "trackedentityattributevalue as " + col + " " + "on " + col
                + ".trackedentityinstanceid = tei.trackedentityinstanceid " + "and " + col
                + ".trackedentityattributeid = " + item.getItem().getId() + " ";

            if ( !params.isOrQuery() && item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    final String encodedFilter = statementBuilder.encode( filter.getFilter(), false );

                    final String queryCol = item.isNumeric() ? (col + ".value") : "lower(" + col + ".value)";

                    sql += "and " + queryCol + " " + filter.getSqlOperator() + " "
                        + StringUtils.lowerCase( filter.getSqlFilter( encodedFilter ) ) + " ";
                }
            }
        }

        if ( params.hasTrackedEntity() )
        {
            sql += hlp.whereAnd() + " tei.trackedentityid = " + params.getTrackedEntity().getId() + " ";
        }

        params.handleOrganisationUnits();

        if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.ALL ) )
        {
            // No restriction
        }
        else if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS ) )
        {
            String ouClause = " (";

            SqlHelper orHlp = new SqlHelper( true );

            for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
            {
                ouClause += orHlp.or() + "ou.path like '" + organisationUnit.getPath() + "%'";
            }

            ouClause += ")";

            sql += hlp.whereAnd() + ouClause;
        }
        else // SELECTED (default)        
        {
            sql += hlp.whereAnd() + " tei.organisationunitid in ("
                + getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) ) + ") ";
        }

        if ( params.hasProgram() )
        {
            sql += hlp.whereAnd() + " exists (" + "select pi.trackedentityinstanceid " + "from programinstance pi ";

            if ( params.hasEventStatus() )
            {
                sql += "left join programstageinstance psi " + "on pi.programinstanceid = psi.programinstanceid ";
            }

            sql += "where pi.trackedentityinstanceid = tei.trackedentityinstanceid " + "and pi.programid = "
                + params.getProgram().getId() + " ";

            if ( params.hasProgramStatus() )
            {
                sql += "and pi.status = '" + params.getProgramStatus() + "' ";
            }

            if ( params.hasFollowUp() )
            {
                sql += "and pi.followup = " + params.getFollowUp() + " ";
            }

            if ( params.hasProgramEnrollmentStartDate() )
            {
                sql += "and pi.enrollmentdate >= '" + getMediumDateString( params.getProgramEnrollmentStartDate() ) + "' ";
            }

            if ( params.hasProgramEnrollmentEndDate() )
            {
                sql += "and pi.enrollmentdate <= '" + getMediumDateString( params.getProgramEnrollmentEndDate() ) + "' ";
            }

            if ( params.hasProgramIncidentStartDate() )
            {
                sql += "and pi.incidentdate >= '" + getMediumDateString( params.getProgramIncidentStartDate() ) + "' ";
            }

            if ( params.hasProgramIncidentEndDate() )
            {
                sql += "and pi.incidentdate <= '" + getMediumDateString( params.getProgramIncidentEndDate() ) + "' ";
            }

            if ( params.hasEventStatus() )
            {
                sql += getEventStatusWhereClause( params );
            }

            sql += ") ";
        }

        if ( params.isOrQuery() && params.hasAttributesOrFilters() )
        {
            final String start = params.getQuery().isOperator( QueryOperator.LIKE ) ? anyChar : wordStart;
            final String end = params.getQuery().isOperator( QueryOperator.LIKE ) ? anyChar : wordEnd;

            sql += hlp.whereAnd() + " (";

            List<String> queryTokens = getTokens( params.getQuery().getFilter() );

            for ( String queryToken : queryTokens )
            {
                final String query = statementBuilder.encode( queryToken, false );

                sql += "(";

                for ( QueryItem item : params.getAttributesAndFilters() )
                {
                    final String col = statementBuilder.columnQuote( item.getItemId() );

                    sql += col + ".value " + regexp + " '" + start + StringUtils.lowerCase( query ) + end + "' or ";
                }

                sql = removeLastOr( sql ) + ") and ";
            }

            sql = removeLastAnd( sql ) + ") ";
        }

        return sql;
    }

    private String getEventStatusWhereClause( TrackedEntityInstanceQueryParams params )
    {
        String start = getMediumDateString( params.getEventStartDate() );
        String end = getMediumDateString( params.getEventEndDate() );

        String sql = StringUtils.EMPTY;

        if ( params.isEventStatus( EventStatus.COMPLETED ) )
        {
            sql = "and psi.executiondate >= '" + start + "' and psi.executiondate <= '" + end + "' "
                + "and psi.status = '" + EventStatus.COMPLETED.name() + "' ";
        }
        else if ( params.isEventStatus( EventStatus.VISITED ) )
        {
            sql = "and psi.executiondate >= '" + start + "' and psi.executiondate <= '" + end + "' "
                + "and psi.status = '" + EventStatus.ACTIVE.name() + "' ";
        }
        else if ( params.isEventStatus( EventStatus.SCHEDULE ) )
        {
            sql = "and psi.executiondate is null and psi.duedate >= '" + start + "' and psi.duedate <= '" + end + "' "
                + "and psi.status is not null and date(now()) <= date(psi.duedate) ";
        }
        else if ( params.isEventStatus( EventStatus.OVERDUE ) )
        {
            sql = "and psi.executiondate is null and psi.duedate >= '" + start + "' and psi.duedate <= '" + end + "' "
                + "and psi.status is not null and date(now()) > date(psi.duedate) ";
        }
        else if ( params.isEventStatus( EventStatus.SKIPPED ) )
        {
            sql = "and psi.duedate >= '" + start + "' and psi.duedate <= '" + end + "' " + "and psi.status = '"
                + EventStatus.SKIPPED.name() + "' ";
        }

        return sql;
    }

    @Override
    public String validate( TrackedEntityInstance instance, TrackedEntityAttributeValue attributeValue, Program program )
    {
        TrackedEntityAttribute attribute = attributeValue.getAttribute();

        try
        {
            if ( attribute.isUnique() )
            {
                Criteria criteria = getCriteria();
                criteria.add( Restrictions.ne( "id", instance.getId() ) );
                criteria.createAlias( "trackedEntityAttributeValues", "attributeValue" );
                criteria.createAlias( "attributeValue.attribute", "attribute" );
                criteria.add( Restrictions.eq( "attributeValue.value", attributeValue.getValue() ) );
                criteria.add( Restrictions.eq( "attributeValue.attribute", attribute ) );

                if ( attribute.getId() != 0 )
                {
                    criteria.add( Restrictions.ne( "id", attribute.getId() ) );
                }

                if ( attribute.getOrgunitScope() )
                {
                    criteria.add( Restrictions.eq( "organisationUnit", instance.getOrganisationUnit() ) );
                }

                if ( program != null && attribute.getProgramScope() )
                {
                    criteria.createAlias( "programInstances", "programInstance" );
                    criteria.add( Restrictions.eq( "programInstance.program", program ) );
                }

                Number rs = (Number) criteria.setProjection(
                    Projections.projectionList().add( Projections.property( "attribute.id" ) ) ).uniqueResult();

                if ( rs != null && rs.intValue() > 0 )
                {
                    return ERROR_DUPLICATE_IDENTIFIER + SEPARATOR + rs.intValue();
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean exists( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from trackedentityinstance where uid=?", Integer.class, uid );
        return result != null && result > 0;
    }
}
