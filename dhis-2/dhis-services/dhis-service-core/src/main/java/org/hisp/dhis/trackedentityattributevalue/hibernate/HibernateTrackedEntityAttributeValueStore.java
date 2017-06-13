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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueStore;

/**
 * @author Abyot Asalefew
 */
public class HibernateTrackedEntityAttributeValueStore
    extends HibernateGenericStore<TrackedEntityAttributeValue>
    implements TrackedEntityAttributeValueStore
{
    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void saveVoid( TrackedEntityAttributeValue attributeValue )
    {
        sessionFactory.getCurrentSession().save( attributeValue );
    }

    @Override
    public int deleteByTrackedEntityInstance( TrackedEntityInstance entityInstance )
    {
        Query query = getQuery( "delete from TrackedEntityAttributeValue where entityInstance = :entityInstance" );
        query.setEntity( "entityInstance", entityInstance );
        return query.executeUpdate();
    }

    @Override
    public TrackedEntityAttributeValue get( TrackedEntityInstance entityInstance, TrackedEntityAttribute attribute )
    {
        return (TrackedEntityAttributeValue) getCriteria( 
            Restrictions.eq( "entityInstance", entityInstance ),
            Restrictions.eq( "attribute", attribute ) ).uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityAttributeValue> get( TrackedEntityInstance entityInstance )
    {
        return getCriteria( Restrictions.eq( "entityInstance", entityInstance ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityAttributeValue> get( TrackedEntityAttribute attribute )
    {
        return getCriteria( Restrictions.eq( "attribute", attribute ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityAttributeValue> get( Collection<TrackedEntityInstance> entityInstances )
    {
        if ( entityInstances == null || entityInstances.isEmpty() )
        {
            return new ArrayList<>();
        }
        
        return getCriteria( Restrictions.in( "entityInstance", entityInstances ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityAttributeValue> searchByValue( TrackedEntityAttribute attribute, String searchText )
    {
        return getCriteria( 
            Restrictions.eq( "attribute", attribute ),
            Restrictions.ilike( "plainValue", "%" + searchText + "%" ) ).list();
    }
    
    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityAttributeValue> get( TrackedEntityAttribute attribute, String value )
    {
        return getCriteria( 
            Restrictions.eq( "attribute", attribute ),
            Restrictions.ilike( "plainValue", value ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityAttributeValue> get( TrackedEntityInstance entityInstance, Program program )
    {
        return getCriteria(
            Restrictions.and( Restrictions.eq( "entityInstance", entityInstance ),
            Restrictions.eq( "attribute.program", program ) ) ).list();
    }
}
