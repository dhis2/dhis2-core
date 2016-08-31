package org.hisp.dhis.appmanager;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Saptarshi
 */
public class DefaultAppManagerServiceTest
{
    /**
     * Test of getAppFolderPath method, of class DefaultAppManagerService.
     */
    @Test
    @Ignore
    public void testGetAppFolderPath()
    {
        DefaultAppManager instance = new DefaultAppManager();
        String expResult = "";
        String result = instance.getAppFolderPath();
        assertEquals( expResult, result );
        // TODO review the generated test code and remove the default call to fail.
        fail( "The test case is a prototype." );
    }

    /**
     * Test of getAppStoreUrl method, of class DefaultAppManagerService.
     */
    @Test
    @Ignore
    public void testGetAppStoreUrl()
    {
        DefaultAppManager instance = new DefaultAppManager();
        String expResult = "";
        String result = instance.getAppStoreUrl();
        assertEquals( expResult, result );
        // TODO review the generated test code and remove the default call to fail.
        fail( "The test case is a prototype." );
    }

    /**
     * Test of getInstalledApps method, of class DefaultAppManagerService.
     */
    @Test
    @Ignore
    public void testGetInstalledApps()
    {
        DefaultAppManager instance = new DefaultAppManager();
        List<App> expResult = null;
        List<App> result = instance.getApps();
        assertEquals( expResult, result );
        // TODO review the generated test code and remove the default call to fail.
        fail( "The test case is a prototype." );
    }

    /**
     * Test of setAppFolderPath method, of class DefaultAppManagerService.
     */
    @Test
    @Ignore
    public void testSetAppFolderPath()
    {
        String appFolderPath = "";
        DefaultAppManager instance = new DefaultAppManager();
        instance.setAppFolderPath( appFolderPath );
        // TODO review the generated test code and remove the default call to fail.
        fail( "The test case is a prototype." );
    }

    /**
     * Test of setAppStoreUrl method, of class DefaultAppManagerService.
     */
    @Test
    @Ignore
    public void testSetAppStoreUrl()
    {
        String appStoreUrl = "";
        DefaultAppManager instance = new DefaultAppManager();
        instance.setAppStoreUrl( appStoreUrl );
        // TODO review the generated test code and remove the default call to fail.
        fail( "The test case is a prototype." );
    }
}