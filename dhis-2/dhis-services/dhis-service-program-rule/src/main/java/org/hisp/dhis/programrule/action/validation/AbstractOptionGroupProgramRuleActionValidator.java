package org.hisp.dhis.programrule.action.validation;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionValidationResult;

/**
 * @author Zubair Asghar
 */

@Slf4j
public abstract class AbstractOptionGroupProgramRuleActionValidator extends AbstractProgramRuleActionValidator
{
    @Override
    public ProgramRuleActionValidationResult validate(ProgramRuleAction programRuleAction, ProgramRuleActionValidationService validationService)
    {
        if ( !programRuleAction.hasOptionGroup() )
        {
            log.debug( String.format( "OptionGroup cannot be null for program rule: %s ",
                    programRuleAction.getProgramRule().getUid() ) );

            return ProgramRuleActionValidationResult.builder()
                    .valid( false )
                    .errorReport( new ErrorReport( Option.class, ErrorCode.E4040,
                            programRuleAction.getProgramRule().getUid() ) )
                    .build();
        }

        OptionGroup optionGroup = programRuleAction.getOptionGroup();

        OptionService optionService = validationService.getOptionService();

        if ( optionService.getOptionGroup( optionGroup.getUid() ) == null )
        {
            log.debug( String.format( "OptionGroup: %s associated with program rule: %s does not exist",
                    optionGroup.getUid(),
                    programRuleAction.getProgramRule().getUid() ) );

            return ProgramRuleActionValidationResult.builder()
                    .valid( false )
                    .errorReport( new ErrorReport( Option.class, ErrorCode.E4041, optionGroup.getUid(),
                            programRuleAction.getProgramRule().getUid() ) )
                    .build();
        }

        return ProgramRuleActionValidationResult.builder().valid( true ).build();
    }
}
