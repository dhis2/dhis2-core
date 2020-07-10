package org.hisp.dhis.tracker.validation;

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
 *
 */

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractImportValidationTest
    extends DhisSpringTest
{
    @Autowired
    protected TrackerBundleService trackerBundleService;

    @Autowired
    protected ObjectBundleService objectBundleService;

    @Autowired
    protected ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    protected DefaultTrackerValidationService trackerValidationService;

    public static final String ADMIN_USER_UID = "M5zQapPyTZI";

    public static final String USER_1 = "---USER1---";

    public static final String USER_2 = "---USER2---";

    public static final String USER_3 = "---USER3---";

    public static final String USER_4 = "---USER4---";

    public static final String USER_5 = "---USER5---";

    public static final String USER_6 = "---USER6---";

    protected TrackerBundleParams createBundleFromJson( String jsonFile )
        throws IOException
    {
        InputStream inputStream = new ClassPathResource( jsonFile ).getInputStream();

        TrackerBundleParams params = renderService.fromJson( inputStream, TrackerBundleParams.class );

        User user = userService.getUser( ADMIN_USER_UID );
        params.setUser( user );

        return params;
    }

    protected void printReport( TrackerValidationReport report )
    {
        for ( TrackerErrorReport errorReport : report.getErrorReports() )
        {
            log.error( errorReport.toString() );
        }
    }

    protected ValidateAndCommitTestUnit validateAndCommit( String jsonFileName, TrackerImportStrategy strategy )
        throws IOException
    {
        return ValidateAndCommitTestUnit.builder()
            .trackerBundleService( trackerBundleService )
            .trackerValidationService( trackerValidationService )
            .trackerBundleParams( createBundleFromJson( jsonFileName ) )
            .trackerImportStrategy( strategy )
            .build()
            .invoke();
    }

    protected ValidateAndCommitTestUnit validateAndCommit( TrackerBundleParams params, TrackerImportStrategy strategy )
    {
        return ValidateAndCommitTestUnit.builder()
            .trackerBundleService( trackerBundleService )
            .trackerValidationService( trackerValidationService )
            .trackerBundleParams( params )
            .trackerImportStrategy( strategy )
            .build()
            .invoke();
    }

    protected ValidateAndCommitTestUnit validateAndCommit( TrackerBundleParams params )
    {
        return validateAndCommit( params, TrackerImportStrategy.CREATE_AND_UPDATE );
    }
}
