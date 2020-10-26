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

import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service( "org.hisp.dhis.keyjsonvalue.MetaDataKeyJsonService" )
public class DefaultMetadataKeyJsonService implements MetadataKeyJsonService
{
    private final KeyJsonValueStore keyJsonValueStore;

    public DefaultMetadataKeyJsonService(
        KeyJsonValueStore keyJsonValueStore )
    {
        checkNotNull( keyJsonValueStore );

        this.keyJsonValueStore = keyJsonValueStore;
    }

    @Override
    @Transactional( readOnly = true )
    public KeyJsonValue getMetaDataVersion( String key )
    {
        return keyJsonValueStore.getKeyJsonValue( MetadataVersionService.METADATASTORE, key );
    }

    @Override
    @Transactional
    public void deleteMetaDataKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.delete( keyJsonValue );
    }

    @Override
    @Transactional
    public long addMetaDataKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.save( keyJsonValue );

        return keyJsonValue.getId();
    }

    @Override
    public List<String> getAllVersions()
    {
        return keyJsonValueStore.getKeysInNamespace( MetadataVersionService.METADATASTORE );
    }
}
