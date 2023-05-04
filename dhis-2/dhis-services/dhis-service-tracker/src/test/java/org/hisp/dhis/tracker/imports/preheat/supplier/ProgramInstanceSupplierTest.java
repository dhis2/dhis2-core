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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.program.ProgramType.WITH_REGISTRATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStore;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class ProgramInstanceSupplierTest extends DhisConvenienceTest
{

    @InjectMocks
    private ProgramInstanceSupplier supplier;

    @Mock
    private EnrollmentStore enrollmentStore;

    @Mock
    private ProgramStore programStore;

    private List<Enrollment> enrollments;

    private Program programWithRegistration;

    private Program programWithoutRegistration;

    private TrackerImportParams params;

    private final BeanRandomizer rnd = BeanRandomizer.create();

    private TrackerPreheat preheat;

    private Enrollment enrollmentWithoutRegistration;

    @BeforeEach
    public void setUp()
    {
        enrollments = rnd.objects( Enrollment.class, 2 ).collect( Collectors.toList() );
        // set the OrgUnit parent to null to avoid recursive errors when mapping
        enrollments.forEach( p -> p.getOrganisationUnit().setParent( null ) );

        programWithRegistration = createProgram( 'A' );
        programWithRegistration.setProgramType( WITH_REGISTRATION );
        Enrollment enrollmentWithRegistration = enrollments.get( 0 );
        enrollmentWithRegistration.setProgram( programWithRegistration );

        programWithoutRegistration = createProgram( 'B' );
        programWithoutRegistration.setProgramType( WITHOUT_REGISTRATION );
        enrollmentWithoutRegistration = enrollments.get( 1 );
        enrollmentWithoutRegistration.setProgram( programWithoutRegistration );

        when( enrollmentStore.getByPrograms( Lists.newArrayList( programWithoutRegistration ) ) )
            .thenReturn( enrollments );

        params = TrackerImportParams.builder().build();
        preheat = new TrackerPreheat();
    }

    @Test
    void verifySupplierWhenNoEventProgramArePresent()
    {
        preheat.put( TrackerIdSchemeParam.UID, programWithRegistration );

        this.supplier.preheatAdd( params, preheat );

        final List<String> programUids = enrollments
            .stream()
            .map( pi -> pi.getProgram().getUid() )
            .collect( Collectors.toList() );
        for ( String programUid : programUids )
        {
            assertNull( preheat.getProgramInstancesWithoutRegistration( programUid ) );
        }
    }

    @Test
    void verifySupplierWhenNoProgramsArePresent()
    {
        when( programStore.getByType( WITHOUT_REGISTRATION ) ).thenReturn( List.of( programWithoutRegistration ) );
        enrollments = rnd.objects( Enrollment.class, 1 ).collect( Collectors.toList() );
        // set the OrgUnit parent to null to avoid recursive errors when mapping
        enrollments.forEach( p -> p.getOrganisationUnit().setParent( null ) );
        Enrollment enrollment = enrollments.get( 0 );
        enrollment.setProgram( programWithoutRegistration );
        when( enrollmentStore.getByPrograms( List.of( programWithoutRegistration ) ) )
            .thenReturn( enrollments );

        this.supplier.preheatAdd( params, preheat );

        assertProgramInstanceInPreheat( enrollment,
            preheat.getProgramInstancesWithoutRegistration( programWithoutRegistration.getUid() ) );
    }

    @Test
    void verifySupplier()
    {
        preheat.put( TrackerIdSchemeParam.UID,
            Lists.newArrayList( programWithRegistration, programWithoutRegistration ) );

        this.supplier.preheatAdd( params, preheat );

        assertProgramInstanceInPreheat( enrollmentWithoutRegistration,
            preheat.getProgramInstancesWithoutRegistration( programWithoutRegistration.getUid() ) );
    }

    private void assertProgramInstanceInPreheat( Enrollment expected, Enrollment actual )
    {
        assertEquals( expected.getUid(), actual.getUid() );
        assertEquals( expected.getProgram().getUid(), actual.getProgram().getUid() );
        assertEquals( actual, preheat.getEnrollment( actual.getUid() ) );
    }
}
