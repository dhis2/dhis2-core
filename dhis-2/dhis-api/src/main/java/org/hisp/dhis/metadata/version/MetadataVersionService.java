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

package org.hisp.dhis.metadata.version;

import org.hisp.dhis.node.types.RootNode;

import java.util.Date;
import java.util.List;

/**
 * Define Service class for functionality related to MetadataVersion.
 *
 * @author aamerm
 */
public interface MetadataVersionService
{
    // ------------------------------------------------------------------------
    //   Constants
    //-------------------------------------------------------------------------
    public static String METADATASTORE = "METADATASTORE";
    public static String METADATAVERSION_NAME_PREFIX = "Version_";

    /**
     * @param version Version object to add.
     * @return ID of the saved version object.
     */
    int addVersion( MetadataVersion version );

    /**
     * @param version Version object to update.
     */
    void updateVersion( MetadataVersion version );

    /**
     * @param id
     * @param name
     */
    void updateVersionName( int id, String name );

    /**
     * @param version Version object to delete.
     */
    void deleteVersion( MetadataVersion version );

    /**
     * @param id Key to lookup the value with.
     * @return Version that matched key, or null if there was no match.
     */
    MetadataVersion getVersionById( int id );

    /**
     * @return List of all version objects.
     */
    List<MetadataVersion> getAllVersions();

    /**
     * Gets the instance's current version
     *
     * @return the current version at which the instance is.
     */
    MetadataVersion getCurrentVersion();

    /**
     * @return initial MetadataVersion of the system
     */
    MetadataVersion getInitialVersion();

    /**
     * Gets all versions between two data ranges on the 'created' date.
     *
     * @param startDate
     * @param endDate
     * @return List of MetadataVersion matching that date range
     */
    List<MetadataVersion> getAllVersionsInBetween( Date startDate, Date endDate );

    /**
     * Returns the created date of the version given the version name
     *
     * @param versionName
     * @return
     */
    Date getCreatedDate( String versionName );

    /**
     * Gets the version by name
     *
     * @param versionName
     * @return
     */
    MetadataVersion getVersionByName( String versionName );

    /**
     * Saves or creates a version given the version type identifier
     *
     * @param versionType
     * @return true if created
     */
    boolean saveVersion( VersionType versionType );

    /**
     * Gets the Version data - the actual json snapshot given the version name.
     *
     * @param versionName
     * @return Json data for the version snapshot
     */
    String getVersionData( String versionName );

    /**
     * Returns the versions list wrapped as RootNode
     *
     * @param versions
     * @return
     */
    RootNode getMetadataVersionsAsNode( List<MetadataVersion> versions );

    /**
     * Creates an entry in the DataStore given the MetadataVersion details.
     *
     * @param versionName
     * @param versionSnapshot
     */
    void createMetadataVersionInDataStore( String versionName, String versionSnapshot );

    /**
     * Checks the integrity of metadata by checking hashcode
     *
     * @param version
     * @param versionSnapshot
     * @return
     */
    boolean isMetadataPassingIntegrity( MetadataVersion version, String versionSnapshot );

    /**
     * Deletes the entry in Data Store given the versionName
     *
     * @param nameSpaceKey
     */
    void deleteMetadataVersionInDataStore( String nameSpaceKey );
}
