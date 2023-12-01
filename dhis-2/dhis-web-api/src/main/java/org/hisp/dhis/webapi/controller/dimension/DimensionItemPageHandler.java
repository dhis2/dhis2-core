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
package org.hisp.dhis.webapi.controller.dimension;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.hisp.dhis.node.NodeUtils.createPager;
import static org.hisp.dhis.webapi.controller.dimension.DimensionController.RESOURCE_PATH;

import java.util.Map;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Component;

/**
 * Small component specialized on pagination rules specific to dimension items. This can be seen as
 * a helper extension of {@link DimensionController#getItems(String, Map, OrderParams)}.
 *
 * @author maikel arabori
 */
@Component
public class DimensionItemPageHandler {

  private LinkService linkService;

  DimensionItemPageHandler(final LinkService linkService) {
    checkNotNull(linkService);
    this.linkService = linkService;
  }

  /**
   * This method will add the pagination child node to the given root node. For this to happen the
   * pagination flag must be set to true. See {@link WebOptions#hasPaging(boolean)}.
   *
   * @param rootNode the root node where the pagination node will be appended to.
   * @param webOptions the WebOptions settings.
   * @param dimensionUid the uid of the dimension queried in the API url. See {@link
   *     DimensionController#getItems(String, Map, OrderParams)}.
   * @param totalOfItems the total of items. This is represented as page total. See {@link
   *     Pager#getTotal()}.
   */
  void addPaginationToNodeIfEnabled(
      final RootNode rootNode,
      final WebOptions webOptions,
      final String dimensionUid,
      final int totalOfItems) {
    final boolean isPaginationEnabled = webOptions.hasPaging(false);

    if (isPaginationEnabled) {
      final String apiRelativeUrl = format(RESOURCE_PATH + "/%s/items", dimensionUid);
      final Pager pager = new Pager(webOptions.getPage(), totalOfItems, webOptions.getPageSize());

      linkService.generatePagerLinks(pager, apiRelativeUrl);
      rootNode.addChild(createPager(pager));
    }
  }
}
