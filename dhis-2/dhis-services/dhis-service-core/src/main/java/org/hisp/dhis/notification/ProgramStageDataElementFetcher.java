/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Fetches the data elements of a program stage that a notification template references. Projects
 * the columns the renderer needs via SQL rather than loading entities.
 *
 * <p>Walking {@link org.hisp.dhis.program.ProgramStage#getProgramStageDataElements()} instead would
 * initialize one DataElement entity per element of the stage, thousands on wide programs, and a
 * stage coming from the tracker importer is preheat mapped and does not carry that association at
 * all. This query is bounded by the number of template placeholders instead.
 *
 * <p>Restricting to the stage's data elements means a placeholder naming a data element that exists
 * but is not part of the stage is absent from the result, which is what makes the renderer report
 * it as such.
 */
@Component
@RequiredArgsConstructor
class ProgramStageDataElementFetcher {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * One row per option of each referenced data element, or a single row with no option when it has
   * no option set. Options are resolved in memory by the renderer, which knows the value to match.
   */
  private static final String SQL =
      """
      select de.uid as de_uid, de.optionsetid, ov.code as option_code, ov.name as option_name
      from programstagedataelement psde
      join dataelement de on de.dataelementid = psde.dataelementid
      left join optionvalue ov on ov.optionsetid = de.optionsetid
      where psde.programstageid = :programStageId
        and de.uid = any(:dataElementUids)
      """;

  Map<String, ProgramStageDataElementInfo> fetch(long programStageId, Set<String> dataElementUids) {
    if (dataElementUids.isEmpty()) {
      return Map.of();
    }

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("programStageId", programStageId);
    parameters.addValue("dataElementUids", dataElementUids.toArray(new String[0]));

    Map<String, Map<String, String>> optionsByDataElement = new HashMap<>();
    Set<String> withOptionSet = new HashSet<>();
    jdbcTemplate.query(
        SQL,
        parameters,
        rs -> {
          String dataElementUid = rs.getString("de_uid");
          Map<String, String> options =
              optionsByDataElement.computeIfAbsent(dataElementUid, k -> new HashMap<>());
          rs.getLong("optionsetid");
          if (!rs.wasNull()) {
            withOptionSet.add(dataElementUid);
          }
          String code = rs.getString("option_code");
          if (code != null) {
            options.put(code, rs.getString("option_name"));
          }
        });

    Map<String, ProgramStageDataElementInfo> result = new HashMap<>(optionsByDataElement.size());
    optionsByDataElement.forEach(
        (uid, options) ->
            result.put(uid, new ProgramStageDataElementInfo(withOptionSet.contains(uid), options)));
    return result;
  }
}
