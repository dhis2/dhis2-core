/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

/**
 * IntegrationTestBaseConfig will scan for Spring Components like we do in production. It will not
 * scan for components in a test package. This is to not include any test configurations. {@link
 * Configuration}s have to be included explicitly via a {@link
 * org.springframework.test.context.ContextConfiguration}. This guarantees that our tests are as
 * close to production as possible while having full control over the components we want to override
 * during testing.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@ComponentScan(
    basePackages = "org.hisp.dhis",
    excludeFilters = {
      @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.hisp\\.dhis\\.test\\..*"),
      // This is excluded as our org.hisp.dhis.test.webapi.MvcTestConfig is a rewrite of
      // WebMvcConfig. We first need to adapt MvcTestConfig to override what needs to be different
      // during testing so we can remove this.
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "org\\.hisp\\.dhis\\.webapi\\.security\\.config\\.WebMvcConfig")
    })
public class IntegrationTestBaseConfig {
  @Bean
  public static SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  public LdapAuthenticator ldapAuthenticator() {
    return authentication -> null;
  }

  @Bean
  public LdapAuthoritiesPopulator ldapAuthoritiesPopulator() {
    return (dirContextOperations, s) -> null;
  }

  @Bean
  public PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }
}
