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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.test.webapi.json.domain.JsonCategoryOption;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests metadata integrity check for which identifies incorrect sharing on the default category
 * option {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/default_category_option_sharing.yaml}
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityCategoryOptionDefaultSharingTest extends AbstractDataIntegrityIntegrationTest {
  @Autowired private CategoryService categoryService;

  @Autowired private CategoryOptionStore categoryOptionStore;

  private final String check = "category_options_default_incorrect_sharing";

  private final String detailsIdType = "categoryOptions";

  @Test
  void testDefaultCategoryOptionHasCorrectSharing() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDefaultCategoryOptionHasIncorrectSharing() {

    // The controller is not going to allow us to update the sharing of the default category option
    // So we need to do it directly in through the service layer.
    CategoryOption defaultOption = categoryService.getDefaultCategoryOption();
    Sharing sharing = new Sharing();
    sharing.setPublicAccess("--------");
    defaultOption.setSharing(sharing);
    categoryOptionStore.update(defaultOption);
    String access = categoryService.getDefaultCategoryOption().getPublicAccess();
    assertEquals("--------", access);
    manager.flush();
    manager.clear();
    // Check the sharing from the API

    JsonCategoryOption jsonCategoryOption =
        GET("/categoryOptions/" + categoryService.getDefaultCategoryOption().getUid())
            .content()
            .as(JsonCategoryOption.class);
    assertEquals("\"--------\"", jsonCategoryOption.getSharing().getPublic().toString());

    assertHasDataIntegrityIssues(
        detailsIdType,
        check,
        100,
        categoryService.getDefaultCategoryOption().getUid(),
        "default",
        null,
        true);
  }
}
