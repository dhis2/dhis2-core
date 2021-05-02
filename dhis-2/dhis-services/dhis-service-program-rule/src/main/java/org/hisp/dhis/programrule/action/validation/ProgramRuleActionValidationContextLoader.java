package org.hisp.dhis.programrule.action.validation;

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
    @Transactional( readOnly = true )
    public ProgramRuleActionValidationContext load(Preheat preheat, PreheatIdentifier preheatIdentifier, ProgramRuleAction ruleAction,
                                                   ProgramRuleActionValidationService validationService )
    {
        ProgramRule rule = preheat.get( preheatIdentifier, ProgramRule.class,
                ruleAction.getProgramRule() );

        return ProgramRuleActionValidationContext.builder()
                .programRule( rule )
                .program( preheat.get( preheatIdentifier, Program.class, rule.getProgram() ) )
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
