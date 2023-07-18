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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class FileResourceControllerMockTest {

  private FileResourceController controller;

  @Mock private FileResourceService fileResourceService;

  @Mock private CurrentUserService currentUserService;

  @Mock private FileResourceUtils fileResourceUtils;

  @Mock private DhisConfigurationProvider dhisConfig;

  @Test
  void testGetOrgUnitImage() throws WebMessageException, IOException {
    controller = new FileResourceController(fileResourceService, fileResourceUtils, dhisConfig);
    FileResource fileResource = new FileResource();
    fileResource.setContentType("image/png");
    fileResource.setDomain(FileResourceDomain.ORG_UNIT);
    fileResource.setUid("id");

    when(fileResourceService.getFileResource("id")).thenReturn(fileResource);

    controller.getFileResourceData(
        "id", new MockHttpServletResponse(), null, currentUserService.getCurrentUser());

    verify(fileResourceService).copyFileResourceContent(any(), any());
  }

  @Test
  void testGetDataValue() {
    controller = new FileResourceController(fileResourceService, fileResourceUtils, dhisConfig);
    FileResource fileResource = new FileResource();
    fileResource.setContentType("image/png");
    fileResource.setDomain(FileResourceDomain.DATA_VALUE);
    fileResource.setUid("id");

    when(fileResourceService.getFileResource("id")).thenReturn(fileResource);

    assertThrows(
        WebMessageException.class,
        () ->
            controller.getFileResourceData(
                "id", new MockHttpServletResponse(), null, currentUserService.getCurrentUser()));
  }
}
