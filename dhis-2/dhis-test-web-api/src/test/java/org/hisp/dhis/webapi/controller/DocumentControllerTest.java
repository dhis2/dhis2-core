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
import static org.hisp.dhis.web.HttpStatus.ACCEPTED;
import static org.hisp.dhis.web.HttpStatus.CONFLICT;
import static org.hisp.dhis.web.HttpStatus.CREATED;
import static org.hisp.dhis.web.HttpStatus.FORBIDDEN;
import static org.hisp.dhis.web.HttpStatus.OK;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs
 */
@Transactional
class DocumentControllerTest extends DhisControllerConvenienceTest {

  @ParameterizedTest
  @CsvSource({
    "DOCUMENT, false",
    "DOCUMENT, true",
    "JOB_DATA, false",
    "MESSAGE_ATTACHMENT, false",
    "DATA_VALUE, false"
  })
  void cannotBindUnassignedFileOfAnotherUser(FileResourceDomain domain, boolean update) {
    String foreignFile = upload("foreign", domain);

    switchToNewUser("documentAuthor", "F_DOCUMENT_PUBLIC_ADD", "F_DOCUMENT_PRIVATE_ADD");
    HttpResponse response;
    if (update) {
      String ownFile = upload("own", FileResourceDomain.DOCUMENT);
      String id = createDocument("Own", ownFile, false);
      response =
          PATCH(
              "/documents/" + id,
              "[{'op':'replace','path':'/url','value':'%s'}]".formatted(foreignFile));
      assertEquals(ownFile, GET("/documents/" + id).content(OK).getString("url").string());
    } else {
      response =
          POST("/documents", "{'name':'Other','url':'%s','external':false}".formatted(foreignFile));
    }

    assertEquals("E4094", errorCode(response));
    assertEquals(FORBIDDEN, GET("/fileResources/{uid}/data", foreignFile).status());
  }

  @Test
  void cannotBindOwnFileOfAnotherDomain() {
    String dataValueFile = upload("value", FileResourceDomain.DATA_VALUE);

    assertEquals(
        "E4094",
        errorCode(
            POST(
                "/documents",
                "{'name':'Other','url':'%s','external':false}".formatted(dataValueFile))));
    assertEquals(
        "DATA_VALUE",
        GET("/fileResources/{uid}", dataValueFile).content(OK).getString("domain").string());
  }

  @Test
  void canBindOwnDocumentFile() {
    switchToNewUser("documentUploader", "F_DOCUMENT_PUBLIC_ADD", "F_DOCUMENT_PRIVATE_ADD");
    String id = createDocument("Own", upload("mine", FileResourceDomain.DOCUMENT), false);

    assertEquals("mine", GET("/documents/" + id + "/data").content("text/plain"));
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

  private String createDocument(String name, String url, boolean external) {
    return assertStatus(
        CREATED,
        POST(
            "/documents", "{'name':'%s','url':'%s','external':%s}".formatted(name, url, external)));
  }

  private String upload(String content, FileResourceDomain domain) {
    return POST_MULTIPART(
            "/fileResources?domain=" + domain.name(),
            new MockMultipartFile("file", "document.txt", "text/plain", content.getBytes(UTF_8)))
        .content(ACCEPTED)
        .getObject("response")
        .getObject("fileResource")
        .getString("id")
        .string();
  }
}
