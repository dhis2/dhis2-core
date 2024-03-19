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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author sultanm
 */
@ExtendWith(MockitoExtension.class)
class DefaultMetadataSyncServiceTest {

  private MetadataSyncService metadataSyncService;

  @Mock private MetadataVersionDelegate metadataVersionDelegate;

  @Mock private MetadataSyncDelegate metadataSyncDelegate;

  @Mock private MetadataVersionService metadataVersionService;

  @Mock private MetadataSyncImportHandler metadataSyncImportHandler;

  private Map<String, List<String>> parameters;

  @BeforeEach
  public void setup() {
    metadataSyncService =
        new DefaultMetadataSyncService(
            metadataVersionDelegate,
            metadataVersionService,
            metadataSyncDelegate,
            metadataSyncImportHandler);
    parameters = new HashMap<>();
  }

  @Test
  void testShouldThrowExceptionWhenVersionNameNotPresentInParameters() {
    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.getParamsFromMap(parameters),
        "Missing required parameter: 'versionName'");
  }

  @Test
  void testShouldThrowExceptionWhenParametersAreNull() {
    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.getParamsFromMap(null),
        "Missing required parameter: 'versionName'");
  }

  @Test
  void testShouldThrowExceptionWhenParametersHaveVersionNameAsNull() {
    parameters.put("versionName", null);

    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.getParamsFromMap(parameters),
        "Missing required parameter: 'versionName'");
  }

  @Test
  void testShouldThrowExceptionWhenParametersHaveVersionNameAssignedToEmptyList() {
    parameters.put("versionName", new ArrayList<>());

    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.getParamsFromMap(parameters),
        "Missing required parameter: 'versionName'");
  }

  @Test
  void testShouldReturnNullWhenVersionNameIsAssignedToListHavingNullEntry() {
    parameters.put("versionName", new ArrayList<>());
    parameters.get("versionName").add(null);

    MetadataSyncParams paramsFromMap = metadataSyncService.getParamsFromMap(parameters);

    assertNull(paramsFromMap.getVersion());
  }

  @Test
  void testShouldReturnNullWhenVersionNameIsAssignedToListHavingEmptyString() {
    parameters.put("versionName", new ArrayList<>());
    parameters.get("versionName").add("");

    MetadataSyncParams paramsFromMap = metadataSyncService.getParamsFromMap(parameters);

    assertNull(paramsFromMap.getVersion());
  }

  @Test
  void testShouldGetExceptionIfRemoteVersionIsNotAvailable() {
    parameters.put("versionName", new ArrayList<>());
    parameters.get("versionName").add("testVersion");

    when(metadataVersionDelegate.getRemoteMetadataVersion("testVersion")).thenReturn(null);

    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.getParamsFromMap(parameters),
        "The MetadataVersion could not be fetched from the remote server for the versionName: testVersion");
  }

  @Test
  void testShouldGetMetadataVersionForGivenVersionName() {
    parameters.put("versionName", new ArrayList<>());
    parameters.get("versionName").add("testVersion");
    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.ATOMIC);

    when(metadataVersionDelegate.getRemoteMetadataVersion("testVersion"))
        .thenReturn(metadataVersion);
    MetadataSyncParams metadataSyncParams = metadataSyncService.getParamsFromMap(parameters);

    assertEquals(metadataVersion, metadataSyncParams.getVersion());
  }

  @Test
  void testShouldThrowExceptionWhenSyncParamsIsNull() {
    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.doMetadataSync(null),
        "MetadataSyncParams cant be null");
  }

  @Test
  void testShouldThrowExceptionWhenVersionIsNulInSyncParams() {
    MetadataSyncParams syncParams = new MetadataSyncParams();
    syncParams.setVersion(null);

    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.doMetadataSync(syncParams),
        "MetadataVersion for the Sync cant be null. The ClassListMap could not be constructed.");
  }

  @Test
  void testShouldThrowExceptionWhenSnapshotReturnsNullForGivenVersion() {

    MetadataSyncParams syncParams = Mockito.mock(MetadataSyncParams.class);
    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.ATOMIC);

    when(syncParams.getVersion()).thenReturn(metadataVersion);
    when(metadataVersionDelegate.downloadMetadataVersionSnapshot(metadataVersion)).thenReturn(null);

    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.doMetadataSync(syncParams),
        "Metadata snapshot can't be null.");
  }

  @Test
  void testShouldThrowExceptionWhenDHISVersionsMismatch() {
    MetadataSyncParams syncParams = Mockito.mock(MetadataSyncParams.class);
    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.ATOMIC);
    String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

    when(syncParams.getVersion()).thenReturn(metadataVersion);
    when(metadataVersionDelegate.downloadMetadataVersionSnapshot(metadataVersion))
        .thenReturn(expectedMetadataSnapshot);
    when(metadataSyncDelegate.shouldStopSync(expectedMetadataSnapshot)).thenReturn(true);
    when(metadataVersionService.isMetadataPassingIntegrity(
            metadataVersion, expectedMetadataSnapshot))
        .thenReturn(true);

    assertThrows(
        DhisVersionMismatchException.class,
        () -> metadataSyncService.doMetadataSync(syncParams),
        "Metadata sync failed because your version of DHIS does not match the master version");
  }

  @Test
  void testShouldNotThrowExceptionWhenDHISVersionsMismatch() throws DhisVersionMismatchException {
    MetadataSyncParams syncParams = Mockito.mock(MetadataSyncParams.class);
    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.ATOMIC);
    String expectedMetadataSnapshot =
        "{\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}";

    when(syncParams.getVersion()).thenReturn(metadataVersion);
    when(metadataVersionDelegate.downloadMetadataVersionSnapshot(metadataVersion))
        .thenReturn(expectedMetadataSnapshot);
    when(metadataSyncDelegate.shouldStopSync(expectedMetadataSnapshot)).thenReturn(false);
    when(metadataVersionService.isMetadataPassingIntegrity(
            metadataVersion, expectedMetadataSnapshot))
        .thenReturn(true);

    metadataSyncService.doMetadataSync(syncParams);
  }

  @Test
  void testShouldThrowExceptionWhenSnapshotNotPassingIntegrity() {
    MetadataSyncParams syncParams = Mockito.mock(MetadataSyncParams.class);
    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.ATOMIC);
    String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

    when(syncParams.getVersion()).thenReturn(metadataVersion);
    when(metadataVersionDelegate.downloadMetadataVersionSnapshot(metadataVersion))
        .thenReturn(expectedMetadataSnapshot);
    when(metadataVersionService.isMetadataPassingIntegrity(
            metadataVersion, expectedMetadataSnapshot))
        .thenReturn(false);

    assertThrows(
        MetadataSyncServiceException.class,
        () -> metadataSyncService.doMetadataSync(syncParams),
        "Metadata snapshot is corrupted.");
  }

  @Test
  void testShouldStoreMetadataSnapshotInDataStoreAndImport() throws DhisVersionMismatchException {
    MetadataSyncParams syncParams = Mockito.mock(MetadataSyncParams.class);
    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.ATOMIC);
    MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
    metadataSyncSummary.setMetadataVersion(metadataVersion);
    String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

    when(syncParams.getVersion()).thenReturn(metadataVersion);
    when(metadataVersionService.getVersionData("testVersion")).thenReturn(null);
    when(metadataVersionDelegate.downloadMetadataVersionSnapshot(metadataVersion))
        .thenReturn(expectedMetadataSnapshot);
    when(metadataVersionService.isMetadataPassingIntegrity(
            metadataVersion, expectedMetadataSnapshot))
        .thenReturn(true);
    when(metadataSyncImportHandler.importMetadata(syncParams, expectedMetadataSnapshot))
        .thenReturn(metadataSyncSummary);

    MetadataSyncSummary actualSummary = metadataSyncService.doMetadataSync(syncParams);

    verify(metadataVersionService, times(1))
        .createMetadataVersionInDataStore(metadataVersion.getName(), expectedMetadataSnapshot);
    assertNull(actualSummary.getImportReport());
    assertNull(actualSummary.getImportSummary());
    assertEquals(metadataVersion, actualSummary.getMetadataVersion());
  }

  @Test
  void testShouldNotStoreMetadataSnapshotInDataStoreWhenAlreadyExistsInLocalStore()
      throws DhisVersionMismatchException {
    MetadataSyncParams syncParams = Mockito.mock(MetadataSyncParams.class);

    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.ATOMIC);

    MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
    metadataSyncSummary.setMetadataVersion(metadataVersion);

    String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

    when(syncParams.getVersion()).thenReturn(metadataVersion);
    when(metadataVersionService.getVersionData("testVersion")).thenReturn(expectedMetadataSnapshot);

    when(metadataSyncImportHandler.importMetadata(syncParams, expectedMetadataSnapshot))
        .thenReturn(metadataSyncSummary);

    MetadataSyncSummary actualSummary = metadataSyncService.doMetadataSync(syncParams);

    verify(metadataVersionService, never())
        .createMetadataVersionInDataStore(metadataVersion.getName(), expectedMetadataSnapshot);
    verify(metadataVersionDelegate, never()).downloadMetadataVersionSnapshot(metadataVersion);
    assertNull(actualSummary.getImportReport());
    assertNull(actualSummary.getImportSummary());
    assertEquals(metadataVersion, actualSummary.getMetadataVersion());
  }

  @Test
  void testShouldVerifyImportParamsAtomicTypeForTheGivenBestEffortVersion()
      throws DhisVersionMismatchException {
    MetadataSyncParams syncParams = new MetadataSyncParams();

    MetadataVersion metadataVersion = new MetadataVersion("testVersion", VersionType.BEST_EFFORT);
    MetadataImportParams metadataImportParams = new MetadataImportParams();

    syncParams.setVersion(metadataVersion);
    syncParams.setImportParams(metadataImportParams);

    MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();
    metadataSyncSummary.setMetadataVersion(metadataVersion);
    String expectedMetadataSnapshot = "{\"date\":\"2016-05-24T05:27:25.128+0000\"}";

    when(metadataVersionService.getVersionData("testVersion")).thenReturn(expectedMetadataSnapshot);

    metadataSyncService.doMetadataSync(syncParams);

    verify(metadataSyncImportHandler, times(1))
        .importMetadata(
            (argThat(
                metadataSyncParams ->
                    syncParams.getImportParams().getAtomicMode().equals(AtomicMode.NONE))),
            eq(expectedMetadataSnapshot));

    verify(metadataVersionService, never())
        .createMetadataVersionInDataStore(metadataVersion.getName(), expectedMetadataSnapshot);
    verify(metadataVersionDelegate, never()).downloadMetadataVersionSnapshot(metadataVersion);
  }
}
