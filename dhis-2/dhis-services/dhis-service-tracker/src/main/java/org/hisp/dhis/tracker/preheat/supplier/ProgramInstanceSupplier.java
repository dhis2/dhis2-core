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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStore;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.mappers.ProgramInstanceMapper;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Component
public class ProgramInstanceSupplier extends AbstractPreheatSupplier
{
    @NonNull
    private final ProgramInstanceStore programInstanceStore;

    @NonNull
    private final ProgramStore programStore;

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {
        List<Program> programsWithoutRegistration = preheat.getAll( Program.class )
            .stream()
            .filter( program -> program.getProgramType().equals( ProgramType.WITHOUT_REGISTRATION ) )
            .collect( Collectors.toList() );
        if ( programsWithoutRegistration.isEmpty() )
        {
            programsWithoutRegistration = programStore.getByType( ProgramType.WITHOUT_REGISTRATION );
        }
        if ( !programsWithoutRegistration.isEmpty() )
        {
            List<ProgramInstance> programInstances = DetachUtils.detach( ProgramInstanceMapper.INSTANCE,
                programInstanceStore.getByPrograms( programsWithoutRegistration ) );

            programInstances
                .forEach( pi -> {
                    preheat.putEnrollment( pi.getUid(), pi );
                    preheat.putProgramInstancesWithoutRegistration( pi.getProgram().getUid(), pi );
                } );
        }
    }
}
