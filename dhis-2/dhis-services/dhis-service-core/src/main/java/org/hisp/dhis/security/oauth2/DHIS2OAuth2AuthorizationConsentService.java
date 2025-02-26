/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security.oauth2;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.security.oauth2.consent.OAuth2AuthorizationConsent;
import org.hisp.dhis.security.oauth2.consent.OAuth2AuthorizationConsentStore;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * DHIS2 implementation of Spring Authorization Server's OAuth2AuthorizationConsentService that uses
 * HibernateOAuth2AuthorizationConsentStore for persistence.
 */
@Service
public class DHIS2OAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {
  private final OAuth2AuthorizationConsentStore authorizationConsentStore;
  private final RegisteredClientRepository clientRepository;

  public DHIS2OAuth2AuthorizationConsentService(
      OAuth2AuthorizationConsentStore authorizationConsentStore,
      RegisteredClientRepository clientRepository) {
    Assert.notNull(authorizationConsentStore, "authorizationConsentStore cannot be null");
    Assert.notNull(clientRepository, "registeredClientRepository cannot be null");
    this.authorizationConsentStore = authorizationConsentStore;
    this.clientRepository = clientRepository;
  }

  @Override
  public void save(
      org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
          authorizationConsent) {
    Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
    this.authorizationConsentStore.save(toEntity(authorizationConsent));
  }

  @Override
  public void remove(
      org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
          authorizationConsent) {
    Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
    this.authorizationConsentStore.deleteByRegisteredClientIdAndPrincipalName(
        authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
  }

  @Override
  public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
      findById(String registeredClientId, String principalName) {
    Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
    Assert.hasText(principalName, "principalName cannot be empty");

    OAuth2AuthorizationConsent entity =
        this.authorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            registeredClientId, principalName);

    return entity != null ? toObject(entity) : null;
  }

  /**
   * Converts a DHIS2 OAuth2AuthorizationConsent entity to Spring's OAuth2AuthorizationConsent
   * domain object.
   *
   * @param entity The DHIS2 OAuth2AuthorizationConsent entity
   * @return The Spring OAuth2AuthorizationConsent
   */
  private org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
      toObject(OAuth2AuthorizationConsent entity) {

    String registeredClientId = entity.getRegisteredClientId();
    RegisteredClient registeredClient = this.clientRepository.findById(registeredClientId);
    if (registeredClient == null) {
      throw new DataRetrievalFailureException(
          "The RegisteredClient with id '"
              + registeredClientId
              + "' was not found in the RegisteredClientRepository.");
    }

    org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent.Builder
        builder =
            org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
                .withId(registeredClientId, entity.getPrincipalName());

    if (entity.getAuthorities() != null) {
      for (String authority : StringUtils.commaDelimitedListToSet(entity.getAuthorities())) {
        builder.authority(new SimpleGrantedAuthority(authority));
      }
    }

    return builder.build();
  }

  /**
   * Converts a Spring OAuth2AuthorizationConsent domain object to a DHIS2
   * OAuth2AuthorizationConsent entity.
   *
   * @param authorizationConsent The Spring OAuth2AuthorizationConsent
   * @return The DHIS2 OAuth2AuthorizationConsent entity
   */
  private OAuth2AuthorizationConsent toEntity(
      org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
          authorizationConsent) {

    // Check if this record already exists to maintain its UID and created date
    OAuth2AuthorizationConsent existingEntity =
        this.authorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());

    OAuth2AuthorizationConsent entity =
        existingEntity != null ? existingEntity : new OAuth2AuthorizationConsent();

    if (existingEntity == null) {
      entity.setUid(CodeGenerator.generateUid());
    }

    entity.setRegisteredClientId(authorizationConsent.getRegisteredClientId());
    entity.setPrincipalName(authorizationConsent.getPrincipalName());

    Set<String> authorities = new HashSet<>();
    for (GrantedAuthority authority : authorizationConsent.getAuthorities()) {
      authorities.add(authority.getAuthority());
    }
    entity.setAuthorities(StringUtils.collectionToCommaDelimitedString(authorities));

    return entity;
  }
}
