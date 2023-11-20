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

import java.sql.*;
import org.flywaydb.core.api.migration.*;
import org.slf4j.*;

public class V2_36_11__Migrate_sharings_to_jsonb extends BaseJavaMigration {
  private static final Logger log =
      LoggerFactory.getLogger(V2_36_11__Migrate_sharings_to_jsonb.class);

  public void migrate(Context context) throws SQLException {
    String[] tableNames = {
      "attribute",
      "userrole",
      "usergroup",
      "sqlview",
      "constant",
      "optionset",
      "optiongroup",
      "optiongroupset",
      "maplegendset",
      "orgunitgroup",
      "orgunitgroupset",
      "dataelementcategoryoption",
      "categoryoptiongroup",
      "categoryoptiongroupSet",
      "dataelementcategory",
      "categorycombo",
      "dataelement",
      "dataelementgroup",
      "dataelementgroupset",
      "indicator",
      "indicatorgroup",
      "indicatorgroupset",
      "dataset",
      "dataapprovallevel",
      "dataapprovalworkflow",
      "validationrule",
      "validationrulegroup",
      "trackedentityattribute",
      "trackedentitytype",
      "programstage",
      "program",
      "eventreport",
      "eventchart",
      "programindicator",
      "programindicatorgroup",
      "relationshiptype",
      "externalmapLayer",
      "map",
      "report",
      "document",
      "visualization",
      "predictorgroup",
      "dashboard",
      "interpretation",
      "programstageinstancefilter",
      "keyjsonvalue"
    };

    String[] tablePkNames = {
      "attributeid",
      "userroleid",
      "usergroupid",
      "sqlviewid",
      "constantid",
      "optionsetid",
      "optiongroupid",
      "optiongroupsetid",
      "maplegendsetid",
      "orgunitgroupid",
      "orgunitgroupsetid",
      "categoryoptionid",
      "categoryoptiongroupid",
      "categoryoptiongroupSetid",
      "categoryid",
      "categorycomboid",
      "dataelementid",
      "dataelementgroupid",
      "dataelementgroupsetid",
      "indicatorid",
      "indicatorgroupid",
      "indicatorgroupsetid",
      "datasetid",
      "dataapprovallevelid",
      "workflowid",
      "validationruleid",
      "validationrulegroupid",
      "trackedentityattributeid",
      "trackedentitytypeid",
      "programstageid",
      "programid",
      "eventreportid",
      "eventchartid",
      "programindicatorid",
      "programindicatorgroupid",
      "relationshiptypeid",
      "externalmapLayerid",
      "mapid",
      "reportid",
      "documentid",
      "visualizationid",
      "predictorgroupid",
      "dashboardid",
      "interpretationid",
      "programstageinstancefilterid",
      "keyjsonvalueid"
    };

    String[] tableUserAccesses = {
      "attributeuseraccesses",
      "userroleuseraccesses",
      "usergroupuseraccesses",
      "sqlviewuseraccesses",
      "constantuseraccesses",
      "optionsetuseraccesses",
      "optiongroupuseraccesses",
      "optiongroupsetuseraccesses",
      "legendsetuseraccesses",
      "orgunitgroupuseraccesses",
      "orgunitgroupsetuseraccesses",
      "dataelementcategoryoptionuseraccesses",
      "categoryoptiongroupuseraccesses",
      "categoryoptiongroupSetuseraccesses",
      "dataelementcategoryuseraccesses",
      "categorycombouseraccesses",
      "dataelementuseraccesses",
      "dataelementgroupuseraccesses",
      "dataelementgroupsetuseraccesses",
      "indicatoruseraccesses",
      "indicatorgroupuseraccesses",
      "indicatorgroupsetuseraccesses",
      "datasetuseraccesses",
      "dataapprovalleveluseraccesses",
      "dataapprovalworkflowuseraccesses",
      "validationruleuseraccesses",
      "validationrulegroupuseraccesses",
      "trackedentityattributeuseraccesses",
      "trackedentitytypeuseraccesses",
      "programstageuseraccesses",
      "programuseraccesses",
      "eventreportuseraccesses",
      "eventchartuseraccesses",
      "programindicatoruseraccesses",
      "programindicatorgroupuseraccesses",
      "relationshiptypeuseraccesses",
      "externalmapLayeruseraccesses",
      "mapuseraccesses",
      "reportuseraccesses",
      "documentuseraccesses",
      "visualization_useraccesses",
      "predictorgroupuseraccesses",
      "dashboarduseraccesses",
      "interpretationuseraccesses",
      "programstageinstancefilteruseraccesses",
      "keyjsonvalueuseraccesses"
    };

    String[] tableUserGroupAccesses = {
      "attributeusergroupaccesses",
      "userroleusergroupaccesses",
      "usergroupusergroupaccesses",
      "sqlviewusergroupaccesses",
      "constantusergroupaccesses",
      "optionsetusergroupaccesses",
      "optiongroupusergroupaccesses",
      "optiongroupsetusergroupaccesses",
      "legendsetusergroupaccesses",
      "orgunitgroupusergroupaccesses",
      "orgunitgroupsetusergroupaccesses",
      "dataelementcategoryoptionusergroupaccesses",
      "categoryoptiongroupusergroupaccesses",
      "categoryoptiongroupSetusergroupaccesses",
      "dataelementcategoryusergroupaccesses",
      "categorycombousergroupaccesses",
      "dataelementusergroupaccesses",
      "dataelementgroupusergroupaccesses",
      "dataelementgroupsetusergroupaccesses",
      "indicatorusergroupaccesses",
      "indicatorgroupusergroupaccesses",
      "indicatorgroupsetusergroupaccesses",
      "datasetusergroupaccesses",
      "dataapprovallevelusergroupaccesses",
      "dataapprovalworkflowusergroupaccesses",
      "validationruleusergroupaccesses",
      "validationrulegroupusergroupaccesses",
      "trackedentityattributeusergroupaccesses",
      "trackedentitytypeusergroupaccesses",
      "programstageusergroupaccesses",
      "programusergroupaccesses",
      "eventreportusergroupaccesses",
      "eventchartusergroupaccesses",
      "programindicatorusergroupaccesses",
      "programindicatorgroupusergroupaccesses",
      "relationshiptypeusergroupaccesses",
      "externalmapLayerusergroupaccesses",
      "mapusergroupaccesses",
      "reportusergroupaccesses",
      "documentusergroupaccesses",
      "visualization_usergroupaccesses",
      "predictorgroupusergroupaccesses",
      "dashboardusergroupaccesses",
      "interpretationusergroupaccesses",
      "programstageinstancefilterusergroupaccesses",
      "keyjsonvalueusergroupaccesses"
    };

    for (int i = 0; i < tableNames.length; i++) {
      doMigration(
          context, tableNames[i], tableUserAccesses[i], tableUserGroupAccesses[i], tablePkNames[i]);
    }
  }

