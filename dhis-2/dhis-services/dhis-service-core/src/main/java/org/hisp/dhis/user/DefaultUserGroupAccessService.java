package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.GenericStore;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class DefaultUserGroupAccessService implements UserGroupAccessService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericStore<UserGroupAccess> userGroupAccessStore;

    public void setUserGroupAccessStore( GenericStore<UserGroupAccess> userGroupAccessStore )
    {
        this.userGroupAccessStore = userGroupAccessStore;
    }

    // -------------------------------------------------------------------------
    // UserGroupAccess
    // -------------------------------------------------------------------------

    @Override
    public void addUserGroupAccess( UserGroupAccess userGroupAccess )
    {
        userGroupAccessStore.save( userGroupAccess );
    }

    @Override
    public void updateUserGroupAccess( UserGroupAccess userGroupAccess )
    {
        userGroupAccessStore.update( userGroupAccess );
    }

    @Override
    public void deleteUserGroupAccess( UserGroupAccess userGroupAccess )
    {
        userGroupAccessStore.delete( userGroupAccess );
    }

    @Override
    public List<UserGroupAccess> getAllUserGroupAccesses()
    {
        return userGroupAccessStore.getAll();
    }
}
