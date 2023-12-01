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

import java.util.Collections;
import org.hisp.dhis.IntegrationH2Test;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.config.ConfigProviderConfiguration;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
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
import org.springframework.transaction.annotation.Transactional;
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
    classes = {ConfigProviderConfiguration.class, MvcTestConfig.class, WebTestConfiguration.class})
@ActiveProfiles("test-h2")
@IntegrationH2Test
@Transactional
public abstract class DhisControllerConvenienceTest extends DhisControllerTestBase {
  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserService _userService;

  @Autowired protected CurrentUserService currentUserService;

  @Autowired protected IdentifiableObjectManager manager;

  @Autowired protected DbmsManager dbmsManager;

  @BeforeEach
  final void setup() throws Exception {
    userService = _userService;
    clearSecurityContext();

    superUser = createAndAddAdminUser("ALL");

    mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    switchContextToUser(superUser);
    currentUser = superUser;

    TestUtils.executeStartupRoutines(webApplicationContext);

    dbmsManager.flushSession();
    dbmsManager.clearSession();
  }

  protected void switchToUserWithOrgUnitDataView(String userName, String orgUnitId) {
    User user = makeUser(userName, Collections.singletonList("ALL"));
    user.getDataViewOrganisationUnits().add(manager.get(OrganisationUnit.class, orgUnitId));
    userService.addUser(user);
    switchContextToUser(user);
  }
}
