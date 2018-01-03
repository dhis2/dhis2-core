package org.hisp.dhis.keyjsonvalue;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.springframework.transaction.annotation.Transactional;
import org.hisp.dhis.system.util.JacksonUtils;
import java.util.List;
import java.util.Date;

/**
 * @author Stian Sandvold
 */
@Transactional
public class DefaultKeyJsonValueService
    implements KeyJsonValueService
{
    private KeyJsonValueStore keyJsonValueStore;

    public void setKeyJsonValueStore( KeyJsonValueStore keyJsonValueStore )
    {
        this.keyJsonValueStore = keyJsonValueStore;
    }

    // -------------------------------------------------------------------------
    // KeyJsonValueService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<String> getNamespaces()
    {
        return keyJsonValueStore.getNamespaces();
    }

    @Override
    public List<String> getKeysInNamespace( String namespace )
    {
        return keyJsonValueStore.getKeysInNamespace( namespace );
    }

    @Override
    public List<String> getKeysInNamespace( String namespace, Date lastUpdated )
    {
        return keyJsonValueStore.getKeysInNamespace( namespace, lastUpdated );
    }

    @Override
    public void deleteNamespace( String namespace )
    {
        keyJsonValueStore.getKeyJsonValueByNamespace( namespace ).forEach( keyJsonValueStore::delete );
    }

    @Override
    public KeyJsonValue getKeyJsonValue( String namespace, String key )
    {
        return keyJsonValueStore.getKeyJsonValue( namespace, key );
    }

    @Override
    public int addKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.save( keyJsonValue );

        return keyJsonValue.getId();
    }

    @Override
    public void updateKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.update( keyJsonValue );
    }

    @Override
    public void deleteKeyJsonValue( KeyJsonValue keyJsonValue )
    {
        keyJsonValueStore.delete( keyJsonValue );
    }

    @Override
    public <T> T getValue( String namespace, String key, Class<T> clazz )
    {
        KeyJsonValue value = getKeyJsonValue( namespace, key );

        if ( value == null || value.getPlainValue() == null )
        {
            return null;
        }
        
        return JacksonUtils.fromJson( value.getPlainValue(), clazz );
    }

    @Override
    public <T> void addValue( String namespace, String key, T object )
    {
        String value = JacksonUtils.toJson( object );
        
        KeyJsonValue keyJsonValue = new KeyJsonValue( namespace, key, value, false );
        
        keyJsonValueStore.save( keyJsonValue );
    }

    @Override
    public <T> void updateValue( String namespace, String key, T object )
    {
        KeyJsonValue keyJsonValue = getKeyJsonValue( namespace, key );
        
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
