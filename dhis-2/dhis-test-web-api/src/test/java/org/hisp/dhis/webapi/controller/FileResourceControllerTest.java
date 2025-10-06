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

import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FileResourceControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testSaveTooBigFileSize() {
    byte[] bytes = new byte[10_000_001];
    MockMultipartFile image =
        new MockMultipartFile("file", "OU_profile_image.png", "image/png", bytes);
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "File size can't be bigger than 10000000, current file size 10000001",
        errorMessage.string());
  }

  @Test
  void testSaveBadAvatarImageData() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage =
        response.content(HttpStatus.INTERNAL_SERVER_ERROR).getString("message");
    assertEquals("Failed to resize image: src cannot be null", errorMessage.string());
  }

  @Test
  void testSaveBadAvatarContentType() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/tiff", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "Invalid content type, valid content types are: image/jpeg,image/png,image/gif",
        errorMessage.string());
  }

  @Test
  void testSaveBadAvatarFileExtension() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.tiff", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "Wrong file extension, valid extensions are: jpg,jpeg,png,gif", errorMessage.string());
  }

  @Test
  void testSaveBadAvatarFileSize() {
    byte[] bytes = new byte[2_000_001];
    MockMultipartFile image =
        new MockMultipartFile("file", "OU_profile_image.png", "image/png", bytes);
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "File size can't be bigger than 2000000, current file size 2000001", errorMessage.string());
  }

  @Test
  void testSaveGoodAvatar() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile("file", "dhis2.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResourceUid");
    assertEquals("dhis2.png", savedObject.getString("name").string());
  }

  @Test
  void testSaveOrgUnitImage() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResourceUid");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());

    String uid = savedObject.getString("id").string();
    JsonObject fr = GET("/fileResources/{uid}", uid).content();
    assertEquals("ORG_UNIT", fr.getString("domain").string());

    JsonObject large = GET("/fileResources/{uid}?dimension=LARGE", uid).content();
    assertEquals("ORG_UNIT", large.getString("domain").string());
  }

  @Test
  void testSaveOrgUnitImageWithUid() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789a", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResourceUid");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("0123456789a", savedObject.getString("id").string());
  }

  @Test
  void testSaveOrgUnitImageWithUid_Update() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789x", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResourceUid");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("0123456789x", savedObject.getString("id").string());

    // now update the resource with a different image but the same UID
    MockMultipartFile image2 =
        new MockMultipartFile(
            "file", "OU_profile_image2.png", "image/png", Files.readAllBytes(file.toPath()));

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
