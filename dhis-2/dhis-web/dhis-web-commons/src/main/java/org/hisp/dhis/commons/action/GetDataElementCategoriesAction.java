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
package org.hisp.dhis.commons.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.User;

/**
 * @author mortenoh
 */
public class GetDataElementCategoriesAction extends ActionPagingSupport<Category> {
  static enum CategoryType {
    DISAGGREGATION,
    ATTRIBUTE
  }

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private CategoryService dataElementCategoryService;

  public void setCategoryService(CategoryService dataElementCategoryService) {
    this.dataElementCategoryService = dataElementCategoryService;
  }

  // -------------------------------------------------------------------------
  // Input & Output
  // -------------------------------------------------------------------------

  private List<Category> dataElementCategories;

  public List<Category> getDataElementCategories() {
    return dataElementCategories;
  }

  private CategoryType type;

  public void setType(CategoryType type) {
    this.type = type;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() throws Exception {
    canReadType(Category.class);

    if (type == null) {
      dataElementCategories =
          new ArrayList<>(dataElementCategoryService.getAllDataElementCategories());
    } else if (type.equals(CategoryType.ATTRIBUTE)) {
      dataElementCategories = new ArrayList<>(dataElementCategoryService.getAttributeCategories());
    } else if (type.equals(CategoryType.DISAGGREGATION)) {
      dataElementCategories =
          new ArrayList<>(dataElementCategoryService.getDisaggregationCategories());
    }

    User currentUser = currentUserService.getCurrentUser();
    dataElementCategories.forEach(instance -> canReadInstance(instance, currentUser));

    Collections.sort(dataElementCategories);

    if (usePaging) {
      this.paging = createPaging(dataElementCategories.size());

      dataElementCategories =
          dataElementCategories.subList(paging.getStartPos(), paging.getEndPos());
    }

    return SUCCESS;
  }
}
