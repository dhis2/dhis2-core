package org.hisp.dhis.keyjsonvalue;

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

import java.util.Date;
import java.util.List;

/**
 * @author Stian Sandvold
 */
public interface KeyJsonValueService
{
    /**
     * Retrieves a list of existing namespaces.
     *
     * @return a list of strings representing the existing namespaces.
     */
    List<String> getNamespaces( boolean isAdmin );

    /**
     * Retrieves list of KeyJsonValue objects belonging to the specified namespace.
     *
     * @param namespace the namespace where the key is associated
     * @return list of matching KeyJsonValues
     */
    List<KeyJsonValue> getKeyJsonValuesInNamespace( String namespace, boolean isAdmin );

    /**
     * Retrieves a list of keys from a namespace which are updated after lastUpdated time.
     *
     * @param namespace   the namespace to retrieve keys from.
     * @param lastUpdated the lastUpdated time to retrieve keys from.
     * @return a list of strings representing the keys from the namespace.
     */
    List<String> getKeysInNamespace( String namespace, Date lastUpdated, boolean isAdmin );

    /**
     * Retrieves a KeyJsonValue based on a namespace and key.
     *
     * @param namespace the namespace where the key is associated.
     * @param key       the key referencing the value.
     * @return the KeyJsonValue matching the key and namespace.
     */
    KeyJsonValue getKeyJsonValue( String namespace, String key, boolean isAdmin );

    /**
     * Adds a new KeyJsonValue.
     *
     * @param keyJsonValue the KeyJsonValue to be stored.
     * @return the id of the KeyJsonValue stored.
     */
    Long addKeyJsonValue( KeyJsonValue keyJsonValue );

    /**
     * Updates a KeyJsonValue.
     *
     * @param keyJsonValue the updated KeyJsonValue.
     */
    void updateKeyJsonValue( KeyJsonValue keyJsonValue );

    /**
     * Deletes a keyJsonValue.
     *
     * @param keyJsonValue the KeyJsonValue to be deleted.
     */
    void deleteKeyJsonValue( KeyJsonValue keyJsonValue );

    /**
     * Deletes all keys associated with a given namespace.
     *
     * @param namespace the namespace to delete
     */
    void deleteNamespace( String namespace );

    /**
     * Retrieves a value object.
     *
     * @param namespace the namespace where the key is associated.
     * @param key       the key referencing the value.
     * @param clazz     the class of the object to retrievev.
     * @return a value object.
     */
    <T> T getValue( String namespace, String key, Class<T> clazz );

    /**
     * Adds a value object.
     *
     * @param namespace the namespace where the key is associated.
     * @param key       the key referencing the value.
     * @param value     the value object to add.
     */
    <T> void addValue( String namespace, String key, T value );

    /**
     * Updates a value object.
     *
     * @param namespace the namespace where the key is associated.
     * @param key       the key referencing the value.
     * @param value     the value object to update.
     */
    <T> void updateValue( String namespace, String key, T value );
}
