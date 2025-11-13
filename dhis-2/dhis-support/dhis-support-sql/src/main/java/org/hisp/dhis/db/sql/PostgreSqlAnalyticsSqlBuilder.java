/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.db.sql;

public class PostgreSqlAnalyticsSqlBuilder extends PostgreSqlBuilder
    implements AnalyticsSqlBuilder {

  /**
   * Returns a subquery that expand the event datavalue jsonb with two additional fields:
   *
   * <ul>
   *   <li>value_name: the name of the organisation unit that the datavalue is associated with
   *   <li>value_code: the code of the organisation unit that the datavalue is associated with
   * </ul>
   *
   * @return a SQL subquery.
   */
  @Override
  public String getEventDataValues() {
    return """
        (select json_object_agg(l2.keys, l2.datavalue) as value
        from (
            select l1.uid,
            l1.keys,
            json_strip_nulls(json_build_object(
            'value', l1.eventdatavalues -> l1.keys ->> 'value',
            'created', l1.eventdatavalues -> l1.keys ->> 'created',
            'storedBy', l1.eventdatavalues -> l1.keys ->> 'storedBy',
            'lastUpdated', l1.eventdatavalues -> l1.keys ->> 'lastUpdated',
            'providedElsewhere', l1.eventdatavalues -> l1.keys -> 'providedElsewhere',
            'value_name', (select ou.name
                from organisationunit ou
                where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'),
            'value_code', (select ou.code
                from organisationunit ou
                where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'))) as datavalue
            from (select inner_evt.*, jsonb_object_keys(inner_evt.eventdatavalues) keys
            from trackerevent inner_evt) as l1) as l2
        where l2.uid = ev.uid
        group by l2.uid)::jsonb
        """;
  }

  @Override
  public String renderTimestamp(String timestampAsString) {
    return timestampAsString;
  }
}
