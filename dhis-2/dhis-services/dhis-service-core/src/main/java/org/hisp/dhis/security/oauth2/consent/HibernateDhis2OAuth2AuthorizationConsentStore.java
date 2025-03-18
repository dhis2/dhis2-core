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
package org.hisp.dhis.security.oauth2.consent;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Hibernate implementation of the OAuth2AuthorizationConsentStore. */
@Repository
public class HibernateDhis2OAuth2AuthorizationConsentStore
    extends HibernateIdentifiableObjectStore<Dhis2OAuth2AuthorizationConsent>
    implements Dhis2OAuth2AuthorizationConsentStore {

  public HibernateDhis2OAuth2AuthorizationConsentStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(
        entityManager,
        jdbcTemplate,
        publisher,
        Dhis2OAuth2AuthorizationConsent.class,
        aclService,
        true);
  }

  @Override
  @CheckForNull
  public Dhis2OAuth2AuthorizationConsent getByRegisteredClientIdAndPrincipalName(
      @Nonnull String registeredClientId, @Nonnull String principalName) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("registeredClientId"), registeredClientId))
            .addPredicate(root -> builder.equal(root.get("principalName"), principalName)));
  }

  @Override
  public void deleteByRegisteredClientIdAndPrincipalName(
      @Nonnull String registeredClientId, @Nonnull String principalName) {
    Dhis2OAuth2AuthorizationConsent consent =
        getByRegisteredClientIdAndPrincipalName(registeredClientId, principalName);
    if (consent != null) {
      delete(consent);
    }
  }
}
