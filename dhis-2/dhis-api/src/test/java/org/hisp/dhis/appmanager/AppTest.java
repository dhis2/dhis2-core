package org.hisp.dhis.appmanager;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Saptarshi
 * @version $Id$
 */
public class AppTest
{

    private App app;

    @Before
    public void setUp()
        throws IOException
    {
        String appJson = FileUtils.readFileToString( new File( this.getClass().getResource( "/manifest.webapp" )
            .getFile() ) );
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        this.app = mapper.readValue( appJson, App.class );
    }

    @After
    public void tearDown()
    {
    }

    // TODO: Verify missing property 
    @Test
    public void testRequiredProperties()
    {
        Assert.assertEquals( "0.1", app.getVersion() );
        Assert.assertEquals( "Test App", app.getName() );
        Assert.assertEquals( "/index.html", app.getLaunchPath() );
        Assert.assertEquals( "*", app.getInstallsAllowedFrom()[0] );
        Assert.assertEquals( "en", app.getDefaultLocale() );
    }

    // TODO: Complete test for skipped optional properties 
    @Test
    public void testOptionalProperties()
    {
        Assert.assertEquals( "Test Description", app.getDescription() );
    }

    @Test
    public void testIcons()
    {
        Assert.assertEquals( "/img/icons/mortar-16.png", app.getIcons().getIcon16() );
        Assert.assertEquals( "/img/icons/mortar-48.png", app.getIcons().getIcon48() );
        Assert.assertEquals( "/img/icons/mortar-128.png", app.getIcons().getIcon128() );
    }

    @Test
    public void testDeveloper()
    {
        Assert.assertEquals( "Test Developer", app.getDeveloper().getName() );
        Assert.assertEquals( "http://test", app.getDeveloper().getUrl() );
        Assert.assertNull( app.getDeveloper().getEmail() );
        Assert.assertNull( app.getDeveloper().getCompany() );
    }
    
    @Test
    public void testActivities()
    {
        AppDhis dhisActivity = app.getActivities().getDhis();
        Assert.assertEquals( "http://localhost:8080/dhis", dhisActivity.getHref() );
        dhisActivity.setHref("ALL TEST");
        Assert.assertEquals( "ALL TEST", dhisActivity.getHref() );
    }
    
}
