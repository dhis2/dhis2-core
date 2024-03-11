package org.hisp.dhis.trackedentity.hibernate;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.*;
import static org.hisp.dhis.system.util.DateUtils.*;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.*;

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
    private OrganisationUnitStore organisationUnitStore;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    public void setOrganisationUnitStore( OrganisationUnitStore organisationUnitStore )
    {
        this.organisationUnitStore = organisationUnitStore;
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

        //If it is a sync job running a query, I need to adjust an HQL a bit, because I am adding 2 joins and don't want duplicates in results
        if ( params.isSynchronizationQuery() )
        {
            hql = hql.replaceFirst( "select tei from", "select distinct tei from" );
        }

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
        return buildTrackedEntityInstanceHql( params )
            .replaceFirst( "select tei from", "select count(distinct tei) from" )
            .replaceFirst( "left join fetch tei.programInstances", "left join tei.programInstances" )
            .replaceFirst( "inner join fetch tei.programOwners", "inner join tei.programOwners" )
            .replaceFirst( "order by case when pi.status = 'ACTIVE' then 1 when pi.status = 'COMPLETED' then 2 else 3 end asc, tei.lastUpdated desc ", "" )
            .replaceFirst( "order by tei.lastUpdated desc ", "" );
    }

    private String buildTrackedEntityInstanceHql( TrackedEntityInstanceQueryParams params )
    {
        SqlHelper hlp = new SqlHelper( true );

        String hql = "select tei from TrackedEntityInstance tei ";

        //Used for switing between registration org unit or ownership org unit. Default source is registration ou.
        String teiOuSource = "tei.organisationUnit";

        if ( params.hasProgram() )
        {
            hql += "left join fetch tei.programInstances as pi ";

            //Joining program owners and using that as tei ou source
            hql += "inner join fetch tei.programOwners as po ";
            teiOuSource = "po.organisationUnit";

            hql += hlp.whereAnd() + " po.program.uid = '" + params.getProgram().getUid() + "'";

            hql += hlp.whereAnd() + " pi.program.uid = '" + params.getProgram().getUid() + "'";

            if ( params.hasProgramStatus() )
            {
                hql += hlp.whereAnd() + "pi.status = '" + params.getProgramStatus() + "'";
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

            if ( !params.isIncludeDeleted() )
            {
                hql += hlp.whereAnd() + "pi.deleted is false ";
            }

        }

        //If it is a sync job that runs the query, fetch only TEAVs that are supposed to be synchronized
        if ( params.isSynchronizationQuery() )
        {

            hql += "left join tei.trackedEntityAttributeValues teav1 " +
                "left join teav1.attribute as attr";

            hql += hlp.whereAnd() + " attr.skipSynchronization = false";
        }

        if ( params.hasTrackedEntityType() )
        {
            hql += hlp.whereAnd() + "tei.trackedEntityType.uid='" + params.getTrackedEntityType().getUid() + "'";
        }

        if ( params.hasLastUpdatedDuration() )
        {
            hql += hlp.whereAnd() + "tei.lastUpdated >= '" +
                getLongGmtDateString( DateUtils.nowMinusDuration( params.getLastUpdatedDuration() ) ) + "'";
        }
        else
        {
            if ( params.hasLastUpdatedStartDate() )
            {
                hql += hlp.whereAnd() + "tei.lastUpdated >= '" +
                    getMediumDateString( params.getLastUpdatedStartDate() ) + "'";
            }

            if ( params.hasLastUpdatedEndDate() )
            {
                hql += hlp.whereAnd() + "tei.lastUpdated < '" +
                    getMediumDateString( getDateAfterAddition( params.getLastUpdatedEndDate(), 1 ) ) + "'";
            }
        }

        if ( params.isSynchronizationQuery() )
        {
            hql += hlp.whereAnd() + "tei.lastUpdated > tei.lastSynchronized";
        }

        //Going for comparing milliseconds instead of always creating new Date( 0 );
        if ( params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0 )
        {
            String skipChangedBefore = DateUtils.getLongDateString( params.getSkipChangedBefore() );
            hql += hlp.whereAnd() + "tei.lastUpdated >= '" + skipChangedBefore + "'";
        }

        params.handleOrganisationUnits();

        if ( params.hasOrganisationUnits() )
        {

            if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS ) )
            {
                String ouClause = "(";

                SqlHelper orHlp = new SqlHelper( true );

                for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
                {
                    ouClause += orHlp.or() + teiOuSource + ".path LIKE '" + organisationUnit.getPath() + "%'";
                }

                ouClause += ")";

                hql += hlp.whereAnd() + ouClause;
            }
            else
            {
                hql += hlp.whereAnd() + teiOuSource + ".uid in (" + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnits() ) ) + ")";
            }
        }

        if ( params.hasQuery() )
        {
            QueryFilter queryFilter = params.getQuery();

            String encodedFilter = queryFilter.getSqlFilter( statementBuilder.encode( queryFilter.getFilter(), false ) );

            hql += hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

            hql += " and teav.plainValue " + queryFilter.getSqlOperator() + encodedFilter + ")";
        }

        if ( params.hasFilters() )
        {
            for ( QueryItem queryItem : params.getFilters() )
            {
                for ( QueryFilter queryFilter : queryItem.getFilters() )
                {
                    String encodedFilter = queryFilter.getSqlFilter( statementBuilder.encode( StringUtils.lowerCase( queryFilter.getFilter() ), false ) );

                    hql += hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

                    hql += " and teav.attribute.uid='" + queryItem.getItemId() + "'";

                    if ( queryItem.isNumeric() )
                    {
                        hql += " and teav.plainValue " + queryFilter.getSqlOperator() + encodedFilter + ")";
                    }
                    else
                    {
                        hql += " and lower(teav.plainValue) " + queryFilter.getSqlOperator() + encodedFilter + ")";
                    }
                }
            }
        }

        if ( !params.isIncludeDeleted() )
        {
            hql += hlp.whereAnd() + " tei.deleted is false ";
        }

        if ( params.hasProgram() )
        {
            hql += " order by case when pi.status = 'ACTIVE' then 1 when pi.status = 'COMPLETED' then 2 else 3 end asc, tei.lastUpdated desc ";
        }
        else
        {
            hql += " order by tei.lastUpdated desc ";
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
                (params.hasProgram() ? "en.status as enrollment_status, " : "") +
                (params.isIncludeDeleted() ? "tei.deleted as " + DELETED + ", " : "") +
                "tei.inactive as " + INACTIVE_ID + ", ";

        for ( QueryItem item : params.getAttributes() )
        {
            String col = statementBuilder.columnQuote( item.getItemId() );

            sql += item.isNumeric() ? "CAST( " + col + ".value AS NUMERIC ) as " : col + ".value as ";

            sql += col + ", ";
        }

        sql = removeLastComma( sql ) + " ";

        // ---------------------------------------------------------------------
        // From and where clause
        // ---------------------------------------------------------------------

        sql += getFromWhereClause( params, hlp );

        // ---------------------------------------------------------------------
        // Order clause
        // ---------------------------------------------------------------------

        sql += getOrderClause( params );

        // ---------------------------------------------------------------------
        // Paging clause
        // ---------------------------------------------------------------------

        if ( params.isPaging() )
        {
            sql += " limit " + params.getPageSizeWithDefault() + " offset " + params.getOffset();
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

            if ( params.isIncludeDeleted() )
            {
                map.put( DELETED, rowSet.getString( DELETED ) );
            }

            for ( QueryItem item : params.getAttributes() )
            {
                map.put( item.getItemId(),
                    isOrgUnit( item ) ? getOrgUnitNameByUid( rowSet.getString( item.getItemId() ) )
                        : rowSet.getString( item.getItemId() ) );
            }

            list.add( map );
        }

        return list;
    }

    @Override
    public int getTrackedEntityInstanceCountForGrid( TrackedEntityInstanceQueryParams params )
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
            + "inner join trackedentitytype te on tei.trackedentitytypeid = te.trackedentitytypeid ";

        String teiOuSource = "tei.organisationunitid";

        if ( params.hasProgram() )
        {
            //Using program owner OU instead of registration OU.
            sql += "inner join (select trackedentityinstanceid, organisationunitid from trackedentityprogramowner where programid = ";
            sql += params.getProgram().getId() + ") as tepo ON tei.trackedentityinstanceid = tepo.trackedentityinstanceid ";
            teiOuSource = "tepo.organisationunitid";

            sql += "inner join ("
                + "select trackedentityinstanceid, min(case when status='ACTIVE' then 0 when status='COMPLETED' then 1 else 2 end) as status "
                + "from programinstance pi ";

            if ( params.hasEventStatus() )
            {
                sql += " inner join (select programinstanceid from programstageinstance psi ";

                sql += getEventStatusWhereClause( params );

                sql += ") as psi on pi.programinstanceid = psi.programinstanceid ";
            }

            sql += " where pi.programid= " + params.getProgram().getId() + " ";

            if ( params.hasProgramStatus() )
            {
                sql += "and status = '" + params.getProgramStatus() + "' ";
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

            if ( !params.isIncludeDeleted() )
            {
                sql += " and pi.deleted is false ";
            }

            sql += " group by trackedentityinstanceid ) as en on tei.trackedentityinstanceid = en.trackedentityinstanceid ";
        }

        sql += "inner join organisationunit ou on " + teiOuSource + " = ou.organisationunitid ";

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

        if ( !params.hasTrackedEntityType() )
        {
            sql += hlp.whereAnd() + " tei.trackedentitytypeid in (" + params.getTrackedEntityTypes().stream()
                .filter( Objects::nonNull )
                .map( TrackedEntityType::getId )
                .map( String::valueOf )
                .collect( Collectors.joining(", ") ) + ") ";
        }
        else
        {
            sql += hlp.whereAnd() + " tei.trackedentitytypeid = " + params.getTrackedEntityType().getId() + " ";
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
            sql += hlp.whereAnd() + " " + teiOuSource + " in ("
                + getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) ) + ") ";
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

        if ( !params.isIncludeDeleted() )
        {
            sql += hlp.whereAnd() + " tei.deleted is false ";
        }

        return sql;
    }

    private String getOrderClause( TrackedEntityInstanceQueryParams params )
    {
        List<String> cols = getStaticGridColumns();

        if ( params.getOrders() != null && params.getAttributes() != null && !params.getAttributes().isEmpty()
            && cols != null && !cols.isEmpty() )
        {
            ArrayList<String> orderFields = new ArrayList<String>();

            for ( String order : params.getOrders() )
            {
                String[] prop = order.split( ":" );

                if ( prop.length == 2 && (prop[1].equals( "desc" ) || prop[1].equals( "asc" )) )
                {
                    if ( cols.contains( prop[0] ) )
                    {
                        orderFields.add( prop[0] + " " + prop[1] );
                    }
                    else
                    {
                        Iterator<QueryItem> itermIterator = params.getAttributes().iterator();

                        while ( itermIterator.hasNext() )
                        {
                            QueryItem item = itermIterator.next();

                            if ( prop[0].equals( item.getItemId() ) )
                            {
                                orderFields.add( statementBuilder.columnQuote( prop[0] ) + " " + prop[1] );
                                break;
                            }
                        }
                    }
                }

            }

            if ( !orderFields.isEmpty() )
            {
                return "order by " + StringUtils.join( orderFields, ',' );
            }
        }

        if ( params.hasProgram() )
        {
            return "order by en.status asc, lastUpdated desc ";
        }

        return "order by lastUpdated desc ";
    }

    private List<String> getStaticGridColumns()
    {

        return Arrays.asList( TRACKED_ENTITY_INSTANCE_ID, CREATED_ID, LAST_UPDATED_ID, ORG_UNIT_ID, ORG_UNIT_NAME, TRACKED_ENTITY_ID, INACTIVE_ID );
    }

    private String getEventStatusWhereClause( TrackedEntityInstanceQueryParams params )
    {
        String start = getMediumDateString( params.getEventStartDate() );
        String end = getMediumDateString( params.getEventEndDate() );

        String sql = " where ";

        if ( params.isEventStatus( EventStatus.COMPLETED ) )
        {
            sql += " psi.executiondate >= '" + start + "' and psi.executiondate <= '" + end + "' "
                + "and psi.status = '" + EventStatus.COMPLETED.name() + "' and ";
        }
        else if ( params.isEventStatus( EventStatus.VISITED ) )
        {
            sql += " psi.executiondate >= '" + start + "' and psi.executiondate <= '" + end + "' "
                + "and psi.status = '" + EventStatus.ACTIVE.name() + "' and ";
        }
        else if ( params.isEventStatus( EventStatus.SCHEDULE ) )
        {
            sql += " psi.executiondate is null and psi.duedate >= '" + start + "' and psi.duedate <= '" + end + "' "
                + "and psi.status is not null and date(now()) <= date(psi.duedate) and ";
        }
        else if ( params.isEventStatus( EventStatus.OVERDUE ) )
        {
            sql += " psi.executiondate is null and psi.duedate >= '" + start + "' and psi.duedate <= '" + end + "' "
                + "and psi.status is not null and date(now()) > date(psi.duedate) and ";
        }
        else if ( params.isEventStatus( EventStatus.SKIPPED ) )
        {
            sql += " psi.duedate >= '" + start + "' and psi.duedate <= '" + end + "' " + "and psi.status = '"
                + EventStatus.SKIPPED.name() + "' and ";
        }

        sql += " psi.deleted is false ";

        return sql;
    }

    @Override
    public boolean exists( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from trackedentityinstance where uid=? and deleted is false", Integer.class, uid );
        return result != null && result > 0;
    }

    @Override
    public boolean existsIncludingDeleted( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from trackedentityinstance where uid=?", Integer.class, uid );
        return result != null && result > 0;
    }

    @Override
    public List<String> getUidsIncludingDeleted( List<String> uids )
    {
        String hql = "select te.uid from TrackedEntityInstance as te where te.uid in (:uids)";
        List<String> resultUids = new ArrayList<>();
        List<List<String>> uidsPartitions = Lists.partition( Lists.newArrayList( uids ), 20000 );

        for ( List<String> uidsPartition : uidsPartitions )
        {
            if ( !uidsPartition.isEmpty() )
            {
                resultUids.addAll( getSession().createQuery( hql, String.class ).setParameter( "uids", uidsPartition ).list() );
            }
        }

        return resultUids;
    }

    @Override
    public void updateTrackedEntityInstancesSyncTimestamp( List<String> trackedEntityInstanceUIDs, Date lastSynchronized )
    {
        String hql = "update TrackedEntityInstance set lastSynchronized = :lastSynchronized WHERE uid in :trackedEntityInstances";
        Query query = getQuery( hql );
        query.setParameter( "lastSynchronized", lastSynchronized );
        query.setParameter( "trackedEntityInstances", trackedEntityInstanceUIDs );

        query.executeUpdate();
    }

    @Override
    public List<TrackedEntityInstance> getTrackedEntityInstancesByUid( List<String> uids, User user )
    {
        return getSharingCriteria( user )
            .add( Restrictions.in( "uid", uids ) )
            .list();
    }

    @Override
    protected void preProcessPredicates( CriteriaBuilder builder, List<Function<Root<TrackedEntityInstance>, Predicate>> predicates )
    {
        predicates.add( root -> builder.equal( root.get( "deleted" ), false ) );
    }


    @Override
    protected TrackedEntityInstance postProcessObject( TrackedEntityInstance trackedEntityInstance )
    {
        return (trackedEntityInstance == null || trackedEntityInstance.isDeleted()) ? null : trackedEntityInstance;
    }

    private boolean isOrgUnit( QueryItem item )
    {
        return item.getValueType().isOrganisationUnit();
    }

    private String getOrgUnitNameByUid( String uid )
    {
        if ( uid != null )
        {
            return Optional.ofNullable( organisationUnitStore.getByUid( uid ) )
                .orElseGet( () -> new OrganisationUnit( "" ) ).getName();
        }

        return StringUtils.EMPTY;
    }
}
