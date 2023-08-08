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
package org.hisp.dhis.actions;

import static io.restassured.RestAssured.oauth2;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.RestAssured;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.config.TestConfiguration;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class LoginActions {
  /**
   * Makes sure user with given name is logged in. Will throw assertion exception if authentication
   * is not successful.
   *
   * @param username
   * @param password
   */
  public void loginAsUser(final String username, final String password) {
    ApiResponse loggedInUser = getLoggedInUserInfo();

    if (loggedInUser.getContentType().contains("json")
        && loggedInUser.extract("username") != null
        && loggedInUser.extract("username").equals(username)) {
      return;
    }

    addAuthenticationHeader(username, password);

    getLoggedInUserInfo().validate().statusCode(200).body("username", equalTo(username));
  }

  /**
   * Makes sure user configured in env variables is logged in. Will throw assertion exception if
   * authentication is not successful.
   */
  public void loginAsSuperUser() {
    String username = TestConfiguration.get().superUserUsername();
    String password = TestConfiguration.get().superUserPassword();
    loginAsUser(username, password);
  }

  /**
   * Makes sure user admin:district is logged in. Will throw assertion exception if authentication
   * is not successful.
   */
  public void loginAsDefaultUser() {
    String username = TestConfiguration.get().defaultUserUsername();
    String password = TestConfiguration.get().defaultUSerPassword();
    loginAsUser(username, password);
  }

  public ApiResponse getLoggedInUserInfo() {
    return new RestApiActions("/me").get();
  }

  public String getLoggedInUserId() {
    return getLoggedInUserInfo().extractString("id");
  }

  /**
   * Adds authentication header that is used in all consecutive requests
   *
   * @param username
   * @param password
   */
  public void addAuthenticationHeader(final String username, final String password) {
    RestAssured.authentication = RestAssured.preemptive().basic(username, password);
  }

  /** Removes authentication header */
  public void removeAuthenticationHeader() {
    RestAssured.authentication = RestAssured.DEFAULT_AUTH;
  }

  /**
   * Logs in with oAuth2 token
   *
   * @param token
   */
  public void loginWithToken(String token) {
    RestAssured.authentication = oauth2(token);
  }

  public void loginAsAdmin() {
    loginAsUser(
        TestConfiguration.get().adminUserUsername(), TestConfiguration.get().adminUserPassword());
  }
}
