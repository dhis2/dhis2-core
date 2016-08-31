package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageStatus;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = "/expressions" )
public class ExpressionController
{
    @Autowired
    protected WebMessageService webMessageService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private I18nManager i18nManager;

    @RequestMapping( value = "/description", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE )
    public void getExpressionDescription( @RequestParam String expression, HttpServletResponse response )
        throws IOException
    {
        I18n i18n = i18nManager.getI18n();

        String result = expressionService.expressionIsValid( expression );
        
        DescriptiveWebMessage message = new DescriptiveWebMessage();
        message.setStatus( ExpressionService.VALID.equals( result ) ? WebMessageStatus.OK : WebMessageStatus.ERROR );
        message.setMessage( i18n.getString( result ) );
        
        if ( result.equals( ExpressionService.VALID ) )
        {
            message.setDescription( expressionService.getExpressionDescription( expression ) );
        }
        
        webMessageService.sendJson( message, response );
    }
}
