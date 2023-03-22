/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
public class EventExporterTest
    extends TrackerTest
{
    @Autowired
    private EventService eventService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    private OrganisationUnit orgUnit;

    final Function<EventSearchParams, List<String>> eventsFunction = ( params ) -> eventService.getEvents( params )
        .getEvents()
        .stream().map( Event::getEvent ).collect( Collectors.toList() );

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        User superuser = userService.getUser( "M5zQapPyTZI" );
        assertNoImportErrors(
            trackerImportService.importTracker( fromJson( "tracker/event_and_enrollment.json", superuser.getUid() ) ) );
        orgUnit = manager.get( OrganisationUnit.class, "h4w96yEMlzO" );

        // to test that events are only returned if the user has read access to
        // ALL COs of an events COC
        CategoryOption co1 = get( CategoryOption.class, "OUUdG3sdOqb" );
        co1.getSharing().setOwner( superuser.getUid() );
        manager.update( co1 );
        CategoryOption co2 = get( CategoryOption.class, "yMj2MnmNI8L" );
        co2.getSharing().setOwner( "o1HMTIzBGo7" );
        manager.update( co2 );

        manager.flush();
    }

    @Test
    public void testExportEvents()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );

        List<String> events = eventsFunction.apply( params );

        assertNotNull( events );
        assertContainsOnly( events, "pTzf9KYMk72", "D9PbzJY8bJM" );
    }

    @Test
    public void testExportEventsWhenFilteringByEnrollment()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "nxP7UnKhomJ" ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( events, "pTzf9KYMk72" );
    }

    @Test
    public void shouldReturnNoEventsWhenParamStartDueDateLaterThanEventDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateStart( parseDate( "2021-02-28T13:05:00.000" ) );

        Events events = eventService.getEvents( params );

        assertNotNull( events );
        assertIsEmpty( events.getEvents() );
    }

    @Test
    public void shouldReturnEventsWhenParamStartDueDateEarlierThanEventsDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateStart( parseDate( "2018-02-28T13:05:00.000" ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( events, "D9PbzJY8bJM", "pTzf9KYMk72" );
    }

    @Test
    public void shouldReturnNoEventsWhenParamEndDueDateEarlierThanEventDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateEnd( parseDate( "2018-02-28T13:05:00.000" ) );

        Events events = eventService.getEvents( params );

        assertNotNull( events );
        assertIsEmpty( events.getEvents() );
    }

    @Test
    public void shouldReturnEventsWhenParamEndDueDateLaterThanEventsDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateEnd( parseDate( "2021-02-28T13:05:00.000" ) );

        List<String> events = eventsFunction.apply( params );

        assertContainsOnly( events, "D9PbzJY8bJM", "pTzf9KYMk72" );
    }

    @Test
    public void shouldReturnEventsNonSuperUserIsOwnerOrHasUserAccess()
    {
        // given events have a COC which has a CO which the
        // user owns yMj2MnmNI8L and has user read access to OUUdG3sdOqb
        injectSecurityContext( userService.getUser( "o1HMTIzBGo7" ) );

        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( get( OrganisationUnit.class, "DiszpKrYNg8" ) );
        params.setEvents( Sets.newHashSet( "lumVtWwwy0O", "cadc5eGj0j7" ) );

        Events events = eventService.getEvents( params );

        assertContainsOnly( eventUids( events ), "lumVtWwwy0O", "cadc5eGj0j7" );
        events.getEvents().forEach( e -> assertEquals(
            String.format( "got category options %s", e.getAttributeCategoryOptions() ), 2, e.getOptionSize() ) );
    }

    @Test
    public void shouldReturnNoEventsGivenUserHasNoAccess()
    {
        // given events have a COC which has a CO (OUUdG3sdOqb/yMj2MnmNI8L)
        // which are not publicly readable, user is not the owner and has no
        // user access
        injectSecurityContext( userService.getUser( "CYVgFNKCaUS" ) );

        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( get( OrganisationUnit.class, "DiszpKrYNg8" ) );
        params.setEvents( Sets.newHashSet( "lumVtWwwy0O", "cadc5eGj0j7" ) );

        List<String> events = eventsFunction.apply( params );

        assertIsEmpty( events );
    }

    @Test
    public void shouldReturnPublicEventsWithMultipleCategoryOptionsGivenNonDefaultPageSize()
    {
        OrganisationUnit orgUnit = get( OrganisationUnit.class, "DiszpKrYNg8" );
        Program program = get( Program.class, "iS7eutanDry" );

        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgram( program );

        params.setOrders( Lists.newArrayList( orderBy( "occurredAt", OrderParam.SortDirection.DESC ) ) );
        params.setPage( 1 );
        params.setPageSize( 3 );

        Events firstPage = eventService.getEvents( params );

        // first page
        assertSlimPager( 1, 3, false, firstPage );
        assertEquals( Lists.newArrayList( "ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp" ), eventUids( firstPage ) );

        params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgram( program );

        params.setOrders( Lists.newArrayList( orderBy( "occurredAt", OrderParam.SortDirection.DESC ) ) );
        params.setPage( 2 );
        params.setPageSize( 3 );

        Events secondPage = eventService.getEvents( params );

        // second (last) page
        assertSlimPager( 2, 3, true, secondPage );
        assertEquals( Lists.newArrayList( "lumVtWwwy0O", "QRYjLTiJTrA", "cadc5eGj0j7" ), eventUids( secondPage ) );

        params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgram( program );

        params.setOrders( Lists.newArrayList( orderBy( "occurredAt", OrderParam.SortDirection.DESC ) ) );
        params.setPage( 3 );
        params.setPageSize( 3 );

        assertIsEmpty( eventsFunction.apply( params ) );
    }

    private static OrderParam orderBy( String field, OrderParam.SortDirection direction )
    {
        return OrderParam.builder().field( field ).direction( direction ).build();
    }

    @Test
    public void shouldReturnEventsWithMultipleCategoryOptionsGivenNonDefaultPageSizeAndTotalPages()
    {
        OrganisationUnit orgUnit = get( OrganisationUnit.class, "DiszpKrYNg8" );
        Program program = get( Program.class, "iS7eutanDry" );

        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgram( program );

        params.setOrders( Lists.newArrayList( orderBy( "occurredAt", OrderParam.SortDirection.DESC ) ) );
        params.setPage( 1 );
        params.setPageSize( 2 );
        params.setTotalPages( true );

        Events events = eventService.getEvents( params );

        assertPager( 1, 2, 6, events );
        assertEquals( Lists.newArrayList( "ck7DzdxqLqA", "OTmjvJDn0Fu" ), eventUids( events ) );
    }

    @Test
    public void shouldReturnEventsGivenCategoryOptionCombo()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( get( OrganisationUnit.class, "DiszpKrYNg8" ) );
        params.setCategoryOptionCombo( get( CategoryOptionCombo.class, "cr89ebDZrac" ) );

        Events events = eventService.getEvents( params );

        // all events should have the same category option combo and options
        assertContainsOnly( eventUids( events ), "kWjSezkXHVp", "OTmjvJDn0Fu" );
        events.getEvents().forEach( e -> {
            assertEquals( "cr89ebDZrac", e.getAttributeOptionCombo() );
            assertEquals( "xwZ2u3WyQR0;M58XdOfhiJ7", e.getAttributeCategoryOptions() );
            assertEquals( String.format( "got category options %s", e.getAttributeCategoryOptions() ), 2,
                e.getOptionSize() );
        } );
    }

    @Test
    public void shouldFailIfCategoryOptionComboOfGivenEventDoesNotHaveAValueForGivenIdScheme()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( get( OrganisationUnit.class, "DiszpKrYNg8" ) );
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setCategoryOptionComboIdScheme( "ATTRIBUTE:GOLswS44mh8" );
        params.setIdSchemes( idSchemes );
        params.setEvents( Sets.newHashSet( "kWjSezkXHVp" ) );

        IllegalStateException ex = assertThrows( IllegalStateException.class, () -> eventService.getEvents( params ) );
        assertStartsWith( "CategoryOptionCombo", ex.getMessage() );
        assertContains( "not have a value assigned for idScheme ATTRIBUTE:GOLswS44mh8", ex.getMessage() );
    }

    @Test
    public void shouldReturnEventsGivenIdSchemeCode()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( get( OrganisationUnit.class, "DiszpKrYNg8" ) );
        params.setCategoryOptionCombo( get( CategoryOptionCombo.class, "cr89ebDZrac" ) );
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setProgramIdScheme( "code" );
        idSchemes.setProgramStageIdScheme( "code" );
        idSchemes.setOrgUnitIdScheme( "code" );
        idSchemes.setCategoryOptionComboIdScheme( "code" );
        params.setIdSchemes( idSchemes );

        Events events = eventService.getEvents( params );

        assertContainsOnly( eventUids( events ), "kWjSezkXHVp", "OTmjvJDn0Fu" );
        // all events should have the same category option combo and options
        events.getEvents().forEach( e -> {
            assertEquals( "multi-program", e.getProgram() );
            assertEquals( "multi-stage", e.getProgramStage() );
            assertEquals( "DiszpKrYNg8", e.getOrgUnit() ); // TODO(ivo):
                                                           // this
                                                           // might be
                                                           // a bug
                                                           // caused
                                                           // by
                                                           // https://github.com/dhis2/dhis2-core/pull/12518
            assertEquals( "COC_1153452", e.getAttributeOptionCombo() );
            assertEquals( "xwZ2u3WyQR0;M58XdOfhiJ7", e.getAttributeCategoryOptions() );
        } );
    }

    @Test
    public void shouldReturnEventsGivenIdSchemeAttribute()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( get( OrganisationUnit.class, "DiszpKrYNg8" ) );
        params.setCategoryOptionCombo( get( CategoryOptionCombo.class, "cr89ebDZrac" ) );
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setProgramIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        idSchemes.setProgramStageIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        idSchemes.setOrgUnitIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        idSchemes.setCategoryOptionComboIdScheme( "ATTRIBUTE:j45AR9cBQKc" );
        params.setIdSchemes( idSchemes );

        Events events = eventService.getEvents( params );

        assertContainsOnly( eventUids( events ), "kWjSezkXHVp", "OTmjvJDn0Fu" );
        // all events should have the same category option combo and options
        events.getEvents().forEach( e -> {
            assertEquals( "multi-program-attribute", e.getProgram() );
            assertEquals( "multi-program-stage-attribute", e.getProgramStage() );
            assertEquals( "DiszpKrYNg8", e.getOrgUnit() ); // TODO(ivo):
            // this
            // might be
            // a bug
            // caused
            // by
            // https://github.com/dhis2/dhis2-core/pull/12518
            assertEquals( "COC_1153452-attribute", e.getAttributeOptionCombo() );
            assertEquals( "xwZ2u3WyQR0;M58XdOfhiJ7", e.getAttributeCategoryOptions() );
        } );
    }

    @Test
    public void shouldSortEntitiesRespectingOrderWhenOrderParamSuppliedBeforeDataElement()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.addDataElements( Collections.singletonList( queryItem( "DATAEL00001" ) ) );

        params.setGridOrders(
            Collections.singletonList(
                OrderParam.builder().field( "DATAEL00001" ).direction( OrderParam.SortDirection.ASC ).build() ) );
        params.setOrders(
            Arrays.asList( OrderParam.builder().field( "status" ).direction( OrderParam.SortDirection.DESC ).build(),
                OrderParam.builder().field( "DATAEL00001" ).direction( OrderParam.SortDirection.ASC ).build() ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertEquals( Arrays.asList( "QS6w44flWAf", "dUE514NMOlo" ), trackedEntities );
    }

    @Test
    public void shouldSortEntitiesRespectingOrderWhenDataElementSuppliedBeforeOrderParam()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.addDataElements( Collections.singletonList( queryItem( "DATAEL00002" ) ) );

        params.setGridOrders(
            Collections.singletonList(
                OrderParam.builder().field( "DATAEL00002" ).direction( OrderParam.SortDirection.ASC ).build() ) );
        params.setOrders(
            Arrays.asList(
                OrderParam.builder().field( "DATAEL00002" ).direction( OrderParam.SortDirection.DESC ).build(),
                OrderParam.builder().field( "storedBy" ).direction( OrderParam.SortDirection.ASC ).build() ) );

        List<String> trackedEntities = eventService.getEvents( params ).getEvents().stream()
            .map( Event::getTrackedEntityInstance )
            .collect( Collectors.toList() );

        assertEquals( Arrays.asList( "dUE514NMOlo", "QS6w44flWAf" ), trackedEntities );
    }

    private static QueryItem queryItem( String teaUid )
    {
        TrackedEntityAttribute at = new TrackedEntityAttribute();
        at.setUid( teaUid );
        at.setValueType( ValueType.TEXT );
        at.setAggregationType( AggregationType.NONE );
        return new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(),
            at.isUnique() );
    }

    private <T extends IdentifiableObject> T get( Class<T> type, String uid )
    {
        T t = manager.get( type, uid );
        assertNotNull( String.format( "metadata with uid '%s' should have been created", uid ), t );
        return t;
    }

    private static List<String> eventUids( Events events )
    {
        return events.getEvents()
            .stream().map( Event::getEvent ).collect( Collectors.toList() );
    }

    private static void assertSlimPager( int pageNumber, int pageSize, boolean isLast, Events events )
    {
        assertTrue( "SlimPager should be returned if totalPages=false", events.getPager() instanceof SlimPager );
        SlimPager pager = (SlimPager) events.getPager();
        // pagination details
        assertEquals( "number of current page", pageNumber, pager.getPage() );
        assertEquals( "page size", pageSize, pager.getPageSize() );
        assertEquals( isLast ? "should be the last page" : "should NOT be the last page", isLast, pager.isLastPage() );
    }

    private static void assertPager( int pageNumber, int pageSize, int totalCount, Events events )
    {
        Pager pager = events.getPager();
        // pagination details
        assertEquals( "number of current page", pageNumber, pager.getPage() );
        assertEquals( "page size", pageSize, pager.getPageSize() );
        assertEquals( "total page count", totalCount, pager.getTotal() );
    }
}