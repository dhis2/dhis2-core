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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.*;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This supplier adds to the pre-heat object a Map-like data structure, where the key is a Program
 * ID (primary key) and the value is a List of Org Units ID (primary keys). The scope of this data
 * structure is to allow the validation to check if the entity or enrollment declared org unit is
 * part of the declared program's org units.
 *
 * <p>This supplier is efficient because it only loads the data that are strictly necessary for this
 * check.
 *
 * @author Luciano Fiandesio
 */
@Component
public class ProgramOrgUnitsSupplier extends JdbcAbstractPreheatSupplier {
  protected ProgramOrgUnitsSupplier(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    // fetch all existing Org Units from payload
    final List<Long> orgUnitIds =
        preheat.getAll(OrganisationUnit.class).stream()
            .map(IdentifiableObject::getId)
            .distinct()
            .collect(Collectors.toList());

    if (orgUnitIds.isEmpty()) {
      return;
    }

    final String sql =
        "SELECT p.uid AS programuid, ou.uid AS organisationunituid "
            + "FROM program_organisationunits po "
            + "JOIN program p ON po.programid=p.programid "
            + "JOIN organisationunit ou ON po.organisationunitid=ou.organisationunitid "
            + "WHERE po.organisationunitid IN ( :ids )";

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("ids", orgUnitIds);

    preheat.setProgramWithOrgUnitsMap(
        jdbcTemplate.query(
            sql,
            parameters,
            rs -> {
              Map<String, List<String>> map = new HashMap<>();

              while (rs.next()) {
                final String pid = rs.getString("programuid");
                final String ouid = rs.getString("organisationunituid");

                if (map.containsKey(pid)) {
                  map.get(pid).add(ouid);
                } else {
                  List<String> ouids = new ArrayList<>();
                  ouids.add(ouid);
                  map.put(pid, ouids);
                }
              }

              return map;
            }));
  }
}
