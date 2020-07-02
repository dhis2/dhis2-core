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
 *
 */

import java.util.List;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface MetadataKeyJsonService
{
    /**
     * Retrieves a KeyJsonValue based on a namespace and key.
     *
     * @param key       the key referencing the value.
     * @return the KeyJsonValue matching the key and namespace.
     */
    KeyJsonValue getMetaDataVersion( String key );

    /**
     * Deletes a keyJsonValue.
     *
     * @param keyJsonValue the KeyJsonValue to be deleted.
     */
    void deleteMetaDataKeyJsonValue( KeyJsonValue keyJsonValue );

    /**
     * Adds a new KeyJsonValue.
     *
     * @param keyJsonValue the KeyJsonValue to be stored.
     * @return the id of the KeyJsonValue stored.
     */
    long addMetaDataKeyJsonValue( KeyJsonValue keyJsonValue );

    List<String> getAllVersions();
}