/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.Map;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonNode;
import org.hisp.dhis.webapi.json.JsonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller tests for
 * {@link org.hisp.dhis.webapi.controller.event.EventChartController}.
 *
 * @author maikel arabori
 */
class EventChartControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    private Program mockProgram;

    @BeforeEach
    public void beforeEach()
    {
        mockProgram = createProgram( 'A' );
        manager.save( mockProgram );
    }

    @Test
    void testThatGetEventVisualizationsContainsLegacyEventCharts()
    {
        // Given
        final String body = "{'name': 'Name Test', 'type':'GAUGE', 'program':{'id':'" + mockProgram.getUid()
            + "'}}";

        // When
        final String uid = assertStatus( CREATED, POST( "/eventCharts/", body ) );

        // Then
        final JsonResponse response = GET( "/eventVisualizations/" + uid ).content();
        final Map<String, JsonNode> nodeMap = (Map<String, JsonNode>) response.node().value();

        assertThat( nodeMap.get( "name" ).toString(), containsString( "Name Test" ) );
        assertThat( nodeMap.get( "type" ).toString(), containsString( "GAUGE" ) );
    }

    @Test
    void testThatGetEventChartsDoesNotContainNewEventVisualizations()
    {
        // Given
        final String body = "{'name': 'Name Test', 'type':'GAUGE', 'program':{'id':'" + mockProgram.getUid()
            + "'}}";

        // When
        final String uid = assertStatus( CREATED, POST( "/eventVisualizations/", body ) );

        // Then
        final JsonResponse response = GET( "/eventCharts/" + uid ).content();
        final Map<String, JsonNode> nodeMap = (Map<String, JsonNode>) response.node().value();

        assertThat( nodeMap.values(), is( empty() ) );
    }
}
