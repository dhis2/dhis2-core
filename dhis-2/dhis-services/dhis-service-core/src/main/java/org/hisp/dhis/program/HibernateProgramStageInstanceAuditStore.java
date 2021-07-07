/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.program;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.springframework.stereotype.Repository;

/**
 * @author Zubair Asghar
 */

@Repository( "org.hisp.dhis.program.ProgramStageInstanceAuditStore" )
public class HibernateProgramStageInstanceAuditStore implements ProgramStageInstanceAuditStore
{
    private SessionFactory sessionFactory;

    public HibernateProgramStageInstanceAuditStore( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void add( ProgramStageInstanceAudit stageInstanceAudit )
    {
        Session session = sessionFactory.getCurrentSession();

        session.save( stageInstanceAudit );
    }

    @Override
    public List<ProgramStageInstanceAudit> getAllAudits( ProgramStageInstance programStageInstance, AuditType auditType,
        int first, int max )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<ProgramStageInstanceAudit> query = builder.createQuery( ProgramStageInstanceAudit.class );
        Root<ProgramStageInstanceAudit> root = query.from( ProgramStageInstanceAudit.class );
        query.select( root );

        List<Predicate> predicates = getProgramStageInstanceAuditCriteria( programStageInstance,
            auditType, builder, root, first, max );
        query.where( predicates.toArray( new Predicate[predicates.size()] ) );
        query.orderBy( builder.desc( root.get( "created" ) ) );

        return sessionFactory.getCurrentSession().createQuery( query )
            .setFirstResult( first )
            .setMaxResults( max )
            .getResultList();
    }

    List<Predicate> getProgramStageInstanceAuditCriteria( ProgramStageInstance programStageInstance,
        AuditType auditType,
        CriteriaBuilder builder, Root<ProgramStageInstanceAudit> root, int first, int max )
    {
        List<Predicate> predicates = new ArrayList<>();

        if ( programStageInstance != null )
        {
            Expression<DataElement> psiExpression = root.get( "programStageInstance" );
            Predicate psiPredicate = psiExpression.in( programStageInstance );
            predicates.add( psiPredicate );
        }

        return predicates;
    }

}
