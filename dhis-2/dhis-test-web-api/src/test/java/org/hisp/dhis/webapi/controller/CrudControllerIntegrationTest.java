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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CrudControllerIntegrationTest extends DhisControllerIntegrationTest {

  @Autowired private UserSettingService userSettingService;

  @Autowired private SystemSettingManager systemSettingManager;

  @Test
  void testGetNonAccessibleObject() {
    User admin = getCurrentUser();
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly', 'sharing':{'public':'--------','owner': '"
                    + admin.getUid()
                    + "'}}"));

    User testUser = createAndAddUser("test");
    injectSecurityContextUser(testUser);

    GET("/dataSets/{id}", id).content(HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetAccessibleObject() {
    User testUser = createAndAddUser("test", null, "F_DATASET_PRIVATE_ADD");
    injectSecurityContextUser(testUser);
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly', 'sharing':{'public':'--------','owner': '"
                    + testUser.getUid()
                    + "'}}"));

    DataSet dataSet = manager.get(DataSet.class, id);
    assertEquals(testUser.getUid(), dataSet.getSharing().getOwner());

    GET("/dataSets/{id}", id).content(HttpStatus.OK);
  }

  @Test
  @DisplayName("Search by token should use translations column instead of default columns")
  void testSearchByToken() {
    setUpTranslation();
    User userA = createAndAddUser("userA", null, "ALL");
    userSettingService.saveUserSetting(UserSettingKey.DB_LOCALE, Locale.FRENCH, userA);

    injectSecurityContextUser(userService.getUserByUsername(userA.getUsername()));

    // TODO: MAS look into this
    //    assertTrue(
    //
    // GET("/dataSets?filter=identifiable:token:bb").content().getArray("dataSets").isEmpty());
    assertFalse(
        GET("/dataSets?filter=identifiable:token:fr").content().getArray("dataSets").isEmpty());
    assertFalse(
        GET("/dataSets?filter=identifiable:token:dataSet")
            .content()
            .getArray("dataSets")
            .isEmpty());
  }

  @Test
  @DisplayName("Search by token should use default properties instead of translations column")
  void testSearchTokenDefaultLocale() {
    setUpTranslation();
    User userA = createAndAddUser("userA", null, "ALL");
    userSettingService.saveUserSetting(UserSettingKey.DB_LOCALE, Locale.ENGLISH, userA);
    injectSecurityContextUser(userA);
    systemSettingManager.saveSystemSetting(SettingKey.DB_LOCALE, Locale.ENGLISH);
    assertTrue(
        GET("/dataSets?filter=identifiable:token:bb").content().getArray("dataSets").isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:fr").content().getArray("dataSets").isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:dataSet")
            .content()
            .getArray("dataSets")
            .isEmpty());

    assertFalse(
        GET("/dataSets?filter=identifiable:token:my").content().getArray("dataSets").isEmpty());
  }

  @Test
  @DisplayName("Search by token should use default properties instead of translations column")
  void testSearchTokenWithNullLocale() {
    setUpTranslation();
    User userA = createAndAddUser("userA", null, "ALL");
    injectSecurityContextUser(userA);

    systemSettingManager.saveSystemSetting(SettingKey.DB_LOCALE, Locale.ENGLISH);
    assertTrue(
        GET("/dataSets?filter=identifiable:token:bb").content().getArray("dataSets").isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:fr").content().getArray("dataSets").isEmpty());
    assertTrue(
        GET("/dataSets?filter=identifiable:token:dataSet")
            .content()
            .getArray("dataSets")
            .isEmpty());

    assertFalse(
        GET("/dataSets?filter=identifiable:token:my").content().getArray("dataSets").isEmpty());
  }

  private void setUpTranslation() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    PUT(
            "/dataSets/" + id + "/translations",
            "{'translations': [{'locale':'fr', 'property':'NAME', 'value':'fr dataSet'}]}")
        .content(HttpStatus.NO_CONTENT);

    JsonArray translations =
        GET("/dataSets/{id}/translations", id).content().getArray("translations");
    assertEquals(1, translations.size());

    assertTrue(
        GET("/dataSets?filter=identifiable:token:fr", id).content().getArray("dataSets").isEmpty());
  }
}
