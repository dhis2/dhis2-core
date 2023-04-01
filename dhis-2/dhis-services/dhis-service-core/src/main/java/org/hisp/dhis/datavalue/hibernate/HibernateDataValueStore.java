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
package org.hisp.dhis.datavalue.hibernate;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Torgeir Lorange Ostby
 */
@Slf4j
@Repository( "org.hisp.dhis.datavalue.DataValueStore" )
public class HibernateDataValueStore extends HibernateGenericStore<DataValue>
    implements DataValueStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final PeriodStore periodStore;

    private final StatementBuilder statementBuilder;

    private static final String DELETED = "deleted";

    private static final String LAST_UPATED = "lastUpdated";

    public HibernateDataValueStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, PeriodStore periodStore, StatementBuilder statementBuilder )
    {
        super( sessionFactory, jdbcTemplate, publisher, DataValue.class, false );
        this.periodStore = periodStore;
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    @Override
    public void addDataValue( DataValue dataValue )
    {
        dataValue.setPeriod( periodStore.reloadForceAddPeriod( dataValue.getPeriod() ) );

        getSession().save( dataValue );
    }

    @Override
    public void updateDataValue( DataValue dataValue )
    {
        dataValue.setPeriod( periodStore.reloadForceAddPeriod( dataValue.getPeriod() ) );

        getSession().update( dataValue );
    }

    @Override
    public void deleteDataValues( OrganisationUnit organisationUnit )
    {
        String hql = "delete from DataValue d where d.source = :source";

        getSession().createQuery( hql ).setParameter( "source", organisationUnit ).executeUpdate();
    }

    @Override
    public void deleteDataValues( DataElement dataElement )
    {
        String hql = "delete from DataValue d where d.dataElement = :dataElement";

        getSession().createQuery( hql )
            .setParameter( "dataElement", dataElement ).executeUpdate();
    }

    @Override
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo )
    {
        return getDataValue( dataElement, period, source, categoryOptionCombo, attributeOptionCombo, false );
    }

    @Override
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo, boolean includeDeleted )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        String includeDeletedSql = includeDeleted ? "" : "and dv.deleted = false ";

        String hql = "select dv from DataValue dv  where dv.dataElement =:dataElement and dv.period =:period "
            + includeDeletedSql
            + "and dv.attributeOptionCombo =:attributeOptionCombo and dv.categoryOptionCombo =:categoryOptionCombo and dv.source =:source ";

        return getSingleResult( getQuery( hql )
            .setParameter( "dataElement", dataElement )
            .setParameter( "period", storedPeriod )
            .setParameter( "source", source )
            .setParameter( "attributeOptionCombo", attributeOptionCombo )
            .setParameter( "categoryOptionCombo", categoryOptionCombo ) );
    }

    @Override
    public DataValue getSoftDeletedDataValue( DataValue dataValue )
    {
        Period storedPeriod = periodStore.reloadPeriod( dataValue.getPeriod() );

        if ( storedPeriod == null )
        {
            return null;
        }

        dataValue.setPeriod( storedPeriod );

        CriteriaBuilder builder = getCriteriaBuilder();

        return getSingleResult( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root, dataValue ) )
            .addPredicate( root -> builder.equal( root.get( DELETED ), true ) ) );
    }

    @Override
    public List<DataValue> getAllDataValues()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( DELETED ), false ) ) );
    }

    @Override
    public int getDataValueCountLastUpdatedBetween( Date startDate, Date endDate, boolean includeDeleted )
    {
        if ( startDate == null && endDate == null )
        {
            throw new IllegalArgumentException( "Start date or end date must be specified" );
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        List<Function<Root<DataValue>, Predicate>> predicateList = new ArrayList<>();

        if ( !includeDeleted )
        {
            predicateList.add( root -> builder.equal( root.get( DELETED ), false ) );
        }

        if ( startDate != null )
        {
            predicateList.add( root -> builder.greaterThanOrEqualTo( root.get( LAST_UPATED ), startDate ) );
        }

        if ( endDate != null )
        {
            predicateList.add( root -> builder.lessThanOrEqualTo( root.get( LAST_UPATED ), endDate ) );
        }

        return getCount( builder, newJpaParameters()
            .addPredicates( predicateList )
            .count( builder::countDistinct ) )
                .intValue();
    }

    @Override
    public boolean dataValueExists( CategoryCombo combo )
    {
        String cocIdsSql = "select distinct categoryoptioncomboid from categorycombos_optioncombos where categorycomboid = :cc";
        List<?> cocIds = getSession().createNativeQuery( cocIdsSql )
            .setParameter( "cc", combo.getId() )
            .list();
        String anyDataValueSql = "select 1 from datavalue dv "
            + "where dv.categoryoptioncomboid in :cocIds or dv.attributeoptioncomboid in :cocIds limit 1";
        return !getSession().createNativeQuery( anyDataValueSql )
            .setParameter( "cocIds", cocIds )
            .list().isEmpty();
    }

    // -------------------------------------------------------------------------
    // getDataValues and related supportive methods
    // -------------------------------------------------------------------------

    @Override
    public List<DataValue> getDataValues( DataExportParams params )
    {
        Set<Period> periods = reloadAndFilterPeriods( params.getPeriods() );
        Set<OrganisationUnit> organisationUnits = params.getAllOrganisationUnits();

        // Return empty list if parameters include periods but none exist

        if ( params.hasPeriods() && periods.isEmpty() )
        {
            return new ArrayList<>();
        }

        String hql = getDataValuesHql( params, periods, organisationUnits );

        Query<DataValue> query = getQuery( hql );

        getDataValuesQueryParameters( params, query, periods, organisationUnits );

        // TODO last updated duration support

        return query.list();
    }

    /**
     * Reloads the periods in the given collection, and filters out periods
     * which do not exist in the database.
     */
    private Set<Period> reloadAndFilterPeriods( Collection<Period> periods )
    {
        return periods != null ? periods.stream()
            .map( periodStore::reloadPeriod )
            .filter( Objects::nonNull )
            .collect( toSet() ) : new HashSet<>();
    }

    /**
     * Gets HQL for getDataValues.
     */
    private String getDataValuesHql( DataExportParams params, Set<Period> periods,
        Set<OrganisationUnit> organisationUnits )
    {
        StringBuilder hql = new StringBuilder( "select dv from DataValue dv " +
            "inner join dv.dataElement de " +
            "inner join dv.period pe " +
            "inner join dv.source ou " +
            "inner join dv.categoryOptionCombo co " +
            "inner join dv.attributeOptionCombo ao " +
            "where de.id in (:dataElements) " );

        if ( !periods.isEmpty() )
        {
            hql.append( "and pe.id in (:periods) " );
        }
        else if ( params.hasStartEndDate() )
        {
            hql.append( "and (pe.startDate >= :startDate and pe.endDate <= :endDate) " );
        }

        if ( params.isIncludeDescendantsForOrganisationUnits() )
        {
            hql.append( "and (" );

            hql.append( params.getOrganisationUnits().stream()
                .map( OrganisationUnit::getPath )
                .map( p -> "ou.path like '" + p + "%'" )
                .collect( joining( " or " ) ) );

            hql.append( ") " );
        }
        else if ( !organisationUnits.isEmpty() )
        {
            hql.append( "and ou.id in (:orgUnits) " );
        }

        if ( params.hasCategoryOptionCombos() )
        {
            hql.append( "and co.id in (:categoryOptionCombos) " );
        }

        if ( params.hasAttributeOptionCombos() )
        {
            hql.append( "and ao.id in (:attributeOptionCombos) " );
        }

        if ( params.hasLastUpdated() || params.hasLastUpdatedDuration() )
        {
            hql.append( "and dv.lastUpdated >= :lastUpdated " );
        }

        if ( !params.isIncludeDeleted() )
        {
            hql.append( "and dv.deleted is false " );
        }

        if ( params.isOrderByOrgUnitPath() )
        {
            hql.append( "order by ou.path " );
        }

        if ( params.isOrderByPeriod() )
        {
            hql.append( params.isOrderByOrgUnitPath()
                ? ","
                : "order by" )
                .append( " pe.startDate, pe.endDate " );
        }

        return hql.toString();
    }

    /**
     * Sets Query parameters for getDataValues.
     */
    private void getDataValuesQueryParameters( DataExportParams params, Query<DataValue> query, Set<Period> periods,
        Set<OrganisationUnit> organisationUnits )
    {
        query.setParameterList( "dataElements", getIdentifiers( params.getAllDataElements() ) );

        if ( !periods.isEmpty() )
        {
            query.setParameterList( "periods", getIdentifiers( periods ) );
        }
        else if ( params.hasStartEndDate() )
        {
            query.setParameter( "startDate", params.getStartDate() ).setParameter( "endDate", params.getEndDate() );
        }

        if ( !params.isIncludeDescendantsForOrganisationUnits() && !organisationUnits.isEmpty() )
        {
            query.setParameterList( "orgUnits", getIdentifiers( organisationUnits ) );
        }

        if ( params.hasCategoryOptionCombos() )
        {
            query.setParameterList( "categoryOptionCombos", getIdentifiers( params.getCategoryOptionCombos() ) );
        }

        if ( params.hasAttributeOptionCombos() )
        {
            query.setParameterList( "attributeOptionCombos", getIdentifiers( params.getAttributeOptionCombos() ) );
        }

        if ( params.hasLastUpdated() )
        {
            query.setParameter( LAST_UPATED, params.getLastUpdated() );
        }
        else if ( params.hasLastUpdatedDuration() )
        {
            query.setParameter( LAST_UPATED, DateUtils.nowMinusDuration( params.getLastUpdatedDuration() ) );
        }

        if ( params.hasLimit() )
        {
            query.setMaxResults( params.getLimit() );
        }
    }

    // -------------------------------------------------------------------------
    // getDeflatedDataValues and related supportive methods
    // -------------------------------------------------------------------------

    @Override
    public List<DeflatedDataValue> getDeflatedDataValues( DataExportParams params )
    {
        SqlHelper sqlHelper = new SqlHelper( true );

        StringBuilder sql = new StringBuilder();
        StringBuilder where = new StringBuilder();

        getDdvSelectFrom( params, sql );
        getDdvDataElementsAndOperands( params, sql, where, sqlHelper );
        getDdvPeriods( params, sql, where, sqlHelper );
        getDdvOrgUnits( params, sql, where, sqlHelper );
        getDdvAttributeOptionCombos( params, where, sqlHelper );
        getDdvDimensionConstraints( params, sql, where, sqlHelper );
        getDdvLastUpdated( params, where, sqlHelper );
        getDdvIncludeDeleted( params, where, sqlHelper );

        sql.append( where );

        getDdvOrderBy( params, sql );

        List<DeflatedDataValue> result = new ArrayList<>();

        jdbcTemplate.query( sql.toString(), resultSet -> {
            DeflatedDataValue ddv = getDdvFromResultSet( resultSet, params.needsOrgUnitDetails() );
            if ( params.hasBlockingQueue() )
            {
                getDdvAddToBlockingQueue( params.getBlockingQueue(), ddv );
            }
            else
            {
                result.add( ddv );
            }
        } );

        if ( params.hasBlockingQueue() )
        {
            getDdvAddToBlockingQueue( params.getBlockingQueue(), END_OF_DDV_DATA );
        }

        log.debug( result.size() + " DeflatedDataValues returned from: " + sql );

        return result;
    }

    /**
     * getDeflatedDataValues - Adds SELECT clause and starts FROM clause.
     */
    private void getDdvSelectFrom( DataExportParams params, StringBuilder sql )
    {
        sql.append( "select dv.dataelementid, dv.periodid, dv.sourceid" +
            ", dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value" +
            ", dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup, dv.deleted" )
            .append( params.needsOrgUnitDetails() ? ", ou.path" : "" )
            .append( " from datavalue dv" );
    }

    /**
     * getDeflatedDataValues - Chooses data elements and data element operands.
     */
    private void getDdvDataElementsAndOperands( DataExportParams params, StringBuilder sql, StringBuilder where,
        SqlHelper sqlHelper )
    {
        List<Long> deIds = new ArrayList<>();
        List<Long> cocIds = new ArrayList<>();
        getDdvDataElementLists( params, deIds, cocIds );

        if ( !cocIds.isEmpty() )
        {
            sql.append( " join " )
                .append( statementBuilder.literalLongLongTable( deIds, cocIds, "deo", "deid", "cocid" ) )
                .append(
                    " on deo.deid = dv.dataelementid and (deo.cocid is null or deo.cocid::bigint = dv.categoryoptioncomboid)" );
        }
        else if ( !deIds.isEmpty() )
        {
            String dataElementIdList = getCommaDelimitedString( deIds );

            where.append( sqlHelper.whereAnd() )
                .append( "dv.dataelementid in (" ).append( dataElementIdList ).append( ")" );
        }
    }

    /**
     * getDeflatedDataValues - Chooses periods.
     */
    private void getDdvPeriods( DataExportParams params, StringBuilder sql, StringBuilder where,
        SqlHelper sqlHelper )
    {
        if ( params.hasPeriods() )
        {
            String periodIdList = getCommaDelimitedString( getIdentifiers( params.getPeriods() ) );

            where.append( sqlHelper.whereAnd() )
                .append( "dv.periodid in (" ).append( periodIdList ).append( ")" );
        }
        else if ( params.hasPeriodTypes() || params.hasStartEndDate() || params.hasIncludedDate() )
        {
            sql.append( " join period p on p.periodid = dv.periodid" );

            if ( params.hasPeriodTypes() )
            {
                sql.append( " join periodtype pt on pt.periodtypeid = p.periodtypeid" );

                String periodTypeIdList = getCommaDelimitedString(
                    params.getPeriodTypes().stream().map( PeriodType::getId ).collect( toList() ) );

                where.append( sqlHelper.whereAnd() )
                    .append( "pt.periodtypeid in (" ).append( periodTypeIdList ).append( ")" );
            }

            if ( params.hasStartEndDate() )
            {
                where.append( sqlHelper.whereAnd() )
                    .append( "p.startdate >= '" ).append( DateUtils.getMediumDateString( params.getStartDate() ) )
                    .append( "'" ).append( " and p.enddate <= '" )
                    .append( DateUtils.getMediumDateString( params.getStartDate() ) ).append( "'" );
            }
            else if ( params.hasIncludedDate() )
            {
                where.append( sqlHelper.whereAnd() )
                    .append( "p.startdate <= '" ).append( DateUtils.getMediumDateString( params.getIncludedDate() ) )
                    .append( "'" ).append( " and p.enddate >= '" )
                    .append( DateUtils.getMediumDateString( params.getIncludedDate() ) ).append( "'" );
            }
        }
    }

    /**
     * getDeflatedDataValues - Chooses organisation units.
     */
    private void getDdvOrgUnits( DataExportParams params, StringBuilder sql, StringBuilder where,
        SqlHelper sqlHelper )
    {
        if ( params.needsOrgUnitDetails() )
        {
            sql.append( " join organisationunit ou on ou.organisationunitid = dv.sourceid" );
        }

        if ( params.hasOrgUnitLevel() )
        {
            where.append( sqlHelper.whereAnd() ).append( "ou.hierarchylevel " )
                .append( params.isIncludeDescendants() ? ">" : "" ).append( "= " )
                .append( params.getOrgUnitLevel() );
        }

        if ( params.hasOrganisationUnits() )
        {
            if ( params.getOuMode() == DESCENDANTS )
            {
                where.append( sqlHelper.whereAnd() ).append( "(" );

                for ( OrganisationUnit parent : params.getOrganisationUnits() )
                {
                    where.append( sqlHelper.or() ).append( "ou.path like '" ).append( parent.getPath() ).append( "%'" );
                }

                where.append( " )" );
            }
            else
            {
                String orgUnitIdList = getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) );

                where.append( sqlHelper.whereAnd() )
                    .append( "dv.sourceid in (" ).append( orgUnitIdList ).append( ")" );
            }
        }
    }

    /**
     * getDeflatedDataValues - Chooses attribute option combinations.
     */
    private void getDdvAttributeOptionCombos( DataExportParams params, StringBuilder where, SqlHelper sqlHelper )
    {
        if ( params.hasAttributeOptionCombos() )
        {
            String aocIdList = getCommaDelimitedString( getIdentifiers( params.getAttributeOptionCombos() ) );

            where.append( sqlHelper.whereAnd() )
                .append( "dv.attributeoptioncomboid in (" ).append( aocIdList ).append( ")" );
        }
    }

    /**
     * getDeflatedDataValues - Adds user dimension constraints.
     */
    private void getDdvDimensionConstraints( DataExportParams params, StringBuilder sql, StringBuilder where,
        SqlHelper sqlHelper )
    {
        if ( params.hasCogDimensionConstraints() || params.hasCoDimensionConstraints() )
        {
            sql.append(
                " join categoryoptioncombos_categoryoptions cc on dv.attributeoptioncomboid = cc.categoryoptioncomboid" );

            if ( params.hasCoDimensionConstraints() )
            {
                String coDimConstraintsList = getCommaDelimitedString(
                    getIdentifiers( params.getCoDimensionConstraints() ) );

                where.append( sqlHelper.whereAnd() )
                    .append( "cc.categoryoptionid in (" ).append( coDimConstraintsList ).append( ") " );
            }

            if ( params.hasCogDimensionConstraints() )
            {
                String cogDimConstraintsList = getCommaDelimitedString(
                    getIdentifiers( params.getCogDimensionConstraints() ) );

                sql.append( " join categoryoptiongroupmembers cogm on cc.categoryoptionid = cogm.categoryoptionid" );

                where.append( sqlHelper.whereAnd() )
                    .append( "cogm.categoryoptiongroupid in (" ).append( cogDimConstraintsList ).append( ")" );
            }
        }
    }

    /**
     * getDeflatedDataValues - Adds LastUpdated constraint.
     */
    private void getDdvLastUpdated( DataExportParams params, StringBuilder where, SqlHelper sqlHelper )
    {
        if ( params.hasLastUpdated() )
        {
            where.append( sqlHelper.whereAnd() )
                .append( "dv.lastupdated >= " ).append( DateUtils.getMediumDateString( params.getLastUpdated() ) );
        }
    }

    /**
     * getDeflatedDataValues - Adds deleted constraint.
     */
    private void getDdvIncludeDeleted( DataExportParams params, StringBuilder where, SqlHelper sqlHelper )
    {
        if ( !params.isIncludeDeleted() )
        {
            where.append( sqlHelper.whereAnd() ).append( "dv.deleted is false" );
        }
    }

    /**
     * getDeflatedDataValues - Adds ORDER BY.
     */
    private void getDdvOrderBy( DataExportParams params, StringBuilder sql )
    {
        if ( params.isOrderByOrgUnitPath() )
        {
            sql.append( " order by ou.path" );
        }
    }

    /**
     * getDeflatedDataValues - Gets data element / category option combo lists.
     * <p>
     * There are two ways that all the category option combos of a data element
     * may be requested: either as a data element (returns the sum of all COCs)
     * or as a wildcard data element operand (having COC == null, returns each
     * individual data element operand.)
     * <p>
     * If the parameters have any non-wildcard data element operands, then this
     * method fills the lists of data element ids and COC ids with equal numbers
     * of values. For a non-wildcard data element operand, the COC will be not
     * null. For a data element or a wildcard DEO, the COC will be null.
     * <p>
     * If there are no non-wildcard data element operands, then only the list of
     * data element ids is populated.
     */
    private void getDdvDataElementLists( DataExportParams params, List<Long> deIds, List<Long> cocIds )
    {
        // Get a collection of unique DataElement ids.
        Collection<Long> dataElementIds = union(
            params.getDataElements().stream()
                .map( DataElement::getId )
                .collect( toSet() ),
            params.getDataElementOperands().stream()
                .filter( deo -> deo.getCategoryOptionCombo() == null )
                .map( deo -> deo.getDataElement().getId() )
                .collect( toSet() ) );

        deIds.addAll( dataElementIds );

        // Get a set of unique DataElement/CategoryOptionCombo id pairs.
        Set<DeflatedDataValue> dataElementOperands = params.getDataElementOperands().stream()
            .filter( deo -> !dataElementIds.contains( deo.getDataElement().getId() ) )
            .filter( deo -> deo.getCategoryOptionCombo() != null )
            .map( deo -> new DeflatedDataValue( deo.getDataElement().getId(), deo.getCategoryOptionCombo().getId() ) )
            .collect( toSet() );

        if ( !dataElementOperands.isEmpty() )
        {
            cocIds.addAll( Collections.nCopies( deIds.size(), null ) );

            for ( DeflatedDataValue ddv : dataElementOperands )
            {
                deIds.add( ddv.getDataElementId() );
                cocIds.add( ddv.getCategoryOptionComboId() );
            }
        }
    }

    /**
     * getDeflatedDataValues - Creates a {@link DeflatedDataValue} from a query
     * result row.
     */
    private DeflatedDataValue getDdvFromResultSet( ResultSet resultSet, boolean joinOrgUnit )
        throws SQLException
    {
        Long dataElementId = resultSet.getLong( 1 );
        Long periodId = resultSet.getLong( 2 );
        Long organisationUnitId = resultSet.getLong( 3 );
        Long categoryOptionComboId = resultSet.getLong( 4 );
        Long attributeOptionComboId = resultSet.getLong( 5 );
        String value = resultSet.getString( 6 );
        String storedBy = resultSet.getString( 7 );
        Date created = resultSet.getDate( 8 );
        Date lastUpdated = resultSet.getDate( 9 );
        String comment = resultSet.getString( 10 );
        boolean followup = resultSet.getBoolean( 11 );
        boolean deleted = resultSet.getBoolean( 12 );
        String sourcePath = joinOrgUnit ? resultSet.getString( 13 ) : null;

        DeflatedDataValue ddv = new DeflatedDataValue( dataElementId, periodId,
            organisationUnitId, categoryOptionComboId, attributeOptionComboId,
            value, storedBy, created, lastUpdated, comment, followup, deleted );

        ddv.setSourcePath( sourcePath );

        return ddv;
    }

    /**
     * getDeflatedDataValues - Adds {@link DeflatedDataValue} to blocking queue.
     */
    private void getDdvAddToBlockingQueue( BlockingQueue<DeflatedDataValue> blockingQueue, DeflatedDataValue ddv )
    {
        try
        {
            if ( !blockingQueue.offer( ddv, DDV_QUEUE_TIMEOUT_VALUE, DDV_QUEUE_TIMEOUT_UNIT ) )
            {
                log.error( "HibernateDataValueStore failed to add to BlockingQueue." );
            }
        }
        catch ( InterruptedException ex )
        {
            log.error( "HibernateDataValueStore BlockingQueue InterruptedException: " + ex.getMessage() );
            Thread.currentThread().interrupt();
        }
    }
}
