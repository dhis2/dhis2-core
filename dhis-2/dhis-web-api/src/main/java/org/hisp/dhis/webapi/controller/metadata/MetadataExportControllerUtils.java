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
package org.hisp.dhis.webapi.controller.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utilities for metadata export controllers.
 *
 * @author Volker Schmidt
 */
public abstract class MetadataExportControllerUtils {
  /**
   * Returns the response entity for metadata download with dependencies.
   *
   * @param contextService the context service that is used to retrieve request parameters.
   * @param exportService the export service that is used to export metadata with dependencies.
   * @param identifiableObject the identifiable object that should be exported with dependencies.
   * @param download <code>true</code> if the data should be downloaded (as attachment), <code>false
   *     </code> otherwise.
   * @return the response with the metadata.
   */
  @Nonnull
  public static ResponseEntity<RootNode> getWithDependencies(
      @Nonnull ContextService contextService,
      @Nonnull MetadataExportService exportService,
      @Nonnull IdentifiableObject identifiableObject,
      boolean download) {
    final MetadataExportParams exportParams =
        exportService.getParamsFromMap(contextService.getParameterValuesMap());
    exportService.validate(exportParams);

    RootNode rootNode =
        exportService.getMetadataWithDependenciesAsNode(identifiableObject, exportParams);

    return createResponseEntity(rootNode, download);
  }

  /**
   * Creates the response entity for the root node. Optionally it can be specified that the data
   * should be downloaded.
   *
   * @param rootNode the root node for which the response entity should be created.
   * @param download <code>true</code> if the data should be downloaded (as attachment), <code>false
   *     </code> otherwise.
   * @return the response with the metadata.
   */
  @Nonnull
  public static ResponseEntity<RootNode> createResponseEntity(
      @Nonnull RootNode rootNode, boolean download) {
    HttpHeaders headers = new HttpHeaders();
    if (download) {
      // triggers that corresponding message converter adds also a file
      // name with a correct extension
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=metadata");
    }
    return new ResponseEntity<>(rootNode, headers, HttpStatus.OK);
  }

  public static ResponseEntity<JsonNode> createJsonNodeResponseEntity(
      @Nonnull JsonNode jsonNode, boolean download) {
    HttpHeaders headers = new HttpHeaders();

    if (download) {
      // triggers that corresponding message converter adds also a file
      // name with a correct extension
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=metadata");
    }

    return new ResponseEntity<>(jsonNode, headers, HttpStatus.OK);
  }

  private MetadataExportControllerUtils() {
    super();
  }
}
