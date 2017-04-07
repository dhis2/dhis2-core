package org.hisp.dhis.user.hibernate;

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

import org.hibernate.Query;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserStore;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Nguyen Hong Duc
 */
public class HibernateUserStore
    extends HibernateIdentifiableObjectStore<User>
    implements UserStore
{
    @Override
    @SuppressWarnings("unchecked")
    public List<User> getUsers( UserQueryParams params )
    {
        return getUserQuery( params, false ).list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<User> getExpiringUsers( UserQueryParams params )
    {
        return getUserQuery( params, false ).list();
    }

    @Override
    public int getUserCount( UserQueryParams params )
    {
        Long count = (Long) getUserQuery( params, true ).uniqueResult();
        return count != null ? count.intValue() : 0;
    }

    private Query getUserQuery( UserQueryParams params, boolean count )
    {
        SqlHelper hlp = new SqlHelper();
        
        String hql = count ? "select count(distinct u) " : "select distinct u ";
        
        hql +=
            "from User u " +
            "inner join u.userCredentials uc " +
            "left join u.groups g ";

        if ( params.getOrganisationUnit() != null )
        {
            hql += "left join u.organisationUnits ou ";

            if ( params.getIncludeOrgUnitChildren() )
            {
                hql += hlp.whereAnd() + " ou.path like :organisationUnitUid ";
            }
            else
            {
                hql += hlp.whereAnd() + " ou = :organisationUnit ";
            }
        }

        if ( params.getQuery() != null )
        {
            hql += hlp.whereAnd() + " (" +
                "lower(u.firstName) like :key " +
                "or lower(u.email) like :key " +
                "or lower(u.surname) like :key " +
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

        if ( params.getDaysPassedSincePasswordChange() != null )
        {
            hql += hlp.whereAnd() + " uc.passwordLastUpdated < :daysPassedSincePasswordChange ";
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
                "and uc.restoreCode is not null " +
                "and uc.restoreExpiry is not null " +
                "and uc.restoreExpiry < current_timestamp() ";
        }
        
        if ( !count )
        {
            hql += "order by u.surname, u.firstName";
        }

        Query query = sessionFactory.getCurrentSession().createQuery( hql );
        
        if ( params.getQuery() != null )
        {
            query.setString( "key", "%" + params.getQuery().toLowerCase() + "%" );
        }
        
        if ( params.getPhoneNumber() != null )
        {
            query.setString( "phoneNumber", params.getPhoneNumber() );
        }
        
        if ( params.isCanManage() && params.getUser() != null )
        {
            Collection<Integer> managedGroups = IdentifiableObjectUtils.getIdentifiers( params.getUser().getManagedGroups() );

            query.setParameterList( "ids", managedGroups );
        }
        
        if ( params.isAuthSubset() && params.getUser() != null )
        {
            Set<String> auths = params.getUser().getUserCredentials().getAllAuthorities();
            
            query.setParameterList( "auths", auths );
        }
        
        if ( params.isDisjointRoles() && params.getUser() != null )
        {
            Collection<Integer> roles = IdentifiableObjectUtils.getIdentifiers( params.getUser().getUserCredentials().getUserAuthorityGroups() );
            
            query.setParameterList( "roles", roles );
        }
        
        if ( params.getLastLogin() != null )
        {
            query.setTimestamp( "lastLogin", params.getLastLogin() );
        }

        if ( params.getDaysPassedSincePasswordChange() != null )
        {
            query.setTimestamp( "daysPassedSincePasswordChange", params.getDaysPassedSincePasswordChange() );
        }

        if ( params.getInactiveSince() != null )
        {
            query.setTimestamp( "inactiveSince", params.getInactiveSince() );
        }
        
        if ( params.getOrganisationUnit() != null )
        {
            if ( params.getIncludeOrgUnitChildren() )
            {
                // Match self and all children of selv in the path column.
                query.setString( "organisationUnitUid", "%/" + params.getOrganisationUnit().getUid() + "%" );
            }
            else
            {
                query.setEntity( "organisationUnit", params.getOrganisationUnit() );
            }
        }
        
        if ( params.getFirst() != null )
        {
            query.setFirstResult( params.getFirst() );
        }
        
        if ( params.getMax() != null )
        {
            query.setMaxResults( params.getMax() ).list();
        }

        return query;
    }

    @Override
    public int getUserCount()
    {
        return ((Long) getQuery( "select count(*) from User" ).
            uniqueResult()).intValue();
    }
}
