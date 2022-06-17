/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.trackedentitydatavalue.hibernate;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityDataValueAuditQueryParams;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditStore;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository( "org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditStore" )
public class HibernateTrackedEntityDataValueAuditStore
    implements TrackedEntityDataValueAuditStore
{
    private static final String PROP_PSI = "programStageInstance";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SessionFactory sessionFactory;

    public HibernateTrackedEntityDataValueAuditStore( SessionFactory sessionFactory )
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
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits(
        TrackedEntityDataValueAuditQueryParams params )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<TrackedEntityDataValueAudit> criteria = builder.createQuery( TrackedEntityDataValueAudit.class );
        Root<TrackedEntityDataValueAudit> tedva = criteria.from( TrackedEntityDataValueAudit.class );
        Join<TrackedEntityDataValueAudit, ProgramStageInstance> psi = tedva.join( PROP_PSI );
        criteria.select( tedva );

        List<Predicate> predicates = getTrackedEntityDataValueAuditCriteria( params, builder, tedva, psi );
        criteria.where( predicates.toArray( new Predicate[0] ) );
        criteria.orderBy( builder.desc( tedva.get( "created" ) ) );

        Query query = sessionFactory.getCurrentSession().createQuery( criteria );

        if ( params.hasPaging() )
        {
            query
                .setFirstResult( params.getPager().getOffset() )
                .setMaxResults( params.getPager().getPageSize() );
        }

        return query.getResultList();
    }

    @Override
    public int countTrackedEntityDataValueAudits( TrackedEntityDataValueAuditQueryParams params )
    {
        CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery( Long.class );
        Root<TrackedEntityDataValueAudit> tedva = criteria.from( TrackedEntityDataValueAudit.class );
        Join<TrackedEntityDataValueAudit, ProgramStageInstance> psi = tedva.join( PROP_PSI );
        criteria.select( builder.countDistinct( tedva.get( "id" ) ) );

        List<Predicate> predicates = getTrackedEntityDataValueAuditCriteria( params, builder, tedva, psi );
        criteria.where( predicates.toArray( new Predicate[predicates.size()] ) );

        return sessionFactory.getCurrentSession().createQuery( criteria ).getSingleResult().intValue();
    }

    @Override
    public void deleteTrackedEntityDataValueAudit( DataElement dataElement )
    {
        String hql = "delete from TrackedEntityDataValueAudit d where d.dataElement = :de";

        sessionFactory.getCurrentSession().createQuery( hql ).setParameter( "de", dataElement ).executeUpdate();
    }

    @Override
    public void deleteTrackedEntityDataValueAudit( ProgramStageInstance psi )
    {
        String hql = "delete from TrackedEntityDataValueAudit d where d.programStageInstance = :psi";

        sessionFactory.getCurrentSession().createQuery( hql ).setParameter( "psi", psi ).executeUpdate();
    }

    private List<Predicate> getTrackedEntityDataValueAuditCriteria( TrackedEntityDataValueAuditQueryParams params,
        CriteriaBuilder builder,
        Root<TrackedEntityDataValueAudit> tedva,
        Join<TrackedEntityDataValueAudit, ProgramStageInstance> psi )
    {
        List<Predicate> predicates = new ArrayList<>();

        if ( !params.getDataElements().isEmpty() )
        {
            predicates.add( tedva.get( "dataElement" ).in( params.getDataElements() ) );
        }

        if ( !params.getOrgUnits().isEmpty() )
        {
            predicates.add( psi.get( "organisationUnit" ).in( params.getOrgUnits() ) );
        }

        if ( !params.getProgramStageInstances().isEmpty() )
        {
            predicates.add( tedva.get( PROP_PSI ).in( params.getProgramStageInstances() ) );
        }

        if ( !params.getProgramStages().isEmpty() )
        {
            predicates.add( psi.get( "programStage" ).in( params.getProgramStages() ) );
        }

        if ( params.getAuditType() != null )
        {
            predicates.add( builder.equal( tedva.get( "auditType" ), params.getAuditType() ) );
        }

        return predicates;
    }
}
