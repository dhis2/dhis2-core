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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummary;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.hisp.dhis.security.Authorities.F_DATAVALUE_ADD;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_PDF;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML_ADX;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.hisp.dhis.webapi.utils.ContextUtils.stripFormatCompressionExtension;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.datavalue.DataEntryPipeline;
import org.hisp.dhis.datavalue.DataExportInputParams;
import org.hisp.dhis.datavalue.DataExportPipeline;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobExecutionService;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:platform", "purpose:data"})
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/dataValueSets")
public class DataValueSetController {

  private final DataExportPipeline dataExportPipeline;
  private final DataEntryPipeline dataEntryPipeline;
  private final JobExecutionService jobExecutionService;

  // -------------------------------------------------------------------------
  // Get
  // -------------------------------------------------------------------------

  @OpenApi.Response(DataValueSet.class)
  @GetMapping
  public void getDataValueSet(
      DataExportInputParams params,
      @RequestParam(required = false) String attachment,
      @RequestParam(required = false) String compression,
      @RequestParam(required = false) String format,
      HttpServletRequest request,
      HttpServletResponse response)
      throws ConflictException {
    if (format == null) {
      String path = request.getPathInfo();
      format = "json";
      if (path.endsWith(".adx+xml")) format = "adx+xml";
      if (path.endsWith(".xml")) format = "xml";
      if (path.endsWith(".csv")) format = "csv";
    }
    switch (format) {
      case "xml" -> getDataValueSetXml(params, attachment, compression, response);
      case "adx+xml" -> getDataValueSetXmlAdx(params, attachment, compression, response);
      case "csv" -> getDataValueSetCsv(params, attachment, compression, response);
      default -> getDataValueSetJson(params, attachment, compression, response);
    }
  }

  private void getDataValueSetXml(
      DataExportInputParams params,
      @RequestParam(required = false) String attachment,
      @RequestParam(required = false) String compression,
      HttpServletResponse response)
      throws ConflictException {
    getDataValueSet(
        attachment,
        compression,
        "xml",
        response,
        CONTENT_TYPE_XML,
        params,
        dataExportPipeline::exportAsXml);
  }

  private void getDataValueSetXmlAdx(
      DataExportInputParams params,
      @RequestParam(required = false) String attachment,
      @RequestParam(required = false) String compression,
      HttpServletResponse response)
      throws ConflictException {
    getDataValueSet(
        attachment,
        compression,
        "xml",
        response,
        CONTENT_TYPE_XML_ADX,
        params,
        dataExportPipeline::exportAsXmlGroups);
  }

  private void getDataValueSetJson(
      DataExportInputParams params,
      @RequestParam(required = false) String attachment,
      @RequestParam(required = false) String compression,
      HttpServletResponse response)
      throws ConflictException {
    getDataValueSet(
        attachment,
        compression,
        "json",
        response,
        CONTENT_TYPE_JSON,
        params,
        dataExportPipeline::exportAsJson);
  }

  private void getDataValueSetCsv(
      DataExportInputParams params,
      @RequestParam(required = false) String attachment,
      @RequestParam(required = false) String compression,
      HttpServletResponse response)
      throws ConflictException {
    getDataValueSet(
        attachment,
        compression,
        "csv",
        response,
        CONTENT_TYPE_CSV,
        params,
        dataExportPipeline::exportAsCsv);
  }

  interface ExportHandler {

    void export(DataExportInputParams params, OutputStream out) throws ConflictException;
  }

