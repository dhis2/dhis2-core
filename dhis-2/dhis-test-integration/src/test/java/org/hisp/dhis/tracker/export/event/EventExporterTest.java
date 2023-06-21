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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.SneakyThrows;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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

    final Function<EventOperationParams, List<String>> eventsFunction = ( params ) -> {
        //We should analyze if it's possible to remove this try/catch when refactoring the event service layer in the epic https://dhis2.atlassian.net/browse/TECH-1534
        try
        {
            return eventService.getEvents( params )
                .getEvents()
                .stream().map( Event::getUid ).collect( Collectors.toList() );
        }
        catch ( BadRequestException | ForbiddenException e )
        {
            throw new RuntimeException( e );
        }
    };

    private TrackedEntity trackedEntity;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        User userA = userService.getUser( "M5zQapPyTZI" );
        assertNoErrors(
            trackerImportService.importTracker( fromJson( "tracker/event_and_enrollment.json", userA.getUid() ) ) );
        orgUnit = get( OrganisationUnit.class, "h4w96yEMlzO" );
        programStage = get( ProgramStage.class, "NpsdDv6kKSO" );
        program = programStage.getProgram();
        trackedEntity = get( TrackedEntity.class, "dUE514NMOlo" );

        // to test that events are only returned if the user has read access to ALL COs of an events COC
        CategoryOption categoryOption = get( CategoryOption.class, "yMj2MnmNI8L" );
        categoryOption.getSharing().setOwner( "o1HMTIzBGo7" );
        manager.update( categoryOption );

        manager.flush();
    }

    @BeforeEach
    void setUp()
    {
        // needed as some tests are run using another user (injectSecurityContext) while most tests expect to be run by admin
        injectAdminUser();
    }

    private Stream<Arguments> getEventsFunctions()
    {
        return Stream.of(
            Arguments.of( eventsFunction ) );
    }

    @Test
    void shouldExportEventAndMapAssignedUserWhenAssignedUserIsNotNull()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .trackedEntityUid( trackedEntity.getUid() ).enrollments( Set.of( "TvctPPhpD8z" ) ).build();

        List<Event> events = eventService.getEvents( params ).getEvents();

        assertEquals( get( Event.class, "D9PbzJY8bJM" ).getAssignedUser(),
            events.get( 0 ).getAssignedUser() );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEvents( Function<EventOperationParams, List<String>> eventFunction )
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .programStageUid( programStage.getUid() ).build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithTotalPages( Function<EventOperationParams, List<String>> eventFunction )
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .programStageUid( programStage.getUid() ).totalPages( true ).build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @Test
    void testExportEventsWhenFilteringByEnrollment()
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .trackedEntityUid( trackedEntity.getUid() ).enrollments( Set.of( "TvctPPhpD8z" ) ).build();

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithExecutionAndUpdateDates( Function<EventOperationParams, List<String>> eventFunction )
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "TvctPPhpD8z" ) ).programStageUid( programStage.getUid() )
            .startDate( getDate( 2018, 1, 1 ) ).endDate( getDate( 2020, 1, 29 ) )
            .skipChangedBefore( getDate( 2018, 1, 1 ) ).build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithLastUpdateDuration( Function<EventOperationParams, List<String>> eventFunction )
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "TvctPPhpD8z" ) ).programStageUid( programStage.getUid() ).updatedWithin( "1d" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWithLastUpdateDates( Function<EventOperationParams, List<String>> eventFunction )
    {
        Date date = new Date();
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "TvctPPhpD8z" ) ).programStageUid( programStage.getUid() )
            .updatedAfter( Date.from(
                date.toInstant().minus( 1, ChronoUnit.DAYS ).atZone( ZoneId.systemDefault() ).toInstant() ) )
            .updatedBefore( Date.from(
                date.toInstant().plus( 1, ChronoUnit.DAYS ).atZone( ZoneId.systemDefault() ).toInstant() ) )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsLike( Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00001" );
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ" ) ).programStageUid( programStage.getUid() )
            .filters( "DATAEL00001:like:%val%" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );

        new QueryItem( dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(),
            null, null );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithStatusFilter(
        Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00001" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ" ) ).programStageUid( programStage.getUid() )
            .programStatus( ProgramStatus.ACTIVE )
            .filters( dataElement.getUid() + ":like:%val%" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithProgramTypeFilter(
        Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00001" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ" ) ).programStageUid( programStage.getUid() )
            .programType( ProgramType.WITH_REGISTRATION )
            .filters( dataElement.getUid() + ":like:%val%" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsEqual( Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00001" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ" ) ).programStageUid( programStage.getUid() )
            .filters( dataElement.getUid() + ":like:%value00001%" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    @Disabled( "This test is disabled because it doesn't look like we offer the feature to filter by multiple values with the like operator" )
    void testExportEventsWhenFilteringByDataElementsIn( Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement datael00001 = dataElement( "DATAEL00001" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ", "TvctPPhpD8z" ) ).programStageUid( programStage.getUid() )
            .filters( datael00001.getUid() + ":like:%value00001%;%value00002%" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithCategoryOptionSuperUser()
    {
        DataElement dataElement = dataElement( "DATAEL00001" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ" ) ).programStageUid( programStage.getUid() )
            .programUid( program.getUid() )
            .attributeCategoryCombo( "bjDvmb4bfuf" )
            .attributeCategoryOptions( Set.of( "xYerKDKCefk" ) )
            .filters( dataElement.getUid() + ":eq:value00001" )
            .build();

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @Test
    void shouldReturnEventsNonSuperUserIsOwnerOrHasUserAccess()
        throws ForbiddenException,
        BadRequestException
    {
        // given events have a COC which has a CO which the
        // user owns yMj2MnmNI8L and has user read access to OUUdG3sdOqb
        injectSecurityContext( userService.getUser( "o1HMTIzBGo7" ) );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( "DiszpKrYNg8" )
            .events( Set.of( "lumVtWwwy0O", "cadc5eGj0j7" ) ).build();

        Events events = eventService.getEvents( params );

        assertContainsOnly( List.of( "lumVtWwwy0O", "cadc5eGj0j7" ), eventUids( events ) );
        List<Executable> executables = events.getEvents().stream()
            .map( e -> (Executable) () -> assertEquals( 2, e.getAttributeOptionCombo().getCategoryOptions().size(),
                String.format( "got category options %s", e.getAttributeOptionCombo().getCategoryOptions() ) ) )
            .collect( Collectors.toList() );
        assertAll( "all events should have the optionSize set which is the number of COs in the COC", executables );
    }

    @Test
    void shouldReturnNoEventsGivenUserHasNoAccess()
    {
        // given events have a COC which has a CO (OUUdG3sdOqb/yMj2MnmNI8L) which are not publicly readable, user is not the owner and has no user access
        injectSecurityContext( userService.getUser( "CYVgFNKCaUS" ) );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( "DiszpKrYNg8" )
            .events( Set.of( "lumVtWwwy0O", "cadc5eGj0j7" ) ).build();

        List<String> events = eventsFunction.apply( params );

        assertIsEmpty( events );
    }

    @Test
    void shouldReturnPublicEventsWithMultipleCategoryOptionsGivenNonDefaultPageSize()
        throws ForbiddenException,
        BadRequestException
    {
        OrganisationUnit orgUnit = get( OrganisationUnit.class, "DiszpKrYNg8" );
        Program program = get( Program.class, "iS7eutanDry" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .programUid( program.getUid() ).orders( List.of( new OrderParam( "occurredAt", SortDirection.DESC ) ) )
            .page( 1 ).pageSize( 3 ).build();

        Events firstPage = eventService.getEvents( params );

        assertAll( "first page",
            () -> assertSlimPager( 1, 3, false, firstPage ),
            () -> assertEquals( List.of( "ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp" ), eventUids( firstPage ) ) );

        params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() ).programUid( program.getUid() )
            .orders( List.of( new OrderParam( "occurredAt", SortDirection.DESC ) ) ).page( 2 ).pageSize( 3 ).build();

        Events secondPage = eventService.getEvents( params );

        assertAll( "second (last) page",
            () -> assertSlimPager( 2, 3, true, secondPage ),
            () -> assertEquals( List.of( "lumVtWwwy0O", "QRYjLTiJTrA", "cadc5eGj0j7" ), eventUids( secondPage ) ) );

        params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() ).programUid( program.getUid() )
            .orders( List.of( new OrderParam( "occurredAt", SortDirection.DESC ) ) ).page( 3 ).pageSize( 3 ).build();

        assertIsEmpty( eventsFunction.apply( params ) );
    }

    @Test
    void shouldReturnEventsWithMultipleCategoryOptionsGivenNonDefaultPageSizeAndTotalPages()
        throws ForbiddenException,
        BadRequestException
    {
        OrganisationUnit orgUnit = get( OrganisationUnit.class, "DiszpKrYNg8" );
        Program program = get( Program.class, "iS7eutanDry" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .programUid( program.getUid() )
            .orders( List.of( new OrderParam( "occurredAt", SortDirection.DESC ) ) ).page( 1 ).pageSize( 2 )
            .totalPages( true ).build();

        Events events = eventService.getEvents( params );

        assertAll( "first page",
            () -> assertPager( 1, 2, 6, events ),
            () -> assertEquals( List.of( "ck7DzdxqLqA", "OTmjvJDn0Fu" ), eventUids( events ) ) );
    }

    @Test
    void shouldReturnEventsGivenCategoryOptionCombo()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( "DiszpKrYNg8" )
            .attributeCategoryCombo( "O4VaNks6tta" )
            .attributeCategoryOptions( Set.of( "xwZ2u3WyQR0", "M58XdOfhiJ7" ) ).build();

        Events events = eventService.getEvents( params );

        assertContainsOnly( List.of( "kWjSezkXHVp", "OTmjvJDn0Fu" ), eventUids( events ) );
        List<Executable> executables = events.getEvents().stream()
            .map( e -> (Executable) () -> assertAll( "category options and combo of event " + e.getUid(),
                () -> assertEquals( "cr89ebDZrac", e.getAttributeOptionCombo().getUid() ),
                () -> assertContainsOnly( Set.of( "xwZ2u3WyQR0", "M58XdOfhiJ7" ),
                    e.getAttributeOptionCombo().getCategoryOptions().stream().map( CategoryOption::getUid )
                        .collect( Collectors.toSet() ) ) ) )
            .collect( Collectors.toList() );
        assertAll( "all events should have the same category option combo and options", executables );
    }

    @Test
    void shouldFailIfCategoryOptionComboOfGivenEventDoesNotHaveAValueForGivenIdScheme()
    {
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setCategoryOptionComboIdScheme( "ATTRIBUTE:GOLswS44mh8" );
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( "DiszpKrYNg8" ).idSchemes( idSchemes )
            .events( Set.of( "kWjSezkXHVp" ) ).build();

        IllegalStateException ex = assertThrows( IllegalStateException.class, () -> eventService.getEvents( params ) );
        assertStartsWith( "CategoryOptionCombo", ex.getMessage() );
        assertContains( "not have a value assigned for idScheme ATTRIBUTE:GOLswS44mh8", ex.getMessage() );
    }

    @Test
    void shouldReturnEventsGivenIdSchemeCode()
        throws ForbiddenException,
        BadRequestException
    {
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setProgramIdScheme( "code" );
        idSchemes.setProgramStageIdScheme( "code" );
        idSchemes.setOrgUnitIdScheme( "code" );
        idSchemes.setCategoryOptionComboIdScheme( "code" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( "DiszpKrYNg8" ).idSchemes( idSchemes )
            .attributeCategoryCombo( "O4VaNks6tta" )
            .attributeCategoryOptions( Set.of( "xwZ2u3WyQR0", "M58XdOfhiJ7" ) ).build();

        Events events = eventService.getEvents( params );

        assertContainsOnly( List.of( "kWjSezkXHVp", "OTmjvJDn0Fu" ), eventUids( events ) );
        List<Executable> executables = events.getEvents().stream()
            .map( e -> (Executable) () -> assertAll( "event " + e.getUid(),
                () -> assertEquals( "multi-program", e.getEnrollment().getProgram().getUid() ),
                () -> assertEquals( "multi-stage", e.getProgramStage().getUid() ),
                () -> assertEquals( "DiszpKrYNg8", e.getOrganisationUnit().getUid() ), // TODO(DHIS2-14968): this might be a bug caused by https://github.com/dhis2/dhis2-core/pull/12518
                () -> assertEquals( "COC_1153452", e.getAttributeOptionCombo().getUid() ),
                () -> assertContainsOnly( Set.of( "xwZ2u3WyQR0", "M58XdOfhiJ7" ),
                    e.getAttributeOptionCombo().getCategoryOptions().stream().map( CategoryOption::getUid )
                        .collect( Collectors.toSet() ) ) ) )
            .collect( Collectors.toList() );
        assertAll( "all events should have the same category option combo and options", executables );
    }

    @Test
    void shouldReturnEventsGivenIdSchemeAttribute()
        throws ForbiddenException,
        BadRequestException
    {
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setProgramIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        idSchemes.setProgramStageIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        idSchemes.setOrgUnitIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        idSchemes.setCategoryOptionComboIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( "DiszpKrYNg8" ).idSchemes( idSchemes )
            .attributeCategoryCombo( "O4VaNks6tta" )
            .attributeCategoryOptions( Set.of( "xwZ2u3WyQR0", "M58XdOfhiJ7" ) ).build();

        Events events = eventService.getEvents( params );

        assertContainsOnly( List.of( "kWjSezkXHVp", "OTmjvJDn0Fu" ), eventUids( events ) );
        List<Executable> executables = events.getEvents().stream()
            .map( e -> (Executable) () -> assertAll( "event " + e.getUid(),
                () -> assertEquals( "multi-program-attribute", e.getEnrollment().getProgram().getUid() ),
                () -> assertEquals( "multi-program-stage-attribute", e.getProgramStage().getUid() ),
                () -> assertEquals( "DiszpKrYNg8", e.getOrganisationUnit().getUid() ), // TODO(DHIS2-14968): this might be a bug caused by https://github.com/dhis2/dhis2-core/pull/12518
                () -> assertEquals( "COC_1153452-attribute", e.getAttributeOptionCombo().getUid() ),
                () -> assertContainsOnly( Set.of( "xwZ2u3WyQR0", "M58XdOfhiJ7" ),
                    e.getAttributeOptionCombo().getCategoryOptions().stream().map( CategoryOption::getUid )
                        .collect( Collectors.toSet() ) ) ) )
            .collect( Collectors.toList() );
        assertAll( "all events should have the same category option combo and options", executables );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithCategoryOptionNotSuperUser()
    {
        injectSecurityContext( createAndAddUser( false, "user", Set.of( orgUnit ), Set.of( orgUnit ),
            "F_EXPORT_DATA" ) );
        DataElement dataElement = dataElement( "DATAEL00002" );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "TvctPPhpD8z" ) )
            .programStageUid( programStage.getUid() ).programUid( program.getUid() )
            .attributeCategoryCombo( "bjDvmb4bfuf" )
            .attributeCategoryOptions( Set.of( "xYerKDKCefk" ) )
            .filters( dataElement.getUid() + ":eq:value00002" )
            .build();

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithOptionSetEqual(
        Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00005" );
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ" ) ).programStageUid( programStage.getUid() )
            .filters( dataElement.getUid() + ":eq:option1" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithOptionSetIn(
        Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00005" );
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ", "TvctPPhpD8z" ) )
            .programStageUid( programStage.getUid() )
            .filters( dataElement.getUid() + ":in:option1;option2" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByDataElementsWithOptionSetLike(
        Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00005" );
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ" ) ).programStageUid( programStage.getUid() )
            .filters( dataElement.getUid() + ":like:%opt%" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "pTzf9KYMk72" ), events );
    }

    @ParameterizedTest
    @MethodSource( "getEventsFunctions" )
    void testExportEventsWhenFilteringByNumericDataElements(
        Function<EventOperationParams, List<String>> eventFunction )
    {
        DataElement dataElement = dataElement( "DATAEL00006" );
        QueryItem queryItem = new QueryItem( dataElement, null, dataElement.getValueType(), null,
            dataElement.getOptionSet() );
        queryItem.addFilter( new QueryFilter( QueryOperator.LT, "77" ) );
        queryItem.addFilter( new QueryFilter( QueryOperator.GT, "8" ) );
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollments( Set.of( "nxP7UnKhomJ", "TvctPPhpD8z" ) ).programStageUid( programStage.getUid() )
            .filters( dataElement.getUid() + ":lt:77:gt:8" )
            .build();

        List<String> events = eventFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM" ), events );
    }

    @Test
    void testEnrollmentEnrolledBeforeSetToBeforeFirstEnrolledAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentEnrolledBefore( parseDate( "2021-02-27T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testEnrollmentEnrolledBeforeEqualToFirstEnrolledAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentEnrolledBefore( parseDate( "2021-02-28T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledBeforeSetToAfterFirstEnrolledAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentEnrolledBefore( parseDate( "2021-02-28T13:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledAfterSetToBeforeLastEnrolledAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentEnrolledAfter( parseDate( "2021-03-27T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledAfterEqualToLastEnrolledAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentEnrolledAfter( parseDate( "2021-03-28T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentEnrolledAfterSetToAfterLastEnrolledAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentEnrolledAfter( parseDate( "2021-03-28T13:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testEnrollmentOccurredBeforeSetToBeforeFirstOccurredAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentOccurredBefore( parseDate( "2021-02-27T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testEnrollmentOccurredBeforeEqualToFirstOccurredAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentOccurredBefore( parseDate( "2021-02-28T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentOccurredBeforeSetToAfterFirstOccurredAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentOccurredBefore( parseDate( "2021-02-28T13:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testEnrollmentOccurredAfterSetToBeforeLastOccurredAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentOccurredAfter( parseDate( "2021-03-27T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentOccurredAfterEqualToLastOccurredAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentOccurredAfter( parseDate( "2021-03-28T12:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "TvctPPhpD8z" ), enrollments );
    }

    @Test
    void testEnrollmentFilterNumericAttributes()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "numericAttr:lt:77:gt:8" ).build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testEnrollmentFilterAttributes()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000:eq:summer day" ).build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void testEnrollmentFilterAttributesWithMultipleFiltersOnDifferentAttributes()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000:eq:rainy day,notUpdated0:eq:winter day" ).build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    @Disabled( "This test is disabled because it doesn't look like we offer the feature to filter by multiple values with the like operator" )
    void testEnrollmentFilterAttributesWithMultipleFiltersOnTheSameAttribute()
        throws ForbiddenException,
        BadRequestException
    {
        QueryItem item = queryItem( "toUpdate000", QueryOperator.LIKE, "day" );
        item.addFilter( new QueryFilter( QueryOperator.LIKE, "in" ) );

        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000:like:%day%:like:%in%" ).build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertContainsOnly( List.of( "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnAttributeAsc()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000" )
            .attributeOrders( List.of( OrderCriteria.of( "toUpdate000", SortDirection.ASC ) ) )
            .orders( List.of( new OrderParam( "toUpdate000", SortDirection.ASC ) ) ).build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnAttributeDesc()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000" )
            .attributeOrders( List.of( OrderCriteria.of( "toUpdate000", SortDirection.DESC ) ) )
            .orders( List.of( new OrderParam( "toUpdate000", SortDirection.DESC ) ) ).build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "QS6w44flWAf", "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnMultipleAttributesDesc()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000,toDelete000" )
            .attributeOrders( List.of( OrderCriteria.of( "toDelete000", SortDirection.DESC ),
                OrderCriteria.of( "toUpdate000", SortDirection.DESC ) ) )
            .orders( List.of( new OrderParam( "toDelete000", SortDirection.DESC ),
                new OrderParam( "toUpdate000", SortDirection.DESC ) ) )
            .build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "QS6w44flWAf", "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void testOrderEventsOnMultipleAttributesAsc()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000,toDelete000" )
            .attributeOrders( List.of( OrderCriteria.of( "toDelete000", SortDirection.DESC ),
                OrderCriteria.of( "toUpdate000", SortDirection.ASC ) ) )
            .orders( List.of( new OrderParam( "toDelete000", SortDirection.DESC ),
                new OrderParam( "toUpdate000", SortDirection.ASC ) ) )
            .build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void testEnrollmentOccurredAfterSetToAfterLastOccurredAtDate()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .enrollmentOccurredAfter( parseDate( "2021-03-28T13:05:00.000" ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertIsEmpty( enrollments );
    }

    @Test
    void testOrderByEnrolledAtDesc()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .orders( List.of( new OrderParam( "enrolledAt", SortDirection.DESC ) ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "TvctPPhpD8z", "nxP7UnKhomJ" ), enrollments );
    }

    @Test
    void testOrderByEnrolledAtAsc()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .orders( List.of( new OrderParam( "enrolledAt", SortDirection.ASC ) ) ).build();

        List<String> enrollments = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "nxP7UnKhomJ", "TvctPPhpD8z" ), enrollments );
    }

    @SneakyThrows
    @Test
    void testOrderByOccurredAtDesc()
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .orders( List.of( new OrderParam( "occurredAt", SortDirection.DESC ) ) ).build();

        Events events = eventService.getEvents( params );

        assertEquals( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), eventUids( events ) );
    }

    @Test
    void testOrderByOccurredAtAsc()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .orders( List.of( new OrderParam( "occurredAt", SortDirection.ASC ) ) ).build();

        Events events = eventService.getEvents( params );

        assertEquals( List.of( "pTzf9KYMk72", "D9PbzJY8bJM" ), eventUids( events ) );
    }

    @Test
    void shouldReturnNoEventsWhenParamStartDueDateLaterThanEventDueDate()
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .scheduledAfter( parseDate( "2021-02-28T13:05:00.000" ) ).build();

        List<String> events = eventsFunction.apply( params );

        assertIsEmpty( events );
    }

    @Test
    void shouldReturnEventsWhenParamStartDueDateEarlierThanEventsDueDate()
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .scheduledAfter( parseDate( "2018-02-28T13:05:00.000" ) ).build();

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @Test
    void shouldReturnNoEventsWhenParamEndDueDateEarlierThanEventDueDate()
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .scheduledBefore( parseDate( "2018-02-28T13:05:00.000" ) ).build();

        List<String> events = eventsFunction.apply( params );

        assertIsEmpty( events );
    }

    @Test
    void shouldReturnEventsWhenParamEndDueDateLaterThanEventsDueDate()
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .scheduledBefore( parseDate( "2021-02-28T13:05:00.000" ) ).build();

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( List.of( "D9PbzJY8bJM", "pTzf9KYMk72" ), events );
    }

    @Test
    void shouldSortEntitiesRespectingOrderWhenAttributeOrderSuppliedBeforeOrderParam()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000" )
            .attributeOrders( List.of( OrderCriteria.of( "toUpdate000", SortDirection.ASC ) ) )
            .orders( List.of( new OrderParam( "toUpdate000", SortDirection.ASC ),
                new OrderParam( "enrolledAt", SortDirection.ASC ) ) )
            .build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void shouldSortEntitiesRespectingOrderWhenOrderParamSuppliedBeforeAttributeOrder()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .filterAttributes( "toUpdate000" )
            .attributeOrders( List.of( OrderCriteria.of( "toUpdate000", SortDirection.DESC ) ) )
            .orders( List.of( new OrderParam( "enrolledAt", SortDirection.DESC ),
                new OrderParam( "toUpdate000", SortDirection.DESC ) ) )
            .build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
    }

    @Test
    void shouldSortEntitiesRespectingOrderWhenDataElementSuppliedBeforeOrderParam()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .orders( List.of( new OrderParam( "dueDate", SortDirection.DESC ),
                new OrderParam( "DATAEL00006", SortDirection.DESC ),
                new OrderParam( "enrolledAt", SortDirection.DESC ) ) )
            .build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "QS6w44flWAf", "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    void shouldSortEntitiesRespectingOrderWhenOrderParamSuppliedBeforeDataElement()
        throws ForbiddenException,
        BadRequestException
    {
        EventOperationParams params = EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .orders( List.of( new OrderParam( "enrolledAt", SortDirection.DESC ),
                new OrderParam( "DATAEL00006", SortDirection.DESC ) ) )
            .build();

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( event -> event.getEnrollment().getTrackedEntity().getUid() )
            .collect( Collectors.toList() );

        assertEquals( List.of( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
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

    private <T extends IdentifiableObject> T get( Class<T> type, String uid )
    {
        T t = manager.get( type, uid );
        assertNotNull( t, () -> String.format( "metadata with uid '%s' should have been created", uid ) );
        return t;
    }

    private static List<String> eventUids( Events events )
    {
        return events.getEvents()
            .stream().map( Event::getUid ).collect( Collectors.toList() );
    }

    private static void assertSlimPager( int pageNumber, int pageSize, boolean isLast, Events events )
    {
        assertInstanceOf( SlimPager.class, events.getPager(), "SlimPager should be returned if totalPages=false" );
        SlimPager pager = (SlimPager) events.getPager();
        assertAll( "pagination details",
            () -> assertEquals( pageNumber, pager.getPage(), "number of current page" ),
            () -> assertEquals( pageSize, pager.getPageSize(), "page size" ),
            () -> assertEquals( isLast, pager.isLastPage(),
                isLast ? "should be the last page" : "should NOT be the last page" ) );
    }

    private static void assertPager( int pageNumber, int pageSize, int totalCount, Events events )
    {
        Pager pager = events.getPager();
        assertAll( "pagination details",
            () -> assertEquals( pageNumber, pager.getPage(), "number of current page" ),
            () -> assertEquals( pageSize, pager.getPageSize(), "page size" ),
            () -> assertEquals( totalCount, pager.getTotal(), "total page count" ) );
    }
}
