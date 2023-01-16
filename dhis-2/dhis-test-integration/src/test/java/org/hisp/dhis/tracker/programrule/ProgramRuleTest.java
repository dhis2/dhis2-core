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
package org.hisp.dhis.tracker.programrule;

import static org.hisp.dhis.tracker.Assertions.assertHasError;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyWarnings;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrorsAndNoWarnings;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1300;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1308;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.tracker.report.Warning;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramRuleTest extends TrackerTest
{
    private static final String ENROLLMENT_UID = "TvctPPhpD8u";

    private static final String EVENT_UID = "D9PbzJY8bJO";

    private static final String PROGRAM_EVENT_UID = "PEVENT12345";

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    private Program program;

    private Program programWithoutRegistration;

    private DataElement dataElement1;

    private ProgramStage programStage;

    @Override
    public void initTest()
        throws IOException
    {
        ObjectBundle bundle = setUpMetadata( "tracker/simple_metadata.json" );
        program = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class, "BFcipDERJnf" );
        programWithoutRegistration = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class,
            "BFcipDERJne" );
        dataElement1 = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00001" );
        DataElement dataElement2 = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00002" );
        programStage = bundle.getPreheat().get( PreheatIdentifier.UID, ProgramStage.class, "NpsdDv6kKSO" );
        ProgramRuleVariable programRuleVariable = createProgramRuleVariableWithDataElement( 'A', program,
            dataElement2 );
        programRuleVariableService.addProgramRuleVariable( programRuleVariable );

        injectAdminUser();
    }

    @Test
    void shouldImportEnrollmentWithNoWarningsWhenThereAreNoProgramRules()
        throws IOException
    {
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment.json" ) );

        assertNoErrorsAndNoWarnings( report );
    }

    @Test
    void shouldImportEnrollmentWithWarningsWhenAWarningIsTriggered()
        throws IOException
    {
        alwaysTrueWarningProgramRule();
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment.json" ) );

        assertHasOnlyWarnings( report.getValidationReport(),
            new Warning( "ProgramRulC", E1300.name(), ENROLLMENT.name(), ENROLLMENT_UID ) );
    }

    @Test
    void shouldNotImportEnrollmentWhenAnErrorIsTriggered()
        throws IOException
    {
        alwaysTrueErrorProgramRule();
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment.json" ) );

        assertHasOnlyErrors( report.getValidationReport(), E1300 );
    }

    @Test
    void shouldImportProgramEventWithWarningsWhenAWarningIsTriggered()
        throws IOException
    {
        alwaysTrueWarningProgramEventProgramRule();
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/program_event.json" ) );

        assertHasOnlyWarnings( report.getValidationReport(),
            new Warning( "ProgramRulD", E1300.name(), EVENT.name(), PROGRAM_EVENT_UID ) );
    }

    @Test
    void shouldNotImportProgramEventWhenAnErrorIsTriggered()
        throws IOException
    {
        alwaysTrueErrorProgramEventProgramRule();
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/program_event.json" ) );

        assertHasOnlyErrors( report.getValidationReport(), E1300 );
    }

    @Test
    void shouldImportEventWithNoWarningsWhenThereAreNoProgramRules()
        throws IOException
    {
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment_completed_event.json" ) );

        assertNoErrorsAndNoWarnings( report );
    }

    @Test
    void shouldImportEventWithWarningsWhenAWarningIsTriggered()
        throws IOException
    {
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment.json" ) );
        assertNoErrors( report );

        alwaysTrueWarningProgramRule();
        report = trackerImportService.importTracker( fromJson( "tracker/programrule/event.json" ) );

        assertHasOnlyWarnings( report.getValidationReport(),
            new Warning( "ProgramRulC", E1300.name(), EVENT.name(), EVENT_UID ) );
    }

    @Test
    void shouldNotImportEventWhenAnErrorIsTriggered()
        throws IOException
    {
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment.json" ) );
        assertNoErrors( report );

        alwaysTrueErrorProgramRule();
        report = trackerImportService.importTracker( fromJson( "tracker/programrule/event.json" ) );

        assertHasOnlyErrors( report.getValidationReport(), E1300 );
    }

    @Test
    void shouldNotValidateEventAndValidateEnrollmentWhenAnErrorIsTriggeredOnEvent()
        throws IOException
    {
        onCompleteErrorProgramRule();
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment_completed_event.json" ) );

        assertHasOnlyErrors( report.getValidationReport(), E1300 );
    }

    @Test
    void shouldNotImportEventAndEnrollmentWhenAnErrorIsTriggeredOnEnrollment()
        throws IOException
    {
        onCompleteErrorProgramRule();
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_completed_enrollment_event.json" ) );

        assertAll(
            () -> assertHasError( report, E1300, ENROLLMENT_UID ),
            () -> assertHasError( report, ValidationCode.E5000, EVENT_UID ) );
    }

    @Test
    void shouldImportEventWhenAnErrorIsTriggeredOnEnrollmentAlreadyPresentInDB()
        throws IOException
    {
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_completed_enrollment.json" ) );
        assertNoErrorsAndNoWarnings( report );

        onCompleteErrorProgramRule();
        report = trackerImportService.importTracker( fromJson( "tracker/programrule/event.json" ) );

        assertNoErrorsAndNoWarnings( report );
    }

    @Test
    void shouldNotImportWhenErrorIsTriggeredOnAllEntities()
        throws IOException
    {
        alwaysTrueErrorProgramRule();
        alwaysTrueErrorProgramEventProgramRule();
        ImportReport report = trackerImportService
            .importTracker( fromJson( "tracker/programrule/tei_enrollment_event_programevent.json" ) );

        assertAll(
            () -> assertHasError( report, E1300, ENROLLMENT_UID ),
            () -> assertHasError( report, E1300, EVENT_UID ),
            () -> assertHasError( report, E1300, PROGRAM_EVENT_UID ) );
    }

    @Test
    void shouldImportWithWarningWhenAWarningIsTriggeredOnEventInSameProgramStage()
        throws IOException
    {
        programStageWarningRule();
        TrackerImportParams params = fromJson( "tracker/tei_enrollment_event.json" );

        ImportReport importReport = trackerImportService.importTracker( params );

        assertNoErrors( importReport );
        assertEquals( 1, importReport.getValidationReport().getWarnings().size() );
    }

    @Test
    void testImportEventInProgramStageSuccessWithWarningRaised()
        throws IOException
    {
        assignProgramRule();
        TrackerImportParams params = fromJson( "tracker/tei_enrollment_event.json" );

        ImportReport importReport = trackerImportService.importTracker( params );

        assertHasOnlyWarnings( importReport.getValidationReport(),
            new Warning(
                "Generated by program rule (`ProgramRulC`) - DataElement `DATAEL00001` is being replaced in event `D9PbzJY8bJO`",
                E1308.name(), EVENT.name(), EVENT_UID ) );

        params = fromJson( "tracker/event_update_no_datavalue.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        importReport = trackerImportService.importTracker( params );

        assertHasOnlyWarnings( importReport.getValidationReport(),
            new Warning(
                "Generated by program rule (`ProgramRulC`) - DataElement `DATAEL00001` is being replaced in event `D9PbzJY8bJO`",
                E1308.name(), EVENT.name(), EVENT_UID ) );

        params = fromJson( "tracker/event_update_datavalue.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        importReport = trackerImportService.importTracker( params );

        assertHasOnlyWarnings( importReport.getValidationReport(),
            new Warning(
                "Generated by program rule (`ProgramRulC`) - DataElement `DATAEL00001` is being replaced in event `D9PbzJY8bJO`",
                E1308.name(), EVENT.name(), EVENT_UID ) );
    }

    private void createProgramRule( char uniqueCharacter, Program program, ProgramRuleActionType actionType )
    {
        ProgramRule programRuleA = createProgramRule( uniqueCharacter, program );
        programRuleA.setUid( "ProgramRul" + uniqueCharacter );
        programRuleService.addProgramRule( programRuleA );
        ProgramRuleAction programRuleActionShowError = createProgramRuleAction( 'A', programRuleA );
        programRuleActionShowError.setProgramRuleActionType( actionType );
        programRuleActionShowError.setContent( "CONTENT" );
        programRuleActionService.addProgramRuleAction( programRuleActionShowError );
        programRuleA.getProgramRuleActions().add( programRuleActionShowError );
        programRuleService.updateProgramRule( programRuleA );
    }

    private void alwaysTrueErrorProgramRule()
    {
        createProgramRule( 'A', program, ProgramRuleActionType.SHOWERROR );
    }

    private void onCompleteErrorProgramRule()
    {
        createProgramRule( 'B', program, ProgramRuleActionType.ERRORONCOMPLETE );
    }

    private void alwaysTrueWarningProgramRule()
    {
        createProgramRule( 'C', program, ProgramRuleActionType.SHOWWARNING );
    }

    private void alwaysTrueWarningProgramEventProgramRule()
    {
        createProgramRule( 'D', programWithoutRegistration, ProgramRuleActionType.SHOWWARNING );
    }

    private void alwaysTrueErrorProgramEventProgramRule()
    {
        createProgramRule( 'E', programWithoutRegistration, ProgramRuleActionType.SHOWERROR );
    }

    private void assignProgramRule()
    {
        ProgramRule programRuleC = createProgramRule( 'F', program );
        programRuleC.setUid( "ProgramRulC" );
        programRuleC.setCondition( "d2:daysBetween('2019-01-28', d2:lastEventDate('ProgramRuleVariableA')) < 5" );
        programRuleService.addProgramRule( programRuleC );
        ProgramRuleAction programRuleActionAssign = createProgramRuleAction( 'C', programRuleC );
        programRuleActionAssign.setProgramRuleActionType( ProgramRuleActionType.ASSIGN );
        programRuleActionAssign.setData( "#{ProgramRuleVariableA}" );
        programRuleActionAssign.setDataElement( dataElement1 );
        programRuleActionService.addProgramRuleAction( programRuleActionAssign );
        programRuleC.getProgramRuleActions().add( programRuleActionAssign );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void programStageWarningRule()
    {
        ProgramRule programRuleB = createProgramRule( 'G', program );
        programRuleB.setProgramStage( programStage );
        programRuleService.addProgramRule( programRuleB );

        ProgramRuleAction programRuleActionShowWarningForProgramStage = createProgramRuleAction( 'B', programRuleB );
        programRuleActionShowWarningForProgramStage.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleActionShowWarningForProgramStage.setContent( "PROGRAM STAGE WARNING" );
        programRuleActionService.addProgramRuleAction( programRuleActionShowWarningForProgramStage );

        programRuleB.getProgramRuleActions().add( programRuleActionShowWarningForProgramStage );
        programRuleService.updateProgramRule( programRuleB );
    }
}
