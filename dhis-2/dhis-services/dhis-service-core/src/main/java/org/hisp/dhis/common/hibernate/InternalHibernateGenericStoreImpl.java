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
package org.hisp.dhis.common.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This class contains methods for generating predicates which are used for
 * validating sharing access permission.
 */
@Slf4j
public class InternalHibernateGenericStoreImpl<T extends BaseIdentifiableObject>
    extends HibernateGenericStore<T>
    implements InternalHibernateGenericStore<T>
{
    protected AclService aclService;

    protected CurrentUserService currentUserService;

    public InternalHibernateGenericStoreImpl( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, Class<T> clazz, AclService aclService,
        CurrentUserService currentUserService, boolean cacheable )
    {
        super( sessionFactory, jdbcTemplate, publisher, clazz, cacheable );

        checkNotNull( aclService );
        checkNotNull( currentUserService );
        this.aclService = aclService;
        this.currentUserService = currentUserService;
    }

    /**
     * Get Predicate for checking Sharing access for given User's uid and
     * UserGroup Uids
     *
     * @param builder CriteriaBuilder
     * @param userUid User Uid for checking access
     * @param userGroupUids List of UserGroup Uid which given user belong to
     * @param access Access String for checking
     *
     * @return List of {@link Predicate}
     */
    protected List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, String userUid,
        Set<String> userGroupUids,
        String access )
    {
        List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

        Function<Root<T>, Predicate> userGroupPredicate = JpaQueryUtils.checkUserGroupsAccess( builder, userGroupUids,
            access );

        Function<Root<T>, Predicate> userPredicate = JpaQueryUtils.checkUserAccess( builder, userUid, access );

        predicates.add( root -> {
            Predicate disjunction = builder.or(
                builder.like( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "public" ) ), access ),
                builder.equal( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "public" ) ), "null" ),
                builder.isNull( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "public" ) ) ),
                builder.isNull( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "owner" ) ) ),
                builder.equal( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "owner" ) ), "null" ),
                builder.equal( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "owner" ) ), userUid ),
                userPredicate.apply( root ) );

            Predicate ugPredicateWithRoot = userGroupPredicate.apply( root );

            if ( ugPredicateWithRoot != null )
            {
                return builder.or( disjunction, ugPredicateWithRoot );
            }

            return disjunction;
        } );

        return predicates;
    }

    /**
     * Get Predicate for checking Data Sharing access for given User's uid and
     * UserGroup Uids
     *
     * @param builder CriteriaBuilder
     * @param userUid User Uid for checking access
     * @param userGroupUids List of UserGroup Uid which given user belong to
     * @param access Access String for checking
     *
     * @return List of {@link Predicate}
     */
    public List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, String userUid,
        Set<String> userGroupUids, String access )
    {
        List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

        preProcessPredicates( builder, predicates );

        Function<Root<T>, Predicate> userGroupPredicate = JpaQueryUtils.checkUserGroupsAccess( builder, userGroupUids,
            access );

        Function<Root<T>, Predicate> userPredicate = JpaQueryUtils.checkUserAccess( builder, userUid, access );

        predicates.add( root -> {
            Predicate disjunction = builder.or(
                builder.like( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "public" ) ), access ),
                builder.equal( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "public" ) ), "null" ),
                builder.isNull( builder.function( JsonbFunctions.EXTRACT_PATH_TEXT, String.class, root.get( "sharing" ),
                    builder.literal( "public" ) ) ),
                userPredicate.apply( root ) );

            Predicate ugPredicateWithRoot = userGroupPredicate.apply( root );

            if ( ugPredicateWithRoot != null )
            {
                return builder.or( disjunction, ugPredicateWithRoot );
            }

            return disjunction;
        } );

        return predicates;
    }

    @Override
    public List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User user )
    {
        return getDataSharingPredicates( builder, user, currentUserService.getCurrentUserGroupsInfo( user ),
            AclService.LIKE_READ_DATA );
    }

    @Override
    public final List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder )
    {
        CurrentUserGroupInfo currentUserGroupsInfo = currentUserService.getCurrentUserGroupsInfo();
        return getSharingPredicates( builder, currentUserService.getCurrentUser(),
            currentUserGroupsInfo, AclService.LIKE_READ_METADATA );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User user )
    {
        CurrentUserGroupInfo currentUserGroupsInfo = currentUserService.getCurrentUserGroupsInfo( user );
        return getSharingPredicates( builder, user, currentUserGroupsInfo,
            AclService.LIKE_READ_METADATA );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User user, String access )
    {
        if ( !sharingEnabled( user ) || user == null )
        {
            return new ArrayList<>();
        }

        Set<String> groupIds = user.getGroups().stream().map( g -> g.getUid() ).collect( Collectors.toSet() );

        return getSharingPredicates( builder, user.getUid(), groupIds, access );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User user,
        CurrentUserGroupInfo groupInfo, String access )
    {
        List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

        if ( user == null || !dataSharingEnabled( user ) || groupInfo == null )
        {
            return predicates;
        }

        return getDataSharingPredicates( builder, groupInfo.getUserUID(), groupInfo.getUserGroupUIDs(), access );
    }

    @Override
    public List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User user,
        String access )
    {
        List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

        if ( user == null || !dataSharingEnabled( user ) )
        {
            return predicates;
        }

        Set<String> groupIds = user.getGroups().stream().map( g -> g.getUid() ).collect( Collectors.toSet() );

        return getDataSharingPredicates( builder, user.getUid(), groupIds, access );
    }

    protected boolean forceAcl()
    {
        return Dashboard.class.isAssignableFrom( clazz );
    }

    /**
     * @deprecated use {@link #sharingEnabled( CurrentUserDetails )} instead.
     */
    @Deprecated
    protected boolean sharingEnabled( User user )
    {
        boolean b = forceAcl();

        if ( b )
        {
            return b;
        }
        else
        {
            return (aclService.isClassShareable( clazz ) && !(user == null || user.isSuper()));
        }
    }

    protected boolean sharingEnabled( CurrentUserDetails user )
    {
        boolean b = forceAcl();

        if ( b )
        {
            return b;
        }
        else
        {
            return (aclService.isClassShareable( clazz ) && !(user == null || user.isSuper()));
        }
    }

    protected boolean dataSharingEnabled( CurrentUserDetails user )
    {
        return aclService.isDataClassShareable( clazz ) && !user.isSuper();
    }

    /**
     * @deprecated use {@link #dataSharingEnabled( CurrentUserDetails )}
     *             instead.
     */
    @Deprecated
    private boolean dataSharingEnabled( User user )
    {
        return aclService.isDataClassShareable( clazz ) && !user.isSuper();
    }

    private List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User user,
        CurrentUserGroupInfo groupInfo, String access )
    {
        if ( user == null || groupInfo == null || !sharingEnabled( user ) )
        {
            return new ArrayList<>( 0 );
        }

        return getSharingPredicates( builder, groupInfo.getUserUID(), groupInfo.getUserGroupUIDs(), access );
    }
}
