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

import com.opensymphony.xwork2.Action;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Lars Helge Overland
 */
public class GetCategoryOptionCombosAction extends BaseAction implements Action {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private DataElementService dataElementService;

  public void setDataElementService(DataElementService dataElementService) {
    this.dataElementService = dataElementService;
  }

  private CategoryService categoryService;

  public void setCategoryService(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  // -------------------------------------------------------------------------
  // Input
  // -------------------------------------------------------------------------

  private Integer id;

  public void setId(Integer id) {
    this.id = id;
  }

  private Integer categoryComboId;

  public void setCategoryComboId(Integer categoryComboId) {
    this.categoryComboId = categoryComboId;
  }

  private String categoryComboUid;

  public void setCategoryComboUid(String categoryComboUid) {
    this.categoryComboUid = categoryComboUid;
  }

  // -------------------------------------------------------------------------
  // Output
  // -------------------------------------------------------------------------

  private Set<CategoryOptionCombo> categoryOptionCombos;

  public Set<CategoryOptionCombo> getCategoryOptionCombos() {
    return categoryOptionCombos;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() {
    canReadType(CategoryOptionCombo.class);

    if (id != null) {
      DataElement dataElement = dataElementService.getDataElement(id);

      if (dataElement != null) {
        categoryOptionCombos = dataElement.getCategoryOptionCombos();
      }
    } else if (categoryComboId != null) {
      CategoryCombo categoryCombo = categoryService.getCategoryCombo(categoryComboId);

      if (categoryCombo != null) {
        categoryOptionCombos = categoryCombo.getOptionCombos();
      }
    } else if (categoryComboUid != null) {
      CategoryCombo categoryCombo = categoryService.getCategoryCombo(categoryComboUid);

      if (categoryCombo != null) {
        categoryOptionCombos = categoryCombo.getOptionCombos();
      }
    }

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    categoryOptionCombos.forEach(instance -> canReadInstance(instance, currentUserDetails));

    return SUCCESS;
  }
}
