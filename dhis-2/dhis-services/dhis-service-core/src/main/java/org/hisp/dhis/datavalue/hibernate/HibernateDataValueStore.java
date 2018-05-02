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

import com.google.common.collect.Sets;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.*;

/**
 * @author Torgeir Lorange Ostby
 */
public class HibernateDataValueStore
    implements DataValueStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SessionFactory sessionFactory;

    public void setSessionFactory( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

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

        sessionFactory.getCurrentSession().save( dataValue );
    }

    @Override
    public void updateDataValue( DataValue dataValue )
    {
        dataValue.setPeriod( periodStore.reloadForceAddPeriod( dataValue.getPeriod() ) );

        sessionFactory.getCurrentSession().update( dataValue );
    }

    @Override
    public void deleteDataValues( OrganisationUnit organisationUnit )
    {
        String hql = "delete from DataValue d where d.source = :source";

        sessionFactory.getCurrentSession().createQuery( hql ).
            setEntity( "source", organisationUnit ).executeUpdate();
    }

    @Override
    public void deleteDataValues( DataElement dataElement )
    {
        String hql = "delete from DataValue d where d.dataElement = :dataElement";

        sessionFactory.getCurrentSession().createQuery( hql )
            .setEntity( "dataElement", dataElement ).executeUpdate();
    }

    @Override
    public DataValue getDataValue( DataElement dataElement, Period period, OrganisationUnit source,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo )
    {
        Session session = sessionFactory.getCurrentSession();

        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        return (DataValue) session.createCriteria( DataValue.class )
            .add( Restrictions.eq( "dataElement", dataElement ) )
            .add( Restrictions.eq( "period", storedPeriod ) )
            .add( Restrictions.eq( "source", source ) )
            .add( Restrictions.eq( "categoryOptionCombo", categoryOptionCombo ) )
            .add( Restrictions.eq( "attributeOptionCombo", attributeOptionCombo ) )
            .add( Restrictions.eq( "deleted", false ) )
            .uniqueResult();
    }

    @Override
    public DataValue getSoftDeletedDataValue( DataValue dataValue )
    {
        Session session = sessionFactory.getCurrentSession();

        Period storedPeriod = periodStore.reloadPeriod( dataValue.getPeriod() );

        if ( storedPeriod == null )
        {
            return null;
        }

        return (DataValue) session.createCriteria( DataValue.class )
            .add( Restrictions.eq( "dataElement", dataValue.getDataElement() ) )
            .add( Restrictions.eq( "period", storedPeriod ) )
            .add( Restrictions.eq( "source", dataValue.getSource() ) )
            .add( Restrictions.eq( "categoryOptionCombo", dataValue.getCategoryOptionCombo() ) )
            .add( Restrictions.eq( "attributeOptionCombo", dataValue.getAttributeOptionCombo() ) )
            .add( Restrictions.eq( "deleted", true ) )
            .uniqueResult();
    }
        
    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
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

        Query query = sessionFactory.getCurrentSession()
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
            query.setDate( "startDate", params.getStartDate() ).setDate( "endDate", params.getEndDate() );
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
            query.setDate( "lastUpdated", params.getLastUpdated() );
        }
        
        if ( params.hasLimit() )
        {
            query.setMaxResults( params.getLimit() );
        }
        
        // TODO last updated duration support

        return query.list();
    }
    
    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataValue> getAllDataValues()
    {
        return sessionFactory.getCurrentSession()
            .createCriteria( DataValue.class )
            .add( Restrictions.eq( "deleted", false ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataValue> getDataValues( OrganisationUnit source, Period period,
        Collection<DataElement> dataElements, CategoryOptionCombo attributeOptionCombo )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null || dataElements == null || dataElements.isEmpty() )
        {
            return new ArrayList<>();
        }

        Session session = sessionFactory.getCurrentSession();

        return session.createCriteria( DataValue.class )
            .add( Restrictions.in( "dataElement", dataElements ) )
            .add( Restrictions.eq( "period", storedPeriod ) )
            .add( Restrictions.eq( "source", source ) )
            .add( Restrictions.eq( "attributeOptionCombo", attributeOptionCombo ) )
            .add( Restrictions.eq( "deleted", false ) )
            .list();
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
            List<OrganisationUnit> orgUnitList = new ArrayList( params.getOrganisationUnits() );
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

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql + where );

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

        return result;
    }

    @Override
    public int getDataValueCountLastUpdatedBetween( Date startDate, Date endDate, boolean includeDeleted )
    {
        if ( startDate == null && endDate == null )
        {
            throw new IllegalArgumentException( "Start date or end date must be specified" );
        }
        
        Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria( DataValue.class )
            .setProjection( Projections.rowCount() );

        if ( !includeDeleted )
        {
            criteria.add( Restrictions.eq( "deleted", false ) );
        }
        
        if ( startDate != null )
        {
            criteria.add( Restrictions.ge( "lastUpdated", startDate ) );
        }
        
        if ( endDate != null )
        {
            criteria.add( Restrictions.le( "lastUpdated", endDate ) );
        }
        
        Number rs = (Number) criteria.uniqueResult();

        return rs != null ? rs.intValue() : 0;
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
