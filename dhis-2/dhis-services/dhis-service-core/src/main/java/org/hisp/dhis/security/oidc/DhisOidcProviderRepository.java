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
package org.hisp.dhis.security.oidc;

import com.google.common.base.MoreObjects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.provider.AzureAdProvider;
import org.hisp.dhis.security.oidc.provider.GoogleProvider;
import org.hisp.dhis.security.oidc.provider.Wso2Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class DhisOidcProviderRepository implements ClientRegistrationRepository {
  @Autowired private DhisConfigurationProvider config;

  private final Map<String, DhisOidcClientRegistration> registrationHashMap = new LinkedHashMap<>();

  @PostConstruct
  public void init() {
    GenericOidcProviderConfigParser.parse(config.getProperties()).forEach(this::addRegistration);
    AzureAdProvider.parse(config.getProperties()).forEach(this::addRegistration);

    addRegistration(GoogleProvider.parse(config.getProperties()));
    addRegistration(Wso2Provider.parse(config.getProperties()));
  }

  public void addRegistration(DhisOidcClientRegistration registration) {
    if (registration == null) {
      return;
    }

    registrationHashMap.putIfAbsent(
        registration.getClientRegistration().getRegistrationId(), registration);
  }

  public void clear() {
    this.registrationHashMap.clear();
  }

  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    final DhisOidcClientRegistration dhisOidcClientRegistration =
        registrationHashMap.get(registrationId);
    if (dhisOidcClientRegistration == null) {
      return null;
    }

    return dhisOidcClientRegistration.getClientRegistration();
  }

  public DhisOidcClientRegistration getDhisOidcClientRegistration(String registrationId) {
    return registrationHashMap.get(registrationId);
  }

  public Set<String> getAllRegistrationId() {
    return registrationHashMap.keySet();
  }

  public DhisOidcClientRegistration findByIssuerUri(String issuerUri) {
    return registrationHashMap.values().stream()
        .filter(
            c ->
                MoreObjects.firstNonNull(
                        c.getClientRegistration().getProviderDetails().getIssuerUri(), "")
                    .equals(issuerUri))
        .findAny()
        .orElse(null);
  }
}
