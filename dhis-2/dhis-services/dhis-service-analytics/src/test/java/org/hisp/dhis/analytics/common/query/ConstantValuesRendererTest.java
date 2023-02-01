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
package org.hisp.dhis.analytics.common.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.junit.jupiter.api.Test;

class ConstantValuesRendererTest
{
    @Test
    void testSingleValue()
    {
        SqlParameterManager parameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, parameterManager );
        String render = ConstantValuesRenderer.of( "test", ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( ":1", render );
        assertEquals( "test", parameterManager.getParametersPlaceHolder().get( "1" ) );
    }

    @Test
    void testSingleValueNV()
    {
        SqlParameterManager parameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, parameterManager );
        String render = ConstantValuesRenderer.of( "NV", ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( "", render );
        assertNull( parameterManager.getParametersPlaceHolder().get( "1" ) );
    }

    @Test
    void testMultipleValues()
    {
        SqlParameterManager parameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, parameterManager );
        List<String> arguments = List.of( "test1", "test2" );
        String render = ConstantValuesRenderer.of( arguments, ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( ":1", render );
        assertEquals( arguments, parameterManager.getParametersPlaceHolder().get( "1" ) );
    }

    @Test
    void testMultipleValuesNV()
    {
        SqlParameterManager parameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, parameterManager );
        List<String> arguments = List.of( "test1", "test2", "NV" );
        String render = ConstantValuesRenderer.of( arguments, ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( ":1", render );
        assertEquals( arguments.subList( 0, 2 ), parameterManager.getParametersPlaceHolder().get( "1" ) );
    }
}
