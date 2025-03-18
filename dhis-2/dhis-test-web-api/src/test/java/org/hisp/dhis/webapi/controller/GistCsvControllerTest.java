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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpClientAdapter.Accept;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.List;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tests the CSV output features of the Gist API.
 *
 * @author Jan Bernitt
 */
@Disabled("TODO(DHIS2-17768 platform): adjust user setup so tests are less brittle")
class GistCsvControllerTest extends AbstractGistControllerTest {
  private static final MediaType TEXT_CSV = new MediaType("text", "csv");

  @Test
  void testList() {
    assertAllUserCsv(
        GET("/users/gist?fields=id,code,education,twitter,employer", Accept(TEXT_CSV)));
  }

  @Test
  void testObject() {
    assertUserCsv(
        GET(
            "/users/" + getAdminUid() + "/gist?fields=id,code,education,twitter,employer",
            Accept(TEXT_CSV)));
  }

  @Test
  void testPropertyList() {
    String id = GET("/userGroups/gist?fields=id&headless=true").content().getString(0).string();
    assertUserCsv(
        GET(
            "/userGroups/" + id + "/users/gist?fields=id,code,education,twitter,employer",
            Accept(TEXT_CSV)));
  }

  private void assertAllUserCsv(HttpResponse response) {
    List<String> split = List.of(response.content(TEXT_CSV.toString()).split("\n"));
    List<User> allUsers = userService.getAllUsers();

    // Remove admin user
    List<String> cleanSplit =
        split.stream().filter(l -> !l.contains(getAdminUser().getUid())).toList();

    assertLinesMatch(
        List.of(
            "id,code,education,twitter,employer",
            allUsers.get(1).getUid() + ",Codeadmin,,,",
            allUsers.get(2).getUid() + ",CodeuserGist,,,"),
        cleanSplit);
  }

  private void assertUserCsv(HttpResponse response) {
    List<String> split = List.of(response.content(TEXT_CSV.toString()).split("\n"));
    List<User> allUsers = userService.getAllUsers();

    assertLinesMatch(
        List.of("id,code,education,twitter,employer", allUsers.get(1).getUid() + ",Codeadmin,,,"),
        split);
  }
}
