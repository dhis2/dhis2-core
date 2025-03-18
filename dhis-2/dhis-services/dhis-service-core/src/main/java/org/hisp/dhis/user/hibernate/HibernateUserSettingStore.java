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
package org.hisp.dhis.user.hibernate;

import static java.util.stream.Collectors.toMap;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.HibernateNativeStore;
import org.hisp.dhis.setting.Settings;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.user.UserSettingStore;
import org.springframework.stereotype.Repository;

/**
 * @author Jan Bernitt (refactored version)
 */
@Slf4j
@Repository
public class HibernateUserSettingStore extends HibernateNativeStore<UserSetting>
    implements UserSettingStore {

  public HibernateUserSettingStore(EntityManager em) {
    super(em, UserSetting.class);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String> getAll(@Nonnull String username) {
    String sql =
        """
      select name, value from usersetting
      where userinfoid = (select userinfoid from userinfo where username = :user limit 1)""";
    Stream<Object[]> res = nativeSynchronizedQuery(sql).setParameter("user", username).stream();
    return res.collect(toMap(row -> (String) row[0], row -> fromBinary((String) row[0], row[1])));
  }

  @Override
  public void put(@Nonnull String username, @Nonnull String key, @Nonnull String value) {
    String sql =
        """
      update usersetting set value = :value
      where name = :key
      and userinfoid = (select u.userinfoid from userinfo u where u.username = :user limit 1)""";
    int updated =
        nativeSynchronizedQuery(sql)
            .setParameter("user", username)
            .setParameter("key", key)
            .setParameter("value", toBinary(value))
            .executeUpdate();
    if (updated > 0) return;
    sql =
        """
      insert into usersetting (userinfoid, name, value)
      (select u.userinfoid, :key, :value from userinfo u where u.username = :user limit 1)""";
    nativeSynchronizedQuery(sql)
        .setParameter("user", username)
        .setParameter("key", key)
        .setParameter("value", toBinary(value))
        .executeUpdate();
  }

  @Override
  public int delete(@Nonnull String username, @Nonnull Set<String> keys) {
    if (keys.isEmpty()) return 0;
    String sql =
        """
      delete from usersetting
        where name in :names
        and userinfoid = (select userinfoid from userinfo where username = :user limit 1)""";
    return nativeSynchronizedQuery(sql)
        .setParameter("user", username)
        .setParameterList("names", keys)
        .executeUpdate();
  }

  @Override
  public void deleteAll(@Nonnull String username) {
    String sql =
        """
      delete from usersetting
        where userinfoid = (select userinfoid from userinfo where username = :user limit 1)""";
    nativeSynchronizedQuery(sql).setParameter("user", username).executeUpdate();
  }

  /**
   * ATM values are stored as binary data serialized from {@link java.io.Serializable}. As we are
   * only dealing with primitive values they all implement {@link Object#toString()} in a way that
   * yields the proper {@link String} form. This is the 1st step in away from storing binary data by
   * only using strings outside the store layer. Also, once settings are updated they always are
   * {@link String}s just still in their binary form.
   */
  private static String fromBinary(String key, Object value) {
    if (value == null) return "";
    if (value instanceof byte[] binary) {
      try {
        ByteArrayInputStream bis = new ByteArrayInputStream(binary);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return Settings.valueOf((Serializable) ois.readObject());
      } catch (Exception ex) {
        log.warn(
            "Failed to de-serialize user setting %s from binary representation, using default"
                .formatted(key));
        return "";
      }
    }
    if (value instanceof Serializable s) return Settings.valueOf(s);
    log.warn(
        "Failed to de-serialize user setting %s from unknown source type: %s, using default"
            .formatted(key, value.getClass()));
    return "";
  }

  private static byte[] toBinary(final String value) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(value);
      out.flush();
      return bos.toByteArray();
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}
