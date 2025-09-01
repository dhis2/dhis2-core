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
package org.hisp.dhis.webapi.controller.datavalue;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.hisp.dhis.security.Authorities.F_DATAVALUE_ADD;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataEntryGroup;
import org.hisp.dhis.datavalue.DataEntryService;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueEntry;
import org.hisp.dhis.datavalue.DataValueQueryParams;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.hisp.dhis.webapi.webdomain.DataValueFollowUpRequest;
import org.hisp.dhis.webapi.webdomain.DataValuesFollowUpRequest;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueCategoryParams;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValuePostParams;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:platform", "purpose:data-entry"})
@RestController
@RequestMapping("/api/dataValues")
@RequiredArgsConstructor
public class DataValueController {

  public static final String FILE_PATH = "/file";

  // ---------------------------------------------------------------------
  // Dependencies
  // ---------------------------------------------------------------------

  private final DataValueService dataValueService;

  private final DataEntryService dataEntryService;

  private final FileResourceService fileResourceService;

  private final DataValidator dataValidator;

  private final FileResourceUtils fileResourceUtils;

  private final DhisConfigurationProvider dhisConfig;

  // ---------------------------------------------------------------------
  // POST
  // ---------------------------------------------------------------------

  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @PostMapping(params = {"de", "pe", "ou"})
  @ResponseStatus(HttpStatus.CREATED)
  public void saveDataValue(
      @OpenApi.Param({UID.class, DataElement.class}) @RequestParam String de,
      @OpenApi.Param({UID.class, CategoryOptionCombo.class}) @RequestParam(required = false)
          String co,
      @OpenApi.Param({UID.class, CategoryCombo.class}) @RequestParam(required = false) String cc,
      @OpenApi.Param({UID.class, CategoryOption.class}) @RequestParam(required = false) String cp,
      @OpenApi.Param(Period.class) @RequestParam String pe,
      @OpenApi.Param({UID.class, OrganisationUnit.class}) @RequestParam String ou,
      @OpenApi.Param({UID.class, DataSet.class}) @RequestParam(required = false) UID ds,
      @RequestParam(required = false) String value,
      @RequestParam(required = false) String comment,
      @RequestParam(required = false) Boolean followUp,
      @RequestParam(required = false) boolean force)
      throws ConflictException, BadRequestException {
    if ("".equals(value) && comment == null && followUp == null) {
      // backwards compatability: value="" => perform delete
      deleteDataValue(
          DataValueQueryParams.builder().de(de).co(co).cc(cc).cp(cp).pe(pe).ou(ou).build(),
          ds,
          force);
      return;
    }
    Set<String> aocCos = cp == null ? null : Set.of(cp.split(";"));
    DataEntryValue.Input dv =
        new DataEntryValue.Input(
            de, ou, co, null, null, cc, aocCos, pe, value, comment, followUp, null);
    // backwards compatability: all writes must assume null=keep current value
    String dataSet = ds == null ? null : ds.getValue();
    DataEntrySummary summary =
        dataEntryService.upsertGroup(
            new DataEntryGroup.Options(false, false, force),
            dataEntryService.decodeGroupKeepUnspecified(
                new DataEntryGroup.Input(dataSet, List.of(dv))),
            transitory());
    if (summary.succeeded() < 1) throw conflictOf(summary, "Failed to upsert data value: ");
  }

  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public void saveDataValueWithBody(@RequestBody DataValuePostParams dv)
      throws ConflictException, BadRequestException {
    UID ds = UID.ofNullable(dv.getDataSet());
    String de = dv.getDataElement();
    DataValueCategoryParams attr = dv.getAttribute();
    String cc = attr == null ? null : attr.getCombo();
    String cp = attr == null ? null : String.join(";", attr.getOptions());
    String ou = dv.getOrgUnit();
    String coc = dv.getCategoryOptionCombo();
    String pe = dv.getPeriod();
    String value = dv.getValue();
    saveDataValue(
        de, coc, cc, cp, pe, ou, ds, value, dv.getComment(), dv.isFollowUp(), dv.isForce());
  }

  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @PostMapping(FILE_PATH)
  public WebMessage saveFileDataValue(
      @OpenApi.Param({UID.class, DataElement.class}) @RequestParam String de,
      @OpenApi.Param({UID.class, CategoryOptionCombo.class}) @RequestParam(required = false)
          String co,
      @OpenApi.Param({UID.class, CategoryCombo.class}) @RequestParam(required = false) String cc,
      @OpenApi.Param({UID.class, CategoryOption.class}) @RequestParam(required = false) String cp,
      @OpenApi.Param({UID.class, Period.class}) @RequestParam String pe,
      @OpenApi.Param({UID.class, OrganisationUnit.class}) @RequestParam String ou,
      @OpenApi.Param({UID.class, DataSet.class}) @RequestParam(required = false) UID ds,
      @RequestParam(required = false) String comment,
      @RequestParam(required = false) Boolean followUp,
      @RequestParam(required = false) boolean force,
      @RequestParam(required = false) MultipartFile file)
      throws IOException, ConflictException, BadRequestException {

    if (file == null) throw new BadRequestException("file is required");
    FileResource fr = fileResourceUtils.saveFileResource(file, FileResourceDomain.DATA_VALUE);

    String value = fr.getUid();
    saveDataValue(de, co, cc, cp, pe, ou, ds, value, comment, followUp, force);

    WebMessage response = new WebMessage(Status.OK, HttpStatus.ACCEPTED);
    response.setResponse(new FileResourceWebMessageResponse(fr));
    return response;
  }

