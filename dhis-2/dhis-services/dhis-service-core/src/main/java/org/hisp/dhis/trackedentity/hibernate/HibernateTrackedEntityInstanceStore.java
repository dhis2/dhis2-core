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
package org.hisp.dhis.trackedentity.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getTokens;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.CREATED_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.DELETED;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.INACTIVE_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.LAST_UPDATED_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.ORG_UNIT_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.ORG_UNIT_NAME;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.OrderColumn.getColumn;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.OrderColumn.isStaticColumn;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.TRACKED_ENTITY_ID;
import static org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams.TRACKED_ENTITY_INSTANCE_ID;
import static org.hisp.dhis.util.DateUtils.getDateAfterAddition;
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;

/**
 * @author Abyot Asalefew Gizaw
 */
@Slf4j
@Repository( "org.hisp.dhis.trackedentity.TrackedEntityInstanceStore" )
public class HibernateTrackedEntityInstanceStore
    extends SoftDeleteHibernateObjectStore<TrackedEntityInstance>
    implements TrackedEntityInstanceStore
{
    private final static String TEI_HQL_BY_UIDS = "from TrackedEntityInstance as tei where tei.uid in (:uids)";

    private static final String AND_PSI_STATUS_EQUALS_SINGLE_QUOTE = "and psi.status = '";

    private static final String OFFSET = "OFFSET";

    private static final String LIMIT = "LIMIT";

    private static final String PSI_EXECUTIONDATE = "PSI.executiondate";

    private static final String PSI_DUEDATE = "PSI.duedate";

    private static final String IS_NULL = "IS NULL";

    private static final String IS_NOT_NULL = "IS NOT NULL";

    private static final String SPACE = " ";

    private static final String SINGLE_QUOTE = "'";

    private static final String EQUALS = " = ";

    private static final String PSI_STATUS = "PSI.status";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final OrganisationUnitStore organisationUnitStore;

    private final StatementBuilder statementBuilder;

    private final static String SELECT_TEI = "select tei from";

    public HibernateTrackedEntityInstanceStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService,
        AclService aclService, OrganisationUnitStore organisationUnitStore, StatementBuilder statementBuilder )
    {
        super( sessionFactory, jdbcTemplate, publisher, TrackedEntityInstance.class, currentUserService, aclService,
            false );

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
        String hql = buildTrackedEntityInstanceHql( params, false );

        // If it is a sync job running a query, I need to adjust an HQL a bit,
        // because I am adding 2 joins and don't want duplicates in results
        if ( params.isSynchronizationQuery() )
        {
            hql = hql.replaceFirst( SELECT_TEI, "select distinct tei from" );
        }

        Query query = getQuery( hql );

        if ( params.isPaging() )
        {
            query.setFirstResult( params.getOffset() );
            query.setMaxResults( params.getPageSizeWithDefault() );
        }

        return query.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Long> getTrackedEntityInstanceIds( TrackedEntityInstanceQueryParams params )
    {
        String sql = getQuery( params, false );
        log.debug( "Tracked entity instance query SQL: " + sql );
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        checkMaxTeiCountReached( params, rowSet );

        List<Long> ids = new ArrayList<>();

        while ( rowSet.next() )
        {
            ids.add( rowSet.getLong( "teiid" ) );
        }

        return ids;
    }

    private String buildTrackedEntityInstanceCountHql( TrackedEntityInstanceQueryParams params )
    {
        return buildTrackedEntityInstanceHql( params, false )
            .replaceFirst( SELECT_TEI, "select count(distinct tei) from" )
            .replaceFirst( "inner join fetch tei.programInstances", "inner join tei.programInstances" )
            .replaceFirst( "inner join fetch pi.programStageInstances", "inner join pi.programStageInstances" )
            .replaceFirst( "inner join fetch psi.assignedUser", "inner join psi.assignedUser" )
            .replaceFirst( "inner join fetch tei.programOwners", "inner join tei.programOwners" )
            .replaceFirst( Pattern.quote( getOrderClauseHql( params ) ), " " );
    }

    private String withProgram( TrackedEntityInstanceQueryParams params, SqlHelper hlp )
    {
        String hql = "";

        if ( params.hasProgram() )
        {
            hql += "inner join fetch tei.programInstances as pi ";

            // Joining program owners and using that as tei ou source
            hql += "inner join fetch tei.programOwners as po ";

            if ( params.hasFilterForEvents() )
            {
                hql += " inner join fetch pi.programStageInstances psi ";

                hql += addConditionally( params.hasAssignedUsers(), () -> "inner join fetch psi.assignedUser au" );

                hql += hlp.whereAnd() + getEventWhereClauseHql( params );

            }

            hql += hlp.whereAnd() + " po.program.uid = '" + params.getProgram().getUid() + "'";

            hql += hlp.whereAnd() + " pi.program.uid = '" + params.getProgram().getUid() + "'";

            hql += addWhereConditionally( hlp, params.hasProgramStatus(),
                () -> "pi.status = '" + params.getProgramStatus() + "'" );

            hql += addWhereConditionally( hlp, params.hasFollowUp(), () -> "pi.followup = " + params.getFollowUp() );

            hql += addWhereConditionally( hlp, params.hasProgramEnrollmentStartDate(),
                () -> "pi.enrollmentDate >= '" + getMediumDateString( params.getProgramEnrollmentStartDate() ) + "'" );

            hql += addWhereConditionally( hlp, params.hasProgramEnrollmentEndDate(),
                () -> "pi.enrollmentDate < '" + getMediumDateString( params.getProgramEnrollmentEndDate() ) + "'" );

            hql += addWhereConditionally( hlp, params.hasProgramIncidentStartDate(),
                () -> "pi.incidentDate >= '" + getMediumDateString( params.getProgramIncidentStartDate() ) + "'" );

            hql += addWhereConditionally( hlp, params.hasProgramIncidentEndDate(),
                () -> "pi.incidentDate < '" + getMediumDateString( params.getProgramIncidentEndDate() ) + "'" );

            hql += addWhereConditionally( hlp, !params.isIncludeDeleted(),
                () -> "pi.deleted is false " );

        }
        return hql;
    }

    private String withOrgUnits( TrackedEntityInstanceQueryParams params, SqlHelper hlp, String teiOuSource )
    {
        String hql = "";
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
                hql += hlp.whereAnd() + teiOuSource + ".uid in ("
                    + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnits() ) ) + ")";
            }
        }
        return hql;
    }

    private String withFilters( TrackedEntityInstanceQueryParams params, SqlHelper hlp )
    {
        String hql = "";
        if ( params.hasFilters() )
        {
            for ( QueryItem queryItem : params.getFilters() )
            {
                for ( QueryFilter queryFilter : queryItem.getFilters() )
                {
                    String encodedFilter = queryFilter.getSqlFilter(
                        statementBuilder.encode( StringUtils.lowerCase( queryFilter.getFilter() ), false ) );

                    hql += hlp.whereAnd()
                        + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

                    hql += " and teav.attribute.uid='" + queryItem.getItemId() + "'";

                    hql += addConditionally( queryItem.isNumeric(),
                        " and teav.plainValue " + queryFilter.getSqlOperator() + encodedFilter + ")",
                        " and lower(teav.plainValue) " + queryFilter.getSqlOperator() + encodedFilter + ")" );
                }
            }
        }
        return hql;
    }

    private String buildTrackedEntityInstanceHql( TrackedEntityInstanceQueryParams params, boolean idOnly )
    {
        SqlHelper hlp = new SqlHelper( true );

        String hql = "select " + (idOnly ? "tei.id" : "tei") + " from TrackedEntityInstance tei ";

        // Used for switching between registration org unit or ownership org
        // unit. Default source is registration ou.
        String teiOuSource = params.hasProgram() ? "po.organisationUnit" : "tei.organisationUnit";

        if ( params.hasAttributeAsOrder() )
        {
            hql += " left join tei.trackedEntityAttributeValues teav2 " +
                "left join teav2.attribute as attr2 ";
        }

        hql += withProgram( params, hlp );

        if ( params.hasAttributeAsOrder() )
        {
            hql += hlp.whereAnd() + " attr2.uid='" + params.getFirstAttributeOrder() + "'  ";
        }

        // If sync job, fetch only TEAVs that are supposed to be synchronized

        hql += addConditionally( params.isSynchronizationQuery(),
            () -> "left join tei.trackedEntityAttributeValues teav1 " +
                "left join teav1.attribute as attr" + hlp.whereAnd() + " attr.skipSynchronization = false" );

        hql += addWhereConditionally( hlp, params.hasTrackedEntityType(),
            () -> " tei.trackedEntityType.uid='" + params.getTrackedEntityType().getUid() + "'" );

        hql += addWhereConditionally( hlp, params.hasTrackedEntityInstances(),
            () -> " tei.uid in (" + getQuotedCommaDelimitedString( params.getTrackedEntityInstanceUids() ) + ")" );

        if ( params.hasLastUpdatedDuration() )
        {
            hql += hlp.whereAnd() + " tei.lastUpdated >= '" +
                getLongGmtDateString( DateUtils.nowMinusDuration( params.getLastUpdatedDuration() ) ) + "'";
        }
        else
        {
            hql += addWhereConditionally( hlp, params.hasLastUpdatedStartDate(), () -> " tei.lastUpdated >= '" +
                getMediumDateString( params.getLastUpdatedStartDate() ) + "'" );

            hql += addWhereConditionally( hlp, params.hasLastUpdatedEndDate(), () -> " tei.lastUpdated < '" +
                getMediumDateString( getDateAfterAddition( params.getLastUpdatedEndDate(), 1 ) ) + "'" );
        }

        hql += addWhereConditionally( hlp, params.isSynchronizationQuery(),
            () -> " tei.lastUpdated > tei.lastSynchronized" );

        // Comparing milliseconds instead of always creating new Date( 0 )

        if ( params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0 )
        {
            String skipChangedBefore = DateUtils.getLongDateString( params.getSkipChangedBefore() );
            hql += hlp.whereAnd() + " tei.lastUpdated >= '" + skipChangedBefore + "'";
        }

        params.handleOrganisationUnits();

        hql += withOrgUnits( params, hlp, teiOuSource );

        if ( params.hasQuery() )
        {
            QueryFilter queryFilter = params.getQuery();

            String encodedFilter = queryFilter
                .getSqlFilter( statementBuilder.encode( queryFilter.getFilter(), false ) );

            hql += hlp.whereAnd() + " exists (from TrackedEntityAttributeValue teav where teav.entityInstance=tei";

            hql += " and teav.plainValue " + queryFilter.getSqlOperator() + encodedFilter + ")";
        }

        hql += withFilters( params, hlp );

        hql += addWhereConditionally( hlp, !params.isIncludeDeleted(), () -> " tei.deleted is false " );

        hql += getOrderClauseHql( params );

        return hql;
    }

    @Override
    public List<Map<String, String>> getTrackedEntityInstancesGrid( TrackedEntityInstanceQueryParams params )
    {
        String sql = getQuery( params, true );
        log.debug( "Tracked entity instance query SQL: " + sql );

        // ---------------------------------------------------------------------
        // Query
        // ---------------------------------------------------------------------

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        checkMaxTeiCountReached( params, rowSet );

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

            if ( !params.getAttributesAndFilters().isEmpty() )
            {
                HashMap<String, String> attributeValues = new HashMap<>();
                String teavString = rowSet.getString( "tea_values" );

                extractFromTeavAggregatedString( attributeValues, teavString );

                for ( QueryItem item : params.getAttributes() )
                {
                    map.put( item.getItemId(),
                        isOrgUnit( item ) && attributeValues.containsKey( item.getItemId() )
                            ? getOrgUnitNameByUid( attributeValues.get( item.getItemId() ) )
                            : attributeValues.get( item.getItemId() ) );
                }
            }

            list.add( map );
        }

        return list;
    }

    private void extractFromTeavAggregatedString( HashMap<String, String> attributeValues, String teavString )
    {
        if ( teavString != null )
        {
            String[] pairs = teavString.split( ";" );

            for ( String pair : pairs )
            {
                String[] teav = pair.split( ":" );

                if ( teav.length == 2 )
                {
                    attributeValues.put( teav[0], teav[1] );
                }
            }
        }
    }

    private void checkMaxTeiCountReached( TrackedEntityInstanceQueryParams params, SqlRowSet rowSet )
    {
        if ( params.getMaxTeiLimit() > 0 && rowSet.last() )
        {
            if ( rowSet.getRow() > params.getMaxTeiLimit() )
            {
                throw new IllegalQueryException( "maxteicountreached" );
            }
            rowSet.beforeFirst();
        }
    }

    @Override
    public int getTrackedEntityInstanceCountForGrid( TrackedEntityInstanceQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Select clause
        // ---------------------------------------------------------------------

        String sql = getCountQuery( params );

        // ---------------------------------------------------------------------
        // Query
        // ---------------------------------------------------------------------

        log.debug( "Tracked entity instance count SQL: " + sql );

        Integer count = jdbcTemplate.queryForObject( sql, Integer.class );

        return count;
    }

    private String getQuery( TrackedEntityInstanceQueryParams params, boolean isGridQuery )
    {
        return new StringBuilder()
            .append( getQuerySelect( params, isGridQuery ) )
            .append( "FROM " )
            .append( getFromSubQuery( params, false ) )
            .append( getQueryRelatedTables( params ) )
            .append( getQueryGroupBy( params ) )
            .append( getQueryOrderBy( false, params, isGridQuery ) )
            .toString();
    }

    private String getCountQuery( TrackedEntityInstanceQueryParams params )
    {
        return new StringBuilder()
            .append( getQueryCountSelect( params ) )
            .append( getQuerySelect( params, true ) )
            .append( "FROM " )
            .append( getFromSubQuery( params, true ) )
            .append( getQueryRelatedTables( params ) )
            .append( getQueryGroupBy( params ) )
            .append( " ) teicount" )
            .toString();
    }

    private String getQuerySelect( TrackedEntityInstanceQueryParams params, boolean isGridQuery )
    {
        StringBuilder select = new StringBuilder()
            .append( "SELECT TEI.uid AS " + TRACKED_ENTITY_INSTANCE_ID + ", " )
            .append( "TEI.created AS " + CREATED_ID + ", " )
            .append( "TEI.lastupdated AS " + LAST_UPDATED_ID + ", " )
            .append( "TEI.ou AS " + ORG_UNIT_ID + ", " )
            .append( "TEI.ouname AS " + ORG_UNIT_NAME + ", " )
            .append( "TET.uid AS " + TRACKED_ENTITY_ID + ", " )
            .append( "TEI.inactive AS " + INACTIVE_ID )
            .append( (params.isIncludeDeleted() ? ", TEI.deleted AS " + DELETED : "") )
            .append( (params.hasAttributes() ? ", string_agg(TEA.uid || ':' || TEAV.value, ';') AS tea_values" : "") );

        if ( !isGridQuery )
        {
            select.append( ", TEI.trackedentityinstanceid AS teiid" );
        }
        select.append( SPACE );

        return select.toString();
    }

    private String getQueryCountSelect( TrackedEntityInstanceQueryParams params )
    {
        return "SELECT count(instance) FROM ( ";
    }

    private String getFromSubQuery( TrackedEntityInstanceQueryParams params, boolean isCountQuery )
    {
        SqlHelper whereAnd = new SqlHelper( true );
        StringBuilder fromSubQuery = new StringBuilder()
            .append( "(" )
            .append( getFromSubQuerySelect( params ) )
            .append( " FROM trackedentityinstance TEI " )

            // INNER JOIN on constraints
            .append( getFromSubQueryJoinAttributeConditions( whereAnd, params ) )
            .append( getFromSubQueryJoinProgramOwnerConditions( params ) )
            .append( getFromSubQueryJoinOrgUnitConditions( params ) )

            // LEFT JOIN attributes we need to sort on.
            .append( getFromSubQueryJoinOrderByAttributes( params ) )

            // WHERE
            .append( getFromSubQueryTrackedEntityConditions( whereAnd, params ) )
            .append( getFromSubQueryProgramInstanceConditions( whereAnd, params ) );

        if ( !isCountQuery )
        {
            // SORT
            fromSubQuery
                .append( getQueryOrderBy( true, params, true ) )

                // LIMIT, OFFSET
                .append( getFromSubQueryLimitAndOffset( params ) );
        }

        return fromSubQuery.append( ") TEI " )
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
                .append( SPACE );
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

        if ( params.hasLastUpdatedDuration() )
        {
            trackedEntity.append( whereAnd.whereAnd() )
                .append( " TEI.lastupdated >= '" )
                .append( getLongGmtDateString( DateUtils.nowMinusDuration( params.getLastUpdatedDuration() ) ) )
                .append( SINGLE_QUOTE );
        }
        else
        {
            if ( params.hasLastUpdatedStartDate() )
            {
                trackedEntity.append( " TEI.lastupdated >= '" )
                    .append( getMediumDateString( params.getLastUpdatedStartDate() ) ).append( SINGLE_QUOTE );
            }
            if ( params.hasLastUpdatedEndDate() )
            {
                trackedEntity.append( " TEI.lastupdated < '" )
                    .append( getMediumDateString( getDateAfterAddition( params.getLastUpdatedEndDate(), 1 ) ) )
                    .append( SINGLE_QUOTE );
            }
        }

        if ( !params.isIncludeDeleted() )
        {
            trackedEntity
                .append( whereAnd.whereAnd() )
                .append( "TEI.deleted IS FALSE " );
        }

        return trackedEntity.toString();
    }

    private String getFromSubQueryJoinAttributeConditions( SqlHelper whereAnd, TrackedEntityInstanceQueryParams params )
    {
        StringBuilder attributes = new StringBuilder();

        List<QueryItem> filterItems = params.getAttributesAndFilters().stream()
            .filter( QueryItem::hasFilter )
            .collect( Collectors.toList() );

        if ( !filterItems.isEmpty() || params.isOrQuery() )
        {
            if ( !params.isOrQuery() )
            {
                joinAttributeValueWithoutQueryParameter( attributes, filterItems );
            }
            else
            {
                joinAttributeValueWithQueryParameter( params, attributes );
            }
        }

        return attributes.toString();
    }

    private void joinAttributeValueWithQueryParameter( TrackedEntityInstanceQueryParams params,
        StringBuilder attributes )
    {
        final String regexp = statementBuilder.getRegexpMatch();
        final String wordStart = statementBuilder.getRegexpWordStart();
        final String wordEnd = statementBuilder.getRegexpWordEnd();
        final String anyChar = "\\.*?";
        final String start = params.getQuery().isOperator( QueryOperator.LIKE ) ? anyChar : wordStart;
        final String end = params.getQuery().isOperator( QueryOperator.LIKE ) ? anyChar : wordEnd;
        SqlHelper orHlp = new SqlHelper( true );

        List<Long> itemIds = params.getAttributesAndFilters().stream()
            .map( QueryItem::getItem )
            .map( DimensionalItemObject::getId )
            .collect( Collectors.toList() );

        attributes
            .append( "INNER JOIN trackedentityattributevalue Q " )
            .append( "ON Q.trackedentityinstanceid IN (" )
            .append( getCommaDelimitedString( itemIds ) )
            .append( ") AND (" );

        for ( String queryToken : getTokens( params.getQuery().getFilter() ) )
        {
            final String query = statementBuilder.encode( queryToken, false );

            attributes
                .append( orHlp.or() )
                .append( "lower(Q.value) " )
                .append( regexp )
                .append( " '" )
                .append( start )
                .append( StringUtils.lowerCase( query ) )
                .append( end )
                .append( SINGLE_QUOTE );
        }

        attributes.append( ")" );
    }

    private void joinAttributeValueWithoutQueryParameter( StringBuilder attributes, List<QueryItem> filterItems )
    {
        for ( QueryItem queryItem : filterItems )
        {
            String col = statementBuilder.columnQuote( queryItem.getItemId() );
            String teaId = col + ".trackedentityattributeid";
            String teav = "lower(" + col + ".value)";
            String teiid = col + ".trackedentityinstanceid";

            attributes
                .append( " INNER JOIN trackedentityattributevalue " )
                .append( col )
                .append( " ON " )
                .append( teaId )
                .append( EQUALS )
                .append( queryItem.getItem().getId() )
                .append( " AND " )
                .append( teiid )
                .append( " = TEI.trackedentityinstanceid " );

            for ( QueryFilter filter : queryItem.getFilters() )
            {
                attributes
                    .append( "AND " )
                    .append( teav )
                    .append( SPACE )
                    .append( filter.getSqlOperator() )
                    .append( SPACE )
                    .append( StringUtils.lowerCase( filter.getSqlFilter( filter.getFilter() ) ) );
            }
        }
    }

    private String getFromSubQueryJoinOrderByAttributes( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder joinOrderAttributes = new StringBuilder();

        for ( QueryItem orderAttribute : getOrderAttributes( params ) )
        {
            if ( orderAttribute.hasFilter() ) // We already joined this if it is
                                              // a filter.
            {
                continue;
            }

            joinOrderAttributes
                .append( " LEFT JOIN trackedentityattributevalue AS " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                .append( " ON " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                .append( ".trackedentityinstanceid = TEI.trackedentityinstanceid " )
                .append( "AND " )
                .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                .append( ".trackedentityattributeid = " )
                .append( orderAttribute.getItem().getId() )
                .append( SPACE );
        }

        return joinOrderAttributes.toString();
    }

    private String getFromSubQueryJoinProgramOwnerConditions( TrackedEntityInstanceQueryParams params )
    {
        if ( !params.hasProgram() )
        {
            return "";
        }

        return new StringBuilder()
            .append( " INNER JOIN trackedentityprogramowner PO " )
            .append( "ON PO.programid = " )
            .append( params.getProgram().getId() )
            .append( " AND PO.trackedentityinstanceid = TEI.trackedentityinstanceid " )
            .toString();
    }

    private String getFromSubQueryJoinOrgUnitConditions( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder orgUnits = new StringBuilder();

        params.handleOrganisationUnits();

        orgUnits
            .append( " INNER JOIN organisationunit OU " )
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
            .append( SPACE );

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
                .append( SPACE );
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
            events.append( whereHlp.whereAnd() );

            if ( params.isEventStatus( EventStatus.COMPLETED ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, PSI_EXECUTIONDATE, start, end ) )
                    .append( whereHlp.whereAnd() )
                    .append( PSI_STATUS )
                    .append( EQUALS )
                    .append( SINGLE_QUOTE )
                    .append( EventStatus.COMPLETED.name() )
                    .append( SINGLE_QUOTE )
                    .append( SPACE );
            }
            else if ( params.isEventStatus( EventStatus.VISITED ) || params.isEventStatus( EventStatus.ACTIVE ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, PSI_EXECUTIONDATE, start, end ) )
                    .append( whereHlp.whereAnd() )
                    .append( PSI_STATUS )
                    .append( EQUALS )
                    .append( SINGLE_QUOTE )
                    .append( EventStatus.ACTIVE.name() )
                    .append( SINGLE_QUOTE )
                    .append( SPACE );
            }
            else if ( params.isEventStatus( EventStatus.SCHEDULE ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, PSI_DUEDATE, start, end ) )
                    .append( whereHlp.whereAnd() )
                    .append( PSI_STATUS )
                    .append( SPACE )
                    .append( IS_NOT_NULL )
                    .append( whereHlp.whereAnd() )
                    .append( PSI_EXECUTIONDATE )
                    .append( SPACE )
                    .append( IS_NULL )
                    .append( whereHlp.whereAnd() )
                    .append( "date(now()) <= date(PSI.duedate) " );
            }
            else if ( params.isEventStatus( EventStatus.OVERDUE ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, PSI_DUEDATE, start, end ) )
                    .append( whereHlp.whereAnd() )
                    .append( PSI_STATUS )
                    .append( SPACE )
                    .append( IS_NOT_NULL )
                    .append( whereHlp.whereAnd() )
                    .append( PSI_EXECUTIONDATE )
                    .append( SPACE )
                    .append( IS_NULL )
                    .append( whereHlp.whereAnd() )
                    .append( "date(now()) > date(PSI.duedate) " );
            }
            else if ( params.isEventStatus( EventStatus.SKIPPED ) )
            {
                events.append( getQueryDateConditionBetween( whereHlp, PSI_DUEDATE, start, end ) )
                    .append( whereHlp.whereAnd() )
                    .append( PSI_STATUS )
                    .append( EQUALS )
                    .append( SINGLE_QUOTE )
                    .append( EventStatus.SKIPPED.name() )
                    .append( SINGLE_QUOTE )
                    .append( SPACE );
            }
        }

        if ( params.hasProgramStage() )
        {
            events
                .append( whereHlp.whereAnd() )
                .append( "PSI.programstageid = " )
                .append( params.getProgramStage().getId() )
                .append( SPACE );
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

        dateBetween
            .append( whereHelper.whereAnd() )
            .append( column )
            .append( " >= '" )
            .append( start )
            .append( SINGLE_QUOTE )
            .append( whereHelper.whereAnd() )
            .append( column )
            .append( " <= '" )
            .append( end )
            .append( "' " );

        return dateBetween.toString();
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
        if ( params.getAttributes().isEmpty() )
        {
            return "";
        }

        StringBuilder groupBy = new StringBuilder()
            .append( "GROUP BY TEI.trackedentityinstanceid, " )
            .append( "TEI.uid, " )
            .append( "TEI.created, " )
            .append( "TEI.lastupdated, " )
            .append( "TEI.ou, " )
            .append( "TEI.ouname, " )
            .append( "TET.uid, " )
            .append( "TEI.inactive " )
            .append( (params.isIncludeDeleted() ? ", TEI.deleted " : "") );

        if ( !getOrderAttributes( params ).isEmpty() )
        {

            for ( QueryItem orderAttribute : getOrderAttributes( params ) )
            {
                groupBy
                    .append( ", TEI." )
                    .append( statementBuilder.columnQuote( orderAttribute.getItemId() ) )
                    .append( SPACE );
            }

        }

        return groupBy.toString();
    }

    private String getOrderClauseHql( TrackedEntityInstanceQueryParams params )
    {
        String orderQuery = "order by tei.lastUpdated desc ";

        ArrayList<String> orderFields = new ArrayList<>();

        if ( params.hasOrders() )
        {
            for ( OrderParam orderParam : params.getOrders() )
            {
                if ( isStaticColumn( orderParam.getField() ) )
                {
                    String columName = getColumn( orderParam.getField() );
                    orderFields.add( columName + " " + orderParam.getDirection() );
                }
                else
                {
                    orderFields.add( "teav2.plainValue " + orderParam.getDirection() );
                    break; // currently we support only a single attribute
                           // order
                }
            }

            if ( !orderFields.isEmpty() )
            {
                orderQuery = "order by " + StringUtils.join( orderFields, ',' );
            }
        }

        return orderQuery;
    }

    private String getQueryOrderBy( boolean innerOrder, TrackedEntityInstanceQueryParams params, boolean gridQuery )
    {
        if ( params.getOrders() != null && params.getAttributes() != null && !params.getAttributes().isEmpty() )
        {
            ArrayList<String> orderFields = new ArrayList<>();

            for ( OrderParam order : params.getOrders() )
            {
                if ( isStaticColumn( order.getField() ) )
                {
                    String columnName = TrackedEntityInstanceQueryParams.OrderColumn.getColumn( order.getField() );
                    orderFields.add( columnName + " " + order.getDirection() );
                }
                else
                {
                    extractDynamicOrderField( innerOrder, params, orderFields, order );
                }
            }

            if ( !orderFields.isEmpty() )
            {
                return "ORDER BY " + StringUtils.join( orderFields, ',' ) + SPACE;
            }
        }

        if ( params.getAttributesAndFilters().stream().noneMatch( qi -> qi.hasFilter() && qi.isUnique() ) )
        {
            return "ORDER BY TEI.trackedentityinstanceid ASC ";
        }
        else
        {
            return "";
        }
    }

    private void extractDynamicOrderField( boolean innerOrder, TrackedEntityInstanceQueryParams params,
        ArrayList<String> orderFields, OrderParam order )
    {
        if ( isAttributeOrder( params, order.getField() )
            || isAttributeFilterOrder( params, order.getField() ) )
        {
            if ( innerOrder )
            {
                orderFields
                    .add( statementBuilder.columnQuote( order.getField() ) + ".value " + order.getDirection() );
            }
            else
            {
                orderFields.add(
                    "TEI." + statementBuilder.columnQuote( order.getField() ) + SPACE + order.getDirection() );
            }
        }
    }

    private boolean isAttributeOrder( TrackedEntityInstanceQueryParams params, String attributeId )
    {
        if ( params.hasAttributes() )
        {
            for ( QueryItem item : params.getAttributes() )
            {
                if ( attributeId.equals( item.getItemId() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isAttributeFilterOrder( TrackedEntityInstanceQueryParams params, String attributeId )
    {
        if ( params.hasFilters() )
        {
            for ( QueryItem item : params.getFilters() )
            {
                if ( attributeId.equals( item.getItemId() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    private List<QueryItem> getOrderAttributes( TrackedEntityInstanceQueryParams params )
    {
        if ( params.getOrders() != null )
        {
            List<String> ordersIdentifier = params.getOrders().stream()
                .map( order -> order.getField() )
                .collect( Collectors.toList() );

            return params.getAttributesAndFilters().stream()
                .filter( queryItem -> ordersIdentifier.contains( queryItem.getItemId() ) )
                .collect( Collectors.toList() );
        }

        return Lists.newArrayList();
    }

    private String getFromSubQueryLimitAndOffset( TrackedEntityInstanceQueryParams params )
    {
        StringBuilder limitOffset = new StringBuilder();
        int limit = params.getMaxTeiLimit();

        if ( limit == 0 && !params.isPaging() )
        {
            return "";
        }
        else if ( limit == 0 && params.isPaging() )
        {
            return limitOffset
                .append( LIMIT )
                .append( SPACE )
                .append( params.getPageSizeWithDefault() )
                .append( SPACE )
                .append( OFFSET )
                .append( SPACE )
                .append( params.getOffset() )
                .append( SPACE )
                .toString();
        }
        else if ( params.isPaging() )
        {
            return limitOffset
                .append( LIMIT )
                .append( SPACE )
                .append( Math.min( limit + 1, params.getPageSizeWithDefault() ) )
                .append( SPACE )
                .append( OFFSET )
                .append( SPACE )
                .append( params.getOffset() )
                .append( SPACE )
                .toString();
        }
        else
        {
            return limitOffset
                .append( LIMIT )
                .append( SPACE )
                .append( limit + 1 ) // We add +1, since we use this limit to
                                     // restrict a user to search to wide.
                .append( SPACE )
                .toString();
        }
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

        hql += addConditionally( params.hasAssignedUsers(),
            () -> "(au.uid in (" + getQuotedCommaDelimitedString( params.getAssignedUsers() ) + ")) and" );

        hql += addConditionally( params.isIncludeOnlyUnassignedEvents(),
            () -> "(psi.assignedUser is null) and" );

        hql += addConditionally( params.isIncludeOnlyAssignedEvents(),
            () -> "(psi.assignedUser is not null) and" );

        hql += " psi.deleted=false ";

        return hql;
    }

    @Override
    public boolean exists( String uid )
    {
        Query query = getSession()
            .createNativeQuery( "select count(*) from trackedentityinstance where uid=:uid and deleted is false" );
        query.setParameter( "uid", uid );
        int count = ((Number) query.getSingleResult()).intValue();

        return count > 0;
    }

    @Override
    public boolean existsIncludingDeleted( String uid )
    {
        Query query = getSession().createNativeQuery( "select count(*) from trackedentityinstance where uid=:uid" );
        query.setParameter( "uid", uid );
        int count = ((Number) query.getSingleResult()).intValue();

        return count > 0;
    }

    @Override
    public List<String> getUidsIncludingDeleted( List<String> uids )
    {
        String hql = "select tei.uid " + TEI_HQL_BY_UIDS;
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
    public List<TrackedEntityInstance> getIncludingDeleted( List<String> uids )
    {
        List<TrackedEntityInstance> trackedEntityInstances = new ArrayList<>();
        List<List<String>> uidsPartitions = Lists.partition( Lists.newArrayList( uids ), 20000 );

        for ( List<String> uidsPartition : uidsPartitions )
        {
            if ( !uidsPartition.isEmpty() )
            {
                trackedEntityInstances.addAll( getSession().createQuery( TEI_HQL_BY_UIDS, TrackedEntityInstance.class )
                    .setParameter( "uids", uidsPartition ).list() );
            }
        }

        return trackedEntityInstances;
    }

    @Override
    public void updateTrackedEntityInstancesSyncTimestamp( List<String> trackedEntityInstanceUIDs,
        Date lastSynchronized )
    {
        final String hql = "update TrackedEntityInstance set lastSynchronized = :lastSynchronized WHERE uid in :trackedEntityInstances";

        getQuery( hql )
            .setParameter( "lastSynchronized", lastSynchronized )
            .setParameter( "trackedEntityInstances", trackedEntityInstanceUIDs )
            .executeUpdate();
    }

    @Override
    public List<TrackedEntityInstance> getTrackedEntityInstancesByUid( List<String> uids, User user )
    {
        return getList( getCriteriaBuilder(), newJpaParameters().addPredicate( root -> root.get( "uid" ).in( uids ) ) );
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

    private String addConditionally( boolean condition, Supplier<String> sqlSnippet )
    {
        return condition ? " " + sqlSnippet.get() + " " : "";
    }

    private String addWhereConditionally( SqlHelper hlp, boolean condition, Supplier<String> sqlSnippet )
    {
        return condition ? hlp.whereAnd() + sqlSnippet.get() : "";
    }

    private String addConditionally( boolean condition, String sqlSnippet, String falseSqlSnippet )
    {
        return condition ? " " + sqlSnippet + " " : " " + falseSqlSnippet + " ";
    }
}