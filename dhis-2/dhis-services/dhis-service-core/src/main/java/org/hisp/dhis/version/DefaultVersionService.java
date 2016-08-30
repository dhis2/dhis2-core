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
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author mortenoh
 */
@Transactional
public class DefaultVersionService
    implements VersionService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private VersionStore versionStore;

    public void setVersionStore( VersionStore versionStore )
    {
        this.versionStore = versionStore;
    }

    // -------------------------------------------------------------------------
    // VersionService implementation
    // -------------------------------------------------------------------------

    @Override
    public int addVersion( Version version )
    {
        return versionStore.save( version );
    }

    @Override
    public void updateVersion( Version version )
    {
        versionStore.update( version );
    }

    @Override
    public void updateVersion( String key )
    {
        updateVersion( key, UUID.randomUUID().toString() );
    }

    @Override
    public void updateVersion( String key, String value )
    {
        Version version = getVersionByKey( key );
        
        if ( version == null )
        {
            version = new Version( key, value );
            addVersion( version );
        }
        else
        {
            version.setValue( value );
            updateVersion( version );
        }
    }

    @Override
    public void deleteVersion( Version version )
    {
        versionStore.delete( version );
    }

    @Override
    public Version getVersion( int id )
    {
        return versionStore.get( id );
    }

    @Override
    public Version getVersionByKey( String key )
    {
        return versionStore.getVersionByKey( key );
    }

    @Override
    public List<Version> getAllVersions()
    {
        return versionStore.getAll();
    }
}