  private void doMigration(
      Context context,
      String tableName,
      String tableUserAccess,
      String tableUserGroupAccess,
      String tablePKName)
      throws SQLException {
    try (Statement statement = context.getConnection().createStatement()) {
      ResultSet resultSet =
          statement.executeQuery(
              "select exists ( select 1 from " + tableName + " where sharing = '{}')");
      resultSet.next();

      if (!resultSet.getBoolean(1)) {
        // Already done for this table
        return;
      }
    }

    String query =
        "update "
            + tableName
            + " _entity set sharing = to_jsonb(jsonb_build_object("
            + "'owner',nullif((select _owner.uid from userinfo _owner where _owner.userinfoid = _entity.userid),'' ),"
            + "'public',nullif(_entity.publicaccess,''),"
            + "'external', false,"
            + "'users',("
            + "select to_jsonb(coalesce(nullif(replace(array_to_string(array ( select json_build_object(_u.uid, jsonb_build_object('id', _u.uid, 'access', _ua.access)) "
            + "from  "
            + tableUserAccess
            + " _tua inner join useraccess _ua on _tua.useraccessid = _ua.useraccessid  "
            + "inner join userinfo _u on _ua.userid = _u.userinfoid "
            + "where _tua."
            + tablePKName
            + " = _entity."
            + tablePKName
            + " ) , ','), '}},{', '},'), ''), NULL)::json)"
            + "),"
            + "'userGroups',("
            + "select to_jsonb(coalesce(nullif(replace(array_to_string(array ( select  json_build_object(_ug.uid, jsonb_build_object('id', _ug.uid, 'access', _uga.access))"
            + "from  "
            + tableUserGroupAccess
            + " _tuga inner join usergroupaccess _uga on _tuga.usergroupaccessid = _uga.usergroupaccessid  "
            + "inner join usergroup _ug on _uga.usergroupid = _ug.usergroupid "
            + "where _tuga."
            + tablePKName
            + " = _entity."
            + tablePKName
            + " ) , ','), '}},{', '},'), ''), NULL)::json)"
            + ")));";

    try (Statement statement = context.getConnection().createStatement()) {
      log.info("Executing sharing migration query: [" + query + "]");
      statement.execute(query);
    } catch (SQLException e) {
      log.error(e.getMessage());
      throw e;
    }
  }
}
