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
package org.hisp.dhis.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

class IconTest extends TrackerTest {
  @Autowired private FileResourceService fileResourceService;

  @Autowired private CustomIconService iconService;
  @Autowired protected UserService _userService;
  private final List<String> keywords = List.of("k1", "k2", "k3");

  @SneakyThrows
  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    User currentUser = userService.getUserByUsername(currentUsername);
    injectSecurityContextUser(currentUser);

    FileResource fileResource = createAndPersistFileResource('A');
    iconService.addCustomIcon(
        new CustomIcon("iconKey", "description", keywords, true, fileResource));
  }

  @Test
  void shouldGetAllIconsByDefault() {}

  @Test
  void shouldGetDefaultIconWhenKeyBelongsToDefaultIcon() throws NotFoundException {}

  @Test
  void shouldGetAllKeywordsWhenRequested() {}

  @Test
  void shouldGetAllIconsFilteredByKeywordWhenRequested() {}

  @Test
  void shouldGetCustomIconsFilteredByKeywordWhenRequested() {}

  @Test
  void shouldGetIconDataWhenKeyBelongsToDefaultIcon() throws NotFoundException, IOException {}

  @Test
  void shouldFailWhenGettingIconDataOfNonDefaultIcon() {
    Exception exception =
        assertThrows(
            NotFoundException.class, () -> iconService.getCustomIconResource("madeUpIconKey"));

    assertEquals("No default icon found with key madeUpIconKey.", exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingCustomIconAndDefaultIconWithSameKeyExists() {}

  @Test
  void shouldUpdateLastUpdatedWhenCustomIconIsUpdated() {}

  public FileResource createAndPersistFileResource(char uniqueChar) {
    byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    String filename = "filename" + uniqueChar;

    HashCode contentMd5 = Hashing.md5().hashBytes(content);
    String contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

    FileResource fileResource =
        new FileResource(
            filename,
            contentType,
            content.length,
            contentMd5.toString(),
            FileResourceDomain.CUSTOM_ICON);
    fileResource.setAssigned(false);
    fileResource.setCreated(new Date());
    fileResource.setAutoFields();

    String fileResourceUid = fileResourceService.asyncSaveFileResource(fileResource, content);
    return fileResourceService.getFileResource(fileResourceUid);
  }
}
