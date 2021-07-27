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
package org.hisp.dhis.tracker.validation;

import java.io.IOException;
import java.io.InputStream;

import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractImportValidationTest
    extends TrackerTest
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

    public static final String USER_1 = "oZZ43IHxuUM";

    public static final String USER_2 = "MbkW4Bhfw7o";

    public static final String USER_3 = "fY22f5N1xSy";

    public static final String USER_4 = "iAji3gyZYQL";

    public static final String USER_5 = "oajYcE7VMBs";

    public static final String USER_6 = "VfaA5WwHLdP";

    protected TrackerImportParams createBundleFromJson( String jsonFile )
        throws IOException
    {
        InputStream inputStream = new ClassPathResource( jsonFile ).getInputStream();

        TrackerImportParams params = renderService.fromJson( inputStream, TrackerImportParams.class );

        User user = userService.getUser( ADMIN_USER_UID );
        params.setUser( user );

        return params;
    }

    @Override
    protected void initTest()
        throws IOException
    {
    }
}
