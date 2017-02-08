package org.hisp.dhis.dxf2.metadata.objectbundle;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.validation.ValidationRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleServiceProgramTest
    extends DhisSpringTest
{
    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Override
    protected void setUpTest() throws Exception
    {
        renderService = _renderService;
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
}