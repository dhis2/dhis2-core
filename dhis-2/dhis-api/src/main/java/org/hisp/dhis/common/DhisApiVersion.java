package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

/**
 * Enum representing web API versions. The API version is exposed through
 * the API URL at <code>/api/{version}/{resource}</code>, where <code>{version}</code>
 * is a numeric value and must match a value of this enum. If omitted, the
 * <code>DEFAULT</code> value will be used. The API resources can also be mapped
 * to all versions using the <code>ALL</code> value.
 * <p>
 * TODO The <code>DEFAULT</code> version must be updated for each release.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum DhisApiVersion
{
    ALL( -1, true ),
    V23( 23 ),
    V24( 24 ),
    V25( 25 ),
    V26( 26 ),
    V27( 27 ),
    DEFAULT( V27.getVersion() );

    final int version;

    final boolean ignore;

    DhisApiVersion( int version )
    {
        this.version = version;
        this.ignore = false;
    }

    DhisApiVersion( int version, boolean ignore )
    {
        this.version = version;
        this.ignore = ignore;
    }

    public int getVersion()
    {
        return version;
    }

    public String getVersionString()
    {
        return this == DEFAULT ? "" : String.valueOf( version );
    }

    public boolean isIgnore()
    {
        return ignore;
    }

    /**
     * Indicates whether this version is equal to the given
     * version.
     *
     * @param apiVersion the API version.
     */
    public boolean eq( DhisApiVersion apiVersion )
    {
        return version == apiVersion.getVersion();
    }

    /**
     * Indicates whether this version is less than the given
     * version.
     *
     * @param apiVersion the API version.
     */
    public boolean lt( DhisApiVersion apiVersion )
    {
        return version < apiVersion.getVersion();
    }

    /**
     * Indicates whether this version is less than or equal to
     * the given version.
     *
     * @param apiVersion the API version.
     */
    public boolean le( DhisApiVersion apiVersion )
    {
        return version <= apiVersion.getVersion();
    }

    /**
     * Indicates whether this version is greater than the given
     * version.
     *
     * @param apiVersion the API version.
     */
    public boolean gt( DhisApiVersion apiVersion )
    {
        return version > apiVersion.getVersion();
    }

    /**
     * Indicates whether this version is greater than or equal to
     * the given version.
     *
     * @param apiVersion the API version.
     */
    public boolean ge( DhisApiVersion apiVersion )
    {
        return version >= apiVersion.getVersion();
    }

    public static DhisApiVersion getVersion( int version )
    {
        for ( int i = 0; i < DhisApiVersion.values().length; i++ )
        {
            DhisApiVersion v = DhisApiVersion.values()[i];

            if ( version == v.getVersion() )
            {
                return v;
            }
        }

        return DEFAULT;
    }
}
