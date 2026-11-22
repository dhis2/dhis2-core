/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.HttpStatus.CREATED;
import static org.hisp.dhis.web.HttpStatus.OK;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.webapi.utils.DhisMockMvcControllerTest.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.report.ReportType;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient.HttpResponse;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller tests for the {@code jasper.reports.enabled} feature toggle. When the toggle is off
 * (the default), introducing or changing a Jasper design and rendering a Jasper report are
 * rejected, including for superusers and for imports that skip validation. HTML authoring and
 * rendering, and deleting reports, remain available. When the toggle is on, only superusers can
 * introduce or change Jasper designs or give a stored report another Jasper type; other report
 * authors keep HTML authoring and edits that keep the design. The direct design upload endpoint
 * always requires ALL, independently of report type and the toggle.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class ReportControllerTest extends DhisControllerConvenienceTest {

  private static final String DESIGN = "<jasperReport/>";

  private static final String OTHER_DESIGN = "<jasperReport><detail/></jasperReport>";

  private static final String HTML_DESIGN = "<html><body><h1>Initial report</h1></body></html>";

  private static final String OTHER_HTML_DESIGN =
      "<html><body><h1>Edited report</h1></body></html>";

  @Autowired private DhisConfigurationProvider config;

  @AfterEach
  void resetJasperReportsToggle() {
    // remove the override so the next test sees the default (off)
    properties().remove(ConfigurationKey.JASPER_REPORTS_ENABLED.getKey());
  }

  // --- OFF (default): Jasper authoring is blocked on every write path --------

  @Test
  void createWithDesignBlockedWhenDisabledForSuperuser() {
    JsonWebMessage message =
        assertWebMessage(HttpStatus.CONFLICT, POST("/reports/", reportBody("Superuser", DESIGN)));
    assertNotNull(message.find(JsonErrorReport.class, e -> e.getErrorCode() == ErrorCode.E1007));
  }

  @Test
  void createWithDesignBlockedWhenDisabledForReportAuthor() {
    switchToNewUser("reportauthor", "F_REPORT_PUBLIC_ADD");
    JsonWebMessage message =
        assertWebMessage(HttpStatus.CONFLICT, POST("/reports/", reportBody("Author", DESIGN)));
    assertNotNull(message.find(JsonErrorReport.class, e -> e.getErrorCode() == ErrorCode.E1007));
  }

  @Test
  void createWithDesignViaMetadataBlockedWhenDisabled() {
    JsonWebMessage message =
        POST("/metadata", metadataBody("Meta", DESIGN))
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    assertNotNull(message.find(JsonErrorReport.class, e -> e.getErrorCode() == ErrorCode.E1007));
  }

  @Test
  void updateReportDesignEndpointBlockedWhenDisabled() {
    enableJasperReports();
    String uid = assertStatus(CREATED, POST("/reports/", reportBody("Endpoint", DESIGN)));
    disableJasperReports();

    JsonWebMessage message =
        assertWebMessage(HttpStatus.CONFLICT, PUT("/reports/" + uid + "/design", OTHER_DESIGN));
    assertEquals(ErrorCode.E1007, message.getErrorCode());
    assertEquals(DESIGN, GET("/reports/" + uid + "/design").content("application/xml"));
  }

  @Test
  void changingDesignOnUpdateBlockedWhenDisabled() {
    enableJasperReports();
    String uid = assertStatus(CREATED, POST("/reports/", reportBody("Update", DESIGN)));
    disableJasperReports();

    JsonWebMessage message =
        assertWebMessage(
            HttpStatus.CONFLICT, PUT("/reports/" + uid, reportBody("Update", OTHER_DESIGN)));
    assertNotNull(message.find(JsonErrorReport.class, e -> e.getErrorCode() == ErrorCode.E1007));
  }

  @Test
  void changingDesignViaJsonPatchBlockedWhenDisabled() {
    enableJasperReports();
    String uid = assertStatus(CREATED, POST("/reports/", reportBody("Patch", DESIGN)));
    disableJasperReports();

    JsonWebMessage message =
        assertWebMessage(
            HttpStatus.CONFLICT,
            PATCH(
                "/reports/" + uid,
                "[{'op':'replace','path':'/designContent','value':'" + OTHER_DESIGN + "'}]"));
    assertNotNull(message.find(JsonErrorReport.class, e -> e.getErrorCode() == ErrorCode.E1007));
  }

  @Test
  void createWithDesignWithSkipValidationBlockedWhenDisabled() {
    assertCommitRejected(
        POST(
            "/metadata?skipValidation=true",
            "{'reports':[" + reportBodyWithId("JasperSkp01", "Skip meta", DESIGN) + "]}"));
    assertCommitRejected(
        POST("/reports?skipValidation=true", reportBodyWithId("JasperSkp02", "Skip", DESIGN)));

    assertNull(getReport("JasperSkp01"));
    assertNull(getReport("JasperSkp02"));
  }

  @Test
  void changingDesignWithSkipValidationBlockedWhenDisabled() {
    enableJasperReports();
    assertStatus(CREATED, POST("/reports/", reportBodyWithId("JasperSkp03", "Skip", DESIGN)));
    disableJasperReports();

    assertCommitRejected(
        POST(
            "/metadata?importStrategy=UPDATE&skipValidation=true",
            "{'reports':[" + reportBodyWithId("JasperSkp03", "Skip", OTHER_DESIGN) + "]}"));
    assertCommitRejected(
        PUT(
            "/reports/JasperSkp03?skipValidation=true",
            reportBodyWithId("JasperSkp03", "Skip", OTHER_DESIGN)));

    assertEquals(DESIGN, getReport("JasperSkp03").getDesignContent());
  }

  // --- OFF (default): rendering a stored Jasper design is blocked ------------

  @Test
  void renderPdfBlockedWhenDisabled() {
    enableJasperReports();
    String uid = assertStatus(CREATED, POST("/reports/", reportBody("Render", DESIGN)));
    disableJasperReports();

    JsonWebMessage message =
        assertWebMessage(HttpStatus.CONFLICT, GET("/reports/" + uid + "/data.pdf"));
    assertEquals(ErrorCode.E1007, message.getErrorCode());
  }

  @Test
  void renderXlsBlockedWhenDisabled() {
    enableJasperReports();
    String uid = assertStatus(CREATED, POST("/reports/", reportBody("RenderXls", DESIGN)));
    disableJasperReports();

    JsonWebMessage message =
        assertWebMessage(HttpStatus.CONFLICT, GET("/reports/" + uid + "/data.xls"));
    assertEquals(ErrorCode.E1007, message.getErrorCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"data", "data.html"})
  void renderOtherFormatsBlockedWhenDisabled(String path) {
    enableJasperReports();
    String uid = assertStatus(CREATED, POST("/reports/", reportBody("Render " + path, DESIGN)));
    disableJasperReports();

    JsonWebMessage message =
        assertWebMessage(HttpStatus.CONFLICT, GET("/reports/" + uid + "/" + path));
    assertEquals(ErrorCode.E1007, message.getErrorCode());
    assertEquals(
        "Feature is disabled by configuration, requires `jasper.reports.enabled = on` in dhis.conf",
        message.getMessage());
  }

  // --- OFF (default): HTML authoring and rendering remain available ----------

  @ParameterizedTest
  @ValueSource(strings = {"PUT", "PATCH"})
  void createAndUpdateHtmlAllowedWhenDisabledForReportAuthor(String method) {
    switchToNewUser("htmlauthor", "F_REPORT_PUBLIC_ADD");
    String uid =
        assertStatus(
            CREATED,
            POST(
                "/reports/",
                """
                {"name":"HTML","type":"HTML","designContent":"%s"}"""
                    .formatted(HTML_DESIGN)));
    assertEquals(HTML_DESIGN, GET("/reports/" + uid + "/design").content("text/html"));

    if ("PUT".equals(method)) {
      assertStatus(
          OK,
          PUT(
              "/reports/" + uid,
              """
              {"name":"HTML","type":"HTML","designContent":"%s"}"""
                  .formatted(OTHER_HTML_DESIGN)));
    } else {
      assertStatus(
          OK,
          PATCH(
              "/reports/" + uid,
              "[{'op':'replace','path':'/designContent','value':'" + OTHER_HTML_DESIGN + "'}]"));
    }

    JsonMixed stored = GET("/reports/" + uid + "?fields=type,designContent").content(OK);
    assertEquals("HTML", stored.getString("type").string());
    assertEquals(OTHER_HTML_DESIGN, stored.getString("designContent").string());
  }

  @Test
  void createAndUpdateHtmlViaMetadataAllowedWhenDisabledForReportAuthor() {
    switchToNewUser("htmlimporter", "F_REPORT_PUBLIC_ADD");
    String uid = "HtmReport01";
    String body =
        """
        {"reports":[{"id":"%s","name":"HTML import","type":"HTML","designContent":"%s"}]}""";
    assertStatus(OK, POST("/metadata", body.formatted(uid, HTML_DESIGN)));
    assertEquals(HTML_DESIGN, GET("/reports/" + uid + "/design").content("text/html"));

    assertStatus(OK, POST("/metadata", body.formatted(uid, OTHER_HTML_DESIGN)));
    JsonMixed stored = GET("/reports/" + uid + "?fields=type,designContent").content(OK);
    assertEquals("HTML", stored.getString("type").string());
    assertEquals(OTHER_HTML_DESIGN, stored.getString("designContent").string());
  }

  @Test
  void uploadAndRenderHtmlAllowedWhenDisabledForSuperuser() {
    String uid =
        assertStatus(
            CREATED,
            POST(
                "/reports/",
                """
                {"name":"HTML upload","type":"HTML","designContent":"%s"}"""
                    .formatted(HTML_DESIGN)));

    assertStatus(HttpStatus.NO_CONTENT, PUT("/reports/" + uid + "/design", OTHER_HTML_DESIGN));

    assertEquals(OTHER_HTML_DESIGN, GET("/reports/" + uid + "/design").content("text/html"));
    assertTrue(
        GET("/reports/" + uid + "/data.html").content("text/html").contains(OTHER_HTML_DESIGN));
  }

  @Test
  void uploadHtmlForbiddenWhenDisabledForReportAuthor() {
    switchToNewUser("htmluploader", "F_REPORT_PUBLIC_ADD");
    String uid =
        assertStatus(
            CREATED,
            POST(
                "/reports/",
                """
                {"name":"HTML upload","type":"HTML","designContent":"%s"}"""
                    .formatted(HTML_DESIGN)));

    assertStatus(HttpStatus.FORBIDDEN, PUT("/reports/" + uid + "/design", OTHER_HTML_DESIGN));
    assertEquals(HTML_DESIGN, GET("/reports/" + uid + "/design").content("text/html"));
  }

  @ParameterizedTest
  @CsvSource({
    "PUT, JASPER_JDBC",
    "PUT, JASPER_REPORT_TABLE",
    "PUT, null",
    "PATCH, JASPER_JDBC",
    "PATCH, JASPER_REPORT_TABLE",
    "PATCH, null",
    "METADATA, JASPER_JDBC",
    "METADATA, JASPER_REPORT_TABLE",
    "METADATA, null"
  })
  void promotingHtmlWithUnchangedDesignBlockedWhenDisabled(String method, String type) {
    enableJasperReports();
    String uid =
        assertStatus(
            CREATED,
            POST(
                "/reports/",
                """
                {"name":"HTML promotion","type":"HTML","designContent":"%s"}"""
                    .formatted(HTML_DESIGN)));
    disableJasperReports();

    String jsonType = "null".equals(type) ? "null" : "\"" + type + "\"";
    String body =
        """
        {"id":"%s","name":"HTML promotion","type":%s,"designContent":"%s"}"""
            .formatted(uid, jsonType, HTML_DESIGN);
    JsonWebMessage message =
        switch (method) {
          case "PUT" ->
              PUT("/reports/" + uid, body).content(HttpStatus.CONFLICT).as(JsonWebMessage.class);
          case "PATCH" ->
              PATCH("/reports/" + uid, "[{'op':'replace','path':'/type','value':" + jsonType + "}]")
                  .content(HttpStatus.CONFLICT)
                  .as(JsonWebMessage.class);
          default ->
              POST("/metadata", "{\"reports\":[" + body + "]}")
                  .content(HttpStatus.CONFLICT)
                  .as(JsonWebMessage.class);
        };
    assertNotNull(message.find(JsonErrorReport.class, e -> e.getErrorCode() == ErrorCode.E1007));

    JsonMixed stored = GET("/reports/" + uid + "?fields=type,designContent").content(OK);
    assertEquals("HTML", stored.getString("type").string());
    assertEquals(HTML_DESIGN, stored.getString("designContent").string());
  }

  // --- OFF (default): unrelated report work still succeeds -------------------

  @Test
  void createWithoutDesignAllowedWhenDisabled() {
    assertStatus(CREATED, POST("/reports/", reportBody("Standard", null)));
  }

  @Test
  void editingReportWithUnchangedDesignAllowedWhenDisabled() {
    enableJasperReports();
    String uid = assertStatus(CREATED, POST("/reports/", reportBody("Keep", DESIGN)));
    disableJasperReports();

    // the design content is unchanged, so editing other fields must still be allowed
    assertStatus(OK, PUT("/reports/" + uid, reportBody("Keep renamed", DESIGN)));
  }

  @Test
  void changingHtmlTypeWithoutDesignStoresNoDesignWhenDisabled() {
    String uid =
        assertStatus(
            CREATED,
            POST(
                "/reports/",
                """
                {"name":"HTML retype","type":"HTML","designContent":"%s"}"""
                    .formatted(HTML_DESIGN)));

    // an update replaces every property, so the HTML content does not stay as a Jasper design
    assertStatus(
        OK,
        PUT(
            "/reports/" + uid,
            """
            {"name":"HTML retype","type":"JASPER_REPORT_TABLE"}"""));

    Report stored = getReport(uid);
    assertEquals(ReportType.JASPER_REPORT_TABLE, stored.getType());
    assertNull(stored.getDesignContent());
  }

  @Test
  void changingHtmlTypeWithoutDesignAllowedWhenDisabledForReportAuthor() {
    switchToNewUser("htmlretyper", "F_REPORT_PUBLIC_ADD");
    String uid =
        assertStatus(CREATED, POST("/reports/", htmlReportBody("HTML retype", HTML_DESIGN)));

    // the superuser rule applies only while the setting is on
    assertStatus(
        OK,
        PUT(
            "/reports/" + uid,
            """
            {"name":"HTML retype","type":"JASPER_REPORT_TABLE"}"""));

    switchToSuperuser();
    Report stored = getReport(uid);
    assertEquals(ReportType.JASPER_REPORT_TABLE, stored.getType());
    assertNull(stored.getDesignContent());
  }

  @Test
  void deletingReportWithDesignAllowedWhenDisabled() {
    enableJasperReports();
    assertStatus(CREATED, POST("/reports/", reportBodyWithId("JasperDel01", "Delete", DESIGN)));
    assertStatus(CREATED, POST("/reports/", reportBodyWithId("JasperDel02", "Delete", DESIGN)));
    disableJasperReports();

    assertStatus(OK, DELETE("/reports/JasperDel01"));
    assertStatus(OK, POST("/metadata?importStrategy=DELETE", "{'reports':[{'id':'JasperDel02'}]}"));

    assertNull(getReport("JasperDel01"));
    assertNull(getReport("JasperDel02"));
  }

  @Test
  void metadataImportWithoutAtomicityStillImportsOtherReportsWhenDisabled() {
    JsonWebMessage message =
        POST(
                "/metadata?atomicMode=NONE",
                "{'reports':["
                    + reportBodyWithId("JasperAtm01", "Jasper", DESIGN)
                    + ","
                    + """
                    {"id":"HtmlAtm0001","name":"HTML","type":"HTML","designContent":"%s"}"""
                        .formatted(HTML_DESIGN)
                    + "]}")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);

    assertNotNull(message.find(JsonErrorReport.class, e -> e.getErrorCode() == ErrorCode.E1007));
    assertNull(getReport("JasperAtm01"));
    assertEquals(HTML_DESIGN, getReport("HtmlAtm0001").getDesignContent());
  }

  @Test
  void htmlWithSkipValidationAllowedWhenDisabled() {
    assertStatus(
        OK,
        POST(
            "/metadata?skipValidation=true",
            """
            {"reports":[{"id":"HtmlSkip001","name":"HTML skip","type":"HTML","designContent":"%s"}]}"""
                .formatted(HTML_DESIGN)));

    assertEquals(HTML_DESIGN, getReport("HtmlSkip001").getDesignContent());
  }

  // --- ON: only superusers can create or change Jasper designs ---------------

  @Test
  void createWithDesignBlockedWhenEnabledForReportAuthor() {
    enableJasperReports();
    switchToNewUser("enabledauthor", "F_REPORT_PUBLIC_ADD");

    assertRejected(
        ErrorCode.E3000, POST("/reports/", reportBodyWithId("JasperAut01", "Author", DESIGN)));
    assertRejected(
        ErrorCode.E3000,
        POST("/reports?skipValidation=true", reportBodyWithId("JasperAut02", "Author", DESIGN)));
    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata",
            "{'reports':[" + reportBodyWithId("JasperAut03", "Author", DESIGN) + "]}"));
    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata?skipValidation=true",
            "{'reports':[" + reportBodyWithId("JasperAut04", "Author", DESIGN) + "]}"));

    switchToSuperuser();
    assertNull(getReport("JasperAut01"));
    assertNull(getReport("JasperAut02"));
    assertNull(getReport("JasperAut03"));
    assertNull(getReport("JasperAut04"));
  }

  @Test
  void changingDesignBlockedWhenEnabledForReportAuthor() {
    enableJasperReports();
    assertStatus(
        CREATED,
        POST(
            "/reports/", sharedReportBody("JasperAut05", "Shared", "JASPER_REPORT_TABLE", DESIGN)));
    switchToNewUser("designeditor", "F_REPORT_PUBLIC_ADD");

    String changed = sharedReportBody("JasperAut05", "Shared", "JASPER_REPORT_TABLE", OTHER_DESIGN);
    assertRejected(ErrorCode.E3001, PUT("/reports/JasperAut05", changed));
    assertRejected(ErrorCode.E3001, PUT("/reports/JasperAut05?skipValidation=true", changed));
    assertRejected(
        ErrorCode.E3001,
        PATCH(
            "/reports/JasperAut05",
            "[{'op':'replace','path':'/designContent','value':'" + OTHER_DESIGN + "'}]"));
    assertRejected(
        ErrorCode.E3001, POST("/metadata?importStrategy=UPDATE", "{'reports':[" + changed + "]}"));
    assertRejected(
        ErrorCode.E3001,
        POST(
            "/metadata?importStrategy=UPDATE&skipValidation=true",
            "{'reports':[" + changed + "]}"));
    assertStatus(HttpStatus.FORBIDDEN, PUT("/reports/JasperAut05/design", OTHER_DESIGN));

    switchToSuperuser();
    assertEquals(DESIGN, getReport("JasperAut05").getDesignContent());
  }

  @ParameterizedTest
  @CsvSource({"PUT, JASPER_JDBC", "PATCH, JASPER_REPORT_TABLE", "METADATA, null"})
  void promotingHtmlBlockedWhenEnabledForReportAuthor(String method, String type) {
    enableJasperReports();
    switchToNewUser("htmlpromoter", "F_REPORT_PUBLIC_ADD");
    String uid =
        assertStatus(CREATED, POST("/reports/", htmlReportBody("HTML promotion", HTML_DESIGN)));

    String jsonType = "null".equals(type) ? "null" : "\"" + type + "\"";
    String body =
        """
        {"id":"%s","name":"HTML promotion","type":%s,"designContent":"%s"}"""
            .formatted(uid, jsonType, HTML_DESIGN);
    HttpResponse response =
        switch (method) {
          case "PUT" -> PUT("/reports/" + uid, body);
          case "PATCH" ->
              PATCH(
                  "/reports/" + uid, "[{'op':'replace','path':'/type','value':" + jsonType + "}]");
          default -> POST("/metadata", "{\"reports\":[" + body + "]}");
        };
    assertRejected(ErrorCode.E3001, response);

    JsonMixed stored = GET("/reports/" + uid + "?fields=type,designContent").content(OK);
    assertEquals("HTML", stored.getString("type").string());
    assertEquals(HTML_DESIGN, stored.getString("designContent").string());
  }

  @Test
  void changingTypeBlockedWhenEnabledForReportAuthor() {
    enableJasperReports();
    assertStatus(
        CREATED,
        POST(
            "/reports/", sharedReportBody("JasperAut06", "Shared", "JASPER_REPORT_TABLE", DESIGN)));
    switchToNewUser("typeeditor", "F_REPORT_PUBLIC_ADD");
    String uid =
        assertStatus(CREATED, POST("/reports/", htmlReportBody("HTML retype", HTML_DESIGN)));

    // another Jasper type for the stored design
    assertRejected(
        ErrorCode.E3001,
        PUT(
            "/reports/JasperAut06",
            sharedReportBody("JasperAut06", "Shared", "JASPER_JDBC", DESIGN)));
    // no type for the stored design (a missing type counts as non-HTML)
    assertRejected(
        ErrorCode.E3001,
        PUT(
            "/reports/JasperAut06",
            """
            {"name":"Shared","sharing":{"public":"rw------"},"designContent":"%s"}"""
                .formatted(DESIGN)));
    // a Jasper type without a design
    assertRejected(
        ErrorCode.E3001,
        PUT(
            "/reports/" + uid,
            """
            {"name":"HTML retype","type":"JASPER_REPORT_TABLE"}"""));

    switchToSuperuser();
    Report jasper = getReport("JasperAut06");
    assertEquals(ReportType.JASPER_REPORT_TABLE, jasper.getType());
    assertEquals(DESIGN, jasper.getDesignContent());
    Report html = getReport(uid);
    assertEquals(ReportType.HTML, html.getType());
    assertEquals(HTML_DESIGN, html.getDesignContent());
  }

  @Test
  void metadataImportWithoutAtomicityStillImportsHtmlReportsWhenEnabledForReportAuthor() {
    enableJasperReports();
    switchToNewUser("atomicauthor", "F_REPORT_PUBLIC_ADD");

    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata?atomicMode=NONE",
            "{'reports':["
                + reportBodyWithId("JasperAtm02", "Jasper", DESIGN)
                + ","
                + """
                {"id":"HtmlAtm0002","name":"HTML","type":"HTML","designContent":"%s"}"""
                    .formatted(HTML_DESIGN)
                + "]}"));

    switchToSuperuser();
    assertNull(getReport("JasperAtm02"));
    assertEquals(HTML_DESIGN, getReport("HtmlAtm0002").getDesignContent());
  }

  @Test
  void designChangesBlockedWhenEnabledForUserWithoutAllAuthority() {
    enableJasperReports();
    assertStatus(
        CREATED,
        POST(
            "/reports/", sharedReportBody("JasperAut07", "Shared", "JASPER_REPORT_TABLE", DESIGN)));
    switchToNewUser(
        "notsuperuser",
        "F_REPORT_PUBLIC_ADD",
        "F_REPORT_DELETE",
        "F_METADATA_IMPORT",
        "F_METADATA_EXPORT",
        "F_METADATA_MANAGE",
        "F_USER_ADD",
        "F_USERROLE_PUBLIC_ADD",
        "F_SYSTEM_SETTING",
        "F_PERFORM_MAINTENANCE");
    String html =
        assertStatus(CREATED, POST("/reports/", htmlReportBody("HTML no ALL", HTML_DESIGN)));

    // a new report with a design
    assertRejected(
        ErrorCode.E3000, POST("/reports/", reportBodyWithId("JasperAut10", "No ALL", DESIGN)));
    assertRejected(
        ErrorCode.E3000,
        POST("/reports?skipValidation=true", reportBodyWithId("JasperAut11", "No ALL", DESIGN)));
    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata",
            "{'reports':[" + reportBodyWithId("JasperAut12", "No ALL", DESIGN) + "]}"));
    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata?skipValidation=true",
            "{'reports':[" + reportBodyWithId("JasperAut08", "No ALL", DESIGN) + "]}"));
    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata?atomicMode=NONE",
            "{'reports':["
                + reportBodyWithId("JasperAtm03", "Jasper", DESIGN)
                + ","
                + """
                {"id":"HtmlAtm0003","name":"HTML","type":"HTML","designContent":"%s"}"""
                    .formatted(HTML_DESIGN)
                + "]}"));

    // another design for the stored report
    String changed = sharedReportBody("JasperAut07", "Shared", "JASPER_REPORT_TABLE", OTHER_DESIGN);
    assertRejected(ErrorCode.E3001, PUT("/reports/JasperAut07", changed));
    assertRejected(ErrorCode.E3001, PUT("/reports/JasperAut07?skipValidation=true", changed));
    assertRejected(
        ErrorCode.E3001,
        PATCH(
            "/reports/JasperAut07",
            "[{'op':'replace','path':'/designContent','value':'" + OTHER_DESIGN + "'}]"));
    assertRejected(
        ErrorCode.E3001, POST("/metadata?importStrategy=UPDATE", "{'reports':[" + changed + "]}"));
    assertRejected(
        ErrorCode.E3001,
        POST(
            "/metadata?importStrategy=UPDATE&skipValidation=true",
            "{'reports':[" + changed + "]}"));
    assertStatus(HttpStatus.FORBIDDEN, PUT("/reports/JasperAut07/design", OTHER_DESIGN));

    // another Jasper type for the stored report, and a Jasper type for an HTML report
    assertRejected(
        ErrorCode.E3001,
        PUT(
            "/reports/JasperAut07",
            sharedReportBody("JasperAut07", "Shared", "JASPER_JDBC", DESIGN)));
    assertRejected(
        ErrorCode.E3001,
        PATCH("/reports/JasperAut07", "[{'op':'replace','path':'/type','value':'JASPER_JDBC'}]"));
    assertRejected(
        ErrorCode.E3001,
        PUT(
            "/reports/" + html,
            """
            {"name":"HTML no ALL","type":"JASPER_JDBC","designContent":"%s"}"""
                .formatted(HTML_DESIGN)));
    assertRejected(
        ErrorCode.E3001,
        PATCH(
            "/reports/" + html, "[{'op':'replace','path':'/type','value':'JASPER_REPORT_TABLE'}]"));

    switchToSuperuser();
    assertNull(getReport("JasperAut10"));
    assertNull(getReport("JasperAut11"));
    assertNull(getReport("JasperAut12"));
    assertNull(getReport("JasperAut08"));
    assertNull(getReport("JasperAtm03"));
    assertEquals(HTML_DESIGN, getReport("HtmlAtm0003").getDesignContent());
    Report stored = getReport("JasperAut07");
    assertEquals(ReportType.JASPER_REPORT_TABLE, stored.getType());
    assertEquals(DESIGN, stored.getDesignContent());
    Report own = getReport(html);
    assertEquals(ReportType.HTML, own.getType());
    assertEquals(HTML_DESIGN, own.getDesignContent());
  }

  @Test
  void changingDesignAllowedWhenEnabled() {
    enableJasperReports();
    assertStatus(CREATED, POST("/reports/", reportBodyWithId("JasperOn001", "On", DESIGN)));

    assertStatus(
        OK, PUT("/reports/JasperOn001", reportBodyWithId("JasperOn001", "On", OTHER_DESIGN)));
    assertEquals(OTHER_DESIGN, getReport("JasperOn001").getDesignContent());

    assertStatus(HttpStatus.NO_CONTENT, PUT("/reports/JasperOn001/design", DESIGN));
    assertEquals(DESIGN, getReport("JasperOn001").getDesignContent());
  }

  @Test
  void skipValidationImportAllowedWhenEnabled() {
    enableJasperReports();

    assertStatus(
        OK,
        POST(
            "/metadata?skipValidation=true",
            "{'reports':[" + reportBodyWithId("JasperOn002", "On skip", DESIGN) + "]}"));

    assertEquals(DESIGN, getReport("JasperOn002").getDesignContent());
  }

  @Test
  void changingTypeAllowedWhenEnabledForSuperuser() {
    enableJasperReports();
    String uid =
        assertStatus(CREATED, POST("/reports/", htmlReportBody("HTML promotion", HTML_DESIGN)));

    assertStatus(
        OK,
        PUT(
            "/reports/" + uid,
            """
            {"name":"HTML promotion","type":"JASPER_REPORT_TABLE","designContent":"%s"}"""
                .formatted(HTML_DESIGN)));
    assertStatus(
        OK, PATCH("/reports/" + uid, "[{'op':'replace','path':'/type','value':'JASPER_JDBC'}]"));
    assertStatus(
        OK,
        POST(
            "/metadata?importStrategy=UPDATE",
            """
            {"reports":[{"id":"%s","name":"Promoted","type":"JASPER_REPORT_TABLE","designContent":"%s"}]}"""
                .formatted(uid, DESIGN)));

    Report stored = getReport(uid);
    assertEquals("Promoted", stored.getName());
    assertEquals(ReportType.JASPER_REPORT_TABLE, stored.getType());
    assertEquals(DESIGN, stored.getDesignContent());
  }

  // --- ON: HTML authoring and edits that keep the design remain available ----

  @ParameterizedTest
  @ValueSource(strings = {"PUT", "PATCH", "METADATA"})
  void createAndUpdateHtmlAllowedWhenEnabledForReportAuthor(String method) {
    enableJasperReports();
    switchToNewUser("htmlauthoron", "F_REPORT_PUBLIC_ADD");
    String uid = assertStatus(CREATED, POST("/reports/", htmlReportBody("HTML", HTML_DESIGN)));

    HttpResponse response =
        switch (method) {
          case "PUT" -> PUT("/reports/" + uid, htmlReportBody("HTML", OTHER_HTML_DESIGN));
          case "PATCH" ->
              PATCH(
                  "/reports/" + uid,
                  "[{'op':'replace','path':'/designContent','value':'" + OTHER_HTML_DESIGN + "'}]");
          default ->
              POST(
                  "/metadata",
                  """
                  {"reports":[{"id":"%s","name":"HTML","type":"HTML","designContent":"%s"}]}"""
                      .formatted(uid, OTHER_HTML_DESIGN));
        };
    assertStatus(OK, response);

    JsonMixed stored = GET("/reports/" + uid + "?fields=type,designContent").content(OK);
    assertEquals("HTML", stored.getString("type").string());
    assertEquals(OTHER_HTML_DESIGN, stored.getString("designContent").string());
  }

  @Test
  void editingReportWithUnchangedDesignAllowedWhenEnabledForReportAuthor() {
    enableJasperReports();
    assertStatus(
        CREATED,
        POST(
            "/reports/", sharedReportBody("JasperAut09", "Shared", "JASPER_REPORT_TABLE", DESIGN)));
    switchToNewUser("reporteditor", "F_REPORT_PUBLIC_ADD");

    assertStatus(
        OK,
        PUT(
            "/reports/JasperAut09",
            sharedReportBody("JasperAut09", "Renamed", "JASPER_REPORT_TABLE", DESIGN)));
    assertStatus(
        OK, PATCH("/reports/JasperAut09", "[{'op':'replace','path':'/name','value':'Patched'}]"));
    assertStatus(
        OK,
        POST(
            "/metadata?importStrategy=UPDATE",
            "{'reports':["
                + sharedReportBody("JasperAut09", "Imported", "JASPER_REPORT_TABLE", DESIGN)
                + "]}"));

    switchToSuperuser();
    Report stored = getReport("JasperAut09");
    assertEquals("Imported", stored.getName());
    assertEquals(ReportType.JASPER_REPORT_TABLE, stored.getType());
    assertEquals(DESIGN, stored.getDesignContent());
  }

  // --- helpers ---------------------------------------------------------------

  private Properties properties() {
    return config.getProperties();
  }

  private void enableJasperReports() {
    properties().setProperty(ConfigurationKey.JASPER_REPORTS_ENABLED.getKey(), "on");
  }

  private void disableJasperReports() {
    properties().setProperty(ConfigurationKey.JASPER_REPORTS_ENABLED.getKey(), "off");
  }

  private String reportBody(String name, String designContent) {
    if (designContent == null) {
      return """
          {"name":"%s","relativePeriods":{"last12Months":true}}"""
          .formatted(name);
    }
    return """
        {"name":"%s","relativePeriods":{"last12Months":true},"designContent":"%s"}"""
        .formatted(name, designContent);
  }

  private String metadataBody(String name, String designContent) {
    return """
        {"reports":[{"name":"%s","relativePeriods":{"last12Months":true},"designContent":"%s"}]}"""
        .formatted(name, designContent);
  }

  private String reportBodyWithId(String id, String name, String designContent) {
    return """
        {"id":"%s","name":"%s","relativePeriods":{"last12Months":true},"designContent":"%s"}"""
        .formatted(id, name, designContent);
  }

  private String htmlReportBody(String name, String designContent) {
    return """
        {"name":"%s","type":"HTML","designContent":"%s"}"""
        .formatted(name, designContent);
  }

  /** A report of the given type that every user may edit. */
  private String sharedReportBody(String id, String name, String type, String designContent) {
    return """
        {"id":"%s","name":"%s","type":"%s","sharing":{"public":"rw------"},"designContent":"%s"}"""
        .formatted(id, name, type, designContent);
  }

  private Report getReport(String uid) {
    manager.flush();
    manager.clear();
    return manager.get(Report.class, uid);
  }

  /** Rejected by the commit-phase check, which runs even when validation is skipped. */
  private static void assertCommitRejected(HttpResponse response) {
    assertEquals(
        ErrorCode.E1007,
        response.content(HttpStatus.CONFLICT).as(JsonWebMessage.class).getErrorCode());
  }

  private static void assertRejected(ErrorCode expected, HttpResponse response) {
    assertNotNull(
        response
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class)
            .find(JsonErrorReport.class, e -> e.getErrorCode() == expected));
  }
}