  // ---------------------------------------------------------------------
  // DELETE
  // ---------------------------------------------------------------------

  @DeleteMapping
  @RequiresAuthority(anyOf = F_DATAVALUE_ADD)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDataValue(
      DataValueQueryParams params,
      @OpenApi.Param({UID.class, DataSet.class}) @RequestParam(required = false) UID ds,
      @RequestParam(required = false) boolean force)
      throws ConflictException, BadRequestException {
    String de = params.getDe();
    String ou = params.getOu();
    String coc = params.getCo();
    String aocCc = params.getCc();
    Set<String> aocCos = params.getCp() == null ? null : Set.of(params.getCp().split(";"));
    String pe = params.getPe();
    DataEntryValue v =
        dataEntryService.decodeValue(
            ds,
            new DataEntryValue.Input(
                de, ou, coc, null, null, aocCc, aocCos, pe, null, null, null, null));

    if (!dataEntryService.deleteValue(force, ds, v.toKey()))
      throw new ConflictException("Data value cannot be deleted because it does not exist");
  }

  // ---------------------------------------------------------------------
  // GET
  // ---------------------------------------------------------------------

  @GetMapping
  public List<String> getDataValue(DataValueQueryParams params, HttpServletResponse response)
      throws BadRequestException, ConflictException, ForbiddenException {

    DataValueEntry dataValue =
        dataValueService.getDataValue(dataEntryService.decodeValue(null, params.toInput()).toKey());

    if (dataValue == null) throw new ConflictException("Data value does not exist");

    Set<UID> notReadable =
        dataEntryService.getNotReadableOptionCombos(
            List.of(dataValue.categoryOptionCombo(), dataValue.attributeOptionCombo()));
    if (!notReadable.isEmpty())
      throw new ForbiddenException(
          "User has no data read access for CategoryOption: "
              + String.join(",", notReadable.stream().map(UID::getValue).toList()));

    setNoStore(response);
    return List.of(dataValue.value());
  }

  // ---------------------------------------------------------------------
  // Follow-up
  // ---------------------------------------------------------------------

