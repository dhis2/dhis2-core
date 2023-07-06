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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link SharingController}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class SharingControllerTest {

  @Mock private CurrentUserService currentUserService;

  @Mock private IdentifiableObjectManager manager;

  @Mock private AclService aclService;

  private MockHttpServletRequest request = new MockHttpServletRequest();

  @InjectMocks private SharingController sharingController;

  @Test
  void notSystemDefaultMetadataNoAccess() {
    final OrganisationUnit organisationUnit = new OrganisationUnit();

    doReturn(OrganisationUnit.class).when(aclService).classForType(eq("organisationUnit"));
    when(aclService.isClassShareable(eq(OrganisationUnit.class))).thenReturn(true);
    doReturn(organisationUnit).when(manager).getNoAcl(eq(OrganisationUnit.class), eq("kkSjhdhks"));
    assertThrows(
        AccessDeniedException.class,
        () -> sharingController.postSharing("organisationUnit", "kkSjhdhks", request));
  }

  @Test
  void systemDefaultMetadataNoAccess() {
    final Category category = new Category();
    category.setName(Category.DEFAULT_NAME + "x");

    doReturn(Category.class).when(aclService).classForType(eq("category"));
    when(aclService.isClassShareable(eq(Category.class))).thenReturn(true);
    when(manager.getNoAcl(eq(Category.class), eq("kkSjhdhks"))).thenReturn(category);
    assertThrows(
        AccessDeniedException.class,
        () -> sharingController.postSharing("category", "kkSjhdhks", request));
  }

  @Test
  void systemDefaultMetadata() throws Exception {
    final Category category = new Category();
    category.setName(Category.DEFAULT_NAME);

    doReturn(Category.class).when(aclService).classForType(eq("category"));
    when(aclService.isClassShareable(eq(Category.class))).thenReturn(true);
    when(manager.getNoAcl(eq(Category.class), eq("kkSjhdhks"))).thenReturn(category);

    WebMessage message = sharingController.postSharing("category", "kkSjhdhks", request);
    assertThat(
        message.getMessage(), containsString("Sharing settings of system default metadata object"));
  }
}
