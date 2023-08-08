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
package org.hisp.dhis.dxf2.metadata.sync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author aamerm
 */
@ExtendWith(MockitoExtension.class)
class MetadataSyncDelegateTest {

  @InjectMocks private MetadataSyncDelegate metadataSyncDelegate;

  @Mock private DefaultMetadataSystemSettingService metadataSystemSettingService;

  @Mock private SystemService systemService;

  @Mock private RenderService renderService;

  @Test
  void testShouldVerifyIfStopSyncReturnFalseIfNoSystemVersionInLocal() {
    String versionSnapshot =
        "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
    SystemInfo systemInfo = new SystemInfo();
    when(systemService.getSystemInfo()).thenReturn(systemInfo);
    boolean shouldStopSync = metadataSyncDelegate.shouldStopSync(versionSnapshot);
    assertFalse(shouldStopSync);
  }

  @Test
  void testShouldVerifyIfStopSyncReturnFalseIfNoSystemVersionInRemote() {
    String versionSnapshot =
        "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
    SystemInfo systemInfo = new SystemInfo();
    systemInfo.setVersion("2.26");
    when(systemService.getSystemInfo()).thenReturn(systemInfo);
    boolean shouldStopSync = metadataSyncDelegate.shouldStopSync(versionSnapshot);
    assertFalse(shouldStopSync);
  }

  @Test
  void testShouldVerifyIfStopSyncReturnTrueIfDHISVersionMismatch() throws IOException {
    String versionSnapshot =
        "{\"system:\": {\"date\":\"2016-06-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\","
            + "\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
    String systemNodeString = "{\"date\":\"2016-06-24T05:27:25.128+0000\", \"version\": \"2.26\"}";
    SystemInfo systemInfo = new SystemInfo();
    systemInfo.setVersion("2.25");
    when(systemService.getSystemInfo()).thenReturn(systemInfo);
    when(metadataSystemSettingService.getStopMetadataSyncSetting()).thenReturn(true);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(systemNodeString);
    when(renderService.getSystemObject(any(ByteArrayInputStream.class), eq(RenderFormat.JSON)))
        .thenReturn(jsonNode);

    boolean shouldStopSync = metadataSyncDelegate.shouldStopSync(versionSnapshot);
    assertTrue(shouldStopSync);
  }

  @Test
  void testShouldVerifyIfStopSyncReturnFalseIfDHISVersionSame() throws IOException {
    String versionSnapshot =
        "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
    String systemNodeString = "{\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}";
    SystemInfo systemInfo = new SystemInfo();
    systemInfo.setVersion("2.26");
    when(systemService.getSystemInfo()).thenReturn(systemInfo);
    when(metadataSystemSettingService.getStopMetadataSyncSetting()).thenReturn(true);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(systemNodeString);
    when(renderService.getSystemObject(any(ByteArrayInputStream.class), eq(RenderFormat.JSON)))
        .thenReturn(jsonNode);

    boolean shouldStopSync = metadataSyncDelegate.shouldStopSync(versionSnapshot);
    assertFalse(shouldStopSync);
  }

  @Test
  void testShouldVerifyIfStopSyncReturnFalseIfStopSyncIsNotSet() {
    String versionSnapshot =
        "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
    SystemInfo systemInfo = new SystemInfo();
    systemInfo.setVersion("2.26");

    when(systemService.getSystemInfo()).thenReturn(systemInfo);
    when(metadataSystemSettingService.getStopMetadataSyncSetting()).thenReturn(false);
    boolean shouldStopSync = metadataSyncDelegate.shouldStopSync(versionSnapshot);
    assertFalse(shouldStopSync);
  }
}
