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

import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.system.util.JacksonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Stian Sandvold
 */
@Service( "org.hisp.dhis.keyjsonvalue.KeyJsonValueService" )
public class DefaultKeyJsonValueService
    implements KeyJsonValueService
{
    private KeyJsonValueStore keyJsonValueStore;

    public KeyJsonValueStore getKeyJsonValueStore()
    {
        return keyJsonValueStore;
    }

    public void setKeyJsonValueStore( KeyJsonValueStore keyJsonValueStore )
    {
        this.keyJsonValueStore = keyJsonValueStore;
    }

    // -------------------------------------------------------------------------
    // KeyJsonValueService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<String> getNamespaces( boolean isAdmin )
    {
        List<String> namespaces = keyJsonValueStore.getNamespaces();
        if ( !isAdmin )
        {
            namespaces.remove( MetadataVersionService.METADATASTORE );
        }

        return namespaces;
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getKeysInNamespace( String namespace, Date lastUpdated, boolean isAdmin )
    {
        if ( !isAdmin && MetadataVersionService.METADATASTORE.equals( namespace ) )
        {
            return Collections.emptyList();
        }

        return keyJsonValueStore.getKeysInNamespace( namespace, lastUpdated );
    }

    @Override
    @Transactional( readOnly = true )
    public KeyJsonValue getKeyJsonValue( String namespace, String key, boolean isAdmin )
    {
        if ( !isAdmin && MetadataVersionService.METADATASTORE.equals( namespace ) )
        {
            return null;
        }

        return keyJsonValueStore.getKeyJsonValue( namespace, key );
    }

    @Override
    @Transactional( readOnly = true )
    public List<KeyJsonValue> getKeyJsonValuesInNamespace( String namespace, boolean isAdmin )
    {
        if ( !isAdmin && MetadataVersionService.METADATASTORE.equals( namespace ) )
        {
            return Collections.emptyList();
        }

        return keyJsonValueStore.getKeyJsonValueByNamespace( namespace );
    }

    @Override
    @Transactional
    public Long addKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        if ( MetadataVersionService.METADATASTORE.equals( keyJsonValue.getNamespace() ) )
        {
            return null;
        }

        keyJsonValueStore.save( keyJsonValue );

        return Long.valueOf( keyJsonValue.getId() );
    }

    @Override
    @Transactional
    public void updateKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        if ( MetadataVersionService.METADATASTORE.equals( keyJsonValue.getNamespace() ) )
        {
            return;
        }

        keyJsonValueStore.update( keyJsonValue );
    }

    @Override
    @Transactional
    public void deleteNamespace( String namespace )
    {
        if ( MetadataVersionService.METADATASTORE.equals( namespace ) )
        {
            return;
        }

        keyJsonValueStore.getKeyJsonValueByNamespace( namespace ).forEach( keyJsonValueStore::delete );
    }

    @Override
    @Transactional
    public void deleteKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        if ( MetadataVersionService.METADATASTORE.equals( keyJsonValue.getNamespace() ) )
        {
            return;
        }

        keyJsonValueStore.delete( keyJsonValue );
    }

    @Override
    @Transactional( readOnly = true )
    public <T> T getValue( String namespace, String key, Class<T> clazz )
    {
        KeyJsonValue value = keyJsonValueStore.getKeyJsonValue( namespace, key );

        if ( value == null || value.getJbPlainValue() == null )
        {
            return null;
        }

        return JacksonUtils.fromJson( value.getJbPlainValue(), clazz );
    }

    @Override
    @Transactional
    public <T> void addValue( String namespace, String key, T object )
    {
        String value = JacksonUtils.toJson( object );

        KeyJsonValue keyJsonValue = new KeyJsonValue( namespace, key, value, false );

        keyJsonValueStore.save( keyJsonValue );
    }

    @Override
    @Transactional
    public <T> void updateValue( String namespace, String key, T object )
    {
        KeyJsonValue keyJsonValue = keyJsonValueStore.getKeyJsonValue( namespace, key );

        if ( keyJsonValue == null )
        {
            throw new IllegalStateException( String.format(
                "No object found for namespace '%s' and key '%s'", namespace, key ) );
        }

        String value = JacksonUtils.toJson( object );

        keyJsonValue.setValue( value );

        keyJsonValueStore.update( keyJsonValue );
    }
}
