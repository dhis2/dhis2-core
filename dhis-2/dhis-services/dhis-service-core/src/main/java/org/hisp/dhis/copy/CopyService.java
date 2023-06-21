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

import static org.hisp.dhis.common.BaseIdentifiableObject.copyList;
import static org.hisp.dhis.common.BaseIdentifiableObject.copySet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
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
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Service that allows copying of a {@link Program} and other {@link Program}
 * properties. There are currently 4 public methods exposed that allow for the
 * copying of those {@link Program} specific objects. This enables several parts
 * of a {@link Program} to be copied in isolation in future if required. The
 * objects that can be copied individually are:
 * <ul>
 * <li>{@link Program}</li>
 * <li>{@link ProgramStage}</li>
 * <li>{@link ProgramIndicator}</li>
 * <li>{@link ProgramRuleVariable}</li>
 * </ul>
 */
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

    private final AclService aclService;

    private final CurrentUserService currentUserService;

    /**
     * Method to copy a {@link Program} from a UID
     *
     * @param uid {@link Program} identifier
     * @param copyOptions Map containing options to be used in the copying
     *        process e.g. 'prefix' key with value to be used on the copied
     *        {@link Program} name property.
     * @return {@link Program} copy
     * @throws NotFoundException if the {@link Program} is not found.
     */
    @Transactional
    public Program copyProgram( String uid, Map<String, String> copyOptions )
        throws NotFoundException,
        ForbiddenException
    {
        Program original = programService.getProgram( uid );
        if ( original != null )
        {
            if ( aclService.canWrite( currentUserService.getCurrentUser(), original ) )
            {
                return applyAllProgramCopySteps( original, copyOptions );
            }
            throw new ForbiddenException( "You don't have write permissions for Program " + uid );
        }
        throw new NotFoundException( Program.class, uid );
    }

    /**
     * Method to copy a ProgramStage and save to the DB.
     *
     * @param original The original {@link ProgramStage} to base the copy from.
     * @param program The {@link Program} to use as the parent of the
     *        {@link ProgramStage} copy
     * @return {@link ProgramStage} copy
     */
    public ProgramStage copyProgramStage( @Nonnull ProgramStage original, @Nonnull Program program )
    {
        //shallow copy PS & save
        ProgramStage copy = ProgramStage.shallowCopy( original, program );
        programStageService.saveProgramStage( copy );

        //deep copy PS & save
        ProgramStage stageCopyDeep = ProgramStage.deepCopy( original, copy );
        stageCopyDeep.getProgramStageDataElements()
            .forEach( programStageDataElementService::addProgramStageDataElement );

        stageCopyDeep.getProgramStageSections()
            .forEach( programStageSectionService::saveProgramStageSection );

        programStageService.saveProgramStage( stageCopyDeep );
        return stageCopyDeep;
    }

    public ProgramIndicator copyProgramIndicator( @Nonnull ProgramIndicator original, @Nonnull Program program,
        Map<String, String> copyOptions )
    {
        ProgramIndicator copy = ProgramIndicator.copyOf( original, program, copyOptions );
        programIndicatorService.addProgramIndicator( copy );
        return copy;
    }

    public ProgramRuleVariable copyProgramRuleVariable( @Nonnull ProgramRuleVariable original,
        @Nonnull Program program, @Nonnull ProgramStage programStage )
    {
        ProgramRuleVariable copy = ProgramRuleVariable.copyOf( original, program, programStage );
        programRuleVariableService.addProgramRuleVariable( copy );
        return copy;
    }

    /**
     * Method with all the necessary steps to copy a {@link Program}. A certain
     * sequence is required when creating a deep copy of a {@link Program}.
     * <ol>
     * <li>Shallow copy of {@link Program} and save to DB, so we can use the
     * reference in the children. Hibernate requires that the object is saved
     * before using elsewhere</li>
     * <li>Copy children and save to DB</li>
     * <li>Save {@link Program} once all children are created and saved to
     * DB</li>
     * </ol>
     *
     * @param original {@link Program}
     * @param copyOptions {@link Map} of options to apply.
     * @return {@link Program} copy
     */
    private Program applyAllProgramCopySteps( Program original, Map<String, String> copyOptions )
    {
        Program copy = Program.shallowCopy( original, copyOptions );
        programService.addProgram( copy );

        copyIndicators( original, copy, copyOptions );
        Map<String, Program.ProgramStageTuple> stageMappings = copyStages( original, copy );
        copySections( original, copy );
        copyAttributes( original, copy );
        copyRuleVariables( original, copy, stageMappings );
        copyEnrollments( original, copy );

        programService.addProgram( copy );

        return copy;
    }

    private Map<String, Program.ProgramStageTuple> copyStages( Program original, Program programCopy )
    {
        Map<String, Program.ProgramStageTuple> stageMappings = new HashMap<>();
        Set<ProgramStage> copyStages = new HashSet<>();
        for ( ProgramStage stageOriginal : original.getProgramStages() )
        {
            ProgramStage copy = copyProgramStage( stageOriginal, programCopy );
            copyStages.add( copy );
            stageMappings.put( stageOriginal.getUid(), new Program.ProgramStageTuple( stageOriginal, copy ) );
        }
        programCopy.setProgramStages( copyStages );
        return stageMappings;
    }

    private void copyEnrollments( Program original, Program copy )
    {
        List<Enrollment> enrollments = copyList( copy,
            enrollmentService.getEnrollments( original ), Enrollment.copyOf );
        enrollments.forEach( enrollmentService::addEnrollment );
    }

    /**
     * This method identifies the correct {@link ProgramStage} to use for the
     * {@link ProgramRuleVariable}. If a key is found then the copy value of the
     * {@link org.hisp.dhis.program.Program.ProgramStageTuple} is returned,
     * otherwise the original {@link ProgramStage} is returned.
     *
     * @param original {@link ProgramRuleVariable}
     * @param stageMappings {@link Map} with a {@link String} key and
     *        {@link org.hisp.dhis.program.Program.ProgramStageTuple} value. The
     *        map key is the UID of the original {@link ProgramStage}. The map
     *        value is a
     *        {@link org.hisp.dhis.program.Program.ProgramStageTuple}, holding
     *        an original {@link ProgramStage} and a copy {@link ProgramStage}.
     */
    private ProgramStage getProgramStageFromMapping( ProgramRuleVariable original,
        Map<String, Program.ProgramStageTuple> stageMappings )
    {
        ProgramStage originalStage = original.getProgramStage();
        if ( originalStage != null && stageMappings.containsKey( originalStage.getUid() ) )
        {
            return stageMappings.get( originalStage.getUid() ).copy();
        }
        return original.getProgramStage();
    }

    private Set<ProgramRuleVariable> copyRuleVariables( Program original, Program programCopy,
        Map<String, Program.ProgramStageTuple> stageMappings )
    {
        Set<ProgramRuleVariable> ruleVariables = new HashSet<>();

        for ( ProgramRuleVariable ruleVariable : original.getProgramRuleVariables() )
        {
            ProgramStage programStage = getProgramStageFromMapping( ruleVariable, stageMappings );
            ProgramRuleVariable copy = copyProgramRuleVariable( ruleVariable, programCopy, programStage );
            ruleVariables.add( copy );
        }
        programCopy.setProgramRuleVariables( ruleVariables );
        return ruleVariables;
    }

    private void copyIndicators( Program original, Program programCopy,
        Map<String, String> copyOptions )
    {
        Set<ProgramIndicator> indicators = original.getProgramIndicators().stream()
            .map( indicator -> copyProgramIndicator( indicator, programCopy, copyOptions ) )
            .collect( Collectors.toSet() );
        programCopy.setProgramIndicators( indicators );
    }

    private void copyAttributes( Program original, Program programCopy )
    {
        programCopy.setProgramAttributes( copyList( programCopy,
            original.getProgramAttributes(), ProgramTrackedEntityAttribute.copyOf ) );
    }

    private void copySections( Program original, Program programCopy )
    {
        Set<ProgramSection> copySections = copySet( programCopy, original.getProgramSections(),
            ProgramSection.copyOf );
        copySections.forEach( programSectionService::addProgramSection );
        programCopy.setProgramSections( copySections );
    }

}
