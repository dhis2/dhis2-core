package org.hisp.dhis.tracker.programrule;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.*;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

public class ProgramRuleIntegrationTest
    extends IntegrationTestBase
{
    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

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
        assertTrue( validationReport.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        Program program = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class, "BFcipDERJnf" );

        ProgramRule programRule = createProgramRule( 'A', program );
        programRuleService.addProgramRule( programRule );

        ProgramRuleAction programRuleActionWarning = createProgramRuleAction( 'A', programRule );
        programRuleActionWarning.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleActionWarning.setContent( "WARNING" );
        programRuleActionService.addProgramRuleAction( programRuleActionWarning );

        programRule.getProgramRuleActions().add( programRuleActionWarning );
        programRuleService.updateProgramRule( programRule );

        userA = userService.getUser( "M5zQapPyTZI" );
    }

    @Test
    public void testImportSuccessWithWaringRaised() throws IOException {

        InputStream inputStream = new ClassPathResource( "tracker/single_tei.json" ).getInputStream();

        TrackerBundleParams params = renderService.fromJson( inputStream, TrackerBundleParams.class );
        params.setUser( userA );
        TrackerImportReport trackerImportTeiReport = trackerImportService.importTracker( build( params ) );

        TrackerBundleParams enrollmentParams = renderService
            .fromJson( new ClassPathResource( "tracker/single_enrollment.json" ).getInputStream(),
                TrackerBundleParams.class );
        enrollmentParams.setUser( userA );
        TrackerImportReport trackerImportEnrollmentReport = trackerImportService
            .importTracker( build( enrollmentParams ) );

        assertNotNull( trackerImportTeiReport );
        assertEquals( TrackerStatus.OK, trackerImportTeiReport.getStatus() );

        assertNotNull( trackerImportEnrollmentReport );
        assertEquals( TrackerStatus.OK, trackerImportEnrollmentReport.getStatus() );
        assertFalse( trackerImportEnrollmentReport.getTrackerValidationReport().getWarningReports().isEmpty() );
    }

    private TrackerImportParams build(TrackerBundleParams params) {
        // @formatter:off
        return TrackerImportParams.builder()
                .user( params.getUser() )
                .importMode( params.getImportMode() )
                .importStrategy( params.getImportStrategy() )
                .skipPatternValidation( true )
                .identifiers( params.getIdentifiers() )
                .atomicMode( params.getAtomicMode() )
                .flushMode( params.getFlushMode() )
                .validationMode( params.getValidationMode() )
                .reportMode( params.getReportMode() )
                .trackedEntities( params.getTrackedEntities() )
                .enrollments( params.getEnrollments() )
                .events( params.getEvents() )
                .relationships( params.getRelationships() )
                .build();
        // @formatter:on
    }
}
