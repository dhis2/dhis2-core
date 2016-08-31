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

import java.util.List;

/**
 * @author mortenoh
 */
public interface VersionService
{
    String ORGANISATIONUNIT_VERSION = "organisationUnit";

    String ID = VersionService.class.getName();

    /**
     * @param version Version object to add.
     * @return ID of the saved version object.
     */
    int addVersion( Version version );

    /**
     * @param version Version object to update.
     */
    void updateVersion( Version version );
    
    /**
     * @param key
     */
    void updateVersion( String key );
    
    /**
     * @param key
     * @param value
     */
    void updateVersion( String key, String value );

    /**
     * @param version Version object to delete.
     */
    void deleteVersion( Version version );

    /**
     * @param id Get Version with this ID.
     * @return Version that matched ID, or null if there was no match.
     */
    Version getVersion( int id );

    /**
     * @param key Key to lookup the value with.
     * @return Version that matched key, or null if there was no match.
     */
    Version getVersionByKey( String key );

    /**
     * @return List of all version objects.
     */
    List<Version> getAllVersions();
}
