package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.commons.util.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Declare transactions on individual methods. The get-methods do not have
 * transactions declared, instead a programmatic transaction is initiated on
 * cache miss in order to reduce the number of transactions to improve performance.
 *
 * @author Torgeir Lorange Ostby
 */
public class DefaultUserSettingService
    implements UserSettingService
{
    /**
     * Cache for user settings. Does not accept nulls. Key is "name-username".
     */
    private static Cache<String, Optional<Serializable>> SETTING_CACHE = Caffeine.newBuilder()
        .expireAfterAccess( 1, TimeUnit.HOURS )
        .initialCapacity( 200 )
        .maximumSize( SystemUtils.isTestRun() ? 0 : 10000 )
        .build();

    private static final Map<String, SettingKey> NAME_SETTING_KEY_MAP = Sets.newHashSet(
        SettingKey.values() ).stream().collect( Collectors.toMap( SettingKey::getName, s -> s ) );

    private String getCacheKey( String settingName, String username )
    {
        return settingName + DimensionalObject.ITEM_SEP + username;
    }

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private UserSettingStore userSettingStore;

    public void setUserSettingStore( UserSettingStore userSettingStore )
    {
        this.userSettingStore = userSettingStore;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    @Autowired
    private TransactionTemplate transactionTemplate;

    // -------------------------------------------------------------------------
    // UserSettingService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void saveUserSetting( UserSettingKey key, Serializable value, String username )
    {
        UserCredentials credentials = userService.getUserCredentialsByUsername( username );

        if ( credentials != null )
        {
            saveUserSetting( key, value, credentials.getUserInfo() );
        }
    }

    @Override
    @Transactional
    public void saveUserSetting( UserSettingKey key, Serializable value )
    {
        User currentUser = currentUserService.getCurrentUser();

        saveUserSetting( key, value, currentUser );
    }

    @Override
    @Transactional
    public void saveUserSetting( UserSettingKey key, Serializable value, User user )
    {
        if ( user == null )
        {
            return;
        }

        SETTING_CACHE.invalidate( getCacheKey( key.getName(), user.getUsername() ) );

        UserSetting userSetting = userSettingStore.getUserSetting( user, key.getName() );

        if ( userSetting == null )
        {
            userSetting = new UserSetting( user, key.getName(), value );

            userSettingStore.addUserSetting( userSetting );
        }
        else
        {
            userSetting.setValue( value );

            userSettingStore.updateUserSetting( userSetting );
        }
    }

    @Override
    @Transactional
    public void deleteUserSetting( UserSetting userSetting )
    {
        SETTING_CACHE.invalidate( getCacheKey( userSetting.getName(), userSetting.getUser().getUsername() ) );

        userSettingStore.deleteUserSetting( userSetting );
    }

    @Override
    @Transactional
    public void deleteUserSetting( UserSettingKey key )
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            UserSetting setting = userSettingStore.getUserSetting( currentUser, key.getName() );

            if ( setting != null )
            {
                deleteUserSetting( setting );
            }
        }
    }

    @Override
    @Transactional
    public void deleteUserSetting( UserSettingKey key, User user )
    {
        UserSetting setting = userSettingStore.getUserSetting( user, key.getName() );

        if ( setting != null )
        {
            deleteUserSetting( setting );
        }
    }

    /**
     * No transaction for this method, transaction is initiated in
     * {@link getUserSettingOptional}.
     */
    @Override
    public Serializable getUserSetting( UserSettingKey key )
    {
        return getUserSetting( key, Optional.empty() ).orElse( null );
    }

    /**
     * No transaction for this method, transaction is initiated in
     * {@link getUserSettingOptional}.
     */
    @Override
    public Serializable getUserSetting( UserSettingKey key, User user )
    {
        return getUserSetting( key, Optional.ofNullable( user ) ).orElse( null );
    }

    @Override
    @Transactional
    public List<UserSetting> getAllUserSettings()
    {
        User currentUser = currentUserService.getCurrentUser();

        return getUserSettings( currentUser );
    }

    @Override
    public Map<String, Serializable> getUserSettingsWithFallbackByUserAsMap( User user, Set<String> names,
        boolean useFallback )
    {
        Map<String, Serializable> result = Sets.newHashSet( getUserSettings( user ) ).stream()
            .filter( userSetting -> userSetting != null && userSetting.getName() != null && userSetting.getValue() != null )
            .collect( Collectors.toMap( UserSetting::getName, UserSetting::getValue ) );

        names.forEach( name -> {
            if ( !result.containsKey( name ) )
            {
                if ( useFallback )
                {
                    result.put( name, systemSettingManager.getSystemSetting( NAME_SETTING_KEY_MAP.get( name ) ) );
                }
                else
                {
                    result.put( name, null );
                }
            }
        } );

        return result;
    }

    @Override
    @Transactional
    public List<UserSetting> getUserSettings( User user )
    {
        if ( user == null )
        {
            return new ArrayList<>();
        }

        return userSettingStore.getAllUserSettings( user );
    }

    @Override
    public void invalidateCache()
    {
        SETTING_CACHE.invalidateAll();
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    private Optional<Serializable> getUserSetting( UserSettingKey key, Optional<User> user )
    {
        if ( key == null )
        {
            return Optional.empty();
        }

        String username = user.isPresent() ? user.get().getUsername() : currentUserService.getCurrentUsername();

        String cacheKey = getCacheKey( key.getName(), username );

        Optional<Serializable> result = SETTING_CACHE.
            get( cacheKey, c -> getUserSettingOptional( key, username ) );

        if ( !result.isPresent() && NAME_SETTING_KEY_MAP.containsKey( key.getName() ) )
        {
            return Optional.ofNullable(
                systemSettingManager.getSystemSetting( NAME_SETTING_KEY_MAP.get( key.getName() ) ) );
        }
        else
        {
            return result;
        }
    }

    /**
     * Get user setting optional. The database call is executed in a
     * programmatic transaction.
     *
     * @param key      the user setting key.
     * @param username the username of the user.
     * @return an optional user setting value.
     */
    private Optional<Serializable> getUserSettingOptional( UserSettingKey key, String username )
    {
        UserCredentials userCredentials = userService.getUserCredentialsByUsername( username );

        if ( userCredentials == null )
        {
            return Optional.empty();
        }

        UserSetting setting = transactionTemplate.execute( new TransactionCallback<UserSetting>()
        {
            public UserSetting doInTransaction( TransactionStatus status )
            {
                return userSettingStore.getUserSetting( userCredentials.getUserInfo(), key.getName() );
            }
        } );

        return setting != null && setting.hasValue() ?
            Optional.of( setting.getValue() ) : Optional.empty();
    }
}
