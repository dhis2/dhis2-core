package org.hisp.dhis.sms.hibernate;

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

import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.sms.outbound.OutboundSmsStore;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Date;
import java.util.List;

public class HibernateOutboundSmsStore
    extends HibernateGenericStore<OutboundSms>
    implements OutboundSmsStore
{
    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void saveOutboundSms( OutboundSms sms )
    {
        checkDate( sms );
        save( sms );
    }

    private void checkDate( OutboundSms sms )
    {
        if ( sms.getDate() == null )
        {
            sms.setDate( new Date() );
        }
    }

    @Override
    public OutboundSms getOutboundSmsbyId( int id )
    {
        return get( id );
    }

    @Override
    public List<OutboundSms> getAllOutboundSms()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addOrder( root -> builder.desc( root.get( "date" ) ) ));
    }

    @Override
    public List<OutboundSms> get( OutboundSmsStatus status )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<OutboundSms> parameters = new JpaQueryParameters<OutboundSms>()
            .addOrder( root -> builder.desc( root.get( "date" ) ) );

        if ( status != null )
        {
            parameters.addPredicate( root -> builder.equal( root.get( "status" ), status ) );
        }

        return getList( builder, parameters );
    }

    @Override
    public void updateOutboundSms( OutboundSms sms )
    {
        update( sms );
    }

    @Override
    public void deleteOutboundSms( OutboundSms sms )
    {
        delete( sms );
    }

    @Override
    public List<OutboundSms> get( OutboundSmsStatus status, Integer min, Integer max )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<OutboundSms> parameters = new JpaQueryParameters<OutboundSms>()
            .addOrder( root -> builder.desc( root.get( "date" ) ) );

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

    @Override
    public List<OutboundSms> getAllOutboundSms( Integer min, Integer max )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<OutboundSms> parameters = new JpaQueryParameters<OutboundSms>()
            .addOrder( root -> builder.desc( root.get( "date" ) ) );

        if ( min != null && max != null )
        {
            parameters.setFirstResult( min ).setMaxResults( max );
        }

        return getList( builder, parameters );
    }
}
