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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.util.Assert.state;

import java.util.List;

import lombok.NoArgsConstructor;

import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Helper class responsible for providing pagination capabilities on top of data
 * item objects.
 *
 * @author maikel arabori
 */
@NoArgsConstructor( access = PRIVATE )
public class PaginationHelper
{
    /**
     * This method will "paginate" the given list based on the given options and
     * return only the elements present in the pagination window.
     *
     * @param options
     * @param itemViewObjects
     * @return the list of "paginated" items
     */
    public static List<DataItem> paginate( WebOptions options,
        List<DataItem> itemViewObjects )
    {
        state( options.getPage() > 0, "Current page must be greater than zero." );
        state( options.getPageSize() > 0, "Page size must be greater than zero." );

        if ( options.hasPaging() && isNotEmpty( itemViewObjects ) )
        {
            // Pagination input.
            int currentPage = options.getPage();
            int totalOfElements = itemViewObjects.size();
            int maxElementsPerPage = options.getPageSize();

            Pager pager = new Pager( currentPage, totalOfElements, maxElementsPerPage );

            int currentElementIndex = pager.getOffset();
            boolean hasMorePages = (totalOfElements - currentElementIndex) > pager.getPageSize();

            if ( hasMorePages )
            {
                int nextElementsWindow = pager.getPageSize() * pager.getPage();
                itemViewObjects = itemViewObjects.subList( currentElementIndex, nextElementsWindow );
            }
            else
            {
                // This is the last page.
                itemViewObjects = itemViewObjects.subList( pager.getOffset(), totalOfElements );
            }
        }

        return itemViewObjects;
    }

    /**
     * Sets the limit of items to be fetched IF paging is enabled. The max limit
     * is set into the paramsMap.
     *
     * @param options the source of pagination params
     * @param paramsMap the map that will receive the max limit param (maxLimit)
     */
    public static void setMaxResultsWhenPaging( WebOptions options, MapSqlParameterSource paramsMap )
    {
        if ( options.hasPaging() )
        {
            int maxLimit = options.getPage() * options.getPageSize();
            paramsMap.addValue( "maxLimit", maxLimit );
        }
    }
}
