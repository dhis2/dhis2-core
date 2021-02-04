package org.hisp.dhis.category.hibernate;

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

import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 */
@Repository( "org.hisp.dhis.category.CategoryOptionComboStore" )
public class HibernateCategoryOptionComboStore
    extends HibernateIdentifiableObjectStore<CategoryOptionCombo>
    implements CategoryOptionComboStore
{
    private DbmsManager dbmsManager;

    public HibernateCategoryOptionComboStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService,
        AclService aclService, DbmsManager dbmsManager )
    {
        super( sessionFactory, jdbcTemplate, publisher, CategoryOptionCombo.class, currentUserService, aclService, true );
        this.dbmsManager = dbmsManager;
    }

    @Override
    public CategoryOptionCombo getCategoryOptionCombo( CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions )
    {
        String hql = "from CategoryOptionCombo co where co.categoryCombo = :categoryCombo";

        for ( CategoryOption option : categoryOptions )
        {
            hql += " and :option" + option.getId() + " in elements (co.categoryOptions)";
        }

        Query<CategoryOptionCombo> query = getQuery( hql );

        query.setParameter( "categoryCombo", categoryCombo );

        for ( CategoryOption option : categoryOptions )
        {
            query.setParameter( "option" + option.getId(), option );
        }

        return query.uniqueResult();
    }

    @Override
    public void updateNames()
    {
        List<CategoryOptionCombo> categoryOptionCombos = getQuery( "from CategoryOptionCombo co where co.name is null" ).list();
        int counter = 0;

        Session session = getSession();

        for ( CategoryOptionCombo coc : categoryOptionCombos )
        {
            session.update( coc );

            if ( ( counter % 400 ) == 0 )
            {
                dbmsManager.clearSession();
            }
        }
    }

    @Override
    public List<CategoryOptionCombo> getCategoryOptionCombosByGroupUid( String groupUid )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<CategoryOptionCombo> query = builder.createQuery( CategoryOptionCombo.class );
        Root<CategoryOptionCombo> root = query.from( CategoryOptionCombo.class );
        Join<Object, Object> joinCatOption = root.join( "categoryOptions", JoinType.INNER );
        Join<Object, Object> joinCatOptionGroup = joinCatOption.join( "groups", JoinType.INNER );
        query.where( builder.equal( joinCatOptionGroup.get( "uid" ), groupUid ) );
        return getSession().createQuery( query ).list();
    }

    @Override
    public void deleteNoRollBack( CategoryOptionCombo categoryOptionCombo )
    {
        ObjectDeletionRequestedEvent event = new ObjectDeletionRequestedEvent( categoryOptionCombo );
        event.setShouldRollBack( false );

        publisher.publishEvent( event );

        getSession().delete( categoryOptionCombo );
    }
}
