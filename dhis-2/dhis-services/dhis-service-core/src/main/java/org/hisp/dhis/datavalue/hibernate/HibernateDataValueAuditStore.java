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

import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;

import java.util.ArrayList;
import java.util.List;

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
        return getDataValueAudits( Lists.newArrayList( dataValue.getDataElement() ), Lists.newArrayList( dataValue.getPeriod() ),
            Lists.newArrayList( dataValue.getSource() ), dataValue.getCategoryOptionCombo(), dataValue.getAttributeOptionCombo(), null );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits,
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo, AuditType auditType )
    {
        Criteria criteria = getDataValueAuditCriteria( dataElements, periods, organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType );
        criteria.addOrder( Order.desc( "created" ) );

        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits,
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo, AuditType auditType, int first, int max )
    {
        Criteria criteria = getDataValueAuditCriteria( dataElements, periods, organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType );
        criteria.addOrder( Order.desc( "created" ) );
        criteria.setFirstResult( first );
        criteria.setMaxResults( max );

        return criteria.list();
    }

    @Override
    public int countDataValueAudits( List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits,
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo, AuditType auditType )
    {
        return ((Number) getDataValueAuditCriteria( dataElements, periods, organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType )
            .setProjection( Projections.countDistinct( "id" ) ).uniqueResult()).intValue();
    }

    private Criteria getDataValueAuditCriteria( List<DataElement> dataElements, List<Period> periods, List<OrganisationUnit> organisationUnits, DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo, AuditType
        auditType )
    {
        Session session = sessionFactory.getCurrentSession();
        List<Period> storedPeriods = new ArrayList<>();

        if ( !periods.isEmpty() )
        {
            for ( Period period : periods )
            {
                Period storedPeriod = periodStore.reloadPeriod( period );

                if ( storedPeriod != null )
                {
                    storedPeriods.add( storedPeriod );
                }
            }
        }

        Criteria criteria = session.createCriteria( DataValueAudit.class );

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
            criteria.add( Restrictions.in( "organisationUnit", organisationUnits ) );
        }

        if ( categoryOptionCombo != null )
        {
            criteria.add( Restrictions.eq( "categoryOptionCombo", categoryOptionCombo ) );
        }

        if ( attributeOptionCombo != null )
        {
            criteria.add( Restrictions.eq( "attributeOptionCombo", attributeOptionCombo ) );
        }

        if ( auditType != null )
        {
            criteria.add( Restrictions.eq( "auditType", auditType ) );
        }

        return criteria;
    }
}
