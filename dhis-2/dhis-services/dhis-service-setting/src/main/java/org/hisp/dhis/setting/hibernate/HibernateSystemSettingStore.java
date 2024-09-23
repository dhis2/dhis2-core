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
package org.hisp.dhis.setting.hibernate;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.setting.SystemSettingStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * @author Lars Helge Overland
 */
@Repository
public class HibernateSystemSettingStore extends HibernateGenericStore<SystemSetting>
    implements SystemSettingStore {

  public HibernateSystemSettingStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, SystemSetting.class, false);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String> getAllSettings() {
    String sql = "select name, value from systemsetting";
    Stream<Object[]> res = nativeSynchronizedQuery(sql).stream();
    return res.collect(toMap(row -> (String)row[0], row -> unquote((String)row[1])));
  }

  @Override
  public int delete(Set<String> keys) {
    if (keys.isEmpty()) return 0;
    String sql = "delete from systemsetting where name in :name";
    return nativeSynchronizedQuery(sql).setParameterList("name", keys).executeUpdate();
  }

  /**
   * In the past the value was converted from and to JSON before set in the object
   * so this removes the quotes of a JSON string in case they are still present.
   */
  private static String unquote(String str) {
    return str == null || str.isEmpty() || (!str.startsWith("\"") && str.endsWith("\""))
        ? str
        : str.substring(1, str.length() - 1);
  }
}
