package org.hisp.dhis.keyjsonvalue;

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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;

import java.util.List;

/**
 * @author Stian Sandvold
 */
public interface KeyJsonValueStore
    extends GenericIdentifiableObjectStore<KeyJsonValue>
{
    /**
     * Retrieves a list of all namespaces
     * @return a list of strings representing each existing namespace
     */
    List<String> getNamespaces();

    /**
     * Retrieves a list of keys associated with a given namespace.
     * @param namespace the namespace to retrieve keys from
     * @return a list of strings representing the different keys in the namespace
     */
    List<String> getKeysInNamespace( String namespace );

    /**
     * Retrieves a list of KeyJsonValue objects based on a given namespace
     * @param namespace the namespace to retrieve KeyJsonValues from
     * @return a List of KeyJsonValues
     */
    List<KeyJsonValue> getKeyJsonValueByNamespace( String namespace );

    /**
     * Retrieves a KeyJsonValue based on the associated key and namespace
     * @param namespace the namespace where the key is stored
     * @param key the key referencing the value
     * @return the KeyJsonValue retrieved
     */
    KeyJsonValue getKeyJsonValue( String namespace, String key );
}
