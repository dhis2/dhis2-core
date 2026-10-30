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
package org.hisp.dhis.security.oidc;

import com.google.common.base.MoreObjects;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.provider.AzureAdProvider;
import org.hisp.dhis.security.oidc.provider.Dhis2InternalOidcProvider;
import org.hisp.dhis.security.oidc.provider.GoogleProvider;
import org.hisp.dhis.security.oidc.provider.Wso2Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * Spring-registered {@link ClientRegistrationRepository} holding every configured DHIS2 OIDC
 * registration, exposed to Spring Security's {@code oauth2Login} filter chain.
 *
 * <p>At startup, {@link #init()} parses {@code dhis.conf}: generic providers ({@code
 * oidc.provider.<id>.*}) are parsed by {@link GenericOidcProviderConfigParser}, and the dedicated
 * built-in providers ({@code google}, {@code azure.0} / {@code azure.1} / ..., {@code wso2}) are
 * parsed by their own provider classes. If {@code oauth2.server.enabled=on}, the internal {@code
 * dhis2-internal} provider is auto-registered; its endpoints are derived from {@code
 * server.base.url} and it is used by the Android Capture app rather than shown on the web login
 * page.
 *
 * <p>Each registration is keyed by its registration id (the provider id used in redirect URIs and
 * the login page). Insertion order is preserved via a {@link LinkedHashMap} so login buttons render
 * in the order providers were parsed.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class DhisOidcProviderRepository implements ClientRegistrationRepository {
  @Autowired private DhisConfigurationProvider config;

  private final Map<String, DhisOidcClientRegistration> registrationHashMap = new LinkedHashMap<>();

  /**
   * Builds the repository from {@code dhis.conf} at startup: parses generic OIDC providers, Azure,
   * Google, and WSO2 configurations, and auto-registers the internal DHIS2 provider when {@code
   * oauth2.server.enabled=on}.
   */
  @PostConstruct
  public void init() {
    GenericOidcProviderConfigParser.parse(config.getProperties()).forEach(this::addRegistration);
    AzureAdProvider.parse(config.getProperties()).forEach(this::addRegistration);

    addRegistration(GoogleProvider.parse(config.getProperties()));
    addRegistration(Wso2Provider.parse(config.getProperties()));

    if (config.isEnabled(ConfigurationKey.OAUTH2_SERVER_ENABLED)) {
      addRegistration(Dhis2InternalOidcProvider.parse(config));
    }
  }

  /**
   * Registers a {@link DhisOidcClientRegistration} under its registration id. Null values are
   * ignored. If a registration with the same id is already present, the existing entry is retained
   * (first-wins semantics).
   *
   * @param registration the registration to add, may be {@code null}
   */
  public void addRegistration(DhisOidcClientRegistration registration) {
    if (registration == null) {
      return;
    }

    registrationHashMap.putIfAbsent(
        registration.getClientRegistration().getRegistrationId(), registration);
  }

  /** Removes every registered provider from this repository. */
  public void clear() {
    this.registrationHashMap.clear();
  }

  /**
   * Returns the Spring {@link ClientRegistration} for the given registration id, or {@code null} if
   * no provider is registered under that id.
   *
   * @param registrationId the provider registration id
   * @return the Spring {@link ClientRegistration}, or {@code null} if unknown
   */
  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    final DhisOidcClientRegistration dhisOidcClientRegistration =
        registrationHashMap.get(registrationId);
    if (dhisOidcClientRegistration == null) {
      return null;
    }

    return dhisOidcClientRegistration.getClientRegistration();
  }

  /**
   * Returns the DHIS2 wrapper registration for the given registration id, or {@code null} if no
   * provider is registered under that id. Unlike {@link #findByRegistrationId(String)}, this
   * returns the DHIS2 wrapper so callers can access DHIS2-specific fields (mapping claim, {@code
   * private_key_jwt} material, external clients, login button metadata).
   *
   * @param registrationId the provider registration id
   * @return the DHIS2 wrapper, or {@code null} if unknown
   */
  public DhisOidcClientRegistration getDhisOidcClientRegistration(String registrationId) {
    return registrationHashMap.get(registrationId);
  }

  /**
   * Returns the set of all registered provider ids, in insertion order.
   *
   * @return set of registration ids
   */
  public Set<String> getAllRegistrationId() {
    return registrationHashMap.keySet();
  }

  /**
   * Finds the first registered provider whose {@code provider_details.issuer_uri} equals the given
   * issuer URI. Comparison ignores a single trailing slash on either side.
   *
   * @param issuerUri the issuer URI to look up
   * @return the matching registration, or {@code null} if no provider declares that issuer
   */
  public DhisOidcClientRegistration findByIssuerUri(String issuerUri) {
    String normalizedInput =
        issuerUri == null
            ? ""
            : issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
    return registrationHashMap.values().stream()
        .filter(
            c -> {
              String providerIssuer =
                  MoreObjects.firstNonNull(
                      c.getClientRegistration().getProviderDetails().getIssuerUri(), "");
              String normalizedProviderIssuer =
                  providerIssuer.endsWith("/") && providerIssuer.length() > 1
                      ? providerIssuer.substring(0, providerIssuer.length() - 1)
                      : providerIssuer;
              return normalizedProviderIssuer.equals(normalizedInput);
            })
        .findAny()
        .orElse(null);
  }
}
