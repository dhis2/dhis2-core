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

import static com.google.common.collect.Sets.newHashSet;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

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

    private User userA;

    private OrganisationUnit orgUnit;

    private ProgramStage programStage;

    private Program program;

    private TrackedEntityInstance trackedEntityInstance;

    @Autowired
    private DataElementService dataElementService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        userA = userService.getUser( "M5zQapPyTZI" );
        assertNoErrors(
            trackerImportService.importTracker( fromJson( "tracker/event_and_enrollment.json", userA.getUid() ) ) );
        orgUnit = manager.get( OrganisationUnit.class, "h4w96yEMlzO" );
        programStage = manager.get( ProgramStage.class, "NpsdDv6kKSO" );
        program = programStage.getProgram();
        trackedEntityInstance = manager.get( TrackedEntityInstance.class, "IOR1AXXl24G" );
        manager.flush();
    }

    @Test
    void testExportEvents()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 2, events.getEvents().size() );
    }

    @Test
    void testExportEventsWithTotalPages()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setTotalPages( true );
        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 2, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByEnrollment()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8z" ) );
        params.setTrackedEntityInstance( trackedEntityInstance );
        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWithExecutionAndUpdateDates()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8z" ) );

        params.setStartDate( getDate( 2018, 1, 1 ) );
        params.setEndDate( getDate( 2020, 1, 1 ) );
        params.setSkipChangedBefore( getDate( 2018, 1, 1 ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsLike()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u" ) );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(),
                null, null ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithStatusFilter()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u" ) );
        params.setProgramStatus( ProgramStatus.ACTIVE );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(),
                null, null ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithProgramTypeFilter()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u" ) );
        params.setProgramType( ProgramType.WITH_REGISTRATION );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(),
                null, null ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsEqual()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u" ) );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( dataElement, QueryOperator.EQ, "value00001", dataElement.getValueType(),
                null,
                null ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsIn()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u", "TvctPPhpD8z" ) );

        DataElement datael00001 = dataElement( "DATAEL00001" );

        params.setDataElements( Set.of(
            new QueryItem( datael00001, QueryOperator.IN, "value00001;value00002", datael00001.getValueType(),
                null,
                null ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 2, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithCategoryOptionSuperUser()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u" ) );
        params.setProgramStage( programStage );
        params.setProgram( program );

        params.setCategoryOptionCombo( manager.get( CategoryOptionCombo.class, "HllvX50cXC0" ) );

        DataElement dataElement = dataElement( "DATAEL00001" );

        params
            .setDataElements(
                Set.of( new QueryItem( dataElement, QueryOperator.EQ, "value00001", dataElement.getValueType(),
                    null, dataElement.getOptionSet() ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithCategoryOptionNotSuperUser()
    {
        injectSecurityContext( createAndAddUser( false, "user", newHashSet( orgUnit ), newHashSet( orgUnit ),
            "F_EXPORT_DATA" ) );

        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8z" ) );
        params.setProgramStage( programStage );
        params.setProgram( program );

        params.setCategoryOptionCombo( manager.get( CategoryOptionCombo.class, "HllvX50cXC0" ) );

        DataElement dataElement = dataElement( "DATAEL00002" );

        params
            .setDataElements(
                Set.of( new QueryItem( dataElement, QueryOperator.EQ, "value00002", dataElement.getValueType(),
                    null, dataElement.getOptionSet() ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithOptionSetEqual()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u" ) );

        DataElement dataElement = dataElement( "DATAEL00005" );

        params.setDataElements(
            Set.of( new QueryItem( dataElement, QueryOperator.EQ, "option1", dataElement.getValueType(),
                null, dataElement.getOptionSet() ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    // TODO
    @Test
    void testExportEventsWhenFilteringByDataElementsWithOptionSetIn()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u", "TvctPPhpD8z" ) );

        DataElement dataElement = dataElement( "DATAEL00005" );

        params.setDataElements(
            Set.of( new QueryItem( dataElement, QueryOperator.IN, "option1;option2", dataElement.getValueType(),
                null, dataElement.getOptionSet() ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 2, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByDataElementsWithOptionSetLike()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8u" ) );

        DataElement dataElement = dataElement( "DATAEL00005" );

        params
            .setDataElements( Set.of( new QueryItem( dataElement, QueryOperator.LIKE, "opt", dataElement.getValueType(),
                null, dataElement.getOptionSet() ) ) );

        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    private DataElement dataElement( String uid )
    {
        return dataElementService.getDataElement( uid );
    }
}
