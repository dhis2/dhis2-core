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
package org.hisp.dhis.tracker.preheat.supplier;

import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.program.ProgramType.WITH_REGISTRATION;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class ProgramInstanceSupplierTest extends DhisConvenienceTest
{
    @InjectMocks
    private ProgramInstanceSupplier supplier;

    @Mock
    private ProgramInstanceStore store;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    private List<ProgramInstance> programInstances;

    private Program programWithRegistration;

    private Program programWithoutRegistration;

    private TrackerImportParams params;

    @Before
    public void setUp()
    {
        programWithRegistration = createProgram( 'A' );
        programWithRegistration.setProgramType( WITH_REGISTRATION );

        programWithoutRegistration = createProgram( 'B' );
        programWithoutRegistration.setProgramType( WITHOUT_REGISTRATION );

        params = TrackerImportParams.builder().build();

        programInstances = rnd.randomObjects( ProgramInstance.class, 2 );
        // set the OrgUnit parent to null to avoid recursive errors when mapping
        programInstances.forEach( p -> p.getOrganisationUnit().setParent( null ) );
        programInstances.get( 0 ).setProgram( programWithRegistration );
        programInstances.get( 1 ).setProgram( programWithoutRegistration );

        when( store.getByPrograms( Lists.newArrayList( programWithoutRegistration ) ) ).thenReturn( programInstances );

    }

    @Test
    public void verifySupplierWhenNoEventProgramArePresent()
    {
        // given
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.put( TrackerIdentifier.UID, programWithRegistration );

        // when
        this.supplier.preheatAdd( params, preheat );

        // then
        final List<String> programUids = programInstances
            .stream()
            .map( pi -> pi.getProgram().getUid() )
            .collect( Collectors.toList() );
        for ( String programUid : programUids )
        {
            assertNull( preheat.getProgramInstancesWithoutRegistration( programUid ) );
        }
    }

    @Test
    public void verifySupplier()
    {
        // given
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.put( TrackerIdentifier.UID, Lists.newArrayList( programWithRegistration, programWithoutRegistration ) );

        // when
        this.supplier.preheatAdd( params, preheat );

        // then
        assertNotNull( preheat.getProgramInstancesWithoutRegistration( programWithoutRegistration.getUid() ) );
    }

}
