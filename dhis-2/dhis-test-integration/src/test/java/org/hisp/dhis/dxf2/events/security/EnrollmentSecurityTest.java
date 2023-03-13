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
package org.hisp.dhis.dxf2.events.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.EnrollmentParams;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.enrollment.Enrollments;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class EnrollmentSecurityTest extends TransactionalIntegrationTest
{

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private AttributeService attributeService;

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
        User admin = createAndInjectAdminUser();
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );
        manager.save( organisationUnitA );
        manager.save( organisationUnitB );
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.save( trackedEntityType, false );
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
        programA.setTrackedEntityType( trackedEntityType );
        programA.getSharing().setOwner( admin );
        manager.save( programA );
        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        programStageA.getProgramStageDataElements().add( programStageDataElement );
        programStageA.setProgram( programA );
        programStageA.getSharing().setOwner( admin );
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
        maleA = createTrackedEntityInstance( organisationUnitA );
        maleB = createTrackedEntityInstance( organisationUnitB );
        femaleA = createTrackedEntityInstance( organisationUnitA );
        femaleB = createTrackedEntityInstance( organisationUnitB );
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
     * program = FULL access orgUnit = Accessible status = SUCCESS
     */
    @Test
    void testUserWithDataReadWrite()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.FULL );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        userService.addUser( user );
        injectSecurityContext( user );
        assertEquals( ImportStatus.SUCCESS,
            enrollmentService.addEnrollment( createEnrollment( programA.getUid(), maleA.getUid() ),
                ImportOptions.getDefaultImportOptions() ).getStatus() );
        assertEquals( ImportStatus.SUCCESS,
            enrollmentService.addEnrollment( createEnrollment( programA.getUid(), maleB.getUid() ),
                ImportOptions.getDefaultImportOptions() ).getStatus() );
        assertEquals( ImportStatus.SUCCESS,
            enrollmentService.addEnrollment( createEnrollment( programA.getUid(), femaleA.getUid() ),
                ImportOptions.getDefaultImportOptions() ).getStatus() );
        assertEquals( ImportStatus.SUCCESS,
            enrollmentService.addEnrollment( createEnrollment( programA.getUid(), femaleB.getUid() ),
                ImportOptions.getDefaultImportOptions() ).getStatus() );
    }

    /**
     * program = DATA READ/WRITE orgUnit = Not Accessible status = ERROR
     */
    @Test
    void testUserWithDataReadWriteNoOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        ImportSummary importReport = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ),
            ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.ERROR, importReport.getStatus() );
        assertEquals( "Program can not be null", importReport.getDescription() );
    }

    /**
     * program = DATA READ orgUnit = Accessible status = ERROR
     */
    @Test
    void testUserWithDataReadOrgUnit()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        ImportSummary importReport = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ),
            ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.ERROR, importReport.getStatus() );
        assertEquals( "Program can not be null", importReport.getDescription() );
    }

    /**
     * program = orgUnit = Accessible status = ERROR
     */
    @Test
    void testUserNoDataAccessOrgUnit()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        ImportSummary importReport = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ),
            ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.ERROR, importReport.getStatus() );
        assertEquals( "Program can not be null", importReport.getDescription() );
    }

    /**
     * program = orgUnit = Not Accessible status = ERROR
     */
    @Test
    void testUserNoDataAccessNoOrgUnit()
    {
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( programA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        ImportSummary importReport = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ),
            ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.ERROR, importReport.getStatus() );
        assertEquals( "Program can not be null", importReport.getDescription() );
    }

    /**
     * program = DATA READ/WRITE orgUnit = Accessible status = SUCCESS
     */
    @Test
    void testGetEnrollmentUserWithDataReadWrite()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        Enrollment enrollment = enrollmentService.getEnrollment( importSummary.getReference(),
            EnrollmentParams.FALSE );
        assertNotNull( enrollment );
        assertEquals( enrollment.getEnrollment(), importSummary.getReference() );
    }

    /**
     * program = DATA READ orgUnit = Accessible status = SUCCESS
     */
    @Test
    void testGetEnrollmentUserWithDataRead()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        Enrollment enrollment = enrollmentService.getEnrollment( importSummary.getReference(),
            EnrollmentParams.FALSE );
        assertNotNull( enrollment );
        assertEquals( enrollment.getEnrollment(), importSummary.getReference() );
    }

    /**
     * program = DATA READ orgUnit = Accessible in search scope status = SUCCESS
     */
    @Test
    void testGetEnrollmentsInSearchScopeForUser()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );
        User user = createUserWithAuth( "user1" );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnitB ) );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );
        user.setDataViewOrganisationUnits( Sets.newHashSet( organisationUnitB ) );
        injectSecurityContext( user );
        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
        params.setProgram( programA );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ACCESSIBLE );
        params.setUser( user );
        Enrollments enrollments = enrollmentService.getEnrollments( params );
        assertNotNull( enrollments );
        assertNotNull( enrollments.getEnrollments() );
        assertEquals( 1, enrollments.getEnrollments().size() );
        assertEquals( importSummary.getReference(), enrollments.getEnrollments().get( 0 ).getEnrollment() );
    }

    /**
     * program = DATA READ orgUnit = Not Accessible status = ERROR
     */
    @Test
    void testGetEnrollmentUserWithDataReadNoOrgUnit()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollment( importSummary.getReference(), EnrollmentParams.FALSE ) );
    }

    /**
     * program = DATA READ/WRITE orgUnit = Not Accessible status = ERROR
     */
    @Test
    void testGetEnrollmentUserWithDataReadWriteNoOrgUnit()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DATA_READ );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollment( importSummary.getReference(), EnrollmentParams.FALSE ) );
    }

    /**
     * program = orgUnit = Accessible status = ERROR
     */
    @Test
    void testGetEnrollmentUserWithNoDataReadWriteOrgUnit()
    {
        ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollment( programA.getUid(), maleA.getUid() ), ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( programA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        assertThrows( IllegalQueryException.class,
            () -> enrollmentService.getEnrollment( importSummary.getReference(), EnrollmentParams.FALSE ) );
    }

    @Test
    void testAddEnrollmentToOrgUnitWithoutProgramAccess()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( programA );
        Enrollment en = createEnrollment( programA.getUid(), maleA.getUid() );
        en.setOrgUnit( organisationUnitB.getUid() );
        ImportSummary importSummary = enrollmentService.addEnrollment( en, ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
        assertEquals( "Program is not assigned to this Organisation Unit: " + organisationUnitB.getUid(),
            importSummary.getDescription() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        programA.getOrganisationUnits().add( organisationUnitB );
        manager.updateNoAcl( programA );
        importSummary = enrollmentService.addEnrollment( en, ImportOptions.getDefaultImportOptions() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    void testAddEnrollmentWithOrgUnitIdSchemeToOrgUnitWithoutProgramAccess()
    {
        programA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.updateNoAcl( programA );
        Enrollment en = createEnrollment( programA.getUid(), maleA.getUid() );
        Attribute attribute = new Attribute();
        attribute.setUnique( true );
        attribute.setUid( "D1DDOl5hTsL" );
        attribute.setValueType( ValueType.NUMBER );
        attribute.setOrganisationUnitAttribute( true );
        attribute.setName( "OrgUnitAttribute" );
        attributeService.addAttribute( attribute );
        AttributeValue av = new AttributeValue();
        av.setAttribute( attribute );
        av.setValue( "1025" );
        organisationUnitB.setAttributeValues( Collections.singleton( av ) );
        manager.updateNoAcl( organisationUnitB );
        en.setOrgUnit( av.getValue() );
        ImportOptions importOptions = new ImportOptions();
        importOptions.getIdSchemes().setOrgUnitIdScheme( "ATTRIBUTE" );
        importOptions.getIdSchemes().getOrgUnitIdScheme().setAttribute( "D1DDOl5hTsL" );
        ImportSummary importSummary = enrollmentService.addEnrollment( en, importOptions );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
        assertEquals( "Program is not assigned to this Organisation Unit: " + av.getValue(),
            importSummary.getDescription() );
        programA.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        programA.getOrganisationUnits().add( organisationUnitB );
        manager.updateNoAcl( programA );
        importSummary = enrollmentService.addEnrollment( en, importOptions );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
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
