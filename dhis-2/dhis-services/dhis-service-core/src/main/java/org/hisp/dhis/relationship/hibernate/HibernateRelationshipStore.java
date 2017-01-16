package org.hisp.dhis.relationship.hibernate;

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

import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew
 */
public class HibernateRelationshipStore
    extends HibernateGenericStore<Relationship>
    implements RelationshipStore
{
    @Override
    @SuppressWarnings( "unchecked" )
    public List<Relationship> getForTrackedEntityInstance( TrackedEntityInstance instance )
    {
        return getCriteria( 
            Restrictions.disjunction().add( 
            Restrictions.eq( "entityInstanceA", instance ) ).add(
            Restrictions.eq( "entityInstanceB", instance ) ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Relationship> getByRelationshipType( RelationshipType relationshipType )
    {
        return getCriteria( Restrictions.eq( "relationshipType", relationshipType ) ).list();
    }

    @SuppressWarnings( "unchecked" )
    public Collection<Relationship> get( TrackedEntityInstance entityInstanceA, RelationshipType relationshipType )
    {
        return getCriteria( 
            Restrictions.eq( "entityInstanceA", entityInstanceA ),
            Restrictions.eq( "relationshipType", relationshipType ) ).list();
    }

    @Override
    public Relationship get( TrackedEntityInstance entityInstanceA, TrackedEntityInstance entityInstanceB, RelationshipType relationshipType )
    {
        return (Relationship) getCriteria( 
            Restrictions.eq( "entityInstanceA", entityInstanceA ),
            Restrictions.eq( "entityInstanceB", entityInstanceB ), 
            Restrictions.eq( "relationshipType", relationshipType ) ).uniqueResult();
    }
}
