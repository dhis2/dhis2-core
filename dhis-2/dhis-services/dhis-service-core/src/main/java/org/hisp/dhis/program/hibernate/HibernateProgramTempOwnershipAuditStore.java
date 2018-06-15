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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceAudit;
import org.hisp.dhis.program.ProgramTempOwnershipAudit;
import org.hisp.dhis.program.ProgramTempOwnershipAuditQueryParams;
import org.hisp.dhis.program.ProgramTempOwnershipAuditStore;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
public class HibernateProgramTempOwnershipAuditStore implements ProgramTempOwnershipAuditStore
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
    // ProgramTempOwnershipAuditStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void addProgramTempOwnershipAudit( ProgramTempOwnershipAudit programTempOwnershipAudit )
    {
        sessionFactory.getCurrentSession().save( programTempOwnershipAudit );
    }

    @Override
    public void deleteProgramTempOwnershipAudit( Program program )
    {
        String hql = "delete ProgramTempOwnershipAudit where program = :program";
        sessionFactory.getCurrentSession().createQuery( hql ).setParameter( "program", program ).executeUpdate();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramTempOwnershipAudit> getProgramTempOwnershipAudits( ProgramTempOwnershipAuditQueryParams params )
    {
        Criteria criteria = getProgramTempOwnershipAuditCriteria( params );
        criteria.addOrder( Order.desc( "created" ) );

        if ( !params.isSkipPaging() )
        {
            criteria.setFirstResult( params.getFirst() );
            criteria.setMaxResults( params.getMax() );
        }

        return criteria.list();
    }

    @Override
    public int getProgramTempOwnershipAuditsCount( ProgramTempOwnershipAuditQueryParams params )
    {
        return ((Number) getProgramTempOwnershipAuditCriteria( params )
            .setProjection( Projections.countDistinct( "id" ) ).uniqueResult()).intValue();
    }

    private Criteria getProgramTempOwnershipAuditCriteria( ProgramTempOwnershipAuditQueryParams params )
    {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria( ProgramInstanceAudit.class );

        if ( params.hasPrograms() )
        {
            criteria.add( Restrictions.in( "program", params.getPrograms() ) );
        }

        if ( params.hasUsers() )
        {
            criteria.add( Restrictions.in( "accessedBy", params.getUsers() ) );
        }

        if ( params.hasStartDate() )
        {
            criteria.add( Restrictions.ge( "created", params.getStartDate() ) );
        }

        if ( params.hasEndDate() )
        {
            criteria.add( Restrictions.le( "created", params.getEndDate() ) );
        }

        return criteria;
    }
}
