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
package org.hisp.dhis.webapi.controller.dataelement;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.LinkService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.EntityType(DataElementOperand.class)
@OpenApi.Document(
    entity = DataElementOperand.class,
    classifiers = {"team:platform", "purpose:metadata"})
@Controller
@RequestMapping("/api/dataElementOperands")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
public class DataElementOperandController {
  private final IdentifiableObjectManager manager;

  private final QueryService queryService;

  private final FieldFilterService fieldFilterService;

  private final LinkService linkService;

  private final CategoryService dataElementCategoryService;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @OpenApi.Property
  public static final class GetDataElementOperandObjectListParams extends GetObjectListParams {
    @OpenApi.Description(
        "When set all existing operands are the basis of the result list (takes precedence).")
    boolean persisted;

    @OpenApi.Description(
        "Whether to include totals when loading operands by `dataSet` or data element groups from `filter`s.")
    boolean totals;

    @OpenApi.Description(
        "When set the operands linked to the specified dataset are the basis for the result list.")
    @OpenApi.Property({UID.class, DataSet.class})
    String dataSet;
  }

  private List<DataElementOperand> getUnfilteredDataElementOperands(
      GetDataElementOperandObjectListParams params) {
    List<DataElementOperand> dataElementOperands = List.of();

    if (params.isPersisted()) {
      dataElementOperands = Lists.newArrayList(manager.getAll(DataElementOperand.class));
    } else {
      boolean totals = params.isTotals();

      String deg =
          CollectionUtils.popStartsWith(
              params.getFilters(), "dataElement.dataElementGroups.id:eq:");
      deg = deg != null ? deg.substring("dataElement.dataElementGroups.id:eq:".length()) : null;

      String ds = params.getDataSet();

      if (deg != null) {
        DataElementGroup dataElementGroup = manager.get(DataElementGroup.class, deg);
        if (dataElementGroup != null) {
          dataElementOperands =
              dataElementCategoryService.getOperands(dataElementGroup.getMembers(), totals);
        }
      } else if (ds != null) {
        DataSet dataSet = manager.get(DataSet.class, ds);
        dataElementOperands = dataElementCategoryService.getOperands(dataSet, totals);
      } else {
        List<DataElement> dataElements = new ArrayList<>(manager.getAllSorted(DataElement.class));
        dataElementOperands = dataElementCategoryService.getOperands(dataElements, totals);
      }
    }
    return dataElementOperands;
  }

  @GetMapping
  public @ResponseBody RootNode getObjectList(GetDataElementOperandObjectListParams params)
      throws QueryParserException {
    List<DataElementOperand> allItems = getUnfilteredDataElementOperands(params);

    // This is needed for two reasons:
    // 1) We are doing in-memory paging;
    // 2) We have to count all items respecting the filtering and the
    // initial universe of elements. In this case, the variable
    // "dataElementOperands".
    Pager pager = null;
    if (params.isPaging()) {
      params.setPaging(false);
      Query<DataElementOperand> queryForCount =
          queryService.getQueryFromUrl(DataElementOperand.class, params);
      queryForCount.setObjects(allItems);

      List<?> totalOfItems = queryService.query(queryForCount);
      int countTotal = isNotEmpty(totalOfItems) ? totalOfItems.size() : 0;
      pager = new Pager(params.getPage(), countTotal, params.getPageSize());
      linkService.generatePagerLinks(pager, DataElementOperand.class);
      params.setPaging(true);
    }

    Query<DataElementOperand> query =
        queryService.getQueryFromUrl(DataElementOperand.class, params);
    query.setDefaultOrder();
    query.setObjects(allItems);

    List<DataElementOperand> pageItems = queryService.query(query);

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(NodeUtils.createPager(pager));
    }

    List<String> fields = params.getFields();
    if (fields == null || fields.isEmpty()) fields = List.of("*");

    rootNode.addChild(
        fieldFilterService.toCollectionNode(
            DataElementOperand.class, new FieldFilterParams(pageItems, fields)));

    return rootNode;
  }
}
