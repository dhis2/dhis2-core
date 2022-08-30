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
package org.hisp.dhis.trackedentityinstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.utils.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class TrackedEntityInstanceQueryLimitTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private UserService _userService;

    private OrganisationUnit orgUnitA;

    private Program program;

    private ProgramInstance pi1;

    private ProgramInstance pi2;

    private ProgramInstance pi3;

    private ProgramInstance pi4;

    private TrackedEntityInstance tei1;

    private TrackedEntityInstance tei2;

    private TrackedEntityInstance tei3;

    private TrackedEntityInstance tei4;

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

        trackedEntityInstanceService.addTrackedEntityInstance( tei1 );
        trackedEntityInstanceService.addTrackedEntityInstance( tei2 );
        trackedEntityInstanceService.addTrackedEntityInstance( tei3 );
        trackedEntityInstanceService.addTrackedEntityInstance( tei4 );

        pi1 = createProgramInstance( program, tei1, orgUnitA );
        pi2 = createProgramInstance( program, tei2, orgUnitA );
        pi3 = createProgramInstance( program, tei3, orgUnitA );
        pi4 = createProgramInstance( program, tei4, orgUnitA );

        programInstanceService.addProgramInstance( pi1 );
        programInstanceService.addProgramInstance( pi2 );
        programInstanceService.addProgramInstance( pi3 );
        programInstanceService.addProgramInstance( pi4 );

        programInstanceService.enrollTrackedEntityInstance( tei1, program, new Date(), new Date(), orgUnitA );
        programInstanceService.enrollTrackedEntityInstance( tei2, program, new Date(), new Date(), orgUnitA );
        programInstanceService.enrollTrackedEntityInstance( tei3, program, new Date(), new Date(), orgUnitA );
        programInstanceService.enrollTrackedEntityInstance( tei4, program, new Date(), new Date(), orgUnitA );

        userService.addUser( user );
    }

    @Test
    void testConfiguredMaxTeiLimit()
    {
        systemSettingManager.saveSystemSetting( SettingKey.TRACKED_ENTITY_MAX_LIMIT, 3 );
        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits( Set.of( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUser( user );
        params.setSkipPaging( true );

        List<Long> teis = trackedEntityInstanceService.getTrackedEntityInstanceIds( params,
            false, false );

        assertNotNull( teis );
        assertEquals( 3, teis.size(), "Size cannot be more than configured Tei max limit" );
    }

    @Test
    void testDefaultMaxTeiLimit()
    {
        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits( Set.of( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUser( user );
        params.setSkipPaging( true );

        List<Long> teis = trackedEntityInstanceService.getTrackedEntityInstanceIds( params,
            false, false );

        assertNotNull( teis );
        Assertions.assertContainsOnly( teis, tei1.getId(), tei2.getId(), tei3.getId(), tei4.getId() );
    }

    @Test
    void testDisabledMaxTeiLimit()
    {
        systemSettingManager.saveSystemSetting( SettingKey.TRACKED_ENTITY_MAX_LIMIT, 0 );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits( Set.of( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUser( user );
        params.setSkipPaging( true );

        List<Long> teis = trackedEntityInstanceService.getTrackedEntityInstanceIds( params,
            false, false );

        assertNotNull( teis );
        Assertions.assertContainsOnly( teis, tei1.getId(), tei2.getId(), tei3.getId(), tei4.getId() );
    }
}
