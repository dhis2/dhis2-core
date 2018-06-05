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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceAudit;
import org.hisp.dhis.program.ProgramInstanceAuditQueryParams;
import org.hisp.dhis.program.ProgramInstanceAuditStore;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public class HibernateProgramInstanceAuditStore
    implements ProgramInstanceAuditStore
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
    // ProgramInstanceAuditStore implementation
    // -------------------------------------------------------------------------
    
    @Override
    public void addProgramInstanceAudit( ProgramInstanceAudit programInstanceAudit )
    {
        sessionFactory.getCurrentSession().save( programInstanceAudit );
    }

    @Override
    public void deleteProgramInstanceAudit( ProgramInstance programInstance )
    {
        String hql = "delete ProgramInstanceAudit where programInstance = :programInstance";
        sessionFactory.getCurrentSession().createQuery( hql ).setParameter( "programInstance", programInstance ).executeUpdate();
    }

    @Override
    @SuppressWarnings( "unchecked" )    
    public List<ProgramInstanceAudit> getProgramInstanceAudits( ProgramInstanceAuditQueryParams params )
    {
        Criteria criteria = getProgramInstanceAuditCriteria( params );
        criteria.addOrder( Order.desc( "created" ) );
        
        if( !params.isSkipPaging() )
        {
            criteria.setFirstResult( params.getFirst() );
            criteria.setMaxResults( params.getMax() );
        }

        return criteria.list();
    }

    @Override
    public int getProgramInstanceAuditsCount( ProgramInstanceAuditQueryParams params )
    {
        return ((Number) getProgramInstanceAuditCriteria( params )
            .setProjection( Projections.countDistinct( "id" ) ).uniqueResult()).intValue();
    }
    
    
    private Criteria getProgramInstanceAuditCriteria( ProgramInstanceAuditQueryParams params )
    {        
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( ProgramInstanceAudit.class );

        if ( params.hasProgramInstances() )
        {
            criteria.add( Restrictions.in( "programInstance", params.getProgramInstances() ) );
        }
        
        if ( params.hasUsers() )
        {
            criteria.add( Restrictions.in( "accessedBy", params.getUsers() ) );
        }
        
        if ( params.hasAuditType() )
        {
            criteria.add( Restrictions.eq( "auditType", params.getAuditType() ) );
        }

        if ( params.hasStartDate() )
        {
            criteria.add(  Restrictions.ge( "created", params.getStartDate() ) );
        }

        if ( params.hasEndDate() )
        {
            criteria.add(  Restrictions.le( "created", params.getEndDate() ) );
        }

        return criteria;
    }
}
