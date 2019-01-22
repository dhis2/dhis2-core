package org.hisp.dhis.datavalue.hibernate;

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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.api.util.DateUtils;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.collect.Sets;

/**
 * @author Torgeir Lorange Ostby
 */
public class HibernateDataValueStore extends HibernateGenericStore<DataValue>
    implements DataValueStore
{
    private static final Log log = LogFactory.getLog( HibernateDataValueStore.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PeriodStore periodStore;

    public void setPeriodStore( PeriodStore periodStore )
    {
        this.periodStore = periodStore;
    }

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
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

        getSession().createQuery( hql ).
            setParameter( "source", organisationUnit ).executeUpdate();
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

        String hql = "select dv from DataValue dv  where dv.dataElement =:dataElement and dv.period =:period and dv.deleted = false  " +
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
        Set<OrganisationUnit> organisationUnits = params.getAllOrganisationUnits();

        // ---------------------------------------------------------------------
        // HQL parameters
        // ---------------------------------------------------------------------

        String hql = 
            "select dv from DataValue dv " +
            "inner join dv.dataElement de " +
            "inner join dv.period pe " +
            "inner join dv.source ou " +
            "inner join dv.categoryOptionCombo co " +
            "inner join dv.attributeOptionCombo ao " +
            "where de.id in (:dataElements) ";

        if ( params.hasPeriods() )
        {
            hql += "and pe.id in (:periods) ";
        }
        else if ( params.hasStartEndDate() )
        {
            hql += "and (pe.startDate >= :startDate and pe.endDate < :endDate) ";
        }
        
        if ( params.isIncludeChildrenForOrganisationUnits() )
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
        
        if ( params.hasAttributeOptionCombos() )
        {
            hql += "and ao.id in (:attributeOptionCombos) ";
        }
        
        if ( params.hasLastUpdated() )
        {
            hql += "and dv.lastUpdated >= :lastUpdated ";
        }

        if ( !params.isIncludeDeleted() )
        {
            hql += "and dv.deleted is false ";
        }

        // ---------------------------------------------------------------------
        // Query parameters
        // ---------------------------------------------------------------------

        Query<DataValue> query = getSession()
            .createQuery( hql )
            .setParameterList( "dataElements", getIdentifiers( dataElements ) );

        if ( params.hasPeriods() )
        {
            Set<Period> periods = params.getPeriods().stream()
                .map( p -> periodStore.reloadPeriod( p ) )
                .collect( Collectors.toSet() );
            
            query.setParameterList( "periods", getIdentifiers( periods ) );
        }
        else if ( params.hasStartEndDate() )
        {
            query.setParameter( "startDate", params.getStartDate() ).setParameter( "endDate", params.getEndDate() );
        }

        if ( !params.isIncludeChildrenForOrganisationUnits() && !organisationUnits.isEmpty() )
        {
            query.setParameterList( "orgUnits", getIdentifiers( organisationUnits ) );
        }
        
        if ( params.hasAttributeOptionCombos() )
        {
            query.setParameterList( "attributeOptionCombos", getIdentifiers( params.getAttributeOptionCombos() ) );
        }
        
        if ( params.hasLastUpdated() )
        {
            query.setParameter( "lastUpdated", params.getLastUpdated() );
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
    public List<DataValue> getDataValues( OrganisationUnit source, Period period,
        Collection<DataElement> dataElements, CategoryOptionCombo attributeOptionCombo )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null || dataElements == null || dataElements.isEmpty() )
        {
            return new ArrayList<>();
        }

        String hql = "select dv from DataValue dv  where dv.dataElement in (:dataElements) and dv.period =:period and dv.deleted = false ";

        if ( source != null )
        {
            hql += " and dv.source =:source ";
        }

        if ( attributeOptionCombo != null )
        {
            hql += " and dv.attributeOptionCombo =:attributeOptionCombo ";
        }

        Query query = getQuery( hql )
            .setParameter( "dataElements", dataElements )
            .setParameter( "period", storedPeriod );

        if ( source != null )
        {
            query.setParameter( "source", source );
        }

        if ( attributeOptionCombo != null )
        {
            query.setParameter( "attributeOptionCombo", attributeOptionCombo );
        }

        return getList( query );
    }

    @Override
    public List<DeflatedDataValue> getDeflatedDataValues( DataExportParams params )
    {
        SqlHelper sqlHelper = new SqlHelper( true );

        String orgUnitId = params.isReturnParentForOrganisationUnits() ? "opath.id" : "dv.sourceid";

        String sql = "select dv.dataelementid, dv.periodid, " + orgUnitId +
            ", dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value" +
            ", dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup" +
            " from datavalue dv";

        String where = "";

        if ( params.hasDataElementOperands() )
        {
            List<DataElementOperand> queryDeos = getQueryDataElementOperands( params );
            List<Integer> deIdList = queryDeos.stream().map( de -> de.getDataElement().getId() ).collect( Collectors.toList() );
            List<Integer> cocIdList = queryDeos.stream()
                .map( de -> de.getCategoryOptionCombo() == null ? null : de.getCategoryOptionCombo().getId() )
                .collect( Collectors.toList() );

            sql += " join " + statementBuilder.literalIntIntTable( deIdList, cocIdList, "deo", "deid", "cocid" )
                + " on deo.deid = dv.dataelementid and (deo.cocid is null or deo.cocid = dv.categoryoptioncomboid)";
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

                String periodTypeIdList = getCommaDelimitedString( params.getPeriodTypes().stream().map( o -> o.getId() ).collect( Collectors.toList() ) );

                where += sqlHelper.whereAnd() + "pt.periodtypeid in (" + periodTypeIdList + ")";
            }

            if ( params.hasStartEndDate() )
            {
                where += sqlHelper.whereAnd() + "p.startdate >= '" + DateUtils.getMediumDateString( params.getStartDate() ) + "'"
                    + " and p.enddate <= '" + DateUtils.getMediumDateString( params.getStartDate() ) + "'";
            }
            else if ( params.hasIncludedDate() )
            {
                where += sqlHelper.whereAnd() + "p.startdate <= '" + DateUtils.getMediumDateString( params.getIncludedDate() ) + "'"
                    + " and p.enddate >= '" + DateUtils.getMediumDateString( params.getIncludedDate() ) + "'";
            }
        }

        if ( params.isIncludeChildrenForOrganisationUnits() || params.isReturnParentForOrganisationUnits() )
        {
            List<OrganisationUnit> orgUnitList = new ArrayList<>( params.getOrganisationUnits() );
            List<Integer> orgUnitIdList = orgUnitList.stream().map(  OrganisationUnit::getId ).collect( Collectors.toList() );
            List<String> orgUnitPathList = orgUnitList.stream().map(  OrganisationUnit::getPath ).collect( Collectors.toList() );

            sql += " join organisationunit ou on ou.organisationunitid = dv.sourceid"
                + " join " + statementBuilder.literalIntStringTable( orgUnitIdList, orgUnitPathList, "opath", "id", "path" )
                + " on ou.path like " + statementBuilder.concatenate( "opath.path", "'%'");
        }
        else if ( params.hasOrganisationUnits() )
        {
            String orgUnitIdList = getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) );

            where += sqlHelper.whereAnd() + "dv.sourceid in (" + orgUnitIdList + ")";
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
                String coDimConstraintsList = getCommaDelimitedString( getIdentifiers( params.getCoDimensionConstraints() ) );

                where += sqlHelper.whereAnd() + "cc.categoryoptionid in (" + coDimConstraintsList + ") ";
            }

            if ( params.hasCogDimensionConstraints() )
            {
                String cogDimConstraintsList = getCommaDelimitedString( getIdentifiers( params.getCogDimensionConstraints() ) );

                sql += " join categoryoptiongroupmembers cogm on cc.categoryoptionid = cogm.categoryoptionid";

                where += sqlHelper.whereAnd() + "cogm.categoryoptiongroupid in (" + cogDimConstraintsList + ")";
            }
        }

        if ( params.hasLastUpdated() )
        {
            where += sqlHelper.whereAnd() + "dv.lastupdated >= " + DateUtils.getMediumDateString( params.getLastUpdated() );
        }

        if ( !params.isIncludeDeleted() )
        {
            where += sqlHelper.whereAnd() + "dv.deleted is false";
        }

        sql += where;

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

            result.add( new DeflatedDataValue( dataElementId, periodId,
                organisationUnitId, categoryOptionComboId, attributeOptionComboId,
                value, storedBy, created, lastUpdated, comment, followup ) );
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
     * Gets a list of DataElementOperands to use for SQL query.
     *
     * If there are data elements to query, these are combined with the
     * data element operands (DEOs) into one list.
     *
     * If, in the resulting set of DEOs, there are DEOs for the same data
     * element both with and without non-null category option combos (COCs),
     * then the DEOs with non-null COCs are removed for that data element.
     * This is because the DEO with the null COC will already match all COCs
     * for that data element. We do not want to match them again, or the
     * same data value rows will be duplicated.
     *
     * @param params the data export parameters
     * @return data element operands to use for query
     */
    private List<DataElementOperand> getQueryDataElementOperands(  DataExportParams params )
    {
        Set<DataElementOperand> deos = params.getDataElementOperands();

        if ( params.hasDataElements() )
        {
            deos = Sets.union( deos, params.getDataElements().stream()
                .map( de -> new DataElementOperand( de ) ).collect( Collectors.toSet() ) );
        }

        Set<Integer> wildDataElementIds = deos.stream()
            .filter( deo -> deo.getCategoryOptionCombo() == null )
            .map( deo -> deo.getDataElement().getId() ).collect( Collectors.toSet() );

        return deos.stream()
            .filter( deo -> deo.getCategoryOptionCombo() == null || !wildDataElementIds.contains( deo.getDataElement().getId() ) )
            .collect( Collectors.toList() );
    }
}
