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
package org.hisp.dhis.sms.hibernate;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsStore;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.sms.hibernate.IncomingSmsStore")
public class HibernateIncomingSmsStore extends HibernateIdentifiableObjectStore<IncomingSms>
    implements IncomingSmsStore {
  public HibernateIncomingSmsStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        IncomingSms.class,
        currentUserService,
        aclService,
        true);
  }

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public List<IncomingSms> getSmsByStatus(SmsMessageStatus status, String originator) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<IncomingSms> parameter =
        newJpaParameters().addOrder(root -> builder.desc(root.get("sentDate")));

    if (status != null) {
      parameter.addPredicate(root -> builder.equal(root.get("status"), status));
    }

    if (originator != null && !originator.isEmpty()) {
      parameter.addPredicate(
          root ->
              JpaQueryUtils.stringPredicateIgnoreCase(
                  builder,
                  root.get("originator"),
                  originator,
                  JpaQueryUtils.StringSearchMode.ANYWHERE));
    }

    return getList(builder, parameter);
  }

  @Override
  public List<IncomingSms> getAll(Integer min, Integer max, boolean hasPagination) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<IncomingSms> parameters = new JpaQueryParameters<IncomingSms>();

    if (hasPagination) {
      parameters.setFirstResult(min).setMaxResults(max);
    }

    return getList(builder, parameters);
  }

  @Override
  public List<IncomingSms> getSmsByOriginator(String originator) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("originator"), originator)));
  }

  @Override
  public List<IncomingSms> getAllUnparsedMessages() {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder, newJpaParameters().addPredicate(root -> builder.equal(root.get("parsed"), false)));
  }

  @Override
  public List<IncomingSms> getSmsByStatus(
      SmsMessageStatus status, String keyword, Integer min, Integer max, boolean hasPagination) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<IncomingSms> parameters = newJpaParameters();

    if (status != null) {
      parameters.addPredicate(root -> builder.equal(root.get("status"), status));
    }

    if (keyword != null) {
      parameters.addPredicate(
          root ->
              JpaQueryUtils.stringPredicateIgnoreCase(
                  builder,
                  root.get("originator"),
                  keyword,
                  JpaQueryUtils.StringSearchMode.ANYWHERE));
    }

    if (hasPagination) {
      parameters.setFirstResult(min).setMaxResults(max);
    }

    return getList(builder, parameters);
  }
}
