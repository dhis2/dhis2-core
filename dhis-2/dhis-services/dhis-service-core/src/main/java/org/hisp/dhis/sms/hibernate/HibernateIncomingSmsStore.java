package org.hisp.dhis.sms.hibernate;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsStore;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

@Repository( "org.hisp.dhis.sms.hibernate.IncomingSmsStore" )
public class HibernateIncomingSmsStore extends HibernateGenericStore<IncomingSms>
    implements IncomingSmsStore
{
    public HibernateIncomingSmsStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher )
    {
        super( sessionFactory, jdbcTemplate, publisher, IncomingSms.class, false );
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public IncomingSms get( int id )
    {
        Session session = sessionFactory.getCurrentSession();
        return session.get( IncomingSms.class, id );
    }

    @Override
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String originator )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<IncomingSms> parameter = newJpaParameters()
        .addOrder( root -> builder.desc( root.get( "sentDate" ) ) );

        if ( status != null )
        {
            parameter.addPredicate( root -> builder.equal( root.get( "status" ), status ) );
        }

        if ( originator != null && !originator.isEmpty() )
        {
            parameter.addPredicate( root -> JpaQueryUtils.stringPredicateIgnoreCase( builder, root.get( "originator" ), originator, JpaQueryUtils.StringSearchMode.ANYWHERE ) );
        }

        return getList( builder, parameter );
    }

    @Override
    public List<IncomingSms> getSmsByOriginator( String originator )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "originator" ), originator ) ) );
    }

    @Override
    public List<IncomingSms> getAllSmses()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addOrder( root -> builder.desc( root.get( "id" ) ) ) );
    }

    @Override
    public long getSmsCount()
    {
        return getCount( getCriteriaBuilder(), newJpaParameters() );
    }

    @Override
    public void delete( IncomingSms incomingSms )
    {
        sessionFactory.getCurrentSession().delete( incomingSms );
    }

    @Override
    public List<IncomingSms> getAllUnparsedMessages()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "parsed" ), false ) ) );
    }

    @Override
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String keyword, Integer min, Integer max )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<IncomingSms> parameters = newJpaParameters()
        .addPredicate( root -> JpaQueryUtils.stringPredicateIgnoreCase( builder, root.get( "originator" ), keyword, JpaQueryUtils.StringSearchMode.ANYWHERE ) );

        if ( status != null )
        {
            parameters.addPredicate( root -> builder.equal( root.get( "status" ), status ) );
        }

        if ( min != null && max != null )
        {
            parameters.setFirstResult( min ).setMaxResults( max );
        }

        return getList( builder, parameters );
    }
}
