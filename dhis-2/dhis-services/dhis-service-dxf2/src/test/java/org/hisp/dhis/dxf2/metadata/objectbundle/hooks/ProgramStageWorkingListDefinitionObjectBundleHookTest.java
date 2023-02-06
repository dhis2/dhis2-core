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
import org.hisp.dhis.programstageworkinglistdefinition.ProgramStageQueryCriteria;
import org.hisp.dhis.programstageworkinglistdefinition.ProgramStageWorkingListDefinition;
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
class ProgramStageWorkingListDefinitionObjectBundleHookTest
{

    @Mock
    private DataElementService dataElementService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    private ProgramStageWorkingListDefinitionObjectBundleHook workingListHook;

    private final ObjectBundleParams objectBundleParams = new ObjectBundleParams();

    private final ObjectBundle bundle = new ObjectBundle( objectBundleParams, new Preheat(), Collections.emptyMap() );

    private ProgramStageQueryCriteria queryCriteria;

    private ProgramStageWorkingListDefinition programStageWorkingListDefinition;

    @BeforeEach
    public void setUp()
    {
        workingListHook = new ProgramStageWorkingListDefinitionObjectBundleHook( dataElementService,
            organisationUnitService, attributeService );
        programStageWorkingListDefinition = new ProgramStageWorkingListDefinition();
        queryCriteria = new ProgramStageQueryCriteria();
    }

    @Test
    void shouldReturnNoErrorsWhenQueryCriteriaSuppliedIsValid()
    {
        queryCriteria.setEnrolledAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
            DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.ABSOLUTE ) );
        queryCriteria.setEnrollmentOccurredAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
            DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.RELATIVE ) );
        queryCriteria.setEventCreatedAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
            DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.ABSOLUTE ) );
        queryCriteria.setScheduledAt( createDatePeriod( DateTime.now().minusDays( 1 ).toDate(),
            DateTime.now().plusDays( 1 ).toDate(), DatePeriodType.RELATIVE ) );
        queryCriteria.setAssignedUsers( Collections.singleton( "User" ) );
        queryCriteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );
        queryCriteria.setOrgUnit( "orgUnit" );
        queryCriteria.setOuMode( OrganisationUnitSelectionMode.SELECTED );
        EventDataFilter dataFilter = new EventDataFilter();
        dataFilter.setDataItem( "DataItem" );
        queryCriteria.setDataFilters( List.of( dataFilter ) );
        AttributeValueFilter attributeValueFilter = new AttributeValueFilter();
        attributeValueFilter.setAttribute( "attribute" );
        queryCriteria.setAttributeValueFilters( Collections.singletonList( attributeValueFilter ) );
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( new OrganisationUnit() );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( new DataElement() );
        when( attributeService.getTrackedEntityAttribute( anyString() ) ).thenReturn( new TrackedEntityAttribute() );

        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 0, errorReports.size() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEnrolledAt()
    {
        queryCriteria.setEnrolledAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals(
            "Start date or end date not specified with ABSOLUTE date period type for item `EnrollmentCreatedDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEnrollmentOccurredAt()
    {
        queryCriteria.setEnrollmentOccurredAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals(
            "Start date or end date not specified with ABSOLUTE date period type for item `EnrollmentIncidentDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForEventCreatedAt()
    {
        queryCriteria.setEventCreatedAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Start date or end date not specified with ABSOLUTE date period type for item `EventDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDatePeriodAbsoluteAndNoStartNorEndDateSpecifiedForScheduledAt()
    {
        queryCriteria.setScheduledAt( createDatePeriod( null, null, DatePeriodType.ABSOLUTE ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals(
            "Start date or end date not specified with ABSOLUTE date period type for item `EventScheduledDate`",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenUsersIsProvidedButNoUsersSupplied()
    {
        queryCriteria.setAssignedUsers( Collections.emptySet() );
        queryCriteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Assigned Users cannot be empty with PROVIDED assigned user mode",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenAssignedUsersIsIncorrect()
    {
        queryCriteria.setAssignedUsers( Collections.emptySet() );
        queryCriteria.setAssignedUserMode( AssignedUserSelectionMode.PROVIDED );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Assigned Users cannot be empty with PROVIDED assigned user mode",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenOrgUnitProvidedDoesNotExist()
    {
        queryCriteria.setOrgUnit( "fakeOrgUnit" );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( null );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Organisation unit does not exist: `fakeOrgUnit`", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenOrgUnitNotProvidedAndModeSelectedOrDescendantsOrChildren()
    {
        queryCriteria.setOuMode( OrganisationUnitSelectionMode.SELECTED );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Organisation Unit cannot be empty with `SELECTED` org unit mode",
            errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDataItemUidNotProvided()
    {
        queryCriteria.setDataFilters( List.of( new EventDataFilter() ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Data item Uid is missing in filter", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenDataItemProvidedDoesNotExist()
    {
        EventDataFilter dataFilter = new EventDataFilter();
        dataFilter.setDataItem( "DataItem" );
        queryCriteria.setDataFilters( Collections.singletonList( dataFilter ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( null );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "No data element found for item: `DataItem`", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenAttributeFilterUidNotProvided()
    {
        queryCriteria.setAttributeValueFilters( List.of( new AttributeValueFilter() ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

        assertEquals( 1, errorReports.size() );
        assertEquals( "Attribute Uid is missing in filter", errorReports.get( 0 ).getMessage() );
    }

    @Test
    void shouldFailWhenAttributeFilterProvidedDoesNotExist()
    {
        AttributeValueFilter attributeValueFilter = new AttributeValueFilter();
        attributeValueFilter.setAttribute( "attribute" );
        queryCriteria.setAttributeValueFilters( Collections.singletonList( attributeValueFilter ) );
        programStageWorkingListDefinition.setProgramStageQueryCriteria( queryCriteria );
        when( attributeService.getTrackedEntityAttribute( anyString() ) ).thenReturn( null );

        List<ErrorReport> errorReports = workingListHook.validate( programStageWorkingListDefinition, bundle );

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