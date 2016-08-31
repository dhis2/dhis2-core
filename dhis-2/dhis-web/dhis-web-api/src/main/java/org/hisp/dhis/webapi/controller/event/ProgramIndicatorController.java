package org.hisp.dhis.webapi.controller.event;

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

import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.common.Status;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.schema.descriptors.ProgramIndicatorSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = ProgramIndicatorSchemaDescriptor.API_ENDPOINT )
public class ProgramIndicatorController
    extends AbstractCrudController<ProgramIndicator>
{
    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private I18nManager i18nManager;

    @RequestMapping( value = "/expression/description", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE )
    public void getExpressionDescription( @RequestBody String expression, HttpServletResponse response )
        throws IOException
    {
        I18n i18n = i18nManager.getI18n();

        String result = programIndicatorService.expressionIsValid( expression );

        DescriptiveWebMessage message = new DescriptiveWebMessage();
        message.setStatus( ProgramIndicator.VALID.equals( result ) ? Status.OK : Status.ERROR );
        message.setMessage( i18n.getString( result ) );

        if ( message.isOk() )
        {
            message.setDescription( programIndicatorService.getExpressionDescription( expression ) );
        }

        webMessageService.sendJson( message, response );
    }

    @RequestMapping( value = "/filter/description", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE )
    public void validateFilter( @RequestBody String expression, HttpServletResponse response )
        throws IOException
    {
        I18n i18n = i18nManager.getI18n();

        String result = programIndicatorService.filterIsValid( expression );

        DescriptiveWebMessage message = new DescriptiveWebMessage();
        message.setStatus( ProgramIndicator.VALID.equals( result ) ? Status.OK : Status.ERROR );
        message.setMessage( i18n.getString( result ) );

        if ( message.isOk() )
        {
            message.setDescription( programIndicatorService.getExpressionDescription( expression ) );
        }

        webMessageService.sendJson( message, response );
    }
}
