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
package org.hisp.dhis.tracker.imports.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_3;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_4;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_5;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EventSecurityImportValidationTest extends TrackerTest
{

    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private EventService programStageServiceInstance;

    @Autowired
    private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserService _userService;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private Program programA;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStage programStageA;

    private ProgramStage programStageB;

    private TrackedEntityType trackedEntityType;

    @Override
    protected void initTest()
        throws IOException
    {
        userService = _userService;
        setUpMetadata( "tracker/tracker_basic_metadata.json" );
        injectAdminUser();
        assertNoErrors( trackerImportService.importTracker( fromJson(
            "tracker/validations/enrollments_te_te-data.json" ) ) );
        assertNoErrors( trackerImportService
            .importTracker( fromJson( "tracker/validations/enrollments_te_enrollments-data.json" ) ) );
        manager.flush();
    }

    private void setupMetadata()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );
        manager.save( organisationUnitA );
        manager.save( organisationUnitB );
        organisationUnitA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( organisationUnitA );
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementB.setValueType( ValueType.INTEGER );
        manager.save( dataElementA );
        manager.save( dataElementB );
        programStageA = createProgramStage( 'A', 0 );
        programStageB = createProgramStage( 'B', 0 );
        programStageB.setRepeatable( true );
        manager.save( programStageA );
        manager.save( programStageB );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        TrackedEntityType trackedEntityTypeFromProgram = createTrackedEntityType( 'C' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeFromProgram );
        manager.save( programA );
        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        programStageA.getProgramStageDataElements().add( programStageDataElement );
        programStageA.setProgram( programA );
        programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementB );
        programStageDataElement.setProgramStage( programStageB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        programStageB.getProgramStageDataElements().add( programStageDataElement );
        programStageB.setProgram( programA );
        programStageB.setMinDaysFromStart( 2 );
        programA.getProgramStages().add( programStageA );
        programA.getProgramStages().add( programStageB );
        programA.setTrackedEntityType( trackedEntityType );
        trackedEntityType.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programStageA );
        manager.update( programStageB );
        manager.update( programA );
        maleA = createTrackedEntityInstance( 'A', organisationUnitA );
        maleB = createTrackedEntityInstance( organisationUnitB );
        femaleA = createTrackedEntityInstance( organisationUnitA );
        femaleB = createTrackedEntityInstance( organisationUnitB );
        maleA.setTrackedEntityType( trackedEntityType );
        maleB.setTrackedEntityType( trackedEntityType );
        femaleA.setTrackedEntityType( trackedEntityType );
        femaleB.setTrackedEntityType( trackedEntityType );
        manager.save( maleA );
        manager.save( maleB );
        manager.save( femaleA );
        manager.save( femaleB );
        int testYear = Calendar.getInstance().get( Calendar.YEAR ) - 1;
        Date dateMar20 = getDate( testYear, 3, 20 );
        Date dateApr10 = getDate( testYear, 4, 10 );
        Enrollment enrollment = enrollmentService.enrollTrackedEntityInstance( maleA, programA,
            dateMar20, dateApr10, organisationUnitA, "MNWZ6hnuhSX" );
        enrollmentService.addEnrollment( enrollment );
        trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner( maleA.getUid(), programA.getUid(),
            organisationUnitA.getUid() );
        manager.update( programA );
        User user = userService.getUser( USER_5 );
        OrganisationUnit qfUVllTs6cS = organisationUnitService.getOrganisationUnit( "QfUVllTs6cS" );
        user.addOrganisationUnit( qfUVllTs6cS );
        user.addOrganisationUnit( organisationUnitA );
        User adminUser = userService.getUser( ADMIN_USER_UID );
        adminUser.addOrganisationUnit( organisationUnitA );
        Program p = programService.getProgram( "prabcdefghA" );
        p.addOrganisationUnit( qfUVllTs6cS );
        programService.updateProgram( p );
        manager.update( user );
        manager.update( adminUser );
    }

    @Test
    void testNoWriteAccessToProgramStage()
        throws IOException
    {
        setupMetadata();
        TrackerImportParams trackerBundleParams = fromJson(
            "tracker/validations/events_error-no-programStage-access.json" );
        User user = userService.getUser( USER_3 );
        trackerBundleParams.setUser( user );
        user.addOrganisationUnit( organisationUnitA );
        manager.update( user );
        trackerBundleParams.setUser( user );
        ImportReport importReport = trackerImportService.importTracker( trackerBundleParams );

        assertHasOnlyErrors( importReport, ValidationCode.E1095, ValidationCode.E1096 );
    }

    @Test
    void testNoUncompleteEventAuth()
        throws IOException
    {
        setupMetadata();
        TrackerImportParams params = fromJson( "tracker/validations/events_error-no-uncomplete.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        ImportReport importReport = trackerImportService.importTracker( params );
        assertNoErrors( importReport );
        // Change just inserted Event to status COMPLETED...
        Event zwwuwNp6gVd = programStageServiceInstance.getEvent( "ZwwuwNp6gVd" );
        zwwuwNp6gVd.setStatus( EventStatus.COMPLETED );
        manager.update( zwwuwNp6gVd );
        TrackerImportParams trackerBundleParams = fromJson(
            "tracker/validations/events_error-no-uncomplete.json" );
        programA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programA );
        programStageA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( programStageA );
        maleA.setPublicAccess( AccessStringHelper.FULL );
        manager.update( maleA );
        User user = userService.getUser( USER_4 );
        user.addOrganisationUnit( organisationUnitA );
        manager.update( user );
        manager.flush();
        manager.clear();
        trackerBundleParams.setUserId( user.getUid() );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        importReport = trackerImportService.importTracker( trackerBundleParams );
        assertHasOnlyErrors( importReport, ValidationCode.E1083 );
    }
}
