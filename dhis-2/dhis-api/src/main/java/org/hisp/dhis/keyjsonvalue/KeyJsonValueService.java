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
package org.hisp.dhis.keyjsonvalue;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.keyjsonvalue.KeyJsonNamespaceProtection.ProtectionType;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Stian Sandvold
 * @author Jan Bernitt
 */
public interface KeyJsonValueService
{
    /**
     * Applies the configuration for the provided protection so it is considered
     * by this service in future requests.
     *
     * @param protection configuration for protection protection
     */
    void addProtection( KeyJsonNamespaceProtection protection );

    /**
     * Removes any {@link KeyJsonNamespaceProtection} configuration for the
     * given namespace (if exists).
     *
     * @param namespace the namespace for which to remove configuration
     */
    void removeProtection( String namespace );

    /**
     * True, if there is at least a single value for the provided namespace.
     *
     * @param namespace the namespace to check
     * @return true, if the namespace exists, else false
     */
    boolean isUsedNamespace( String namespace );

    /**
     * Retrieves a list of existing namespaces.
     *
     * This does not include {@link ProtectionType#HIDDEN} namespaces that the
     * current user cannot see.
     *
     * @return a list of strings representing the existing namespaces.
     */
    List<String> getNamespaces();

    /**
     * Retrieves list of KeyJsonValue objects belonging to the specified
     * namespace.
     *
     * @param namespace the namespace where the key is associated
     * @return list of matching KeyJsonValues
     * @throws AccessDeniedException when user lacks authority for namespace
     */
    List<KeyJsonValue> getKeyJsonValuesInNamespace( String namespace );

    /**
     * Retrieves a list of keys from a namespace which are updated after
     * lastUpdated time.
     *
     * @param namespace the namespace to retrieve keys from.
     * @param lastUpdated the lastUpdated time to retrieve keys from.
     * @return a list of strings representing the keys from the namespace.
     * @throws AccessDeniedException when user lacks authority for namespace
     */
    List<String> getKeysInNamespace( String namespace, Date lastUpdated );

    /**
     * Retrieves a KeyJsonValue based on a namespace and key.
     *
     * @param namespace the namespace where the key is associated.
     * @param key the key referencing the value.
     * @return the KeyJsonValue matching the key and namespace.
     * @throws AccessDeniedException when user lacks authority for namespace
     */
    KeyJsonValue getKeyJsonValue( String namespace, String key );

    /**
     * Adds a new entry.
     *
     * @param entry the KeyJsonValue to be stored.
     * @throws IllegalStateException when an entry with same namespace and key
     *         already exists
     * @throws IllegalArgumentException when the entry value is not valid JSON
     * @throws AccessDeniedException when user lacks authority for namespace or
     *         entry
     */
    void addKeyJsonValue( KeyJsonValue entry );

    /**
     * Updates an entry.
     *
     * @param entry the updated KeyJsonValue.
     * @throws IllegalArgumentException when the entry value is not valid JSON
     * @throws AccessDeniedException when user lacks authority for namespace or
     *         entry
     */
    void updateKeyJsonValue( KeyJsonValue entry );

    /**
     * Deletes an entry.
     *
     * @param entry the KeyJsonValue to be deleted.
     * @throws AccessDeniedException when user lacks authority for namespace or
     *         entry
     */
    void deleteKeyJsonValue( KeyJsonValue entry );

    /**
     * Deletes all entries associated with a given namespace.
     *
     * @param namespace the namespace to delete
     * @throws AccessDeniedException when user lacks authority for namespace or
     *         any of the entries
     */
    void deleteNamespace( String namespace );

}
