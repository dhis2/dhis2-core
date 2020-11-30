package org.hisp.dhis.tracker.bundle;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.*;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.hasItem;
import static org.hisp.dhis.tracker.utils.ImportUtils.build;
import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class EventDataValueTest
    extends DhisSpringTest
{
    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Override
    protected void setUpTest()
        throws IOException
    {
        preCreateInjectAdminUserWithoutPersistence();

        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "tracker/simple_metadata.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertTrue( validationReport.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        final User userA = userService.getUser("M5zQapPyTZI");

        InputStream inputStream = new ClassPathResource( "tracker/single_tei.json" ).getInputStream();

        TrackerBundleParams teiParams = renderService.fromJson( inputStream, TrackerBundleParams.class );
        params.setUser( userA );
        TrackerImportReport teiImportReport = trackerImportService.importTracker( build( teiParams ) );

        assertTrue( teiImportReport.getValidationReport().getErrorReports().isEmpty() );

        TrackerBundleParams enrollmentParams = renderService
            .fromJson( new ClassPathResource( "tracker/single_enrollment.json" ).getInputStream(),
                TrackerBundleParams.class );
        enrollmentParams.setUser( userA );
        TrackerImportReport enrollmentImportReport = trackerImportService.importTracker( build( enrollmentParams ) );
        assertTrue( enrollmentImportReport.getValidationReport().getErrorReports().isEmpty() );
    }

    @Test
    public void testEventDataValue()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/event_with_data_values.json" ).getInputStream(),
                TrackerBundleParams.class );

        TrackerBundle trackerBundle = trackerBundleService.create( TrackerBundleParams.builder()
            .trackedEntities( trackerBundleParams.getTrackedEntities() )
            .enrollments( trackerBundleParams.getEnrollments() )
            .events( trackerBundleParams.getEvents() )
            .build() );

        trackerBundleService.commit( trackerBundle );

        List<ProgramStageInstance> events = manager.getAll( ProgramStageInstance.class );
        assertEquals( 1, events.size() );

        ProgramStageInstance psi = events.get( 0 );

        Set<EventDataValue> eventDataValues = psi.getEventDataValues();

        assertEquals( 4, eventDataValues.size() );
    }

    @Test
    public void testTrackedEntityProgramAttributeValueUpdate()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/event_with_data_values.json" ).getInputStream(),
                TrackerBundleParams.class );

        TrackerBundle trackerBundle = trackerBundleService.create( TrackerBundleParams.builder()
            .trackedEntities( trackerBundleParams.getTrackedEntities() )
            .enrollments( trackerBundleParams.getEnrollments() )
            .events( trackerBundleParams.getEvents() )
            .build() );


        trackerBundleService.commit( trackerBundle );

        List<ProgramStageInstance> events = manager.getAll( ProgramStageInstance.class );
        assertEquals( 1, events.size() );

        ProgramStageInstance psi = events.get( 0 );

        Set<EventDataValue> eventDataValues = psi.getEventDataValues();

        assertEquals( 4, eventDataValues.size() );

        // update

        trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/event_with_updated_data_values.json" ).getInputStream(),
                TrackerBundleParams.class );

        trackerBundle = trackerBundleService.create( TrackerBundleParams.builder()
            .trackedEntities( trackerBundleParams.getTrackedEntities() )
            .enrollments( trackerBundleParams.getEnrollments() )
            .events( trackerBundleParams.getEvents() )
            .build() );

        trackerBundleService.commit( trackerBundle );

        List<ProgramStageInstance> updatedEvents = manager.getAll( ProgramStageInstance.class );
        assertEquals( 1, updatedEvents.size() );

        ProgramStageInstance updatedPsi = events.get( 0 );

        assertEquals( 3, updatedPsi.getEventDataValues().size() );
        List<String> values = updatedPsi.getEventDataValues()
            .stream()
            .map( EventDataValue::getValue )
            .collect( Collectors.toList() );

        assertThat( values, hasItem( "First" ) );
        assertThat( values, hasItem( "Second" ) );
        assertThat( values, hasItem( "Fourth updated" ) );

    }
}
