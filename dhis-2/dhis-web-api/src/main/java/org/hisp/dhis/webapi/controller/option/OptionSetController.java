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
package org.hisp.dhis.webapi.controller.option;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.descriptors.OptionSetSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags( "metadata" )
@Controller
@RequestMapping( value = OptionSetSchemaDescriptor.API_ENDPOINT )
@AllArgsConstructor
public class OptionSetController
    extends AbstractCrudController<OptionSet>
{
    private final OptionService optionService;

    @GetMapping( "/{uid}/metadata" )
    public ResponseEntity<MetadataExportParams> getOptionSetWithDependencies( @PathVariable( "uid" ) String pvUid,
        @RequestParam( required = false, defaultValue = "false" ) boolean download )
        throws WebMessageException
    {
        OptionSet optionSet = optionService.getOptionSet( pvUid );

        if ( optionSet == null )
        {
            throw new WebMessageException( notFound( "OptionSet not found for uid: " + pvUid ) );
        }

        MetadataExportParams exportParams = exportService.getParamsFromMap( contextService.getParameterValuesMap() );
        exportService.validate( exportParams );
        exportParams.setObjectExportWithDependencies( optionSet );

        return ResponseEntity.ok( exportParams );
    }

    @Override
    protected void preCreateEntity( OptionSet entity )
    {
        optionService.validateOptionSet( entity );
    }

    @Override
    protected void preUpdateEntity( OptionSet entity, OptionSet newEntity )
    {
        optionService.validateOptionSet( newEntity );
    }
}
