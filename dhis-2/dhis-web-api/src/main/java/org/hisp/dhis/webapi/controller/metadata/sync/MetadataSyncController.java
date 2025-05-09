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
package org.hisp.dhis.webapi.controller.metadata.sync;

import static org.hisp.dhis.security.Authorities.F_METADATA_MANAGE;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncService;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncImportException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.sync.exception.RemoteServerUnavailableException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.controller.exception.MetadataImportConflictException;
import org.hisp.dhis.webapi.controller.exception.MetadataSyncException;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the automated sync of the metadata
 *
 * @author vanyas
 */
@OpenApi.Document(
    entity = Metadata.class,
    classifiers = {"team:platform", "purpose:metadata"})
@RestController
@RequestMapping("/api/metadata/sync")
public class MetadataSyncController {
  @Autowired private ContextService contextService;

  @Autowired private MetadataSyncService metadataSyncService;

  @RequiresAuthority(anyOf = F_METADATA_MANAGE)
  @GetMapping
  public ResponseEntity<MetadataSyncSummary> metadataSync()
      throws MetadataSyncException,
          BadRequestException,
          MetadataImportConflictException,
          ForbiddenException {
    MetadataSyncParams syncParams;
    MetadataSyncSummary metadataSyncSummary;

    synchronized (metadataSyncService) {
      try {
        syncParams = metadataSyncService.getParamsFromMap(contextService.getParameterValuesMap());
      } catch (RemoteServerUnavailableException exception) {
        throw new MetadataSyncException(exception.getMessage(), exception);

      } catch (MetadataSyncServiceException serviceException) {
        throw new BadRequestException(
            "Error in parsing inputParams " + serviceException.getMessage());
      }

      try {
        boolean isSyncRequired = metadataSyncService.isSyncRequired(syncParams);

        if (isSyncRequired) {
          metadataSyncSummary = metadataSyncService.doMetadataSync(syncParams);
          validateSyncSummaryResponse(metadataSyncSummary);
        } else {
          throw new MetadataImportConflictException(
              "Version already exists in system and hence not starting the sync.");
        }
      } catch (MetadataSyncImportException importerException) {
        throw new MetadataSyncException(
            "Runtime exception occurred while doing import: " + importerException.getMessage());
      } catch (MetadataSyncServiceException serviceException) {
        throw new MetadataSyncException(
            "Exception occurred while doing metadata sync: " + serviceException.getMessage());
      } catch (DhisVersionMismatchException versionMismatchException) {
        throw new ForbiddenException(
            "Exception occurred while doing metadata sync: "
                + versionMismatchException.getMessage());
      }
    }

    return new ResponseEntity<>(metadataSyncSummary, HttpStatus.OK);
  }

  private void validateSyncSummaryResponse(MetadataSyncSummary metadataSyncSummary)
      throws MetadataImportConflictException {
    ImportReport importReport = metadataSyncSummary.getImportReport();
    if (importReport.getStatus() != Status.OK) {
      throw new MetadataImportConflictException(metadataSyncSummary);
    }
  }
}
