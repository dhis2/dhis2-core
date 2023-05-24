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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramCopyTuple;
import org.hisp.dhis.program.ProgramEnrollmentsTuple;
import org.hisp.dhis.program.ProgramService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CopyService
{
    private ProgramService programService;

    private EnrollmentService enrollmentService;

    public CopyService( ProgramService programService, EnrollmentService enrollmentService )
    {
        this.programService = programService;
        this.enrollmentService = enrollmentService;
    }

    public String copyProgramFromUid( String uid, Map<String, String> copyOptions )
        throws NotFoundException,
        ConflictException
    {
        try
        {
            Program original = programService.getProgram( uid );
            if ( original != null )
            {
                return applyAllProgramCopySteps( original, copyOptions );
            }
            throw new NotFoundException( Program.class, uid );
        }
        catch ( DataIntegrityViolationException div )
        {
            throw new ConflictException( Objects.requireNonNull( div.getRootCause() ).getMessage() );
        }
    }

    public String applyAllProgramCopySteps( Program program, Map<String, String> copyOptions )
    {
        return Program.copyOf
            .andThen( saveNewProgram )
            .andThen( copyEnrollments )
            .andThen( saveNewEnrollments )
            .apply( program, copyOptions );
    }

    private final UnaryOperator<ProgramCopyTuple> saveNewProgram = programCopyTuple -> {
        programService.addProgram( programCopyTuple.copy() );
        return programCopyTuple;
    };

    private final Function<ProgramCopyTuple, ProgramEnrollmentsTuple> copyEnrollments = programCopyTuple -> {
        List<Enrollment> copiedEnrollments = enrollmentService.getEnrollments( programCopyTuple.original() )
            .stream()
            .map( enrollment -> Enrollment.copyOf( enrollment, programCopyTuple.copy() ) )
            .toList();
        return new ProgramEnrollmentsTuple( programCopyTuple.copy(), copiedEnrollments );
    };

    private final Function<ProgramEnrollmentsTuple, String> saveNewEnrollments = programEnrollmentsTuple -> {
        programEnrollmentsTuple.enrollments().forEach( enrollmentService::addEnrollment );
        return programEnrollmentsTuple.program().getUid();
    };
}
