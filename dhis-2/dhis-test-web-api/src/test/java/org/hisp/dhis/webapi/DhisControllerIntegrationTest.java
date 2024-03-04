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

import java.time.Duration;
import java.util.Date;
import java.util.function.BooleanSupplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.config.TestContainerPostgresConfig;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Base class for all Spring Mock MVC based controller tests which use postgres database in a docker
 * instance.
 *
 * @author Viet Nguyen
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
    classes = {TestContainerPostgresConfig.class, MvcTestConfig.class, WebTestConfiguration.class})
@ActiveProfiles(profiles = {"test-postgres"})
@IntegrationTest
@Transactional
public class DhisControllerIntegrationTest extends DhisControllerTestBase {
  public static final String ORG_HISP_DHIS_DATASOURCE_QUERY = "org.hisp.dhis.datasource.query";

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserService _userService;

  @Autowired protected IdentifiableObjectManager manager;

  @Autowired protected DbmsManager dbmsManager;

  @Autowired protected DhisConfigurationProvider dhisConfigurationProvider;

  @Autowired private TransactionTemplate txTemplate;

  @BeforeEach
  final void setup() {
    userService = _userService;
    clearSecurityContext();

    createAndPersistAdminUserAndRole();

    superUser = createAndAddAdminUser("ALL");

    mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    switchContextToUser(superUser);
    currentUser = superUser;

    TestUtils.executeStartupRoutines(webApplicationContext);

    integrationTestBefore();

    dbmsManager.flushSession();
    dbmsManager.clearSession();

    beforeEach();
  }

  protected void beforeEach() {}

  protected void lookUpInjectUserSecurityContext(User user) {
    if (user == null) {
      clearSecurityContext();
      return;
    }
    hibernateService.flushSession();

    User foundUser = manager.find(User.class, user.getId());
    injectSecurityContext(UserDetails.fromUser(foundUser));
  }

  protected User createAndPersistAdminUserAndRole() {
    UserRole role = createUserRole("Superuser_Test_" + CodeGenerator.generateUid(), "ALL");
    role.setUid(CodeGenerator.generateUid());

    manager.persist(role);

    User user = new User();
    user.setUid(CodeGenerator.generateUid());
    user.setFirstName("Admin");
    user.setSurname("User");
    user.setUsername(DEFAULT_USERNAME + "_test_" + CodeGenerator.generateUid());
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.getUserRoles().add(role);
    user.setLastUpdated(new Date());
    user.setCreated(new Date());

    manager.persist(user);
    lookUpInjectUserSecurityContext(user);

    return user;
  }

  protected void integrationTestBefore() {
    boolean enableQueryLogging =
        dhisConfigurationProvider.isEnabled(ConfigurationKey.ENABLE_QUERY_LOGGING);
    // Enable to query logger to log only what's happening inside the test
    // method
    if (enableQueryLogging) {
      Configurator.setLevel(ORG_HISP_DHIS_DATASOURCE_QUERY, Level.INFO);
      Configurator.setRootLevel(Level.INFO);
    }
  }

  protected static boolean await(Duration timeout, BooleanSupplier test)
      throws InterruptedException {
    while (!timeout.isNegative() && !test.getAsBoolean()) {
      Thread.sleep(20);
      timeout = timeout.minusMillis(20);
    }
    if (!timeout.isNegative()) return true;
    return test.getAsBoolean();
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
