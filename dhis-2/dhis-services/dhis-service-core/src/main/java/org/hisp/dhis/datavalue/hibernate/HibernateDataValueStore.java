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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
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

    private PeriodStore periodStore;

    private StatementBuilder statementBuilder;

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
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        String hql = "select dv from DataValue dv  where dv.dataElement =:dataElement and dv.period =:period and dv.deleted = false  "
            +
            "and dv.attributeOptionCombo =:attributeOptionCombo and dv.categoryOptionCombo =:categoryOptionCombo and dv.source =:source ";

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
            .addPredicate( root -> builder.equal( root.get( "deleted" ), true ) ) );
    }

    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    @Override
    public List<DataValue> getDataValues( DataExportParams params )
    {
        Set<DataElement> dataElements = params.getAllDataElements();
        Set<Period> periods = reloadAndFilterPeriods( params.getPeriods() );
        Set<OrganisationUnit> organisationUnits = params.getAllOrganisationUnits();

        // Return empty list if parameters include periods but none exist

        if ( params.hasPeriods() && periods.isEmpty() )
        {
            return new ArrayList<>();
        }

        // ---------------------------------------------------------------------
        // HQL parameters
        // ---------------------------------------------------------------------

        String hql = "select dv from DataValue dv " +
            "inner join dv.dataElement de " +
            "inner join dv.period pe " +
            "inner join dv.source ou " +
            "inner join dv.categoryOptionCombo co " +
            "inner join dv.attributeOptionCombo ao " +
            "where de.id in (:dataElements) ";

        if ( !periods.isEmpty() )
        {
            hql += "and pe.id in (:periods) ";
        }
        else if ( params.hasStartEndDate() )
        {
            hql += "and (pe.startDate >= :startDate and pe.endDate <= :endDate) ";
        }

        if ( params.isIncludeDescendantsForOrganisationUnits() )
        {
            hql += "and (";

            for ( OrganisationUnit unit : params.getOrganisationUnits() )
            {
                hql += "ou.path like '" + unit.getPath() + "%' or ";
            }

            hql = removeLastOr( hql );

            hql += ") ";
        }
        else if ( !organisationUnits.isEmpty() )
        {
            hql += "and ou.id in (:orgUnits) ";
        }

        if ( params.hasCategoryOptionCombos() )
        {
            hql += "and co.id in (:categoryOptionCombos) ";
        }

        if ( params.hasAttributeOptionCombos() )
        {
            hql += "and ao.id in (:attributeOptionCombos) ";
        }

        if ( params.hasLastUpdated() || params.hasLastUpdatedDuration() )
        {
            hql += "and dv.lastUpdated >= :lastUpdated ";
        }

        if ( !params.isIncludeDeleted() )
        {
            hql += "and dv.deleted is false ";
        }

        if ( params.isOrderByOrgUnitPath() )
        {
            hql += "order by ou.path ";
        }

        if ( params.isOrderByPeriod() )
        {
            hql += params.isOrderByOrgUnitPath()
                ? ","
                : "order by";
            hql += " pe.startDate, pe.endDate ";
        }

        // ---------------------------------------------------------------------
        // Query parameters
        // ---------------------------------------------------------------------

        Query<DataValue> query = getQuery( hql )
            .setParameterList( "dataElements", getIdentifiers( dataElements ) );

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
            query.setParameter( "lastUpdated", params.getLastUpdated() );
        }
        else if ( params.hasLastUpdatedDuration() )
        {
            query.setParameter( "lastUpdated", DateUtils.nowMinusDuration( params.getLastUpdatedDuration() ) );
        }

        if ( params.hasLimit() )
        {
            query.setMaxResults( params.getLimit() );
        }

        // TODO last updated duration support

        return query.list();
    }

    @Override
    public List<DataValue> getAllDataValues()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "deleted" ), false ) ) );
    }

    @Override
    public List<DeflatedDataValue> getDeflatedDataValues( DataExportParams params )
    {
        SqlHelper sqlHelper = new SqlHelper( true );

        boolean joinOrgUnit = params.isOrderByOrgUnitPath()
            || params.hasOrgUnitLevel()
            || params.getOuMode() == DESCENDANTS
            || params.isIncludeDescendants();

        String sql = "select dv.dataelementid, dv.periodid, dv.sourceid" +
            ", dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value" +
            ", dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup, dv.deleted" +
            (joinOrgUnit ? ", ou.path" : "") +
            " from datavalue dv";

        String where = "";

        List<DataElementOperand> queryDeos = getQueryDataElementOperands( params );

        if ( queryDeos != null )
        {
            List<Long> deIdList = queryDeos.stream().map( de -> de.getDataElement().getId() )
                .collect( Collectors.toList() );
            List<Long> cocIdList = queryDeos.stream()
                .map( de -> de.getCategoryOptionCombo() == null ? null : de.getCategoryOptionCombo().getId() )
                .collect( Collectors.toList() );

            sql += " join " + statementBuilder.literalLongLongTable( deIdList, cocIdList, "deo", "deid", "cocid" )
                + " on deo.deid = dv.dataelementid and (deo.cocid is null or deo.cocid::bigint = dv.categoryoptioncomboid)";
        }
        else if ( params.hasDataElements() )
        {
            String dataElementIdList = getCommaDelimitedString( getIdentifiers( params.getDataElements() ) );

            where += sqlHelper.whereAnd() + "dv.dataelementid in (" + dataElementIdList + ")";
        }

        if ( params.hasPeriods() )
        {
            String periodIdList = getCommaDelimitedString( getIdentifiers( params.getPeriods() ) );

            where += sqlHelper.whereAnd() + "dv.periodid in (" + periodIdList + ")";
        }
        else if ( params.hasPeriodTypes() || params.hasStartEndDate() || params.hasIncludedDate() )
        {
            sql += " join period p on p.periodid = dv.periodid";

            if ( params.hasPeriodTypes() )
            {
                sql += " join periodtype pt on pt.periodtypeid = p.periodtypeid";

                String periodTypeIdList = getCommaDelimitedString(
                    params.getPeriodTypes().stream().map( o -> o.getId() ).collect( Collectors.toList() ) );

                where += sqlHelper.whereAnd() + "pt.periodtypeid in (" + periodTypeIdList + ")";
            }

            if ( params.hasStartEndDate() )
            {
                where += sqlHelper.whereAnd() + "p.startdate >= '"
                    + DateUtils.getMediumDateString( params.getStartDate() ) + "'"
                    + " and p.enddate <= '" + DateUtils.getMediumDateString( params.getStartDate() ) + "'";
            }
            else if ( params.hasIncludedDate() )
            {
                where += sqlHelper.whereAnd() + "p.startdate <= '"
                    + DateUtils.getMediumDateString( params.getIncludedDate() ) + "'"
                    + " and p.enddate >= '" + DateUtils.getMediumDateString( params.getIncludedDate() ) + "'";
            }
        }

        if ( joinOrgUnit )
        {
            sql += " join organisationunit ou on ou.organisationunitid = dv.sourceid";
        }

        if ( params.hasOrgUnitLevel() )
        {
            where += sqlHelper.whereAnd() + "ou.hierarchylevel " +
                (params.isIncludeDescendants() ? ">" : "") +
                "= " + params.getOrgUnitLevel();
        }

        if ( params.hasOrganisationUnits() )
        {
            if ( params.getOuMode() == DESCENDANTS )
            {
                where += sqlHelper.whereAnd() + "(";

                for ( OrganisationUnit parent : params.getOrganisationUnits() )
                {
                    where += sqlHelper.or() + "ou.path like '" + parent.getPath() + "%'";
                }

                where += " )";
            }
            else
            {
                String orgUnitIdList = getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) );

                where += sqlHelper.whereAnd() + "dv.sourceid in (" + orgUnitIdList + ")";
            }
        }

        if ( params.hasAttributeOptionCombos() )
        {
            String aocIdList = getCommaDelimitedString( getIdentifiers( params.getAttributeOptionCombos() ) );

            where += sqlHelper.whereAnd() + "dv.attributeoptioncomboid in (" + aocIdList + ")";
        }

        if ( params.hasCogDimensionConstraints() || params.hasCoDimensionConstraints() )
        {
            sql += " join categoryoptioncombos_categoryoptions cc on dv.attributeoptioncomboid = cc.categoryoptioncomboid";

            if ( params.hasCoDimensionConstraints() )
            {
                String coDimConstraintsList = getCommaDelimitedString(
                    getIdentifiers( params.getCoDimensionConstraints() ) );

                where += sqlHelper.whereAnd() + "cc.categoryoptionid in (" + coDimConstraintsList + ") ";
            }

            if ( params.hasCogDimensionConstraints() )
            {
                String cogDimConstraintsList = getCommaDelimitedString(
                    getIdentifiers( params.getCogDimensionConstraints() ) );

                sql += " join categoryoptiongroupmembers cogm on cc.categoryoptionid = cogm.categoryoptionid";

                where += sqlHelper.whereAnd() + "cogm.categoryoptiongroupid in (" + cogDimConstraintsList + ")";
            }
        }

        if ( params.hasLastUpdated() )
        {
            where += sqlHelper.whereAnd() + "dv.lastupdated >= "
                + DateUtils.getMediumDateString( params.getLastUpdated() );
        }

        if ( !params.isIncludeDeleted() )
        {
            where += sqlHelper.whereAnd() + "dv.deleted is false";
        }

        sql += where;

        if ( params.isOrderByOrgUnitPath() )
        {
            sql += " order by ou.path";
        }

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        List<DeflatedDataValue> result = new ArrayList<>();

        while ( rowSet.next() )
        {
            Integer dataElementId = rowSet.getInt( 1 );
            Integer periodId = rowSet.getInt( 2 );
            Integer organisationUnitId = rowSet.getInt( 3 );
            Integer categoryOptionComboId = rowSet.getInt( 4 );
            Integer attributeOptionComboId = rowSet.getInt( 5 );
            String value = rowSet.getString( 6 );
            String storedBy = rowSet.getString( 7 );
            Date created = rowSet.getDate( 8 );
            Date lastUpdated = rowSet.getDate( 9 );
            String comment = rowSet.getString( 10 );
            boolean followup = rowSet.getBoolean( 11 );
            boolean deleted = rowSet.getBoolean( 12 );
            String sourcePath = joinOrgUnit ? rowSet.getString( 13 ) : null;

            DeflatedDataValue ddv = new DeflatedDataValue( dataElementId, periodId,
                organisationUnitId, categoryOptionComboId, attributeOptionComboId,
                value, storedBy, created, lastUpdated, comment, followup, deleted );

            ddv.setSourcePath( sourcePath );

            if ( params.hasBlockingQueue() )
            {
                if ( !addToBlockingQueue( params.getBlockingQueue(), ddv ) )
                {
                    return result; // Abort
                }
            }
            else
            {
                result.add( ddv );
            }
        }

        if ( params.hasBlockingQueue() )
        {
            addToBlockingQueue( params.getBlockingQueue(), END_OF_DDV_DATA );
        }

        log.debug( result.size() + " DeflatedDataValues returned from: " + sql );

        return result;
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
            predicateList.add( root -> builder.equal( root.get( "deleted" ), false ) );
        }

        if ( startDate != null )
        {
            predicateList.add( root -> builder.greaterThanOrEqualTo( root.get( "lastUpdated" ), startDate ) );
        }

        if ( endDate != null )
        {
            predicateList.add( root -> builder.lessThanOrEqualTo( root.get( "lastUpdated" ), endDate ) );
        }

        return getCount( builder, newJpaParameters()
            .addPredicates( predicateList )
            .count( root -> builder.countDistinct( root ) ) )
                .intValue();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Reloads the periods in the given collection, and filters out periods
     * which do not exist in the database.
     *
     * @param periods the collection of {@link Period}.
     * @return a set of reloaded {@link Period}.
     */
    private Set<Period> reloadAndFilterPeriods( Collection<Period> periods )
    {
        return periods != null ? periods.stream()
            .map( p -> periodStore.reloadPeriod( p ) )
            .filter( Objects::nonNull )
            .collect( Collectors.toSet() ) : new HashSet<>();
    }

    /**
     * Gets a list of DataElementOperands to use for SQL query.
     * <p>
     * If there are no DataElementOperands ("DEOs") parameters, or if the
     * DataElement in each DEO is also present as a DataElement parameter, then
     * return null. In this case the SQL query need only fetch all datavalues
     * where the DataElement is in the DataElement list.
     * <p>
     * However if there are some DEOs parameters with DataElements that are not
     * also DataElements parameters, then return a list of DataElementOperands
     * where the CategoryOptionCombo is specified for a specific DEO parameter,
     * or is null (implying here a wildcard) for any specified DataElement
     * parameter. This list will be used to form a selection for all
     * DataElementOperands and DataElements together.
     *
     * @param params the data export parameters
     * @return data element operand list (if any) for the SQL query
     */
    private List<DataElementOperand> getQueryDataElementOperands( DataExportParams params )
    {
        List<DataElementOperand> deos = params.getDataElementOperands().stream()
            .filter( deo -> !params.getDataElements().contains( deo.getDataElement() ) )
            .collect( Collectors.toList() );

        if ( deos.isEmpty() )
        {
            return null;
        }

        for ( DataElement de : params.getDataElements() )
        {
            deos.add( new DataElementOperand( de, null ) );
        }

        return deos;
    }

    /**
     * Adds a {@see DeflatedDataValue} to a blocking queue
     *
     * @param blockingQueue the queue to add to
     * @param ddv the deflated data value
     * @return true if it was added, false if timeout
     */
    private boolean addToBlockingQueue( BlockingQueue<DeflatedDataValue> blockingQueue, DeflatedDataValue ddv )
    {
        try
        {
            return blockingQueue.offer( ddv, DDV_QUEUE_TIMEOUT_VALUE, DDV_QUEUE_TIMEOUT_UNIT );
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();

            return false;
        }
    }
}
