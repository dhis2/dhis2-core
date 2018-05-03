package org.hisp.dhis.trackedentityattributevalue.hibernate;

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

import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.query.Query;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditStore;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateTrackedEntityAttributeValueAuditStore
    implements TrackedEntityAttributeValueAuditStore
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
    public void addTrackedEntityAttributeValueAudit( TrackedEntityAttributeValueAudit trackedEntityAttributeValueAudit )
    {
        Session session = sessionFactory.getCurrentSession();
        session.save( trackedEntityAttributeValueAudit );
    }

    @Override
    public List<TrackedEntityAttributeValueAudit> getTrackedEntityAttributeValueAudits( List<TrackedEntityAttribute> trackedEntityAttributes,
        List<TrackedEntityInstance> trackedEntityInstances, AuditType auditType )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();

        CriteriaQuery<TrackedEntityAttributeValueAudit>  query = builder.createQuery( TrackedEntityAttributeValueAudit.class );

        Root<TrackedEntityAttributeValueAudit> root = query.from( TrackedEntityAttributeValueAudit.class );

        List<Predicate> predicates = getTrackedEntityAttributeValueAuditCriteria( builder, root, trackedEntityAttributes, trackedEntityInstances, auditType );

        query.where( predicates.toArray( new Predicate[0] ) )
            .orderBy( builder.desc( root.get( "created" ) ) );

        return sessionFactory.getCurrentSession()
                            .createQuery( query )
                            .getResultList();
    }

    @Override
    public List<TrackedEntityAttributeValueAudit> getTrackedEntityAttributeValueAudits( List<TrackedEntityAttribute> trackedEntityAttributes,
        List<TrackedEntityInstance> trackedEntityInstances, AuditType auditType, int first, int max )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();

        CriteriaQuery<TrackedEntityAttributeValueAudit>  query = builder.createQuery( TrackedEntityAttributeValueAudit.class );

        Root<TrackedEntityAttributeValueAudit> root = query.from( TrackedEntityAttributeValueAudit.class );

        List<Predicate> predicates = getTrackedEntityAttributeValueAuditCriteria( builder, root, trackedEntityAttributes, trackedEntityInstances, auditType );

        query.where( predicates.toArray( new Predicate[0] ) )
            .orderBy( builder.desc( root.get( "created" ) ) );

        return sessionFactory.getCurrentSession()
                .createQuery( query )
                .setFirstResult( first )
                .setMaxResults( max ).getResultList();
    }

    @Override
    public int countTrackedEntityAttributeValueAudits( List<TrackedEntityAttribute> trackedEntityAttributes,
        List<TrackedEntityInstance> trackedEntityInstances, AuditType auditType )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();

        CriteriaQuery<Long>  query = builder.createQuery( Long.class );

        Root<TrackedEntityAttributeValueAudit> root = query.from( TrackedEntityAttributeValueAudit.class );

        List<Predicate> predicates = getTrackedEntityAttributeValueAuditCriteria( builder, root, trackedEntityAttributes, trackedEntityInstances, auditType );

        query.select( builder.countDistinct( root.get( "id" ) ) )
             .where( predicates.toArray( new Predicate[0] ) )
             .orderBy( builder.desc( root.get( "created" ) ) );

        return ( sessionFactory.getCurrentSession()
                                .createQuery( query )
                                .uniqueResult() ).intValue();
    }

    @Override
    public void deleteTrackedEntityAttributeValueAudits( TrackedEntityInstance entityInstance )
    {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery( "delete TrackedEntityAttributeValueAudit where entityInstance = :entityInstance" );
        query.setParameter( "entityInstance", entityInstance );
        query.executeUpdate();
    }
    
    private List<Predicate> getTrackedEntityAttributeValueAuditCriteria( CriteriaBuilder builder, Root<TrackedEntityAttributeValueAudit> root, List<TrackedEntityAttribute> trackedEntityAttributes, List<TrackedEntityInstance> trackedEntityInstances, AuditType auditType )
    {
        List<Predicate> predicates = new ArrayList<>();

        if ( !trackedEntityAttributes.isEmpty() )
        {
            predicates.add( root.get( "attribute" ).in( trackedEntityAttributes ) );
        }

        if ( !trackedEntityInstances.isEmpty() )
        {
            predicates.add(  root.get( "entityInstance" ).in( trackedEntityInstances ) );
        }

        if ( auditType != null )
        {
            predicates.add(  builder.equal( root.get( "auditType" ), auditType ) );
        }

        return predicates;
    }
}
