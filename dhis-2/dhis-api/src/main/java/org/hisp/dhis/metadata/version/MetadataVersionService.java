package org.hisp.dhis.metadata.version;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
    String METADATASTORE = "METADATASTORE";
    String METADATAVERSION_NAME_PREFIX = "Version_";

    /**
     * Adds the metadata version.
     *
     * @param version the metadata version object to add.
     * @return the identifier of the saved version object.
     */
    int addVersion( MetadataVersion version );

    /**
     * Updates the metadata version.
     *
     * @param version the metadata version to update.
     */
    void updateVersion( MetadataVersion version );

    /**
     * Updates the name of the metadata version with the given identifier and name.
     *
     * @param id   the identifier.
     * @param name the name.
     */
    void updateVersionName( int id, String name );

    /**
     * @param version Version object to delete.
     */
    void deleteVersion( MetadataVersion version );

    /**
     * Gets the metadata version with the given identifier.
     *
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
     * Gets all versions between two data ranges on the created date.
     *
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return a list of metadata versions matching the date range.
     */
    List<MetadataVersion> getAllVersionsInBetween( Date startDate, Date endDate );

    /**
     * Returns the created date of the version given the version name
     *
     * @param versionName the version name.
     * @return the created date.
     */
    Date getCreatedDate( String versionName );

    /**
     * Gets the metadata version with the given name.
     *
     * @param versionName the version name.
     * @return the metadata version.
     */
    MetadataVersion getVersionByName( String versionName );

    /**
     * Saves or creates a version given the version type identifier
     *
     * @param versionType the version type.
     * @return true if created
     */
    boolean saveVersion( VersionType versionType );

    /**
     * Gets the Version data - the actual JSON snapshot given the version name.
     *
     * @param versionName
     * @return JSON data for the version snapshot
     */
    String getVersionData( String versionName );

    /**
     * Creates an entry in the DataStore given the MetadataVersion details.
     *
     * @param versionName
     * @param versionSnapshot
     */
    void createMetadataVersionInDataStore( String versionName, String versionSnapshot );

    /**
     * Checks the integrity of metadata by checking hash code.
     *
     * @param version
     * @param versionSnapshot
     * @return true if the metadata passes the integrity check.
     */
    boolean isMetadataPassingIntegrity( MetadataVersion version, String versionSnapshot );

    /**
     * Deletes the entry in Data Store given the versionName
     *
     * @param nameSpaceKey the name space key.
     */
    void deleteMetadataVersionInDataStore( String nameSpaceKey );
}
