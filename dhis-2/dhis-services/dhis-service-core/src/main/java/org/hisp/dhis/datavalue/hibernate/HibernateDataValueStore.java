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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.Map4;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;

/**
 * @author Torgeir Lorange Ostby
 */
public class HibernateDataValueStore
    implements DataValueStore
{
    private static final Log log = LogFactory.getLog( HibernateDataValueStore.class );

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
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo )
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
            
            hql = TextUtils.removeLastOr( hql );
            
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
        Collection<DataElement> dataElements, DataElementCategoryOptionCombo attributeOptionCombo )
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
    public Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> getDataElementOperandValues(
        Collection<DataElementOperand> dataElementOperands, Collection<Period> periods,
        Collection<OrganisationUnit> orgUnits )
    {
        Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> result = new Map4<>();

        Collection<Integer> periodIdList = IdentifiableObjectUtils.getIdentifiers( periods );

        SetMap<DataElement, DataElementOperand> deosByDataElement = getDeosByDataElement( dataElementOperands );

        if ( periods.size() == 0 || dataElementOperands.size() == 0 )
        {
            return result;
        }

        List<String> paths = orgUnits.stream().map( OrganisationUnit::getPath ).collect( Collectors.toList() );

        String pathsTable = statementBuilder.literalStringTable( paths, "p", "path" );

        String sql = "select dv.dataelementid, coc.uid, aoc.uid, dv.periodid, p.path, " +
            "sum( cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) ) as value " +
            "from datavalue dv " +
            "join organisationunit o on o.organisationunitid = dv.sourceid " +
            "join categoryoptioncombo coc on coc.categoryoptioncomboid = dv.categoryoptioncomboid " +
            "join categoryoptioncombo aoc on aoc.categoryoptioncomboid = dv.attributeoptioncomboid " +
            "join " + pathsTable + " on o.path like p.path || '%' " +
            "where dv.periodid in (" + TextUtils.getCommaDelimitedString( periodIdList ) + ") " +
            "and dv.value is not null " +
            "and dv.deleted is false " +
            "and ( ";

            String snippit = "";

            for ( DataElement dataElement : deosByDataElement.keySet() )
            {
                sql += snippit + "( dv.dataelementid = " + dataElement.getId()
                    + getDisaggRestriction( deosByDataElement.get( dataElement ) )
                    + " ) ";

                snippit = "or ";
            }

            sql += ") group by dv.dataelementid, coc.uid, aoc.uid, dv.periodid, p.path";

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        Map<Integer, DataElement> dataElementsById = IdentifiableObjectUtils.getIdentifierMap( deosByDataElement.keySet() );
        Map<Integer, Period> periodsById = IdentifiableObjectUtils.getIdentifierMap( periods );
        Map<String, OrganisationUnit> orgUnitsByPath = orgUnits.stream().collect( Collectors.toMap( o -> o.getPath(), o -> o ) );

        while ( rowSet.next() )
        {
            Integer dataElementId = rowSet.getInt( 1 );
            String categoryOptionComboUid = rowSet.getString( 2 );
            String attributeOptionComboUid = rowSet.getString( 3 );
            Integer periodId = rowSet.getInt( 4 );
            String path = rowSet.getString( 5 );
            Double value = rowSet.getDouble( 6 );

            DataElement dataElement = dataElementsById.get ( dataElementId );
            Period period = periodsById.get( periodId );
            OrganisationUnit orgUnit = orgUnitsByPath.get( path );

            Set<DataElementOperand> deos = deosByDataElement.get( dataElement );

            for ( DataElementOperand deo : deos )
            {
                if ( deo.getCategoryOptionCombo() == null || deo.getCategoryOptionCombo().getUid().equals( categoryOptionComboUid ) )
                {
                    double existingValue = ObjectUtils.firstNonNull( result.getValue(orgUnit, period, attributeOptionComboUid, deo ), 0.0 );

                    result.putEntry( orgUnit, period, attributeOptionComboUid, deo, value + existingValue );
                }
            }
        }

        return result;
    }

    /**
     * Groups a collection of DataElementOperands into sets according to the
     * DataElement each one contains, and returns a map from each DataElement
     * to the set of DataElementOperands containing it.
     *
     * @param deos the collection of DataElementOperands.
     * @return the map from DataElement to its DataElementOperands.
     */
    private SetMap<DataElement, DataElementOperand> getDeosByDataElement( Collection<DataElementOperand> deos )
    {
        SetMap<DataElement, DataElementOperand> deosByDataElement = new SetMap<>();

        for ( DataElementOperand deo : deos )
        {
            deosByDataElement.putValue( deo.getDataElement(), deo );
        }

        return deosByDataElement;
    }

    /**
     * Examines a set of DataElementOperands, and returns a SQL condition
     * restricting the CategoryOptionCombo to a list of specific combos
     * if only specific combos are required, or returns no restriction
     * if all CategoryOptionCombos are to be fetched.
     *
     * @param deos the collection of DataElementOperands.
     * @return the SQL restriction.
     */
    private String getDisaggRestriction( Set<DataElementOperand> deos )
    {
        String restiction = " and coc.uid in ( ";
        String snippit = "";

        for ( DataElementOperand deo : deos )
        {
            if ( deo.getCategoryOptionCombo() == null )
            {
                return "";
            }

            restiction += snippit + "'" + deo.getCategoryOptionCombo().getUid() + "'";

            snippit = ", ";
        }

        return restiction + " )";
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

    @Override
    public MapMapMap<Integer, String, DimensionalItemObject, Double> getDataValueMapByAttributeCombo(
        Set<DataElementOperand> dataElementOperands, Date date,
        List<OrganisationUnit> orgUnits, Collection<PeriodType> periodTypes, DataElementCategoryOptionCombo attributeCombo,
        Set<CategoryOptionGroup> cogDimensionConstraints, Set<DataElementCategoryOption> coDimensionConstraints )
    {
        SetMap<DataElement, DataElementOperand> deosByDataElement = getDeosByDataElement( dataElementOperands );

        MapMapMap<Integer, String, DimensionalItemObject, Double> map = new MapMapMap<>();

        if ( dataElementOperands.isEmpty() || periodTypes.isEmpty()
            || ( cogDimensionConstraints != null && cogDimensionConstraints.isEmpty() )
            || ( coDimensionConstraints != null && coDimensionConstraints.isEmpty() ) )
        {
            return map;
        }

        String joinCo = coDimensionConstraints == null && cogDimensionConstraints == null ? StringUtils.EMPTY :
            "join categoryoptioncombos_categoryoptions c_c on dv.attributeoptioncomboid = c_c.categoryoptioncomboid ";

        String joinCog = cogDimensionConstraints == null ? StringUtils.EMPTY :
            "join categoryoptiongroupmembers cogm on c_c.categoryoptionid = cogm.categoryoptionid ";

        String whereCo = coDimensionConstraints == null ? StringUtils.EMPTY :
            "and c_c.categoryoptionid in (" + TextUtils.getCommaDelimitedString( getIdentifiers( coDimensionConstraints ) ) + ") ";

        String whereCog = cogDimensionConstraints == null ? StringUtils.EMPTY :
            "and cogm.categoryoptiongroupid in (" + TextUtils.getCommaDelimitedString( getIdentifiers( cogDimensionConstraints ) ) + ") ";

        String whereCombo = attributeCombo == null ? StringUtils.EMPTY :
            "and dv.attributeoptioncomboid = " + attributeCombo.getId() + " ";

        String sql = "select dv.sourceid, dv.dataelementid, coc.uid, aoc.uid, dv.value, p.startdate, p.enddate " +
            "from datavalue dv " +
            "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid " +
            "inner join period p on p.periodid = dv.periodid " + joinCo + joinCog +
            "where dv.dataelementid in (" + TextUtils.getCommaDelimitedString(getIdentifiers( deosByDataElement.keySet() ) ) + ") " +
            "and dv.sourceid in (" + TextUtils.getCommaDelimitedString(getIdentifiers( orgUnits ) ) + ") " +
            "and p.startdate <= '" + DateUtils.getMediumDateString( date ) + "' " +
            "and p.enddate >= '" + DateUtils.getMediumDateString( date ) + "' " +
            "and p.periodtypeid in (" + TextUtils.getCommaDelimitedString( getIds( periodTypes ) ) + ") " +
            "and dv.deleted is false " +
            whereCo + whereCog + whereCombo;

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        MapMapMap<Integer, String, DataElementOperand, Long> checkForDuplicates = new MapMapMap<>();

        int rowCount = 0;

        Map<Integer, DataElement> dataElementsById = IdentifiableObjectUtils.getIdentifierMap( deosByDataElement.keySet() );

        while ( rowSet.next() )
        {
            rowCount++;
            int orgUnitId = rowSet.getInt( 1 );
            int dataElementId = rowSet.getInt( 2 );
            String categoryOptionCombo = rowSet.getString( 3 );
            String attributeOptionCombo = rowSet.getString( 4 );
            Double value = MathUtils.parseDouble( rowSet.getString( 5 ) );
            Date periodStartDate = rowSet.getDate( 6 );
            Date periodEndDate = rowSet.getDate( 7 );
            long periodInterval = periodEndDate.getTime() - periodStartDate.getTime();
            DataElement dataElement = dataElementsById.get( dataElementId );

            if ( value != null )
            {
                Set<DataElementOperand> deos = deosByDataElement.get( dataElement );

                for ( DataElementOperand deo : deos )
                {
                    if ( deo.getCategoryOptionCombo() == null || deo.getCategoryOptionCombo().getUid().equals( categoryOptionCombo ) )
                    {
                        double existingValue = ObjectUtils.firstNonNull( map.getValue(orgUnitId, attributeOptionCombo, deo), 0.0 );

                        Long existingPeriodInterval = checkForDuplicates.getValue( orgUnitId, attributeOptionCombo, deo );

                        if ( existingPeriodInterval != null )
                        {
                            if ( existingPeriodInterval < periodInterval )
                            {
                                continue; // Do not overwrite the previous value if for a shorter interval
                            }
                            else if ( existingPeriodInterval > periodInterval )
                            {
                                existingValue = 0.0; // Overwrite previous value if for a longer interval
                            }
                        }

                        map.putEntry( orgUnitId, attributeOptionCombo, deo, value + existingValue);

                        checkForDuplicates.putEntry( orgUnitId, attributeOptionCombo, deo, periodInterval );
                    }
                }
            }
        }

        log.trace( "getDataValueMapByAttributeCombo: " + rowCount + " rows into " + map.size() + " map entries from \"" + sql + "\"" );

        return map;
    }
    
    private Set<Integer> getIds( Collection<PeriodType> periodTypes )
    {
        Set<Integer> ids = new HashSet<>();
        
        for ( PeriodType pt : periodTypes )
        {
            ids.add( pt.getId() );
        }
        
        return ids;
    }
}
