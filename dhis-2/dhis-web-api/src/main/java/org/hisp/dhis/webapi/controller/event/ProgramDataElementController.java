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
package org.hisp.dhis.webapi.controller.event;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@OpenApi.EntityType(ProgramDataElementDimensionItem.class)
@OpenApi.Document(
    entity = DimensionalItemObject.class,
    classifiers = {"team:tracker", "purpose:metadata"})
@Controller
@RequestMapping("/api/programDataElements")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ProgramDataElementController {

  private final QueryService queryService;
  private final FieldFilterService fieldFilterService;
  private final ProgramService programService;

  @GetMapping
  public @ResponseBody RootNode getObjectList(
      @OpenApi.Param({UID.class, Program.class}) @RequestParam String program,
      GetObjectListParams params)
      throws QueryParserException {

    List<ProgramDataElementDimensionItem> allItems =
        programService.getGeneratedProgramDataElements(program);

    Pager pager = null;
    if (params.isPaging()) {
      params.setPaging(false);
      Query<ProgramDataElementDimensionItem> queryForCount =
          queryService.getQueryFromUrl(ProgramDataElementDimensionItem.class, params);
      queryForCount.setObjects(allItems);
      List<?> totalOfItems = queryService.query(queryForCount);
      int countTotal = isNotEmpty(totalOfItems) ? totalOfItems.size() : 0;
      pager = new Pager(params.getPage(), countTotal, params.getPageSize());
      params.setPaging(true);
    }

    Query<ProgramDataElementDimensionItem> query =
        queryService.getQueryFromUrl(ProgramDataElementDimensionItem.class, params);
    query.setDefaultOrder();
    query.setObjects(allItems);

    List<ProgramDataElementDimensionItem> pageItems = queryService.query(query);

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(NodeUtils.createPager(pager));
    }

    List<String> fields = params.getFields();
    if (fields == null || fields.isEmpty()) fields = List.of("*");
    rootNode.addChild(
        fieldFilterService.toCollectionNode(
            ProgramDataElementDimensionItem.class, new FieldFilterParams(pageItems, fields)));

    return rootNode;
  }
}
