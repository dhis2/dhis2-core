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
package org.hisp.dhis.dataexchange.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class AnalyticsDataExchangeServiceTest
{
    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private DataQueryService dataQueryService;

    @Mock
    private DataValueSetService dataValueSetService;

    private AnalyticsDataExchangeService service;

    @BeforeEach
    void beforeEach()
    {
        service = new AnalyticsDataExchangeService( analyticsService, dataQueryService, dataValueSetService );
    }

    @Test
    void testGetOrDefault()
    {
        assertEquals( "CODE", service.getOrDefault( "CODE" ) );
        assertEquals( "UID", service.getOrDefault( null ) );
    }

    @Test
    void testToIdSchemeOrDefault()
    {
        assertEquals( IdScheme.CODE, service.toIdSchemeOrDefault( "code" ) );
        assertEquals( IdScheme.UID, service.toIdSchemeOrDefault( "UID" ) );
        assertEquals( IdScheme.UID, service.toIdSchemeOrDefault( null ) );
    }

    @Test
    void testGetDhis2Client()
    {
        Api api = new Api( "https://play.dhis2.org/demo", "d2pat_5xVA12xyUbWNedQxy4ohH77WlxRGVvZZ1151814092" );

        Target target = new Target();
        target.setType( TargetType.EXTERNAL );
        target.setApi( api );

        AnalyticsDataExchange exchange = new AnalyticsDataExchange();
        exchange.setTarget( target );

        Dhis2Client client = service.getDhis2Client( exchange );

        assertEquals( "https://play.dhis2.org/demo", client.getUrl() );
    }
}
