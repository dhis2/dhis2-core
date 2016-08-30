package org.hisp.dhis.version;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class VersionServiceTest
    extends DhisSpringTest
{
    @Autowired
    private VersionService versionService;

    private Version versionA;

    private Version versionB;

    @Override
    protected void setUpTest()
    {
        versionA = new Version();
        versionA.setKey( "keyA" );
        versionA.setValue( "valueA" );

        versionB = new Version();
        versionB.setKey( "keyB" );
        versionB.setValue( "valueB" );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddVersion()
    {
        int idA = versionService.addVersion( versionA );
        int idB = versionService.addVersion( versionB );

        assertTrue( idA >= 0 );
        assertTrue( idB >= 0 );

        versionA = versionService.getVersion( idA );
        versionB = versionService.getVersion( idB );

        assertNotNull( versionA );
        assertNotNull( versionB );

        assertEquals( "valueA", versionA.getValue() );
        assertEquals( "valueB", versionB.getValue() );
    }

    @Test
    public void testUpdateVersion()
    {
        int id = versionService.addVersion( versionA );
        versionService.updateVersion( "keyA", "changedValueA" );
        versionA = versionService.getVersion( id );

        assertNotNull( versionA );
        assertEquals( "changedValueA", versionA.getValue() );
    }

    @Test
    public void testDeleteVersion()
    {
        int id = versionService.addVersion( versionA );
        versionService.deleteVersion( versionA );
        assertNull( versionService.getVersion( id ) );
    }

    @Test
    public void testGetVersion()
    {
        int id = versionService.addVersion( versionA );
        versionA = versionService.getVersion( id );

        assertNotNull( versionA );
        assertEquals( "valueA", versionA.getValue() );
    }

    @Test
    public void testGetVersionByKey()
    {
        versionService.addVersion( versionA );
        versionA = versionService.getVersionByKey( "keyA" );

        assertNotNull( versionA );
        assertEquals( "valueA", versionA.getValue() );
    }

    @Test
    public void testGetAllVersions()
    {
        versionService.addVersion( versionA );
        versionService.addVersion( versionB );

        List<Version> versions = versionService.getAllVersions();

        assertNotNull( versions );
        assertEquals( 2, versions.size() );
    }
}
