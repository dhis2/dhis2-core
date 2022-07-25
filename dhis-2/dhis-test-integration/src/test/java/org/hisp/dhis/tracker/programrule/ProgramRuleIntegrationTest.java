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

import static org.hisp.dhis.tracker.Assertions.assertHasWarning;
import static org.hisp.dhis.tracker.Assertions.assertNoImportErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

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
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerWarningReport;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Order( 1 )
class ProgramRuleIntegrationTest extends TrackerTest
{
    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Override
    public void initTest()
        throws IOException
    {
        ObjectBundle bundle = setUpMetadata( "tracker/simple_metadata.json" );
        Program program = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class, "BFcipDERJnf" );
        Program programWithoutRegistration = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class,
            "BFcipDERJne" );
        DataElement dataElement1 = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00001" );
        DataElement dataElement2 = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00002" );
        ProgramStage programStage = bundle.getPreheat().get( PreheatIdentifier.UID, ProgramStage.class, "NpsdDv6kKSO" );
        ProgramRuleVariable programRuleVariable = createProgramRuleVariableWithDataElement( 'A', program,
            dataElement2 );
        programRuleVariableService.addProgramRuleVariable( programRuleVariable );
        ProgramRule programRuleA = createProgramRule( 'A', program );
        programRuleA.setUid( "ProgramRule" );
        programRuleService.addProgramRule( programRuleA );
        ProgramRule programRuleC = createProgramRule( 'C', program );
        programRuleC.setUid( "ProgramRulC" );
        programRuleService.addProgramRule( programRuleC );
        ProgramRule programRuleWithoutRegistration = createProgramRule( 'W', programWithoutRegistration );
        programRuleService.addProgramRule( programRuleWithoutRegistration );
        ProgramRule programRuleB = createProgramRule( 'B', program );
        programRuleB.setProgramStage( programStage );
        programRuleService.addProgramRule( programRuleB );
        ProgramRuleAction programRuleActionShowWarning = createProgramRuleAction( 'A', programRuleA );
        programRuleActionShowWarning.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleActionShowWarning.setContent( "WARNING" );
        programRuleActionService.addProgramRuleAction( programRuleActionShowWarning );
        ProgramRuleAction anotherProgramRuleActionShowWarning = createProgramRuleAction( 'D',
            programRuleWithoutRegistration );
        anotherProgramRuleActionShowWarning.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        anotherProgramRuleActionShowWarning.setContent( "WARNING" );
        programRuleActionService.addProgramRuleAction( anotherProgramRuleActionShowWarning );
        ProgramRuleAction programRuleActionAssign = createProgramRuleAction( 'C', programRuleC );
        programRuleActionAssign.setProgramRuleActionType( ProgramRuleActionType.ASSIGN );
        programRuleActionAssign.setData( "#{ProgramRuleVariableA}" );
        programRuleActionAssign.setDataElement( dataElement1 );
        programRuleActionService.addProgramRuleAction( programRuleActionAssign );
        ProgramRuleAction programRuleActionShowWarningForProgramStage = createProgramRuleAction( 'B', programRuleB );
        programRuleActionShowWarningForProgramStage.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleActionShowWarningForProgramStage.setContent( "PROGRAM STAGE WARNING" );
        programRuleActionService.addProgramRuleAction( programRuleActionShowWarningForProgramStage );
        programRuleA.getProgramRuleActions().add( programRuleActionShowWarning );
        programRuleService.updateProgramRule( programRuleA );
        programRuleC.getProgramRuleActions().add( programRuleActionAssign );
        programRuleService.updateProgramRule( programRuleC );
        programRuleWithoutRegistration.getProgramRuleActions().add( anotherProgramRuleActionShowWarning );
        programRuleService.updateProgramRule( programRuleWithoutRegistration );
        programRuleB.getProgramRuleActions().add( programRuleActionShowWarningForProgramStage );
        programRuleService.updateProgramRule( programRuleB );

        injectAdminUser();
    }

    @Test
    void testImportProgramEventSuccessWithWarningRaised()
        throws IOException
    {
        TrackerImportReport trackerImportReport = trackerImportService
            .importTracker( fromJson( "tracker/program_event.json" ) );

        assertNoImportErrors( trackerImportReport );
        assertEquals( 1, trackerImportReport.getValidationReport().getWarnings().size() );
    }

    @Test
    void testImportEnrollmentSuccessWithWarningRaised()
        throws IOException
    {
        TrackerImportReport trackerImportTeiReport = trackerImportService
            .importTracker( fromJson( "tracker/single_tei.json" ) );
        assertNoImportErrors( trackerImportTeiReport );

        TrackerImportReport trackerImportEnrollmentReport = trackerImportService
            .importTracker( fromJson( "tracker/single_enrollment.json" ) );

        assertNoImportErrors( trackerImportEnrollmentReport );
        assertEquals( 1, trackerImportEnrollmentReport.getValidationReport().getWarnings().size() );
    }

    @Test
    void testImportEventInProgramStageSuccessWithWarningRaised()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/tei_enrollment_event.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertNoImportErrors( trackerImportReport );
        List<TrackerWarningReport> warningReports = trackerImportReport.getValidationReport().getWarnings();
        assertEquals( 4, warningReports.size() );
        assertEquals( 3,
            warningReports.stream().filter( w -> w.getTrackerType().equals( TrackerType.EVENT ) ).count() );
        assertEquals( 1,
            warningReports.stream().filter( w -> w.getTrackerType().equals( TrackerType.ENROLLMENT ) ).count() );

        params = fromJson( "tracker/event_update_no_datavalue.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        trackerImportReport = trackerImportService.importTracker( params );

        assertNoImportErrors( trackerImportReport );
        assertHasWarning( trackerImportReport.getValidationReport(), TrackerErrorCode.E1308 );
        assertEquals(
            "Generated by program rule (`ProgramRulC`) - DataElement `DATAEL00001` is being replaced in event `EVENT123456`",
            warningReports.get( 0 ).getWarningMessage() );
        assertEquals( 3, warningReports.size() );

        params = fromJson( "tracker/event_update_datavalue.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        trackerImportReport = trackerImportService.importTracker( params );

        assertNoImportErrors( trackerImportReport );
        warningReports = trackerImportReport.getValidationReport().getWarnings();
        assertHasWarning( trackerImportReport.getValidationReport(), TrackerErrorCode.E1308 );
        assertEquals(
            "Generated by program rule (`ProgramRulC`) - DataElement `DATAEL00001` is being replaced in event `EVENT123456`",
            warningReports.get( 0 ).getWarningMessage() );
        assertEquals( 3, warningReports.size() );
    }
}
