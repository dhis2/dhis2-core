package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zubair@dhis2.org on 04.01.18.
 */
public class RuleActionSendMessageImplementer implements RuleActionImplementer
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramNotificationTemplateStore programNotificationTemplateStore;

    @Autowired
    private ProgramNotificationService programNotificationService;

    @Override
    public boolean accept( RuleAction ruleAction )
    {
        return ruleAction instanceof RuleActionSendMessage;
    }

    @Override
    public void implement( RuleAction ruleAction, ProgramInstance programInstance )
    {
        programNotificationService.sendProgramRuleTriggeredNotifications( getNotificationTemplate( ruleAction ), programInstance );
    }

    @Override
    public void implement( RuleAction ruleAction, ProgramStageInstance programStageInstance )
    {
        programNotificationService.sendProgramRuleTriggeredNotifications( getNotificationTemplate( ruleAction ), programStageInstance );
    }

    private ProgramNotificationTemplate getNotificationTemplate( RuleAction action )
    {
        RuleActionSendMessage sendMessage = (RuleActionSendMessage) action;

        return programNotificationTemplateStore.getByUid( sendMessage.notification() );
    }
}
