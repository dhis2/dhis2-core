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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.webapi.utils.HeaderUtils.X_CONTENT_TYPE_OPTIONS_VALUE;
import static org.hisp.dhis.webapi.utils.HeaderUtils.X_XSS_PROTECTION_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.TrackedEntityInstanceSupportService;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceAsyncStrategyImpl;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceStrategyImpl;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceSyncStrategyImpl;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class TrackedEntityInstanceControllerTest {

  private MockMvc mockMvc;

  @Mock private CurrentUserService currentUserService;

  @Mock private TrackedEntityInstanceAsyncStrategyImpl trackedEntityInstanceAsyncStrategy;

  @Mock private TrackedEntityInstanceSyncStrategyImpl trackedEntityInstanceSyncStrategy;

  @Mock private DhisConfigurationProvider config;

  @Mock private User user;

  @Mock private org.hisp.dhis.trackedentity.TrackedEntityInstanceService instanceService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private FileResourceService fileResourceService;

  @Mock private TrackedEntityInstance trackedEntityInstance;

  @Mock private TrackedEntityInstanceSupportService trackedEntityInstanceSupportService;

  private static final String ENDPOINT = TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT;

  @BeforeEach
  public void setUp() throws BadRequestException, IOException {
    final TrackedEntityInstanceController controller =
        new TrackedEntityInstanceController(
            mock(TrackedEntityInstanceService.class),
            instanceService,
            null,
            null,
            null,
            currentUserService,
            fileResourceService,
            trackerAccessManager,
            trackedEntityInstanceSupportService,
            null,
            new TrackedEntityInstanceStrategyImpl(
                trackedEntityInstanceSyncStrategy, trackedEntityInstanceAsyncStrategy),
            config);

    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void shouldRetrieveImageAsAnAttachment() throws Exception {
    String teUid = CodeGenerator.generateUid();
    String attributeUid = CodeGenerator.generateUid();
    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setUid(attributeUid);
    attribute.setValueType(ValueType.IMAGE);
    TrackedEntityTypeAttribute teta = new TrackedEntityTypeAttribute();
    teta.setUid(attributeUid);
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setTrackedEntityTypeAttributes(List.of(teta));
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setAttribute(attribute);
    attributeValue.setValue("fileName");
    FileResource fileResource = new FileResource();
    fileResource.setDomain(FileResourceDomain.DATA_VALUE);
    fileResource.setStorageStatus(FileResourceStorageStatus.STORED);
    fileResource.setContentType("image/png");
    fileResource.setName("dhis2.png");

    File file = new ClassPathResource("images/dhis2.png").getFile();

    when(fileResourceService.getFileResource("fileName")).thenReturn(fileResource);
    when(fileResourceService.getFileResourceContent(fileResource))
        .thenReturn(new FileInputStream(file));
    when(config.getProperty(ConfigurationKey.CSP_HEADER_VALUE)).thenReturn("script-src 'none';");
    when(trackedEntityInstanceSupportService.getTrackedEntityAttributeValue(
            teUid, attributeUid, null))
        .thenReturn(attributeValue);

    mockMvc
        .perform(
            get(ENDPOINT + "/" + teUid + "/" + attributeUid + "/image")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(header().string(ContextUtils.HEADER_CONTENT_DISPOSITION, "filename=dhis2.png"))
        .andExpect(header().string("Content-Security-Policy", "script-src 'none';"))
        .andExpect(header().string("X-Content-Type-Options", X_CONTENT_TYPE_OPTIONS_VALUE))
        .andExpect(header().string("X-XSS-Protection", X_XSS_PROTECTION_VALUE))
        .andReturn();
  }

  @Test
  void shouldRetrieveFileAsAnAttachment() throws Exception {
    String teUid = CodeGenerator.generateUid();
    String attributeUid = CodeGenerator.generateUid();
    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setUid(attributeUid);
    attribute.setValueType(ValueType.FILE_RESOURCE);
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setAttribute(attribute);
    attributeValue.setValue("fileName");
    FileResource fileResource = new FileResource();
    fileResource.setDomain(FileResourceDomain.DATA_VALUE);
    fileResource.setStorageStatus(FileResourceStorageStatus.STORED);
    fileResource.setContentType("file/png");
    fileResource.setName("dhis2.png");

    when(instanceService.getTrackedEntityInstance(teUid)).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(Set.of(attributeValue));
    when(fileResourceService.getFileResource("fileName")).thenReturn(fileResource);
    when(config.getProperty(ConfigurationKey.CSP_HEADER_VALUE)).thenReturn("script-src 'none';");

    mockMvc
        .perform(
            get(ENDPOINT + "/" + teUid + "/" + attributeUid + "/file")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(ContextUtils.HEADER_CONTENT_DISPOSITION, "attachment; filename=dhis2.png"))
        .andExpect(header().string("Content-Security-Policy", "script-src 'none';"))
        .andExpect(header().string("X-Content-Type-Options", X_CONTENT_TYPE_OPTIONS_VALUE))
        .andExpect(header().string("X-XSS-Protection", X_XSS_PROTECTION_VALUE))
        .andReturn();
  }

  @Test
  void shouldCallSyncStrategy() throws Exception {
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(trackedEntityInstanceSyncStrategy.mergeOrDeleteTrackedEntityInstances(any()))
        .thenReturn(new ImportSummaries());

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andReturn();

    verify(trackedEntityInstanceSyncStrategy, times(1)).mergeOrDeleteTrackedEntityInstances(any());
    verify(trackedEntityInstanceAsyncStrategy, times(0)).mergeOrDeleteTrackedEntityInstances(any());
  }

  @Test
  void shouldCallAsyncStrategy() throws Exception {
    when(currentUserService.getCurrentUser()).thenReturn(user);
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .param("async", "true")
                .content("{}"))
        .andExpect(status().isOk())
        .andReturn();

    verify(trackedEntityInstanceSyncStrategy, times(0)).mergeOrDeleteTrackedEntityInstances(any());
    verify(trackedEntityInstanceAsyncStrategy, times(1)).mergeOrDeleteTrackedEntityInstances(any());
  }
}
