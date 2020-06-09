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
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.validation.AbstractImportValidationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;

import static org.junit.Assert.*;

public class SetMandatoryFieldImplementerTest extends AbstractImportValidationTest
{

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
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private SetMandatoryFieldImplementer implementerToTest;

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
        DataElement dataElement = bundle.getPreheat()
            .get( PreheatIdentifier.UID, DataElement.class, "DSKTW8qFP0z" );
        ProgramRuleVariable programRuleVariable = createProgramRuleVariable( 'A', program );
        programRuleVariable.setDataElement( dataElement );
        programRuleVariable.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableService.addProgramRuleVariable( programRuleVariable );

        ProgramRule programRule = createProgramRule( 'A',
            program );
        programRuleService.addProgramRule( programRule );

        ProgramRuleAction programRuleAction = createProgramRuleAction( 'A', programRule );
        programRuleAction.setProgramRuleActionType( ProgramRuleActionType.SETMANDATORYFIELD );
        programRuleActionService.addProgramRuleAction( programRuleAction );
        programRuleAction.setDataElement( dataElement );

        ProgramRule programRule2 = createProgramRule( 'B',
            program );
        programRule2.setCondition( "d2:hasValue(#{ProgramRuleVariableA})" );
        programRuleService.addProgramRule( programRule2 );
        ProgramRuleAction programRuleAction2 = createProgramRuleAction( 'B', programRule2 );
        programRuleAction2.setProgramRuleActionType( ProgramRuleActionType.SHOWERROR );
        programRuleAction2.setContent( "SHOW ERROR DATA" );
        programRuleActionService.addProgramRuleAction( programRuleAction2 );

        programRule.setProgramRuleActions( Sets.newHashSet( programRuleAction ) );
        programRuleService.updateProgramRule( programRule );
        programRule2.setProgramRuleActions( Sets.newHashSet( programRuleAction2 ) );
        programRuleService.updateProgramRule( programRule2 );
    }

    @Test
    public void testValidateOkMandatoryFieldsForEvents()
        throws IOException
    {
        TrackerBundleParams bundleParams = createBundleFromJson( "tracker/event_events_and_enrollment.json" );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundleParams );

        trackerBundles = trackerBundleService.runRuleEngine( trackerBundles );

        Map<String, List<String>> errors = implementerToTest.validateEvents( trackerBundles.get( 0 ) );

        assertFalse( errors.isEmpty() );

        errors.entrySet().stream()
            .forEach( e -> assertTrue( e.getValue().isEmpty() ) );
    }

    @Test
    public void testValidateWithErrorMandatoryFieldsForEvents()
        throws IOException
    {
        TrackerBundleParams bundleParams = createBundleFromJson(
            "tracker/event_events_and_enrollment_with_null_data_element.json" );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundleParams );

        trackerBundles = trackerBundleService.runRuleEngine( trackerBundles );

        Map<String, List<String>> errors = implementerToTest.validateEvents( trackerBundles.get( 0 ) );

        assertFalse( errors.isEmpty() );

        errors.entrySet().stream()
            .filter( e -> !e.getKey().equals( "D9PbzJY8bJO" ) )
            .forEach( e -> assertTrue( e.getValue().isEmpty() ) );

        errors.entrySet().stream()
            .filter( e -> e.getKey().equals( "D9PbzJY8bJO" ) )
            .forEach( e -> assertTrue( e.getValue().size() == 1 ) );
    }
}