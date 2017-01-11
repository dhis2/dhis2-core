package org.hisp.dhis.sms.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsStore;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class HibernateIncomingSmsStore
    implements IncomingSmsStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SessionFactory sessionFactory;

    public void setSessionFactory( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public int save( IncomingSms sms )
    {
        return (Integer) sessionFactory.getCurrentSession().save( sms );
    }

    @Override
    public IncomingSms get( int id )
    {
        Session session = sessionFactory.getCurrentSession();
        return (IncomingSms) session.get( IncomingSms.class, id );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String keyword )
    {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria( IncomingSms.class ).addOrder( Order.desc( "sentDate" ) );
        if ( status != null )
        {
            criteria.add( Restrictions.eq( "status", status ) );
        }
        criteria.add( Restrictions.ilike( "originator", "%" + keyword + "%" ) );
        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<IncomingSms> getSmsByOriginator( String originator )
    {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( IncomingSms.class );
        criteria.add( Restrictions.eq( "originator", originator ) );
        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<IncomingSms> getAllSmses()
    {
        return sessionFactory.getCurrentSession().createCriteria( IncomingSms.class ).addOrder( Order.desc( "id" ) )
            .list();
    }

    @Override
    public long getSmsCount()
    {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria( IncomingSms.class );
        criteria.setProjection( Projections.rowCount() );
        Long count = (Long) criteria.uniqueResult();
        return count != null ? count.longValue() : (long) 0;
    }

    @Override
    public void delete( IncomingSms incomingSms )
    {
        sessionFactory.getCurrentSession().delete( incomingSms );
    }

    @Override
    public void update( IncomingSms incomingSms )
    {
        sessionFactory.getCurrentSession().update( incomingSms );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public List<IncomingSms> getAllUnparsedSmses()
    {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( IncomingSms.class );
        criteria.add( Restrictions.eq( "parsed", false ) );
        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String keyword, Integer min, Integer max )
    {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria( IncomingSms.class ).addOrder( Order.desc( "sentDate" ) );

        if ( status != null )
        {
            criteria.add( Restrictions.eq( "status", status ) );
        }
        criteria.add( Restrictions.ilike( "originator", "%" + keyword + "%" ) );

        if ( min != null && max != null )
        {
            criteria.setFirstResult( min ).setMaxResults( max );
        }

        return criteria.list();
    }
}
