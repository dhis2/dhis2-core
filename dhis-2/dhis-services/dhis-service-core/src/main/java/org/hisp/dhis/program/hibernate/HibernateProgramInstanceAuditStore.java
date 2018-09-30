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

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceAudit;
import org.hisp.dhis.program.ProgramInstanceAuditQueryParams;
import org.hisp.dhis.program.ProgramInstanceAuditStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public class HibernateProgramInstanceAuditStore
    extends HibernateGenericStore<ProgramInstanceAudit>
    implements ProgramInstanceAuditStore
{

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
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
        save( programInstanceAudit );
    }

    @Override
    public void deleteProgramInstanceAudit( ProgramInstance programInstance )
    {
        String hql = "delete ProgramInstanceAudit where programInstance = :programInstance";
        sessionFactory.getCurrentSession().createQuery( hql ).setParameter( "programInstance", programInstance ).executeUpdate();
    }

    @Override
    public List<ProgramInstanceAudit> getProgramInstanceAudits( ProgramInstanceAuditQueryParams params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<ProgramInstanceAudit> jpaParameters = newJpaParameters()
            .addPredicates( getProgramInstanceAuditCriteria( params, builder ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        if( !params.isSkipPaging() )
        {
            jpaParameters.setFirstResult( params.getFirst() ).setMaxResults( params.getMax() );
        }

        return getList( builder, jpaParameters );
    }

    @Override
    public int getProgramInstanceAuditsCount( ProgramInstanceAuditQueryParams params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getCount( builder, newJpaParameters()
            .addPredicates( getProgramInstanceAuditCriteria( params, builder ) )
            .setUseDistinct( true ) )
            .intValue();
    }

    private List<Function<Root<ProgramInstanceAudit>, Predicate>> getProgramInstanceAuditCriteria( ProgramInstanceAuditQueryParams params, CriteriaBuilder builder )
    {
        List<Function<Root<ProgramInstanceAudit>, Predicate>> predicates = new ArrayList<>();

        if ( params.hasProgramInstances() )
        {
            predicates.add( root -> root.get( "programInstance" ).in( params.getProgramInstances() ) );
        }
        
        if ( params.hasPrograms() )
        {
            predicates.add( root -> root.join( "programInstance" ).get( "program" ).in( params.getPrograms() ) );
        }
        
        if ( params.hasUsers() )
        {
            predicates.add( root -> root.get( "accessedBy" ).in( params.getUsers() ) );
        }
        
        if ( params.hasAuditType() )
        {
            predicates.add( root -> builder.equal( root.get( "auditType" ), params.getAuditType() ) );
        }

        if ( params.hasStartDate() )
        {
            predicates.add( root -> builder.greaterThanOrEqualTo( root.get( "created" ), params.getStartDate() ) );
        }

        if ( params.hasEndDate() )
        {
            predicates.add(  root -> builder.lessThanOrEqualTo( root.get( "created" ), params.getEndDate() ) );
        }

        return predicates;
    }
}
