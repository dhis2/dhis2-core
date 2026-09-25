/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.ACCEPTED;
import static org.hisp.dhis.http.HttpStatus.CONFLICT;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.http.HttpStatus.NO_CONTENT;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs
 */
@Transactional
class DocumentControllerTest extends H2ControllerIntegrationTestBase {
  private String owner;
  private String user;
  private String group;

  @BeforeEach
  void setUpSharing() {
    owner = GET("/me?fields=id").content(OK).getString("id").string();
    User reader = makeUser("R");
    userService.addUser(reader);
    user = reader.getUid();
    group = assertStatus(CREATED, POST("/userGroups", "{'name':'Document readers'}"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createRetainsExplicitSharing(boolean external) {
    String url = external ? "https://example.org/document" : upload("original");
    String id =
        assertStatus(
            CREATED,
            POST(
                "/documents",
                "{'name':'Document','url':'%s','external':%s,'sharing':%s}"
                    .formatted(url, external, sharing("r-------"))));

    assertSharing(id, "r-------");
  }

  @ParameterizedTest
  @CsvSource({"true, PATCH", "false, PATCH", "true, PUT", "false, PUT"})
  void renameRetainsSharingAndFile(boolean external, String method) {
    String url = external ? "https://example.org/document" : upload("original");
    String id = createDocument("Document", url, external);
    assertStatus(NO_CONTENT, PUT("/documents/" + id + "/sharing", sharing("r-------")));

    HttpResponse response =
        switch (method) {
          case "PATCH" ->
              PATCH("/documents/" + id, "[{'op':'replace','path':'/name','value':'Renamed'}]");
          case "PUT" ->
              PUT(
                  "/documents/" + id + "?skipSharing=true",
                  "{'name':'Renamed','url':'%s','external':%s}".formatted(url, external));
          default -> throw new IllegalArgumentException(method);
        };
    assertStatus(OK, response);

    JsonObject document = GET("/documents/" + id).content(OK);
    assertEquals("Renamed", document.getString("name").string());
    assertEquals(url, document.getString("url").string());
    assertSharing(id, "r-------");
    if (!external) {
      assertEquals("original", GET("/documents/" + id + "/data").content("text/plain"));
    }
  }

  @Test
  void putAppliesExplicitSharingChanges() {
    String id = createDocument("Document", "https://example.org/document", true);
    assertStatus(NO_CONTENT, PUT("/documents/" + id + "/sharing", sharing("r-------")));

    assertStatus(
        OK,
        PUT(
            "/documents/" + id,
            "{'name':'Document','url':'https://example.org/document','external':true,'sharing':%s}"
                .formatted(sharing("rw------"))));

    assertSharing(id, "rw------");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void cannotAssignAnotherDocumentsFile(boolean update) {
    String assignedFile = upload("owned");
    String ownerId = createDocument("Owner", assignedFile, false);
    HttpResponse response;
    if (update) {
      String ownFile = upload("original");
      String id = createDocument("Other", ownFile, false);
      response =
          PATCH(
              "/documents/" + id,
              "[{'op':'replace','path':'/url','value':'%s'}]".formatted(assignedFile));
      assertEquals(ownFile, GET("/documents/" + id).content(OK).getString("url").string());
    } else {
      response =
          POST(
              "/documents", "{'name':'Other','url':'%s','external':false}".formatted(assignedFile));
    }

    assertEquals(
        "E4016",
        response
            .content(CONFLICT)
            .getObject("response")
            .getArray("errorReports")
            .getObject(0)
            .getString("errorCode")
            .string());
    assertEquals("owned", GET("/documents/" + ownerId + "/data").content("text/plain"));
  }

  @Test
  void canReplaceFileWithUnassignedResource() {
    String id = createDocument("Document", upload("original"), false);
    String replacement = upload("replacement");

    assertStatus(
        OK,
        PATCH(
            "/documents/" + id,
            "[{'op':'replace','path':'/url','value':'%s'}]".formatted(replacement)));

    assertEquals("replacement", GET("/documents/" + id + "/data").content("text/plain"));
  }

  private String createDocument(String name, String url, boolean external) {
    return assertStatus(
        CREATED,
        POST(
            "/documents", "{'name':'%s','url':'%s','external':%s}".formatted(name, url, external)));
  }

  private String upload(String content) {
    return POST_MULTIPART(
            "/fileResources?domain=DOCUMENT",
            new MockMultipartFile("file", "document.txt", "text/plain", content.getBytes(UTF_8)))
        .content(ACCEPTED)
        .getObject("response")
        .getObject("fileResource")
        .getString("id")
        .string();
  }

  private String sharing(String access) {
    return """
        {'owner':'%s','public':'--------',
         'users':{'%s':{'id':'%s','access':'%s'}},
         'userGroups':{'%s':{'id':'%s','access':'%s'}}}
        """
        .formatted(owner, user, user, access, group, group, access);
  }

  private void assertSharing(String id, String access) {
    JsonObject sharing =
        GET("/documents/" + id + "?fields=sharing").content(OK).getObject("sharing");
    assertEquals(owner, sharing.getString("owner").string());
    assertEquals("--------", sharing.getString("public").string());
    assertEquals(access, sharing.getObject("users").getObject(user).getString("access").string());
    assertEquals(
        access, sharing.getObject("userGroups").getObject(group).getString("access").string());
  }
}
