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
package org.hisp.dhis.analytics.common.processing;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hisp.dhis.analytics.tei.query.TeiFields.getGridHeaders;
import static org.hisp.dhis.feedback.ErrorCode.E7230;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.springframework.stereotype.Component;

/**
 * Analytics component responsible for processing flags and data related to the
 * "headers" param.
 *
 * @author maikel arabori
 */
@Component
public class HeaderParamsHandler
{
    /**
     * Adds the correct headers into the given {@Grid}. It takes into
     * consideration the header params, if any.
     *
     * @param grid the {@link Grid}.
     * @param teiQueryParams all headers represent by a set of
     *        {@link GridHeader}.
     * @param fields the columns to retain, represented by {@link Field}.
     * @throws IllegalQueryException if header requested does not exist.
     */
    public void handle( Grid grid, TeiQueryParams teiQueryParams, List<Field> fields )
    {
        Set<GridHeader> headers = getGridHeaders( teiQueryParams, fields );
        Set<String> paramHeaders = teiQueryParams.getCommonParams().getHeaders();

        if ( isEmpty( paramHeaders ) )
        {
            // Adds all headers.
            headers.forEach( grid::addHeader );
        }
        else
        {
            List<GridHeader> gridHeaders = new ArrayList<>( headers );

            // Adds only the headers present in params, in the same order.
            paramHeaders.forEach( header -> {
                GridHeader gridHeader = new GridHeader( header );

                if ( gridHeaders.contains( gridHeader ) )
                {
                    int element = gridHeaders.indexOf( gridHeader );
                    grid.addHeader( gridHeaders.get( element ) );
                }
            } );
        }

        checkHeaders( headers, paramHeaders );
    }

    /**
     * Simply checks if the headers requested are valid ones.
     *
     * @param gridHeaders the set of {@link GridHeader}.
     * @param paramHeaders the set of param headers.
     * @throws IllegalQueryException if any header in "paramHeaders" is not
     *         present in the given "gridHeaders".
     */
    private void checkHeaders( Set<GridHeader> gridHeaders, Set<String> paramHeaders )
    {
        paramHeaders.forEach( header -> {
            GridHeader gridHeader = new GridHeader( header );

            if ( !gridHeaders.contains( gridHeader ) )
            {
                throw new IllegalQueryException( new ErrorMessage( E7230, header ) );
            }
        } );
    }
}
