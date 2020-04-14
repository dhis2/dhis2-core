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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Stian Sandvold
 */
@Service( "org.hisp.dhis.keyjsonvalue.KeyJsonValueService" )
public class DefaultKeyJsonValueService
    implements KeyJsonValueService
{
    private final KeyJsonValueStore keyJsonValueStore;
    private final ObjectMapper jsonMapper;

    public DefaultKeyJsonValueService(
        KeyJsonValueStore keyJsonValueStore,
        ObjectMapper jsonMapper )
    {
        this.jsonMapper = jsonMapper;
        checkNotNull( keyJsonValueStore );

        this.keyJsonValueStore = keyJsonValueStore;
    }

    // -------------------------------------------------------------------------
    // KeyJsonValueService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<String> getNamespaces()
    {
        return keyJsonValueStore.getNamespaces();
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getKeysInNamespace( String namespace )
    {
        return keyJsonValueStore.getKeysInNamespace( namespace );
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getKeysInNamespace( String namespace, Date lastUpdated )
    {
        return keyJsonValueStore.getKeysInNamespace( namespace, lastUpdated );
    }

    @Override
    @Transactional
    public void deleteNamespace( String namespace )
    {
        keyJsonValueStore.getKeyJsonValueByNamespace( namespace ).forEach( keyJsonValueStore::delete );
    }

    @Override
    @Transactional( readOnly = true )
    public KeyJsonValue getKeyJsonValue( String namespace, String key )
    {
        return keyJsonValueStore.getKeyJsonValue( namespace, key );
    }

    @Override
    @Transactional( readOnly = true )
    public List<KeyJsonValue> getKeyJsonValuesInNamespace( String namespace )
    {
        return keyJsonValueStore.getKeyJsonValueByNamespace( namespace );
    }

    @Override
    @Transactional
    public long addKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.save( keyJsonValue );

        return keyJsonValue.getId();
    }

    @Override
    @Transactional
    public void updateKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.update( keyJsonValue );
    }

    @Override
    @Transactional
    public void deleteKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.delete( keyJsonValue );
    }

    @Override
    @Transactional( readOnly = true )
    public <T> T getValue( String namespace, String key, Class<T> clazz )
    {
        KeyJsonValue value = getKeyJsonValue( namespace, key );

        if ( value == null || value.getJbPlainValue() == null )
        {
            return null;
        }

        try
        {
            return jsonMapper.readValue( value.getJbPlainValue(), clazz );
        }
        catch ( JsonProcessingException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    @Override
    @Transactional
    public <T> void addValue( String namespace, String key, T object )
    {
        String value = null;

        try
        {
            value = jsonMapper.writeValueAsString( object );
        }
        catch ( JsonProcessingException ex )
        {
            throw new UncheckedIOException( ex );
        }

        KeyJsonValue keyJsonValue = new KeyJsonValue( namespace, key, value, false );

        keyJsonValueStore.save( keyJsonValue );
    }

    @Override
    @Transactional
    public <T> void updateValue( String namespace, String key, T object )
    {
        KeyJsonValue keyJsonValue = getKeyJsonValue( namespace, key );

        if ( keyJsonValue == null )
        {
            throw new IllegalStateException( String.format(
                "No object found for namespace '%s' and key '%s'", namespace, key ) );
        }

        String value = null;

        try
        {
            value = jsonMapper.writeValueAsString( object );
        }
        catch ( JsonProcessingException ex )
        {
            throw new UncheckedIOException( ex );
        }

        keyJsonValue.setValue( value );

        keyJsonValueStore.update( keyJsonValue );
    }
}
