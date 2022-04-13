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
package org.hisp.dhis.webapi.controller.event;

import static org.springframework.http.MediaType.*;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.schema.descriptors.ProgramIndicatorSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = ProgramIndicatorSchemaDescriptor.API_ENDPOINT )
@RequiredArgsConstructor
public class ProgramIndicatorController
    extends AbstractCrudController<ProgramIndicator>
{
    private final ProgramIndicatorService programIndicatorService;

    private final I18nManager i18nManager;

    @PostMapping( value = "/expression/description", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage getExpressionDescription( @RequestBody Expression expression )
    {
        I18n i18n = i18nManager.getI18n();

        if ( expression == null || StringUtils.isEmpty( expression.getExpression() ) )
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }
        try
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setDescription( programIndicatorService.getExpressionDescription( expression.getExpression() ) )
                .setMessage( i18n.getString( ProgramIndicator.VALID ) );
        }
        catch ( IllegalStateException e )
        {
            return new DescriptiveWebMessage( Status.ERROR, HttpStatus.OK )
                .setDescription( e.getMessage() )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }
    }

    @PostMapping( value = "/expression/description", produces = APPLICATION_JSON_VALUE, consumes = TEXT_PLAIN_VALUE )
    @ResponseBody
    public WebMessage getExpressionDescription( @RequestBody String expression )
    {
        I18n i18n = i18nManager.getI18n();

        if ( StringUtils.isEmpty( expression ) )
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }

        try
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setDescription( programIndicatorService.getExpressionDescription( expression ) )
                .setMessage( i18n.getString( ProgramIndicator.VALID ) );
        }
        catch ( IllegalStateException e )
        {
            return new DescriptiveWebMessage( Status.ERROR, HttpStatus.OK )
                .setDescription( e.getMessage() )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }
    }

    @PostMapping( value = "/filter/description", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage validateFilter( @RequestBody Expression expression )
    {
        I18n i18n = i18nManager.getI18n();

        if ( expression == null || StringUtils.isEmpty( expression.getExpression() ) )
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }

        try
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setDescription( programIndicatorService.getFilterDescription( expression.getExpression() ) )
                .setMessage( i18n.getString( ProgramIndicator.VALID ) );
        }
        catch ( IllegalStateException e )
        {
            return new DescriptiveWebMessage( Status.ERROR, HttpStatus.OK )
                .setDescription( e.getMessage() )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }
    }

    @PostMapping( value = "/filter/description", produces = APPLICATION_JSON_VALUE, consumes = TEXT_PLAIN_VALUE )
    @ResponseBody
    public WebMessage validateFilter( @RequestBody String expression )
    {
        I18n i18n = i18nManager.getI18n();

        if ( StringUtils.isEmpty( expression ) )
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }

        try
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setDescription( programIndicatorService.getFilterDescription( expression ) )
                .setMessage( i18n.getString( ProgramIndicator.VALID ) );
        }
        catch ( IllegalStateException e )
        {
            return new DescriptiveWebMessage( Status.ERROR, HttpStatus.OK )
                .setDescription( e.getMessage() )
                .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
        }
    }
}
