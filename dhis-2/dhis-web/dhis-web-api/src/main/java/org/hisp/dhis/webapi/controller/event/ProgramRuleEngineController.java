package org.hisp.dhis.webapi.controller.event;

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
import org.hisp.dhis.program.*;
import org.hisp.dhis.programrule.engine.ProgramRuleEngineService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by zubair@dhis2.org on 24.10.17.
 */
@RestController
@RequestMapping( value = "/programRuleEngine" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ProgramRuleEngineController
{
    @Autowired
    private ProgramRuleEngineService programRuleEngineService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private RenderService renderService;

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_RULE_MANAGEMENT')" )
    @RequestMapping( value = "/enrollment/{programInstanceId}", method = RequestMethod.GET, produces = "application/json" )
    public void evaluateEnrollment( @PathVariable String programInstanceId, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        ProgramInstance programInstance = programInstanceService.getProgramInstance( programInstanceId );

        List<RuleEffect> ruleEffects = programRuleEngineService.evaluate( programInstance );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        renderService.toJson( response.getOutputStream(), ruleEffects );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PROGRAM_RULE_MANAGEMENT')" )
    @RequestMapping( value = "/event/{programStageInstanceId}", method = RequestMethod.GET, produces = "application/json" )
    public void evaluateEvent( @PathVariable String programStageInstanceId, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( programStageInstanceId );

        List<RuleEffect> ruleEffects = programRuleEngineService.evaluate( programStageInstance );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        renderService.toJson( response.getOutputStream(), ruleEffects );
    }
}
