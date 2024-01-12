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
package org.hisp.dhis.webapi.controller.metadata;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.csv.CsvImportClass;
import org.hisp.dhis.dxf2.csv.CsvImportOptions;
import org.hisp.dhis.dxf2.csv.CsvImportService;
import org.hisp.dhis.dxf2.gml.GmlImportService;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsonpatch.BulkJsonPatches;
import org.hisp.dhis.jsonpatch.BulkPatchManager;
import org.hisp.dhis.jsonpatch.BulkPatchParameters;
import org.hisp.dhis.jsonpatch.validator.BulkPatchValidatorFactory;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobSchedulerService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags("metadata")
@Controller
@RequestMapping("/metadata")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
public class MetadataImportExportController {

  private final MetadataImportService metadataImportService;
  private final ContextService contextService;
  private final RenderService renderService;
  private final SchemaService schemaService;
  private final CsvImportService csvImportService;
  private final GmlImportService gmlImportService;
  private final MetadataExportService metadataExportService;
  private final UserService userService;
  private final UserSettingService userSettingService;
  private final JobConfigurationService jobConfigurationService;
  private final JobSchedulerService jobSchedulerService;
  private final ObjectMapper jsonMapper;
  private final BulkPatchManager bulkPatchManager;

  @PostMapping(value = "", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage postJsonMetadata(HttpServletRequest request)
      throws IOException, ConflictException, @OpenApi.Ignore NotFoundException {
    MetadataImportParams params = getMetadataImportParams();

    if (params.isAsync()) {
      return startAsyncMetadata(params, MimeType.valueOf("application/json"), request);
    }

    final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects =
        renderService.fromMetadata(
            StreamUtils.wrapAndCheckCompressionFormat(request.getInputStream()), RenderFormat.JSON);

    ImportReport importReport =
        metadataImportService.importMetadata(params, new MetadataObjects(objects));

    return importReport(importReport).withPlainResponseBefore(DhisApiVersion.V38);
  }

  @PostMapping(value = "", consumes = "application/csv")
  @ResponseBody
  public WebMessage postCsvMetadata(HttpServletRequest request)
      throws IOException, ConflictException, @OpenApi.Ignore NotFoundException {
    MetadataImportParams params = getMetadataImportParams();

    String classKey = request.getParameter("classKey");

    if (StringUtils.isEmpty(classKey) || !CsvImportClass.classExists(classKey)) {
      return conflict("Cannot find Csv import class:  " + classKey);
    }

    params.setCsvImportClass(CsvImportClass.valueOf(classKey));

    if (params.isAsync()) {
      return startAsyncMetadata(params, MimeType.valueOf("application/csv"), request);
    }

    Metadata metadata =
        csvImportService.fromCsv(
            request.getInputStream(),
            new CsvImportOptions()
                .setImportClass(params.getCsvImportClass())
                .setFirstRowIsHeader(params.isFirstRowIsHeader()));

    ImportReport report =
        metadataImportService.importMetadata(
            params,
            new MetadataObjects().addMetadata(schemaService.getMetadataSchemas(), metadata));

    return importReport(report).withPlainResponseBefore(DhisApiVersion.V38);
  }

  @PostMapping(value = "/gml", consumes = APPLICATION_XML_VALUE)
  @ResponseBody
  public WebMessage postGmlMetadata(HttpServletRequest request)
      throws IOException, ConflictException, @OpenApi.Ignore NotFoundException {
    MetadataImportParams params = getMetadataImportParams();

    if (params.isAsync()) {
      return startAsyncMetadata(params, MimeType.valueOf("application/xml"), request);
    }
    ImportReport importReport =
        gmlImportService.importGml(request.getInputStream(), params, NoopJobProgress.INSTANCE);
    return importReport(importReport).withPlainResponseBefore(DhisApiVersion.V38);
  }

  @GetMapping("/csvImportClasses")
  public @ResponseBody List<CsvImportClass> getCsvImportClasses() {
    return Arrays.asList(CsvImportClass.values());
  }

  @GetMapping
  public ResponseEntity<MetadataExportParams> getMetadata(
      @RequestParam(required = false, defaultValue = "false") boolean translate,
      @RequestParam(required = false) String locale,
      @RequestParam(defaultValue = "false") boolean download) {
    if (translate) {
      setTranslationParams(new TranslateParams(true, locale));
    }

    MetadataExportParams params =
        metadataExportService.getParamsFromMap(contextService.getParameterValuesMap());
    metadataExportService.validate(params);

    return ResponseEntity.ok(params);
  }

  @ResponseBody
  @PatchMapping(
      value = "sharing",
      consumes = "application/json-patch+json",
      produces = APPLICATION_JSON_VALUE)
  public WebMessage bulkSharing(
      @RequestParam(required = false, defaultValue = "false") boolean atomic,
      HttpServletRequest request)
      throws IOException {
    final BulkJsonPatches bulkJsonPatches =
        jsonMapper.readValue(request.getInputStream(), BulkJsonPatches.class);

    BulkPatchParameters patchParams =
        BulkPatchParameters.builder().validators(BulkPatchValidatorFactory.SHARING).build();

    List<IdentifiableObject> patchedObjects =
        bulkPatchManager.applyPatches(bulkJsonPatches, patchParams);

    if (patchedObjects.isEmpty() || (atomic && !patchParams.hasErrorReports())) {
      ImportReport importReport = new ImportReport();
      importReport.addTypeReports(patchParams.getTypeReports());
      importReport.setStatus(Status.ERROR);
      return importReport(importReport);
    }

    MetadataImportParams importParams = getMetadataImportParams();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    importParams
        .setUser(UID.of(currentUser))
        .setImportStrategy(ImportStrategy.UPDATE)
        .setAtomicMode(atomic ? AtomicMode.ALL : AtomicMode.NONE);

    ImportReport importReport =
        metadataImportService.importMetadata(
            importParams, new MetadataObjects().addObjects(patchedObjects));

    if (patchParams.hasErrorReports()) {
      importReport.addTypeReports(patchParams.getTypeReports());
      importReport.setStatus(
          importReport.getStatus() == Status.OK ? Status.WARNING : importReport.getStatus());
    }

    return importReport(importReport);
  }

  // ----------------------------------------------------------------------------------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------------------------------------------------------------------------------

  private MetadataImportParams getMetadataImportParams() {
    return metadataImportService.getParamsFromMap(contextService.getParameterValuesMap());
  }

  private WebMessage startAsyncMetadata(
      MetadataImportParams params, MimeType contentType, HttpServletRequest request)
      throws IOException, ConflictException, NotFoundException {
    JobConfiguration config = new JobConfiguration(JobType.METADATA_IMPORT);

    config.setExecutedBy(CurrentUserUtil.getCurrentUserDetails().getUid());
    config.setJobParameters(params);
    jobSchedulerService.executeNow(
        jobConfigurationService.create(config, contentType, request.getInputStream()));
    return jobConfigurationReport(config);
  }

  private void setTranslationParams(TranslateParams translateParams) {
    Locale dbLocale = getLocaleWithDefault(translateParams);
    CurrentUserUtil.setUserSetting(UserSettingKey.DB_LOCALE, dbLocale);
  }

  private Locale getLocaleWithDefault(TranslateParams translateParams) {
    return translateParams.isTranslate()
        ? translateParams.getLocaleWithDefault(
            (Locale) userSettingService.getUserSetting(UserSettingKey.DB_LOCALE))
        : null;
  }
}
