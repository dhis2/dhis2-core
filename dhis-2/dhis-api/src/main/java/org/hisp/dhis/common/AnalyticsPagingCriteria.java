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
package org.hisp.dhis.common;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains paging criteria that can be used to execute an analytics
 * query.
 */
@Getter
@Setter
public class AnalyticsPagingCriteria extends RequestTypeAware
{
    /**
     * The page number. Default page is 1.
     */
    private Integer page = 1;

    /**
     * The page size.
     */
    private Integer pageSize = 50;

    /**
     * The paging parameter. When set to false we should not paginate. The
     * default is true (paginate).
     */
    private boolean paging = true;

    /**
     * The paging parameter. When set to false we should not count total pages.
     * The default is true (count total pages).
     */
    private boolean totalPages = true;

    /**
     * Sets the page size, taking the configurable max records limit into
     * account. Note that a value of 0 represents unlimited records.
     *
     * @param maxLimit the max limit as defined in the system setting
     *        'ANALYTICS_MAX_LIMIT'.
     */
    public void definePageSize( int maxLimit )
    {
        if ( isPaging() )
        {
            if ( getPageSize() != null && maxLimit != 0 && getPageSize() > maxLimit )
            {
                setPageSize( maxLimit );
            }
        }
    }
}
