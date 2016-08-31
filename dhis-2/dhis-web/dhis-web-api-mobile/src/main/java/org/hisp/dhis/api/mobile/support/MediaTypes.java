package org.hisp.dhis.api.mobile.support;

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

import java.util.Arrays;
import java.util.List;

import org.springframework.http.MediaType;

public class MediaTypes
{
    public static String ACTIVITYVALUELIST_SERIALIZED = "application/vnd.org.dhis2.activityvaluelist+serialized";
    public static MediaType ACTIVITYVALUELIST_SERIALIZED_TYPE = 
        MediaType.parseMediaType( ACTIVITYVALUELIST_SERIALIZED );

    public static String DATASETVALUE_SERIALIZED = "application/vnd.org.dhis2.datasetvalue+serialized";
    public static MediaType DATASETVALUE_SERIALIZED_TYPE = 
        MediaType.parseMediaType( DATASETVALUE_SERIALIZED );

    public static String MOBILE_SERIALIZED = "application/vnd.org.dhis2.mobile+serialized";
    //public static String MOBILE_SERIALIZED = "application/vnd.org.dhis2.mobile+serialized;charset=UTF-8";
    public static MediaType MOBILE_SERIALIZED_TYPE = 
        MediaType.parseMediaType( MOBILE_SERIALIZED );
    
    public static String MOBILE_SERIALIZED_WITH_CHARSET = "application/vnd.org.dhis2.mobile+serialized;charset=UTF-8";
    public static MediaType MOBILE_SERIALIZED_TYPE_WITH_CHARSET = 
        MediaType.parseMediaType( MOBILE_SERIALIZED_WITH_CHARSET );

    public static List<MediaType> MEDIA_TYPES = Arrays.asList( new MediaType[] { ACTIVITYVALUELIST_SERIALIZED_TYPE,
            DATASETVALUE_SERIALIZED_TYPE, MOBILE_SERIALIZED_TYPE, MOBILE_SERIALIZED_TYPE_WITH_CHARSET } );
}