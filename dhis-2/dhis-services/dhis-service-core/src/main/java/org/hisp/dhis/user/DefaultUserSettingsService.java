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
package org.hisp.dhis.user;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.setting.SessionUserSettings;
import org.hisp.dhis.setting.Settings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.setting.UserSettings;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Declare transactions on individual methods. The get-methods do not have transactions declared,
 * instead a programmatic transaction is initiated on cache miss in order to reduce the number of
 * transactions to improve performance.
 *
 * @author Torgeir Lorange Ostby
 */
@Service
@RequiredArgsConstructor
public class DefaultUserSettingsService implements UserSettingsService, UserSessionAware {

  private final UserStore userStore;
  private final UserSettingStore userSettingStore;
  private final SystemSettingsProvider systemSettingsProvider;
  private final TransactionTemplate transactionTemplate;

  @Override
  @EventListener
  public void onSessionInit(AuthenticationSuccessEvent event) {
    Object principal = event.getAuthentication().getPrincipal();
    if (principal instanceof UserDetails user) {
      String username = user.getUsername();
      SessionUserSettings.put(username, getUserSettings(username, true));
    }
  }

  @Override
  @EventListener
  public void onSessionExpired(SessionDestroyedEvent event) {
    for (SecurityContext context : event.getSecurityContexts()) {
      Object principal = context.getAuthentication().getPrincipal();
      if (principal instanceof UserDetails user) {
        SessionUserSettings.clear(user.getUsername());
      }
    }
  }

  @Nonnull
  @Override
  public UserSettings getUserSettings(@Nonnull String username, boolean includeSystemFallbacks) {
    if (includeSystemFallbacks) {
      Optional<UserSettings> settings = SessionUserSettings.get(username);
      if (settings.isPresent()) return settings.get();
    }
    return requireNonNull(
        transactionTemplate.execute(
            status -> getUserSettingsInternal(username, includeSystemFallbacks)));
  }

  @Nonnull
  private UserSettings getUserSettingsInternal(@Nonnull String username, boolean includeFallbacks) {
    UserSettings settings = UserSettings.of(userSettingStore.getAll(username));
    return includeFallbacks
        ? settings.withFallback(systemSettingsProvider.getCurrentSettings().toMap())
        : settings;
  }

  @Override
  @Transactional
  public void put(@Nonnull String key, Serializable value)
      throws NotFoundException, BadRequestException {
    try {
      put(key, value, CurrentUserUtil.getCurrentUsername());
    } catch (ConflictException ex) {
      // we know the user exists so this should never happen
      throw new NoSuchElementException(ex);
    }
  }

  @Override
  @Transactional
  public void put(@Nonnull String key, @CheckForNull Serializable value, @Nonnull String username)
      throws NotFoundException, BadRequestException, ConflictException {
    putAll(Map.of(key, Settings.valueOf(value)), username);
  }

  @Override
  @Transactional
  public void putAll(@Nonnull Map<String, String> settings, @Nonnull String username)
      throws NotFoundException, BadRequestException, ConflictException {
    if (settings.isEmpty()) return;
    validateAll(settings);

    User user = userStore.getUserByUsername(username);
    if (user == null)
      throw new ConflictException(
          "%s with username %s could not be found."
              .formatted(User.class.getSimpleName(), username));
    Set<String> deletes = new HashSet<>();
    for (Map.Entry<String, String> e : settings.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      if (value == null || value.isEmpty()) {
        deletes.add(key);
      } else {
        userSettingStore.put(username, key, value);
      }
    }
    if (!deletes.isEmpty()) userSettingStore.delete(username, deletes);
    updateSession(username);
  }

  private void validateAll(@Nonnull Map<String, String> settings)
      throws NotFoundException, BadRequestException {
    Set<String> allowed = UserSettings.keysWithDefaults();
    List<String> illegal =
        settings.keySet().stream().filter(key -> !allowed.contains(key)).toList();
    if (!illegal.isEmpty())
      throw new NotFoundException("Setting does not exist: " + String.join(",", illegal));
    UserSettings empty = UserSettings.of(Map.of());
    for (Map.Entry<String, String> e : settings.entrySet()) {
      if (!empty.isValid(e.getKey(), e.getValue()))
        throw new BadRequestException(
            "Not a valid value for setting %s: %s".formatted(e.getKey(), e.getValue()));
    }
  }

  @Override
  @Transactional
  public void deleteAll(@Nonnull String username) {
    userSettingStore.deleteAll(username);
    updateSession(username);
  }

  private void updateSession(String username) {
    Optional<UserSettings> settings = SessionUserSettings.get(username);
    if (settings.isPresent()) {
      SessionUserSettings.put(username, getUserSettingsInternal(username, true));
    }
  }
}
