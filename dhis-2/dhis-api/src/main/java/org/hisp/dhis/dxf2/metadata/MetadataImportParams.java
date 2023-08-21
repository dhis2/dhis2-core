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
package org.hisp.dhis.dxf2.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.csv.CsvImportClass;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
@OpenApi.Shared
@JacksonXmlRootElement(localName = "metadataImportParams", namespace = DxfNamespaces.DXF_2_0)
public class MetadataImportParams implements JobParameters {

  /** UID of the User to use for import job (important for threaded imports). */
  @OpenApi.Property({UID.class, User.class})
  @JsonProperty
  private UID user;

  /**
   * How should the user property be handled, by default it is left as is. You can override this to
   * use current user, or a selected user instead (not yet supported).
   */
  @JsonProperty private UserOverrideMode userOverrideMode = UserOverrideMode.NONE;

  /** UID of the User to use for override, can be current or a selected user. */
  @OpenApi.Property({UID.class, User.class})
  @JsonProperty
  private UID overrideUser;

  /** Should import be imported or just validated. */
  @JsonProperty private ObjectBundleMode importMode = ObjectBundleMode.COMMIT;

  /** What identifiers to match on. */
  @JsonProperty private PreheatIdentifier identifier = PreheatIdentifier.UID;

  /** Preheat mode to use (default is REFERENCE and should not be changed). */
  @JsonProperty private PreheatMode preheatMode = PreheatMode.REFERENCE;

  /** Sets import strategy (create, update, etc). */
  @JsonProperty private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

  /** Should import be treated as a atomic import (all or nothing). */
  @JsonProperty private AtomicMode atomicMode = AtomicMode.ALL;

  /** Merge mode for object updates (default is REPLACE). */
  @JsonProperty private MergeMode mergeMode = MergeMode.REPLACE;

  /** Flush for every object or per type. */
  @JsonProperty private FlushMode flushMode = FlushMode.AUTO;

  /**
   * Decides how much to report back to the user (errors only, or a more full per object report).
   */
  @JsonProperty private ImportReportMode importReportMode = ImportReportMode.ERRORS;

  /** Should sharing be considered when importing objects. */
  @JsonProperty private boolean skipSharing;

  /** Should translation be considered when importing objects. */
  @JsonProperty private boolean skipTranslation;

  /** Skip validation of objects (not recommended). */
  @JsonProperty private boolean skipValidation;

  /** Is this import request from Metadata Sync service. */
  @JsonProperty private boolean metadataSyncImport;

  /** Should this run as an asynchronous import */
  @JsonProperty private boolean async;

  /** Name of file that was used for import (if available). */
  @OpenApi.Ignore @JsonProperty private String filename;

  /** Metadata Class name for importing using CSV */
  @OpenApi.Ignore @JsonProperty private CsvImportClass csvImportClass;

  /** Specify whether the first row is header in CSV import */
  @OpenApi.Ignore @JsonProperty private boolean firstRowIsHeader = true;
}
