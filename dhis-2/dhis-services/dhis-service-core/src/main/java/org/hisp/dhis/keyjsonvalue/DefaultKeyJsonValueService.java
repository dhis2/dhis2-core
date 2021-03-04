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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.hisp.dhis.keyjsonvalue.KeyJsonNamespaceProtection.ProtectionType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Sandvold
 * @author Jan Bernitt
 */
@Service( "org.hisp.dhis.keyjsonvalue.KeyJsonValueService" )
public class DefaultKeyJsonValueService
    implements KeyJsonValueService
{

    private final Map<String, KeyJsonNamespaceProtection> protectionByNamespace = new ConcurrentHashMap<>();

    private final KeyJsonValueStore store;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final RenderService renderService;

    public DefaultKeyJsonValueService( KeyJsonValueStore store, CurrentUserService currentUserService,
        AclService aclService, RenderService renderService )
    {
        checkNotNull( store );
        checkNotNull( currentUserService );
        checkNotNull( aclService );
        checkNotNull( renderService );

        this.store = store;
        this.currentUserService = currentUserService;
        this.aclService = aclService;
        this.renderService = renderService;
    }

    // -------------------------------------------------------------------------
    // KeyJsonValueService implementation
    // -------------------------------------------------------------------------

    @Override
    public void addProtection( KeyJsonNamespaceProtection protection )
    {
        protectionByNamespace.put( protection.getNamespace(), protection );
    }

    @Override
    public void removeProtection( String namespace )
    {
        protectionByNamespace.remove( namespace );
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getNamespaces()
    {
        return store.getNamespaces().stream().filter( this::isNamespaceVisible ).collect( toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isUsedNamespace( String namespace )
    {
        return readProtectedIn( namespace, false,
            () -> store.countKeysInNamespace( namespace ) > 0 );
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getKeysInNamespace( String namespace, Date lastUpdated )
    {
        return readProtectedIn( namespace, emptyList(),
            () -> store.getKeysInNamespace( namespace, lastUpdated ) );
    }

    @Override
    @Transactional( readOnly = true )
    public KeyJsonValue getKeyJsonValue( String namespace, String key )
    {
        return readProtectedIn( namespace, null,
            () -> store.getKeyJsonValue( namespace, key ) );
    }

    @Override
    @Transactional( readOnly = true )
    public List<KeyJsonValue> getKeyJsonValuesInNamespace( String namespace )
    {
        return readProtectedIn( namespace, emptyList(),
            () -> store.getKeyJsonValueByNamespace( namespace ) );
    }

    @Override
    @Transactional
    public void addKeyJsonValue( KeyJsonValue entry )
    {
        if ( getKeyJsonValue( entry.getNamespace(), entry.getKey() ) != null )
        {
            throw new IllegalStateException(
                "The key '" + entry.getKey() + "' already exists on the namespace '" + entry.getNamespace() + "'." );
        }
        validateJsonValue( entry );
        writeProtectedIn( entry.getNamespace(),
            () -> singletonList( entry ),
            () -> store.save( entry ) );
    }

    @Override
    @Transactional
    public void updateKeyJsonValue( KeyJsonValue entry )
    {
        validateJsonValue( entry );
        writeProtectedIn( entry.getNamespace(),
            () -> singletonList( entry ),
            () -> store.update( entry ) );
    }

    @Override
    @Transactional
    public void deleteNamespace( String namespace )
    {
        writeProtectedIn( namespace,
            () -> getKeyJsonValuesInNamespace( namespace ),
            () -> store.deleteNamespace( namespace ) );
    }

    @Override
    @Transactional
    public void deleteKeyJsonValue( KeyJsonValue entry )
    {
        writeProtectedIn( entry.getNamespace(),
            () -> singletonList( entry ),
            () -> store.delete( entry ) );
    }

    private <T> T readProtectedIn( String namespace, T whenHidden, Supplier<T> read )
    {
        KeyJsonNamespaceProtection protection = protectionByNamespace.get( namespace );
        if ( protection == null
            || protection.getReads() == ProtectionType.NONE
            || currentUserHasAuthority( protection.getAuthorities() ) )
        {
            return read.get();
        }
        else if ( protection.getReads() == ProtectionType.RESTRICTED )
        {
            throw accessDeniedTo( namespace );
        }
        return whenHidden;
    }

    private void writeProtectedIn( String namespace, Supplier<List<KeyJsonValue>> whenSharing, Runnable write )
    {
        KeyJsonNamespaceProtection protection = protectionByNamespace.get( namespace );
        if ( protection == null || protection.getWrites() == ProtectionType.NONE )
        {
            write.run();
        }
        else if ( currentUserHasAuthority( protection.getAuthorities() ) )
        {
            // might also need to check ACL
            if ( protection.isSharingUsed() )
            {
                for ( KeyJsonValue entry : whenSharing.get() )
                {
                    if ( !aclService.canWrite( currentUserService.getCurrentUser(), entry ) )
                    {
                        throw accessDeniedTo( namespace, entry.getKey() );
                    }
                }
            }
            write.run();
        }
        else if ( protection.getWrites() == ProtectionType.RESTRICTED )
        {
            throw accessDeniedTo( namespace );
        }
        // HIDDEN: the operation silently just isn't run
    }

    private AccessDeniedException accessDeniedTo( String namespace )
    {
        return new AccessDeniedException(
            "The namespace '" + namespace
                + "' is protected, and you don't have the right authority to access or modify it." );
    }

    private AccessDeniedException accessDeniedTo( String namespace, String key )
    {
        return new AccessDeniedException(
            "You do not have the authority to modify the key: '" + key + "' in the namespace: '" + namespace + "'" );
    }

    private boolean isNamespaceVisible( String namespace )
    {
        KeyJsonNamespaceProtection protection = protectionByNamespace.get( namespace );
        return protection == null
            || protection.getReads() != ProtectionType.HIDDEN
            || currentUserHasAuthority( protection.getAuthorities() );
    }

    private boolean currentUserHasAuthority( Set<String> authorities )
    {
        User currentUser = currentUserService.getCurrentUser();
        if ( currentUser == null )
        {
            return false;
        }
        UserCredentials credentials = currentUser.getUserCredentials();
        return credentials.isSuper() || !authorities.isEmpty() && credentials.hasAnyAuthority( authorities );
    }

    private void validateJsonValue( KeyJsonValue entry )
    {
        String json = entry.getValue();
        try
        {
            if ( json != null && !renderService.isValidJson( json ) )
            {
                throw new IllegalArgumentException( "The data is not valid JSON." );
            }
        }
        catch ( IOException ex )
        {
            throw new IllegalArgumentException( "The data is not valid JSON.", ex );
        }
    }
}
