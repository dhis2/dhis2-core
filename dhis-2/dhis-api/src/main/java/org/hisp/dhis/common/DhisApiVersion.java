package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2016, University of Oslo
 *  All rights reserved.
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

import org.springframework.util.StringUtils;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum DhisApiVersion
{
    /**
     * Default mapping /api/name
     */
    DEFAULT( -1 ),

    /**
     * Map to all versions, not including default.
     */
    ALL( -2, true ),

    /**
     * /api/23/name
     */
    V23( 23 ),

    /**
     * /api/24/name
     */
    V24( 24 ),

    /**
     * /api/25/name
     */
    V25( 25 ),

    /**
     * /api/26/name
     */
    V26( 26 );

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

    public static DhisApiVersion getVersion( int version )
    {
        if ( StringUtils.isEmpty( version ) )
        {
            return DhisApiVersion.DEFAULT;
        }

        for ( int i = 0; i < DhisApiVersion.values().length; i++ )
        {
            DhisApiVersion v = DhisApiVersion.values()[i];

            if ( version == v.getVersion() )
            {
                return v;
            }
        }

        throw new RuntimeException( "Invalid value `" + version + "` for enum ApiVersion.Version" );
    }
}
