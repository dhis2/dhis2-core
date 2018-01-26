package org.hisp.dhis.dxf2.events.security;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class EnrollmentSecurityTests
    extends DhisSpringTest
{
    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private IdentifiableObjectManager manager;

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

    @Override
    protected void setUpTest()
    {
        userService = _userService;

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );
        manager.save( organisationUnitA );
        manager.save( organisationUnitB );

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

        manager.update( programStageA );
        manager.update( programStageB );
        manager.update( programA );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );

        maleA = createTrackedEntityInstance( 'A', organisationUnitA );
        maleB = createTrackedEntityInstance( 'B', organisationUnitB );
        femaleA = createTrackedEntityInstance( 'C', organisationUnitA );
        femaleB = createTrackedEntityInstance( 'D', organisationUnitB );

        maleA.setTrackedEntityType( trackedEntityType );
        maleB.setTrackedEntityType( trackedEntityType );
        femaleA.setTrackedEntityType( trackedEntityType );
        femaleB.setTrackedEntityType( trackedEntityType );

        manager.save( maleA );
        manager.save( maleB );
        manager.save( femaleA );
        manager.save( femaleB );
    }

    /**
     * program = DATA READ/WRITE
     * orgUnit = Accessible
     * status = SUCCESS
     */
    @Test
    public void testUserWithDataReadWrite()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        assertEquals( ImportStatus.SUCCESS, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.SUCCESS, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.SUCCESS, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.SUCCESS, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );
    }

    /**
     * program = DATA READ/WRITE
     * orgUnit = Not Accessible
     * status = ERROR
     */
    @Test
    public void testUserWithDataReadWriteNoOrgUnit()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );

        User user = createUser( "user1" );

        injectSecurityContext( user );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );
    }

    /**
     * program = DATA READ
     * orgUnit = Accessible
     * status = ERROR
     */
    @Test
    public void testUserWithDataReadOrgUnit()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );
    }

    /**
     * program =
     * orgUnit = Accessible
     * status = ERROR
     */
    @Test
    public void testUserNoDataAccessOrgUnit()
    {
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( programA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );
    }

    /**
     * program =
     * orgUnit = Not Accessible
     * status = ERROR
     */
    @Test
    public void testUserNoDataAccessNoOrgUnit()
    {
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( programA );

        User user = createUser( "user1" );

        injectSecurityContext( user );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleA.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );

        assertEquals( ImportStatus.ERROR, enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), femaleB.getUid() ), ImportOptions.getDefaultImportOptions() ).getStatus() );
    }

    /**
     * program = DATA READ/WRITE
     * orgUnit = Accessible
     * status = SUCCESS
     */
    @Test
    public void testGetEnrollmentUserWithDataReadWrite()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        Enrollment enrollment = enrollmentService.getEnrollment( importSummary.getReference() );
        assertNotNull( enrollment );
        assertEquals( enrollment.getEnrollment(), importSummary.getReference() );
    }

    /**
     * program = DATA READ
     * orgUnit = Accessible
     * status = SUCCESS
     */
    @Test
    public void testGetEnrollmentUserWithDataRead()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        Enrollment enrollment = enrollmentService.getEnrollment( importSummary.getReference() );
        assertNotNull( enrollment );
        assertEquals( enrollment.getEnrollment(), importSummary.getReference() );
    }

    /**
     * program = DATA READ
     * orgUnit = Not Accessible
     * status = ERROR
     */
    @Test( expected = IllegalQueryException.class )
    public void testGetEnrollmentUserWithDataReadNoOrgUnit()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );

        User user = createUser( "user1" );

        injectSecurityContext( user );

        enrollmentService.getEnrollment( importSummary.getReference() );
    }

    /**
     * program = DATA READ/WRITE
     * orgUnit = Not Accessible
     * status = ERROR
     */
    @Test( expected = IllegalQueryException.class )
    public void testGetEnrollmentUserWithDataReadWriteNoOrgUnit()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );

        User user = createUser( "user1" );

        injectSecurityContext( user );

        enrollmentService.getEnrollment( importSummary.getReference() );
    }

    /**
     * program =
     * orgUnit = Accessible
     * status = ERROR
     */
    @Test( expected = IllegalQueryException.class )
    public void testGetEnrollmentUserWithNoDataReadWriteOrgUnit()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( programA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        enrollmentService.getEnrollment( importSummary.getReference() );
    }

    private Enrollment createEnrollment( String program, String person )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setOrgUnit( organisationUnitA.getUid() );
        enrollment.setProgram( program );
        enrollment.setTrackedEntityInstance( person );
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );

        return enrollment;
    }
}
