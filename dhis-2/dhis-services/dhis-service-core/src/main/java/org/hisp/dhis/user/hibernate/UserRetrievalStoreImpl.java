/*
 * Copyright (c) 2004-2023, University of Oslo
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
import javax.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.QueryHints;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRetrievalStore;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Repository
@RequiredArgsConstructor
public class UserRetrievalStoreImpl implements UserRetrievalStore {

  private final EntityManager entityManager;

  @Override
  public User getUserByUsername(String username) {
    return getUserByUsername(username, false);
  }

  private User getUserByUsername(String username, boolean ignoreCase) {
    if (username == null) {
      return null;
    }
    String hql =
        ignoreCase
            ? "from User u where lower(u.username) = lower(:username)"
            : "from User u where u.username = :username";

    TypedQuery<User> typedQuery = entityManager.createQuery(hql, User.class);
    typedQuery.setParameter("username", username);
    typedQuery.setHint(QueryHints.CACHEABLE, true);

    return getSingleResult(typedQuery);
  }

  public static <T> T getSingleResult(TypedQuery<T> query) {
    query.setMaxResults(1);
    List<T> list = query.getResultList();
    if (list == null || list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }
}
