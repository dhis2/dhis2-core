package org.hisp.dhis.dataset.hibernate;

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

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateLockExceptionStore
    extends HibernateGenericStore<LockException>
    implements LockExceptionStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetStore dataSetStore;

    public void setDataSetStore( DataSetStore dataSetStore )
    {
        this.dataSetStore = dataSetStore;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // LockExceptionStore Implementation
    // -------------------------------------------------------------------------

    @Override
    public int save( LockException lockException )
    {
        lockException.setPeriod( periodService.reloadPeriod( lockException.getPeriod() ) );

        return super.save( lockException );
    }

    @Override
    public void update( LockException lockException )
    {
        lockException.setPeriod( periodService.reloadPeriod( lockException.getPeriod() ) );

        super.update( lockException );
    }

    @Override
    public List<LockException> getCombinations()
    {
        final String sql = "select distinct datasetid, periodid from lockexception";

        final List<LockException> lockExceptions = new ArrayList<>();

        jdbcTemplate.query( sql, new RowCallbackHandler()
        {
            @Override
            public void processRow( ResultSet rs ) throws SQLException
            {
                int dataSetId = rs.getInt( 1 );
                int periodId = rs.getInt( 2 );

                LockException lockException = new LockException();
                Period period = periodService.getPeriod( periodId );
                DataSet dataSet = dataSetStore.get( dataSetId );

                lockException.setDataSet( dataSet );
                lockException.setPeriod( period );

                lockExceptions.add( lockException );
            }
        } );

        return lockExceptions;
    }

    @Override
    public void deleteCombination( DataSet dataSet, Period period )
    {
        final String hql = "delete from LockException where dataSet=:dataSet and period=:period";

        Query query = getQuery( hql );
        query.setParameter( "dataSet", dataSet );
        query.setParameter( "period", period );

        query.executeUpdate();
    }


    @Override
    public void deleteCombination( DataSet dataSet, Period period, OrganisationUnit organisationUnit )
    {
        final String hql = "delete from LockException where dataSet=:dataSet and period=:period and organisationUnit=:organisationUnit";

        Query query = getQuery( hql );
        query.setParameter( "dataSet", dataSet );
        query.setParameter( "period", period );
        query.setParameter( "organisationUnit", organisationUnit );

        query.executeUpdate();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<LockException> getAllOrderedName( int first, int max )
    {
        Criteria criteria = getCriteria();
        criteria.setFirstResult( first );
        criteria.setMaxResults( max );

        return criteria.list();
    }

    @Override
    public long getCount( DataElement dataElement, Period period, OrganisationUnit organisationUnit )
    {
        Criteria criteria = getCriteria(
            Restrictions.eq( "period", periodService.reloadPeriod( period ) ),
            Restrictions.eq( "organisationUnit", organisationUnit ),
            Restrictions.in( "dataSet", dataElement.getDataSets() ) );

        return (Long) criteria.setProjection( Projections.rowCount() ).uniqueResult();
    }

    @Override
    public long getCount( DataSet dataSet, Period period, OrganisationUnit organisationUnit )
    {
        Criteria criteria = getCriteria(
            Restrictions.eq( "period", periodService.reloadPeriod( period ) ),
            Restrictions.eq( "organisationUnit", organisationUnit ),
            Restrictions.eq( "dataSet", dataSet ) );

        return (Long) criteria.setProjection( Projections.rowCount() ).uniqueResult();
    }
}
