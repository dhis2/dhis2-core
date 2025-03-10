/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static java.util.Arrays.asList;
import static org.hisp.dhis.appmanager.AndroidSettingsApp.AUTHORITY;
import static org.hisp.dhis.appmanager.AndroidSettingsApp.NAMESPACE;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonDatastoreValue;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Update related tests that were moved over from in-memory DB controller tests as they now use SQL
 * to perform the update that does not work with the H2 database.
 *
 * @author Jan Bernitt
 */
@Transactional
class DatastoreControllerIntegrationTest extends PostgresControllerIntegrationTestBase {

  @Autowired private AppManager appManager;

  /**
   * Only used directly to setup namespace protection as this is by intention not possible using the
   * REST API.
   */
  @Autowired private DatastoreService service;

  @BeforeEach
  void setUp() {
    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    currentUserDetails.setId(0L);
    clearSecurityContext();
    injectSecurityContext(currentUserDetails);
  }

  @Test
  void testUpdateKeyJsonValue_AndroidApp() {
    switchToNewUser("not-an-android-manager");
    assertEquals(
        "Namespace 'ANDROID_SETTINGS_APP' is protected, access denied",
        PUT("/dataStore/" + NAMESPACE + "/key", "[]").error(HttpStatus.FORBIDDEN).getMessage());
    switchToNewUser("andriod-manager", AUTHORITY);
    assertStatus(HttpStatus.CREATED, PUT("/dataStore/" + NAMESPACE + "/key", "[]"));
  }

  @Test
  void testUpdateKeyJsonValue_App() throws IOException {
    assertEquals(
        AppStatus.OK,
        appManager
            .installApp(new ClassPathResource("app/test-app.zip").getFile(), "test-app.zip")
            .getAppState());
    // by default we are an app manager
    switchToNewUser("app-admin", Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString());

    assertStatus(HttpStatus.CREATED, POST("/dataStore/test-app-ns/key1", "[]"));
    assertStatus(HttpStatus.OK, PUT("/dataStore/test-app-ns/key1", "{}"));
    switchToNewUser("just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test");
    assertStatus(HttpStatus.OK, PUT("/dataStore/test-app-ns/key1", "{}"));
    switchToNewUser("has-no-app-authority");
    assertEquals(
        "Namespace 'test-app-ns' is protected, access denied",
        PUT("/dataStore/test-app-ns/key1", "{}").error(HttpStatus.FORBIDDEN).getMessage());
  }

  @Test
  void testPutEntry_EntryAlreadyExistsAndIsUpdated() {

    assertStatus(HttpStatus.CREATED, POST("/dataStore/ns1/key1", "42"));
    doInTransaction(
        () -> assertStatus(HttpStatus.CREATED, PUT("/dataStore/pets/emu", "{\"name\":\"harry\"}")));
    doInTransaction(
        () -> assertStatus(HttpStatus.OK, PUT("/dataStore/pets/emu", "{\"name\":\"james\"}")));
    JsonDatastoreValue emu = GET("/dataStore/pets/emu").content().as(JsonDatastoreValue.class);
    assertEquals("james", emu.getString("name").string());
  }

  @Test
  void testUpdateKeyJsonValue_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection(
        "pets", DatastoreNamespaceProtection.ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.FORBIDDEN, PUT("/dataStore/pets/cat", "[]"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, PUT("/dataStore/pets/cat", "[]"));
  }

  @Test
  void testUpdateKeyJsonValue() {
    doInTransaction(() -> assertStatus(HttpStatus.CREATED, POST("/dataStore/animals/cat", "[]")));
    doInTransaction(() -> assertStatus(HttpStatus.OK, PUT("/dataStore/animals/cat", "[1,2,3]")));
    assertEquals(asList(1, 2, 3), GET("/dataStore/animals/cat").content().numberValues());
  }

  @Test
  void testPutKeyJsonValue_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection(
        "pets", DatastoreNamespaceProtection.ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.CREATED, PUT("/dataStore/pets/cat", "[]"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, PUT("/dataStore/pets/cat", "[]"));
  }

  @Test
  void testUpdateKeyJsonValue_ProtectedNamespaceWithSharing() throws ForbiddenException {
    switchToAdminUser();

    setUpNamespaceProtectionWithSharing(
        "pets", DatastoreNamespaceProtection.ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    String uid = GET("/dataStore/pets/cat/metaData").content().as(JsonDatastoreValue.class).getId();

    assertStatus(
        HttpStatus.OK,
        POST("/sharing?type=dataStore&id=" + uid, "{'object':{'publicAccess':'r-------'}}"));

    DatastoreEntry entry = service.getEntry("pets", "cat");
    DatastoreEntry datastoreEntry = manager.get(DatastoreEntry.class, entry.getId());

    switchToNewUser("someone", "pets-admin");
    assertEquals(
        "Access denied for key 'cat' in namespace 'pets'",
        PUT("/dataStore/pets/cat", "[]").error(HttpStatus.FORBIDDEN).getMessage());
    switchToAdminUser();
    assertStatus(HttpStatus.OK, PUT("/dataStore/pets/cat", "[]"));
  }

  private void setUpNamespaceProtection(
      String namespace,
      DatastoreNamespaceProtection.ProtectionType readWrite,
      String... authorities) {
    service.addProtection(new DatastoreNamespaceProtection(namespace, readWrite, authorities));
  }

  private void setUpNamespaceProtectionWithSharing(
      String namespace,
      DatastoreNamespaceProtection.ProtectionType readWrite,
      String... authorities) {
    service.addProtection(new DatastoreNamespaceProtection(namespace, readWrite, authorities));
  }
}
