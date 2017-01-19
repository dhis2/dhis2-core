package org.hisp.dhis.userkeyjsonvalue;

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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.user.User;

import java.util.List;

/**
 * @author Stian Sandvold
 */
public interface UserKeyJsonValueStore
    extends GenericIdentifiableObjectStore<UserKeyJsonValue>
{
    /**
     * Retrieves a KeyJsonValue based on the associated key and user
     * @param user the user where the key is stored
     * @param namespace the namespace referencing the value
     * @param key the key referencing the value
     * @return the KeyJsonValue retrieved
     */
    UserKeyJsonValue getUserKeyJsonValue( User user, String namespace, String key );

    /**
     * Retrieves a list of namespaces associated with a user
     * @param user to search namespaces for
     * @return a list of strings representing namespaces
     */
    List<String> getNamespacesByUser( User user );

    /**
     * Retrieves a list of keys associated with a given user and namespace.
     * @param user the user to retrieve keys from
     * @param namespace the namespace to search
     * @return a list of strings representing the different keys stored on the user
     */
    List<String> getKeysByUserAndNamespace( User user, String namespace );

    /**
     * Retrieves all UserKeyJsonvalues from a given user and namespace
     * @param user to search
     * @param namespace to search
     */
    List<UserKeyJsonValue> getUserKeyJsonValueByUserAndNamespace( User user, String namespace );
}
