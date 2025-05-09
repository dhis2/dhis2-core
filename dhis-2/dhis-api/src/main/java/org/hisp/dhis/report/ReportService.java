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
package org.hisp.dhis.report;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import net.sf.jasperreports.engine.JasperPrint;
import org.hisp.dhis.period.Period;

/**
 * @author Lars Helge Overland
 */
public interface ReportService {
  String ID = ReportService.class.getName();

  String REPORTTYPE_PDF = "pdf";

  String REPORTTYPE_XLS = "xls";

  String PARAM_RELATIVE_PERIODS = "periods";

  String PARAM_RELATIVE_ISO_PERIODS = "periods_iso";

  String PARAM_ORG_UNITS = "organisationunits";

  String PARAM_ORG_UNITS_UID = "organisationunits_uid";

  String PARAM_ORGANISATIONUNIT_LEVEL = "organisationunit_level";

  String PARAM_ORGANISATIONUNIT_LEVEL_COLUMN = "organisationunit_level_column";

  String PARAM_ORGANISATIONUNIT_UID_LEVEL_COLUMN = "organisationunit_uid_level_column";

  String PARAM_ORGANISATIONUNIT_COLUMN_NAME = "organisationunit_name";

  String PARAM_PERIOD_NAME = "period_name";

  /**
   * Renders a Jasper Report.
   *
   * <p>Will make the following params available:
   *
   * <p>"periods" String of relative period ids (String) "organisationunits" String of selected
   * organisation unit ids (String) "period_name" Name of the selected period (String)
   * "organisationunit_name" Name of the selected organisation unit (String)
   * "organisationunit_level" Level of the selected organisation unit (int)
   * "organisationunit_level_column" Name of the relevant level column in table
   * analytics_rs_orgunitstructure (String)
   *
   * @param out the OutputStream to write the report to.
   * @param reportUid the uid of the report to render.
   * @param period the period to use as parameter.
   * @param organisationUnitUid the uid of the org unit to use as parameter.
   * @param type the type of the report, can be "xls" and "pdf".
   */
  JasperPrint renderReport(
      OutputStream out, String reportUid, Period period, String organisationUnitUid, String type);

  /**
   * Renders and writes a HTML-based standard report to the given Writer.
   *
   * @param writer the Writer.
   * @param uid the report uid.
   * @param date the date.
   * @param ou the organisation unit uid.
   */
  void renderHtmlReport(Writer writer, String uid, Date date, String ou);

  /**
   * Saves a Report.
   *
   * @param report the Report to save.
   * @return the generated identifier.
   */
  long saveReport(Report report);

  /**
   * Retrieves the Report with the given identifier.
   *
   * @param id the identifier of the Report to retrieve.
   * @return the Report.
   */
  Report getReport(long id);

  /**
   * Retrieves the Report with the given uid.
   *
   * @param uid the uid of the Report to retrieve.
   * @return the Report.
   */
  Report getReport(String uid);

  /**
   * Deletes a Report.
   *
   * @param report the Report to delete.
   */
  void deleteReport(Report report);

  /**
   * Retrieves all Reports.
   *
   * @return a List of Reports.
   */
  List<Report> getAllReports();
}
