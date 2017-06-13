package org.hisp.dhis.commons.action;

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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.comparator.UserComparator;
import org.hisp.dhis.util.ContextUtils;

/**
 * @author mortenoh
 */
public class GetUsersAction
    extends ActionPagingSupport<User>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String key;

    public void setKey( String key )
    {
        this.key = key;
    }

    private List<User> users;

    public List<User> getUsers()
    {
        return users;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        //TODO: Allow user with F_USER_VIEW_WITHIN_MANAGED_GROUP and restrict viewing to within managed groups.

        users = new ArrayList<>( userService.getAllUsers() );

        ContextUtils.clearIfNotModified( ServletActionContext.getRequest(), ServletActionContext.getResponse(), users );
        
        if ( key != null )
        {
            filterByKey( key, true );
        }

        Collections.sort( users, new UserComparator() );

        if ( usePaging )
        {
            this.paging = createPaging( users.size() );

            users = users.subList( paging.getStartPos(), paging.getEndPos() );
        }

        return SUCCESS;
    }

    private void filterByKey( String key, boolean ignoreCase )
    {
        ListIterator<User> iterator = users.listIterator();

        if ( ignoreCase )
        {
            key = key.toLowerCase();
        }

        while ( iterator.hasNext() )
        {
            User user = iterator.next();
            String name = ignoreCase ? user.getName().toLowerCase() : user.getName();

            if ( name.indexOf( key ) == -1 )
            {
                iterator.remove();
            }
        }
    }
}
