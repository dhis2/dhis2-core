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
package org.hisp.dhis.trackedentityfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
class TrackedEntityInstanceFilterServiceTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private ProgramService programService;

    @Autowired
    private TrackedEntityInstanceFilterService trackedEntityInstanceFilterService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private UserService _userService;

    private Program programA;

    private Program programB;

    @BeforeEach
    void init()
    {
        userService = _userService;
    }

    @Override
    public void setUpTest()
    {
        programA = createProgram( 'A', null, null );
        programB = createProgram( 'B', null, null );
        programService.addProgram( programA );
        programService.addProgram( programB );
    }

    @Test
    void testAddGet()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );
        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        long idB = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );
        assertEquals( idA, trackedEntityInstanceFilterA.getId() );
        assertEquals( idB, trackedEntityInstanceFilterB.getId() );
        assertEquals( trackedEntityInstanceFilterA, trackedEntityInstanceFilterService.get( idA ) );
        assertEquals( trackedEntityInstanceFilterB, trackedEntityInstanceFilterService.get( idB ) );
    }

    @Test
    void testDefaultPrivateAccess()
    {
        long idA = trackedEntityInstanceFilterService.add( createTrackedEntityInstanceFilter( 'A', programA ) );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = trackedEntityInstanceFilterService.get( idA );
        assertEquals( trackedEntityInstanceFilterA.getPublicAccess(), AccessStringHelper.DEFAULT );
    }

    @Test
    void testGetAll()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );
        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService.getAll();
        assertEquals( trackedEntityInstanceFilters.size(), 2 );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterA ) );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterB ) );
    }

    @Test
    void testValidateProgramInTeiFilter()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        assertEquals( 0, trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA ).size() );
        trackedEntityInstanceFilterA.setProgram( createProgram( 'z' ) );
        List<String> errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Program is specified but does not exist: " + trackedEntityInstanceFilterA.getProgram().getUid() );

        trackedEntityInstanceFilterA.setProgram( programA );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 0, errors.size() );
    }

    @Test
    void testValidateAssignedUsersInTeiFilter()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );
        List<String> errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Assigned Users cannot be empty with PROVIDED assigned user mode" );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setAssignedUsers( Collections.singleton( "useruid" ) );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 0, errors.size() );
    }

    @Test
    void testValidateOrganisationUnitsSelectedModeInTeiFilter()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setOuMode( OrganisationUnitSelectionMode.SELECTED );
        List<String> errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Organisation Unit cannot be empty with SELECTED org unit mode" );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setOuMode( OrganisationUnitSelectionMode.CHILDREN );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Organisation Unit cannot be empty with CHILDREN org unit mode" );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setOuMode( OrganisationUnitSelectionMode.DESCENDANTS );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Organisation Unit cannot be empty with DESCENDANTS org unit mode" );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setOuMode( OrganisationUnitSelectionMode.SELECTED );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setOrganisationUnit( "organisationunituid" );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 0, errors.size() );
    }

    @Test
    void testValidateOrderParamsInTeiFilter()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setOrder( "aaa:asc,created:desc" );
        List<String> errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Invalid order property: aaa" );
    }

    @Test
    void testValidateDateFilterPeriods()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        DateFilterPeriod incorrectDateFilterPeriod = new DateFilterPeriod();
        incorrectDateFilterPeriod.setType( DatePeriodType.ABSOLUTE );

        DateFilterPeriod correctDateFilterPeriod = new DateFilterPeriod();
        correctDateFilterPeriod.setType( DatePeriodType.ABSOLUTE );
        correctDateFilterPeriod.setStartDate( new Date() );
        TrackedEntityAttribute attributeA = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeService.addTrackedEntityAttribute( attributeA );

        AttributeValueFilter avf1 = new AttributeValueFilter();
        avf1.setAttribute( attributeA.getUid() );
        avf1.setDateFilter( incorrectDateFilterPeriod );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().getAttributeValueFilters().add( avf1 );
        List<String> errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Start date or end date not specified with ABSOLUTE date period type for " + attributeA.getUid() );

        avf1.setDateFilter( correctDateFilterPeriod );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 0, errors.size() );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().getAttributeValueFilters().clear();

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setEnrollmentCreatedDate( incorrectDateFilterPeriod );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Start date or end date not specified with ABSOLUTE date period type for EnrollmentCreatedDate" );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setEnrollmentCreatedDate( null );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setEnrollmentIncidentDate( incorrectDateFilterPeriod );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Start date or end date not specified with ABSOLUTE date period type for EnrollmentIncidentDate" );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setEnrollmentIncidentDate( null );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setLastUpdatedDate( incorrectDateFilterPeriod );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Start date or end date not specified with ABSOLUTE date period type for LastUpdatedDate" );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setLastUpdatedDate( null );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().setEventDate( incorrectDateFilterPeriod );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ),
            "Start date or end date not specified with ABSOLUTE date period type for EventDate" );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().setEventDate( null );

    }

    @Test
    void testValidateAttributeInTeiAttributeValueFilter()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityAttribute attributeA = createTrackedEntityAttribute( 'A' );
        TrackedEntityAttribute attributeB = createTrackedEntityAttribute( 'B' );
        trackedEntityAttributeService.addTrackedEntityAttribute( attributeA );

        AttributeValueFilter avf1 = new AttributeValueFilter();
        avf1.setAttribute( attributeA.getUid() );
        avf1.setEq( "abc" );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().getAttributeValueFilters().add( avf1 );
        assertEquals( 0, trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA ).size() );

        AttributeValueFilter avf2 = new AttributeValueFilter();
        avf2.setAttribute( attributeB.getUid() );
        avf2.setEq( "abcef" );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().getAttributeValueFilters().add( avf2 );

        List<String> errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals(
            errors.get( 0 ), "No tracked entity attribute found for attribute:" + avf2.getAttribute() );

        trackedEntityInstanceFilterA.getEntityQueryCriteria().getAttributeValueFilters().clear();
        avf2.setAttribute( "" );
        trackedEntityInstanceFilterA.getEntityQueryCriteria().getAttributeValueFilters().add( avf2 );
        errors = trackedEntityInstanceFilterService.validate( trackedEntityInstanceFilterA );
        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ), "Attribute Uid is missing in filter" );
    }

    @Test
    void testGetByProgram()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterC = createTrackedEntityInstanceFilter( 'C', programA );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterC );
        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService
            .get( programA );
        assertEquals( trackedEntityInstanceFilters.size(), 2 );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterA ) );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterC ) );
        assertFalse( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterB ) );
    }

    @Test
    void testUpdate()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        trackedEntityInstanceFilterA.setProgram( programB );
        trackedEntityInstanceFilterService.update( trackedEntityInstanceFilterA );
        assertEquals( trackedEntityInstanceFilterA, trackedEntityInstanceFilterService.get( idA ) );
        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService
            .get( programB );
        assertEquals( trackedEntityInstanceFilters.size(), 1 );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterA ) );
        trackedEntityInstanceFilters = trackedEntityInstanceFilterService.get( programA );
        assertEquals( trackedEntityInstanceFilters.size(), 0 );
    }

    @Test
    void testDelete()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );
        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        long idB = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );
        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService.getAll();
        assertEquals( trackedEntityInstanceFilters.size(), 2 );
        trackedEntityInstanceFilterService.delete( trackedEntityInstanceFilterService.get( idA ) );
        assertNull( trackedEntityInstanceFilterService.get( idA ) );
        assertNotNull( trackedEntityInstanceFilterService.get( idB ) );
    }

    @Test
    void testSaveWithoutAuthority()
    {
        createUserAndInjectSecurityContext( false );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        assertNotNull( trackedEntityInstanceFilterService.get( idA ) );
    }

    @Test
    void testSaveWithAuthority()
    {
        createUserAndInjectSecurityContext( false, "F_PROGRAMSTAGE_ADD" );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        assertNotNull( trackedEntityInstanceFilterService.get( idA ) );
    }
}
