/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.dimension;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.dimension.AnalyticsDimensionService;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.commons.jackson.domain.JsonRoot;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.PaginationUtils;
import org.hisp.dhis.webapi.utils.PaginationUtils.PagedEntities;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@OpenApi.EntityType(DimensionalObject.class)
@OpenApi.Document(classifiers = {"team:analytics", "purpose:metadata"})
@Controller
@RequestMapping("/api/dimensions")
@RequiredArgsConstructor
public class DimensionController
    extends AbstractCrudController<DimensionalObject, GetObjectListParams> {

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------
  public static final String RESOURCE_PATH = "/dimensions";

  private final DimensionService dimensionService;

  private final AnalyticsDimensionService analyticsDimensionService;

  private final IdentifiableObjectManager identifiableObjectManager;

  private final DimensionItemPageHandler dimensionItemPageHandler;

  // -------------------------------------------------------------------------
  // Controller
  // -------------------------------------------------------------------------

  @Nonnull
  @Override
  protected DimensionalObject getEntity(String uid) throws NotFoundException {
    if (isNotBlank(uid) && isValidUid(uid)) {
      return dimensionService.getDimensionalObjectCopy(uid, true);
    }
    throw new NotFoundException(format("No dimensional object with id `%s` exists", uid));
  }

  /**
   * This method is overridden as {@link DimensionalObject} requires different retrieval and paging
   * considerations compared to the base generic method. There are many different types of {@link
   * DimensionalObject} and there is no specific {@link InternalHibernateGenericStore} to retrieve
   * them from.
   */
  @OpenApi.Response(GetObjectListResponse.class)
  @Override
  @GetMapping
  public @ResponseBody ResponseEntity<StreamingJsonRoot<DimensionalObject>> getObjectList(
      GetObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser) {

    addProgrammaticModifiers(params);
    PagedEntities<DimensionalObject> pagedEntities = getPagedEntities(params);
    linkService.generatePagerLinks(pagedEntities.pager(), RESOURCE_PATH);

    return ResponseEntity.ok(
        new StreamingJsonRoot<>(
            pagedEntities.pager(),
            getSchema().getCollectionName(),
            org.hisp.dhis.fieldfiltering.FieldFilterParams.of(
                pagedEntities.entities(), params.getFieldsJsonList())));
  }

  @Override
  @GetMapping(produces = {"text/csv", "application/text"})
  public ResponseEntity<String> getObjectListCsv(
      GetObjectListParams params,
      @CurrentUser UserDetails currentUser,
      @RequestParam(defaultValue = ",") char separator,
      @RequestParam(defaultValue = ";") String arraySeparator,
      @RequestParam(defaultValue = "false") boolean skipHeader,
      HttpServletResponse response)
      throws IOException {

    addProgrammaticModifiers(params);
    PagedEntities<DimensionalObject> pagedEntities = getPagedEntities(params);
    String csv =
        applyCsvSteps(
            params.getFieldsCsvList(),
            pagedEntities.entities(),
            separator,
            arraySeparator,
            skipHeader);

    return ResponseEntity.ok(csv);
  }

  @OpenApi.Response(
      status = OpenApi.Response.Status.OK,
      object = {
        @OpenApi.Property(name = "pager", value = Pager.class),
        @OpenApi.Property(name = "items", value = DimensionalItemObject[].class)
      })
  @GetMapping("/{uid}/items")
  public @ResponseBody RootNode getItems(@PathVariable String uid, GetObjectListParams params)
      throws QueryParserException {

    // This is the base list used in this flow. It contains only items
    // allowed to the current user.
    List<DimensionalItemObject> readableItems = dimensionService.getCanReadDimensionItems(uid);

    // The query engine is just used as a tool to do in-memory filtering
    // This is needed for two reasons:
    // 1) We are doing in-memory paging;
    // 2) We have to count all items respecting the filtering.
    boolean paging = params.isPaging();
    int totalOfItems = 0;
    if (paging) {
      params.setPaging(false);
      Query<DimensionalItemObject> queryForCount =
          queryService.getQueryFromUrl(DimensionalItemObject.class, params);
      queryForCount.setObjects(readableItems);

      List<?> totalItems = queryService.query(queryForCount);
      totalOfItems = isNotEmpty(totalItems) ? totalItems.size() : 0;
      params.setPaging(true);
    }

    Query<DimensionalItemObject> query =
        queryService.getQueryFromUrl(DimensionalItemObject.class, params);
    query.setObjects(readableItems);
    query.setDefaultOrder();

    List<DimensionalItemObject> paginatedItems = queryService.query(query);
    if (!paging) totalOfItems = paginatedItems.size();

    RootNode rootNode = NodeUtils.createMetadata();

    List<String> fields = params.getFieldsJsonList();
    CollectionNode collectionNode =
        rootNode.addChild(
            oldFieldFilterService.toCollectionNode(
                DimensionalItemObject.class, new FieldFilterParams(paginatedItems, fields)));
    collectionNode.setName("items");

    for (Node node : collectionNode.getChildren()) {
      ((AbstractNode) node).setName("item");
    }

    // Adding pagination elements to the root node.
    dimensionItemPageHandler.addPaginationToNodeIfEnabled(rootNode, params, uid, totalOfItems);

    return rootNode;
  }

  @OpenApi.Response(
      status = OpenApi.Response.Status.OK,
      object = {
        @OpenApi.Property(name = "pager", value = Pager.class),
        @OpenApi.Property(name = "dimensions", value = OpenApi.EntityType[].class)
      })
  @GetMapping("/constraints")
  public @ResponseBody ResponseEntity<JsonRoot> getDimensionConstraints(
      @RequestParam(value = "links", defaultValue = "true", required = false) Boolean links,
      @RequestParam(defaultValue = "*") List<FieldPath> fields) {
    List<DimensionalObject> dimensionConstraints = dimensionService.getDimensionConstraints();

    if (links) {
      linkService.generateLinks(dimensionConstraints, false);
    }

    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(dimensionConstraints, fields);

    return ResponseEntity.ok(new JsonRoot("dimensions", objectNodes));
  }

  @OpenApi.Response(
      status = OpenApi.Response.Status.OK,
      object = {
        @OpenApi.Property(name = "pager", value = Pager.class),
        @OpenApi.Property(name = "dimensions", value = OpenApi.EntityType[].class)
      })
  @GetMapping("/recommendations")
  public ResponseEntity<JsonRoot> getRecommendedDimensions(
      @RequestParam Set<String> dimension,
      @RequestParam(defaultValue = "id,displayName") List<FieldPath> fields) {
    DataQueryRequest request = DataQueryRequest.newBuilder().dimension(dimension).build();

    List<DimensionalObject> dimensions =
        analyticsDimensionService.getRecommendedDimensions(request);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(dimensions, fields);

    return ResponseEntity.ok(new JsonRoot("dimensions", objectNodes));
  }

  @OpenApi.Response(
      status = OpenApi.Response.Status.OK,
      object = {
        @OpenApi.Property(name = "pager", value = Pager.class),
        @OpenApi.Property(name = "dimensions", value = OpenApi.EntityType[].class)
      })
  @GetMapping("/dataSet/{uid}")
  public ResponseEntity<JsonRoot> getDimensionsForDataSet(
      @PathVariable String uid,
      @RequestParam(value = "links", defaultValue = "true", required = false) boolean links,
      @RequestParam(defaultValue = "*") List<FieldPath> fields)
      throws NotFoundException {
    WebMetadata metadata = new WebMetadata();

    DataSet dataSet = identifiableObjectManager.get(DataSet.class, uid);

    if (dataSet == null) {
      throw new NotFoundException(DataSet.class, uid);
    }

    List<DimensionalObject> dimensions = new ArrayList<>();
    dimensions.addAll(
        dataSet.getCategoryCombo().getCategories().stream().filter(ca -> !ca.isDefault()).toList());
    dimensions.addAll(dataSet.getCategoryOptionGroupSets());

    dimensions =
        dimensionService.filterReadableObjects(CurrentUserUtil.getCurrentUserDetails(), dimensions);

    ArrayList<DimensionalObject> copies = new ArrayList<>();
    for (DimensionalObject dim : dimensions) {
      copies.add(dimensionService.getDimensionalObjectCopy(dim.getUid(), true));
    }
    metadata.setDimensions(copies);

    if (links) {
      linkService.generateLinks(metadata, false);
    }

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(metadata.getDimensions(), fields);

    return ResponseEntity.ok(new JsonRoot("dimensions", objectNodes));
  }

  private PagedEntities<DimensionalObject> getPagedEntities(GetObjectListParams params) {
    Query<DimensionalObject> filteredQuery =
        queryService.getQueryFromUrl(DimensionalObject.class, params);
    filteredQuery.setObjects(dimensionService.getAllDimensions());

    filteredQuery.setSkipPaging(true); // paging is done post
    List<DimensionalObject> filteredNotPaged = queryService.query(filteredQuery);
    return PaginationUtils.addPagingIfEnabled(params, filteredNotPaged);
  }
}
