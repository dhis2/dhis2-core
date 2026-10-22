/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hisp.dhis.web.HttpStatus.*;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class DocumentControllerTest extends DhisControllerConvenienceTest {
  @Autowired private FileResourceService fileResourceService;
  @Autowired private FileResourceContentStore fileResourceContentStore;

  @ParameterizedTest
  @CsvSource({
    "DOCUMENT, false",
    "DOCUMENT, true",
    "MESSAGE_ATTACHMENT, false",
    "DATA_VALUE, false"
  })
  void cannotBindUnassignedFileOfAnotherUser(FileResourceDomain domain, boolean update) {
    String foreignFile = upload("foreign", domain);
    switchToNewUser("documentAuthor", "F_DOCUMENT_PUBLIC_ADD", "F_DOCUMENT_PRIVATE_ADD");
    HttpResponse response;
    if (update) {
      String ownFile = upload("own", FileResourceDomain.DOCUMENT);
      String id = createDocument("Own", ownFile);
      response =
          PATCH(
              "/documents/" + id, "[{'op':'replace','path':'/url','value':'" + foreignFile + "'}]");
      assertEquals(ownFile, GET("/documents/" + id).content(OK).getString("url").string());
    } else {
      response =
          POST("/documents", "{'name':'Other','url':'" + foreignFile + "','external':false}");
    }
    assertEquals("E4094", errorCode(response));
    assertEquals(UNAUTHORIZED, GET("/fileResources/{uid}/data", foreignFile).status());
  }

  @Test
  void cannotBindOwnFileOfAnotherDomain() {
    String file = upload("value", FileResourceDomain.DATA_VALUE);
    assertEquals(
        "E4094",
        errorCode(POST("/documents", "{'name':'Other','url':'" + file + "','external':false}")));
    assertEquals(
        "DATA_VALUE", GET("/fileResources/{uid}", file).content(OK).getString("domain").string());
  }

  @Test
  void canBindOwnDocumentFile() {
    switchToNewUser("documentUploader", "F_DOCUMENT_PUBLIC_ADD", "F_DOCUMENT_PRIVATE_ADD");
    String id = createDocument("Own", upload("mine", FileResourceDomain.DOCUMENT));
    assertEquals("mine", GET("/documents/" + id + "/data").content("text/plain"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void cannotAssignAnotherDocumentsFile(boolean update) {
    String assigned = upload("owned", FileResourceDomain.DOCUMENT);
    String ownerId = createDocument("Owner", assigned);
    HttpResponse response;
    if (update) {
      String own = upload("original", FileResourceDomain.DOCUMENT);
      String id = createDocument("Other", own);
      response =
          PATCH("/documents/" + id, "[{'op':'replace','path':'/url','value':'" + assigned + "'}]");
      assertEquals(own, GET("/documents/" + id).content(OK).getString("url").string());
    } else {
      response = POST("/documents", "{'name':'Other','url':'" + assigned + "','external':false}");
    }
    assertEquals("E4016", errorCode(response));
    assertEquals("owned", GET("/documents/" + ownerId + "/data").content("text/plain"));
  }

  @Test
  void canKeepAlreadyBoundFileOnUpdate() {
    String file = upload("original", FileResourceDomain.DOCUMENT);
    String id = createDocument("Document", file);
    assertStatus(
        OK, PATCH("/documents/" + id, "[{'op':'replace','path':'/name','value':'Renamed'}]"));
    assertEquals("Renamed", GET("/documents/" + id).content(OK).getString("name").string());
    assertEquals(file, GET("/documents/" + id).content(OK).getString("url").string());
    assertEquals("original", GET("/documents/" + id + "/data").content("text/plain"));
  }

  @Test
  void canReplaceFileWithUnassignedResource() {
    String id = createDocument("Document", upload("original", FileResourceDomain.DOCUMENT));
    String replacement = upload("replacement", FileResourceDomain.DOCUMENT);
    assertStatus(
        OK,
        PATCH(
            "/documents/" + id, "[{'op':'replace','path':'/url','value':'" + replacement + "'}]"));
    assertEquals("replacement", GET("/documents/" + id + "/data").content("text/plain"));
  }

  private static String errorCode(HttpResponse response) {
    return response
        .content(CONFLICT)
        .getObject("response")
        .getArray("errorReports")
        .getObject(0)
        .getString("errorCode")
        .string();
  }

  private String createDocument(String name, String file) {
    return assertStatus(
        CREATED,
        POST("/documents", "{'name':'" + name + "','url':'" + file + "','external':false}"));
  }

  private String upload(String content, FileResourceDomain domain) {
    byte[] bytes = content.getBytes(UTF_8);
    String uid =
        POST_MULTIPART(
                "/fileResources?domain=" + domain.name(),
                new MockMultipartFile("file", "document.txt", "text/plain", bytes))
            .content(ACCEPTED)
            .getObject("response")
            .getObject("fileResource")
            .getString("id")
            .string();
    fileResourceContentStore.saveFileResourceContent(
        fileResourceService.getFileResource(uid), bytes);
    return uid;
  }
}
