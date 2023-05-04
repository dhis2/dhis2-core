/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.datastore;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Sandvold (initial)
 * @author Jan Bernitt (namespace protection)
 */
@AllArgsConstructor
@Service
public class DefaultDatastoreService
    implements DatastoreService
{
    private final Map<String, DatastoreNamespaceProtection> protectionByNamespace = new ConcurrentHashMap<>();

    private final DatastoreStore store;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final RenderService renderService;

    @Override
    public void addProtection( DatastoreNamespaceProtection protection )
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
    public <T> T getFields( DatastoreQuery query, Function<Stream<DatastoreFields>, T> transform )
    {
        DatastoreQueryValidator.validate( query );
        return readProtectedIn( query.getNamespace(), null,
            () -> store.getFields( query, transform ) );
    }

    @Override
    public DatastoreQuery plan( DatastoreQuery query )
        throws IllegalQueryException
    {
        DatastoreQueryValidator.validate( query );
        return query;
    }

    @Override
    @Transactional( readOnly = true )
    public DatastoreEntry getEntry( String namespace, String key )
    {
        return readProtectedIn( namespace, null,
            () -> store.getEntry( namespace, key ) );
    }

    @Override
    @Transactional
    public void addEntry( DatastoreEntry entry )
    {
        if ( getEntry( entry.getNamespace(), entry.getKey() ) != null )
        {
            throw new IllegalStateException( String.format(
                "Key '%s' already exists in namespace '%s'", entry.getKey(), entry.getNamespace() ) );
        }
        validateEntry( entry );
        writeProtectedIn( entry.getNamespace(),
            () -> singletonList( entry ),
            () -> store.save( entry ) );
    }

    @Override
    @Transactional
    public void updateEntry( DatastoreEntry entry )
    {
        validateEntry( entry );
        DatastoreNamespaceProtection protection = protectionByNamespace.get( entry.getNamespace() );
        Runnable update = protection == null || protection.isSharingRespected()
            ? () -> store.update( entry )
            : () -> store.updateNoAcl( entry );
        writeProtectedIn( entry.getNamespace(),
            () -> singletonList( entry ),
            update );
    }

    @Override
    @Transactional
    public void saveOrUpdateEntry( DatastoreEntry entry )
    {
        validateEntry( entry );
        DatastoreEntry existing = getEntry( entry.getNamespace(), entry.getKey() );
        if ( existing != null )
        {
            existing.setValue( entry.getValue() );
            writeProtectedIn( entry.getNamespace(),
                () -> singletonList( existing ),
                () -> store.update( existing ) );
        }
        else
        {
            writeProtectedIn( entry.getNamespace(),
                () -> singletonList( entry ),
                () -> store.save( entry ) );
        }
    }

    @Override
    @Transactional
    public void deleteNamespace( String namespace )
    {
        writeProtectedIn( namespace,
            () -> store.getEntryByNamespace( namespace ),
            () -> store.deleteNamespace( namespace ) );
    }

    @Override
    @Transactional
    public void deleteEntry( DatastoreEntry entry )
    {
        writeProtectedIn( entry.getNamespace(),
            () -> singletonList( entry ),
            () -> store.delete( entry ) );
    }

    private <T> T readProtectedIn( String namespace, T whenHidden, Supplier<T> read )
    {
        DatastoreNamespaceProtection protection = protectionByNamespace.get( namespace );
        if ( protection == null
            || protection.getReads() == ProtectionType.NONE
            || currentUserHasAuthority( protection.getAuthorities() ) )
        {
            T res = read.get();
            if ( res instanceof DatastoreEntry && protection != null && protection.isSharingRespected() )
            {
                DatastoreEntry entry = (DatastoreEntry) res;
                if ( !aclService.canRead( currentUserService.getCurrentUser(), entry ) )
                {
                    throw new AccessDeniedException( String.format(
                        "Access denied for key '%s' in namespace '%s'", entry.getKey(), namespace ) );
                }
            }
            return res;
        }
        else if ( protection.getReads() == ProtectionType.RESTRICTED )
        {
            throw accessDeniedTo( namespace );
        }
        return whenHidden;
    }

    private void writeProtectedIn( String namespace, Supplier<List<DatastoreEntry>> whenSharing, Runnable write )
    {
        DatastoreNamespaceProtection protection = protectionByNamespace.get( namespace );
        if ( protection == null || protection.getWrites() == ProtectionType.NONE )
        {
            write.run();
        }
        else if ( currentUserHasAuthority( protection.getAuthorities() ) )
        {
            // might also need to check sharing
            if ( protection.isSharingRespected() )
            {
                for ( DatastoreEntry entry : whenSharing.get() )
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
        return new AccessDeniedException( String.format(
            "Namespace '%s' is protected, access denied", namespace ) );
    }

    private AccessDeniedException accessDeniedTo( String namespace, String key )
    {
        return new AccessDeniedException( String.format(
            "Access denied for key '%s' in namespace '%s'", key, namespace ) );
    }

    private boolean isNamespaceVisible( String namespace )
    {
        DatastoreNamespaceProtection protection = protectionByNamespace.get( namespace );
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
        return currentUser.isSuper() || !authorities.isEmpty() && currentUser.hasAnyAuthority( authorities );
    }

    private void validateEntry( DatastoreEntry entry )
    {
        String json = entry.getValue();
        try
        {
            if ( json != null && !renderService.isValidJson( json ) )
            {
                throw new IllegalArgumentException( String.format(
                    "Invalid JSON value for key '%s'", entry.getKey() ) );
            }
        }
        catch ( IOException ex )
        {
            throw new IllegalArgumentException( String.format(
                "Invalid JSON value for key '%s'", entry.getKey() ), ex );
        }
    }
}
