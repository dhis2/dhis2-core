<<<<<<< HEAD
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

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackedEntityAttributeTest
    extends IntegrationTestBase
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

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Override
    protected void setUpTest() throws IOException
    {
        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "tracker/te_with_tea_metadata.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertTrue( validationReport.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Test
    public void testTrackedAttributePreheater() throws IOException
    {
        TrackerBundle trackerBundle = renderService.fromJson( new ClassPathResource( "tracker/te_with_tea_data.json" ).getInputStream(),
            TrackerBundleParams.class ).toTrackerBundle();

        TrackerPreheatParams preheatParams = TrackerPreheatParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        TrackerPreheat preheat = trackerPreheatService.preheat( preheatParams );

        assertNotNull( preheat.get( TrackerIdScheme.UID, OrganisationUnit.class, "cNEZTkdAvmg" ) );
        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityType.class, "KrYIdvLxkMb" ) );

        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityAttribute.class, "sYn3tkL3XKa" ) );
        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityAttribute.class, "TsfP85GKsU5" ) );
        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityAttribute.class, "sTGqP5JNy6E" ) );
    }

    @Test
    public void testTrackedAttributeValueBundleImporter() throws IOException
    {
        TrackerBundle trackerBundle = renderService.fromJson( new ClassPathResource( "tracker/te_with_tea_data.json" ).getInputStream(),
            TrackerBundleParams.class ).toTrackerBundle();

        List<TrackerBundle> trackerBundles = trackerBundleService.create( TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build() );

        assertEquals( 1, trackerBundles.size() );

        trackerBundleService.commit( trackerBundles.get( 0 ) );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService.getTrackedEntityAttributeValues(
            trackedEntityInstance );
=======
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
package org.hisp.dhis.tracker.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackedEntityAttributeTest
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

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

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
            new ClassPathResource( "tracker/te_with_tea_metadata.json" ).getInputStream(), RenderFormat.JSON );

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
    public void testTrackedAttributePreheater()
        throws IOException
    {
        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/te_with_tea_data.json" ).getInputStream(),
                TrackerBundleParams.class )
            .toTrackerBundle();

        TrackerPreheatParams preheatParams = TrackerPreheatParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        TrackerPreheat preheat = trackerPreheatService.preheat( preheatParams );

        assertNotNull( preheat.get( TrackerIdScheme.UID, OrganisationUnit.class, "cNEZTkdAvmg" ) );
        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityType.class, "KrYIdvLxkMb" ) );

        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityAttribute.class, "sYn3tkL3XKa" ) );
        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityAttribute.class, "TsfP85GKsU5" ) );
        assertNotNull( preheat.get( TrackerIdScheme.UID, TrackedEntityAttribute.class, "sTGqP5JNy6E" ) );
    }

    @Test
    public void testTrackedAttributeValueBundleImporter()
        throws IOException
    {
        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/te_with_tea_data.json" ).getInputStream(),
                TrackerBundleParams.class )
            .toTrackerBundle();

        List<TrackerBundle> trackerBundles = trackerBundleService.create( TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build() );

        assertEquals( 1, trackerBundles.size() );

        trackerBundleService.commit( trackerBundles.get( 0 ) );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(
                trackedEntityInstance );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        assertEquals( 3, attributeValues.size() );
    }
}
