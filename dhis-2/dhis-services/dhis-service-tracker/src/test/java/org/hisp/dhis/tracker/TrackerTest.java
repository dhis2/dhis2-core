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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Luciano Fiandesio
 */
public abstract class TrackerTest extends TransactionalIntegrationTest
{
    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Autowired
    protected UserService _userService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Override
    protected void setUpTest()
        throws IOException
    {

        preCreateInjectAdminUserWithoutPersistence();

        renderService = _renderService;
        userService = _userService;

        initTest();

        // Clear the session to simulate different API call after the setup
        manager.clear();
    }

    protected abstract void initTest()
        throws IOException;

    protected ObjectBundle setUpMetadata( String path )
        throws IOException
    {

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( path ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        assertNoErrors( objectBundleValidationService.validate( bundle ) );

        objectBundleService.commit( bundle );

        return bundle;
    }

    protected TrackerImportParams fromJson( String path )
        throws IOException
    {
        TrackerImportParams trackerImportParams = _fromJson( path );
        trackerImportParams.setUser( currentUserService.getCurrentUser() );
        return trackerImportParams;
    }

    protected TrackerImportParams fromJson( String path, String userUid )
        throws IOException
    {
        TrackerImportParams trackerImportParams = _fromJson( path );
        trackerImportParams.setUserId( userUid );
        return trackerImportParams;
    }

    protected TrackerImportParams fromJson( String path, User user )
        throws IOException
    {
        TrackerImportParams trackerImportParams = _fromJson( path );
        trackerImportParams.setUser( user );
        return trackerImportParams;
    }

    protected TrackerImportParams _fromJson( String path )
        throws IOException
    {
        return renderService.fromJson( new ClassPathResource( path ).getInputStream(), TrackerImportParams.class );
    }

    protected void assertNoImportErrors( TrackerImportReport report )
    {
        assertTrue( report.getValidationReport().getErrorReports().isEmpty() );
    }

    public static void assertNoErrors( ObjectBundleValidationReport report )
    {
        assertNotNull( report );
        List<String> errors = new ArrayList<>();
        report.forEachErrorReport( err -> errors.add( err.toString() ) );
        assertFalse( String.format( "Expected no errors, instead got: %s\n", errors ), report.hasErrorReports() );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
}