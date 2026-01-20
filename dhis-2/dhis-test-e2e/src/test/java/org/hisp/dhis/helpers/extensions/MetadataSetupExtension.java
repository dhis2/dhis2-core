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
package org.hisp.dhis.helpers.extensions;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.TestRunStorage;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.MaintenanceActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataSetupExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
  private static boolean started = false;

  private static final Map<String, String> createdData = new LinkedHashMap<>();

  private static final Logger logger = LogManager.getLogger(MetadataSetupExtension.class.getName());

  private static final MaintenanceActions maintenanceApiActions = new MaintenanceActions();

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!started) {
      started = true;
      logger.info("Importing metadata for tests");

      // The following line registers a callback hook when the root test
      // context is shut down
      context.getRoot().getStore(GLOBAL).put("MetadataSetupExtension", this);

      MetadataActions metadataActions = new MetadataActions();

      new LoginActions().loginAsDefaultUser();

      String[] files = {
        "src/test/resources/setup/userGroups.json",
        "src/test/resources/setup/metadata.json",
        // importing for the second time to make sure all sharing is set
        // up correctly - there are bugs in metadata importer
        "src/test/resources/setup/metadata.json",
        "src/test/resources/setup/tracker_metadata.json",
        "src/test/resources/setup/userRoles.json",
        "src/test/resources/setup/users.json"
      };

      String queryParams = "async=false";
      for (String fileName : files) {
        metadataActions.importAndValidateMetadata(new File(fileName), queryParams);
      }

      setupUsers();

      createdData.putAll(TestRunStorage.getCreatedEntities());
      TestRunStorage.removeAllEntities();
    }
  }

  private void setupUsers() {
    logger.info("Adding users to the TA user group");
    UserActions userActions = new UserActions();
    String[] users = {
      TestConfiguration.get().superUserUsername(),
      TestConfiguration.get().defaultUserUsername(),
      TestConfiguration.get().adminUserUsername()
    };

    String userGroupId = Constants.USER_GROUP_ID;

    for (String user : users) {
      String userId =
          userActions
              .get(String.format("?filter=username:eq:%s", user))
              .extractString("users.id[0]");

      if (userId == null) {
        return;
      }
      userActions.addUserToUserGroup(userId, userGroupId);
      TestRunStorage.removeEntity("users", userId);
    }
  }

  private void iterateCreatedData(Consumer<String> stringConsumer) {

    for (String id : createdData.keySet()) {
      stringConsumer.accept(id);
    }
  }

  @Override
  public void close() {
    if (TestConfiguration.get().shouldCleanUp()) {
      TestCleanUp testCleanUp = new TestCleanUp();

      iterateCreatedData(id -> testCleanUp.deleteEntity(createdData.get(id), id));
      // clean-up category option combos, which are autogenerated (not tracked by e2e tests)
      maintenanceApiActions
          .post("categoryOptionComboUpdate", new QueryParamsBuilder().build())
          .validateStatus(200);
    }
  }
}
