package org.hisp.dhis.programrule.config;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.engine.*;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Enrico Colasante
 */
@Configuration( "ruleEngineConfig" )
public class ProgramRuleConfig
{
    @Autowired
    private ProgramRuleEntityMapperService programRuleEntityMapperService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ConstantService constantService;

    /**
     * This bean is used in the system when an event is intercepted by
     * {@link ProgramRuleEngineListener}. Only the notification rule actions are
     * executed.
     */
    @Bean( "notificationRuleEngine" )
    public ProgramRuleEngine oldRuleEngine( OldImplementableRuleService oldImplementableRuleService )
    {
        return new ProgramRuleEngine( programRuleEntityMapperService, programRuleVariableService,
            organisationUnitGroupService, currentUserService, constantService,
            oldImplementableRuleService );
    }

    /**
     * This bean is used when the new importer is called. All the relevant rule
     * actions are executed.
     */
    @Bean( "serviceTrackerRuleEngine" )
    public ProgramRuleEngine newRuleEngine( NewImplementableRuleService newImplementableRuleService )
    {
        return new ProgramRuleEngine( programRuleEntityMapperService, programRuleVariableService,
            organisationUnitGroupService, currentUserService, constantService,
            newImplementableRuleService );
    }
}
