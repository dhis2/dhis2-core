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
package org.hisp.dhis.webapi.controller.validation;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.descriptors.ValidationResultSchemaDescriptor;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultService;
import org.hisp.dhis.validation.ValidationResultsDeletionRequest;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

/**
 * @author Stian Sandvold
 */
@OpenApi.Tags( "data" )
@RestController
@RequestMapping( value = ValidationResultSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.ALL, DhisApiVersion.DEFAULT } )
public class ValidationResultController
{
    private final FieldFilterService fieldFilterService;

    private final ValidationResultService validationResultService;

    private final ContextService contextService;

    public ValidationResultController( FieldFilterService fieldFilterService,
        ValidationResultService validationResultService,
        ContextService contextService )
    {
        this.fieldFilterService = fieldFilterService;
        this.validationResultService = validationResultService;
        this.contextService = contextService;
    }

    @GetMapping
    public @ResponseBody RootNode getObjectList( ValidationResultQuery query, HttpServletResponse response )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<ValidationResult> validationResults = validationResultService.getValidationResults( query );

        RootNode rootNode = NodeUtils.createMetadata();

        if ( !query.isSkipPaging() )
        {
            query.setTotal( validationResultService.countValidationResults( query ) );
            rootNode.addChild( NodeUtils.createPager( query.getPager() ) );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( ValidationResult.class,
            new FieldFilterParams( validationResults, fields ) ) );

        setNoStore( response );
        return rootNode;
    }

    @GetMapping( value = "/{id}" )
    public @ResponseBody ValidationResult getObject( @PathVariable int id )
        throws WebMessageException
    {
        ValidationResult result = validationResultService.getById( id );
        checkFound( id, result );
        return result;
    }

    @PreAuthorize( "hasRole('F_PERFORM_MAINTENANCE')" )
    @DeleteMapping( value = "/{id}" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void delete( @PathVariable int id )
        throws WebMessageException
    {
        ValidationResult result = validationResultService.getById( id );
        checkFound( id, result );
        validationResultService.deleteValidationResult( result );
    }

    @PreAuthorize( "hasRole('F_PERFORM_MAINTENANCE')" )
    @DeleteMapping
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void deleteValidationResults( ValidationResultsDeletionRequest request )
    {
        validationResultService.deleteValidationResults( request );
    }

    private void checkFound( int id, ValidationResult result )
        throws WebMessageException
    {
        if ( result == null )
        {
            throw new WebMessageException(
                notFound( "Validation result with id " + id + " was not found" ) );
        }
    }
}
