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
package org.hisp.dhis.analytics.shared;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.util.Assert.notNull;

import java.util.ArrayList;
import java.util.Set;

import org.hisp.dhis.common.Grid;

/**
 * This class is responsible for encapsulating the grid header creation and
 * provide supportive methods related to headers.
 *
 * @author maikel arabori
 */
public class GridHeaders
{
    private GridHeaders()
    {
    }

    /**
     * This method will retain the given headers in the give Grid. If the set of
     * headers provided is null or empty, no changes will be made to the Grid.
     * Its headers will remain as is.
     *
     * @param grid
     * @param headersToRetain
     */
    public static void retainHeadersOnGrid( final Grid grid, final Set<String> headersToRetain )
    {
        notNull( grid, "The 'grid' cannot be null" );

        final boolean hasHeadersToRetain = isNotEmpty( headersToRetain );

        if ( hasHeadersToRetain )
        {
            grid.retainColumns( headersToRetain );
            grid.repositionColumns( grid.repositionHeaders( new ArrayList<>( headersToRetain ) ) );
        }
    }
}
