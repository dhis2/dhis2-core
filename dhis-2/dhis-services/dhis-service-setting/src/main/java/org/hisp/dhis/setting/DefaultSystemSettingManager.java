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
package org.hisp.dhis.setting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.system.util.SerializableOptional;
import org.hisp.dhis.system.util.ValidationUtils;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Declare transactions on individual methods. The get-methods do not have
 * transactions declared, instead a programmatic transaction is initiated on
 * cache miss in order to reduce the number of transactions to improve
 * performance.
 *
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
@Slf4j
public class DefaultSystemSettingManager
    implements SystemSettingManager
{
    private static final ImmutableMap<String, SettingKey> NAME_KEY_MAP = ImmutableMap.copyOf( Lists.newArrayList(
        SettingKey.values() ).stream().collect( Collectors.toMap( SettingKey::getName, e -> e ) ) );

    /**
     * Cache for system settings. Does not accept nulls. Disabled during test
     * phase.
     */
    private final Cache<SerializableOptional> settingCache;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final SystemSettingStore systemSettingStore;

    private final PBEStringEncryptor pbeStringEncryptor;

    private final List<String> flags;

    public DefaultSystemSettingManager( SystemSettingStore systemSettingStore,
        @Qualifier( "tripleDesStringEncryptor" ) PBEStringEncryptor pbeStringEncryptor,
        CacheProvider cacheProvider, List<String> flags )
    {
        checkNotNull( systemSettingStore );
        checkNotNull( pbeStringEncryptor );
        checkNotNull( cacheProvider );
        checkNotNull( flags );

        this.systemSettingStore = systemSettingStore;
        this.pbeStringEncryptor = pbeStringEncryptor;
        this.flags = flags;
        this.settingCache = cacheProvider.createSystemSettingCache();
    }

    // -------------------------------------------------------------------------
    // SystemSettingManager implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void saveSystemSetting( SettingKey key, Serializable value )
    {
        settingCache.invalidate( key.getName() );

        SystemSetting setting = systemSettingStore.getByName( key.getName() );

        if ( isConfidential( key.getName() ) )
        {
            value = pbeStringEncryptor.encrypt( value.toString() );
        }

        if ( setting == null )
        {
            setting = new SystemSetting();

            setting.setName( key.getName() );
            setting.setDisplayValue( value );

            systemSettingStore.save( setting );
        }
        else
        {
            setting.setDisplayValue( value );

            systemSettingStore.update( setting );
        }
    }

    @Override
    @Transactional
    public void saveSystemSettingTranslation( SettingKey key, String locale, String translation )
    {
        SystemSetting setting = systemSettingStore.getByName( key.getName() );

        if ( setting == null && !translation.isEmpty() )
        {
            throw new IllegalStateException( "No entry found for key: " + key.getName() );
        }
        else if ( setting != null )
        {
            if ( translation.isEmpty() )
            {
                setting.getTranslations().remove( locale );
            }
            else
            {
                setting.getTranslations().put( locale, translation );
            }

            settingCache.invalidate( key.getName() );
            systemSettingStore.update( setting );
        }
    }

    @Override
    @Transactional
    public void deleteSystemSetting( SettingKey key )
    {
        SystemSetting setting = systemSettingStore.getByName( key.getName() );

        if ( setting != null )
        {
            settingCache.invalidate( key.getName() );

            systemSettingStore.delete( setting );
        }
    }

    /**
     * Note: No transaction for this method, transaction is instead initiated at
     * the store level behind the cache to avoid the transaction overhead for
     * cache hits.
     */
    @Override
    @Transactional( readOnly = true )
    public Serializable getSystemSetting( SettingKey key )
    {
        return getSystemSetting( key, key.getDefaultValue() );
    }

    /**
     * Note: No transaction for this method, transaction is instead initiated at
     * the store level behind the cache to avoid the transaction overhead for
     * cache hits.
     */
    @Override
    @Transactional( readOnly = true )
    public Serializable getSystemSetting( SettingKey key, Serializable defaultValue )
    {
        SerializableOptional value = settingCache.get( key.getName(),
            k -> getSystemSettingOptional( k, defaultValue ) ).get();

        return value.get();
    }

    /**
     * Get system setting {@link SerializableOptional}. The return object is
     * never null in order to cache requests for system settings which have no
     * value or default value.
     *
     * @param name the system setting name.
     * @param defaultValue the default value for the system setting.
     * @return an optional system setting value.
     */
    private SerializableOptional getSystemSettingOptional( String name, Serializable defaultValue )
    {
        SystemSetting setting = systemSettingStore.getByName( name );

        if ( setting != null && setting.hasValue() )
        {
            if ( isConfidential( name ) )
            {
                try
                {
                    return SerializableOptional.of( pbeStringEncryptor.decrypt( (String) setting.getDisplayValue() ) );
                }
                catch ( EncryptionOperationNotPossibleException e )
                {
                    log.warn( "Could not decrypt system setting '" + name + "'" );
                    return SerializableOptional.empty();
                }
            }
            else
            {
                return SerializableOptional.of( setting.getDisplayValue() );
            }
        }
        else
        {
            return SerializableOptional.of( defaultValue );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public Optional<String> getSystemSettingTranslation( SettingKey key, String locale )
    {
        SystemSetting setting = systemSettingStore.getByName( key.getName() );

        if ( setting != null )
        {
            return setting.getTranslation( locale );
        }

        return Optional.empty();
    }

    @Override
    @Transactional( readOnly = true )
    public List<SystemSetting> getAllSystemSettings()
    {
        return systemSettingStore.getAll().stream()
            .filter( systemSetting -> !isConfidential( systemSetting.getName() ) ).collect( Collectors.toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public Map<String, Serializable> getSystemSettingsAsMap()
    {
        final Map<String, Serializable> settingsMap = new HashMap<>();

        for ( SettingKey key : SettingKey.values() )
        {
            if ( key.hasDefaultValue() )
            {
                settingsMap.put( key.getName(), key.getDefaultValue() );
            }
        }

        Collection<SystemSetting> systemSettings = getAllSystemSettings();

        for ( SystemSetting systemSetting : systemSettings )
        {
            Serializable settingValue = systemSetting.getDisplayValue();

            if ( settingValue == null )
            {
                Optional<SettingKey> setting = SettingKey.getByName( systemSetting.getName() );

                if ( setting.isPresent() )
                {
                    settingValue = setting.get().getDefaultValue();
                }
            }

            settingsMap.put( systemSetting.getName(), settingValue );
        }

        return settingsMap;
    }

    @Override
    @Transactional( readOnly = true )
    public Map<String, Serializable> getSystemSettings( Collection<SettingKey> keys )
    {
        Map<String, Serializable> map = new HashMap<>();

        for ( SettingKey setting : keys )
        {
            Serializable value = getSystemSetting( setting );

            if ( value != null )
            {
                map.put( setting.getName(), value );
            }
        }

        return map;
    }

    @Override
    public void invalidateCache()
    {
        settingCache.invalidateAll();
    }

    // -------------------------------------------------------------------------
    // Specific methods
    // -------------------------------------------------------------------------

    @Override
    public List<String> getFlags()
    {
        Collections.sort( flags );
        return flags;
    }

    @Override
    @Transactional( readOnly = true )
    public String getFlagImage()
    {
        String flag = (String) getSystemSetting( SettingKey.FLAG );

        return flag != null ? flag + ".png" : null;
    }

    @Override
    @Transactional( readOnly = true )
    public String getEmailHostName()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.EMAIL_HOST_NAME ) );
    }

    @Override
    @Transactional( readOnly = true )
    public int getEmailPort()
    {
        return (Integer) getSystemSetting( SettingKey.EMAIL_PORT );
    }

    @Override
    @Transactional( readOnly = true )
    public String getEmailUsername()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.EMAIL_USERNAME ) );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean getEmailTls()
    {
        return (Boolean) getSystemSetting( SettingKey.EMAIL_TLS );
    }

    @Override
    @Transactional( readOnly = true )
    public String getEmailSender()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.EMAIL_SENDER ) );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean accountRecoveryEnabled()
    {
        return (Boolean) getSystemSetting( SettingKey.ACCOUNT_RECOVERY );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean selfRegistrationNoRecaptcha()
    {
        return (Boolean) getSystemSetting( SettingKey.SELF_REGISTRATION_NO_RECAPTCHA );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean emailConfigured()
    {
        return StringUtils.isNotBlank( getEmailHostName() )
            && StringUtils.isNotBlank( getEmailUsername() );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean systemNotificationEmailValid()
    {
        String address = (String) getSystemSetting( SettingKey.SYSTEM_NOTIFICATIONS_EMAIL );

        return address != null && ValidationUtils.emailIsValid( address );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean hideUnapprovedDataInAnalytics()
    {
        // -1 means approval is disabled
        return (int) getSystemSetting( SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD ) >= 0;
    }

    @Override
    @Transactional( readOnly = true )
    public String googleAnalyticsUA()
    {
        return StringUtils.trimToNull( (String) getSystemSetting( SettingKey.GOOGLE_ANALYTICS_UA ) );
    }

    @Override
    @Transactional( readOnly = true )
    public Integer credentialsExpires()
    {
        return (Integer) getSystemSetting( SettingKey.CREDENTIALS_EXPIRES );
    }

    @Override
    public boolean isConfidential( String name )
    {
        return NAME_KEY_MAP.containsKey( name ) && NAME_KEY_MAP.get( name ).isConfidential();
    }

    @Override
    public boolean isTranslatable( final String name )
    {
        return NAME_KEY_MAP.containsKey( name ) && NAME_KEY_MAP.get( name ).isTranslatable();
    }
}
