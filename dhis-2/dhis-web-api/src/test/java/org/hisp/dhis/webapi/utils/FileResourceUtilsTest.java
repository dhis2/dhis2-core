/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileResourceUtilsTest {

  @Test
  @DisplayName("When resizing an Org Unit image, its content type is kept")
  void orgUnitImageResizingKeepsContentType() throws IOException {
    // given a png image file exists
    InputStream in = getClass().getResourceAsStream("/fileResources/org_unit.png");
    MockMultipartFile file = new MockMultipartFile("file", "org_unit.png", "image/png", in);

    // when the file is processed for resizing
    MultipartFile resizedFile = FileResourceUtils.resizeOrgToDefaultSize(file);

    // then its original content type is kept
    String contentType = resizedFile.getContentType();
    assertNotNull(contentType);
    assertEquals("image/png", contentType);
  }

  @Test
  @DisplayName("When resizing an Avatar image, its content type is kept")
  void avatarImageResizingKeepsContentType() throws IOException {
    // given a png image file exists
    InputStream in = getClass().getResourceAsStream("/fileResources/user_avatar.png");
    MockMultipartFile file = new MockMultipartFile("file", "user_avatar.png", "image/png", in);

    // when the file is processed for resizing
    MultipartFile resizedFile = FileResourceUtils.resizeAvatarToDefaultSize(file);

    // then its original content type is kept
    String contentType = resizedFile.getContentType();
    assertNotNull(contentType);
    assertEquals("image/png", contentType);
  }
}
