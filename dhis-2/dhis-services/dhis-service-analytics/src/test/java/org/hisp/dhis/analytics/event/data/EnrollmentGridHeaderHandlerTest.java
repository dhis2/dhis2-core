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
package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.event.data.EnrollmentGridHeaderHandler.ITEM_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.EnrollmentGridHeaderHandler.ITEM_ORG_UNIT_NAME;
import static org.hisp.dhis.analytics.event.data.EnrollmentGridHeaderHandler.ITEM_STORED_BY;
import static org.hisp.dhis.analytics.event.data.EnrollmentGridHeaderHandler.createGridUsingHeaders;

import java.util.List;

import org.hisp.dhis.common.Grid;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EnrollmentGridHeaderHandler}
 */
class EnrollmentGridHeaderHandlerTest
{

    @Test
    void testCreateGridUsingHeaders()
    {
        // Given
        final List<String> headers = List.of( ITEM_STORED_BY, ITEM_GEOMETRY, ITEM_ORG_UNIT_NAME );

        // When
        final Grid expected = createGridUsingHeaders( headers );

        // Then
        assertThat( expected.getHeaderWidth(), is( equalTo( 3 ) ) );
        assertThat( expected.getHeaders().size(), is( equalTo( 3 ) ) );
        assertThat( expected.getHeaders().get( 0 ).getName(), is( equalTo( ITEM_STORED_BY ) ) );
        assertThat( expected.getHeaders().get( 1 ).getName(), is( equalTo( ITEM_GEOMETRY ) ) );
        assertThat( expected.getHeaders().get( 2 ).getName(), is( equalTo( ITEM_ORG_UNIT_NAME ) ) );
    }
}
