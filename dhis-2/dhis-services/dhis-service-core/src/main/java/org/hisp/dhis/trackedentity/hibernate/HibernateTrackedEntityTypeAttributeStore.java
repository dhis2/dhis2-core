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
package org.hisp.dhis.trackedentity.hibernate;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttributeStore;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository( "org.hisp.dhis.program.TrackedEntityTypeAttributeStore" )
public class HibernateTrackedEntityTypeAttributeStore
    extends HibernateIdentifiableObjectStore<TrackedEntityTypeAttribute>
    implements TrackedEntityTypeAttributeStore
{
    public HibernateTrackedEntityTypeAttributeStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, DeletedObjectService deletedObjectService,
        AclService aclService, StatementBuilder statementBuilder )
    {
        super( sessionFactory, jdbcTemplate, publisher, TrackedEntityTypeAttribute.class, currentUserService, deletedObjectService,
            aclService, true );
    }

    @Override
    public List<TrackedEntityAttribute> getAttributes( List<TrackedEntityType> trackedEntityTypes )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<TrackedEntityAttribute> query = builder.createQuery( TrackedEntityAttribute.class );
        Root<TrackedEntityTypeAttribute> root = query.from( TrackedEntityTypeAttribute.class );
        query.select( root.get( "trackedEntityAttribute" ) );
        query.where( root.get( "trackedEntityType" ).in( trackedEntityTypes ) );
        query.distinct( true );

        return getSession().createQuery( query ).getResultList();
    }
}
