package org.hisp.dhis.tracker.preheat.supplier;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class ProgramInstanceSupplierTest
{
    @InjectMocks
    private ProgramInstanceSupplier supplier;

    @Mock
    private ProgramInstanceStore store;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifySupplier()
    {
        final List<ProgramInstance> programInstances = rnd.randomObjects( ProgramInstance.class, 5 );
        // set the OrgUnit parent to null to avoid recursive errors when mapping
        programInstances.forEach( p -> p.getOrganisationUnit().setParent( null ) );
        when( store.getByType( WITHOUT_REGISTRATION ) ).thenReturn( programInstances );

        final TrackerImportParams params = TrackerImportParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        final List<String> programUids = programInstances.stream().map( pi -> pi.getProgram().getUid() )
            .collect( Collectors.toList() );
        for ( String programUid : programUids )
        {
            assertNotNull( preheat.getProgramInstancesWithoutRegistration( programUid ) );
        }
    }

}
