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

import java.util.List;

/**
 * The {@link MetadataKeyJsonService} gives direct access to
 * {@link KeyJsonValue}s in the {@link #METADATA_STORE_NS} namespace.
 *
 * In contract to the generic {@link KeyJsonValueService} this service is not
 * restricted by {@link KeyJsonNamespaceProtection} rules. It is therefore only
 * meant for internal use and should never be exposed in a REST API.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface MetadataKeyJsonService
{
    /**
     * Name of the namespace used for {@link KeyJsonValue} entries belonging to
     * metadata entries.
     */
    String METADATA_STORE_NS = "METADATASTORE";

    /**
     * The authority required to read/write entries in the
     * {@link MetadataKeyJsonService#METADATA_STORE_NS}
     */
    String METADATA_SYNC_AUTHORITY = "METADATA_SYNC";

    /**
     * Retrieves an entry based key and {@link #METADATA_STORE_NS} namespace.
     *
     * @param key the key referencing the value.
     * @return the entry matching the key or {@code null}
     */
    KeyJsonValue getMetaDataVersion( String key );

    /**
     * Deletes a entry.
     *
     * @param entry the KeyJsonValue to be deleted.
     * @throws IllegalArgumentException when the entry given does not use the
     *         {@link #METADATA_STORE_NS} namespace.
     */
    void deleteMetaDataKeyJsonValue( KeyJsonValue entry );

    /**
     * Adds a new entry.
     *
     * @param entry the KeyJsonValue to be stored.
     * @return the id of the KeyJsonValue stored.
     * @throws IllegalArgumentException when the entry given does not use the
     *         {@link #METADATA_STORE_NS} namespace.
     */
    long addMetaDataKeyJsonValue( KeyJsonValue entry );

    List<String> getAllVersions();
}
