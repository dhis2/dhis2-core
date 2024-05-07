package org.hisp.dhis.webapi.controller.security;


import static org.hisp.dhis.config.ConfigProviderConfiguration.overrideProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisAuthenticationApiTest;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonImpersonateUserResponse;
import org.hisp.dhis.webapi.json.domain.JsonLoginResponse;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */

class ImpersonateUserControllerTest extends DhisControllerConvenienceTest {

  static {
    overrideProperties.put(ConfigurationKey.SWITCH_USER_FEATURE_ENABLED.getKey(), "true");
  }

  @Autowired SystemSettingManager systemSettingManager;
  @Autowired private SessionRegistry sessionRegistry;

  @Autowired private DhisConfigurationProvider config;

  @Test
  void testImpersonateUserOK() {
    String usernameToImpersonate = "usera";
    createUserWithAuth(usernameToImpersonate, "ALL");

    JsonImpersonateUserResponse responseB =
        POST("/auth/impersonate?username=%s".formatted(usernameToImpersonate))
            .content(HttpStatus.OK)
            .as(JsonImpersonateUserResponse.class);

    assertEquals("IMPERSONATION_SUCCESS", responseB.getLoginStatus());
    assertEquals(usernameToImpersonate, responseB.getImpersonatedUsername());
  }

  @Test
  void testImpersonateUserNonExistent() {
    String usernameToImpersonate = "dontexist";

    JsonImpersonateUserResponse responseB =
        POST("/auth/impersonate?username=%s".formatted(usernameToImpersonate))
            .content(HttpStatus.OK)
            .as(JsonImpersonateUserResponse.class);

    assertEquals("GENERIC_FAILURE", responseB.getLoginStatus());
    assertEquals("message", responseB.getMessage());
    assertEquals(usernameToImpersonate, responseB.getImpersonatedUsername());
  }
}
