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
package org.hisp.dhis.setting;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.system.util.SerializableOptional;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Declare transactions on individual methods. The get-methods do not have transactions declared,
 * instead a programmatic transaction is initiated on cache miss in order to reduce the number of
 * transactions to improve performance.
 *
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
@Slf4j
public class DefaultSystemSettingManager implements SystemSettingManager {
  private static final Map<String, SettingKey> NAME_KEY_MAP =
      Map.copyOf(
          Lists.newArrayList(SettingKey.values()).stream()
              .collect(Collectors.toMap(SettingKey::getName, e -> e)));

  /** Cache for system settings. Does not accept nulls. Disabled during test phase. */
  private final Cache<SerializableOptional> settingCache;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final SystemSettingStore systemSettingStore;

  private final PBEStringEncryptor pbeStringEncryptor;

  private final TransactionTemplate transactionTemplate;

  private final List<String> flags;

  public DefaultSystemSettingManager(
      SystemSettingStore systemSettingStore,
      @Qualifier("tripleDesStringEncryptor") PBEStringEncryptor pbeStringEncryptor,
      CacheProvider cacheProvider,
      List<String> flags,
      TransactionTemplate transactionTemplate) {
    checkNotNull(systemSettingStore);
    checkNotNull(pbeStringEncryptor);
    checkNotNull(cacheProvider);
    checkNotNull(flags);

    this.systemSettingStore = systemSettingStore;
    this.pbeStringEncryptor = pbeStringEncryptor;
    this.flags = flags;
    this.settingCache = cacheProvider.createSystemSettingCache();
    this.transactionTemplate = transactionTemplate;
  }

  // -------------------------------------------------------------------------
  // SystemSettingManager implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void saveSystemSetting(SettingKey key, Serializable value) {
    settingCache.invalidate(key.getName());

    SystemSetting setting = systemSettingStore.getByName(key.getName());

    if (isConfidential(key.getName())) {
      value = pbeStringEncryptor.encrypt(value.toString());
    }

    if (setting == null) {
      setting = new SystemSetting();

      setting.setName(key.getName());
      setting.setDisplayValue(value);

      systemSettingStore.save(setting);
    } else {
      setting.setDisplayValue(value);

      systemSettingStore.update(setting);
    }
  }

  @Override
  @Transactional
  public void saveSystemSettingTranslation(SettingKey key, String locale, String translation) {
    SystemSetting setting = systemSettingStore.getByName(key.getName());

    if (setting == null && !translation.isEmpty()) {
      throw new IllegalStateException("No entry found for key: " + key.getName());
    }
    if (setting != null) {
      if (translation.isEmpty()) {
        setting.getTranslations().remove(locale);
      } else {
        setting.getTranslations().put(locale, translation);
      }

      settingCache.invalidate(key.getName());
      systemSettingStore.update(setting);
    }
  }

  @Override
  @Transactional
  public void deleteSystemSetting(SettingKey key) {
    SystemSetting setting = systemSettingStore.getByName(key.getName());

    if (setting != null) {
      settingCache.invalidate(key.getName());

      systemSettingStore.delete(setting);
    }
  }

  /**
   * Note: No transaction for this method, transaction is instead initiated at the store level
   * behind the cache to avoid the transaction overhead for cache hits.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T extends Serializable> T getSystemSetting(SettingKey key, T defaultValue) {
    SerializableOptional value =
        settingCache.get(key.getName(), k -> getSystemSettingOptional(k, defaultValue));

    return defaultIfNull((T) value.get(), defaultValue);
  }

  /**
   * Get system setting {@link SerializableOptional}. The return object is never null in order to
   * cache requests for system settings which have no value or default value.
   *
   * @param name the system setting name.
   * @param defaultValue the default value for the system setting.
   * @return an optional system setting value.
   */
  private SerializableOptional getSystemSettingOptional(String name, Serializable defaultValue) {
    Serializable displayValue = getSettingDisplayValue(name);

    if (displayValue != null) {
      if (isConfidential(name)) {
        try {
          return SerializableOptional.of(pbeStringEncryptor.decrypt((String) displayValue));
        } catch (ClassCastException | EncryptionOperationNotPossibleException e) {
          log.warn("Could not decrypt system setting '" + name + "'");
          return SerializableOptional.empty();
        }
      }
      return SerializableOptional.of(displayValue);
    }
    return SerializableOptional.of(defaultValue);
  }

  private Serializable getSettingDisplayValue(String name) {
    SystemSetting setting =
        transactionTemplate.execute(status -> systemSettingStore.getByName(name));

    if (setting != null && setting.hasValue()) {
      return setting.getDisplayValue();
    }

    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> getSystemSettingTranslation(SettingKey key, String locale) {
    SystemSetting setting = systemSettingStore.getByName(key.getName());

    if (setting != null) {
      return setting.getTranslation(locale);
    }

    return Optional.empty();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SystemSetting> getAllSystemSettings() {
    return systemSettingStore.getAll().stream()
        .filter(systemSetting -> !isConfidential(systemSetting.getName()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Serializable> getSystemSettingsAsMap() {
    final Map<String, Serializable> settingsMap = new HashMap<>();

    for (SettingKey key : SettingKey.values()) {
      if (key.hasDefaultValue()) {
        settingsMap.put(key.getName(), key.getDefaultValue());
      }
    }

    Collection<SystemSetting> systemSettings = getAllSystemSettings();

    for (SystemSetting systemSetting : systemSettings) {
      Serializable settingValue = systemSetting.getDisplayValue();

      if (settingValue == null) {
        Optional<SettingKey> setting = SettingKey.getByName(systemSetting.getName());

        if (setting.isPresent()) {
          settingValue = setting.get().getDefaultValue();
        }
      }

      settingsMap.put(systemSetting.getName(), settingValue);
    }

    return settingsMap;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Serializable> getSystemSettings(Collection<SettingKey> keys) {
    Map<String, Serializable> map = new HashMap<>();

    for (SettingKey setting : keys) {
      Serializable value = getSystemSetting(setting, setting.getClazz());

      if (value != null) {
        map.put(setting.getName(), value);
      }
    }

    return map;
  }

  @Override
  public void invalidateCache() {
    settingCache.invalidateAll();
  }

  // -------------------------------------------------------------------------
  // Specific methods
  // -------------------------------------------------------------------------

  @Override
  public List<String> getFlags() {
    Collections.sort(flags);
    return flags;
  }

  @Override
  public boolean isConfidential(String name) {
    SettingKey key = NAME_KEY_MAP.get(name);
    return key != null && key.isConfidential();
  }

  @Override
  public boolean isTranslatable(final String name) {
    SettingKey key = NAME_KEY_MAP.get(name);
    return key != null && key.isTranslatable();
  }
}
