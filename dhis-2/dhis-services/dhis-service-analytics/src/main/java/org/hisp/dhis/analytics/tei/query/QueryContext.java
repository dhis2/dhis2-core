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
package org.hisp.dhis.analytics.tei.query;

import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import org.hisp.dhis.analytics.shared.query.Renderable;
import org.hisp.dhis.analytics.shared.query.Table;
import org.hisp.dhis.analytics.tei.TeiQueryParams;

@RequiredArgsConstructor( staticName = "of" )
public class QueryContext
{
    @Getter
    private final TeiQueryParams teiQueryParams;

    @Delegate
    private final ParameterManager parameterManager = new ParameterManager();

    public String getMainTableName()
    {
        return ANALYTICS_TEI + getTetTableSuffix();
    }

    public String getTetTableSuffix()
    {
        return teiQueryParams.getTrackedEntityType().getUid().toLowerCase();
    }

    public Renderable getMainTable()
    {
        return Table.ofStrings( getMainTableName(), TEI_ALIAS );
    }

    private static class ParameterManager
    {
        private int parameterIndex = 0;

        @Getter
        private final Map<String, Object> parametersByPlaceHolder = new HashMap<>();

        public String bindParamAndGetIndex( Object param )
        {
            parameterIndex++;
            parametersByPlaceHolder.put( String.valueOf( parameterIndex ), param );
            return ":" + parameterIndex;
        }
    }
}
