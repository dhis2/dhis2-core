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
package org.hisp.dhis.programrule.action.validation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */

@Component
public class ProgramRuleActionValidationContextLoader
{
    @Nonnull
    private final ProgramRuleActionValidationService validationService;

    private final IdentifiableObjectManager objectManager;

    public ProgramRuleActionValidationContextLoader(
        @Nonnull ProgramRuleActionValidationService actionValidationService, IdentifiableObjectManager manager )
    {
        this.validationService = actionValidationService;
        this.objectManager = manager;
    }

    @Transactional( readOnly = true )
    public ProgramRuleActionValidationContext load( Preheat preheat, PreheatIdentifier preheatIdentifier,
        ProgramRuleAction ruleAction )
    {
        ProgramRule rule = preheat.get( preheatIdentifier, ProgramRule.class,
            ruleAction.getProgramRule() );

        Program program = preheat.get( preheatIdentifier, Program.class, rule.getProgram() );

        if ( program == null )
        {
            program = objectManager.get( Program.class, rule.getProgram().getUid() );
            preheat.put( preheatIdentifier, program );
        }

        List<ProgramStage> stages = preheat.getAll( preheatIdentifier, new ArrayList<>( program.getProgramStages() ) );

        return ProgramRuleActionValidationContext.builder()
            .programRule( rule )
            .program( program )
            .programStages( stages )
            .dataElement( ruleAction.hasDataElement()
                ? preheat.get( preheatIdentifier, DataElement.class, ruleAction.getDataElement() )
                : null )
            .trackedEntityAttribute(
                ruleAction.hasTrackedEntityAttribute() ? preheat.get( preheatIdentifier, TrackedEntityAttribute.class,
                    ruleAction.getAttribute() ) : null )
            .notificationTemplate(
                ruleAction.hasNotification() ? preheat.get( preheatIdentifier, ProgramNotificationTemplate.class,
                    ruleAction.getTemplateUid() ) : null )
            .programStageSection(
                ruleAction.hasProgramStageSection() ? preheat.get( preheatIdentifier, ProgramStageSection.class,
                    ruleAction.getProgramStageSection() ) : null )
            .programStage( ruleAction.hasProgramStage() ? preheat.get( preheatIdentifier, ProgramStage.class,
                ruleAction.getProgramStage() ) : null )
            .option( ruleAction.hasOption() ? preheat.get( preheatIdentifier, Option.class,
                ruleAction.getOption() ) : null )
            .optionGroup( ruleAction.hasOptionGroup() ? preheat.get( preheatIdentifier, OptionGroup.class,
                ruleAction.getOptionGroup() ) : null )
            .programRuleActionValidationService( validationService )
            .build();
    }
}
