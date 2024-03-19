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
package org.hisp.dhis.db.migration.v36;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Slf4j
public class V2_36_6__Remove_duplicate_mappings_from_programstage_to_usergroups
    extends BaseJavaMigration {

  private static final String USERGROUPID = "usergroupid";

  private static final String PROGRAMSTAGEID = "programstageid";

  private static final String SAFE_COLUMN_RENAME_SQL =
      "DO $$ "
          + "BEGIN "
          + "  IF EXISTS(SELECT * "
          + "    FROM information_schema.columns "
          + "    WHERE table_name='programstageusergroupaccesses' and column_name='programid') "
          + "  THEN "
          + "alter table programstageusergroupaccesses rename COLUMN programid to programstageid; "
          + "END IF; "
          + "END $$; ";

  private static final String CHECK_DUPLICATE_PGMSTG_USERGROUP_MAPPING =
      "SELECT count(*),programstageid,name,usergroupid  "
          + "FROM   (SELECT ps.*,uga.* "
          + "       FROM   programstage ps "
          + "       LEFT JOIN programstageusergroupaccesses psuga "
          + "               ON ps.programstageid = psuga.programstageid "
          + "       LEFT JOIN usergroupaccess uga "
          + "               ON psuga.usergroupaccessid = uga.usergroupaccessid) AS pscouga "
          + "GROUP  BY programstageid,name,usergroupid HAVING count(*) > 1";

  private static final String GET_ACCESS_STRING_FOR_PS_UG_COMBO =
      "SELECT usergroupaccessid,usergroupid,programstageid, "
          + "CASE "
          + "WHEN access = '--------' THEN 1 "
          + "WHEN access = 'r-------' THEN 2 "
          + "WHEN access LIKE '_w------' THEN 3 "
          + "WHEN access LIKE '__r-----' THEN 4 "
          + "WHEN access LIKE '___w----' THEN 5 "
          + "ELSE 0 "
          + "END as accesslevel "
          + "FROM   (SELECT psuga.programstageid,uga.*  "
          + "       FROM  programstageusergroupaccesses psuga "
          + "       LEFT JOIN usergroupaccess uga "
          + "               ON psuga.usergroupaccessid = uga.usergroupaccessid "
          + " WHERE  programstageid IN (%s)  and usergroupid IN (%s)) AS pstguga order by accesslevel desc";

  private static final String DELETE_PS_USRGRP_ACCESS =
      "delete from programstageusergroupaccesses where usergroupaccessid in (%s)";

  private static final String DELETE_USRGRP_ACCESS =
      "delete from usergroupaccess where usergroupaccessid in (%s)";

  @Override
  public void migrate(Context context) throws Exception {
    List<DuplicateProgramStageUserGroupWithCount> duplicatePgmStgUsrgrps = new ArrayList<>();
    Set<String> programStageIds = new HashSet<>();
    Set<String> userGroupIds = new HashSet<>();
    long totalCount = 0;

    // 0. For backward compatibility, safely rename the psuga programid
    // column to programstageid
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(SAFE_COLUMN_RENAME_SQL);
    }

    // 1. Check if there are duplicate mappings. If not simply return.
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(CHECK_DUPLICATE_PGMSTG_USERGROUP_MAPPING)) {
      if (rs.next() == false) {
        log.info(
            "No duplicate mappings for programstage to usergroups. Skipping duplicate cleanup migration");
        return;
      } else {
        DuplicateProgramStageUserGroupWithCount duplicatePgmStgUsrgrp = null;
        do {
          duplicatePgmStgUsrgrp = new DuplicateProgramStageUserGroupWithCount();
          duplicatePgmStgUsrgrp.setName(rs.getString("name"));
          duplicatePgmStgUsrgrp.setProgramStageId(rs.getLong(PROGRAMSTAGEID));
          programStageIds.add(rs.getString(PROGRAMSTAGEID));
          duplicatePgmStgUsrgrp.setUserGroupId(rs.getLong(USERGROUPID));
          userGroupIds.add(rs.getString(USERGROUPID));
          duplicatePgmStgUsrgrp.setCount(rs.getInt("count"));
          totalCount = totalCount + rs.getInt("count");
          duplicatePgmStgUsrgrps.add(duplicatePgmStgUsrgrp);
        } while (rs.next());
      }
    }

    Set<Pair<String, String>> pgmStgUsrGroupPairs = new HashSet<>();
    Set<String> deleteUserGroupAccessIds = new HashSet<>();

    String pgmStgIdsCommaSeparated = StringUtils.join(programStageIds, ",");
    String userGroupIdsCommaSeparated = StringUtils.join(userGroupIds, ",");

    // 2. Get ordered access strings for each categoryoptionid and
    // usergroupid combination
    try (Statement stmt = context.getConnection().createStatement();
        ResultSet rs =
            stmt.executeQuery(
                String.format(
                    GET_ACCESS_STRING_FOR_PS_UG_COMBO,
                    pgmStgIdsCommaSeparated,
                    userGroupIdsCommaSeparated)); ) {
      Pair<String, String> currentPair = null;
      while (rs.next()) {
        currentPair = Pair.of(rs.getString(PROGRAMSTAGEID), rs.getString(USERGROUPID));
        if (pgmStgUsrGroupPairs.contains(currentPair)) {
          deleteUserGroupAccessIds.add(rs.getString("usergroupaccessid"));
        } else {
          pgmStgUsrGroupPairs.add(currentPair);
        }
      }
    }
    log.info(
        "Total identified duplicate userGroupAccessIds to delete : "
            + deleteUserGroupAccessIds.size());
    log.info("Total programStage duplicated in mappings: " + duplicatePgmStgUsrgrps.size());
    log.info("Total impacted rows in usergroupaccess mapping tables: " + totalCount);

    String usrGrpAccessIdsCommaSeparated = StringUtils.join(deleteUserGroupAccessIds, ",");

    try (Statement stmt = context.getConnection().createStatement(); ) {
      // Delete redundant usergroupaccessid from
      // programstageusergroupaccesses table
      int deletedPsugaCount =
          stmt.executeUpdate(String.format(DELETE_PS_USRGRP_ACCESS, usrGrpAccessIdsCommaSeparated));
      log.info(
          "Deleted userGroupAccessIds from programstageusergroupaccesses table : "
              + deletedPsugaCount);

      // Delete redundant usergroupaccessid from usergroupaccess table
      int deletedUgaCount =
          stmt.executeUpdate(String.format(DELETE_USRGRP_ACCESS, usrGrpAccessIdsCommaSeparated));
      log.info("Deleted userGroupAccessIds from usergroupaccess table : " + deletedUgaCount);
    }
  }
}

@Getter
@Setter
class DuplicateProgramStageUserGroupWithCount {
  private String name;

  private long programStageId;

  private long userGroupId;

  private int count;
}
