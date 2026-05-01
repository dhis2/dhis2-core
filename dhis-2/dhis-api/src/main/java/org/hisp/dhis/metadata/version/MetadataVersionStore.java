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
package org.hisp.dhis.metadata.version;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.GenericStore;

/**
 * Define MetadataStore to interact with the database.
 *
 * @author aamerm
 */
public interface MetadataVersionStore extends GenericStore<MetadataVersion> {
  String ID = MetadataVersionStore.class.getName();

  /**
   * @param id Key to lookup.
   * @return MetadataVersion Value that matched key, or null if there was no match.
   */
  MetadataVersion getVersionByKey(long id);

  /**
   * Get the version by name.
   *
   * @param versionName
   * @return MetadataVersion object matched by the name
   */
  MetadataVersion getVersionByName(String versionName);

  /**
   * Gets the current version in the system.
   *
   * @return MetadataVersion object which is the latest in the system
   */
  MetadataVersion getCurrentVersion();

  /**
   * Gets MetadataVersion 's based on start created and end created dates
   *
   * @param startDate
   * @param endDate
   * @return List of MetadataVersion objects lying in that range of dates
   */
  List<MetadataVersion> getAllVersionsInBetween(Date startDate, Date endDate);

  /**
   * @return Initial/First MetadataVersion of the system
   */
  MetadataVersion getInitialVersion();

  /**
   * Streams the metadata snapshot for the given version name directly from the underlying datastore
   * row to the output stream. Avoids materialising the snapshot as a Java String, which is
   * memory-prohibitive for large snapshots and would also trip Jackson's default 20MB string-token
   * limit during deserialisation of the wrapper object.
   *
   * @param versionName the version name
   * @param out the output stream to write the snapshot to
   * @return true if the snapshot was found and written, false if no snapshot exists
   * @throws IOException if writing to the output stream fails
   */
  boolean streamMetadataVersionData(String versionName, OutputStream out) throws IOException;

  /**
   * Returns whether a metadata snapshot exists for the given version name. Intended as a cheap
   * pre-flight check before opening a response output stream — once {@link
   * #streamMetadataVersionData} starts writing, response headers are committed and an error status
   * can no longer be returned to the client (especially relevant for the gzipped variant where
   * {@code GZIPOutputStream} writes its magic header on construction).
   *
   * @param versionName the version name
   * @return true if a snapshot row exists in the metadata datastore namespace
   */
  boolean metadataVersionSnapshotExists(String versionName);
}
