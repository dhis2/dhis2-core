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
package org.hisp.dhis.security.utils;

import java.util.List;
import java.util.regex.Pattern;

public class CspUtils
{
    public static final String DEFAULT_HEADER_VALUE = "script-src 'none'; ";

    public static final Pattern P_1 = Pattern.compile( "^.+/events/files$" );

    public static final Pattern P_2 = Pattern.compile( "^.+trackedEntityInstance/[a-zA-Z\\d]+/[a-zA-Z\\d]+/image$" );

    public static final Pattern P_3 = Pattern.compile( "^.+/dataValues/files$" );

    public static final Pattern P_4 = Pattern.compile(
        "^.+messageConversations/[a-zA-Z\\d]+/[a-zA-Z\\d]+/attachments/[a-zA-Z\\d]+$" );

    public static final Pattern P_5 = Pattern.compile( "^.+fileResources/[a-zA-Z\\d]+/data$" );

    public static final Pattern P_6 = Pattern.compile( "^.+audits/files/[a-zA-Z\\d]+$" );

    public static final Pattern P_7 = Pattern.compile( "^.+externalFileResources/[a-zA-Z\\d]+$" );

    public static final List<Pattern> DEFAULT_FILTERED_URL_PATTERNS = List.of( P_1, P_2, P_3, P_4, P_5, P_6, P_7 );

    public static final Pattern ALL_API = Pattern.compile( "^/api/[a-zA-Z\\d].+" );

    public static final Pattern STATIC_IN_API_1 = Pattern.compile( "^/api/staticContent/[a-zA-Z\\d].+" );

    public static final Pattern STATIC_IN_API_2 = Pattern.compile( "^/api/files/style/external$" );

    public static final List<Pattern> STATIC_RESOURCES_IN_API_URL_PATTERNS = List.of( STATIC_IN_API_1,
        STATIC_IN_API_2 );
}
