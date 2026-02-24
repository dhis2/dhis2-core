/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.config.H2DhisConfigurationProvider;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ContextConfiguration(classes = UserIdHeaderFilterTest.TestConfig.class)
class UserIdHeaderFilterTest extends H2ControllerIntegrationTestBase {
  private static final String ENCRYPTION_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
  private static final String HEADER_NAME = "X-User-ID";

  @Autowired private RequestIdFilter requestIdFilter;
  @Autowired private ApiVersionFilter apiVersionFilter;
  @Autowired private UserIdFilter userIdHeaderFilter;

  @BeforeEach
  void setupFilters() {
    mvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilter(requestIdFilter)
            .addFilter(apiVersionFilter)
            .addFilter(userIdHeaderFilter)
            .build();
  }

  @Test
  void shouldEncryptUserUidInHeader() throws Exception {
    HttpResponse response = GET("/test/userHeader");
    String header = response.header(HEADER_NAME);
    assertNotNull(header);

    String payload = decryptHeader(header, ENCRYPTION_KEY);
    assertEquals(getAdminUid(), payload);
  }

  private static String decryptHeader(String header, String key) throws GeneralSecurityException {
    byte[] combined = Base64.getUrlDecoder().decode(header.substring(3));
    byte[] iv = Arrays.copyOfRange(combined, 0, 12);
    byte[] cipherText = Arrays.copyOfRange(combined, 12, combined.length);
    byte[] keyBytes = Base64.getDecoder().decode(key);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, iv));
    return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
  }

  @Configuration
  static class TestConfig {
    @Bean
    @Primary
    DhisConfigurationProvider dhisConfigurationProvider() {
      H2DhisConfigurationProvider provider = new H2DhisConfigurationProvider();
      Properties properties = new Properties();
      properties.setProperty("logging.user_id_header.enabled", "on");
      properties.setProperty("logging.user_id_encryption_key", ENCRYPTION_KEY);
      provider.addProperties(properties);
      return provider;
    }
  }

  @Controller
  static class TestUserHeaderController {
    @GetMapping("/api/test/userHeader")
    @ResponseBody
    public String ok() {
      return "ok";
    }
  }
}
