package org.hisp.dhis.security;

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

/**
 * Options for user account restore operation. These options are represented
 * in the user account restore email as a prefix to the restore token.
 * This token is hashed, and the hash is stored in the database. This means
 * that the options cannot be hacked to change them, because then the token
 * would no longer match the saved hash in the database.
 *
 * @author Jim Grace
 */

public enum RestoreOptions
{
    RECOVER_PASSWORD_OPTION ( "R", RestoreType.RECOVER_PASSWORD, false ),
    INVITE_WITH_USERNAME_CHOICE ( "IC", RestoreType.INVITE, true ),
    INVITE_WITH_DEFINED_USERNAME ( "ID", RestoreType.INVITE, false );

    /**
     * Prefix to be used on restore token, to represent this set of options.
     */
    private final String tokenPrefix;

    /**
     * The type of restore operation to perform (i.e. password recovery
     * or invite to create account.)
     */
    private final RestoreType restoreType;

    /**
     * Defines whether the user can choose a username at the time of restore.
     */
    private final boolean usernameChoice;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private RestoreOptions( String tokenPrefix, RestoreType restoreType, boolean usernameChoice )
    {
        this.tokenPrefix = tokenPrefix;
        this.restoreType = restoreType;
        this.usernameChoice = usernameChoice;
    }

    // -------------------------------------------------------------------------
    // Get Restore Options from a token string
    // -------------------------------------------------------------------------

    static public RestoreOptions getRestoreOptions( String token )
    {
        for ( RestoreOptions ro : RestoreOptions.values() )
        {
            if ( token.startsWith( ro.getTokenPrefix() ) )
            {
                return ro;
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getTokenPrefix()
    {
        return tokenPrefix;
    }

    public RestoreType getRestoreType()
    {
        return restoreType;
    }

    public boolean isUsernameChoice()
    {
        return usernameChoice;
    }
}
