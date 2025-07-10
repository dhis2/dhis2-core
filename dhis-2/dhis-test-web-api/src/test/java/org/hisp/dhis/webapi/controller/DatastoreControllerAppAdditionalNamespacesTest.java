/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * A test for the {@code additionalNamespaces} features in the app manifest that allows to associate
 * extra namespaces with an app and define with authorities are needed to read or write.
 *
 * @author Jan Bernitt
 */
@Transactional
class DatastoreControllerAppAdditionalNamespacesTest extends H2ControllerIntegrationTestBase {

  @Autowired private AppManager appManager;

  @BeforeEach
  void setUp() throws IOException {
    assertEquals(
        AppStatus.OK,
        appManager
            .installApp(new ClassPathResource("app/test-app-with-additional-ns.zip").getFile())
            .getAppState());
  }

  @Test
  void testReadAccess_NoAuthority() {
    switchToNewUser("tester1", "not-a-helpful-authority");
    assertStatus(HttpStatus.FORBIDDEN, GET("/dataStore/test-app-extra-ns/hello"));
  }

  @Test
  void testReadAccess_Super() {
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/test-app-extra-ns/hello"));
  }

  @Test
  void testReadAccess_ReadOnly() {
    switchToNewUser("tester2", "test-app-read");
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/test-app-extra-ns/hello"));
  }

  @Test
  void testReadAccess_ReadWrite() {
    switchToNewUser("tester6", "test-app-common");
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/test-app-extra-ns/hello"));
  }

  @Test
  void testWriteAccess_NoAuthority() {
    switchToNewUser("tester3", "not-a-helpful-authority");
    assertStatus(HttpStatus.FORBIDDEN, POST("/dataStore/test-app-extra-ns/hello", "true"));
  }

  @Test
  void testWriteAccess_ReadOnly() {
    switchToNewUser("tester7", "test-app-read");
    assertStatus(HttpStatus.FORBIDDEN, POST("/dataStore/test-app-extra-ns/hello", "true"));
  }

  @Test
  void testWriteAccess_Super() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/test-app-extra-ns/hello", "true"));
    assertTrue(GET("/dataStore/test-app-extra-ns/hello").content().booleanValue());
  }

  @Test
  void testWriteAccess_ReadWrite() {
    switchToNewUser("tester5", "test-app-common");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/test-app-extra-ns/hello", "true"));
    assertTrue(GET("/dataStore/test-app-extra-ns/hello").content().booleanValue());
  }
}
