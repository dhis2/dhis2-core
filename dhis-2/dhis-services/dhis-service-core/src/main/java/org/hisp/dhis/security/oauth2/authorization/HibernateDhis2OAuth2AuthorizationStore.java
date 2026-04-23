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
package org.hisp.dhis.security.oauth2.authorization;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Hibernate-backed store for {@link Dhis2OAuth2Authorization}. Backs {@link
 * Dhis2OAuth2AuthorizationServiceImpl} and exposes token-value lookups across the {@code state},
 * {@code authorization_code}, {@code access_token}, {@code refresh_token}, {@code oidc_id_token},
 * {@code user_code}, and {@code device_code} columns.
 */
@Repository
public class HibernateDhis2OAuth2AuthorizationStore
    extends HibernateIdentifiableObjectStore<Dhis2OAuth2Authorization>
    implements Dhis2OAuth2AuthorizationStore {

  public HibernateDhis2OAuth2AuthorizationStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Dhis2OAuth2Authorization.class, aclService, true);
  }

  /** Look up the authorization holding the given {@code state} value. */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByState(@Nonnull String state) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder, newJpaParameters().addPredicate(root -> builder.equal(root.get("state"), state)));
  }

  /** Look up the authorization holding the given authorization-code value. */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByAuthorizationCode(@Nonnull String authorizationCode) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(
                root -> builder.equal(root.get("authorizationCodeValue"), authorizationCode)));
  }

  /** Look up the authorization holding the given access-token value. */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByAccessToken(@Nonnull String accessToken) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("accessTokenValue"), accessToken)));
  }

  /** Look up the authorization holding the given refresh-token value. */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByRefreshToken(@Nonnull String refreshToken) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("refreshTokenValue"), refreshToken)));
  }

  /** Look up the authorization holding the given OIDC ID-token value. */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByOidcIdToken(@Nonnull String idToken) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("oidcIdTokenValue"), idToken)));
  }

  /** Look up the authorization holding the given device-flow user-code value. */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByUserCode(@Nonnull String userCode) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("userCodeValue"), userCode)));
  }

  /** Look up the authorization holding the given device-flow device-code value. */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByDeviceCode(@Nonnull String deviceCode) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("deviceCodeValue"), deviceCode)));
  }

  /**
   * Look up an authorization by matching the token value against all token columns ({@code state},
   * {@code authorization_code}, {@code access_token}, {@code refresh_token}, {@code oidc_id_token},
   * {@code user_code}, {@code device_code}).
   */
  @Override
  @CheckForNull
  public Dhis2OAuth2Authorization getByToken(@Nonnull String token) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(
                root ->
                    builder.or(
                        builder.equal(root.get("state"), token),
                        builder.equal(root.get("authorizationCodeValue"), token),
                        builder.equal(root.get("accessTokenValue"), token),
                        builder.equal(root.get("refreshTokenValue"), token),
                        builder.equal(root.get("oidcIdTokenValue"), token),
                        builder.equal(root.get("userCodeValue"), token),
                        builder.equal(root.get("deviceCodeValue"), token))));
  }

  /** Hard-delete the authorization row with the given UID. */
  @Override
  public void deleteByUID(@Nonnull String uid) {
    Query<Dhis2OAuth2Authorization> query =
        getQuery("delete from Dhis2OAuth2Authorization a where a.uid = :uid");
    query.setParameter("uid", uid);
    query.executeUpdate();
  }
}
