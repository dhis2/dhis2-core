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
package org.hisp.dhis.trackedentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityInstanceServiceTest
    extends IntegrationTestBase
{
    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private UserService _userService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    private ProgramStageInstance programStageInstance;

    private ProgramInstance programInstance;

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityInstance entityInstanceA1;

    private TrackedEntityInstance entityInstanceB1;

    private TrackedEntityInstance entityInstanceC1;

    private TrackedEntityInstance entityInstanceD1;

    private OrganisationUnit organisationUnit;

    private TrackedEntityType trackedEntityType;

    private TrackedEntityAttribute trackedEntityAttribute;

    private final static String ATTRIBUTE_VALUE = "Value";

    private User superUser;

    private User user;

    @Override
    public void setUpTest()
    {
        super.userService = _userService;

        this.superUser = preCreateInjectAdminUser();

        trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityAttribute = createTrackedEntityAttribute( 'H' );

        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        entityInstanceA1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceB1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceC1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceD1 = createTrackedEntityInstance( organisationUnit );

        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceB1.setTrackedEntityType( trackedEntityType );
        entityInstanceC1.setTrackedEntityType( trackedEntityType );
        entityInstanceD1.setTrackedEntityType( trackedEntityType );

        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        programStage = createProgramStage( 'A', program );
        programStage.setSortOrder( 1 );
        programStageService.saveProgramStage( programStage );
        program.setProgramStages( Set.of( programStage ) );
        programService.updateProgram( program );

        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );

        user = createUserWithAuth( "testUser" );
        user.setTeiSearchOrganisationUnits( Set.of( organisationUnit ) );
        injectSecurityContext( user );
    }

    @Test
    void testSaveTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    void testDeleteTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        TrackedEntityInstance teiB = entityInstanceService.getTrackedEntityInstance( idB );
        assertNotNull( teiA );
        assertNotNull( teiB );
        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );
        entityInstanceService.deleteTrackedEntityInstance( entityInstanceB1 );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );
    }

    @Test
    void testDeleteTrackedEntityInstanceAndLinkedEnrollmentsAndEvents()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        programInstance = createProgramInstance( program, entityInstanceA1, organisationUnit );
        long psIdA = programInstanceService.addProgramInstance( programInstance );
        programStageInstance = new ProgramStageInstance( programInstance, programStage );
        long psiIdA = programStageInstanceService.addProgramStageInstance( programStageInstance );
        programInstance.setProgramStageInstances( Set.of( programStageInstance ) );
        entityInstanceA1.setProgramInstances( Set.of( programInstance ) );
        programInstanceService.updateProgramInstance( programInstance );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );
        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        ProgramInstance psA = programInstanceService.getProgramInstance( psIdA );
        ProgramStageInstance psiA = programStageInstanceService.getProgramStageInstance( psiIdA );
        assertNotNull( teiA );
        assertNotNull( psA );
        assertNotNull( psiA );
        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( programInstanceService.getProgramInstance( psIdA ) );
        assertNull( programStageInstanceService.getProgramStageInstance( psiIdA ) );
    }

    @Test
    void testUpdateTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        entityInstanceA1.setName( "B" );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );
        assertEquals( "B", entityInstanceService.getTrackedEntityInstance( idA ).getName() );
    }

    @Test
    void testGetTrackedEntityInstanceById()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( idA ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    void testGetTrackedEntityInstanceByUid()
    {
        entityInstanceA1.setUid( "A1" );
        entityInstanceB1.setUid( "B1" );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( "A1" ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( "B1" ) );
    }

    @Test
    void testStoredByColumnForTrackedEntityInstance()
    {
        entityInstanceA1.setStoredBy( "test" );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( entityInstanceA1.getUid() );
        assertEquals( "test", tei.getStoredBy() );
    }

    @Test
    void testTrackedEntityAttributeFilter()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        User user = createAndAddUser( false, "attributeFilterUser", Set.of( organisationUnit ),
            Set.of( organisationUnit ) );
        injectSecurityContext( user );

        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();

        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setEntityInstance( entityInstanceA1 );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );

        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValue );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        Grid grid = entityInstanceService.getTrackedEntityInstancesGrid( params );

        assertEquals( 1, grid.getHeight() );
    }

    @Test
    void testTrackedEntityInstanceGridWithNoFilterableAttributes()
    {
        injectSecurityContext( superUser );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        assertThrows( IllegalArgumentException.class,
            () -> entityInstanceService.getTrackedEntityInstancesGrid( params ) );
    }

    @Test
    void testTrackedEntityInstanceGridWithNoDisplayAttributes()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( false );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        assertThrows( IllegalArgumentException.class,
            () -> entityInstanceService.getTrackedEntityInstancesGrid( params ) );
    }

    @Test
    void shouldOrderEntitiesByIdWhenParamCreatedAtSupplied()
    {
        entityInstanceA1.setCreated( DateTime.now().plusDays( 1 ).toDate() );
        entityInstanceB1.setCreated( DateTime.now().toDate() );
        entityInstanceC1.setCreated( DateTime.now().minusDays( 1 ).toDate() );
        entityInstanceD1.setCreated( DateTime.now().plusDays( 2 ).toDate() );

        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "createdAt", OrderParam.SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceB1.getId(), entityInstanceC1.getId(),
            entityInstanceD1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByIdWhenParamUpdatedAtSupplied()
    {
        entityInstanceA1.setLastUpdated( DateTime.now().plusDays( 1 ).toDate() );
        entityInstanceB1.setLastUpdated( DateTime.now().toDate() );
        entityInstanceC1.setLastUpdated( DateTime.now().minusDays( 1 ).toDate() );
        entityInstanceD1.setLastUpdated( DateTime.now().plusDays( 2 ).toDate() );

        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "updatedAt", OrderParam.SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceB1.getId(), entityInstanceC1.getId(),
            entityInstanceD1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByIdWhenParamTrackedEntitySupplied()
    {
        entityInstanceD1.setUid( "UID-D" );
        entityInstanceC1.setUid( "UID-C" );
        entityInstanceB1.setUid( "UID-B" );
        entityInstanceA1.setUid( "UID-A" );

        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "trackedEntity", OrderParam.SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceD1.getId(), entityInstanceC1.getId(), entityInstanceB1.getId(),
            entityInstanceA1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByLastUpdatedAtClientWhenParamUpdatedAtClientSupplied()
    {
        entityInstanceA1.setLastUpdatedAtClient( DateTime.now().plusDays( 1 ).toDate() );
        entityInstanceB1.setLastUpdatedAtClient( DateTime.now().toDate() );
        entityInstanceC1.setLastUpdatedAtClient( DateTime.now().minusDays( 1 ).toDate() );
        entityInstanceD1.setLastUpdatedAtClient( DateTime.now().plusDays( 2 ).toDate() );

        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "updatedAtClient", OrderParam.SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceD1.getId(), entityInstanceA1.getId(), entityInstanceB1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByEnrollmentDateWhenParamEnrolledAtSupplied()
    {
        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        programInstanceService.enrollTrackedEntityInstance( entityInstanceA1, program,
            DateTime.now().minusDays( 3 ).toDate(), new Date(), organisationUnit );
        programInstanceService.enrollTrackedEntityInstance( entityInstanceB1, program,
            DateTime.now().plusDays( 2 ).toDate(), new Date(), organisationUnit );
        programInstanceService.enrollTrackedEntityInstance( entityInstanceC1, program,
            DateTime.now().minusDays( 2 ).toDate(), new Date(), organisationUnit );
        programInstanceService.enrollTrackedEntityInstance( entityInstanceD1, program,
            DateTime.now().plusDays( 1 ).toDate(), new Date(), organisationUnit );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "enrolledAt", OrderParam.SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceD1.getId(), entityInstanceC1.getId(),
            entityInstanceA1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesAndKeepOrderOfParamsWhenMultipleStaticFieldsSupplied()
    {
        entityInstanceA1.setInactive( true );
        entityInstanceB1.setInactive( true );
        entityInstanceC1.setInactive( false );
        entityInstanceD1.setInactive( false );

        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        programInstanceService.enrollTrackedEntityInstance( entityInstanceA1, program,
            DateTime.now().minusDays( 1 ).toDate(), new Date(), organisationUnit );
        programInstanceService.enrollTrackedEntityInstance( entityInstanceB1, program,
            DateTime.now().plusDays( 2 ).toDate(), new Date(), organisationUnit );
        programInstanceService.enrollTrackedEntityInstance( entityInstanceC1, program,
            DateTime.now().minusDays( 2 ).toDate(), new Date(), organisationUnit );
        programInstanceService.enrollTrackedEntityInstance( entityInstanceD1, program,
            DateTime.now().plusDays( 1 ).toDate(), new Date(), organisationUnit );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "inactive", OrderParam.SortDirection.DESC ),
            new OrderParam( "enrolledAt", OrderParam.SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceA1.getId(), entityInstanceD1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByIdWhenNoOrderParamProvided()
    {
        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceB1.getId(), entityInstanceC1.getId(),
            entityInstanceD1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderByNonStaticFieldWhenNonStaticFieldProvided()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        saveTeav( entityInstanceA1, trackedEntityAttribute, "3-Attribute Value A1" );
        saveTeav( entityInstanceB1, trackedEntityAttribute, "1-Attribute Value B1" );
        saveTeav( entityInstanceC1, trackedEntityAttribute, "4-Attribute Value C1" );
        saveTeav( entityInstanceD1, trackedEntityAttribute, "2-Attribute Value D1" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), OrderParam.SortDirection.ASC ) ) );
        params.setAttributes( List.of( new QueryItem( trackedEntityAttribute ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceD1.getId(), entityInstanceA1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesAndKeepOrderOfParamsWhenStaticAndNonStaticFieldsSupplied()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        entityInstanceA1.setInactive( true );
        entityInstanceB1.setInactive( false );
        entityInstanceC1.setInactive( true );
        entityInstanceD1.setInactive( false );

        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        saveTeav( entityInstanceA1, trackedEntityAttribute, "2-Attribute Value" );
        saveTeav( entityInstanceB1, trackedEntityAttribute, "2-Attribute Value" );
        saveTeav( entityInstanceC1, trackedEntityAttribute, "1-Attribute Value" );
        saveTeav( entityInstanceD1, trackedEntityAttribute, "1-Attribute Value" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), OrderParam.SortDirection.DESC ),
            new OrderParam( "inactive", OrderParam.SortDirection.ASC ) ) );
        params.setAttributes( List.of( new QueryItem( trackedEntityAttribute ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceA1.getId(), entityInstanceD1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByAttributeDescendingWhenAttributeDescendingProvided()
    {
        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        saveTeav( entityInstanceA1, trackedEntityAttribute, "A" );
        saveTeav( entityInstanceB1, trackedEntityAttribute, "D" );
        saveTeav( entityInstanceC1, trackedEntityAttribute, "C" );
        saveTeav( entityInstanceD1, trackedEntityAttribute, "B" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), OrderParam.SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceC1.getId(), entityInstanceD1.getId(),
            entityInstanceA1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByAttributeAscendingWhenAttributeAscendingProvided()
    {
        List.of( entityInstanceA1, entityInstanceB1, entityInstanceC1, entityInstanceD1 )
            .forEach( entityInstanceService::addTrackedEntityInstance );

        saveTeav( entityInstanceA1, trackedEntityAttribute, "A" );
        saveTeav( entityInstanceB1, trackedEntityAttribute, "D" );
        saveTeav( entityInstanceC1, trackedEntityAttribute, "C" );
        saveTeav( entityInstanceD1, trackedEntityAttribute, "B" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), OrderParam.SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceD1.getId(), entityInstanceC1.getId(),
            entityInstanceB1.getId() ), teiIdList );
    }

    @Test
    void shouldCountOneEntityWhenOnePresent()
    {
        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        int counter = entityInstanceService.getTrackedEntityInstanceCount( new TrackedEntityInstanceQueryParams(), true,
            true );

        assertEquals( 1, counter );
    }

    @Test
    void shouldCountZeroEntitiesWhenNonePresent()
    {
        int trackedEntitiesCounter = entityInstanceService
            .getTrackedEntityInstanceCount( new TrackedEntityInstanceQueryParams(), true, true );

        assertEquals( 0, trackedEntitiesCounter );
    }

    private void saveTeav( TrackedEntityInstance trackedEntityInstance,
        TrackedEntityAttribute attribute, String value )
    {
        TrackedEntityAttributeValue trackedEntityAttributeValueA1 = new TrackedEntityAttributeValue();

        trackedEntityAttributeValueA1.setAttribute( attribute );
        trackedEntityAttributeValueA1.setEntityInstance( trackedEntityInstance );
        trackedEntityAttributeValueA1.setValue( value );

        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValueA1 );
    }
}
