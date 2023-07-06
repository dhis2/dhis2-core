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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementQueryParams;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.MinMaxDataElementSchemaDescriptor;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@OpenApi.Tags("analytics")
@Controller
@RequestMapping(value = MinMaxDataElementSchemaDescriptor.API_ENDPOINT)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class MinMaxDataElementController {
  private final ContextService contextService;

  private final MinMaxDataElementService minMaxService;

  private final FieldFilterService fieldFilterService;

  private final RenderService renderService;

  private final IdentifiableObjectManager manager;

  // --------------------------------------------------------------------------
  // GET
  // --------------------------------------------------------------------------

  @GetMapping
  public @ResponseBody RootNode getObjectList(MinMaxDataElementQueryParams query)
      throws QueryParserException {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));
    List<String> filters = Lists.newArrayList(contextService.getParameterValues("filter"));
    query.setFilters(filters);

    if (fields.isEmpty()) {
      fields.addAll(Preset.ALL.getFields());
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
  @ResponseBody
  public WebMessage postJsonObject(HttpServletRequest request) throws Exception {
    MinMaxDataElement minMax =
        renderService.fromJson(request.getInputStream(), MinMaxDataElement.class);

    validate(minMax);

    minMax = getReferences(minMax);

    MinMaxDataElement persisted =
        minMaxService.getMinMaxDataElement(
            minMax.getSource(), minMax.getDataElement(), minMax.getOptionCombo());

    if (Objects.isNull(persisted)) {
      minMaxService.addMinMaxDataElement(minMax);
    } else {
      persisted.mergeWith(minMax);
      minMaxService.updateMinMaxDataElement(persisted);
    }

    return created();
  }

  // --------------------------------------------------------------------------
  // DELETE
  // --------------------------------------------------------------------------

  @DeleteMapping(consumes = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ALL') or hasRole('F_MINMAX_DATAELEMENT_DELETE')")
  @ResponseBody
  public WebMessage deleteObject(HttpServletRequest request) throws Exception {
    MinMaxDataElement minMax =
        renderService.fromJson(request.getInputStream(), MinMaxDataElement.class);

    validate(minMax);

    minMax = getReferences(minMax);

    MinMaxDataElement persisted =
        minMaxService.getMinMaxDataElement(
            minMax.getSource(), minMax.getDataElement(), minMax.getOptionCombo());

    if (Objects.isNull(persisted)) {
      return notFound("Can not find MinMaxDataElement.");
    }

    minMaxService.deleteMinMaxDataElement(persisted);

    return ok("MinMaxDataElement deleted.");
  }

  private void validate(MinMaxDataElement minMax) throws WebMessageException {
    if (!ObjectUtils.allNonNull(
        minMax.getDataElement(), minMax.getSource(), minMax.getOptionCombo())) {
      throw new WebMessageException(
          notFound("Missing required parameters : Source, DataElement, OptionCombo."));
    }
  }

  private MinMaxDataElement getReferences(MinMaxDataElement m) throws WebMessageException {
    try {
      m.setDataElement(
          Objects.requireNonNull(manager.get(DataElement.class, m.getDataElement().getUid())));
      m.setSource(
          Objects.requireNonNull(manager.get(OrganisationUnit.class, m.getSource().getUid())));
      m.setOptionCombo(
          Objects.requireNonNull(
              manager.get(CategoryOptionCombo.class, m.getOptionCombo().getUid())));
      return m;
    } catch (NullPointerException e) {
      throw new WebMessageException(
          notFound("Invalid required parameters: source, dataElement, optionCombo"));
    }
  }
}
