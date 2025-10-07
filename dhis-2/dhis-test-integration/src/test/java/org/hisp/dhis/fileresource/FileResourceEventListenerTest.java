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
package org.hisp.dhis.fileresource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.persistence.EntityManager;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.fileresource.events.ImageFileSavedEvent;
import org.hisp.dhis.fileresource.hibernate.HibernateFileResourceStore;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.DefaultAuthenticationService;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luca Cambi
 */
@ExtendWith(MockitoExtension.class)
class FileResourceEventListenerTest extends IntegrationTestBase {

  @InjectMocks private FileResourceEventListener fileResourceEventListener;

  @Autowired private UserService _userService;

  @Mock FileResourceContentStore fileResourceContentStore;

  @Mock HibernateFileResourceStore fileResourceStore;

  @Mock ImageProcessingService imageProcessingService;

  @BeforeEach
  public void init() {
    userService = _userService;
    createAndAddUser("file_resource_user");
    fileResourceEventListener =
        new FileResourceEventListener(
            new DefaultFileResourceService(
                fileResourceStore, null, fileResourceContentStore, null, mock(EntityManager.class)),
            fileResourceContentStore,
            new DefaultAuthenticationService(userService),
            imageProcessingService);
  }

  @Test
  void shouldUpdateFileImageWithImageFileSaveEvent() throws IOException {
    FileResource fileResource = new FileResource();
    fileResource.setUid(CodeGenerator.generateUid());

    File file = File.createTempFile("file-resource", "test");

    Map<ImageFileDimension, File> map = Map.of(ImageFileDimension.LARGE, file);
    ImageFileSavedEvent event =
        new ImageFileSavedEvent(
            fileResource.getUid(), file, CurrentUserUtil.getCurrentUserDetails().getUid());

    when(fileResourceContentStore.saveFileResourceContent(fileResource, map)).thenReturn("uid");
    when(fileResourceStore.getByUid(fileResource.getUid())).thenReturn(fileResource);
    when(imageProcessingService.createImages(fileResource, event.file())).thenReturn(map);
    doCallRealMethod().when(fileResourceStore).update(any(FileResource.class));

    assertDoesNotThrow(() -> fileResourceEventListener.saveImageFile(event));

    verify(fileResourceStore).update(any(FileResource.class), any(UserDetails.class));
  }
}
