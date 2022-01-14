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
package org.hisp.dhis.dataitem.query;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Interface responsible for providing the basic and necessary methods regarding
 * general data item queries.
 *
 * @author maikel arabori
 */
public interface DataItemQuery
{
    /**
     * Builds and returns the SQL statement required by the implementation.
     *
     * @param paramsMap
     * @return the full SQL statement
     */
    String getStatement( MapSqlParameterSource paramsMap );

    /**
     * Checks if the query rules match the required conditions so the query can
     * be executed.
     *
     * @param paramsMap
     * @return true if matches, false otherwise
     */
    boolean matchQueryRules( MapSqlParameterSource paramsMap );

    /**
     * Simply returns the entity associated with the respective interface/query
     * implementation.
     *
     * @return the entity associated to the interface implementation
     */
    Class<? extends BaseIdentifiableObject> getRootEntity();
}
