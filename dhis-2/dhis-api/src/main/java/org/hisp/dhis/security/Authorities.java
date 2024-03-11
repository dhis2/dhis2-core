package org.hisp.dhis.security;

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

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
public enum Authorities
{
    F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS( "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS" ),
    F_TEI_CASCADE_DELETE( "F_TEI_CASCADE_DELETE" ),
    F_ENROLLMENT_CASCADE_DELETE( "F_ENROLLMENT_CASCADE_DELETE" ),
    F_EDIT_EXPIRED( "F_EDIT_EXPIRED" ),
    F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION( "F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION" ),
    F_SKIP_DATA_IMPORT_AUDIT( "F_SKIP_DATA_IMPORT_AUDIT" );

    private String authority;

    Authorities( String authority )
    {
        this.authority = authority;
    }

    public String getAuthority()
    {
        return authority;
    }
}
