package org.hisp.dhis.user.action.usergroup;

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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;

public class GetUserGroupListAction
    extends ActionPagingSupport<UserGroup>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private UserGroupService userGroupService;

    public void setUserGroupService( UserGroupService userGroupService )
    {
        this.userGroupService = userGroupService;
    }
    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    private List<UserGroup> userGroupList;

    public List<UserGroup> getUserGroupList()
    {
        return userGroupList;
    }

    private Map<UserGroup, Boolean> isCurrentUserMemberMap;

    public Map<UserGroup, Boolean> getIsCurrentUserMemberMap()
    {
        return isCurrentUserMemberMap;
    }

    private String key;
    
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    private String currentUserUid;

    public String getCurrentUserUid()
    {
        return currentUserUid;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( isNotBlank( key ) ) // Filter on key only if set
        {
            this.paging = createPaging( userGroupService.getUserGroupCountByName( key ) );
            
            userGroupList = userGroupService.getUserGroupsBetweenByName( key, paging.getStartPos(), paging.getPageSize() );
        }
        else
        {
            this.paging = createPaging( userGroupService.getUserGroupCount() );
            
            userGroupList = userGroupService.getUserGroupsBetween( paging.getStartPos(), paging.getPageSize() );
        }

        currentUserUid = currentUserService.getCurrentUser().getUid();

        isCurrentUserMemberMap = populateMemberShipMap( userGroupList );
        
        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Map<UserGroup, Boolean> populateMemberShipMap( List<UserGroup> userGroups )
    {
        User currentUser = currentUserService.getCurrentUser();
        
        Map<UserGroup, Boolean> map = new HashMap<>();
        
        if ( currentUser != null && currentUser.getGroups() != null )
        {
            Set<UserGroup> members = currentUser.getGroups();
            
            for ( UserGroup ug : userGroups )
            {
                map.put( ug, members.contains( ug ) );
            }
        }

        return map;
    }
}

