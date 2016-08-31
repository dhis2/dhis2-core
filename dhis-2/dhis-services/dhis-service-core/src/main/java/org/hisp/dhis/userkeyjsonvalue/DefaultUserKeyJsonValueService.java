package org.hisp.dhis.userkeyjsonvalue;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Stian Sandvold
 */
@Transactional
public class DefaultUserKeyJsonValueService
    implements UserKeyJsonValueService
{
    private UserKeyJsonValueStore userKeyJsonValueStore;

    public void setUserKeyJsonValueStore( UserKeyJsonValueStore userKeyJsonValueStore )
    {
        this.userKeyJsonValueStore = userKeyJsonValueStore;
    }

    public UserKeyJsonValueStore getUserKeyJsonValueStore()
    {
        return this.userKeyJsonValueStore;
    }

    @Override
    public UserKeyJsonValue getUserKeyJsonValue( User user, String namespace, String key )
    {
        return userKeyJsonValueStore.getUserKeyJsonValue( user, namespace, key );
    }

    @Override
    public int addUserKeyJsonValue( UserKeyJsonValue userKeyJsonValue )
    {
        return userKeyJsonValueStore.save( userKeyJsonValue );
    }

    @Override
    public void updateUserKeyJsonValue( UserKeyJsonValue userKeyJsonValue )
    {
        userKeyJsonValueStore.update( userKeyJsonValue );
    }

    @Override
    public void deleteUserKeyJsonValue( UserKeyJsonValue userKeyJsonValue )
    {
        userKeyJsonValueStore.delete( userKeyJsonValue );
    }

    @Override
    public List<String> getNamespacesByUser( User user )
    {
        return userKeyJsonValueStore.getNamespacesByUser( user );
    }

    @Override
    public List<String> getKeysByUserAndNamespace( User user, String namespace )
    {
        return userKeyJsonValueStore.getKeysByUserAndNamespace( user, namespace );
    }

    @Override
    public void deleteNamespaceFromUser( User user, String namespace )
    {
        userKeyJsonValueStore.getUserKeyJsonValueByUserAndNamespace( user, namespace ).forEach(
            userKeyJsonValueStore::delete );
    }
}
