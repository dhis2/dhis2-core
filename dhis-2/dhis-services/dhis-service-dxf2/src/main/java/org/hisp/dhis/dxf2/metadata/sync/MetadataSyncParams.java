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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.metadata.version.MetadataVersion;

/**
 * Defines the structure of metadata sync params
 *
 * @author vanyas
 */
public class MetadataSyncParams {
  private MetadataImportParams importParams;

  private MetadataVersion version;

  private Map<String, List<String>> parameters = new HashMap<String, List<String>>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public MetadataSyncParams() {}

  public MetadataSyncParams(MetadataImportParams importParams, MetadataVersion version) {
    this.importParams = importParams;
    this.version = version;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public MetadataImportParams getImportParams() {
    return importParams;
  }

  public void setImportParams(MetadataImportParams importParams) {
    this.importParams = importParams;
  }

  public MetadataVersion getVersion() {
    return version;
  }

  public void setVersion(MetadataVersion version) {
    this.version = version;
  }

  public Map<String, List<String>> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, List<String>> parameters) {
    this.parameters = parameters;
  }
}
