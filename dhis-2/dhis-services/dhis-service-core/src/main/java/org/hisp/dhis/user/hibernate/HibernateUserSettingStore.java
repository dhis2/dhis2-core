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
package org.hisp.dhis.user.hibernate;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.user.UserSettingStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 */
@Repository
public class HibernateUserSettingStore extends HibernateGenericStore<UserSetting>
    implements UserSettingStore {

  public HibernateUserSettingStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, UserSetting.class, false);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String> getAllSettings(String username) {
    String sql =
        """
      select name, value from usersetting
      where userinfoid = (select userinfoid from userinfo where username = :user)""";
    Stream<Object[]> res = nativeSynchronizedQuery(sql).setParameter("user", username).stream();
    return res.collect(toMap(row -> (String) row[0], row -> toString(row[1])));
  }

  @Override
  public int delete(String username, Set<String> keys) {
    if (keys.isEmpty()) return 0;
    String sql =
        """
      delete from usersetting
        where name in :names
        and userinfoid = (select userinfoid from userinfo where username = :user)""";
    return nativeSynchronizedQuery(sql)
        .setParameter("user", username)
        .setParameterList("names", keys)
        .executeUpdate();
  }

  @Override
  public int deleteAll(String username) {
    String sql =
        """
      delete from usersetting
        where userinfoid = (select userinfoid from userinfo where username = :user)""";
    return nativeSynchronizedQuery(sql).setParameter("user", username).executeUpdate();
  }

  /**
   * ATM values are stored as binary data serialized from {@link java.io.Serializable}. As we are
   * only dealing with primitive values they all implement {@link Object#toString()} in a way that
   * yields the proper {@link String} form. This is the 1st step in away from storing binary data by
   * only using strings outside the store layer. Also, once settings are updated they always are
   * {@link String}s just still in their binary form.
   */
  private static String toString(Object value) {
    return value == null ? null : value.toString();
  }
}
