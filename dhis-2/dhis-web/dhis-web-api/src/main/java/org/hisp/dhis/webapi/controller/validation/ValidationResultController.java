package org.hisp.dhis.webapi.controller.validation;
/*
 * Copyright (c) 2004-2016, University of Oslo
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
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.descriptors.ValidationResultSchemaDescriptor;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( value = ValidationResultSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.ALL, DhisApiVersion.DEFAULT } )
public class ValidationResultController
    extends AbstractCrudController<ValidationResult>
{

    @Override
    public void replaceTranslations(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    @Override
    public void partialUpdateObject(
        @PathVariable( "uid" ) String pvUid, @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void updateObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void postJsonObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void postXmlObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void putJsonObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void putXmlObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void deleteObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public
    @ResponseBody
    RootNode getCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        @RequestParam Map<String, String> parameters,
        TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void addCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void addCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void replaceCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void replaceCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void addCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void deleteCollectionItemsJson(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void deleteCollectionItemsXml(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

    public void deleteCollectionItem(
        @PathVariable( "uid" ) String pvUid,
        @PathVariable( "property" ) String pvProperty,
        @PathVariable( "itemId" ) String pvItemId,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "Method not supported for this endpoint" );
    }

}
