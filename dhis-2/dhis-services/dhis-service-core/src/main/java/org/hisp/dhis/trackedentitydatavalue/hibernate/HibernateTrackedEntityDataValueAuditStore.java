package org.hisp.dhis.trackedentitydatavalue.hibernate;

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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditStore;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateTrackedEntityDataValueAuditStore
    implements TrackedEntityDataValueAuditStore
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
    public void addTrackedEntityDataValueAudit( TrackedEntityDataValueAudit trackedEntityDataValueAudit )
    {
        Session session = sessionFactory.getCurrentSession();
        session.save( trackedEntityDataValueAudit );
    }

    @Override
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( List<DataElement> dataElements,
        List<ProgramStageInstance> programStageInstances, AuditType auditType )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<TrackedEntityDataValueAudit> query = builder.createQuery( TrackedEntityDataValueAudit.class );
        Root<TrackedEntityDataValueAudit> root = query.from( TrackedEntityDataValueAudit.class );
        query.select( root );
        query = getTrackedEntityDataValueAuditCriteria( dataElements, programStageInstances, auditType, builder, query, root );
        query.orderBy( builder.desc( root.get( "created" ) ) );

        return sessionFactory.getCurrentSession().createQuery( query ).getResultList();
    }

    @Override
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( List<DataElement> dataElements,
        List<ProgramStageInstance> programStageInstances, AuditType auditType, int first, int max )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<TrackedEntityDataValueAudit> query = builder.createQuery( TrackedEntityDataValueAudit.class );
        Root<TrackedEntityDataValueAudit> root = query.from( TrackedEntityDataValueAudit.class );
        query.select( root );
        query = getTrackedEntityDataValueAuditCriteria( dataElements, programStageInstances, auditType, builder, query, root );
        query.orderBy( builder.desc( root.get( "created" ) ) );

        return sessionFactory.getCurrentSession().createQuery( query )
                .setFirstResult( first )
                .setMaxResults( max )
                .getResultList();
    }

    @Override
    public int countTrackedEntityDataValueAudits( List<DataElement> dataElements, List<ProgramStageInstance> programStageInstances, AuditType auditType ) {

        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery( Long.class );
        Root<TrackedEntityDataValueAudit> root = query.from( TrackedEntityDataValueAudit.class );
        query.select( builder.countDistinct( root.get( "id" ) ) );
        query = getTrackedEntityDataValueAuditCriteria(dataElements, programStageInstances, auditType, builder, query, root );

        return sessionFactory.getCurrentSession().createQuery( query ).getSingleResult().intValue();
    }

    private CriteriaQuery getTrackedEntityDataValueAuditCriteria( List<DataElement> dataElements, List<ProgramStageInstance> programStageInstances,
        AuditType auditType, CriteriaBuilder builder,  CriteriaQuery query, Root<TrackedEntityDataValueAudit> root )
    {
        if ( dataElements != null && !dataElements.isEmpty() )
        {
            Expression<DataElement> dataElementExpression = root.get( "dataElement" );
            Predicate dataElementPredicate = dataElementExpression.in( dataElements );
            query.where( dataElementPredicate );
        }

        if ( programStageInstances != null && !programStageInstances.isEmpty() )
        {
            Expression<DataElement> psiExpression = root.get( "programStageInstance" );
            Predicate psiPredicate = psiExpression.in( programStageInstances );
            query.where( psiPredicate );
        }

        if ( auditType != null )
        {
            query.where( builder.equal( root.get( "auditType" ), auditType ) );
        }

        return query;
    }
}
