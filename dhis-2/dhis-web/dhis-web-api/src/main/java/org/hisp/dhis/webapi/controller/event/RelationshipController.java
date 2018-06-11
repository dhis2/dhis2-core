package org.hisp.dhis.webapi.controller.event;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.descriptors.RelationshipSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@RestController( value = RelationshipSchemaDescriptor.API_ENDPOINT )
@ApiVersion( DhisApiVersion.ALL )
public class RelationshipController
{

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private WebMessageService webMessageService;

    @PostMapping
    public void postTrackedEntityInstanceJson(
        @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {

        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = relationshipService.addRelationshipsJson( inputStream, importOptions );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        importSummaries.getImportSummaries().stream()
            .filter(
                importSummary -> !importOptions.isDryRun() &&
                    !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                    !importOptions.getImportStrategy().isDelete() &&
                    (!importOptions.getImportStrategy().isSync() || importSummary.getImportCount().getDeleted() == 0) )
            .forEach( importSummary -> importSummary.setHref(
                ContextUtils.getRootPath( request ) + RelationshipSchemaDescriptor.API_ENDPOINT + "/" + importSummary.getReference() ) );

        if ( importSummaries.getImportSummaries().size() == 1 )
        {
            ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );
            importSummary.setImportOptions( importOptions );

            if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
            {
                response.setHeader( "Location", getResourcePath( request, importSummary ) );
            }
        }

        response.setStatus( HttpServletResponse.SC_CREATED );
        webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
    }


    private String getResourcePath( HttpServletRequest request, ImportSummary importSummary )
    {
        return ContextUtils.getContextPath( request ) + "/api/" + "trackedEntityInstances" + "/" + importSummary.getReference();
    }
}