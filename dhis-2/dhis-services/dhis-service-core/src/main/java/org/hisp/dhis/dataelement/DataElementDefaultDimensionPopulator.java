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
package org.hisp.dhis.dataelement;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.user.CurrentUserUtil.clearSecurityContext;
import static org.hisp.dhis.user.CurrentUserUtil.injectUserInSecurityContext;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;

/**
 * When storing DataValues without associated dimensions there is a need to refer to a default
 * dimension. This populator persists a CategoryCombo named by the
 * CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME property and a corresponding
 * DataElementCatoryOptionCombo which should be used for this purpose.
 *
 * @author Lars Helge Overland
 * @author Abyot Aselefew
 */
@Slf4j
public class DataElementDefaultDimensionPopulator extends TransactionContextStartupRoutine {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final DataElementService dataElementService;
  private final CategoryService categoryService;

  public DataElementDefaultDimensionPopulator(
      DataElementService dataElementService, CategoryService categoryService) {
    checkNotNull(dataElementService);
    checkNotNull(categoryService);
    this.dataElementService = dataElementService;
    this.categoryService = categoryService;
  }

  // -------------------------------------------------------------------------
  // Execute
  // -------------------------------------------------------------------------

  @Override
  public void executeInTransaction() {
    SystemUser actingUser = new SystemUser();

    boolean hasCurrentUser = CurrentUserUtil.hasCurrentUser();
    if (!hasCurrentUser) {
      injectUserInSecurityContext(actingUser);
    }

    Category defaultCategory =
        categoryService.getCategoryByName(Category.DEFAULT_NAME, new SystemUser());

    if (defaultCategory == null) {
      categoryService.generateDefaultDimension(actingUser);
      defaultCategory = categoryService.getCategoryByName(Category.DEFAULT_NAME, new SystemUser());
      log.info("Added default category");
    }

    categoryService.updateCategory(defaultCategory, actingUser);
    String defaultName = CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
    CategoryCombo categoryCombo = categoryService.getCategoryComboByName(defaultName);

    if (categoryCombo == null) {
      categoryService.generateDefaultDimension(actingUser);
      log.info("Added default dataelement dimension");
      categoryCombo = categoryService.getCategoryComboByName(defaultName);
    }

    // ---------------------------------------------------------------------
    // Any data elements without dimensions need to be associated at least
    // with the default dimension
    // ---------------------------------------------------------------------

    Collection<DataElement> dataElements = dataElementService.getAllDataElements();

    for (DataElement dataElement : dataElements) {
      if (dataElement.getCategoryCombo() == null) {
        dataElement.setCategoryCombo(categoryCombo);
        dataElementService.updateDataElement(dataElement);
      }
    }

    if (!hasCurrentUser) {
      clearSecurityContext();
    }
  }
}
