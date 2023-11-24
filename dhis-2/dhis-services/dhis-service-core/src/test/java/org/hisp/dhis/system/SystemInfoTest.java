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
package org.hisp.dhis.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.system.database.DatabaseInfo;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SystemInfo}. */
class SystemInfoTest {

  @Test
  void testWithoutSensitiveInfo() {
    SystemInfo info =
        SystemInfo.builder()
            .jasperReportsVersion("x")
            .environmentVariable("x")
            .fileStoreProvider("x")
            .readOnlyMode("x")
            .nodeId("x")
            .javaVersion("x")
            .javaVendor("x")
            .javaOpts("x")
            .osName("x")
            .osArchitecture("x")
            .externalDirectory("x")
            .readReplicaCount(-1)
            .memoryInfo("x")
            .cpuCores(-1)
            .systemMonitoringUrl("x")
            .encryption(true)
            .redisEnabled(true)
            .redisHostname("x")
            .databaseInfo(DatabaseInfo.builder().name("x").build())
            .build();

    info = info.withoutSensitiveInfo();

    assertNull(info.getJasperReportsVersion());
    assertNull(info.getEnvironmentVariable());
    assertNull(info.getFileStoreProvider());
    assertNull(info.getReadOnlyMode());
    assertNull(info.getNodeId());
    assertNull(info.getJavaVersion());
    assertNull(info.getJavaVendor());
    assertNull(info.getJavaOpts());
    assertNull(info.getOsName());
    assertNull(info.getOsArchitecture());
    assertNull(info.getExternalDirectory());
    assertNull(info.getReadReplicaCount());
    assertNull(info.getMemoryInfo());
    assertNull(info.getCpuCores());
    assertNull(info.getSystemMonitoringUrl());
    assertNull(info.getRedisHostname());
    assertFalse(info.isRedisEnabled());
    assertFalse(info.isEncryption());
    assertNull(info.getDatabaseInfo().getName());
  }
}