  private void getDataValueSet(
      String attachment,
      String compression,
      String format,
      HttpServletResponse response,
      String contentType,
      DataExportInputParams params,
      ExportHandler handler)
      throws ConflictException {

    response.setContentType(contentType);
    setNoStore(response);

    try (OutputStream out =
        compress(params, response, attachment, Compression.fromValue(compression), format)) {
      handler.export(params, out);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // -------------------------------------------------------------------------
  // Post
  // -------------------------------------------------------------------------

  @PostMapping(consumes = APPLICATION_XML_VALUE)
  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @ResponseBody
  public WebMessage postDxf2DataValueSet(ImportOptions importOptions, HttpServletRequest request)
      throws IOException, ConflictException, BadRequestException {
    if (importOptions.isAsync()) {
      return startAsyncImport(importOptions, MediaType.APPLICATION_XML, request);
    }
    return importSummary(
        dataEntryPipeline.importXml(request.getInputStream(), importOptions, transitory()));
  }

  @PostMapping(consumes = CONTENT_TYPE_XML_ADX)
  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @ResponseBody
  public WebMessage postAdxDataValueSet(ImportOptions importOptions, HttpServletRequest request)
      throws IOException, ConflictException, BadRequestException {
    if (importOptions.isAsync()) {
      return startAsyncImport(importOptions, MimeType.valueOf("application/adx+xml"), request);
    }
    return importSummary(
        dataEntryPipeline.importXml(request.getInputStream(), importOptions, transitory()));
  }

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @ResponseBody
  public WebMessage postJsonDataValueSet(ImportOptions importOptions, HttpServletRequest request)
      throws IOException, ConflictException {
    if (importOptions.isAsync()) {
      return startAsyncImport(importOptions, MediaType.APPLICATION_JSON, request);
    }
    return importSummary(
        dataEntryPipeline.importJson(request.getInputStream(), importOptions, transitory()));
  }

  @PostMapping(consumes = "application/csv")
  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @ResponseBody
  public WebMessage postCsvDataValueSet(ImportOptions importOptions, HttpServletRequest request)
      throws IOException, ConflictException {
    if (importOptions.isAsync()) {
      return startAsyncImport(importOptions, MimeType.valueOf("application/csv"), request);
    }
    return importSummary(
        dataEntryPipeline.importCsv(request.getInputStream(), importOptions, transitory()));
  }

  @PostMapping(consumes = CONTENT_TYPE_PDF)
  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @ResponseBody
  public WebMessage postPdfDataValueSet(ImportOptions importOptions, HttpServletRequest request)
      throws IOException, ConflictException, BadRequestException {
    if (importOptions.isAsync()) {
      return startAsyncImport(importOptions, MediaType.APPLICATION_PDF, request);
    }
    return importSummary(
        dataEntryPipeline.importPdf(request.getInputStream(), importOptions, transitory()));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Starts an asynchronous import task. */
  private WebMessage startAsyncImport(
      ImportOptions importOptions, MimeType mimeType, HttpServletRequest request)
      throws ConflictException, IOException {
    JobConfiguration config = new JobConfiguration(DATAVALUE_IMPORT);
    config.setExecutedBy(CurrentUserUtil.getCurrentUserDetails().getUid());
    config.setJobParameters(importOptions);

    jobExecutionService.executeOnceNow(config, mimeType, request.getInputStream());

    return jobConfigurationReport(config);
  }

  /**
   * Returns an output stream with the appropriate compression based on the given {@link
   * Compression} argument.
   *
   * @param response the {@link HttpServletResponse}.
   * @param attachment the file download attachment name
   * @param compression the Compression {@link Compression}
   * @param format the file format, can be json, xml or csv.
   * @return Compressed OutputStream if given compression is given, otherwise just return
   *     uncompressed outputStream
   */
  private OutputStream compress(
      DataExportInputParams params,
      HttpServletResponse response,
      String attachment,
      Compression compression,
      String format)
      throws IOException, HttpMessageNotWritableException {
    String fileName = getAttachmentFileName(attachment, params);

    if (Compression.GZIP == compression) {
      // make sure to remove format + gz/gzip extension if present
      fileName = stripFormatCompressionExtension(fileName, format, "gzip");
      fileName = stripFormatCompressionExtension(fileName, format, "gz");

      response.setHeader(
          ContextUtils.HEADER_CONTENT_DISPOSITION,
          "attachment; filename=" + fileName + "." + format + ".gz");
      response.setHeader(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary");
      return new GZIPOutputStream(response.getOutputStream());
    } else if (Compression.ZIP == compression) {
      // make sure to remove format + zip extension if present
      fileName = stripFormatCompressionExtension(fileName, format, "zip");

      response.setHeader(
          ContextUtils.HEADER_CONTENT_DISPOSITION,
          "attachment; filename=" + fileName + "." + format + ".zip");
      response.setHeader(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary");

      ZipOutputStream outputStream = new ZipOutputStream(response.getOutputStream());
      outputStream.putNextEntry(new ZipEntry(fileName + "." + format));

      return outputStream;
    } else {
      // file download only if attachment is explicitly specified for
      // no-compression option.
      if (!StringUtils.isEmpty(attachment)) {
        response.addHeader(
            ContextUtils.HEADER_CONTENT_DISPOSITION,
            "attachment; filename=" + fileName + "." + format);
        response.addHeader(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary");
      }

      return response.getOutputStream();
    }
  }

  /**
   * Generate file name with format "dataValues_startDate_endDate" or
   *
   * <p>"{attachment}_startDate_endDate" if value of the attachment parameter is not empty.
   *
   * <p>Date format is "yyyy-mm-dd". This can only apply if the request has startDate and endDate.
   * Otherwise, will return default file name "dataValues".
   *
   * @param attachment The attachment parameter.
   * @param params {@link DataExportInputParams} contains startDate and endDate parameter.
   * @return the export file name.
   */
  private String getAttachmentFileName(String attachment, DataExportInputParams params) {
    String fileName = StringUtils.isEmpty(attachment) ? "dataValues" : attachment;

    if (params.getStartDate() == null || params.getEndDate() == null) {
      return fileName;
    }

    String dates =
        String.join(
            "_",
            DateUtils.getSqlDateString(params.getStartDate()),
            DateUtils.getSqlDateString(params.getEndDate()));

    fileName = fileName.contains(".") ? fileName.substring(0, fileName.indexOf(".")) : fileName;

    return String.join("_", fileName, dates);
  }
}
