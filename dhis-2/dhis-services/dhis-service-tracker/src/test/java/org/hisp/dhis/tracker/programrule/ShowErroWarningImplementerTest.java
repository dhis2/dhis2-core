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

package org.hisp.dhis.tracker.programrule;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.validation.AbstractImportValidationTest;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;

import static org.junit.Assert.*;

public class ShowErroWarningImplementerTest
    extends AbstractImportValidationTest
{

    private final static String CONTENT = "SHOW ERROR DATA";

    private final static String DATA = "2 + 2";

    private final static String EVALUATED_DATA = "4.0";

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ShowWarningOnCompleteImplementer warningOnCompleteImplementer;

    @Autowired
    private ShowErrorOnCompleteImplementer errorOnCompleteImplementer;

    @Autowired
    private ShowErrorImplementer errorImplementer;

    @Autowired
    private ShowWarningImplementer warningImplementer;

    private List<TrackerBundle> trackerBundles;

    @Override
    protected void setUpTest()
        throws IOException
    {
        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "tracker/event_metadata.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertTrue( validationReport.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        Program program = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class, "BFcipDERJne" );

        ProgramRule programRule = createProgramRule( 'B',
            program );
        programRule.setCondition( "true" );
        programRuleService.addProgramRule( programRule );
        ProgramRuleAction programRuleActionError = createProgramRuleAction( 'A', programRule );
        programRuleActionError.setProgramRuleActionType( ProgramRuleActionType.SHOWERROR );

        programRuleActionError.setContent( CONTENT );
        programRuleActionError.setData( DATA );
        programRuleActionService.addProgramRuleAction( programRuleActionError );

        ProgramRuleAction programRuleActionErrorOnComplete = createProgramRuleAction( 'B', programRule );
        programRuleActionErrorOnComplete.setProgramRuleActionType( ProgramRuleActionType.ERRORONCOMPLETE );

        programRuleActionErrorOnComplete.setContent( CONTENT );
        programRuleActionErrorOnComplete.setData( DATA );
        programRuleActionService.addProgramRuleAction( programRuleActionErrorOnComplete );

        ProgramRuleAction programRuleActionWarning = createProgramRuleAction( 'C', programRule );
        programRuleActionWarning.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );

        programRuleActionWarning.setContent( CONTENT );
        programRuleActionWarning.setData( DATA );
        programRuleActionService.addProgramRuleAction( programRuleActionWarning );

        ProgramRuleAction programRuleActionWarningOnComplete = createProgramRuleAction( 'D', programRule );
        programRuleActionWarningOnComplete.setProgramRuleActionType( ProgramRuleActionType.WARNINGONCOMPLETE );

        programRuleActionWarningOnComplete.setContent( CONTENT );
        programRuleActionWarningOnComplete.setData( DATA );
        programRuleActionService.addProgramRuleAction( programRuleActionWarningOnComplete );

        programRule.setProgramRuleActions( Sets.newHashSet( programRuleActionError, programRuleActionErrorOnComplete,
            programRuleActionWarning, programRuleActionWarningOnComplete ) );
        programRuleService.updateProgramRule( programRule );

        TrackerBundleParams bundleParams = createBundleFromJson( "tracker/event_events_and_enrollment.json" );

        trackerBundles = trackerBundleService.create( bundleParams );

        trackerBundles = trackerBundleService.runRuleEngine( trackerBundles );
    }

    @Test
    public void testValidateShowErrorRuleActionForEvents()
    {
        Map<String, List<String>> errors = errorImplementer.validateEvents( trackerBundles.get( 0 ) );

        assertErrors( errors );
    }

    @Test
    public void testValidateShowErrorRuleActionForEnrollment()
    {
        Map<String, List<String>> errors = errorImplementer.validateEnrollments( trackerBundles.get( 0 ) );

        assertErrors( errors );
    }

    @Test
    public void testValidateShowWarningRuleActionForEvents()
    {
        Map<String, List<String>> warnings = warningImplementer.validateEvents( trackerBundles.get( 0 ) );

        assertErrors( warnings );
    }

    @Test
    public void testValidateShowWarningRuleActionForEnrollment()
    {
        Map<String, List<String>> warnings = warningImplementer.validateEnrollments( trackerBundles.get( 0 ) );

        assertErrors( warnings );
    }

    @Test
    public void testValidateShowErrorOnCompleteRuleActionForEvents()
    {
        Map<String, List<String>> errors = errorOnCompleteImplementer.validateEvents( trackerBundles.get( 0 ) );

        assertErrors( errors );
    }

    @Test
    public void testValidateShowErrorOnCompleteRuleActionForEnrollment()
    {
        Map<String, List<String>> errors = errorOnCompleteImplementer.validateEnrollments( trackerBundles.get( 0 ) );

        assertErrors( errors );
    }

    @Test
    public void testValidateShowWarningOnCompleteRuleActionForEvents()
    {
        Map<String, List<String>> warnings = warningOnCompleteImplementer.validateEvents( trackerBundles.get( 0 ) );

        assertErrors( warnings );
    }

    @Test
    public void testValidateShowWarningOnCompleteRuleActionForEnrollment()
    {
        Map<String, List<String>> warnings = warningOnCompleteImplementer
            .validateEnrollments( trackerBundles.get( 0 ) );

        assertErrors( warnings );
    }

    public void assertErrors( Map<String, List<String>> errors )
    {
        assertFalse( errors.isEmpty() );

        errors.entrySet().stream()
            .forEach( e -> assertTrue( e.getValue().size() == 1 ) );

        errors
            .values()
            .stream()
            .flatMap( e -> e.stream() )
            .forEach( e -> assertTrue( e.equals( CONTENT + " " + EVALUATED_DATA ) ) );
    }
}