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
package org.hisp.dhis.setting.hibernate;

import static java.util.stream.Collectors.toMap;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hisp.dhis.HibernateNativeStore;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.setting.SystemSettingStore;
import org.springframework.stereotype.Repository;

/**
 * @author Jan Bernitt (refactored version)
 */
@Repository
public class HibernateSystemSettingStore extends HibernateNativeStore<SystemSetting>
    implements SystemSettingStore {

  public HibernateSystemSettingStore(EntityManager em) {
    super(em, SystemSetting.class);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String> getAll() {
    String sql = "select name, value from systemsetting";
    Stream<Object[]> res = nativeSynchronizedQuery(sql).stream();
    return res.collect(toMap(row -> (String) row[0], row -> (String) row[1]));
  }

  @Override
  public void put(@Nonnull String key, @Nonnull String value) {
    String sql = "update systemsetting set value = :value where name = :key";
    int updated =
        nativeSynchronizedQuery(sql)
            .setParameter("key", key)
            .setParameter("value", value)
            .executeUpdate();
    if (updated > 0) return;
    sql =
        "insert into systemsetting (systemsettingid, name, value) (select nextval('hibernate_sequence'), :key, :value)";
    nativeSynchronizedQuery(sql)
        .setParameter("key", key)
        .setParameter("value", value)
        .executeUpdate();
  }

  @Override
  public int delete(@Nonnull Set<String> keys) {
    if (keys.isEmpty()) return 0;
    String sql = "delete from systemsetting where name in :keys";
    return nativeSynchronizedQuery(sql).setParameterList("keys", keys).executeUpdate();
  }
}
