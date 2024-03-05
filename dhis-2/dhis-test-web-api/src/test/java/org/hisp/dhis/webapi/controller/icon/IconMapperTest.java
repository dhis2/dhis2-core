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
package org.hisp.dhis.webapi.controller.icon;

import static org.hisp.dhis.fileresource.FileResourceDomain.CUSTOM_ICON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.Icon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IconMapperTest {

  @Mock private FileResourceService fileResourceService;

  private static final String KEY = "icon key";
  private static final String CODE = "icon_code";

  private static final String DESCRIPTION = "description";

  private static final Set<String> KEYWORDS = Set.of("k1", "k2");

  private static final FileResource fileResource = new FileResource();
  private static final FileResource fileResourceUpdated = new FileResource();

  private IconMapper iconMapper;

  @BeforeEach
  void setUp() {
    fileResource.setUid("file_uid1");
    fileResourceUpdated.setUid("file_uid2");
    iconMapper = new IconMapper(fileResourceService);
  }

  @Test
  void shouldReturnCustomIconFromIconDto() throws BadRequestException {
    IconRequest iconRequest =
        new IconRequest(KEY, DESCRIPTION, KEYWORDS, fileResource.getUid(), true);
    when(fileResourceService.getFileResource(fileResource.getUid(), CUSTOM_ICON))
        .thenReturn(Optional.of(fileResource));

    Icon icon = iconMapper.to(iconRequest);

    assertEquals(KEY, icon.getKey());
    assertEquals(DESCRIPTION, icon.getDescription());
    assertEquals(KEYWORDS, icon.getKeywords());
    assertEquals(fileResource.getUid(), icon.getFileResource().getUid());
  }

  @Test
  void shouldReturnDefaultIconFromIconDto() throws BadRequestException {
    IconRequest iconRequest =
        new IconRequest(KEY, DESCRIPTION, KEYWORDS, fileResource.getUid(), false);

    Icon defaultIcon = iconMapper.to(iconRequest);

    verify(fileResourceService, times(0))
        .getFileResource(anyString(), any(FileResourceDomain.class));
    assertEquals(KEY, defaultIcon.getKey());
    assertEquals(DESCRIPTION, defaultIcon.getDescription());
    assertEquals(KEYWORDS, defaultIcon.getKeywords());
    assertNull(defaultIcon.getFileResource());
  }

  @Test
  void shouldMergeIconRequestToPersistedIcon() throws BadRequestException {
    Icon persisted = new Icon(KEY, DESCRIPTION, KEYWORDS, true, fileResource);
    String updatedDescription = DESCRIPTION + "1";
    IconRequest iconRequest = new IconRequest(KEY, updatedDescription, KEYWORDS, "file_uid2", true);

    when(fileResourceService.getFileResource(anyString(), any(FileResourceDomain.class)))
        .thenReturn(Optional.of(fileResourceUpdated));

    iconMapper.merge(persisted, iconRequest);

    assertEquals(updatedDescription, persisted.getDescription());
    assertSame(fileResourceUpdated, persisted.getFileResource());
  }

  @Test
  void shouldFailMergeWhenFileDoesNotExist() throws BadRequestException {
    Icon persisted = new Icon(KEY, DESCRIPTION, KEYWORDS, true, fileResource);

    IconRequest iconRequest =
        new IconRequest(KEY, DESCRIPTION, KEYWORDS, fileResource.getUid(), true);

    when(fileResourceService.getFileResource(anyString(), any(FileResourceDomain.class)))
        .thenReturn(Optional.empty());

    Exception exception =
        assertThrows(BadRequestException.class, () -> iconMapper.merge(persisted, iconRequest));
    assertEquals(
        String.format("FileResource with uid %s does not exist", iconRequest.getFileResourceId()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenMappingToCustomIconWithNonExistentFileResource() {
    IconRequest iconRequest =
        new IconRequest(KEY, DESCRIPTION, KEYWORDS, fileResource.getUid(), true);

    Exception exception = assertThrows(BadRequestException.class, () -> iconMapper.to(iconRequest));
    assertEquals(
        String.format("FileResource with uid %s does not exist", iconRequest.getFileResourceId()),
        exception.getMessage());
  }
}
