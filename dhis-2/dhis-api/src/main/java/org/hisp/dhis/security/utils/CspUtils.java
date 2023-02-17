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

    public static final Pattern p1 = Pattern.compile( "^.+/events/files$" );

    public static final Pattern p2 = Pattern.compile( "^.+trackedEntityInstance/[a-zA-Z\\d]+/[a-zA-Z\\d]+/image$" );

    public static final Pattern p3 = Pattern.compile( "^.+/dataValues/files$" );

    public static final Pattern p4 = Pattern.compile(
        "^.+messageConversations/[a-zA-Z\\d]+/[a-zA-Z\\d]+/attachments/[a-zA-Z\\d]+$" );

    public static final Pattern p5 = Pattern.compile( "^.+fileResources/[a-zA-Z\\d]+/data$" );

    public static final Pattern p6 = Pattern.compile( "^.+audits/files/[a-zA-Z\\d]+$" );

    public static final Pattern p7 = Pattern.compile( "^.+externalFileResources/[a-zA-Z\\d]+$" );

    public static final List<Pattern> DEFAULT_FILTERED_URL_PATTERNS = List.of( p1, p2, p3, p4, p5, p6, p7 );
}
