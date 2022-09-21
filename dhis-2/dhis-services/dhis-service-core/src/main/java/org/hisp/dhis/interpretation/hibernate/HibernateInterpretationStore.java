/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.interpretation.hibernate;

package org.hisp.dhis.interpretation.hibernate;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationStore;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 */
@Repository( "org.hisp.dhis.interpretation.InterpretationStore" )
public class HibernateInterpretationStore
    extends HibernateIdentifiableObjectStore<Interpretation> implements InterpretationStore
{
    @Autowired
    public HibernateInterpretationStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
<<<<<<< HEAD
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService )
=======
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, DeletedObjectService deletedObjectService, AclService aclService )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        super( sessionFactory, jdbcTemplate, publisher, Interpretation.class, currentUserService, aclService, false );
    }

    public List<Interpretation> getInterpretations( User user )
    {
        String hql = "select distinct i from Interpretation i left join i.comments c " +
            "where i.user = :user or c.user = :user order by i.lastUpdated desc";

        Query<Interpretation> query = getQuery( hql )
            .setParameter( "user", user )
            .setCacheable( cacheable );

        return query.list();
    }

    public List<Interpretation> getInterpretations( User user, int first, int max )
    {
        String hql = "select distinct i from Interpretation i left join i.comments c " +
            "where i.user = :user or c.user = :user order by i.lastUpdated desc";

        Query<Interpretation> query = getQuery( hql )
            .setParameter( "user", user )
            .setMaxResults( first )
            .setMaxResults( max )
            .setCacheable( cacheable );

        return query.list();
    }

    @Override
    public long countMapInterpretations( Map map )
    {
        Query<Long> query = getTypedQuery( "select count(distinct c) from " + clazz.getName() + " c where c.map=:map" );
        query.setParameter( "map", map );
        return query.uniqueResult();
    }

    @Override
    public long countVisualizationInterpretations( Visualization visualization )
    {
<<<<<<< HEAD
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where c.visualization=:visualization" )
            .setParameter( "visualization", visualization )
            .setCacheable( cacheable );
=======
        Query query = getQuery(
            "select count(distinct c) from " + clazz.getName() + " c where c.visualization=:visualization" )
                .setParameter( "visualization", visualization )
                .setCacheable( cacheable );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public Interpretation getByVisualizationId( long id )
    {
        String hql = "from Interpretation i where i.visualization.id = " + id;
        Query<Interpretation> query = getQuery( hql );
        return query.uniqueResult();
    }
}
