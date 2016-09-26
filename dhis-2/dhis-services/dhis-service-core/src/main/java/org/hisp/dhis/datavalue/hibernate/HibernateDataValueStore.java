package org.hisp.dhis.datavalue.hibernate;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.api.client.util.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
    public List<DataValue> getAllDataValues()
    {
        return sessionFactory.getCurrentSession()
            .createCriteria( DataValue.class )
            .add( Restrictions.eq( "deleted", false ) )
            .list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataValue> getDataValues( Collection<DataElement> dataElements, 
        Collection<Period> periods, Collection<OrganisationUnit> organisationUnits )
    {        
        Set<Period> storedPeriods = Sets.newHashSet();
        
        for ( Period period : periods )
        {
            storedPeriods.add( periodStore.reloadPeriod( period ) );
        }

        if ( dataElements.isEmpty() && storedPeriods.isEmpty() && organisationUnits.isEmpty() )
        {
            return new ArrayList<>();
        }
        
        Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria( DataValue.class )
            .add( Restrictions.eq( "deleted", false ) );
        
        if ( !dataElements.isEmpty() )
        {
            criteria.add( Restrictions.in( "dataElement", dataElements ) );
        }
        
        if ( !storedPeriods.isEmpty() )
        {
            criteria.add( Restrictions.in( "period", storedPeriods ) );
        }
        
        if ( !organisationUnits.isEmpty() )
        {
            criteria.add( Restrictions.in( "source", organisationUnits ) );
        }
            
        return criteria.list();
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
    public List<DeflatedDataValue> getDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
	  Collection<Period> periods, Collection<OrganisationUnit> sources )
    {
        List<DeflatedDataValue> result = new ArrayList<DeflatedDataValue>();
        Collection<Integer> periodIdList = IdentifiableObjectUtils.getIdentifiers( periods );
        List<Integer> sourceIdList = IdentifiableObjectUtils.getIdentifiers( sources );
        Integer dataElementId = dataElement.getId();

        String sql = "select categoryoptioncomboid, attributeoptioncomboid, value, " +
            "sourceid, periodid, storedby, created, lastupdated, comment, followup " +
            "from datavalue " +
            "where dataelementid=" + dataElementId + " " +
            ( ( categoryOptionCombo == null ) ? "" : ( "and categoryoptioncomboid=" + categoryOptionCombo.getId() + " " ) ) +
            "and sourceid in (" + TextUtils.getCommaDelimitedString( sourceIdList ) + ") " +
            "and periodid in (" + TextUtils.getCommaDelimitedString( periodIdList ) + ") " +
            "and deleted is false";

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            Integer categoryOptionComboId = rowSet.getInt( 1 );
            Integer attributeOptionComboId = rowSet.getInt( 2 );
            String value = rowSet.getString( 3 );
            Integer sourceId = rowSet.getInt( 4 );
            Integer periodId = rowSet.getInt( 5 );
            String storedBy = rowSet.getString( 6 );
            Date created = rowSet.getDate( 7 );
            Date lastUpdated = rowSet.getDate( 8 );
            String comment = rowSet.getString( 9 );
            boolean followup = rowSet.getBoolean( 10 );

            if ( value != null )
            {
                DeflatedDataValue dv = new DeflatedDataValue( dataElementId, periodId, sourceId,
                    categoryOptionComboId, attributeOptionComboId, value,
                    storedBy, created, lastUpdated,
                    comment, followup );

                result.add( dv );
            }
        }

        return result;
    }

    @Override
    public List<DeflatedDataValue> sumRecursiveDeflatedDataValues(
        DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, OrganisationUnit source )
    {
        List<DeflatedDataValue> result = new ArrayList<DeflatedDataValue>();
        Collection<Integer> periodIdList = IdentifiableObjectUtils.getIdentifiers( periods );
        Integer dataElementId = dataElement.getId();
        String sourcePrefix = source.getPath();
        Integer sourceId = source.getId();

        String castType = statementBuilder.getDoubleColumnType();

        String sql = "select dataelementid, categoryoptioncomboid, attributeoptioncomboid, periodid, " +
            "sum(cast(value as "+castType+")) as value " +
            "from datavalue, organisationunit " +
            "where dataelementid=" + dataElementId + " " +
            "and sourceid = organisationunitid " +
            ((categoryOptionCombo == null) ? "" :
                ("and categoryoptioncomboid=" + categoryOptionCombo.getId() + " ")) +
            "and path like '" + sourcePrefix + "%' " +
            "and periodid in (" + TextUtils.getCommaDelimitedString( periodIdList ) + ") " +
            "and deleted is false " +
            "group by dataelementid, categoryoptioncomboid, attributeoptioncomboid, periodid";

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            Integer categoryOptionComboId = rowSet.getInt( 2 );
            Integer attributeOptionComboId = rowSet.getInt( 3 );
            Integer periodId = rowSet.getInt( 4 );
            String value = rowSet.getString( 5 );

            if ( value != null )
            {
                DeflatedDataValue dv = new DeflatedDataValue( dataElementId, periodId, sourceId,
                    categoryOptionComboId, attributeOptionComboId, value );

                result.add( dv );
            }
        }

        return result;
    }

    @Override
    public int getDataValueCountLastUpdatedAfter( Date date )
    {
        Criteria criteria = sessionFactory.getCurrentSession()
            .createCriteria( DataValue.class )
            .add( Restrictions.ge( "lastUpdated", date ) )
            .add( Restrictions.eq( "deleted", false ) )
            .setProjection( Projections.rowCount() );

        Number rs = (Number) criteria.uniqueResult();

        return rs != null ? rs.intValue() : 0;
    }

    @Override
    public MapMap<Integer, DataElementOperand, Double> getDataValueMapByAttributeCombo( Collection<DataElement> dataElements, Date date,
        OrganisationUnit source, Collection<PeriodType> periodTypes, DataElementCategoryOptionCombo attributeCombo,
        Set<CategoryOptionGroup> cogDimensionConstraints, Set<DataElementCategoryOption> coDimensionConstraints,
        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap )
    {
        MapMap<Integer, DataElementOperand, Double> map = new MapMap<>();

        if ( dataElements.isEmpty() || periodTypes.isEmpty()
            || ( cogDimensionConstraints != null && cogDimensionConstraints.isEmpty() )
            || ( coDimensionConstraints != null && coDimensionConstraints.isEmpty() ) )
        {
            return map;
        }

        String joinCo = coDimensionConstraints == null && cogDimensionConstraints == null ? "" :
            "join categoryoptioncombos_categoryoptions c_c on dv.attributeoptioncomboid = c_c.categoryoptioncomboid ";

        String joinCog = cogDimensionConstraints == null ? "" :
            "join categoryoptiongroupmembers cogm on c_c.categoryoptionid = cogm.categoryoptionid ";

        String whereCo = coDimensionConstraints == null ? "" :
            "and c_c.categoryoptionid in (" + TextUtils.getCommaDelimitedString( getIdentifiers( coDimensionConstraints ) ) + ") ";

        String whereCog = cogDimensionConstraints == null ? "" :
            "and cogm.categoryoptiongroupid in (" + TextUtils.getCommaDelimitedString( getIdentifiers( cogDimensionConstraints ) ) + ") ";

        String whereCombo = attributeCombo == null ? "" :
            "and dv.attributeoptioncomboid = " + attributeCombo.getId() + " ";

        String sql = "select de.uid, coc.uid, dv.attributeoptioncomboid, dv.value, dv.lastupdated, p.startdate, p.enddate " +
            "from datavalue dv " +
            "join dataelement de on dv.dataelementid = de.dataelementid " +
            "join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "join period p on p.periodid = dv.periodid " + joinCo + joinCog +
            "where dv.dataelementid in (" + TextUtils.getCommaDelimitedString( getIdentifiers( dataElements ) ) + ") " +
            "and dv.sourceid = " + source.getId() + " " +
            "and p.startdate <= '" + DateUtils.getMediumDateString( date ) + "' " +
            "and p.enddate >= '" + DateUtils.getMediumDateString( date ) + "' " +
            "and p.periodtypeid in (" + TextUtils.getCommaDelimitedString( getIds( periodTypes ) ) + ") " +
            "and dv.deleted is false " +
            whereCo + whereCog + whereCombo;

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        MapMap<Integer, DataElementOperand, Long> checkForDuplicates = new MapMap<>();

        while ( rowSet.next() )
        {
            String dataElement = rowSet.getString( 1 );
            String categoryOptionCombo = rowSet.getString( 2 );
            Integer attributeOptionComboId = rowSet.getInt( 3 );
            Double value = MathUtils.parseDouble( rowSet.getString( 4 ) );
            Date lastUpdated = rowSet.getDate( 5 );
            Date periodStartDate = rowSet.getDate( 6 );
            Date periodEndDate = rowSet.getDate( 7 );
            long periodInterval = periodEndDate.getTime() - periodStartDate.getTime();

            log.trace( "row: " + dataElement + " = " + value + " [" + periodStartDate + " : " + periodEndDate + "]");

            if ( value != null )
            {
                DataElementOperand dataElementOperand = new DataElementOperand( dataElement, categoryOptionCombo );

                Long existingPeriodInterval = checkForDuplicates.getValue( attributeOptionComboId, dataElementOperand );

                if ( existingPeriodInterval != null && existingPeriodInterval < periodInterval )
                {
                    // Don't overwrite the previous value if for a shorter interval
                    continue;
                }
                map.putEntry( attributeOptionComboId, dataElementOperand, value );

                if ( lastUpdatedMap != null )
                {
                    lastUpdatedMap.putEntry( attributeOptionComboId, dataElementOperand, lastUpdated );
                }

                checkForDuplicates.putEntry( attributeOptionComboId, dataElementOperand, periodInterval );
            }
        }

        return map;
    }
    
    private List<Integer> getIds( Collection<PeriodType> periodTypes )
    {
        List<Integer> ids = new ArrayList<>();
        
        for ( PeriodType pt : periodTypes )
        {
            ids.add( pt.getId() );
        }
        
        return ids;
    }
}
