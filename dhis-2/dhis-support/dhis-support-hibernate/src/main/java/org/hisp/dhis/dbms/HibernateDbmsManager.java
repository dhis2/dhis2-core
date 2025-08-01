/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dbms;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HibernateDbmsManager implements DbmsManager {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final JdbcTemplate jdbcTemplate;

  private final EntityManager entityManager;

  private final HibernateCacheManager cacheManager;

  // -------------------------------------------------------------------------
  // DbmsManager implementation
  // -------------------------------------------------------------------------

  @Override
  public void emptyDatabase() {
    emptyTable("keyjsonvalue");

    emptyTable("maplegend");
    emptyTable("maplegendset");

    emptyTable("constant");
    emptyTable("sqlview");

    emptyTable("smscommandcodes");
    emptyTable("smscodes");
    emptyTable("smscommands");
    emptyTable("incomingsms");

    emptyTable("datavalueaudit");
    emptyTable("datavalue");
    emptyTable("completedatasetregistration");

    emptyTable("pushanalysisrecipientusergroups");
    emptyTable("pushanalysis");

    emptyTable("potentialduplicate");

    emptyTable("dashboarditem_users");
    emptyTable("dashboarditem_resources");
    emptyTable("dashboarditem_reports");
    emptyTable("dashboard_items");
    emptyTable("dashboarditem");
    emptyTable("dashboard");

    emptyTable("interpretation_comments");
    emptyTable("interpretationcomment");
    emptyTable("interpretation");

    emptyTable("report");
    emptyTable("datastatisticsevent");

    emptyTable("visualization_yearlyseries");
    emptyTable("visualization_rows");
    emptyTable("visualization_periods");
    emptyTable("visualization_orgunitlevels");
    emptyTable("visualization_orgunitgroupsetdimensions");
    emptyTable("visualization_organisationunits");
    emptyTable("visualization_itemorgunitgroups");
    emptyTable("visualization_filters");
    emptyTable("visualization_dataelementgroupsetdimensions");
    emptyTable("visualization_datadimensionitems");
    emptyTable("visualization_columns");
    emptyTable("visualization_categoryoptiongroupsetdimensions");
    emptyTable("visualization_categorydimensions");
    emptyTable("visualization_axis");
    emptyTable("axis");
    emptyTable("visualization");

    emptyTable("eventreport_attributedimensions");
    emptyTable("eventreport_columns");
    emptyTable("eventreport_dataelementdimensions");
    emptyTable("eventreport_filters");
    emptyTable("eventreport_itemorgunitgroups");
    emptyTable("eventreport_organisationunits");
    emptyTable("eventreport_orgunitgroupsetdimensions");
    emptyTable("eventreport_orgunitlevels");
    emptyTable("eventreport_periods");
    emptyTable("eventreport_programindicatordimensions");
    emptyTable("eventreport_rows");
    emptyTable("eventreport");

    emptyTable("eventchart_attributedimensions");
    emptyTable("eventchart_columns");
    emptyTable("eventchart_dataelementdimensions");
    emptyTable("eventchart_filters");
    emptyTable("eventchart_itemorgunitgroups");
    emptyTable("eventchart_organisationunits");
    emptyTable("eventchart_orgunitgroupsetdimensions");
    emptyTable("eventchart_orgunitlevels");
    emptyTable("eventchart_periods");
    emptyTable("eventchart_programindicatordimensions");
    emptyTable("eventchart_rows");
    emptyTable("eventchart");

    emptyTable("eventvisualization_attributedimensions");
    emptyTable("eventvisualization_columns");
    emptyTable("eventvisualization_dataelementdimensions");
    emptyTable("eventvisualization_filters");
    emptyTable("eventvisualization_itemorgunitgroups");
    emptyTable("eventvisualization_organisationunits");
    emptyTable("eventvisualization_orgunitgroupsetdimensions");
    emptyTable("eventvisualization_orgunitlevels");
    emptyTable("eventvisualization_periods");
    emptyTable("eventvisualization_programindicatordimensions");
    emptyTable("eventvisualization_rows");
    emptyTable("eventvisualization");

    emptyTable("dataelementgroupsetdimension_items");
    emptyTable("dataelementgroupsetdimension");
    emptyTable("categoryoptiongroupsetdimension");
    emptyTable("categoryoptiongroupsetdimension_items");
    emptyTable("orgunitgroupsetdimension_items");
    emptyTable("orgunitgroupsetdimension");

    emptyTable("program_userroles");

    emptyTable("users_catdimensionconstraints");
    emptyTable("users_cogsdimensionconstraints");
    emptyTable("userrolemembers");
    emptyTable("userroleauthorities");
    emptyTable("userdatavieworgunits");
    emptyTable("usermembership");
    emptyTable("userrole");

    emptyTable("orgunitgroupsetmembers");
    emptyTable("orgunitgroupset");

    emptyTable("orgunitgroupmembers");
    emptyTable("orgunitgroup");

    emptyTable("validationrulegroupmembers");
    emptyTable("validationrulegroup");

    emptyTable("validationresult");

    emptyTable("validationrule");

    emptyTable("dataapproval");

    emptyTable("lockexception");

    emptyTable("sectiongreyedfields");
    emptyTable("sectiondataelements");
    emptyTable("sectionindicators");
    emptyTable("section");

    emptyTable("datasetsource");
    emptyTable("datasetelement");
    emptyTable("datasetindicators");
    emptyTable("datasetoperands");
    emptyTable("dataset");

    emptyTable("dataapprovalaudit");
    emptyTable("dataapprovalworkflowlevels");
    emptyTable("dataapprovalworkflow");
    emptyTable("dataapprovallevel");

    emptyTable("predictorgroupmembers");
    emptyTable("predictorgroup");

    emptyTable("predictororgunitlevels");
    emptyTable("predictor");

    emptyTable("datadimensionitem");

    emptyTable("programrulevariable");
    emptyTable("programruleaction");
    emptyTable("programrule");

    emptyRelationships();

    emptyTable("programnotificationinstance");
    emptyTable("trackedentitydatavalueaudit");
    emptyTable("trackereventchangelog");
    emptyTable("singleeventchangelog");
    emptyTable("trackedentityprogramowner");

    emptyTable("programmessage_phonenumbers");
    emptyTable("programmessage_emailaddresses");
    emptyTable("programmessage_deliverychannels");
    emptyTable("programmessage");

    emptyTable("trackerevent_notes");
    emptyTable("singleevent_notes");
    emptyTable("enrollment_notes");
    emptyTable("note");
    emptyTable("event");
    emptyTable("enrollment");
    emptyTable("programnotificationtemplate");
    emptyTable("programstagedataelement");
    emptyTable("programstagesection_dataelements");
    emptyTable("programstagesection");
    emptyTable("programstage");
    emptyTable("program_organisationunits");
    emptyTable("program_attributes");
    emptyTable("periodboundary");
    emptyTable("programindicator");
    emptyTable("programownershiphistory");
    emptyTable("programtempownershipaudit");
    emptyTable("programtempowner");
    emptyTable("program");

    emptyTable("eventfilter");

    emptyTable("trackedentityattributevalue");
    emptyTable("trackedentityattributevalueaudit");
    emptyTable("trackedentitychangelog");
    emptyTable("trackedentitytypeattribute");
    emptyTable("trackedentityattribute");
    emptyTable("trackedentity");
    emptyTable("trackedentitytype");

    emptyTable("minmaxdataelement");

    emptyTable("dataelementgroupsetmembers");
    emptyTable("dataelementgroupset");

    emptyTable("dataelementgroupmembers");
    emptyTable("dataelementgroup");

    emptyTable("dataelementaggregationlevels");
    emptyTable("dataelementoperand");
    emptyTable("dataelement");

    emptyTable("categoryoptioncombos_categoryoptions");
    emptyTable("categorycombos_optioncombos");
    emptyTable("categorycombos_categories");
    emptyTable("categories_categoryoptions");

    emptyTable("userteisearchorgunits");
    emptyTable("categoryoption_organisationunits");
    emptyTable("userdatavieworgunits");
    emptyTable("organisationunit");
    emptyTable("orgunitlevel");

    emptyTable("version");
    emptyTable("deletedobject");
    emptyTable("period");

    emptyTable("configuration");
    emptyTable("indicatorgroupsetmembers");
    emptyTable("indicatorgroupset");

    emptyTable("indicatorgroupmembers");
    emptyTable("indicatorgroup");

    emptyTable("indicator");
    emptyTable("indicatortype");

    emptyTable("categoryoptiongroupsetmembers");
    emptyTable("categoryoptiongroupset");

    emptyTable("categoryoptiongroupmembers");
    emptyTable("categoryoptiongroup");

    emptyTable("expression");
    emptyTable("expressiondimensionitem");
    emptyTable("categoryoptioncombo");
    emptyTable("categorycombo");
    emptyTable("category");
    emptyTable("categoryoption");

    emptyTable("optionvalue");
    emptyTable("optionset");

    emptyTable("systemsetting");

    emptyTable("attribute");

    emptyTable("messageconversation_usermessages");
    emptyTable("usermessage");
    emptyTable("messageconversation_messages");
    emptyTable("messageconversation");
    emptyTable("message");

    emptyTable("usergroupmembers");
    emptyTable("usergroup");

    emptyTable("previouspasswords");
    emptyTable("usersetting");
    emptyTable("fileresource");
    emptyTable("jobconfiguration");
    emptyTable("route");

    dropTable("analytics_rs_orgunitstructure");
    dropTable("analytics_rs_datasetorganisationunitcategory");
    dropTable("analytics_rs_categoryoptioncomboname");
    dropTable("analytics_rs_dataelementgroupsetstructure");
    dropTable("analytics_rs_indicatorgroupsetstructure");
    dropTable("analytics_rs_organisationunitgroupsetstructure");
    dropTable("analytics_rs_categorystructure");
    dropTable("analytics_rs_dataelementstructure");
    dropTable("analytics_rs_dateperiodstructure");
    dropTable("analytics_rs_periodstructure");
    dropTable("analytics_rs_dataelementcategoryoptioncombo");
    dropTable("analytics_rs_dataapprovalremaplevel");
    dropTable("analytics_rs_dataapprovalminlevel");

    emptyTable("reservedvalue");
    emptyTable("sequentialnumbercounter");

    emptyTable("audit");
    emptyTable("eventhook");
    emptyTable("dataentryform");

    // userinfo should be last, since many tables has a foreign key to it
    emptyTable("userinfo");

    log.debug("Cleared database contents");

    cacheManager.clearCache();

    log.debug("Cleared Hibernate cache");

    flushSession();
  }

  @Override
  public void clearSession() {
    entityManager.flush();
    entityManager.clear();
  }

  @Override
  public void flushSession() {
    entityManager.flush();
  }

  @Override
  public void emptyTable(String table) {
    try {
      jdbcTemplate.update("delete from " + table);
    } catch (BadSqlGrammarException ex) {
      log.debug("Table " + table + " does not exist");
    }
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private void dropTable(String table) {
    try {
      String sql = String.format("drop table if exists %s;", table);
      jdbcTemplate.execute(sql);
    } catch (BadSqlGrammarException ex) {
      log.debug("Table " + table + " does not exist");
    }
  }

  private void emptyRelationships() {
    try {
      String sql =
          """
          update relationshipitem set relationshipid = null;
          delete from relationship;
          delete from relationshipitem;
          update relationshiptype set from_relationshipconstraintid = null,to_relationshipconstraintid = null;
          delete from relationshipconstraint;
          delete from relationshiptype;
          """;
      jdbcTemplate.update(sql);
    } catch (BadSqlGrammarException ex) {
      log.debug("Could not empty relationship tables");
    }
  }
}
