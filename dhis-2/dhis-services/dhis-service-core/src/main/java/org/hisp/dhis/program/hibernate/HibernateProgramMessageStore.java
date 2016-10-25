package org.hisp.dhis.program.hibernate;

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
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageStore;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

@Transactional
public class HibernateProgramMessageStore
    extends HibernateIdentifiableObjectStore<ProgramMessage>
    implements ProgramMessageStore
{
    private static final String TABLE_NAME = "ProgramMessage";

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramMessage> getProgramMessages( ProgramMessageQueryParams params )
    {
        Query query = getHqlQuery( params );

        if ( params.hasPaging() )
        {
            query.setFirstResult( params.getPage() );
            query.setMaxResults( params.getPageSize() );
        }

        return query.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramMessage> getAllOutboundMessages()
    {
        Criteria criteria = getSession().createCriteria( ProgramMessage.class );
        criteria.add( Restrictions.and( Restrictions.eq( "messageStatus", "OUTBOUND" ),
            Restrictions.eq( "messageCatagory", "OUTGOING" ) ) );

        return criteria.list();
    }

    @Override
    public boolean exists( String uid )
    {
        ProgramMessage programMessage = getByUid( uid );

        return programMessage != null && programMessage.getId() > 0;
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    private Query getHqlQuery( ProgramMessageQueryParams params )
    {
        SqlHelper helper = new SqlHelper( true );

        String hql = " select distinct pm from " + TABLE_NAME + " pm ";

        if ( params.hasProgramInstance() )
        {
            hql += helper.whereAnd() + "pm.programInstance = :programInstance";
        }

        if ( params.hasProgramStageInstance() )
        {
            hql += helper.whereAnd() + "pm.programStageInstance = :programStageInstance";
        }

        hql += params.getMessageStatus() != null
            ? helper.whereAnd() + "pm.messageStatus = :messageStatus" : ""; 

        hql += params.getAfterDate() != null ? helper.whereAnd() + "pm.processeddate > :processeddate" : "" ;

        hql += params.getBeforeDate() != null
            ? helper.whereAnd() + "pm.processeddate < :processeddate" : ""; 

        Query query = sessionFactory.getCurrentSession().createQuery( hql );
        
        if ( params.hasProgramInstance() )
        {
            query.setInteger( "programInstance", params.getProgramInstance().getId() );
        }

        if ( params.hasProgramStageInstance() )
        {
            query.setInteger( "programStageInstance", params.getProgramStageInstance().getId() );
        }
        
        if ( params.getMessageStatus() != null)
        {
            query.setParameter( "messageStatus", params.getMessageStatus() );
        }

        if ( params.getAfterDate() != null )
        {
            query.setTime( "processeddate", params.getAfterDate() );
        }
        
        if ( params.getBeforeDate() != null )
        {
            query.setTime( "processeddate", params.getBeforeDate() );
        }
        
        return query;
    }
}
