/*
 * Copyright (c) 2004-2026, University of Oslo
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

import java.util.Date;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.category.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests metadata integrity check which identifies a start or end date on the default category
 * option {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/default_category_option_start_end_dates.yaml}
 */
class DataIntegrityCategoryOptionDefaultDatesControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  @Autowired private CategoryService categoryService;

  @Autowired private CategoryOptionStore categoryOptionStore;

  private final String check = "category_options_default_has_start_or_end_date";

  private final String detailsIdType = "categoryOptions";

  @Test
  void testDefaultCategoryOptionHasNoDates() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDefaultCategoryOptionHasEndDate() {
    setDefaultCategoryOptionDates(null, date(2020, 12, 31));

    assertHasDataIntegrityIssues(
        detailsIdType,
        check,
        100,
        categoryService.getDefaultCategoryOption().getUid(),
        "default",
        "Start date: none, end date: 2020-12-31",
        true);
  }

  @Test
  void testDefaultCategoryOptionHasStartAndEndDate() {
    setDefaultCategoryOptionDates(date(2019, 1, 1), date(2020, 12, 31));

    assertHasDataIntegrityIssues(
        detailsIdType,
        check,
        100,
        categoryService.getDefaultCategoryOption().getUid(),
        "default",
        "Start date: 2019-01-01, end date: 2020-12-31",
        true);
  }

  private void setDefaultCategoryOptionDates(Date startDate, Date endDate) {
    // The API may not allow changes to the default category option, so set the dates directly
    // through the store.
    CategoryOption defaultOption = categoryService.getDefaultCategoryOption();
    defaultOption.setStartDate(startDate);
    defaultOption.setEndDate(endDate);
    categoryOptionStore.update(defaultOption);
    manager.flush();
    manager.clear();
  }
}
