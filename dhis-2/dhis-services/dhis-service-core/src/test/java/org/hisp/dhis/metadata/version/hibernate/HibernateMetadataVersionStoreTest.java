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

package org.hisp.dhis.metadata.version.hibernate;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionStore;
import org.hisp.dhis.metadata.version.VersionType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;


/**
 * @author sultanm.
 */

public class HibernateMetadataVersionStoreTest extends DhisSpringTest
{
    @Autowired
    private MetadataVersionStore metadataVersionStore;

    public void testGetInitialVersion() throws Exception
    {
        MetadataVersion metadataVersion1 = new MetadataVersion( "version1", VersionType.ATOMIC );
        metadataVersion1.setHashCode( "12232" );
        metadataVersionStore.save( metadataVersion1 );
        MetadataVersion metadataVersion2 = new MetadataVersion( "version2", VersionType.ATOMIC );
        metadataVersion2.setHashCode( "12222" );
        metadataVersionStore.save( metadataVersion2 );
        assertEquals( metadataVersion1, metadataVersionStore.getInitialVersion() );
        metadataVersionStore.delete( metadataVersion1 );
        metadataVersionStore.delete( metadataVersion2 );
        assertNull( metadataVersionStore.getInitialVersion() );
    }

    @Test
    public void testGetVersionByKey() throws Exception
    {
        MetadataVersion metadataVersion = new MetadataVersion( "version1", VersionType.ATOMIC );
        metadataVersion.setHashCode( "12345" );
        metadataVersionStore.save( metadataVersion );

        assertEquals( metadataVersion, metadataVersionStore.getVersionByKey( metadataVersionStore.getCurrentVersion().getId() ) );
        int nonExistingId = 9999;

        assertNull( metadataVersionStore.getVersionByKey( nonExistingId ) );

        metadataVersionStore.delete( metadataVersion );
    }

    @Test
    public void testGetVersionByName() throws Exception
    {
        MetadataVersion metadataVersion = new MetadataVersion( "version1", VersionType.ATOMIC );
        metadataVersion.setHashCode( "12345" );
        metadataVersionStore.save( metadataVersion );
        assertEquals( metadataVersion, metadataVersionStore.getVersionByName( "version1" ) );
        assertNull( metadataVersionStore.getVersionByName( "non_existing" ) );
        metadataVersionStore.delete( metadataVersion );
    }

    @Test
    public void testGetAllVersionsInBetween() throws Exception
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ssZ" );
        org.joda.time.DateTime dateTime1 = dateTimeFormatter.parseDateTime( "2016-06-20 10:45:50Z" );
        org.joda.time.DateTime dateTime2 = dateTimeFormatter.parseDateTime( "2016-06-21 10:45:50Z" );
        org.joda.time.DateTime dateTime3 = dateTimeFormatter.parseDateTime( "2016-06-22 10:45:50Z" );

        assertEquals( 0, metadataVersionStore.getAllVersionsInBetween( new Date(), new Date() ).size() );

        MetadataVersion metadataVersion2 = new MetadataVersion( "version2", VersionType.ATOMIC );
        metadataVersion2.setHashCode( "12222" );
        metadataVersion2.setCreated( dateTime1.toDate() );
        metadataVersionStore.save( metadataVersion2 );

        MetadataVersion metadataVersion3 = new MetadataVersion( "version3", VersionType.ATOMIC );
        metadataVersion3.setHashCode( "12255" );
        metadataVersion3.setCreated( dateTime2.toDate() );
        metadataVersionStore.save( metadataVersion3 );

        MetadataVersion metadataVersion4 = new MetadataVersion( "version4", VersionType.ATOMIC );
        metadataVersion4.setHashCode( "12267" );
        metadataVersion4.setCreated( dateTime3.toDate() );
        metadataVersionStore.save( metadataVersion4 );

        List<MetadataVersion> allVersionsInBetween = metadataVersionStore.getAllVersionsInBetween( dateTime1.toDate(), dateTime2.toDate() );

        assertEquals( 2, allVersionsInBetween.size() );
        assertEquals( metadataVersion2, allVersionsInBetween.get( 0 ) );
        assertEquals( metadataVersion3, allVersionsInBetween.get( 1 ) );
        assertEquals( 0, metadataVersionStore.getAllVersionsInBetween( new Date(), new Date() ).size() );

        metadataVersionStore.delete( metadataVersion2 );
        metadataVersionStore.delete( metadataVersion3 );
        metadataVersionStore.delete( metadataVersion4 );
    }

    @Test
    public void testUpdate() throws Exception
    {
        MetadataVersion metadataVersion = new MetadataVersion( "version1", VersionType.ATOMIC );
        metadataVersion.setHashCode( "12345" );
        metadataVersionStore.save( metadataVersion );

        metadataVersion.setName( "newVersion" );
        metadataVersionStore.update( metadataVersion );

        assertNotNull( metadataVersionStore.getVersionByName( "newVersion" ) );
        metadataVersionStore.delete( metadataVersion );
    }

    @Test
    public void testDelete() throws Exception
    {
        assertNull( metadataVersionStore.getVersionByName( "version1" ) );
    }
}