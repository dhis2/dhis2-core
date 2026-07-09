/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.sync.exception.RemoteServerUnavailableException;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.springframework.stereotype.Service;

/**
 * Performs the meta data sync related tasks in service layer.
 *
 * @author vanyas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMetadataSyncService implements MetadataSyncService {
  private final MetadataVersionDelegate metadataVersionDelegate;

  private final MetadataVersionService metadataVersionService;

  private final MetadataSyncDelegate metadataSyncDelegate;

  private final MetadataSyncImportHandler metadataSyncImportHandler;

  @Override
  public MetadataSyncParams getParamsFromMap(Map<String, List<String>> parameters) {
    List<String> versionName = getVersionsFromParams(parameters);
    MetadataImportParams importParams = new MetadataImportParams();
    importParams.setMetadataSyncImport(true);
    MetadataSyncParams syncParams = new MetadataSyncParams();
    syncParams.setImportParams(importParams);
    String versionNameStr = versionName.get(0);

    if (StringUtils.isNotEmpty(versionNameStr)) {
      MetadataVersion version;

      try {
        version = metadataVersionDelegate.getRemoteMetadataVersion(versionNameStr);
      } catch (MetadataVersionServiceException e) {
        throw new MetadataSyncServiceException(e.getMessage(), e);
      }

      if (version == null) {
        throw new MetadataSyncServiceException(
            "The MetadataVersion could not be fetched from the remote server for the versionName: "
                + versionNameStr);
      }

      syncParams.setVersion(version);
    }

    syncParams.setParameters(parameters);

    return syncParams;
  }

  @Override
  public synchronized MetadataSyncSummary doMetadataSync(MetadataSyncParams syncParams)
      throws MetadataSyncServiceException, DhisVersionMismatchException {
    MetadataVersion version = getMetadataVersion(syncParams);

    setMetadataImportMode(syncParams, version);

    // Obtain the snapshot as a byte[] buffer. Coming from the local datastore it is streamed
    // (without MetadataWrapper deserialisation); coming from remote it is downloaded and
    // integrity-checked. Holding it once as UTF-8 bytes is substantially cheaper than keeping
    // a Java String alive for the whole import, which the previous String-based flow required.
    byte[] snapshotBytes = getOrFetchSnapshotBytes(version);

    if (metadataSyncDelegate.shouldStopSync(new ByteArrayInputStream(snapshotBytes))) {
      throw new DhisVersionMismatchException(
          "Metadata sync failed because your version of DHIS does not match the master version");
    }

    MetadataSyncSummary metadataSyncSummary =
        metadataSyncImportHandler.importMetadata(
            syncParams, new ByteArrayInputStream(snapshotBytes));

    log.info("Metadata Sync Summary: " + metadataSyncSummary);

    return metadataSyncSummary;
  }

  @Override
  public boolean isSyncRequired(MetadataSyncParams syncParams) {
    MetadataVersion version = getMetadataVersion(syncParams);
    return (metadataVersionService.getVersionByName(version.getName()) == null);
  }

  /**
   * Returns the metadata snapshot bytes for the given version. If the snapshot is already stored
   * locally it is streamed directly from the datastore; otherwise it is downloaded from the remote
   * server, integrity-checked, and saved locally before returning.
   */
  private byte[] getOrFetchSnapshotBytes(MetadataVersion version) {
    byte[] local = readLocalSnapshotBytes(version);
    if (local != null) {
      log.info("Rendering the MetadataVersion from local DataStore");
      return local;
    }

    String remoteSnapshot = getMetadataVersionSnapshotFromRemote(version);

    if (!metadataVersionService.isMetadataPassingIntegrity(version, remoteSnapshot)) {
      throw new MetadataSyncServiceException(
          "Metadata snapshot is corrupted. Not saving it locally");
    }

    metadataVersionService.createMetadataVersionInDataStore(version.getName(), remoteSnapshot);
    log.info(
        "Downloaded the metadata snapshot from remote and saved in Data Store for the version: "
            + version);

    return remoteSnapshot.getBytes(StandardCharsets.UTF_8);
  }

  /** Returns the locally stored snapshot bytes, or {@code null} if none are stored. */
  @CheckForNull
  private byte[] readLocalSnapshotBytes(MetadataVersion version) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      if (!metadataVersionService.streamVersionData(version.getName(), buffer)
          || buffer.size() == 0) {
        return null;
      }
    } catch (IOException e) {
      throw new MetadataSyncServiceException(
          "Exception occurred while reading local metadata snapshot for version "
              + version.getName(),
          e);
    }
    return buffer.toByteArray();
  }

  private String getMetadataVersionSnapshotFromRemote(MetadataVersion version) {
    String metadataVersionSnapshot;

    try {
      metadataVersionSnapshot = metadataVersionDelegate.downloadMetadataVersionSnapshot(version);
    } catch (MetadataVersionServiceException | RemoteServerUnavailableException e) {
      throw new MetadataSyncServiceException(e.getMessage(), e);
    }

    if (metadataVersionSnapshot == null) {
      throw new MetadataSyncServiceException("Metadata snapshot can't be null.");
    }

    return metadataVersionSnapshot;
  }

  private void setMetadataImportMode(MetadataSyncParams syncParams, MetadataVersion version) {
    if (VersionType.BEST_EFFORT.equals(version.getType())) {
      syncParams.getImportParams().setAtomicMode(AtomicMode.NONE);
    }
  }

  // ----------------------------------------------------------------------------------------
  // Private Methods
  // ----------------------------------------------------------------------------------------

  private List<String> getVersionsFromParams(Map<String, List<String>> parameters) {
    if (parameters == null) {
      throw new MetadataSyncServiceException("Missing required parameter: 'versionName'");
    }

    List<String> versionName = parameters.get("versionName");

    if (versionName == null || versionName.size() == 0) {
      throw new MetadataSyncServiceException("Missing required parameter: 'versionName'");
    }

    return versionName;
  }

  private MetadataVersion getMetadataVersion(MetadataSyncParams syncParams) {
    if (syncParams == null) {
      throw new MetadataSyncServiceException("MetadataSyncParams cant be null");
    }

    MetadataVersion version = syncParams.getVersion();

    if (version == null) {
      throw new MetadataSyncServiceException(
          "MetadataVersion for the Sync cant be null. The ClassListMap could not be constructed.");
    }

    return version;
  }
}
