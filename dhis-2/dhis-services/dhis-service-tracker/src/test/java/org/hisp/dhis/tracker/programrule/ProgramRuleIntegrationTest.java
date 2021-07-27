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
package org.hisp.dhis.tracker.programrule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerWarningReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

public class ProgramRuleIntegrationTest
    extends TransactionalIntegrationTest
{
    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    private User userA;

    @Override
    public void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "tracker/simple_metadata.json" ).getInputStream(),
                RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertFalse( validationReport.hasErrorReports() );

        objectBundleService.commit( bundle );

        Program program = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class, "BFcipDERJnf" );
        Program programWithoutRegistration = bundle.getPreheat()
            .get( PreheatIdentifier.UID, Program.class, "BFcipDERJne" );
        DataElement dataElement = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00001" );
        ProgramStage programStage = bundle.getPreheat().get( PreheatIdentifier.UID, ProgramStage.class, "NpsdDv6kKSO" );

        ProgramRule programRuleA = createProgramRule( 'A', program );
        programRuleA.setUid( "ProgramRule" );
        programRuleService.addProgramRule( programRuleA );

        ProgramRule programRuleWithoutRegistration = createProgramRule( 'W', programWithoutRegistration );
        programRuleService.addProgramRule( programRuleWithoutRegistration );

        ProgramRule programRuleB = createProgramRule( 'B', program );
        programRuleB.setProgramStage( programStage );
        programRuleService.addProgramRule( programRuleB );

        ProgramRuleAction programRuleActionShowWarning = createProgramRuleAction( 'A', programRuleA );
        programRuleActionShowWarning.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleActionShowWarning.setContent( "WARNING" );
        programRuleActionService.addProgramRuleAction( programRuleActionShowWarning );

        ProgramRuleAction programRuleActionAssign = createProgramRuleAction( 'C', programRuleA );
        programRuleActionAssign.setProgramRuleActionType( ProgramRuleActionType.ASSIGN );
        programRuleActionAssign.setData( "'NEWTEXT'" );
        programRuleActionAssign.setDataElement( dataElement );
        programRuleActionService.addProgramRuleAction( programRuleActionAssign );

        ProgramRuleAction programRuleActionShowWarningForProgramStage = createProgramRuleAction( 'B', programRuleB );
        programRuleActionShowWarningForProgramStage.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleActionShowWarningForProgramStage.setContent( "PROGRAM STAGE WARNING" );
        programRuleActionService.addProgramRuleAction( programRuleActionShowWarningForProgramStage );

        programRuleA.getProgramRuleActions().add( programRuleActionShowWarning );
        programRuleA.getProgramRuleActions().add( programRuleActionAssign );

        programRuleWithoutRegistration.getProgramRuleActions().add( programRuleActionShowWarning );
        programRuleService.updateProgramRule( programRuleWithoutRegistration );

        programRuleB.getProgramRuleActions().add( programRuleActionShowWarningForProgramStage );
        programRuleService.updateProgramRule( programRuleB );

        userA = userService.getUser( "M5zQapPyTZI" );
    }

    @Test
    public void testImportProgramEventSuccessWithWarningRaised()
        throws IOException
    {
        InputStream inputStream = new ClassPathResource( "tracker/program_event.json" ).getInputStream();

        TrackerImportParams params = renderService.fromJson( inputStream, TrackerImportParams.class );
        params.setUserId( userA.getUid() );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertNotNull( trackerImportReport );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertEquals( 1, trackerImportReport.getValidationReport().getWarningReports().size() );
    }

    @Test
    public void testImportEnrollmentSuccessWithWarningRaised()
        throws IOException
    {
        InputStream inputStream = new ClassPathResource( "tracker/single_tei.json" ).getInputStream();

        TrackerImportParams params = renderService.fromJson( inputStream, TrackerImportParams.class );
        params.setUserId( userA.getUid() );
        TrackerImportReport trackerImportTeiReport = trackerImportService.importTracker( params );

        TrackerImportParams enrollmentParams = renderService
            .fromJson( new ClassPathResource( "tracker/single_enrollment.json" ).getInputStream(),
                TrackerImportParams.class );
        enrollmentParams.setUserId( userA.getUid() );
        TrackerImportReport trackerImportEnrollmentReport = trackerImportService
            .importTracker( enrollmentParams );

        assertNotNull( trackerImportTeiReport );
        assertEquals( TrackerStatus.OK, trackerImportTeiReport.getStatus() );

        assertNotNull( trackerImportEnrollmentReport );
        assertEquals( TrackerStatus.OK, trackerImportEnrollmentReport.getStatus() );
        assertEquals( 1, trackerImportEnrollmentReport.getValidationReport().getWarningReports().size() );
    }

    @Test
    public void testImportEventInProgramStageSuccessWithWarningRaised()
        throws IOException
    {
        InputStream inputStream = new ClassPathResource( "tracker/tei_enrollment_event.json" ).getInputStream();

        TrackerImportParams params = renderService.fromJson( inputStream, TrackerImportParams.class );
        params.setUserId( userA.getUid() );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertNotNull( trackerImportReport );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        List<TrackerWarningReport> warningReports = trackerImportReport.getValidationReport().getWarningReports();
        assertEquals( 4, warningReports.size() );
        assertEquals( 3,
            warningReports.stream().filter( w -> w.getTrackerType().equals( TrackerType.EVENT ) ).count() );
        assertEquals( 1,
            warningReports.stream().filter( w -> w.getTrackerType().equals( TrackerType.ENROLLMENT ) ).count() );

        inputStream = new ClassPathResource( "tracker/event_update_no_datavalue.json" ).getInputStream();

        params = renderService.fromJson( inputStream, TrackerImportParams.class );
        params.setUserId( userA.getUid() );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );
        trackerImportReport = trackerImportService.importTracker( params );

        assertNotNull( trackerImportReport );
        assertEquals( TrackerStatus.ERROR, trackerImportReport.getStatus() );

        List<TrackerErrorReport> errorReports = trackerImportReport.getValidationReport().getErrorReports();
        assertEquals( 1, errorReports.size() );
        assertEquals( TrackerErrorCode.E1307, errorReports.get( 0 ).getErrorCode() );
        assertEquals(
            "Generated by program rule (`ProgramRule`) - Unable to assign value to data element `DATAEL00001`. The provided value must be empty or match the calculated value `NEWTEXT`",
            errorReports.get( 0 ).getErrorMessage() );
    }
}
