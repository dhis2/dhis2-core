package org.hisp.dhis.webapi.controller.metadata.sync;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncService;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncImportException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.webmessage.WebMessageResponse;
import org.hisp.dhis.exception.RemoteServerUnavailableException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.MetadataImportConflictException;
import org.hisp.dhis.webapi.controller.exception.MetadataSyncException;
import org.hisp.dhis.webapi.controller.exception.OperationNotAllowedException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller for the automated sync of the metadata
 *
 * @author vanyas
 */

@RestController
@RequestMapping( "/metadata/sync" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class MetadataSyncController
    extends CrudControllerAdvice
{
    @Autowired
    private ContextService contextService;

    @Autowired
    private MetadataSyncService metadataSyncService;

    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_MANAGE')" )
    @GetMapping
    public ResponseEntity<? extends WebMessageResponse> metadataSync(HttpServletRequest request, HttpServletResponse response)
        throws MetadataSyncException, BadRequestException, MetadataImportConflictException, OperationNotAllowedException
    {
        MetadataSyncParams syncParams;
        MetadataSyncSummary metadataSyncSummary = null;

        synchronized ( metadataSyncService )
        {
            try
            {
                syncParams = metadataSyncService.getParamsFromMap( contextService.getParameterValuesMap() );
            }
            catch ( RemoteServerUnavailableException exception )
            {
                throw new MetadataSyncException( exception.getMessage(), exception );

            }
            catch ( MetadataSyncServiceException serviceException )
            {
                throw new BadRequestException( "Error in parsing inputParams " + serviceException.getMessage(), serviceException );
            }

            try
            {
                boolean isSyncRequired = metadataSyncService.isSyncRequired(syncParams);

                if( isSyncRequired )
                {
                    metadataSyncSummary = metadataSyncService.doMetadataSync( syncParams );
                    validateSyncSummaryResponse(metadataSyncSummary);
                }
                else
                {
                    throw new MetadataImportConflictException( "Version already exists in system and hence not starting the sync." );
                }
            }

            catch (MetadataSyncImportException importerException)
            {
                throw new MetadataSyncException( "Runtime exception occurred while doing import: " + importerException.getMessage() );
            }
            catch ( MetadataSyncServiceException serviceException )
            {
                throw new MetadataSyncException( "Exception occurred while doing metadata sync: " + serviceException.getMessage() );
            }
            catch ( DhisVersionMismatchException versionMismatchException )
            {
                throw new OperationNotAllowedException( "Exception occurred while doing metadata sync: " + versionMismatchException.getMessage() );
            }
        }

        return new ResponseEntity<MetadataSyncSummary>( metadataSyncSummary, HttpStatus.OK );
    }

    private void validateSyncSummaryResponse( MetadataSyncSummary metadataSyncSummary ) throws MetadataImportConflictException
    {
        ImportReport importReport = metadataSyncSummary.getImportReport();
        if(importReport.getStatus() != Status.OK){
            throw new MetadataImportConflictException( metadataSyncSummary );
        }
    }
}
