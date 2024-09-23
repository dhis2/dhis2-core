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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.setting.UserSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Declare transactions on individual methods. The get-methods do not have transactions declared,
 * instead a programmatic transaction is initiated on cache miss in order to reduce the number of
 * transactions to improve performance.
 *
 * @author Torgeir Lorange Ostby
 */
@Service
@RequiredArgsConstructor
public class DefaultUserSettingService implements UserSettingService {

  private final UserStore userStore;
  private final UserSettingStore userSettingStore;

  @Override
  @Transactional
  public UserSettings getSettings(@Nonnull String username) {
    //Note: this does **not** use transaction template as we always need a TX when this is called
    return UserSettings.of(userSettingStore.getAllSettings(username));
  }

  @Override
  @Transactional
  public void saveUserSetting(@Nonnull String key, String value) {
    try {
      saveUserSetting(key, value, CurrentUserUtil.getCurrentUsername());
    } catch (NotFoundException ex) {
      // we know the user exists so this should never happen
      throw new NoSuchElementException(ex);
    }
  }

  @Override
  @Transactional
  public void saveUserSetting(@Nonnull String key,  @CheckForNull String value, @Nonnull String username) throws NotFoundException {
    saveUserSettings(mapOf(key, value), username);
  }

  @Override
  @Transactional
  public void saveUserSettings(@Nonnull Map<String, String> settings, @Nonnull String username) throws NotFoundException {
    User user = userStore.getUserByUsername(username);
    if (user == null) throw new NotFoundException("%s with username %s could not be found.".formatted(User.class.getSimpleName(), username));
    for (Map.Entry<String, String> e : settings.entrySet()) {
      String value = e.getValue();
      UserSetting setting = new UserSetting(user, e.getKey(), value);
      if (value == null || value.isEmpty()) {
        userSettingStore.delete(setting);
      } else {
        userSettingStore.save(setting);
      }
    }
  }

  @Override
  @Transactional
  public void deleteAllUserSettings(@Nonnull String username) {
    userSettingStore.deleteAll(username);
  }

  private static Map<String, String> mapOf(@Nonnull String key, @CheckForNull String value) {
    if (value != null) return Map.of(key, value);
    Map<String, String> map = new HashMap<>(); // needed because of null
    map.put(key, null);
    return map;
  }
}
