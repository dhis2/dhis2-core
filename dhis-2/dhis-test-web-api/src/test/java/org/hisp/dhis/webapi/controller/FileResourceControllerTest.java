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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileResourceControllerTest extends DhisControllerConvenienceTest {

  @Test
  void testSaveOrgUnitImage() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());

    String uid = savedObject.getString("id").string();
    JsonObject fr = GET("/fileResources/{uid}", uid).content();
    assertEquals("ORG_UNIT", fr.getString("domain").string());

    JsonObject large = GET("/fileResources/{uid}?dimension=LARGE", uid).content();
    assertEquals("ORG_UNIT", large.getString("domain").string());
  }

  @Test
  void testSaveOrgUnitImageWithUid() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789a", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("0123456789a", savedObject.getString("id").string());
  }

  @Test
  void testSaveOrgUnitImageWithUid_Update() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789x", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("0123456789x", savedObject.getString("id").string());

    // now update the resource with a different image but the same UID
    MockMultipartFile image2 =
        new MockMultipartFile(
            "file", "OU_profile_image2.png", "image/png", "<<png data>>".getBytes());

    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "ERROR",
            "FileResource already exists: `0123456789x`",
            POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789x", image2)
                .content(HttpStatus.CONFLICT));
    assertEquals(ErrorCode.E1119, message.getErrorCode());
  }
}
