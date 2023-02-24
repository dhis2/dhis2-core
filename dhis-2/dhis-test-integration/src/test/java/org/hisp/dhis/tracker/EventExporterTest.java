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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Enrico Colasante
 */
class EventExporterTest extends TrackerTest
{

    @Autowired
    private EventService eventService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private DataElementService dataElementService;

    private OrganisationUnit orgUnit;

    private ProgramStage programStage;

    private Program program;

    final Function<EventSearchParams, List<String>> eventsFunction = ( params ) -> eventService.getEvents( params )
        .getEvents()
        .stream().map( Event::getEvent ).collect( Collectors.toList() );

    /**
     * EVENT_ID is at position 0 in column headers in events grid
     */
    final Function<EventSearchParams, List<String>> eventsGridFunction = ( params ) -> eventService
        .getEventsGrid( params )
        .getRows()
        .stream().map( r -> r.get( 0 ).toString() ).collect( Collectors.toList() );

    private TrackedEntityInstance trackedEntityInstance;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        User userA = userService.getUser( "M5zQapPyTZI" );
        assertNoErrors(
            trackerImportService.importTracker( fromJson( "tracker/event_and_enrollment.json", userA.getUid() ) ) );
        orgUnit = manager.get( OrganisationUnit.class, "h4w96yEMlzO" );
        programStage = manager.get( ProgramStage.class, "NpsdDv6kKSO" );
        program = programStage.getProgram();
        trackedEntityInstance = manager.get( TrackedEntityInstance.class, "dUE514NMOlo" );
        manager.flush();
    }

    private Stream<Arguments> getEventsFunctions()
    {
        return Stream.of(
            Arguments.of( eventsFunction ),
            Arguments.of( eventsGridFunction ) );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEvents( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramStage( programStage );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithTotalPages( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setTotalPages( true );
        params.setProgramStage( programStage );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @Test
    void testExportEventsWhenFilteringByEnrollment()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setTrackedEntityInstance( trackedEntityInstance );
        params.setProgramInstances( Set.of( "TvctPPhpD8z" ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithExecutionAndUpdateDates( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );

        params.setStartDate( getDate( 2018, 1, 1 ) );
        params.setEndDate( getDate( 2020, 1, 1 ) );
        params.setSkipChangedBefore( getDate( 2018, 1, 1 ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithLastUpdateDuration( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );

        params.setLastUpdatedDuration( "1d" );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithLastUpdateDates( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );

        Date date = new Date();

        params.setLastUpdatedStartDate( Date.from(
            date.toInstant().minus( 1, ChronoUnit.DAYS ).atZone( ZoneId.systemDefault() ).toInstant() ) );

        params.setLastUpdatedEndDate( Date.from(
            date.toInstant().plus( 1, ChronoUnit.DAYS ).atZone( ZoneId.systemDefault() ).toInstant() ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsLike( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ" ) );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(),
                null, null ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithStatusFilter(
        Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ" ) );
        params.setProgramStatus( ProgramStatus.ACTIVE );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(),
                null, null ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithProgramTypeFilter(
        Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ" ) );
        params.setProgramType( ProgramType.WITH_REGISTRATION );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(),
                null, null ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsEqual( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ" ) );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.EQ, "value00001", dataElement.getValueType(),
                null,
                null ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsIn( Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ", "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );

        DataElement datael00001 = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( datael00001, QueryOperator.IN, "value00001;value00002", datael00001.getValueType(),
                null,
                null ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithCategoryOptionSuperUser()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ" ) );
        params.setProgramStage( programStage );
        params.setProgram( program );

        params.setCategoryOptionCombo( manager.get( CategoryOptionCombo.class, "HllvX50cXC0" ) );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params
            .setDataElements(
                Set.of( new QueryItem( dataElement, QueryOperator.EQ, "value00001", dataElement.getValueType(),
                    null, dataElement.getOptionSet() ) ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithCategoryOptionNotSuperUser()
    {
        injectSecurityContext( createAndAddUser( false, "user", Set.of( orgUnit ), Set.of( orgUnit ),
            "F_EXPORT_DATA" ) );

        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );
        params.setProgram( program );

        params.setCategoryOptionCombo( manager.get( CategoryOptionCombo.class, "HllvX50cXC0" ) );

        DataElement dataElement = dataElement( "DATAEL00002" );

        params
            .setDataElements(
                Set.of( new QueryItem( dataElement, QueryOperator.EQ, "value00002", dataElement.getValueType(),
                    null, dataElement.getOptionSet() ) ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithOptionSetEqual(
        Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ" ) );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00005" );

        params.setDataElements(
            Set.of( new QueryItem( dataElement, QueryOperator.EQ, "option1", dataElement.getValueType(),
                null, dataElement.getOptionSet() ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithOptionSetIn(
        Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ", "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00005" );

        params.setDataElements(
            Set.of( new QueryItem( dataElement, QueryOperator.IN, "option1;option2", dataElement.getValueType(),
                null, dataElement.getOptionSet() ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithOptionSetLike(
        Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ" ) );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00005" );

        params
            .setDataElements( Set.of( new QueryItem( dataElement, QueryOperator.LIKE, "opt", dataElement.getValueType(),
                null, dataElement.getOptionSet() ) ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByNumericDataElements(
        Function<EventSearchParams, List<String>> eventFunction )
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Set.of( "nxP7UnKhomJ", "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );

        DataElement dataElement = dataElement( "DATAEL00006" );

        QueryItem queryItem = new QueryItem( dataElement, null, dataElement.getValueType(), null,
            dataElement.getOptionSet() );
        queryItem.addFilter( new QueryFilter( QueryOperator.LT, "77" ) );
        queryItem.addFilter( new QueryFilter( QueryOperator.GT, "8" ) );
        params.setDataElements( Set.of( queryItem ) );

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @Test
    void testEnrollmentEnrolledBeforeSetToBeforeFirstEnrolledAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentEnrolledBefore( parseDate( "2021-02-27T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testEnrollmentEnrolledBeforeEqualToFirstEnrolledAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentEnrolledBefore( parseDate( "2021-02-28T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledBeforeSetToAfterFirstEnrolledAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentEnrolledBefore( parseDate( "2021-02-28T13:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledAfterSetToBeforeLastEnrolledAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentEnrolledAfter( parseDate( "2021-03-27T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledAfterEqualToLastEnrolledAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentEnrolledAfter( parseDate( "2021-03-28T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledAfterSetToAfterLastEnrolledAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentEnrolledAfter( parseDate( "2021-03-28T13:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testEnrollmentOccurredBeforeSetToBeforeFirstOccurredAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentOccurredBefore( parseDate( "2021-02-27T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testEnrollmentOccurredBeforeEqualToFirstOccurredAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentOccurredBefore( parseDate( "2021-02-28T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentOccurredBeforeSetToAfterFirstOccurredAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentOccurredBefore( parseDate( "2021-02-28T13:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentOccurredAfterSetToBeforeLastOccurredAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentOccurredAfter( parseDate( "2021-03-27T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentOccurredAfterEqualToLastOccurredAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentOccurredAfter( parseDate( "2021-03-28T12:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentFilterNumericAttributes()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        QueryItem queryItem = numericQueryItem( "numericAttr" );
        QueryFilter lessThan = new QueryFilter( QueryOperator.LT, "77" );
        QueryFilter greaterThan = new QueryFilter( QueryOperator.GT, "8" );
        queryItem.setFilters( List.of( lessThan, greaterThan ) );

        params.addFilterAttributes( queryItem );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertFalse( true, "testing redirectTestOutputToFile when running tests" );
        assertContainsOnly( List.of( "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testEnrollmentFilterAttributes()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        params.addFilterAttributes( queryItem( "toUpdate000", QueryOperator.EQ, "summer day" ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertFalse( true, "testing redirectTestOutputToFile when running tests" );
        assertContainsOnly( List.of( "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void testEnrollmentFilterAttributesWithMultipleFiltersOnDifferentAttributes()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        params.addFilterAttributes( List.of(
            queryItem( "toUpdate000", QueryOperator.EQ, "rainy day" ),
            queryItem( "notUpdated0", QueryOperator.EQ, "winter day" ) ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testEnrollmentFilterAttributesWithMultipleFiltersOnTheSameAttribute()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        QueryItem item = queryItem( "toUpdate000", QueryOperator.LIKE, "day" );
        item.addFilter( new QueryFilter( QueryOperator.LIKE, "in" ) );
        params.addFilterAttributes( item );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnAttributeAsc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        params.addFilterAttributes( queryItem( "toUpdate000" ) );
        params.addAttributeOrders( List.of( new OrderParam( "toUpdate000", OrderParam.SortDirection.ASC ) ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertEquals( List.of( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnAttributeDesc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        params.addFilterAttributes( queryItem( "toUpdate000" ) );
        params.addAttributeOrders( List.of( new OrderParam( "toUpdate000", OrderParam.SortDirection.DESC ) ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertEquals( List.of( "QS6w44flWAf", "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnMultipleAttributesDesc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        params.addFilterAttributes( List.of( queryItem( "toUpdate000" ), queryItem( "toDelete000" ) ) );
        params.addAttributeOrders( List.of( new OrderParam( "toDelete000", OrderParam.SortDirection.DESC ),
            new OrderParam( "toUpdate000", OrderParam.SortDirection.DESC ) ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertEquals( List.of( "QS6w44flWAf", "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnMultipleAttributesAsc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        params.addFilterAttributes( List.of( queryItem( "toUpdate000" ), queryItem( "toDelete000" ) ) );
        params.addAttributeOrders( List.of( new OrderParam( "toDelete000", OrderParam.SortDirection.DESC ),
            new OrderParam( "toUpdate000", OrderParam.SortDirection.ASC ) ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertEquals( List.of( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void testEnrollmentOccurredAfterSetToAfterLastOccurredAtDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setEnrollmentOccurredAfter( parseDate( "2021-03-28T13:05:00.000" ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testOrderByEnrolledAtDesc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.addOrders( List.of( new OrderParam( "enrolledAt", OrderParam.SortDirection.DESC ) ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertEquals( List.of( "TvctPPhpD8z", "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testOrderByEnrolledAtAsc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.addOrders( List.of( new OrderParam( "enrolledAt", OrderParam.SortDirection.ASC ) ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertEquals( List.of( "nxP7UnKhomJ", "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testOrderByOccurredAtDesc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.addOrders( List.of( new OrderParam( "occurredAt", OrderParam.SortDirection.DESC ) ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertEquals( List.of( "TvctPPhpD8z", "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testOrderByOccurredAtAsc()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.addOrders( List.of( new OrderParam( "occurredAt", OrderParam.SortDirection.ASC ) ) );

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream().map( Event::getEnrollment )
            .collect( Collectors.toList() );

        assertEquals( List.of( "nxP7UnKhomJ", "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void shouldReturnNoEventsWhenParamStartDueDateLaterThanEventDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateStart( parseDate( "2021-02-28T13:05:00.000" ) );

        List<String> events = eventsFunction.apply( params );

        assertIsEmpty( events );
    }

    @Test
    void shouldReturnEventsWhenParamStartDueDateEarlierThanEventsDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateStart( parseDate( "2018-02-28T13:05:00.000" ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @Test
    void shouldReturnNoEventsWhenParamEndDueDateEarlierThanEventDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateEnd( parseDate( "2018-02-28T13:05:00.000" ) );

        List<String> events = eventsFunction.apply( params );

        assertIsEmpty( events );
    }

    @Test
    void shouldReturnEventsWhenParamEndDueDateLaterThanEventsDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateEnd( parseDate( "2021-02-28T13:05:00.000" ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    private DataElement dataElement( String uid )
    {
        return dataElementService.getDataElement( uid );
    }

    private static QueryItem queryItem( String teaUid, QueryOperator operator, String filter )
    {
        QueryItem item = queryItem( teaUid );
        item.addFilter( new QueryFilter( operator, filter ) );
        return item;
    }

    private static QueryItem queryItem( String teaUid )
    {
        return queryItem( teaUid, ValueType.TEXT );
    }

    private static QueryItem numericQueryItem( String teaUid )
    {
        return queryItem( teaUid, ValueType.INTEGER );
    }

    private static QueryItem queryItem( String teaUid, ValueType valueType )
    {
        TrackedEntityAttribute at = new TrackedEntityAttribute();
        at.setUid( teaUid );
        at.setValueType( valueType );
        at.setAggregationType( AggregationType.NONE );
        return new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(),
            at.isUnique() );
    }

}
