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
package org.hisp.dhis.dxf2.events;

import static org.hisp.dhis.user.UserRole.AUTHORITY_ALL;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.EnrollmentParams;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author <luca@dhis2.org>
 */
class EnrollmentImportTest extends TransactionalIntegrationTest
{
    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    private TrackedEntity trackedEntity;

    private OrganisationUnit organisationUnitA;

    private Program program;

    private Enrollment enrollment;

    private User user;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;

        organisationUnitA = createOrganisationUnit( 'A' );
        manager.save( organisationUnitA );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );

        trackedEntity = createTrackedEntity( organisationUnitA );
        trackedEntity.setTrackedEntityType( trackedEntityType );
        manager.save( trackedEntity );

        program = createProgram( 'A', new HashSet<>(), organisationUnitA );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        manager.save( program );

        enrollment = new Enrollment();
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        enrollment.setProgram( program );
        enrollment.setStatus( ProgramStatus.ACTIVE );
        enrollment.setStoredBy( "test" );
        enrollment.setName( "test" );
        enrollment.enrollTrackedEntity( trackedEntity, program );
        manager.save( enrollment );

        user = createAndAddAdminUser( AUTHORITY_ALL );
    }

    @Test
    void shouldSetCreatedByUserInfoWhenCreateEnrollments()
    {
        String enrollmentUid = CodeGenerator.generateUid();

        org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment = enrollment( organisationUnitA.getUid(),
            program.getUid(),
            trackedEntity.getUid() );
        enrollment.setEnrollment( enrollmentUid );

        ImportSummaries importSummaries = enrollmentService.addEnrollments( List.of( enrollment ),
            new ImportOptions().setUser( user ),
            null );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() ),
            () -> assertEquals( UserInfoSnapshot.from( user ),
                enrollmentService.getEnrollment( enrollmentUid, EnrollmentParams.FALSE )
                    .getCreatedByUserInfo() ),
            () -> assertEquals( UserInfoSnapshot.from( user ),
                enrollmentService.getEnrollment( enrollmentUid, EnrollmentParams.FALSE )
                    .getLastUpdatedByUserInfo() ) );
    }

    @Test
    void shouldSetUpdatedByUserInfoWhenUpdateEnrollments()
    {
        org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment = enrollment( organisationUnitA.getUid(),
            program.getUid(),
            trackedEntity.getUid() );
        enrollment.setEnrollment( this.enrollment.getUid() );

        ImportSummaries importSummaries = enrollmentService.updateEnrollments( List.of( enrollment ),
            new ImportOptions().setUser( user ),
            true );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() ),
            () -> assertEquals( UserInfoSnapshot.from( user ),
                enrollmentService.getEnrollment( this.enrollment.getUid(), EnrollmentParams.FALSE )
                    .getLastUpdatedByUserInfo() ) );
    }

    private org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment( String orgUnit, String program,
        String trackedEntity )
    {
        org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment = new org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment();
        enrollment.setOrgUnit( orgUnit );
        enrollment.setProgram( program );
        enrollment.setTrackedEntityInstance( trackedEntity );
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        return enrollment;
    }
}
