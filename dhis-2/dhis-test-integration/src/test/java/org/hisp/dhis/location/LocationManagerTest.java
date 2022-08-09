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
package org.hisp.dhis.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Slf4j
class LocationManagerTest extends SingleSetupIntegrationTestBase
{

    private InputStream in;

    private OutputStream out;

    private File file;

    @Autowired
    private LocationManager locationManager;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
    {
        setExternalTestDir( locationManager );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // InputStream
    // -------------------------------------------------------------------------
    @Test
    void testGetInputStream()
    {
        try
        {
            in = locationManager.getInputStream( "test.properties" );
            assertNotNull( in );
        }
        catch ( LocationManagerException ex )
        {
            // External directory not set or the file does not exist
        }
        try
        {
            in = locationManager.getInputStream( "doesnotexist.properties" );
            fail();
        }
        catch ( Exception ex )
        {
            assertEquals( LocationManagerException.class, ex.getClass() );
        }
    }

    @Test
    void testInputStreamWithDirs()
    {
        try
        {
            in = locationManager.getInputStream( "test.properties", "test", "dir" );
            assertNotNull( in );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set or the file does not exist" );
        }
        try
        {
            in = locationManager.getInputStream( "doesnotexist.properties", "does", "not", "exist" );
            fail();
        }
        catch ( Exception ex )
        {
            assertEquals( LocationManagerException.class, ex.getClass() );
        }
    }

    // -------------------------------------------------------------------------
    // File for reading
    // -------------------------------------------------------------------------
    @Test
    void testGetFileForReading()
    {
        try
        {
            file = locationManager.getFileForReading( "test.properties" );
            assertTrue( file.getAbsolutePath().length() > 0 );
            log.debug( "File for reading: " + file.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set or the file does not exist" );
        }
        try
        {
            file = locationManager.getFileForReading( "doesnotexist.properties" );
            fail();
        }
        catch ( Exception ex )
        {
            assertEquals( LocationManagerException.class, ex.getClass() );
        }
    }

    @Test
    void testGetFileForReadingWithDirs()
    {
        try
        {
            file = locationManager.getFileForReading( "test.properties", "test", "dir" );
            assertTrue( file.getAbsolutePath().length() > 0 );
            log.debug( "File for reading with dirs: " + file.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set or the file does not exist" );
        }
        try
        {
            file = locationManager.getFileForReading( "doesnotexist.properties", "does", "not", "exist" );
            fail();
        }
        catch ( Exception ex )
        {
            assertEquals( LocationManagerException.class, ex.getClass() );
        }
    }

    public void testBuildDirectory()
    {
        try
        {
            File dir = locationManager.buildDirectory( "test", "dir" );
            log.debug( "Built directory: " + dir.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
    }

    // -------------------------------------------------------------------------
    // OutputStream
    // -------------------------------------------------------------------------
    @Test
    void testGetOutputStream()
    {
        try
        {
            out = locationManager.getOutputStream( "test.properties" );
            assertNotNull( out );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
    }

    @Test
    void testGetOutputStreamWithDirs()
    {
        try
        {
            out = locationManager.getOutputStream( "test.properties", "test" );
            assertNotNull( out );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
    }

    // -------------------------------------------------------------------------
    // File for writing
    // -------------------------------------------------------------------------
    @Test
    void testGetFileForWriting()
    {
        try
        {
            file = locationManager.getFileForWriting( "test.properties" );
            assertTrue( file.getAbsolutePath().length() > 0 );
            log.debug( "File for writing: " + file.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
        try
        {
            file = locationManager.getFileForWriting( "doesnotexist.properties" );
            assertTrue( file.getAbsolutePath().length() > 0 );
            log.debug( "File for writing which does not exist: " + file.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
    }

    @Test
    void testGetFileForWritingWithDirs()
    {
        try
        {
            file = locationManager.getFileForWriting( "test.properties", "test" );
            assertTrue( file.getAbsolutePath().length() > 0 );
            log.debug( "File for writing with dirs: " + file.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
        try
        {
            file = locationManager.getFileForWriting( "doesnotexist.properties", "does", "not", "exist" );
            assertTrue( file.getAbsolutePath().length() > 0 );
            log.debug( "File for writing with dirs which does not exist: " + file.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
    }

    // -------------------------------------------------------------------------
    // External directory and environment variables
    // -------------------------------------------------------------------------
    @Test
    void testGetExternalDirectory()
    {
        try
        {
            file = locationManager.getExternalDirectory();
            assertTrue( file.getAbsolutePath().length() > 0 );
            log.debug( "External directory: " + file.getAbsolutePath() );
        }
        catch ( LocationManagerException ex )
        {
            log.debug( "External directory not set" );
        }
    }

    @Test
    void testExternalDirectorySet()
    {
        boolean set = locationManager.externalDirectorySet();
        if ( set )
        {
            log.debug( "External directory set" );
        }
        else
        {
            log.debug( "External directory not set" );
        }
    }

    @Test
    void testGetEnvironmentVariable()
    {
        String env = locationManager.getEnvironmentVariable();
        log.debug( "Environment variable: " + env );
    }
}
