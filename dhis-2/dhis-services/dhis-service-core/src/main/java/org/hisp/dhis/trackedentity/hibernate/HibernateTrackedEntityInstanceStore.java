package org.hisp.dhis.trackedentity.hibernate;

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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.hisp.dhis.common.*;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getTokens;
import static org.hisp.dhis.commons.util.TextUtils.removeLastAnd;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.CREATED_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.DELETED;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.INACTIVE_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.LAST_UPDATED_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.ORG_UNIT_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.ORG_UNIT_NAME;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.TRACKED_ENTITY_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.TRACKED_ENTITY_INSTANCE_ID;
import static org.hisp.dhis.util.DateUtils.getDateAfterAddition;
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

/**
 * @author Abyot Asalefew Gizaw
 */
@Slf4j
@Repository( "org.hisp.dhis.trackedentity.TrackedEntityInstanceStore" )
public class HibernateTrackedEntityInstanceStore
    extends SoftDeleteHibernateObjectStore<TrackedEntityInstance>
    implements TrackedEntityInstanceStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final OrganisationUnitStore organisationUnitStore;

    private final StatementBuilder statementBuilder;

    public HibernateTrackedEntityInstanceStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService,
        DeletedObjectService deletedObjectService, AclService aclService,
        OrganisationUnitStore organisationUnitStore, StatementBuilder statementBuilder )
    {
        super( sessionFactory, jdbcTemplate, publisher, TrackedEntityInstance.class, currentUserService,
            deletedObjectService,
            aclService, false );

        checkNotNull( statementBuilder );
        checkNotNull( organisationUnitStore );

        this.statementBuilder = statementBuilder;
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
    public List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params )
    {
        String hql = buildTrackedEntityInstanceHql( params );

        //If it is a sync job running a query, I need to adjust an HQL a bit, because I am adding 2 joins and don't want duplicates in results
        if ( params.isSynchronizationQuery() )
        {
            hql = hql.replaceFirst( "select tei from", "select distinct tei from" );
        }

        Query<TrackedEntityInstance> query = getQuery( hql );

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
            .replaceFirst( "inner join fetch tei.programInstances", "inner join tei.programInstances" )
            .replaceFirst( "inner join fetch pi.programStageInstances", "inner join pi.programStageInstances" )
            .replaceFirst( "inner join fetch psi.assignedUser", "inner join psi.assignedUser" )
            .replaceFirst( "inner join fetch tei.programOwners", "inner join tei.programOwners" )
            .replaceFirst(
                "order by case when pi.status = 'ACTIVE' then 1 when pi.status = 'COMPLETED' then 2 else 3 end asc, tei.lastUpdated desc ",
                "" )
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
            hql += "inner join fetch tei.programInstances as pi ";

            //Joining program owners and using that as tei ou source
            hql += "inner join fetch tei.programOwners as po ";
            teiOuSource = "po.organisationUnit";

            if ( params.hasFilterForEvents() )
            {
                hql += " inner join fetch pi.programStageInstances psi ";

                if ( params.hasAssignedUsers() )
                {
                    hql += " inner join fetch psi.assignedUser au ";
                }

                hql += hlp.whereAnd() + getEventWhereClauseHql( params );

            }

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
                hql += hlp.whereAnd() + "pi.enrollmentDate >= '" + getMediumDateString(
                    params.getProgramEnrollmentStartDate() ) + "'";
            }

            if ( params.hasProgramEnrollmentEndDate() )
            {
                hql += hlp.whereAnd() + "pi.enrollmentDate < '" + getMediumDateString(
                    params.getProgramEnrollmentEndDate() ) + "'";
            }

            if ( params.hasProgramIncidentStartDate() )
            {
                hql += hlp.whereAnd() + "pi.incidentDate >= '" + getMediumDateString(
                    params.getProgramIncidentStartDate() ) + "'";
            }

            if ( params.hasProgramIncidentEndDate() )
            {
                hql +=
                    hlp.whereAnd() + "pi.incidentDate < '" + getMediumDateString( params.getProgramIncidentEndDate() )
                        + "'";
            }

            if ( !params.isIncludeDeleted() )
            {
                hql += hlp.whereAnd() + "pi.deleted is false ";
            }

        }

        // If sync job, fetch only TEAVs that are supposed to be synchronized

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

        if ( params.hasTrackedEntityInstances() )
        {
            hql += hlp.whereAnd() + " tei.uid in (" + getQuotedCommaDelimitedString(
                params.getTrackedEntityInstanceUids() ) + ")";
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

        // Comparing milliseconds instead of always creating new Date( 0 )

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
                hql += hlp.whereAnd() + teiOuSource + ".uid in (" + getQuotedCommaDelimitedString(
                    getUids( params.getOrganisationUnits() ) ) + ")";
            }
        }

        if ( params.hasQuery() )
        {
            QueryFilter queryFilter = params.getQuery();

            String encodedFilter = queryFilter
                .getSqlFilter( statementBuilder.encode( queryFilter.getFilter(), false ) );

            hql += hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

            hql += " and teav.plainValue " + queryFilter.getSqlOperator() + encodedFilter + ")";
        }

        if ( params.hasFilters() )
        {
            for ( QueryItem queryItem : params.getFilters() )
            {
                for ( QueryFilter queryFilter : queryItem.getFilters() )
                {
                    String encodedFilter = queryFilter.getSqlFilter(
                        statementBuilder.encode( StringUtils.lowerCase( queryFilter.getFilter() ), false ) );

                    hql +=
                        hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

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
    public List<Map<String, String>> getTrackedEntityInstancesGridV2( TrackedEntityInstanceQueryParams params )
    {
        String sql = getQuery( params );
        log.info( "Tracked entity instance query SQL: " + sql );
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        if ( params.getMaxTeiLimit() > 0 && rowSet.last() )
        {
            if ( rowSet.getRow() > params.getMaxTeiLimit() )
            {
                throw new IllegalQueryException( "maxteicountreached" );
            }
            rowSet.beforeFirst();
        }

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

            HashMap<String, String> attributeValues = new HashMap<>();

            String[] pairs = rowSet.getString( "tea_values" ).split( ";" );

            for ( String pair : pairs )
            {
                String[] teav = pair.split( ":" );

                if ( teav.length == 2 )
                {
                    attributeValues.put( teav[0], teav[1] );
                }
            }

            for ( QueryItem item : params.getAttributes() )
            {
                map.put( item.getItemId(),
                    isOrgUnit( item ) ? getOrgUnitNameByUid( attributeValues.get( item.getItemId() ) )
                        : attributeValues.get( item.getItemId() ) );
            }

            list.add( map );
        }

        return list;
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

        log.info( "Tracked entity instance query SQL: " + sql );

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

    private String getQuery( TrackedEntityInstanceQueryParams params )
    {
        return new StringBuilder()
            .append( getQuerySelect( params ) )
            .append( "FROM " )
            .append( getFromSubQuery( params ) )
            .append( getQueryRelatedTables( params ) )
            .append( getQueryGroupBy( params ) )
            .toString();
    }

    private String getQuerySelect( TrackedEntityInstanceQueryParams params )
    {
        return new StringBuilder()
            .append( "SELECT TEI.uid AS " + TRACKED_ENTITY_INSTANCE_ID + ", " )
            .append( "TEI.created AS " + CREATED_ID + ", " )
            .append( "TEI.lastupdated AS " + LAST_UPDATED_ID + ", " )
            .append( "TEI.ou AS " + ORG_UNIT_ID + ", " )
            .append( "TEI.ouname AS " + ORG_UNIT_NAME + ", " )
            .append( "TET.uid AS " + TRACKED_ENTITY_ID + ", " )
            .append( "TEI.inactive AS " + INACTIVE_ID )
            .append( (params.isIncludeDeleted() ? ", TEI.deleted AS " + DELETED : "") )
            .append( (params.hasAttributes() ? ", string_agg(TEA.uid || ':' || TEAV.value, ';') AS tea_values" : "") )
            .append( " " )
            .toString();
    }

    private String getFromSubQuery( TrackedEntityInstanceQueryParams params )
    {
        SqlHelper whereAnd = new SqlHelper( true );
        return new StringBuilder()
            .append( "(" )
            .append( getFromSubQuerySelect( params ) )
            .append( "FROM trackedentityinstance TEI " )

            // INNER JOIN on constraints
            .append( getFromSubQueryJoinProgramOwner( params ) )
            .append( getFromSubQueryJoinOrgUnit( params ) )
            .append( getFromSubQueryJoinOrderByAttributes( params ) )

            // WHERE
            .append( getFromSubQueryTrackedEntityConditions( whereAnd, params ) )
            .append( getFromSubQueryProgramInstanceConditions( whereAnd, params ) )
            .append( getFromSubQueryAttributeConditions( whereAnd, params ) )

            // SORT
            .append( getFromSubQueryOrderBy( params ) )

            // LIMIT, OFFSET
            .append( getFromSubQueryLimitAndOffset( params ) )
            .append( ") TEI " )
            .toString();
    }

    private String getFromSubQuerySelect( TrackedEntityInstanceQueryParams params )
    {
        return new StringBuilder()
            .append( "SELECT " )
            .append( "TEI.trackedentityinstanceid, " )
            .append( "TEI.uid, " )
            .append( "TEI.created, " )
            .append( "TEI.lastupdated, " )
            .append( "TEI.inactive, " )
            .append( "TEI.trackedentitytypeid, " )
            .append( "TEI.deleted, " )
            .append( "OU.uid as ou, " )
            .append( "OU.name as ouname " )
            .append( getFromSubQueryOrderAttributes( params ) )
            .toString();
    }

    private String getFromSubQueryOrderAttributes( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder orderAttributes = new StringBuilder();

        for ( QueryItem orderAttribute : getOrderAttributes( params ) )
        {
            orderAttributes
                .append( ", " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                .append( ".value AS " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) );
        }

        return orderAttributes.toString();
    }

    private String getFromSubQueryTrackedEntityConditions( SqlHelper whereAnd, TrackedEntityInstanceQueryParams params )
    {
        StringBuilder trackedEntity = new StringBuilder();

        if ( params.hasTrackedEntityType() )
        {
            trackedEntity
                .append( whereAnd.whereAnd() )
                .append( "TEI.trackedentitytypeid = " )
                .append( params.getTrackedEntityType().getId() )
                .append( " " );
        }
        else
        {
            trackedEntity
                .append( whereAnd.whereAnd() )
                .append( "TEI.trackedentitytypeid IN (" )
                .append( getCommaDelimitedString( getIdentifiers( params.getTrackedEntityTypes() ) ) )
                .append( ") " );
        }

        if ( params.hasTrackedEntityInstances() )
        {
            trackedEntity
                .append( whereAnd.whereAnd() )
                .append( "TEI.uid IN (" )
                .append( getQuotedCommaDelimitedString( params.getTrackedEntityInstanceUids() ) )
                .append( ") " );
        }

        if ( !params.isIncludeDeleted() )
        {
            trackedEntity
                .append( whereAnd.whereAnd() )
                .append( "TEI.deleted IS FALSE " );
        }

        return trackedEntity.toString();
    }

    private String getFromSubQueryAttributeConditions( SqlHelper whereAnd, TrackedEntityInstanceQueryParams params )
    {
        StringBuilder attributes = new StringBuilder();

        List<QueryItem> filterItems = params.getAttributesAndFilters().stream()
            .filter( QueryItem::hasFilter )
            .collect( Collectors.toList() );

        QueryItem uniqueItem = filterItems.stream()
            .filter( QueryItem::isUnique )
            .findAny()
            .orElse( null );

        // If we have unique values, and we dont have a OR-query, we can optimise the query by just searching for 1
        // unique item.
        if ( uniqueItem != null )
        {
            filterItems = Lists.newArrayList( uniqueItem );
        }

        if ( !filterItems.isEmpty() || params.isOrQuery() )
        {

            attributes
                .append( whereAnd.whereAnd() )
                .append( "EXISTS (" )
                .append( "SELECT trackedentityinstanceid " )
                .append( "FROM trackedentityattributevalue" );

            if ( !params.isOrQuery() )
            {
                SqlHelper whereHlp = new SqlHelper( true );

                for ( QueryItem queryItem : filterItems )
                {
                    attributes
                        .append( whereHlp.whereAnd() )
                        .append( "(trackedentityattributeid = " )
                        .append( queryItem.getItem().getId() )
                        .append( " " );

                    for ( QueryFilter filter : queryItem.getFilters() )
                    {
                        attributes
                            .append( "AND lower(value) " )
                            .append( filter.getSqlOperator() )
                            .append( " " )
                            .append( StringUtils.lowerCase( filter.getSqlFilter( filter.getFilter() ) ) );
                    }

                    attributes.append( ")" );
                }
            }
            else
            {
                final String regexp = statementBuilder.getRegexpMatch();
                final String wordStart = statementBuilder.getRegexpWordStart();
                final String wordEnd = statementBuilder.getRegexpWordEnd();
                final String anyChar = "\\.*?";
                final String start = params.getQuery().isOperator( QueryOperator.LIKE ) ? anyChar : wordStart;
                final String end = params.getQuery().isOperator( QueryOperator.LIKE ) ? anyChar : wordEnd;
                SqlHelper whereHlp = new SqlHelper( true );

                for ( QueryItem item : params.getAttributesAndFilters() )
                {
                    SqlHelper orHlp = new SqlHelper( true );

                    attributes
                        .append( whereHlp.whereOr() )
                        .append( "(trackedentityattributeid = " )
                        .append( item.getItem().getId() )
                        .append( " AND (" );

                    for ( String queryToken : getTokens( params.getQuery().getFilter() ) )
                    {
                        final String query = statementBuilder.encode( queryToken, false );

                        attributes
                            .append( orHlp.or() )
                            .append( "lower(value) " )
                            .append( regexp )
                            .append( " '" )
                            .append( start )
                            .append( StringUtils.lowerCase( query ) )
                            .append( end )
                            .append( "'" );
                    }

                    attributes.append( "))" );
                }
            }

            attributes.append( ") " );
        }

        return attributes.toString();
    }

    private String getFromSubQueryJoinOrderByAttributes( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder joinOrderAttributes = new StringBuilder();

        for ( QueryItem orderAttribute : getOrderAttributes( params ) )
        {
            joinOrderAttributes
                .append( "LEFT JOIN trackedentityattributevalue AS " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                .append( " ON " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                .append( ".trackedentityinstanceid = TEI.trackedentityinstanceid " )
                .append( "AND " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                .append( ".trackedentityattributeid = " )
                .append( orderAttribute.getItem().getId() )
                .append( " " );
        }

        return joinOrderAttributes.toString();
    }

    private String getFromSubQueryJoinProgramOwner( TrackedEntityInstanceQueryParams params )
    {
        if ( !params.hasProgram() )
        {
            return "";
        }

        return new StringBuilder()
            .append( "INNER JOIN trackedentityprogramowner PO " )
            .append( "ON PO.programid = " )
            .append( params.getProgram().getId() )
            .append( " AND PO.trackedentityinstanceid = TEI.trackedentityinstanceid " )
            .toString();
    }

    private String getFromSubQueryJoinOrgUnit( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder orgUnits = new StringBuilder();

        params.handleOrganisationUnits();

        orgUnits
            .append( "INNER JOIN organisationunit OU " )
            .append( "ON OU.organisationunitid = " )
            .append( (params.hasProgram() ? "PO.organisationunitid " : "TEI.organisationunitid ") );

        if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS ) )
        {
            SqlHelper orHlp = new SqlHelper( true );

            orgUnits.append( "AND (" );

            for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
            {
                orgUnits
                    .append( orHlp.or() )
                    .append( "OU.path LIKE '" )
                    .append( organisationUnit.getPath() )
                    .append( "%'" );
            }

            orgUnits.append( ") " );
        }
        else if ( !params.isOrganisationUnitMode( OrganisationUnitSelectionMode.ALL ) )
        {
            orgUnits
                .append( "AND OU.organisationunitid IN (" )
                .append( getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) ) )
                .append( ") " );
        }

        return orgUnits.toString();
    }

    private String getFromSubQueryProgramInstanceConditions( SqlHelper whereAnd,
        TrackedEntityInstanceQueryParams params )
    {
        StringBuilder program = new StringBuilder();

        if ( !params.hasProgram() )
        {
            return "";
        }

        program
            .append( whereAnd.whereAnd() )
            .append( "EXISTS (" )
            .append( "SELECT PI.trackedentityinstanceid " )
            .append( "FROM programinstance PI " );

        if ( params.hasFilterForEvents() )
        {
            program.append( getFromSubQueryProgramStageInstance( params ) );
        }

        program
            .append( "WHERE PI.trackedentityinstanceid = TEI.trackedentityinstanceid " )
            .append( "AND PI.programid = " )
            .append( params.getProgram().getId() )
            .append( " " );

        if ( params.hasProgramStatus() )
        {
            program
                .append( "AND PI.status = '" )
                .append( params.getProgramStatus() )
                .append( "' " );
        }

        if ( params.hasFollowUp() )
        {
            program
                .append( "AND PI.followup IS " )
                .append( params.getFollowUp() )
                .append( " " );
        }

        if ( params.hasProgramEnrollmentStartDate() )
        {
            program
                .append( "AND PI.enrollmentdate >= '" )
                .append( getMediumDateString( params.getProgramEnrollmentStartDate() ) )
                .append( "' " );
        }

        if ( params.hasProgramEnrollmentEndDate() )
        {
            program
                .append( "AND PI.enrollmentdate <= '" )
                .append( getMediumDateString( params.getProgramEnrollmentEndDate() ) )
                .append( "' " );
        }

        if ( params.hasProgramIncidentStartDate() )
        {
            program
                .append( "AND PI.incidentdate >= '" )
                .append( getMediumDateString( params.getProgramIncidentStartDate() ) )
                .append( "' " );
        }

        if ( params.hasProgramIncidentEndDate() )
        {
            program
                .append( "AND PI.incidentdate <= '" )
                .append( getMediumDateString( params.getProgramIncidentEndDate() ) )
                .append( "' " );
        }

        if ( !params.isIncludeDeleted() )
        {
            program.append( "AND PI.deleted is false " );
        }

        program.append( ") " );

        return program.toString();
    }

    private String getFromSubQueryProgramStageInstance( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder events = new StringBuilder();
        SqlHelper whereHlp = new SqlHelper( true );

        events
            .append( "INNER JOIN (" )
            .append( "SELECT PSI.programinstanceid " )
            .append( "FROM programstageinstance PSI " );

        if ( params.hasAssignedUsers() )
        {
            events
                .append( "INNER JOIN (" )
                .append( "SELECT userinfoid AS userid " )
                .append( "FROM userinfo " )
                .append( "WHERE uid IN (" )
                .append( getQuotedCommaDelimitedString( params.getAssignedUsers() ) )
                .append( ") " )
                .append( ") AU ON AU.userid = PSI.assigneduserid" );
        }

        if ( params.hasEventStatus() )
        {
            String start = getMediumDateString( params.getEventStartDate() );
            String end = getMediumDateString( params.getEventEndDate() );

            if ( params.isEventStatus( EventStatus.SCHEDULE ) || params.isEventStatus( EventStatus.OVERDUE ) )
            {
                events.append( "PSI.status IS NOT NULL " );
            }
            else
            {
                events
                    .append( whereHlp.whereAnd() )
                    .append( "PSI.status = '" )
                    .append( params.getEventStatus() )
                    .append( "' " );
            }

            if ( params.isEventStatus( EventStatus.COMPLETED ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, "PSI.executiondate", start, end ) );
            }
            else if ( params.isEventStatus( EventStatus.VISITED ) || params.isEventStatus( EventStatus.ACTIVE ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, "PSI.executiondate", start, end ) );
            }
            else if ( params.isEventStatus( EventStatus.SCHEDULE ) )
            {
                events
                    .append( getQueryDateConditionBetween( whereHlp, "PSI.duedate", start, end ) )
                    .append( "AND PSI.executiondate IS NULL " )
                    .append( "AND date(now()) <= date(PSI.duedate) " );
            }
            else if ( params.isEventStatus( EventStatus.OVERDUE ) )
            {
                events
                    .append( getQueryDateConditionBetween( whereHlp, "PSI.duedate", start, end ) )
                    .append( "AND PSI.executiondate IS NULL " )
                    .append( "AND date(now()) > date(PSI.duedate) " );
            }
            else if ( params.isEventStatus( EventStatus.SKIPPED ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, "PSI.duedate", start, end ) );
            }
        }

        if ( params.hasProgramStage() )
        {
            events
                .append( whereHlp.whereAnd() )
                .append( "PSI.programstageid = " )
                .append( params.getProgramStage().getId() )
                .append( " " );
        }

        if ( params.isIncludeOnlyUnassignedEvents() )
        {
            events
                .append( whereHlp.whereAnd() )
                .append( "PSI.assigneduserid IS NULL " );
        }

        if ( params.isIncludeOnlyAssignedEvents() )
        {
            events
                .append( whereHlp.whereAnd() )
                .append( "PSI.assigneduserid IS NOT NULL " );
        }

        if ( !params.isIncludeDeleted() )
        {
            events.append( whereHlp.whereAnd() )
                .append( "PSI.deleted IS FALSE" );
        }

        events.append( ") PSI ON PSI.programinstanceid = PI.programinstanceid " );

        return events.toString();
    }

    private String getQueryDateConditionBetween( SqlHelper whereHelper, String column, String start, String end )
    {
        StringBuilder dateBetween = new StringBuilder();

        if ( start != null )
        {
            dateBetween
                .append( whereHelper.whereAnd() )
                .append( column )
                .append( " >= " )
                .append( start );
        }

        if ( end != null )
        {
            dateBetween
                .append( whereHelper.whereAnd() )
                .append( column )
                .append( " <= " )
                .append( end );
        }

        return dateBetween.append( " " ).toString();
    }

    private String getQueryRelatedTables( TrackedEntityInstanceQueryParams params )
    {
        List<QueryItem> attributes = params.getAttributes();
        StringBuilder relatedTables = new StringBuilder();

        relatedTables.append( "LEFT JOIN trackedentitytype TET ON TET.trackedentitytypeid = TEI.trackedentitytypeid " );

        if ( !attributes.isEmpty() )
        {
            String attributeString = getCommaDelimitedString( attributes.stream()
                .map( QueryItem::getItem )
                .map( IdentifiableObject::getId )
                .collect( Collectors.toList() ) );

            relatedTables.append( "LEFT JOIN trackedentityattributevalue TEAV " )
                .append( "ON TEAV.trackedentityinstanceid = TEI.trackedentityinstanceid " )
                .append( "AND TEAV.trackedentityattributeid IN (" )
                .append( attributeString )
                .append( ") " );

            relatedTables.append( "LEFT JOIN trackedentityattribute TEA " )
                .append( "ON TEA.trackedentityattributeid = TEAV.trackedentityattributeid " );
        }

        return relatedTables.toString();
    }

    private String getQueryGroupBy( TrackedEntityInstanceQueryParams params )
    {
        return new StringBuilder()
            .append( "GROUP BY TEI.uid, " )
            .append( "TEI.created, " )
            .append( "TEI.lastupdated, " )
            .append( "TEI.ou, " )
            .append( "TEI.ouname, " )
            .append( "TET.uid, " )
            .append( "TEI.inactive " )
            .append( (params.isIncludeDeleted() ? ", TEI.deleted " : "") )
            .toString();
    }

    private String getFromSubQueryOrderBy( TrackedEntityInstanceQueryParams params )
    {
        List<String> cols = getStaticGridColumns();

        if ( params.getOrders() != null && params.getAttributes() != null && !params.getAttributes().isEmpty() && !cols
            .isEmpty() )
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

                        for ( QueryItem item : params.getAttributes() )
                        {
                            if ( prop[0].equals( item.getItemId() ) )
                            {
                                orderFields.add( statementBuilder.columnQuote( prop[0] ) + ".value " + prop[1] );
                                break;
                            }
                        }
                    }
                }

            }

            if ( !orderFields.isEmpty() )
            {
                return "ORDER BY " + StringUtils.join( orderFields, ',' ) + " ";
            }
        }

        return "ORDER BY TEI.lastupdated DESC ";
    }

    private List<QueryItem> getOrderAttributes( TrackedEntityInstanceQueryParams params )
    {
        if ( params.getOrders() != null )
        {
            List<String> ordersIdentifier = params.getOrders().stream()
                .map( order -> order.split( ":" )[0] )
                .collect( Collectors.toList() );

            return params.getAttributes().stream()
                .filter( queryItem -> ordersIdentifier.contains( queryItem.getItemId() ) )
                .collect( Collectors.toList() );
        }

        return Lists.newArrayList();
    }

    private String getFromSubQueryLimitAndOffset( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder limitOffset = new StringBuilder();
        int limit = 0;

        if ( params.hasTrackedEntityType() )
        {
            limit = params.getTrackedEntityType().getMaxTeiCountToReturn();
        }

        if ( params.hasProgram() )
        {
            limit = params.getProgram().getMaxTeiCountToReturn();
        }

        if ( limit == 0 && !params.isPaging() )
        {
            return "";
        }
        else if ( limit == 0 && params.isPaging() )
        {
            return limitOffset
                .append( "LIMIT " )
                .append( params.getPageSizeWithDefault() )
                .append( " OFFSET " )
                .append( params.getOffset() )
                .append( " " )
                .toString();
        }
        else if ( params.isPaging() )
        {
            return limitOffset
                .append( "LIMIT " )
                .append( Math.min( limit + 1, params.getPageSizeWithDefault() ) )
                .append( " OFFSET " )
                .append( params.getOffset() )
                .append( " " )
                .toString();
        }
        else
        {
            return limitOffset
                .append( "LIMIT " )
                .append( limit + 1 ) // We add +1, since we use this limit to restrict a user to search to wide.
                .append( " " )
                .toString();
        }
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
            sql += params.getProgram().getId()
                + ") as tepo ON tei.trackedentityinstanceid = tepo.trackedentityinstanceid ";
            teiOuSource = "tepo.organisationunitid";

            sql += "inner join ("
                + "select trackedentityinstanceid, min(case when status='ACTIVE' then 0 when status='COMPLETED' then 1 else 2 end) as status "
                + "from programinstance pi ";

            if ( params.hasFilterForEvents() )
            {
                sql += " inner join (select programinstanceid from programstageinstance psi ";

                if ( params.hasAssignedUsers() )
                {
                    sql += " left join userinfo au on (psi.assigneduserid=au.userinfoid) ";
                }

                sql += getEventWhereClause( params );

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
                sql +=
                    "and pi.enrollmentdate >= '" + getMediumDateString( params.getProgramEnrollmentStartDate() ) + "' ";
            }

            if ( params.hasProgramEnrollmentEndDate() )
            {
                sql +=
                    "and pi.enrollmentdate <= '" + getMediumDateString( params.getProgramEnrollmentEndDate() ) + "' ";
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

        if ( params.hasTrackedEntityInstances() )
        {
            sql += hlp.whereAnd() + " tei.uid in (" + getQuotedCommaDelimitedString(
                params.getTrackedEntityInstanceUids() ) + ")";
        }

        if ( !params.hasTrackedEntityType() )
        {
            sql += hlp.whereAnd() + " tei.trackedentitytypeid in (" + params.getTrackedEntityTypes().stream()
                .filter( Objects::nonNull )
                .map( TrackedEntityType::getId )
                .map( String::valueOf )
                .collect( Collectors.joining( ", " ) ) + ") ";
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

        return Arrays.asList( TRACKED_ENTITY_INSTANCE_ID, CREATED_ID, LAST_UPDATED_ID, ORG_UNIT_ID, ORG_UNIT_NAME,
            TRACKED_ENTITY_ID, INACTIVE_ID );
    }

    private String getEventWhereClause( TrackedEntityInstanceQueryParams params )
    {
        String sql = " where ";

        if ( params.hasEventStatus() )
        {
            String start = getMediumDateString( params.getEventStartDate() );
            String end = getMediumDateString( params.getEventEndDate() );

            if ( params.isEventStatus( EventStatus.COMPLETED ) )
            {
                sql += " psi.executiondate >= '" + start + "' and psi.executiondate <= '" + end + "' "
                    + "and psi.status = '" + EventStatus.COMPLETED.name()
                    + "' and ";
            }
            else if ( params.isEventStatus( EventStatus.VISITED ) || params.isEventStatus( EventStatus.ACTIVE ) )
            {
                sql += " psi.executiondate >= '" + start + "' and psi.executiondate <= '" + end + "' "
                    + "and psi.status = '" + EventStatus.ACTIVE.name()
                    + "' and ";
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
        }

        if ( params.hasProgramStage() )
        {
            sql += " psi.programstageid = " + params.getProgramStage().getId() + " and ";
        }

        if ( params.hasAssignedUsers() )
        {
            sql += " (au.uid in (" + getQuotedCommaDelimitedString( params.getAssignedUsers() ) + ")) and ";
        }

        if ( params.isIncludeOnlyUnassignedEvents() )
        {
            sql += " (psi.assigneduserid is null) and ";
        }

        if ( params.isIncludeOnlyAssignedEvents() )
        {
            sql += " (psi.assigneduserid is not null) and ";
        }

        sql += " psi.deleted is false ";

        return sql;
    }

    private String getEventWhereClauseHql( TrackedEntityInstanceQueryParams params )
    {
        String hql = "";

        if ( params.hasEventStatus() )
        {
            String start = getMediumDateString( params.getEventStartDate() );
            String end = getMediumDateString( params.getEventEndDate() );

            if ( params.isEventStatus( EventStatus.COMPLETED ) )
            {
                hql += " psi.executionDate >= '" + start + "' and psi.executionDate <= '" + end + "' "
                    + "and psi.status = '" + EventStatus.COMPLETED.name()
                    + "' and ";
            }
            else if ( params.isEventStatus( EventStatus.VISITED ) || params.isEventStatus( EventStatus.ACTIVE ) )
            {
                hql += " psi.executionDate >= '" + start + "' and psi.executionDate <= '" + end + "' "
                    + "and psi.status = '" + EventStatus.ACTIVE.name()
                    + "' and ";
            }
            else if ( params.isEventStatus( EventStatus.SCHEDULE ) )
            {
                hql += " psi.executionDate is null and psi.dueDate >= '" + start + "' and psi.dueDate <= '" + end + "' "
                    + "and psi.status is not null and current_date <= psi.dueDate and ";
            }
            else if ( params.isEventStatus( EventStatus.OVERDUE ) )
            {
                hql += " psi.executionDate is null and psi.dueDate >= '" + start + "' and psi.dueDate <= '" + end + "' "
                    + "and psi.status is not null and current_date > psi.dueDate and ";
            }
            else if ( params.isEventStatus( EventStatus.SKIPPED ) )
            {
                hql += " psi.dueDate >= '" + start + "' and psi.dueDate <= '" + end + "' " + "and psi.status = '"
                    + EventStatus.SKIPPED.name() + "' and ";
            }
        }

        if ( params.hasProgramStage() )
        {
            hql += " psi.programStage.uid = " + params.getProgramStage().getUid() + " and ";
        }

        if ( params.hasAssignedUsers() )
        {
            hql += " (au.uid in (" + getQuotedCommaDelimitedString( params.getAssignedUsers() ) + ")) and ";
        }

        if ( params.isIncludeOnlyUnassignedEvents() )
        {
            hql += " (psi.assignedUser is null) and ";
        }

        if ( params.isIncludeOnlyAssignedEvents() )
        {
            hql += " (psi.assignedUser is not null) and ";
        }

        hql += " psi.deleted=false ";

        return hql;
    }

    @Override
    public boolean exists( String uid )
    {
        Query query = getSession()
            .createNativeQuery( "select count(*) from trackedentityinstance where uid=? and deleted is false" );
        query.setParameter( 1, uid );
        int count = ((Number) query.getSingleResult()).intValue();

        return count > 0;
    }

    @Override
    public boolean existsIncludingDeleted( String uid )
    {
        Query query = getSession().createNativeQuery( "select count(*) from trackedentityinstance where uid=?" );
        query.setParameter( 1, uid );
        int count = ((Number) query.getSingleResult()).intValue();

        return count > 0;
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
                resultUids.addAll(
                    getSession().createQuery( hql, String.class ).setParameter( "uids", uidsPartition ).list() );
            }
        }

        return resultUids;
    }

    @Override
    public void updateTrackedEntityInstancesSyncTimestamp( List<String> trackedEntityInstanceUIDs,
        Date lastSynchronized )
    {
        String hql = "update TrackedEntityInstance set lastSynchronized = :lastSynchronized WHERE uid in :trackedEntityInstances";

        getQuery( hql )
            .setParameter( "lastSynchronized", lastSynchronized )
            .setParameter( "trackedEntityInstances", trackedEntityInstanceUIDs )
            .executeUpdate();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityInstance> getTrackedEntityInstancesByUid( List<String> uids, User user )
    {
        return getSharingCriteria( user )
            .add( Restrictions.in( "uid", uids ) )
            .list();
    }

    @Override
    protected void preProcessPredicates( CriteriaBuilder builder,
        List<Function<Root<TrackedEntityInstance>, Predicate>> predicates )
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