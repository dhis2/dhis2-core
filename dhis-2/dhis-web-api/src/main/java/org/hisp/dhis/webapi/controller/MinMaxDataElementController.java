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
package org.hisp.dhis.webapi.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.csv.CSV;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ImportSuccessResponse;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementQueryParams;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.minmax.MinMaxValue;
import org.hisp.dhis.minmax.MinMaxValueDeleteRequest;
import org.hisp.dhis.minmax.MinMaxValueKey;
import org.hisp.dhis.minmax.MinMaxValueUpsertRequest;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@OpenApi.Tags("data")
@Controller
@RequestMapping("/api/minMaxDataElements")
@AllArgsConstructor
public class MinMaxDataElementController {

  private final ContextService contextService;
  private final MinMaxDataElementService minMaxService;
  private final FieldFilterService fieldFilterService;

  // OBS: This is a workaround for the change in the implementation of the
  // FieldPreset interface in current versions.
  private static final List<String> ALL_FIELDS = List.of("*");

  @GetMapping
  public @ResponseBody RootNode getObjectList(MinMaxDataElementQueryParams query)
      throws QueryParserException {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));
    List<String> filters = Lists.newArrayList(contextService.getParameterValues("filter"));
    query.setFilters(filters);

    if (fields.isEmpty()) {
      fields.addAll(ALL_FIELDS);
    }

    List<MinMaxDataElement> minMaxDataElements = minMaxService.getMinMaxDataElements(query);

    RootNode rootNode = NodeUtils.createMetadata();

    if (!query.isSkipPaging()) {
      query.setTotal(minMaxService.countMinMaxDataElements(query));
      rootNode.addChild(NodeUtils.createPager(query.getPager()));
    }

    rootNode.addChild(
        fieldFilterService.toCollectionNode(
            MinMaxDataElement.class, new FieldFilterParams(minMaxDataElements, fields)));

    return rootNode;
  }

  // --------------------------------------------------------------------------
  // POST
  // --------------------------------------------------------------------------

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_ADD')")
  @ResponseStatus(HttpStatus.CREATED)
  public void postJsonObject(@RequestBody MinMaxDataElement body) throws BadRequestException {
    minMaxService.importValue(MinMaxValue.of(body));
  }

  // --------------------------------------------------------------------------
  // DELETE
  // --------------------------------------------------------------------------

  @DeleteMapping(consumes = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_ADD')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteObject(@RequestBody MinMaxDataElement body)
      throws BadRequestException, NotFoundException {
    minMaxService.deleteValue(MinMaxValueKey.of(body));
  }

  /*
  Bulk import
  */

  @PostMapping(value = "/upsert", consumes = "application/json")
  @PreAuthorize("hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_ADD')")
  public @ResponseBody ImportSuccessResponse bulkPostJson(
      @RequestBody MinMaxValueUpsertRequest request) throws BadRequestException {

    int imported = minMaxService.importAll(request);

    return ImportSuccessResponse.ok()
        .message("Successfully imported %d min-max values".formatted(imported))
        .successful(imported)
        .ignored(request.values().size() - imported)
        .build();
  }

  @PostMapping(value = "/delete", consumes = "application/json")
  @PreAuthorize("hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_ADD')")
  public @ResponseBody ImportSuccessResponse bulkDeleteJson(
      @RequestBody MinMaxValueDeleteRequest request) throws BadRequestException {

    int deleted = minMaxService.deleteAll(request);

    return ImportSuccessResponse.ok()
        .message("Successfully deleted %d min-max values".formatted(deleted))
        .successful(deleted)
        .ignored(request.values().size() - deleted)
        .build();
  }

  @PostMapping(value = "/upsert", consumes = "multipart/form-data")
  @PreAuthorize("hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_ADD')")
  public @ResponseBody ImportSuccessResponse bulkPostCsv(
      @RequestParam("file") MultipartFile file, @RequestParam UID dataSet)
      throws BadRequestException {

    return bulkPostJson(new MinMaxValueUpsertRequest(dataSet, csvToEntries(file)));
  }

  @PostMapping(value = "/delete", consumes = "multipart/form-data")
  @PreAuthorize("hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_ADD')")
  public @ResponseBody ImportSuccessResponse bulkDeleteCsv(
      @RequestParam("file") MultipartFile file, @RequestParam UID dataSet)
      throws BadRequestException {

    return bulkDeleteJson(new MinMaxValueDeleteRequest(dataSet, csvToKeys(file)));
  }

  private static List<MinMaxValue> csvToEntries(MultipartFile file) throws BadRequestException {
    try (InputStream in = file.getInputStream()) {
      List<MinMaxValue> entries = CSV.of(in).as(MinMaxValue.class).list();
      if (entries.isEmpty())
        throw new BadRequestException(ErrorCode.E2046, "No data found in the CSV file.");
      return entries;
    } catch (Exception ex) {
      throw new BadRequestException(ErrorCode.E2046, ex.getMessage());
    }
  }

  private static List<MinMaxValueKey> csvToKeys(MultipartFile file) throws BadRequestException {
    try (InputStream in = file.getInputStream()) {
      List<MinMaxValueKey> keys = CSV.of(in).as(MinMaxValueKey.class).list();
      if (keys.isEmpty())
        throw new BadRequestException(ErrorCode.E2046, "No data found in the CSV file.");
      return keys;
    } catch (IOException ex) {
      throw new BadRequestException(ErrorCode.E2046, ex.getMessage());
    }
  }
}
