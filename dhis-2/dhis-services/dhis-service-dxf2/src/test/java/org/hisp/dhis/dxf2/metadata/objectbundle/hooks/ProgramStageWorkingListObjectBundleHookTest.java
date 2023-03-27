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

import static org.hisp.dhis.feedback.ErrorCode.E4062;
import static org.hisp.dhis.feedback.ErrorCode.E4063;
import static org.hisp.dhis.feedback.ErrorCode.E4064;
import static org.hisp.dhis.feedback.ErrorCode.E4065;
import static org.hisp.dhis.feedback.ErrorCode.E4066;
import static org.hisp.dhis.feedback.ErrorCode.E4067;
import static org.hisp.dhis.feedback.ErrorCode.E4068;
import static org.hisp.dhis.feedback.ErrorCode.E7500;
import static org.hisp.dhis.utils.Assertions.assertErrorReport;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
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
            .eventOccurredAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
                DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.ABSOLUTE ) )
            .eventScheduledAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
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

        assertIsEmpty( errorReports );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEnrolledAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .enrolledAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4062, "EnrollmentCreatedDate" );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEnrollmentOccurredAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .enrollmentOccurredAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4062, "EnrollmentIncidentDate" );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEventCreatedAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .eventCreatedAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4062, "EventCreatedDate" );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEventOccurredAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .eventOccurredAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4062, "EventOccurredDate" );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForScheduledAt()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .eventScheduledAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4062, "EventScheduledDate" );
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

        assertErrorReport( errorReports, E4063 );
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

        assertErrorReport( errorReports, E4063 );
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

        assertErrorReport( errorReports, E7500, "fakeOrgUnit" );
    }

    @Test
    void shouldFailWhenOrgUnitNotProvidedAndModeSelectedOrDescendantsOrChildren()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .ouMode( OrganisationUnitSelectionMode.SELECTED )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4064, "SELECTED" );
    }

    @Test
    void shouldFailWhenDataItemUidNotProvided()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .dataFilters( List.of( new EventDataFilter() ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4065 );
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

        assertErrorReport( errorReports, E4066, "DataItem" );
    }

    @Test
    void shouldFailWhenAttributeFilterUidNotProvided()
    {
        ProgramStageQueryCriteria queryCriteria = ProgramStageQueryCriteria.builder()
            .attributeValueFilters( List.of( new AttributeValueFilter() ) )
            .build();
        programStageWorkingList.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingList, bundle );

        assertErrorReport( errorReports, E4067 );
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

        assertErrorReport( errorReports, E4068, "attribute" );
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