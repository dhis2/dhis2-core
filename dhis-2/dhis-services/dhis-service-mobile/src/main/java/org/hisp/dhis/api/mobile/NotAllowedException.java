package org.hisp.dhis.api.mobile;

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

public class NotAllowedException
    extends Exception
{
    public static final NotAllowedException INVALID_PROGRAM_STAGE = new NotAllowedException( "INVALID_PROGRAM_STAGE" );

    public static final NotAllowedException INVALID_DATASET_ASSOCIATION = new NotAllowedException( "INVALID_DATASET_ASSOCIATION" );

    public static final NotAllowedException INVALID_PERIOD = new NotAllowedException( "INVALID_PERIOD" );

    public static final NotAllowedException INVALID_FILTER = new NotAllowedException( "INVALID_FILTER");

    public static final NotAllowedException DATASET_LOCKED = new NotAllowedException( "DATASET_LOCKED" );

    public static final NotAllowedException NO_USER = new NotAllowedException( "NO_USER", "No user is logged in." );
    
    public static final NotAllowedException NEED_MORE_SPECIFIC = new NotAllowedException( "NEED_MORE_SPECIFIC");
    
    public static final NotAllowedException NO_BENEFICIARY_FOUND = new NotAllowedException( "NO_BENEFICIARY_FOUND");
    
    public static final NotAllowedException NO_PROGRAM_FOUND = new NotAllowedException( "NO_PROGRAM_FOUND");
    
    public static final NotAllowedException NO_PROGRAM_BELONG_ORGUNIT = new NotAllowedException( "NO_PROGRAM_BELONG_ORGUNIT");
    
    public static final NotAllowedException NO_EVENT_FOUND = new NotAllowedException( "NO_EVENT_FOUND");
    
    private String reason;

    public NotAllowedException( String reason )
    {
        this.reason = reason;
    }

    private NotAllowedException( String reason, String message )
    {
        super( message );
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }

}
