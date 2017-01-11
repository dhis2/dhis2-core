package org.hisp.dhis.trackedentitydatavalue.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditStore;

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
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( List<DataElement> dataElements,
        List<ProgramStageInstance> programStageInstances, AuditType auditType )
    {
        Criteria criteria = getTrackedEntityDataValueAuditCriteria( dataElements, programStageInstances, auditType );
        criteria.addOrder( Order.desc( "created" ) );

        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( List<DataElement> dataElements,
        List<ProgramStageInstance> programStageInstances, AuditType auditType, int first, int max )
    {
        Criteria criteria = getTrackedEntityDataValueAuditCriteria( dataElements, programStageInstances, auditType );
        criteria.addOrder( Order.desc( "created" ) );
        criteria.setFirstResult( first );
        criteria.setMaxResults( max );

        return criteria.list();
    }

    @Override
    public int countTrackedEntityDataValueAudits( List<DataElement> dataElements, List<ProgramStageInstance> programStageInstances, AuditType auditType )
    {
        return ((Number) getTrackedEntityDataValueAuditCriteria( dataElements, programStageInstances, auditType )
            .setProjection( Projections.countDistinct( "id" ) ).uniqueResult()).intValue();
    }
    
    @Override
    public void deleteTrackedEntityDataValueAudits( ProgramStageInstance programStageInstance )
    {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery( "delete TrackedEntityDataValueAudit where programStageInstance = :programStageInstance" );
        query.setEntity( "programStageInstance", programStageInstance );
        query.executeUpdate();
    }

    private Criteria getTrackedEntityDataValueAuditCriteria( List<DataElement> dataElements, List<ProgramStageInstance> programStageInstances,
        AuditType auditType )
    {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria( TrackedEntityDataValueAudit.class );

        if ( !dataElements.isEmpty() )
        {
            criteria.add( Restrictions.in( "dataElement", dataElements ) );
        }

        if ( !programStageInstances.isEmpty() )
        {
            criteria.add( Restrictions.in( "programStageInstance", programStageInstances ) );
        }

        if ( auditType != null )
        {
            criteria.add( Restrictions.eq( "auditType", auditType ) );
        }

        return criteria;
    }
}
