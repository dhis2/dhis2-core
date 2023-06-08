/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.copy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramSectionService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CopyService
{
    private final ProgramService programService;

    private final ProgramSectionService programSectionService;

    private final ProgramStageService programStageService;

    private final ProgramStageSectionService programStageSectionService;

    private final ProgramStageDataElementService programStageDataElementService;

    private final ProgramIndicatorService programIndicatorService;

    private final ProgramRuleVariableService programRuleVariableService;

    private final EnrollmentService enrollmentService;

    @Transactional
    public String copyProgramFromUid( String uid, Map<String, String> copyOptions )
        throws NotFoundException
    {
        Program original = programService.getProgram( uid );
        if ( original != null )
        {
            return applyAllProgramCopySteps( original, copyOptions );
        }
        throw new NotFoundException( Program.class, uid );
    }

    private String applyAllProgramCopySteps( Program original, Map<String, String> copyOptions )
    {
        //shallow copy Program & save
        Program copy = Program.shallowCopy( original, copyOptions );
        programService.addProgram( copy );

        copyStages( original, copy );
        copyAndSaveSections( original, copy );
        copyAttributes( original, copy );
        copyAndSaveIndicators( original, copy );
        copyAndSaveRuleVariables( original, copy );

        programService.addProgram( copy );

        return copy.getUid();
    }

    private void copyAndSaveRuleVariables( Program original, Program programCopy )
    {
        Set<ProgramRuleVariable> programRuleVariables = Program.copyProgramRuleVariables( programCopy,
            original.getProgramRuleVariables() );
        programRuleVariables.forEach( programRuleVariableService::addProgramRuleVariable );
        programCopy.setProgramRuleVariables( programRuleVariables );
    }

    private void copyAndSaveIndicators( Program original, Program programCopy )
    {
        Set<ProgramIndicator> programIndicators = Program.copyProgramIndicators( programCopy,
            original.getProgramIndicators() );
        programIndicators.forEach( programIndicatorService::addProgramIndicator );
        programCopy.setProgramIndicators( programIndicators );
    }

    private void copyAttributes( Program original, Program programCopy )
    {
        programCopy.setProgramAttributes( Program.copyProgramAttributes( programCopy,
            original.getProgramAttributes() ) );
    }

    private void copyAndSaveSections( Program original, Program programCopy )
    {
        Set<ProgramSection> copySections = Program.copyProgramSections( programCopy, original.getProgramSections() );
        copySections.forEach( programSectionService::addProgramSection );
        programCopy.setProgramSections( copySections );
    }

    private void copyStages( Program original, Program programCopy )
    {
        HashSet<ProgramStage> copyStages = new HashSet<>();
        for ( ProgramStage stageOriginal : original.getProgramStages() )
        {
            //shallow copy PS & save
            ProgramStage stageCopy = ProgramStage.shallowCopy( stageOriginal, programCopy );
            programStageService.saveProgramStage( stageCopy );

            //deep copy PS & save
            ProgramStage stageCopyDeep = ProgramStage.deepCopy( stageOriginal, stageCopy );
            stageCopyDeep.getProgramStageDataElements()
                .forEach( programStageDataElementService::addProgramStageDataElement );

            stageCopyDeep.getProgramStageSections()
                .forEach( programStageSectionService::saveProgramStageSection );

            programStageService.saveProgramStage( stageCopyDeep );
            copyStages.add( stageCopyDeep );
        }
        programCopy.setProgramStages( copyStages );
    }
}
