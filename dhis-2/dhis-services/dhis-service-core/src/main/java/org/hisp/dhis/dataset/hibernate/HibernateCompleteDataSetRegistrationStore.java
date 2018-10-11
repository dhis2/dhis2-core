package org.hisp.dhis.dataset.hibernate;

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

import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class HibernateCompleteDataSetRegistrationStore extends HibernateGenericStore<CompleteDataSetRegistration>
    implements CompleteDataSetRegistrationStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

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
        
        getSession().save( registration );
    }

    @Override
    public void updateCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        registration.setPeriod( periodStore.reloadForceAddPeriod( registration.getPeriod() ) );
        
        getSession().update( registration );
    }

    @Override
    public CompleteDataSetRegistration getCompleteDataSetRegistration( DataSet dataSet, Period period,
        OrganisationUnit source, CategoryOptionCombo attributeOptionCombo )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        return getSingleResult( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root, new CompleteDataSetRegistration( dataSet, storedPeriod, source, attributeOptionCombo ) ) ) );
    }

    @Override
    public void deleteCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        getSession().delete( registration );
    }

    @Override
    public List<CompleteDataSetRegistration> getAllCompleteDataSetRegistrations()
    {
        return getList( getCriteriaBuilder(), newJpaParameters() );
    }

    @Override
    public List<CompleteDataSetRegistration> getCompleteDataSetRegistrations(
        Collection<DataSet> dataSets, Collection<OrganisationUnit> sources, Collection<Period> periods )
    {
        for ( Period period : periods )
        {
            period = periodStore.reloadPeriod( period );
        }

        return getList( getCriteriaBuilder(), newJpaParameters()
            .addPredicate( root -> root.get( "dataSet" ).in( dataSets ) )
            .addPredicate( root -> root.get( "source" ).in( sources ) )
            .addPredicate( root -> root.get( "period" ).in( periods ) ) );
    }

    @Override
    public List<CompleteDataSetRegistration> getCompleteDataSetRegistrations(
        DataSet dataSet, Collection<OrganisationUnit> sources, Period period, Date deadline )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        if ( storedPeriod == null )
        {
            return null;
        }

        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "dataSet" ), dataSet ) )
            .addPredicate( root -> root.get( "source" ).in( sources ) )
            .addPredicate( root -> builder.equal( root.get( "period" ), period ) )
            .addPredicate( root -> builder.lessThanOrEqualTo( root.get( "date" ), deadline ) ) );
    }

    @Override
    public void deleteCompleteDataSetRegistrations( DataSet dataSet )
    {
        String hql = "delete from CompleteDataSetRegistration c where c.dataSet = :dataSet";

        getSession().createQuery( hql ).
            setParameter( "dataSet", dataSet ).executeUpdate();
    }
    
    @Override
    public void deleteCompleteDataSetRegistrations( OrganisationUnit unit )
    {
        String hql = "delete from CompleteDataSetRegistration c where c.source = :source";

        getSession().createQuery( hql ).
            setParameter( "source", unit ).executeUpdate();
    }
}
