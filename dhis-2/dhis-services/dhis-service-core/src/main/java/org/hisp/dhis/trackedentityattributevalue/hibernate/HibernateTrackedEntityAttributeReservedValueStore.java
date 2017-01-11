package org.hisp.dhis.trackedentityattributevalue.hibernate;
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

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeReservedValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeReservedValueStore;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * @author Markus Bekken
 */
public class HibernateTrackedEntityAttributeReservedValueStore
    implements TrackedEntityAttributeReservedValueStore
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
    public TrackedEntityAttributeReservedValue saveTrackedEntityAttributeReservedValue(
        TrackedEntityAttributeReservedValue trackedEntityAttributeReservedValue )
    {
        trackedEntityAttributeReservedValue.setAutoFields();
        
        Session session = sessionFactory.getCurrentSession();
        session.save( trackedEntityAttributeReservedValue );
        session.flush();
        
        return trackedEntityAttributeReservedValue;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityAttributeReservedValue> getTrackedEntityReservedValues(
        TrackedEntityAttribute trackedEntityAttribute, String value )
    {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria( TrackedEntityAttributeReservedValue.class );
        criteria.add( Restrictions.eq( "trackedEntityAttribute", trackedEntityAttribute ) );
        criteria.add( Restrictions.eq( "value", value ) );
        criteria.setMaxResults( 2 );
        return criteria.list();
    }
    
}
