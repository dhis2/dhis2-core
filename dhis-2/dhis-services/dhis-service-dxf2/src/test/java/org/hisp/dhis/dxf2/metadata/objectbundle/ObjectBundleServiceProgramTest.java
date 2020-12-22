package org.hisp.dhis.dxf2.metadata.objectbundle;

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


import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.validation.ValidationRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleServiceProgramTest
    extends TransactionalIntegrationTest
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
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest() throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    public void testCreateSimpleProgramNoReg() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_noreg.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        validate.getErrorReports().forEach( System.out::println );

        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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
    public void testCreateSimpleProgramWithSectionsNoReg() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_noreg_sections.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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
    public void testCreateSimpleProgramReg() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_reg1.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );

        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        validate.getErrorReports().forEach( System.out::println );

        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
        List<User> users = manager.getAll( User.class );
        List<Program> programs = manager.getAll( Program.class );
        List<ProgramStage> programStages = manager.getAll( ProgramStage.class );
        List<ProgramStageDataElement> programStageDataElements = manager.getAll( ProgramStageDataElement.class );
        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = manager.getAll( ProgramTrackedEntityAttribute.class );

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
    public void testProgramRuleCreation() throws IOException
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

        assertFalse( validate1.getErrorReports().isEmpty() );
        assertEquals( 1, validate1.getErrorReports().size() );

        List<ErrorReport> errorReports = validate1.getErrorReports();
        ErrorReport errorReport = errorReports.get( 0 );

        assertEquals( ErrorCode.E5003, errorReport.getErrorCode() );
    }

    @Test
    public void testProgramRuleUpdate() throws IOException
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

        assertTrue( validate1.getErrorReports().isEmpty() );
    }

    @Test
    public void testCreateSimpleProgramRegNextScheduleDate() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_reg1_valid_nextschedule.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );

        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_reg1_invalid_nextschedule.json" ).getInputStream(), RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );

        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        validate.getErrorReports().forEach( System.out::println );
        assertFalse( validate.getErrorReports().isEmpty() );
    }

    @Test
    public void testValidateTrackedEntityAttributeSecurityNotShared()
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
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_tea_update.json" ).getInputStream(), RenderFormat.JSON );

        String[] testAuths = {
            "F_DATAELEMENT_PUBLIC_ADD",
            "F_PROGRAM_PUBLIC_ADD",
            "F_ORGANISATIONUNIT_ADD",
            "F_ORGANISATIONUNITLEVEL_UPDATE",
            "F_TRACKED_ENTITY_ATTRIBUTE_PUBLIC_ADD",
            "F_USER_ADD",
            "F_PROGRAMSTAGE_ADD",
            "F_TRACKED_ENTITY_ADD",
            "F_TRACKED_ENTITY_UPDATE",
            "F_USER_ADD_WITHIN_MANAGED_GROUP"
        };

        User testUser = createUser( "A", testAuths );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setUser( testUser );

        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        validate.getErrorReports().forEach( System.out::println );
        assertFalse( validate.getErrorReports().isEmpty() );
    }

    @Test
    public void testValidateTrackedEntityAttributeSecurityShared()
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
        validate.getErrorReports().forEach( System.out::println );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        String[] testAuths = {
            "F_DATAELEMENT_PUBLIC_ADD",
            "F_PROGRAM_PUBLIC_ADD",
            "F_ORGANISATIONUNIT_ADD",
            "F_ORGANISATIONUNITLEVEL_UPDATE",
            "F_TRACKED_ENTITY_ATTRIBUTE_PUBLIC_ADD",
            "F_USER_ADD",
            "F_PROGRAMSTAGE_ADD",
            "F_TRACKED_ENTITY_ADD",
            "F_TRACKED_ENTITY_UPDATE",
            "F_USER_ADD_WITHIN_MANAGED_GROUP"
        };

        User testUser = createUser( "A", testAuths );

        TrackedEntityAttribute tea1 = manager.get( TrackedEntityAttribute.class, "cpaMZredRXb" );
        TrackedEntityAttribute tea2 = manager.get( TrackedEntityAttribute.class, "QhEcRpLZwMb" );

        UserAccess userAccess1 = new UserAccess( testUser, "rw------" );
        tea1.getSharing().addUserAccess( userAccess1 );

        UserAccess userAccess2 = new UserAccess( testUser, "rw------" );
        tea2.getSharing().addUserAccess( userAccess2 );

        manager.update( tea1 );
        manager.update( tea2 );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_tea_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        params.setUser( testUser );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        validate.getErrorReports().forEach( System.out::println );
        assertTrue( validate.getErrorReports().isEmpty() );
    }

    private void createProgramRuleMetadata() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_program_and_programrules.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        validate.getErrorReports().forEach( System.out::println );

        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );
    }
}