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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.mockito.Mockito.when;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class DefaultTrackerAccessManagerTest
{

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private DefaultTrackerAccessManager trackerAccessManager;

    @Test
    void shouldHaveAccessWhenProgramOpenAndSearchAccessAvailable()
    {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel( OPEN );
        OrganisationUnit orgUnit = new OrganisationUnit();
        when( organisationUnitService.isInUserSearchHierarchy( user, orgUnit ) ).thenReturn( true );

        Assertions.assertTrue( trackerAccessManager.canAccess( user, program, orgUnit ) );
    }

    @Test
    void shouldNotHaveAccessWhenProgramNullAndSearchAccessNotAvailable()
    {
        User user = new User();
        OrganisationUnit orgUnit = new OrganisationUnit();
        when( organisationUnitService.isInUserSearchHierarchy( user, orgUnit ) ).thenReturn( false );

        Assertions.assertFalse( trackerAccessManager.canAccess( user, null, orgUnit ) );
    }

    @Test
    void shouldHaveAccessWhenProgramClosedAndCaptureAccessAvailable()
    {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel( CLOSED );
        OrganisationUnit orgUnit = new OrganisationUnit();
        when( organisationUnitService.isInUserHierarchy( user, orgUnit ) ).thenReturn( true );

        Assertions.assertTrue( trackerAccessManager.canAccess( user, program, orgUnit ) );
    }

    @Test
    void shouldNotHaveAccessWhenProgramProtectedAndCaptureAccessNotAvailable()
    {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel( PROTECTED );
        OrganisationUnit orgUnit = new OrganisationUnit();
        when( organisationUnitService.isInUserHierarchy( user, orgUnit ) ).thenReturn( false );

        Assertions.assertFalse( trackerAccessManager.canAccess( user, program, orgUnit ) );
    }
}
