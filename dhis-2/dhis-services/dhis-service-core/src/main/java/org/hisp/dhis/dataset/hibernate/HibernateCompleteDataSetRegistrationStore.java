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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class HibernateCompleteDataSetRegistrationStore
    implements CompleteDataSetRegistrationStore
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

    // -------------------------------------------------------------------------
    // DataSetCompleteRegistrationStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void saveCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        registration.setPeriod( periodStore.reloadForceAddPeriod( registration.getPeriod() ) );
        
        sessionFactory.getCurrentSession().save( registration );
    }

    @Override
    public void updateCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        registration.setPeriod( periodStore.reloadForceAddPeriod( registration.getPeriod() ) );
        
        sessionFactory.getCurrentSession().update( registration );
    }

    @Override
    public CompleteDataSetRegistration getCompleteDataSetRegistration( DataSet dataSet, Period period,
        OrganisationUnit source, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( CompleteDataSetRegistration.class );
        
        criteria.add( Restrictions.eq( "dataSet", dataSet ) );
        criteria.add( Restrictions.eq( "period", storedPeriod ) );
        criteria.add( Restrictions.eq( "source", source ) );
        criteria.add( Restrictions.eq( "attributeOptionCombo", attributeOptionCombo ) );
        
        return (CompleteDataSetRegistration) criteria.uniqueResult();
    }

    @Override
    public void deleteCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        sessionFactory.getCurrentSession().delete( registration );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<CompleteDataSetRegistration> getCompleteDataSetRegistrations( 
        DataSet dataSet, Collection<OrganisationUnit> sources, Period period )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( CompleteDataSetRegistration.class );
        
        criteria.add( Restrictions.eq( "dataSet", dataSet ) );
        criteria.add( Restrictions.eq( "period", storedPeriod ) );
        criteria.add( Restrictions.in( "source", sources ) );
        
        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<CompleteDataSetRegistration> getAllCompleteDataSetRegistrations()
    {
        return sessionFactory.getCurrentSession().createCriteria( CompleteDataSetRegistration.class ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<CompleteDataSetRegistration> getCompleteDataSetRegistrations( 
        Collection<DataSet> dataSets, Collection<OrganisationUnit> sources, Collection<Period> periods )
    {
        for ( Period period : periods )
        {
            period = periodStore.reloadPeriod( period );
        }        
        
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( CompleteDataSetRegistration.class );
        
        criteria.add( Restrictions.in( "dataSet", dataSets ) );
        criteria.add( Restrictions.in( "source", sources ) );
        criteria.add( Restrictions.in( "period", periods ) );
        
        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<CompleteDataSetRegistration> getCompleteDataSetRegistrations( 
        DataSet dataSet, Collection<OrganisationUnit> sources, Period period, Date deadline )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( CompleteDataSetRegistration.class );
        
        criteria.add( Restrictions.eq( "dataSet", dataSet ) );
        criteria.add( Restrictions.in( "source", sources ) );
        criteria.add( Restrictions.eq( "period", period ) );
        criteria.add( Restrictions.le( "date", deadline ) );
        
        return criteria.list();
    }

    @Override
    public void deleteCompleteDataSetRegistrations( DataSet dataSet )
    {
        String hql = "delete from CompleteDataSetRegistration c where c.dataSet = :dataSet";
        
        sessionFactory.getCurrentSession().createQuery( hql ).
            setEntity( "dataSet", dataSet ).executeUpdate();
    }
    
    @Override
    public void deleteCompleteDataSetRegistrations( OrganisationUnit unit )
    {
        String hql = "delete from CompleteDataSetRegistration c where c.source = :source";

        sessionFactory.getCurrentSession().createQuery( hql ).
            setEntity( "source", unit ).executeUpdate();
    }
}
