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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.validation.ValidationRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class ObjectBundleServiceProgramTest extends TransactionalIntegrationTest
{

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Override
    protected void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    void testCreateSimpleProgramNoReg()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/program_noreg.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<ValidationRule> validationRules = manager.getAll( ValidationRule.class );
        List<Program> programs = manager.getAll( Program.class );
        List<ProgramStage> programStages = manager.getAll( ProgramStage.class );
        List<ProgramStageDataElement> programStageDataElements = manager.getAll( ProgramStageDataElement.class );
        assertFalse( dataSets.isEmpty() );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 1, validationRules.size() );
        assertEquals( 1, programs.size() );
        assertEquals( 1, programStages.size() );
        assertEquals( 3, programStageDataElements.size() );
        ProgramStage programStage = programStages.get( 0 );
        assertEquals( 3, programStage.getProgramStageDataElements().size() );
    }

    @Test
    void testCreateSimpleProgramWithSectionsNoReg()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_noreg_sections.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<ValidationRule> validationRules = manager.getAll( ValidationRule.class );
        List<Program> programs = manager.getAll( Program.class );
        List<ProgramStage> programStages = manager.getAll( ProgramStage.class );
        List<ProgramStageDataElement> programStageDataElements = manager.getAll( ProgramStageDataElement.class );
        List<ProgramStageSection> programStageSections = manager.getAll( ProgramStageSection.class );
        assertFalse( dataSets.isEmpty() );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 1, validationRules.size() );
        assertEquals( 1, programs.size() );
        assertEquals( 1, programStages.size() );
        assertEquals( 3, programStageDataElements.size() );
        assertEquals( 2, programStageSections.size() );
        ProgramStage programStage = programStages.get( 0 );
        assertEquals( 3, programStage.getProgramStageDataElements().size() );
        assertEquals( 2, programStage.getProgramStageSections().size() );
    }

    @Test
    void testCreateSimpleProgramReg()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/program_reg1.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<Program> programs = manager.getAll( Program.class );
        List<ProgramStage> programStages = manager.getAll( ProgramStage.class );
        List<ProgramStageDataElement> programStageDataElements = manager.getAll( ProgramStageDataElement.class );
        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = manager
            .getAll( ProgramTrackedEntityAttribute.class );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 1, programs.size() );
        assertEquals( 2, programStages.size() );
        assertEquals( 4, programStageDataElements.size() );
        assertEquals( 2, programTrackedEntityAttributes.size() );
    }

    @Test
    void testProgramRuleCreation()
        throws IOException
    {
        createProgramRuleMetadata();
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata1 = renderService.fromMetadata(
            new ClassPathResource( "dxf2/duplicate_program_rule.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params1 = new ObjectBundleParams();
        params1.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params1.setImportStrategy( ImportStrategy.CREATE );
        params1.setObjects( metadata1 );
        ObjectBundle bundle1 = objectBundleService.create( params1 );
        ObjectBundleValidationReport validate1 = objectBundleValidationService.validate( bundle1 );
        assertFalse( validate1.hasErrorReports() );
        assertEquals( 0, validate1.getErrorReportsCount() );
    }

    @Test
    void testProgramRuleUpdate()
        throws IOException
    {
        createProgramRuleMetadata();
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata1 = renderService.fromMetadata(
            new ClassPathResource( "dxf2/existing_program_rule.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params1 = new ObjectBundleParams();
        params1.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params1.setImportStrategy( ImportStrategy.UPDATE );
        params1.setObjects( metadata1 );
        ObjectBundle bundle1 = objectBundleService.create( params1 );
        ObjectBundleValidationReport validate1 = objectBundleValidationService.validate( bundle1 );
        assertFalse( validate1.hasErrorReports() );
    }

    @Test
    void testInvalidProgramRuleAction()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_program_and_program_rules_with_invalid_ruleActions.json" )
                .getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertTrue( validate.hasErrorReports() );
        assertTrue( validate.hasErrorReport( report -> report.getErrorCode() == ErrorCode.E4047 ) );
    }

    @Test
    void testValidProgramRuleAction()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_program_and_program_rules_with_valid_ruleActions.json" )
                .getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertFalse( validate.hasErrorReports() );
    }

    @Test
    void testCreateSimpleProgramRegNextScheduleDate()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_reg1_valid_nextschedule.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_reg1_invalid_nextschedule.json" ).getInputStream(),
            RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertTrue( validate.hasErrorReports() );
    }

    @Test
    void testValidateTrackedEntityAttributeSecurityNotShared()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_tea_not_shared.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_tea_update.json" ).getInputStream(),
            RenderFormat.JSON );
        String[] testAuths = { "F_DATAELEMENT_PUBLIC_ADD", "F_PROGRAM_PUBLIC_ADD", "F_ORGANISATIONUNIT_ADD",
            "F_ORGANISATIONUNITLEVEL_UPDATE", "F_TRACKED_ENTITY_ATTRIBUTE_PUBLIC_ADD", "F_USER_ADD",
            "F_PROGRAMSTAGE_ADD", "F_TRACKED_ENTITY_ADD", "F_TRACKED_ENTITY_UPDATE",
            "F_USER_ADD_WITHIN_MANAGED_GROUP" };
        User testUser = createUserWithAuth( "A", testAuths );

        injectSecurityContext( testUser );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setUser( testUser );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertTrue( validate.hasErrorReports() );
    }

    @Test
    void testValidateTrackedEntityAttributeSecurityShared()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_tea_not_shared.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        String[] testAuths = { "F_DATAELEMENT_PUBLIC_ADD", "F_PROGRAM_PUBLIC_ADD", "F_ORGANISATIONUNIT_ADD",
            "F_ORGANISATIONUNITLEVEL_UPDATE", "F_TRACKED_ENTITY_ATTRIBUTE_PUBLIC_ADD", "F_USER_ADD",
            "F_PROGRAMSTAGE_ADD", "F_TRACKED_ENTITY_ADD", "F_TRACKED_ENTITY_UPDATE",
            "F_USER_ADD_WITHIN_MANAGED_GROUP" };
        User testUser = createUserWithAuth( "A", testAuths );

        TrackedEntityAttribute tea1 = manager.get( TrackedEntityAttribute.class, "cpaMZredRXb" );
        TrackedEntityAttribute tea2 = manager.get( TrackedEntityAttribute.class, "QhEcRpLZwMb" );
        UserAccess userAccess1 = new UserAccess( testUser, "rw------" );
        tea1.getSharing().addUserAccess( userAccess1 );
        UserAccess userAccess2 = new UserAccess( testUser, "rw------" );
        tea2.getSharing().addUserAccess( userAccess2 );
        manager.update( tea1 );
        manager.update( tea2 );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_tea_update.json" ).getInputStream(),
            RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        params.setUser( testUser );
        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertFalse( validate.hasErrorReports() );
    }

    private void createProgramRuleMetadata()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_program_and_programrules.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        validate.forEachErrorReport( System.out::println );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
    }
}
