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
package org.hisp.dhis.webapi;

import java.util.Date;
import java.util.Properties;
import lombok.Getter;
import org.hisp.dhis.IntegrationH2Test;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.config.H2DhisConfigurationProvider;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for convenient testing of the web API on basis of {@link
 * org.hisp.dhis.jsontree.JsonMixed} responses.
 *
 * @author Jan Bernitt
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
    classes = {
      ImpersonateUserControllerBaseTest.DhisConfiguration.class,
      MvcTestConfig.class,
      WebTestConfiguration.class
    })
@ActiveProfiles({"test-h2", "impersonate-user-test"})
@IntegrationH2Test
@Transactional
public abstract class ImpersonateUserControllerBaseTest extends DhisControllerTestBase {

  static class DhisConfiguration {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      H2DhisConfigurationProvider provider = new H2DhisConfigurationProvider();

      Properties properties = new Properties();
      properties.put(ConfigurationKey.SWITCH_USER_FEATURE_ENABLED.getKey(), "true");
      provider.addProperties(properties);

      return provider;
    }
  }

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserService _userService;

  @Autowired private RenderService _renderService;

  @Autowired protected IdentifiableObjectManager manager;

  @Autowired protected DbmsManager dbmsManager;

  @Autowired private TransactionTemplate txTemplate;

  @Getter private User adminUser;

  @BeforeEach
  final void setup() {
    userService = _userService;
    renderService = _renderService;
    clearSecurityContext();

    this.adminUser = _preCreateInjectAdminUserWithoutPersistence();
    manager.persist(adminUser);
    _injectSecurityContextUser(adminUser);

    superUser = createAndAddAdminUser("ALL");

    mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    switchContextToUser(superUser);
    currentUser = superUser;

    TestUtils.executeStartupRoutines(webApplicationContext);

    dbmsManager.flushSession();
    dbmsManager.clearSession();
  }

  private void _injectSecurityContextUser(User user) {
    if (user == null) {
      clearSecurityContext();
      return;
    }
    hibernateService.flushSession();
    User user1 = manager.find(User.class, user.getId());
    injectSecurityContext(UserDetails.fromUser(user1));
  }

  private User _preCreateInjectAdminUserWithoutPersistence() {
    UserRole role = createUserRole("Superuser_Test_" + CodeGenerator.generateUid(), "ALL");
    role.setUid(CodeGenerator.generateUid());
    manager.persist(role);
    User user = new User();
    String uid = CodeGenerator.generateUid();
    user.setUid(uid);
    user.setFirstName("Firstname_" + uid);
    user.setSurname("Surname_" + uid);
    user.setUsername(DEFAULT_USERNAME + "_test_" + CodeGenerator.generateUid());
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.getUserRoles().add(role);
    user.setLastUpdated(new Date());
    user.setCreated(new Date());
    return user;
  }

  protected final void doInTransaction(Runnable operation) {
    final int defaultPropagationBehaviour = txTemplate.getPropagationBehavior();
    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(
        status -> {
          operation.run();
          return null;
        });
    // restore original propagation behaviour
    txTemplate.setPropagationBehavior(defaultPropagationBehaviour);
  }
}
