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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class TrackedEntityQueryLimitTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private UserService _userService;

    private OrganisationUnit orgUnitA;

    private Program program;

    private Enrollment enrollment1;

    private Enrollment enrollment2;

    private Enrollment enrollment3;

    private Enrollment enrollment4;

    private TrackedEntity tei1;

    private TrackedEntity tei2;

    private TrackedEntity tei3;

    private TrackedEntity tei4;

    private TrackedEntityType teiType;

    private User user;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        user = createAndInjectAdminUser();

        orgUnitA = createOrganisationUnit( "A" );
        organisationUnitService.addOrganisationUnit( orgUnitA );

        user.getOrganisationUnits().add( orgUnitA );

        teiType = createTrackedEntityType( 'P' );
        trackedEntityTypeService.addTrackedEntityType( teiType );

        program = createProgram( 'P' );
        programService.addProgram( program );

        tei1 = createTrackedEntityInstance( orgUnitA );
        tei2 = createTrackedEntityInstance( orgUnitA );
        tei3 = createTrackedEntityInstance( orgUnitA );
        tei4 = createTrackedEntityInstance( orgUnitA );
        tei1.setTrackedEntityType( teiType );
        tei2.setTrackedEntityType( teiType );
        tei3.setTrackedEntityType( teiType );
        tei4.setTrackedEntityType( teiType );

        trackedEntityService.addTrackedEntity( tei1 );
        trackedEntityService.addTrackedEntity( tei2 );
        trackedEntityService.addTrackedEntity( tei3 );
        trackedEntityService.addTrackedEntity( tei4 );

        enrollment1 = createEnrollment( program, tei1, orgUnitA );
        enrollment2 = createEnrollment( program, tei2, orgUnitA );
        enrollment3 = createEnrollment( program, tei3, orgUnitA );
        enrollment4 = createEnrollment( program, tei4, orgUnitA );

        enrollmentService.addEnrollment( enrollment1 );
        enrollmentService.addEnrollment( enrollment2 );
        enrollmentService.addEnrollment( enrollment3 );
        enrollmentService.addEnrollment( enrollment4 );

        enrollmentService.enrollTrackedEntityInstance( tei1, program, new Date(), new Date(), orgUnitA );
        enrollmentService.enrollTrackedEntityInstance( tei2, program, new Date(), new Date(), orgUnitA );
        enrollmentService.enrollTrackedEntityInstance( tei3, program, new Date(), new Date(), orgUnitA );
        enrollmentService.enrollTrackedEntityInstance( tei4, program, new Date(), new Date(), orgUnitA );

        userService.addUser( user );
    }

    @Test
    void testConfiguredPositiveMaxTeiLimit()
    {
        systemSettingManager.saveSystemSetting( SettingKey.TRACKED_ENTITY_MAX_LIMIT, 3 );
        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits( Set.of( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUserWithAssignedUsers( null, user, null );
        params.setSkipPaging( true );

        List<Long> teis = trackedEntityService.getTrackedEntityIds( params,
            false, false );

        assertNotNull( teis );
        assertEquals( 3, teis.size(), "Size cannot be more than configured Tei max limit" );
    }

    @Test
    void testConfiguredNegativeMaxTeiLimit()
    {
        systemSettingManager.saveSystemSetting( SettingKey.TRACKED_ENTITY_MAX_LIMIT, -1 );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits( Set.of( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUserWithAssignedUsers( null, user, null );
        params.setSkipPaging( true );

        List<Long> teis = trackedEntityService.getTrackedEntityIds( params,
            false, false );

        assertContainsOnly( List.of( tei1.getId(), tei2.getId(), tei3.getId(), tei4.getId() ), teis );
    }

    @Test
    void testDefaultMaxTeiLimit()
    {
        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits( Set.of( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUserWithAssignedUsers( null, user, null );
        params.setSkipPaging( true );

        List<Long> teis = trackedEntityService.getTrackedEntityIds( params,
            false, false );

        assertContainsOnly( List.of( tei1.getId(), tei2.getId(), tei3.getId(), tei4.getId() ), teis );
    }

    @Test
    void testDisabledMaxTeiLimit()
    {
        systemSettingManager.saveSystemSetting( SettingKey.TRACKED_ENTITY_MAX_LIMIT, 0 );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits( Set.of( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUserWithAssignedUsers( null, user, null );
        params.setSkipPaging( true );

        List<Long> teis = trackedEntityService.getTrackedEntityIds( params,
            false, false );

        assertContainsOnly( List.of( tei1.getId(), tei2.getId(), tei3.getId(), tei4.getId() ), teis );
    }
}
