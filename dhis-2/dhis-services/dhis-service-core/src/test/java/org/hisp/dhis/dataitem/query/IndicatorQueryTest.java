/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dataitem.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

class IndicatorQueryTest {

  @Test
  void getStatement() {
    IndicatorQuery query = new IndicatorQuery();
    MapSqlParameterSource parameterSource = new MapSqlParameterSource(Map.of());
    String expectation =
        "( select * from ( select cast (null as text) as program_name, "
            + "cast (null as text) as program_uid, "
            + "cast (null as text) as program_shortname, "
            + "indicator.uid as item_uid, "
            + "indicator.name as item_name, "
            + "indicator.shortname as item_shortname, "
            + "cast (null as text) as item_valuetype, "
            + "indicator.code as item_code, "
            + "indicator.sharing as item_sharing, "
            + "cast (null as text) as item_domaintype, "
            + "cast ('INDICATOR' as text) as item_type, "
            + "cast (null as text) as expression, "
            + "cast (null as text) as optionset_uid, "
            + "cast (null as text) as optionvalue_uid, "
            + "cast (null as text) as optionvalue_name, "
            + "cast (null as text) as optionvalue_code, "
            + "cast (null as bool) as item_skipanalytics, "
            + "indicator.name as i18n_first_name, "
            + "cast (null as text) as i18n_second_name, "
            + "indicator.shortname as i18n_first_shortname, "
            + "cast (null as text) as i18n_second_shortname "
            + "from indicator  group by item_name, item_uid, item_code, item_sharing, item_shortname, i18n_first_name, i18n_first_shortname, i18n_second_name, i18n_second_shortname ) t "
            + "where  ( ( (jsonb_extract_path_text(t.item_sharing, 'public') is null "
            + "or jsonb_extract_path_text(t.item_sharing, 'public') = 'null' "
            + "or jsonb_extract_path_text(t.item_sharing, 'public') like 'r%') "
            + "or (jsonb_extract_path_text(t.item_sharing, 'owner') is null "
            + "or jsonb_extract_path_text(t.item_sharing, 'owner') = 'null' "
            + "or jsonb_extract_path_text(t.item_sharing, 'owner') = :userUid) "
            + "or (jsonb_has_user_id(t.item_sharing, :userUid) = true  "
            + "and jsonb_check_user_access(t.item_sharing, :userUid, 'r%') = true) ) ))";

    String sql = query.getStatement(parameterSource);

    assertTrue(sql.equals(expectation));
  }
}
