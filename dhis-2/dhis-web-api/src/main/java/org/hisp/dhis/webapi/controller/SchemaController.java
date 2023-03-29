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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.errorReports;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.jackson.domain.JsonRoot;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.LinkService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags( "system" )
@RestController
@RequestMapping( "/schemas" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class SchemaController
{
    private final SchemaService schemaService;

    private final SchemaValidator schemaValidator;

    private final LinkService linkService;

    private final FieldFilterService fieldFilterService;

    @Qualifier( "jsonMapper" )
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<JsonRoot> getSchemas( @RequestParam( defaultValue = "*" ) List<String> fields )
    {
        List<Schema> schemas = schemaService.getSortedSchemas();
        linkService.generateSchemaLinks( schemas );

        FieldFilterParams<Schema> params = FieldFilterParams.of( schemas, fields );
        List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes( params );

        return ResponseEntity.ok( JsonRoot.of( "schemas", objectNodes ) );
    }

    @GetMapping( "/{type}" )
    public ResponseEntity<ObjectNode> getSchema( @PathVariable String type,
        @RequestParam( defaultValue = "*" ) List<String> fields )
        throws NotFoundException
    {
        Schema schema = getSchemaFromType( type );

        linkService.generateSchemaLinks( schema );

        FieldFilterParams<Schema> params = FieldFilterParams.of( schema, fields );
        List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes( params );

        return ResponseEntity.ok( objectNodes.get( 0 ) );
    }

    @RequestMapping( value = "/{type}", method = { RequestMethod.POST, RequestMethod.PUT }, consumes = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public WebMessage validateSchema( @PathVariable String type, HttpServletRequest request,
        HttpServletResponse response )
        throws IOException,
        NotFoundException
    {
        Schema schema = getSchemaFromType( type );
        Object object = objectMapper.readValue( request.getInputStream(), schema.getKlass() );
        List<ErrorReport> validationViolations = schemaValidator.validate( object );

        return errorReports( validationViolations );
    }

    @GetMapping( "/{type}/{property}" )
    public Property getSchemaProperty( @PathVariable String type, @PathVariable String property )
        throws NotFoundException
    {
        Schema schema = getSchemaFromType( type );
        if ( schema.hasProperty( property ) )
        {
            return schema.getProperty( property );
        }

        throw new NotFoundException(
            "Property " + property + " does not exist on type " + type + "." );
    }

    @Nonnull
    private Schema getSchemaFromType( String type )
        throws NotFoundException
    {
        Schema schema = schemaService.getSchemaBySingularName( type );

        if ( schema == null )
        {
            throw new NotFoundException( "Type " + type + " does not exist." );
        }

        return schema;
    }
}
