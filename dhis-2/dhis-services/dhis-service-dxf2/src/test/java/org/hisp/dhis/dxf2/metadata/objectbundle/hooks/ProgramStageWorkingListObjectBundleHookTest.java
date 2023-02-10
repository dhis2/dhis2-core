/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.programstagefilter.EventDataFilter;
import org.hisp.dhis.programstageworkinglist.ProgramStageQueryCriteria;
import org.hisp.dhis.programstageworkinglist.ProgramStageWorkingList;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityfilter.AttributeValueFilter;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class ProgramStageWorkingListObjectBundleHookTest
{

    @Mock
    private DataElementService dataElementService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    private ProgramStageWorkingListObjectBundleHook workingListHook;

    private final ObjectBundleParams objectBundleParams = new ObjectBundleParams();

    private final ObjectBundle bundle = new ObjectBundle( objectBundleParams, new Preheat(), Collections.emptyMap() );

    private ProgramStageWorkingList programStageWorkingList;

    @BeforeEach
    public void setUp()
    {
        workingListHook = new ProgramStageWorkingListObjectBundleHook( dataElementService,
            organisationUnitService, attributeService );
        programStageWorkingList = new ProgramStageWorkingList();
    }

    @Test
    void shouldReturnNoErrorsWhenQueryCriteriaSuppliedIsValid()
    {
        EventDataFilter dataFilter = new EventDataFilter();
        dataFilter.setDataItem( "DataItem" );
        AttributeValueFilter attributeValueFilter = new AttributeValueFilter();
        attributeValueFilter.setAttribute( "attribute" );
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .enrolledAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
                DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.ABSOLUTE ) )
            .enrollmentOccurredAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
                DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.RELATIVE ) )
            .eventCreatedAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
                DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.ABSOLUTE ) )
            .scheduledAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
                DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.RELATIVE ) )
            .assignedUsers( Collections.singleton( "User" ) )
            .assignedUserMode( AssignedUserSelectionMode.PROVIDED )
            .orgUnit( "orgUnit" )
            .ouMode( OrganisationUnitSelectionMode.SELECTED )
            .dataFilters( List.of( dataFilter ) )
            .attributeValueFilters( Collections.singletonList( attributeValueFilter ) )
            .build();

        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( new OrganisationUnit() );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( new DataElement() );
        when( attributeService.getTrackedEntityAttribute( anyString() ) ).thenReturn( new TrackedEntityAttribute() );
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 0, errorReports.size() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEnrolledAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .enrolledAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals(
            "Start date or end date have to be specified when date period type is set to ABSOLUTE for item `EnrollmentCreatedDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEnrollmentOccurredAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .enrollmentOccurredAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals(
            "Start date or end date have to be specified when date period type is set to ABSOLUTE for item `EnrollmentIncidentDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEventCreatedAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .eventCreatedAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals(
            "Start date or end date have to be specified when date period type is set to ABSOLUTE for item `EventDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForScheduledAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .scheduledAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals(
            "Start date or end date have to be specified when date period type is set to ABSOLUTE for item `EventScheduledDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenUsersIsProvidedButNoUsersSupplied()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .assignedUsers( Collections.emptySet() )
            .assignedUserMode( AssignedUserSelectionMode.PROVIDED )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Assigned users cannot be empty when assigned user mode is set to PROVIDED",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenAssignedUsersIsIncorrect()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .assignedUsers( Collections.emptySet() )
            .assignedUserMode( AssignedUserSelectionMode.PROVIDED )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Assigned users cannot be empty when assigned user mode is set to PROVIDED",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenOrgUnitProvidedDoesNotExist()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .orgUnit( "fakeOrgUnit" )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( null );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Organisation unit does not exist: `fakeOrgUnit`", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenOrgUnitNotProvidedAndModeSelectedOrDescendantsOrChildren()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .ouMode( OrganisationUnitSelectionMode.SELECTED )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Organisation Unit cannot be empty with `SELECTED` org unit mode",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDataItemUidNotProvided()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .dataFilters( List.of( new EventDataFilter() ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Data item UID is missing in filter", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDataItemProvidedDoesNotExist()
    {
        EventDataFilter dataFilter = new EventDataFilter();
        dataFilter.setDataItem( "DataItem" );
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .dataFilters( Collections.singletonList( dataFilter ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( null );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "No data element found for item: `DataItem`", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenAttributeFilterUidNotProvided()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .attributeValueFilters( List.of( new AttributeValueFilter() ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Attribute UID is missing in filter", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenAttributeFilterProvidedDoesNotExist()
    {
        AttributeValueFilter attributeValueFilter = new AttributeValueFilter();
        attributeValueFilter.setAttribute( "attribute" );

        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .attributeValueFilters( Collections.singletonList( attributeValueFilter ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );
        when( attributeService.getTrackedEntityAttribute( anyString() ) ).thenReturn( null );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "No tracked entity attribute found for attribute: `attribute`",
            errorReports.get( 0 ).getMessage() );
    }

    private DateFilterPeriod createDatePeriod( Date startDate, Date endDate, DatePeriodType dateType )
    {
        DateFilterPeriod dateFilterPeriod = new DateFilterPeriod();
        dateFilterPeriod.setStartDate( startDate );
        dateFilterPeriod.setEndDate( endDate );
        dateFilterPeriod.setType( dateType );

        return dateFilterPeriod;
    }
}