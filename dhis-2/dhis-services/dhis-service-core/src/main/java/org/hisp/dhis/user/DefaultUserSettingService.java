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
package org.hisp.dhis.user;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.SerializableOptional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * Declare transactions on individual methods. The get-methods do not have
 * transactions declared, instead a programmatic transaction is initiated on
 * cache miss in order to reduce the number of transactions to improve
 * performance.
 *
 * @author Torgeir Lorange Ostby
 */
@Service( "org.hisp.dhis.user.UserSettingService" )
public class DefaultUserSettingService
    implements UserSettingService
{
    private static final Map<String, SettingKey> NAME_SETTING_KEY_MAP = Sets.newHashSet(
        SettingKey.values() ).stream().collect( Collectors.toMap( SettingKey::getName, s -> s ) );

    /**
     * Cache for user settings. Does not accept nulls. Disabled during test
     * phase.
     */
    private final Cache<SerializableOptional> userSettingCache;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final CurrentUserService currentUserService;

    private final UserSettingStore userSettingStore;

    private final UserService userService;

    private final SystemSettingManager systemSettingManager;

    public DefaultUserSettingService( CacheProvider cacheProvider,
        CurrentUserService currentUserService,
        UserSettingStore userSettingStore, UserService userService,
        SystemSettingManager systemSettingManager )
    {
        checkNotNull( cacheProvider );
        checkNotNull( currentUserService );
        checkNotNull( userSettingStore );
        checkNotNull( userService );
        checkNotNull( systemSettingManager );

        this.currentUserService = currentUserService;
        this.userSettingStore = userSettingStore;
        this.userService = userService;
        this.systemSettingManager = systemSettingManager;
        this.userSettingCache = cacheProvider.createUserSettingCache();
    }

    // -------------------------------------------------------------------------
    // UserSettingService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void saveUserSetting( UserSettingKey key, Serializable value, String username )
    {
        User user = userService.getUserByUsername( username );

        if ( user != null )
        {
            saveUserSetting( key, value, user );
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

        userSettingCache.invalidate( getCacheKey( key.getName(), user.getUsername() ) );

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
    public void saveUserSettings( UserSettings settings, User user )
    {
        if ( settings == null )
        {
            return; // nothing to do
        }
        for ( UserSettingKey key : UserSettingKey.values() )
        {
            Serializable value = key.getGetter().apply( settings );
            if ( value != null )
            {
                saveUserSetting( key, value, user );
            }
        }
    }

    @Override
    @Transactional
    public void deleteUserSetting( UserSetting userSetting )
    {
        userSettingCache.invalidate( getCacheKey( userSetting.getName(), userSetting.getUser().getUsername() ) );

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
     * Note: No transaction for this method, transaction is instead initiated at
     * the store level behind the cache to avoid the transaction overhead for
     * cache hits.
     */
    @Override
    @Transactional( readOnly = true )
    public Serializable getUserSetting( UserSettingKey key )
    {
        return getUserSetting( key, Optional.empty() ).get();
    }

    /**
     * Note: No transaction for this method, transaction is instead initiated at
     * the store level behind the cache to avoid the transaction overhead for
     * cache hits.
     */
    @Override
    @Transactional( readOnly = true )
    public Serializable getUserSetting( UserSettingKey key, User user )
    {
        return getUserSetting( key, Optional.ofNullable( user ) ).get();
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserSetting> getAllUserSettings()
    {
        User currentUser = currentUserService.getCurrentUser();

        return getUserSettings( currentUser );
    }

    @Override
    @Transactional( readOnly = true )
    public Map<String, Serializable> getUserSettingsWithFallbackByUserAsMap( User user,
        Set<UserSettingKey> userSettingKeys,
        boolean useFallback )
    {
        Map<String, Serializable> result = Sets.newHashSet( getUserSettings( user ) ).stream()
            .filter(
                userSetting -> userSetting != null && userSetting.getName() != null && userSetting.getValue() != null )
            .collect( Collectors.toMap( UserSetting::getName, UserSetting::getValue ) );

        userSettingKeys.forEach( userSettingKey -> {
            if ( !result.containsKey( userSettingKey.getName() ) )
            {
                Optional<SettingKey> systemSettingKey = SettingKey.getByName( userSettingKey.getName() );

                if ( useFallback && systemSettingKey.isPresent() )
                {
                    SettingKey setting = systemSettingKey.get();
                    result.put( userSettingKey.getName(),
                        systemSettingManager.getSystemSetting( setting, setting.getClazz() ) );
                }
                else
                {
                    result.put( userSettingKey.getName(), null );
                }
            }
        } );

        return result;
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserSetting> getUserSettings( User user )
    {
        if ( user == null )
        {
            return new ArrayList<>();
        }

        List<UserSetting> userSettings = userSettingStore.getAllUserSettings( user );
        Set<UserSetting> defaultUserSettings = UserSettingKey.getDefaultUserSettings( user );

        userSettings.addAll(
            defaultUserSettings.stream().filter( x -> !userSettings.contains( x ) ).collect( Collectors.toList() ) );

        return userSettings;
    }

    @Override
    public void invalidateCache()
    {
        userSettingCache.invalidateAll();
    }

    @Override
    @Transactional( readOnly = true )
    public Map<String, Serializable> getUserSettingsAsMap()
    {
        Set<UserSettingKey> userSettingKeys = Stream.of( UserSettingKey.values() ).collect( Collectors.toSet() );

        return getUserSettingsWithFallbackByUserAsMap( currentUserService.getCurrentUser(), userSettingKeys, false );
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    /**
     * Returns a user setting optional. If the user settings does not have a
     * value or default value, a corresponding system setting will be looked up.
     *
     * @param key the user setting key.
     * @param user an optional {@link User}.
     * @return an optional user setting value.
     */
    private SerializableOptional getUserSetting( UserSettingKey key, Optional<User> user )
    {
        if ( key == null )
        {
            return SerializableOptional.empty();
        }

        String username = user.isPresent() ? user.get().getUsername() : currentUserService.getCurrentUsername();

        String cacheKey = getCacheKey( key.getName(), username );

        SerializableOptional result = userSettingCache
            .get( cacheKey, c -> getUserSettingOptional( key, username ) );

        if ( !result.isPresent() && NAME_SETTING_KEY_MAP.containsKey( key.getName() ) )
        {
            SettingKey settingKey = NAME_SETTING_KEY_MAP.get( key.getName() );
            return SerializableOptional.of(
                systemSettingManager.getSystemSetting( settingKey, settingKey.getClazz() ) );
        }
        else
        {
            return result;
        }
    }

    /**
     * Get user setting optional. If the user setting exists and has a value,
     * the value is returned. If not, the default value for the key is returned,
     * if not present, an empty optional is returned. The return object is never
     * null in order to cache requests for system settings which have no value
     * or default value.
     *
     * @param key the user setting key.
     * @param username the username of the user.
     * @return an optional user setting value.
     */
    private SerializableOptional getUserSettingOptional( UserSettingKey key, String username )
    {
        User user = userService.getUserByUsername( username );

        if ( user == null )
        {
            return SerializableOptional.empty();
        }

        UserSetting setting = userSettingStore.getUserSettingTx( user, key.getName() );

        Serializable value = setting != null && setting.hasValue() ? setting.getValue() : key.getDefaultValue();

        return SerializableOptional.of( value );
    }

    /**
     * Returns the cache key for the given setting name and username.
     *
     * @param settingName the setting name.
     * @param username the username.
     * @return the cache key.
     */
    private String getCacheKey( String settingName, String username )
    {
        return settingName + DimensionalObject.ITEM_SEP + username;
    }
}
