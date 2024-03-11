package org.hisp.dhis.program.hibernate;

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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.program.EventSyncStore;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public class HibernateEventSyncStore implements EventSyncStore
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
    // Implementation methods
    // -------------------------------------------------------------------------
    
    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramStageInstance> getEvents( List<String> uids )
    {
        if ( uids.isEmpty() )
        {
            return new ArrayList<>();
        }
        
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( ProgramStageInstance.class );
        
        criteria.add( Restrictions.in( "uid", uids ) );
        
        return criteria.list();
    }

    @Override
    public ProgramStageInstance getEvent( String uid )
    {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( ProgramStageInstance.class );
        criteria.add( Restrictions.eq( "uid", uid ) );

        return (ProgramStageInstance) criteria.uniqueResult();
    }

    @Override
    public ProgramInstance getEnrollment( String uid )
    {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( ProgramInstance.class );
        criteria.add( Restrictions.eq( "uid", uid ) );

        return (ProgramInstance) criteria.uniqueResult();
    }
}