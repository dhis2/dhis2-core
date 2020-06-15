package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.Set;

/**
 * This interface defined methods for getting access to the currently logged in
 * user and clearing the logged in state. If no user is logged in or the auto
 * access admin is active, all user access methods will return null.
 *
 * @author Torgeir Lorange Ostby
 * @version $Id: CurrentUserService.java 5708 2008-09-16 14:28:32Z larshelg $
 */
public interface CurrentUserService
{
    String ID = CurrentUserService.class.getName();

    /**
     * @return the username of the currently logged in user. If no user is
     *          logged in or the auto access admin is active, null is returned.
     */
    String getCurrentUsername();

    /**
     * @return the currently logged in user. If no user is logged in or the auto
     *          access admin is active, null is returned.
     */
    User getCurrentUser();

    /**
     * @return the user info for the currently logged in user. If no user is
     *          logged in or the auto access admin is active, null is returned.
     */
    UserInfo getCurrentUserInfo();

    /**
     * @return the data capture organisation units of the current user, empty set
     *          if no current user.
     */
    Set<OrganisationUnit> getCurrentUserOrganisationUnits();

    /**
     * @return true if the current logged in user has the ALL privileges set, false
     *          otherwise.
     */
    boolean currentUserIsSuper();

    /**
     * Indicates whether the current user has been granted the given authority.
     */
    boolean currentUserIsAuthorized( String auth );

    /**
     * Expire all the sessions associated with current user.
     */
    void expireUserSessions();

    /**
     * Return UserCredentials of current User
     *
     * @return UserCredentials of current User
     */
    UserCredentials getCurrentUserCredentials();
}
