package org.hisp.dhis.datavalue.hibernate;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
public class HibernateDataValueAuditStore
    implements DataValueAuditStore
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
    // DataValueAuditStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void addDataValueAudit( DataValueAudit dataValueAudit )
    {
        Session session = sessionFactory.getCurrentSession();

        session.save( dataValueAudit );
    }

    @Override
    public void deleteDataValueAudits( OrganisationUnit organisationUnit )
    {
        String hql = "delete from DataValueAudit d where d.organisationUnit = :unit";
        
        sessionFactory.getCurrentSession().createQuery( hql ).
            setEntity( "unit", organisationUnit ).executeUpdate();
    }

    @Override
    public List<DataValueAudit> getDataValueAudits( DataValue dataValue )
    {
        return getDataValueAudits( dataValue.getDataElement(), dataValue.getPeriod(),
            dataValue.getSource(), dataValue.getCategoryOptionCombo(), dataValue.getAttributeOptionCombo() );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DataValueAudit> getDataValueAudits( DataElement dataElement, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        Session session = sessionFactory.getCurrentSession();

        Period storedPeriod = periodStore.reloadPeriod( period );

        if( storedPeriod == null )
        {
            return new ArrayList<>();
        }

        Criteria criteria = session.createCriteria( DataValueAudit.class )
            .add( Restrictions.eq( "dataElement", dataElement ) )
            .add( Restrictions.eq( "period", storedPeriod ) )
            .add( Restrictions.eq( "organisationUnit", organisationUnit ) )
            .add( Restrictions.eq( "categoryOptionCombo", categoryOptionCombo ) )
            .add( Restrictions.eq( "attributeOptionCombo", attributeOptionCombo ) )
            .addOrder( Order.desc( "timestamp" ) );

        return criteria.list();
    }

    @Override
    public void deleteDataValueAudit( DataValueAudit dataValueAudit )
    {
        Session session = sessionFactory.getCurrentSession();

        session.delete( dataValueAudit );
    }

    @Override
    public int deleteDataValueAuditByDataElement( DataElement dataElement )
    {
        Query query = sessionFactory.getCurrentSession()
            .createQuery( "delete DataValueAudit where dataElement = :dataElement" )
            .setEntity( "dataElement", dataElement );

        return query.executeUpdate();
    }

    @Override
    public int deleteDataValueAuditByPeriod( Period period )
    {
        Period storedPeriod = periodStore.reloadPeriod( period );

        Query query = sessionFactory.getCurrentSession()
            .createQuery( "delete DataValueAudit where period = :period" )
            .setEntity( "period", storedPeriod );

        return query.executeUpdate();
    }

    @Override
    public int deleteDataValueAuditByOrganisationUnit( OrganisationUnit organisationUnit )
    {
        Query query = sessionFactory.getCurrentSession()
            .createQuery( "delete DataValueAudit where organisationUnit = :organisationUnit" )
            .setEntity( "organisationUnit", organisationUnit );

        return query.executeUpdate();
    }

    @Override
    public int deleteDataValueAuditByCategoryOptionCombo( DataElementCategoryOptionCombo categoryOptionCombo )
    {
        Query query = sessionFactory.getCurrentSession()
            .createQuery( "delete DataValueAudit where categoryOptionCombo = :categoryOptionCombo or attributeOptionCombo = :categoryOptionCombo" )
            .setEntity( "categoryOptionCombo", categoryOptionCombo );

        return query.executeUpdate();
    }
}
