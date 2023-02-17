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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.engine.ProgramRuleEngineService;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.hisp.dhis.schema.descriptors.ProgramRuleSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author markusbekken
 */
@OpenApi.Tags( "tracker" )
@Controller
@AllArgsConstructor
@RequestMapping( value = ProgramRuleSchemaDescriptor.API_ENDPOINT )
public class ProgramRuleController
    extends AbstractCrudController<ProgramRule>
{
    private final I18nManager i18nManager;

    private final ProgramRuleEngineService programRuleEngineService;

    @PostMapping( value = "/condition/description", produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage validateCondition( @RequestBody String condition, @RequestParam String programId )
    {
        I18n i18n = i18nManager.getI18n();

        RuleValidationResult result = programRuleEngineService.getDescription( condition, programId );

        if ( result.isValid() )
        {
            return new DescriptiveWebMessage( Status.OK, HttpStatus.OK )
                .setDescription( result.getDescription() )
                .setMessage( i18n.getString( ProgramIndicator.VALID ) );
        }
        String description = null;
        if ( result.getErrorMessage() != null )
        {
            description = result.getErrorMessage();
        }
        else if ( result.getException() != null )
        {
            description = result.getException().getMessage();
        }
        return new DescriptiveWebMessage( Status.ERROR, HttpStatus.OK )
            .setDescription( description )
            .setMessage( i18n.getString( ProgramIndicator.EXPRESSION_NOT_VALID ) );
    }
}
