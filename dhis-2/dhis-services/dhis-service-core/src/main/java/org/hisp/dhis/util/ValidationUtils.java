/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.util;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityfilter.AttributeValueFilter;
import org.springframework.util.CollectionUtils;

public class ValidationUtils
{

    private ValidationUtils()
    {
    }

    public static void validateAttributeValueFilters( List<String> errors, List<AttributeValueFilter> attributes,
        Function<String, TrackedEntityAttribute> attributeFetcher )
    {
        if ( !CollectionUtils.isEmpty( attributes ) )
        {
            attributes.forEach( avf -> {
                if ( StringUtils.isEmpty( avf.getAttribute() ) )
                {
                    errors.add( "Attribute Uid is missing in filter" );
                }
                else
                {
                    TrackedEntityAttribute tea = attributeFetcher.apply( avf.getAttribute() );
                    if ( tea == null )
                    {
                        errors.add( "No tracked entity attribute found for attribute:" + avf.getAttribute() );
                    }
                }

                validateDateFilterPeriod( errors, avf.getAttribute(), avf.getDateFilter() );
            } );
        }
    }

    public static void validateDateFilterPeriod( List<String> errors, String item, DateFilterPeriod dateFilterPeriod )
    {
        if ( dateFilterPeriod == null || dateFilterPeriod.getType() == null )
        {
            return;
        }

        if ( dateFilterPeriod.getType() == DatePeriodType.ABSOLUTE
            && dateFilterPeriod.getStartDate() == null && dateFilterPeriod.getEndDate() == null )
        {
            errors.add( "Start date or end date not specified with ABSOLUTE date period type for " + item );
        }
    }
}