  @PutMapping(value = "/followup")
  @ResponseStatus(value = HttpStatus.OK)
  public void setDataValueFollowUp(@RequestBody DataValueFollowUpRequest request)
      throws ConflictException, BadRequestException {
    if (request == null || request.getFollowup() == null)
      throw new IllegalQueryException(ErrorCode.E2033);
    DataEntryGroup group =
        dataEntryService.decodeGroupKeepUnspecified(
            new DataEntryGroup.Input(List.of(request.toDataEntryValue())));
    DataEntrySummary summary =
        dataEntryService.upsertGroup(new DataEntryGroup.Options(), group, transitory());
    if (summary.succeeded() < 1) throw conflictOf(summary, "Failed to update followup: ");
  }

  @PutMapping(value = "/followups")
  @ResponseStatus(value = HttpStatus.OK)
  public void setDataValuesFollowUp(@RequestBody DataValuesFollowUpRequest request)
      throws ConflictException, BadRequestException {
    List<DataValueFollowUpRequest> values = request == null ? null : request.getValues();
    if (values == null
        || values.isEmpty()
        || values.stream().anyMatch(e -> e.getFollowup() == null))
      throw new IllegalQueryException(ErrorCode.E2033);

    DataEntrySummary summary =
        dataEntryService.upsertGroup(
            new DataEntryGroup.Options(false, true, false),
            dataEntryService.decodeGroupKeepUnspecified(
                new DataEntryGroup.Input(
                    values.stream().map(DataValueFollowUpRequest::toDataEntryValue).toList())),
            transitory());
    if (summary.succeeded() < values.size())
      throw conflictOf(summary, "Failed to update followup: ");
  }

  // ---------------------------------------------------------------------
  // GET file
  // ---------------------------------------------------------------------

  @OpenApi.Response(byte[].class)
  @GetMapping("/files")
  public void getDataValueFile(
      DataValueQueryParams params,
      @RequestParam(defaultValue = "original") String dimension,
      HttpServletResponse response)
      throws WebMessageException, BadRequestException, ConflictException, NotFoundException {

    DataValueEntry dataValue =
        dataValueService.getDataValue(dataEntryService.decodeValue(null, params.toInput()).toKey());

    if (dataValue == null) throw new ConflictException("Data value does not exist");

    String uid = dataValue.value();

    // NB: this isn't strictly implied but close enough
    if (!UID.isValid(uid)) throw new ConflictException("DataElement must be of type file");

    FileResource fileResource = fileResourceService.getFileResource(uid);

    if (fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE)
      throw new NotFoundException(FileResource.class, uid);

    FileResourceStorageStatus storageStatus = fileResource.getStorageStatus();

    if (storageStatus != FileResourceStorageStatus.STORED) {
      // Special case:
      // The FileResource exists and has been tied to this DataValue,
      // however, the underlying file
      // content is still not stored to the (most likely external) file
      // store provider.

      // HTTP 409, for lack of a more suitable status code
      throw new WebMessageException(
          conflict(
                  "The content is being processed and is not available yet. Try again later.",
                  "The content requested is in transit to the file store and will be available at a later time.")
              .setResponse(new FileResourceWebMessageResponse(fileResource)));
    }

    response.setContentType(fileResource.getContentType());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName());
    response.setHeader(
        HttpHeaders.CONTENT_LENGTH,
        String.valueOf(fileResourceService.getFileResourceContentLength(fileResource)));

    HeaderUtils.setSecurityHeaders(
        response, dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE));
    setNoStore(response);

    try {
      fileResourceService.copyFileResourceContent(fileResource, response.getOutputStream());
    } catch (IOException e) {
      throw new WebMessageException(
          error(FileResourceStream.EXCEPTION_IO, FileResourceStream.EXCEPTION_IO_DEV));
    }
  }

  private static ConflictException conflictOf(DataEntrySummary summary, String msg) {
    return new ConflictException(
        msg
            + summary.errors().stream()
                .map(DataEntrySummary.DataEntryError::message)
                .collect(joining(", ")));
  }
}
