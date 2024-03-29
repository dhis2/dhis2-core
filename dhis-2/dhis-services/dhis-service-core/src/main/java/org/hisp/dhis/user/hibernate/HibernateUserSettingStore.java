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

import java.util.List;
import javax.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.user.UserSettingStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Repository("org.hisp.dhis.user.UserSettingStore")
public class HibernateUserSettingStore implements UserSettingStore {
  private static final boolean CACHEABLE = true;

  private EntityManager entityManager;

  public HibernateUserSettingStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------
  // -------------------------------------------------------------------------
  // UserSettingStore implementation
  // -------------------------------------------------------------------------

  @Override
  public void addUserSetting(UserSetting userSetting) {
    getSession().save(userSetting);
  }

  @Override
  public void updateUserSetting(UserSetting userSetting) {
    getSession().update(userSetting);
  }

  @Override
  @Transactional
  public UserSetting getUserSettingTx(String username, String name) {
    return getUserSetting(username, name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public UserSetting getUserSetting(String username, String name) {
    Query<UserSetting> query =
        getSession()
            .createQuery(
                "from UserSetting us where us.user.username = :username and us.name = :name");
    query.setParameter("username", username);
    query.setParameter("name", name);
    query.setCacheable(CACHEABLE);

    return query.uniqueResult();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<UserSetting> getAllUserSettings(String username) {
    Query<UserSetting> query =
        getSession().createQuery("from UserSetting us where us.user.username = :username");
    query.setParameter("username", username);
    query.setCacheable(CACHEABLE);

    return query.list();
  }

  @Override
  public void deleteUserSetting(UserSetting userSetting) {
    getSession().delete(userSetting);
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
