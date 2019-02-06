package org.hisp.dhis.tracker.preheat;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerPreheatServiceTest
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
    private TrackerPreheatService trackerPreheatService;

    @Override
    protected void setUpTest()
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    public void testEventMetadata() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "tracker/event_metadata.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertTrue( validationReport.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );
    }

    @Test
    public void testCollectIdentifiersSimple() throws IOException
    {
        TrackerBundleParams params = new TrackerBundleParams();
        Map<Class<?>, Map<TrackerIdentifier, Set<String>>> collectedMap = TrackerIdentifierCollector.collect( params );
        assertTrue( collectedMap.isEmpty() );
    }

    @Test
    public void testCollectIdentifiersEvents() throws IOException
    {
        TrackerBundleParams params = renderService.fromJson( new ClassPathResource( "tracker/event_events.json" ).getInputStream(),
            TrackerBundleParams.class );

        assertTrue( params.getTrackedEntities().isEmpty() );
        assertTrue( params.getEnrollments().isEmpty() );
        assertFalse( params.getEvents().isEmpty() );

        Map<Class<?>, Map<TrackerIdentifier, Set<String>>> collectedMap = TrackerIdentifierCollector.collect( params );

        assertTrue( collectedMap.containsKey( DataElement.class ) );
        assertTrue( collectedMap.get( DataElement.class ).containsKey( TrackerIdentifier.UID ) );

        Set<String> dataElements = collectedMap.get( DataElement.class ).get( TrackerIdentifier.UID );

        assertTrue( dataElements.contains( "DSKTW8qFP0z" ) );
        assertTrue( dataElements.contains( "VD2olWcRozZ" ) );
        assertTrue( dataElements.contains( "WS3e6pInnuA" ) );
        assertTrue( dataElements.contains( "h7hXjNVMiRl" ) );
        assertTrue( dataElements.contains( "KCLriKKezWO" ) );
        assertTrue( dataElements.contains( "A8qxpcalLtf" ) );
        assertTrue( dataElements.contains( "zal5vkVEpV0" ) );
        assertTrue( dataElements.contains( "kAfSfT69m0E" ) );
        assertTrue( dataElements.contains( "wIUShyK7lIa" ) );
        assertTrue( dataElements.contains( "ICfA0klVxkd" ) );
        assertTrue( dataElements.contains( "xaPkxL28aPx" ) );
        assertTrue( dataElements.contains( "Kvm949EFjjv" ) );
        assertTrue( dataElements.contains( "xaQ4gqDYGL9" ) );
        assertTrue( dataElements.contains( "MqGMwzt7M7w" ) );
        assertTrue( dataElements.contains( "WdTLfnn4S1I" ) );
        assertTrue( dataElements.contains( "hxUvZqnmBDv" ) );
        assertTrue( dataElements.contains( "JXF90RhgNiI" ) );
        assertTrue( dataElements.contains( "gfEoDU4GtXK" ) );
        assertTrue( dataElements.contains( "qw67QlOlzdp" ) );

        assertTrue( collectedMap.containsKey( Program.class ) );
        assertTrue( collectedMap.containsKey( ProgramStage.class ) );
        assertTrue( collectedMap.containsKey( OrganisationUnit.class ) );
    }

    @Test
    public void testPreheatValidation() throws IOException
    {
        TrackerBundle bundle = renderService.fromJson( new ClassPathResource( "tracker/event_events.json" ).getInputStream(),
            TrackerBundleParams.class ).toTrackerBundle();

        assertTrue( bundle.getTrackedEntities().isEmpty() );
        assertTrue( bundle.getEnrollments().isEmpty() );
        assertFalse( bundle.getEvents().isEmpty() );

        TrackerPreheatParams params = new TrackerPreheatParams();
        trackerPreheatService.validate( params );
    }

    @Test
    public void testPreheatEvents() throws IOException
    {
        TrackerBundle bundle = renderService.fromJson( new ClassPathResource( "tracker/event_events.json" ).getInputStream(),
            TrackerBundleParams.class ).toTrackerBundle();

        assertTrue( bundle.getTrackedEntities().isEmpty() );
        assertTrue( bundle.getEnrollments().isEmpty() );
        assertFalse( bundle.getEvents().isEmpty() );

        TrackerPreheatParams params = new TrackerPreheatParams()
            .setTrackedEntities( bundle.getTrackedEntities() )
            .setEnrollments( bundle.getEnrollments() )
            .setEvents( bundle.getEvents() );

        trackerPreheatService.validate( params );

        TrackerPreheat preheat = trackerPreheatService.preheat( params );

        assertNotNull( preheat );
    }
}
