/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.setting.SystemSettings.isConfidential;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
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
public class DefaultSystemSettingsService implements SystemSettingsService {

  /** This is a per thread cache of the settings */
  private final ThreadLocal<SystemSettings> currentSettings = new ThreadLocal<>();

  private final SystemSettingStore systemSettingStore;
  private final @Qualifier("tripleDesStringEncryptor") PBEStringEncryptor pbeStringEncryptor;
  private final TransactionTemplate transactionTemplate;

  /** The "cache" for the current settings */
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
    Map<String, String> values = transactionTemplate.execute(status -> systemSettingStore.getAll());
    allSettings = SystemSettings.of(values == null ? Map.of() : values, this::decrypt);
    return allSettings;
  }

  @Override
  @Transactional
  public void put(@Nonnull String key, @CheckForNull Serializable value) {
    try {
      putAll(Map.of(key, Settings.valueOf(value)));
    } catch (NotFoundException | BadRequestException ex) {
      // Note: This is a compromise as otherwise the exception would propagate
      // to lots of places that have not yet been adjusted to using the feedback exceptions
      log.error("Unable to put setting", ex);
    }
  }

  @Override
  @Transactional
  public void putAll(@Nonnull Map<String, String> settings)
      throws NotFoundException, BadRequestException {
    if (settings.isEmpty()) return;
    validateAll(settings);

    Set<String> deletes = new HashSet<>();
    for (Map.Entry<String, String> e : settings.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      if (value == null || value.isEmpty()) {
        deletes.add(key);
      } else {
        systemSettingStore.put(
            key, isConfidential(key) ? pbeStringEncryptor.encrypt(value) : value);
      }
    }
    deleteAll(deletes);
    allSettings = null; // invalidate
  }

  private void validateAll(@Nonnull Map<String, String> settings)
      throws NotFoundException, BadRequestException {
    Set<String> allowed = SystemSettings.keysWithDefaults();
    List<String> illegal =
        settings.keySet().stream().filter(key -> !allowed.contains(key)).toList();
    if (!illegal.isEmpty())
      throw new NotFoundException("Setting does not exist: " + String.join(",", illegal));
    SystemSettings empty = SystemSettings.of(Map.of());
    for (Map.Entry<String, String> e : settings.entrySet()) {
      if (!empty.isValid(e.getKey(), e.getValue()))
        throw new BadRequestException(
            "Not a valid value for setting %s: %s".formatted(e.getKey(), e.getValue()));
    }
  }

  @Override
  @Transactional
  public void deleteAll(@Nonnull Set<String> keys) {
    if (keys.isEmpty()) return;
    if (systemSettingStore.delete(keys) > 0) allSettings = null; // invalidate
  }

  private String decrypt(String key, String value) {
    try {
      return pbeStringEncryptor.decrypt(value);
    } catch (EncryptionOperationNotPossibleException ex) {
      log.warn("Could not decrypt system setting '" + key + "'");
      return "";
    }
  }
}
