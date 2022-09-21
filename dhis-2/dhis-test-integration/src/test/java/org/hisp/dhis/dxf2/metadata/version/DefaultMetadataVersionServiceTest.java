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
package org.hisp.dhis.dxf2.metadata.version;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.MetadataDatastoreService;
import org.hisp.dhis.dxf2.metadata.systemsettings.MetadataSystemSettingService;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author sultanm
 */
class DefaultMetadataVersionServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private MetadataVersionService versionService;

    @Autowired
    private MetadataDatastoreService metaDataDatastoreService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private MetadataSystemSettingService metadataSystemSettingService;

    private MetadataVersion versionA;

    private MetadataVersion versionB;

    private Date startDate;

    private Date endDate;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Override
    protected void setUpTest()
    {
        startDate = new Date();
        versionA = new MetadataVersion( "Version_1", VersionType.ATOMIC );
        versionA.setHashCode( "12345" );
        versionA.setCreated( startDate );
        versionB = new MetadataVersion( "Version_2", VersionType.BEST_EFFORT );
        versionB.setCreated( DateUtils.addHours( startDate, 1 ) );
        versionB.setHashCode( "abcdef" );
        endDate = DateUtils.addDays( startDate, 1 );
    }

    @Test
    void testShouldAddVersions()
    {
        long idA = versionService.addVersion( versionA );

        assertTrue( idA >= 0 );
        assertEqualMetadataVersions( versionA, versionService.getVersionById( idA ) );

        long idB = versionService.addVersion( versionB );

        assertTrue( idB >= 0 );
        assertEqualMetadataVersions( versionB, versionService.getVersionById( idB ) );
    }

    @Test
    void testShouldDeleteAVersion()
    {
        long id = versionService.addVersion( versionA );

        versionService.deleteVersion( versionA );

        assertNull( versionService.getVersionById( id ) );
    }

    @Test
    void testShouldGetVersionsBasedOnIdOrName()
    {
        long idA = versionService.addVersion( versionA );

        assertEqualMetadataVersions( versionA, versionService.getVersionById( idA ) );

        versionService.addVersion( versionB );

        assertEqualMetadataVersions( versionB, versionService.getVersionByName( "Version_2" ) );
    }

    @Test
    void testShouldReturnTheLatestVersion()
    {
        versionService.addVersion( versionA );
        dbmsManager.clearSession();
        versionService.addVersion( versionB );

        assertEqualMetadataVersions( versionB, versionService.getCurrentVersion() );
    }

    @Test
    void testGetInitialVersion()
    {
        versionService.addVersion( versionA );
        dbmsManager.clearSession();
        versionService.addVersion( versionB );

        assertEquals( versionA, versionService.getInitialVersion() );
    }

    @Test
    void testShouldReturnVersionsBetweenGivenTimeStamps()
    {
        List<MetadataVersion> versions;
        versionService.addVersion( versionA );
        versions = versionService.getAllVersionsInBetween( startDate, endDate );

        assertEquals( 1, versions.size() );
        assertEqualMetadataVersions( versionA, versions.get( 0 ) );

        versionService.addVersion( versionB );
        versions = versionService.getAllVersionsInBetween( startDate, endDate );

        assertEquals( 2, versions.size() );
        assertEqualMetadataVersions( versionB, versions.get( 1 ) );

        Date dateBetweenAandB = DateUtils.addMinutes( versionA.getCreated(), 1 );
        versions = versionService.getAllVersionsInBetween( dateBetweenAandB, endDate );

        assertEquals( 1, versions.size() );
        assertEqualMetadataVersions( versionB, versions.get( 0 ) );
    }

    @Test
    void testShouldReturnAllVersionsInSystem()
    {
        assertEquals( 0, versionService.getAllVersions().size() );
        versionService.addVersion( versionA );
        versionService.addVersion( versionB );

        List<MetadataVersion> versions = versionService.getAllVersions();

        assertNotNull( versions );
        assertEquals( 2, versions.size() );
    }

    @Test
    void testShouldSaveVersionAndSnapShot()
    {
        versionService.addVersion( versionA );
        versionService.saveVersion( VersionType.ATOMIC );

        // testing if correct version is saved in metadataVersion table
        assertEquals( "Version_2", versionService.getCurrentVersion().getName() );
        assertEquals( VersionType.ATOMIC, versionService.getCurrentVersion().getType() );

        // testing if correct version name is saved in system setting
        assertEquals( "Version_2", metadataSystemSettingService.getSystemMetadataVersion() );

        // testing hash code for the given metadata string
        MetadataVersion metadataVersionSnap = versionService.getVersionByName( "Version_2" );
        assertEquals( metadataVersionSnap.getHashCode(), versionService.getCurrentVersion().getHashCode() );

        // testing if correct version is saved in keyjsonvalue table
        List<String> versions;
        versions = metaDataDatastoreService.getAllVersions();

        assertEquals( 1, versions.size() );
        assertEquals( "Version_2", versions.get( 0 ) );

        DataElement de1 = createDataElement( 'A' );
        manager.save( de1 );
        dbmsManager.clearSession();

        versionService.saveVersion( VersionType.BEST_EFFORT );
        DatastoreEntry expectedJson = metaDataDatastoreService.getMetaDataVersion( "Version_3" );
        List<String> allVersions = metaDataDatastoreService.getAllVersions();

        assertEquals( 2, allVersions.size() );
        assertEquals( "Version_3", allVersions.get( 1 ) );
        assertTrue( expectedJson.getJbPlainValue().contains( "DataElementA" ) );
    }

    @Test
    void testShouldCreateASnapshotThatContainsOnlyDelta()
    {
        versionService.addVersion( versionA );
        DataElement de1 = createDataElement( 'A' );
        manager.save( de1 );
        dbmsManager.clearSession();
        versionService.saveVersion( VersionType.BEST_EFFORT );
        de1 = createDataElement( 'B' );
        manager.save( de1 );
        dbmsManager.clearSession();
        versionService.saveVersion( VersionType.BEST_EFFORT );

        DatastoreEntry expectedJson = metaDataDatastoreService.getMetaDataVersion( "Version_3" );

        assertFalse( expectedJson.getJbPlainValue().contains( "DataElementA" ) );
        assertTrue( expectedJson.getJbPlainValue().contains( "DataElementB" ) );
    }

    @Test
    void testShouldGiveValidVersionDataIfExists()
    {
        versionService.createMetadataVersionInDataStore( "myVersion", "myJson" );

        assertEquals( "myJson", versionService.getVersionData( "myVersion" ) );
    }

    @Test
    void testShouldReturnNullWhenAVersionDoesNotExist()
    {
        assertNull( versionService.getVersionData( "myNonExistingVersion" ) );
    }

    @Test
    void testShouldStoreSnapshotInMetadataStore()
    {
        versionService.createMetadataVersionInDataStore( "myVersion", "mySnapshot" );

        dbmsManager.flushSession();

        assertEquals( "mySnapshot", versionService.getVersionData( "myVersion" ) );
    }

    @Test
    void testShouldThrowMetadataVersionServiceExceptionWhenSnapshotIsEmpty()
    {
        assertThrows( MetadataVersionServiceException.class,
            () -> versionService.createMetadataVersionInDataStore( "myVersion", "" ) );
    }

    @Test
    void testShouldThrowMetadataVersionServiceExceptionWhenSnapshotIsNull()
    {
        assertThrows( MetadataVersionServiceException.class,
            () -> versionService.createMetadataVersionInDataStore( "myVersion", null ) );
    }

    @Test
    void shouldThrowAnExceptionWhenVersionAndItsShanpShotAreNull()
    {
        assertThrows( MetadataVersionServiceException.class,
            () -> versionService.isMetadataPassingIntegrity( null, null ) );
    }

    public static void assertEqualMetadataVersions( MetadataVersion v1, MetadataVersion v2 )
    {
        if ( v1 == null && v2 == null )
        {
            return;
        }
        else if ( v1 == null || v2 == null )
        {
            fail( String.format( "Either both versions or none must be null. v1: %s, v2: %s", v1, v2 ) );
        }

        assertAll(
            () -> assertEquals( v1.getCreated(), v2.getCreated(), "date must match" ),
            () -> assertEquals( v1.getName(), v2.getName(), "name must match" ),
            () -> assertEquals( v1.getType(), v2.getType(), "type must match" ) );
    }
}
