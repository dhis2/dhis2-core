/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.hisp.dhis.setting.SettingKey.ANALYTICS_MAX_LIMIT;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

/**
 * Processor class for CommonQueryRequest objects.
 */
@Component
@RequiredArgsConstructor
public class CommonQueryRequestProcessor implements Processor<CommonQueryRequest>
{
    private final SystemSettingManager systemSettingManager;

    /**
     * Based on the given query request object {@link CommonQueryRequest}, this
     * method will process/compute existing values in the request object, and
     * populated all necessary attributes of this same object.
     *
     * @param commonQueryRequest the {@link CommonQueryRequest} to process.
     * @return the processed {@link CommonQueryRequest}.
     */
    @Override
    public CommonQueryRequest process( CommonQueryRequest commonQueryRequest )
    {
        return computePagingParams( commonQueryRequest );
    }

    /**
     * Apply paging parameters to the given {@link CommonQueryRequest} object,
     * taking into account the system setting for the maximum limit and the
     * ignoreLimit flag.
     *
     * @param commonQueryRequest the {@link CommonQueryRequest} to compute the
     * @return the computed {@link CommonQueryRequest}
     */
    private CommonQueryRequest computePagingParams( CommonQueryRequest commonQueryRequest )
    {
        int maxLimit = systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT );
        boolean unlimited = maxLimit == 0;
        boolean ignoreLimit = commonQueryRequest.isIgnoreLimit();
        boolean hasMaxLimit = !unlimited && !ignoreLimit;

        if ( commonQueryRequest.isPaging() )
        {
            boolean pageSizeOverMaxLimit = commonQueryRequest.getPageSize() > maxLimit;

            if ( hasMaxLimit && pageSizeOverMaxLimit )
            {
                return commonQueryRequest.withPageSize( maxLimit );
            }

            return commonQueryRequest;
        }
        else
        {
            if ( unlimited )
            {
                return commonQueryRequest.withIgnoreLimit( true );
            }

            return commonQueryRequest.withPageSize( maxLimit );
        }
    }
}
