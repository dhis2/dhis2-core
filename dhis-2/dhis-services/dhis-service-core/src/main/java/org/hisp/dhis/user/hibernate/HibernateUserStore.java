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
package org.hisp.dhis.user.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.QueryHints;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Nguyen Hong Duc
 */
@Slf4j
@Repository( "org.hisp.dhis.user.UserStore" )
public class HibernateUserStore
    extends HibernateIdentifiableObjectStore<User>
    implements UserStore
{
    public static final String DISABLED_COLUMN = "disabled";

    private final SchemaService schemaService;

    public HibernateUserStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService,
        AclService aclService, SchemaService schemaService )
    {
        super( sessionFactory, jdbcTemplate, publisher, User.class, currentUserService, aclService, true );

        checkNotNull( schemaService );
        this.schemaService = schemaService;
    }

    @Override
    public void save( User user, boolean clearSharing )
    {
        super.save( user, clearSharing );

        currentUserService.invalidateUserGroupCache( user.getUsername() );
    }

    @Override
    public List<User> getUsers( UserQueryParams params, @Nullable List<String> orders )
    {
        return extractUserQueryUsers( getUserQuery( params, orders, false ).list() );
    }

    @Override
    public List<User> getUsers( UserQueryParams params )
    {
        return getUsers( params, null );
    }

    @Override
    public List<User> getExpiringUsers( UserQueryParams params )
    {
        return extractUserQueryUsers( getUserQuery( params, null, false ).list() );
    }

    @Override
    public int getUserCount( UserQueryParams params )
    {
        Long count = (Long) getUserQuery( params, null, true ).uniqueResult();
        return count != null ? count.intValue() : 0;
    }

    @Nonnull
    private List<User> extractUserQueryUsers( @Nonnull List<?> result )
    {
        if ( result.isEmpty() )
        {
            return Collections.emptyList();
        }

        final List<User> users = new ArrayList<>( result.size() );
        for ( Object o : result )
        {
            if ( o instanceof User )
            {
                users.add( (User) o );
            }
            else if ( o.getClass().isArray() )
            {
                users.add( (User) ((Object[]) o)[0] );
            }
        }
        return users;
    }

    private Query getUserQuery( UserQueryParams params, List<String> orders, boolean count )
    {
        SqlHelper hlp = new SqlHelper();

        List<Order> convertedOrder = null;
        String hql = null;

        if ( count )
        {
            hql = "select count(distinct u) ";
        }
        else
        {
            Schema userSchema = schemaService.getSchema( User.class );
            convertedOrder = QueryUtils.convertOrderStrings( orders, userSchema );

            hql = Stream.of( "select distinct u", JpaQueryUtils.createSelectOrderExpression( convertedOrder, "u" ) )
                .filter( Objects::nonNull ).collect( Collectors.joining( "," ) );
            hql += " ";
        }

        hql += "from User u ";

        if ( count )
        {
            hql += "inner join u.userCredentials uc ";
        }
        else
        {
            hql += "inner join fetch u.userCredentials uc ";
        }

        if ( params.isPrefetchUserGroups() && !count )
        {
            hql += "left join fetch u.groups g ";
        }
        else
        {
            hql += "left join u.groups g ";
        }

        if ( params.hasOrganisationUnits() )
        {
            hql += "left join u.organisationUnits ou ";

            if ( params.isIncludeOrgUnitChildren() )
            {
                hql += hlp.whereAnd() + " (";

                for ( OrganisationUnit ou : params.getOrganisationUnits() )
                {
                    hql += format( "ou.path like :ou%s or ", ou.getUid() );
                }

                hql = TextUtils.removeLastOr( hql ) + ")";
            }
            else
            {
                hql += hlp.whereAnd() + " ou.id in (:ouIds) ";
            }
        }

        if ( params.hasDataViewOrganisationUnits() )
        {
            hql += "left join u.dataViewOrganisationUnits dwou ";

            if ( params.isIncludeOrgUnitChildren() )
            {
                hql += hlp.whereAnd() + " (";

                for ( OrganisationUnit ou : params.getDataViewOrganisationUnits() )
                {
                    hql += format( "dwou.path like :dwOu%s or ", ou.getUid() );
                }

                hql = TextUtils.removeLastOr( hql ) + ")";
            }
            else
            {
                hql += hlp.whereAnd() + " dwou.id in (:dwOuIds) ";
            }
        }

        if ( params.hasTeiSearchOrganisationUnits() )
        {
            hql += "left join u.teiSearchOrganisationUnits tsou ";

            if ( params.isIncludeOrgUnitChildren() )
            {
                hql += hlp.whereAnd() + " (";

                for ( OrganisationUnit ou : params.getTeiSearchOrganisationUnits() )
                {
                    hql += format( "tsou.path like :tsOu%s or ", ou.getUid() );
                }

                hql = TextUtils.removeLastOr( hql ) + ")";
            }
            else
            {
                hql += hlp.whereAnd() + " tsou.id in (:tsOuIds) ";
            }
        }

        if ( params.hasUserGroups() )
        {
            hql += hlp.whereAnd() + " g.id in (:userGroupIds) ";
        }

        if ( params.getDisabled() != null )
        {
            hql += hlp.whereAnd() + " uc.disabled = :disabled ";
        }

        if ( params.isNot2FA() )
        {
            hql += hlp.whereAnd() + " uc.secret is null ";
        }

        if ( params.getQuery() != null )
        {
            hql += hlp.whereAnd() + " (" +
                "concat(lower(u.firstName),' ',lower(u.surname)) like :key " +
                "or lower(u.email) like :key " +
                "or lower(uc.username) like :key) ";
        }

        if ( params.getPhoneNumber() != null )
        {
            hql += hlp.whereAnd() + " u.phoneNumber = :phoneNumber ";
        }

        if ( params.isCanManage() && params.getUser() != null )
        {
            hql += hlp.whereAnd() + " g.id in (:ids) ";
        }

        if ( params.isAuthSubset() && params.getUser() != null )
        {
            hql += hlp.whereAnd() + " not exists (" +
                "select uc2 from UserCredentials uc2 " +
                "inner join uc2.userAuthorityGroups ag2 " +
                "inner join ag2.authorities a " +
                "where uc2.id = uc.id " +
                "and a not in (:auths) ) ";
        }

        // TODO handle users with no user roles

        if ( params.isDisjointRoles() && params.getUser() != null )
        {
            hql += hlp.whereAnd() + " not exists (" +
                "select uc3 from UserCredentials uc3 " +
                "inner join uc3.userAuthorityGroups ag3 " +
                "where uc3.id = uc.id " +
                "and ag3.id in (:roles) ) ";
        }

        if ( params.getLastLogin() != null )
        {
            hql += hlp.whereAnd() + " uc.lastLogin >= :lastLogin ";
        }

        if ( params.getInactiveSince() != null )
        {
            hql += hlp.whereAnd() + " uc.lastLogin < :inactiveSince ";
        }

        if ( params.getPasswordLastUpdated() != null )
        {
            hql += hlp.whereAnd() + " uc.passwordLastUpdated < :passwordLastUpdated ";
        }

        if ( params.isSelfRegistered() )
        {
            hql += hlp.whereAnd() + " uc.selfRegistered = true ";
        }

        if ( UserInvitationStatus.ALL.equals( params.getInvitationStatus() ) )
        {
            hql += hlp.whereAnd() + " uc.invitation = true ";
        }

        if ( UserInvitationStatus.EXPIRED.equals( params.getInvitationStatus() ) )
        {
            hql += hlp.whereAnd() + " uc.invitation = true " +
                "and uc.restoreToken is not null " +
                "and uc.restoreExpiry is not null " +
                "and uc.restoreExpiry < current_timestamp() ";
        }

        if ( !count )
        {
            String orderExpression = JpaQueryUtils.createOrderExpression( convertedOrder, "u" );
            hql += "order by " + StringUtils.defaultString( orderExpression, "u.surname, u.firstName" );
        }

        // ---------------------------------------------------------------------
        // Query parameters
        // ---------------------------------------------------------------------

        log.debug( "User query HQL: '{}'", hql );

        Query query = getQuery( hql );

        if ( params.getQuery() != null )
        {
            query.setParameter( "key", "%" + params.getQuery().toLowerCase() + "%" );
        }

        if ( params.getPhoneNumber() != null )
        {
            query.setParameter( "phoneNumber", params.getPhoneNumber() );
        }

        if ( params.isCanManage() && params.getUser() != null )
        {
            Collection<Long> managedGroups = IdentifiableObjectUtils
                .getIdentifiers( params.getUser().getManagedGroups() );

            query.setParameterList( "ids", managedGroups );
        }

        if ( params.getDisabled() != null )
        {
            query.setParameter( DISABLED_COLUMN, params.getDisabled() );
        }

        if ( params.isAuthSubset() && params.getUser() != null )
        {
            Set<String> auths = params.getUser().getUserCredentials().getAllAuthorities();

            query.setParameterList( "auths", auths );
        }

        if ( params.isDisjointRoles() && params.getUser() != null )
        {
            Collection<Long> roles = IdentifiableObjectUtils
                .getIdentifiers( params.getUser().getUserCredentials().getUserAuthorityGroups() );

            query.setParameterList( "roles", roles );
        }

        if ( params.getLastLogin() != null )
        {
            query.setParameter( "lastLogin", params.getLastLogin() );
        }

        if ( params.getPasswordLastUpdated() != null )
        {
            query.setParameter( "passwordLastUpdated", params.getPasswordLastUpdated() );
        }

        if ( params.getInactiveSince() != null )
        {
            query.setParameter( "inactiveSince", params.getInactiveSince() );
        }

        if ( params.hasOrganisationUnits() )
        {
            if ( params.isIncludeOrgUnitChildren() )
            {
                for ( OrganisationUnit ou : params.getOrganisationUnits() )
                {
                    query.setParameter( format( "ou%s", ou.getUid() ), "%/" + ou.getUid() + "%" );
                }
            }
            else
            {
                Collection<Long> ouIds = IdentifiableObjectUtils.getIdentifiers( params.getOrganisationUnits() );

                query.setParameterList( "ouIds", ouIds );
            }
        }

        if ( params.hasDataViewOrganisationUnits() )
        {
            if ( params.isIncludeOrgUnitChildren() )
            {
                for ( OrganisationUnit ou : params.getDataViewOrganisationUnits() )
                {
                    query.setParameter( format( "dwOu%s", ou.getUid() ), "%/" + ou.getUid() + "%" );
                }
            }
            else
            {
                Collection<Long> ouIds = IdentifiableObjectUtils
                    .getIdentifiers( params.getDataViewOrganisationUnits() );

                query.setParameterList( "dwOuIds", ouIds );
            }
        }

        if ( params.hasTeiSearchOrganisationUnits() )
        {
            if ( params.isIncludeOrgUnitChildren() )
            {
                for ( OrganisationUnit ou : params.getTeiSearchOrganisationUnits() )
                {
                    query.setParameter( format( "tsOu%s", ou.getUid() ), "%/" + ou.getUid() + "%" );
                }
            }
            else
            {
                Collection<Long> ouIds = IdentifiableObjectUtils
                    .getIdentifiers( params.getTeiSearchOrganisationUnits() );

                query.setParameterList( "tsOuIds", ouIds );
            }
        }

        if ( params.hasUserGroups() )
        {
            Collection<Long> userGroupIds = IdentifiableObjectUtils.getIdentifiers( params.getUserGroups() );

            query.setParameterList( "userGroupIds", userGroupIds );
        }

        if ( !count )
        {
            if ( params.getFirst() != null )
            {
                query.setFirstResult( params.getFirst() );
            }

            if ( params.getMax() != null )
            {
                query.setMaxResults( params.getMax() );
            }
        }

        return query;
    }

    @Override
    public int getUserCount()
    {
        Query<Long> query = getTypedQuery( "select count(*) from User" );
        return query.uniqueResult().intValue();
    }

    @Override
    public User getUser( long id )
    {
        return getSession().get( User.class, id );
    }

    @Override
    public UserCredentials getUserCredentialsByUsername( String username )
    {
        if ( username == null )
        {
            return null;
        }

        String hql = "from UserCredentials uc where uc.username = :username";

        TypedQuery<UserCredentials> typedQuery = sessionFactory.getCurrentSession().createQuery( hql,
            UserCredentials.class );
        typedQuery.setParameter( "username", username );
        typedQuery.setHint( QueryHints.CACHEABLE, true );

        return QueryUtils.getSingleResult( typedQuery );
    }

    @Override
    public CurrentUserGroupInfo getCurrentUserGroupInfo( long userId )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery( Object[].class );
        Root<User> root = query.from( User.class );
        query.where( builder.equal( root.get( "id" ), userId ) );
        query.select( builder.array( root.get( "uid" ), root.join( "groups", JoinType.LEFT ).get( "uid" ) ) );

        List<Object[]> results = getSession().createQuery( query ).getResultList();

        CurrentUserGroupInfo currentUserGroupInfo = new CurrentUserGroupInfo();

        if ( CollectionUtils.isEmpty( results ) )
        {
            return currentUserGroupInfo;
        }

        for ( Object[] result : results )
        {
            if ( currentUserGroupInfo.getUserUID() == null )
            {
                currentUserGroupInfo.setUserUID( result[0].toString() );
            }

            if ( result[1] != null )
            {
                currentUserGroupInfo.getUserGroupUIDs().add( result[1].toString() );
            }
        }

        return currentUserGroupInfo;
    }

    @Override
    public int disableUsersInactiveSince( Date inactiveSince )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaUpdate<UserCredentials> update = builder.createCriteriaUpdate( UserCredentials.class );
        Root<UserCredentials> uc = update.from( UserCredentials.class );
        update.where( builder.and(
            // just so we do not count rows already disabled
            builder.equal( uc.get( DISABLED_COLUMN ), false ),
            builder.lessThanOrEqualTo( uc.get( "lastLogin" ), inactiveSince ) ) );
        update.set( DISABLED_COLUMN, true );
        return getSession().createQuery( update ).executeUpdate();
    }

    @Override
    public String getDisplayName( String userUid )
    {
        String sql = "select concat(firstname, ' ', surname) from userinfo where uid =:uid";
        Query<String> query = getSession().createNativeQuery( sql );
        query.setParameter( "uid", userUid );
        return query.getSingleResult();
    }
}
