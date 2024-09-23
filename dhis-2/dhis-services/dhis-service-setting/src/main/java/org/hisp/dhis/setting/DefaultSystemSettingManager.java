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

import static org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType.RESTRICTED;
import static org.hisp.dhis.setting.SystemSettings.isConfidential;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Manages {@link SystemSettings}.
 *
 * <p>Access is "cached" by keeping all {@link SystemSettings} in a field {@link #allSettings}. When
 * any settings are added or removed the field is set {@code null} causing a reload from DB on next
 * access. Only then a TX is opened using {@link TransactionTemplate}.
 *
 * <p>In addition, settings are "cached" per thread (request) using a {@link ThreadLocal} so that a
 * request always sees the same instance throughout the request. The {@link ThreadLocal} is cleared
 * at the start and end of each request. This instance is initialized on first access.
 *
 * @author Jan Bernitt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSystemSettingManager implements SystemSettingManager {

  /**
   * The namespace used to store settings translations in the datastore.
   */
  private static final String NS = "settings-translations";

  private final DatastoreService datastore;

  @PostConstruct
  private void init() {
    // Note: this uses a protection to prevent direct access
    // but from within this service it datastore is read-accessed as SystemUser
    datastore.addProtection(
        new DatastoreNamespaceProtection(
            NS, RESTRICTED, Authorities.F_SYSTEM_SETTING.name()));
  }

  /**
   * This is a per thread cache of the settings
   */
  private final ThreadLocal<SystemSettings> currentSettings = new ThreadLocal<>();
  private final SystemSettingStore systemSettingStore;
  private final @Qualifier("tripleDesStringEncryptor") PBEStringEncryptor pbeStringEncryptor;
  private final TransactionTemplate transactionTemplate;

  private SystemSettings allSettings;

  @Override
  @NonTransactional
  public void clearCurrentSettings() {
    currentSettings.remove();
  }

  @Override
  @IndirectTransactional
  public SystemSettings getCurrentSettings() {
    SystemSettings res = currentSettings.get();
    if (res == null) {
      res = getAllSettings();
      currentSettings.set(res);
    }
    return res;
  }

  /**
   * Note: The reason this uses {@link TransactionTemplate} is because most of the time the result
   * is available in the {@link ThreadLocal} so no TX is needed. Only when this method is called and
   * {@link #allSettings} is indeed {@code null} a TX is needed and thus opened using {@link
   * TransactionTemplate}.
   */
  private SystemSettings getAllSettings() {
    if (allSettings != null) return allSettings;
    Map<String, String> values = transactionTemplate.execute(status -> systemSettingStore.getAllSettings());
    allSettings = SystemSettings.of(values == null ? Map.of() : values, pbeStringEncryptor::decrypt);
    return allSettings;
  }

  @Override
  @Transactional
  public void saveSystemSettings(Map<String, String> settings) {
    if (settings.isEmpty()) return;
    for (Map.Entry<String, String> e : settings.entrySet()) {
      String name = e.getKey();
      String value = e.getValue();
      if (value != null && !value.isEmpty() && isConfidential(name))
        value = pbeStringEncryptor.encrypt(value);
      if (value == null || value.isEmpty()) {
        systemSettingStore.delete(new SystemSetting(name, null));
      } else {
        systemSettingStore.save(new SystemSetting(name, value));
      }
    }
    allSettings = null; // invalidate
  }

  @Override
  @Transactional
  public void deleteSystemSettings(Set<String> names) {
    if (systemSettingStore.delete(names) > 0)
      allSettings = null; // invalidate
  }

  @Override
  @Transactional
  public void saveSystemSettingTranslation(String key, String locale, String translation) throws ForbiddenException, BadRequestException {
    String datastoreKey = getDatastoreKey(key, locale);
    if (translation == null || translation.isEmpty()) {
      datastore.deleteEntry(new DatastoreEntry(NS, datastoreKey), getCurrentUserDetails());
    } else {
      DatastoreEntry entry = new DatastoreEntry(NS, datastoreKey, translation);
      datastore.saveOrUpdateEntry(entry, getCurrentUserDetails());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> getSystemSettingTranslation(String key, String locale)  {
    DatastoreEntry entry = null;
    try {
      entry = datastore.getEntry(NS, getDatastoreKey(key, locale), new SystemUser());
    } catch (ForbiddenException e) {
      // this should never happen as we use SuperUser
      // but, we really don't want to propagate the exception
      throw new RuntimeException(e);
    }
    return entry == null ? Optional.empty() : Optional.ofNullable(entry.getValue());
  }

  /**
   * @param key a settings key
   * @param locale a language tag
   * @return the key used in the datastore for a system setting tranaslation
   */
  private static String getDatastoreKey(String key, String locale) {
    return key+":"+locale;
  }
}
