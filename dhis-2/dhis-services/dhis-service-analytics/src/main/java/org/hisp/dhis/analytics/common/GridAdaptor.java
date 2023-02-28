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
package org.hisp.dhis.analytics.common;

import static org.springframework.util.Assert.notNull;

import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.common.processing.HeaderParamsHandler;
import org.hisp.dhis.analytics.common.processing.MetadataParamsHandler;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Component;

/**
 * Component that provides operations for generation or manipulation of Grid
 * objects. It basically encapsulates any required Grid logic that is not
 * supported by the Grid itself.
 *
 * @author maikel arabori
 */
@AllArgsConstructor
@Component
public class GridAdaptor
{
    private final HeaderParamsHandler headerParamsHandler;

    private final MetadataParamsHandler metadataParamsHandler;

    private final AnalyticsSecurityManager analyticsSecurityManager;

    private final CurrentUserService currentUserService;

    /**
     * Based on the given headers and result map, this method takes care of the
     * logic needed to create a valid {@link Grid} object. If the given
     * "sqlQueryResult" is not present, the resulting {@link Grid} will have
     * empty rows.
     *
     * @param sqlQueryResult the optional of {@link SqlQueryResult}.
     * @param teiQueryParams the {@link TeiQueryParams}.
     *
     * @return the {@link Grid} object.
     *
     * @throws IllegalArgumentException if headers is null/empty or contain at
     *         least one null element, or if the queryResult is null.
     */
    public Grid createGrid( Optional<SqlQueryResult> sqlQueryResult, long rowsCount,
        TeiQueryParams teiQueryParams, List<Field> fields )
    {
        notNull( teiQueryParams, "The 'teiQueryParams' must not be null" );

        Grid grid = new ListGrid();

        // Adding headers.
        headerParamsHandler.handle( grid, teiQueryParams, fields );

        // Adding rows.
        sqlQueryResult.ifPresent( queryResult -> grid.addNamedRows( queryResult.result() ) );

        // Adding metadata info.
        metadataParamsHandler.handle( grid, teiQueryParams.getCommonParams(), currentUserService.getCurrentUser(),
            rowsCount );

        return grid;
    }
}
