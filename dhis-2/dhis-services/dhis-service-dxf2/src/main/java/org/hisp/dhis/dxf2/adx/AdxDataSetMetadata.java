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
package org.hisp.dhis.dxf2.adx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.xerces.util.XMLChar;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;

/**
 * @author bobj
 */
public class AdxDataSetMetadata {
  // Lookup category options per cat option combo

  private final Map<Long, Map<String, String>> categoryOptionMap;

  AdxDataSetMetadata(DataSet dataSet, IdSchemes idSchemes) throws AdxException {
    categoryOptionMap = new HashMap<>();

    Set<CategoryCombo> catCombos = new HashSet<>();

    catCombos.add(dataSet.getCategoryCombo());

    for (DataSetElement element : dataSet.getDataSetElements()) {
      catCombos.add(element.getResolvedCategoryCombo());
    }

    for (CategoryCombo categoryCombo : catCombos) {
      for (CategoryOptionCombo catOptCombo : categoryCombo.getOptionCombos()) {
        addExplodedCategoryAttributes(catOptCombo, idSchemes);
      }
    }
  }

  private void addExplodedCategoryAttributes(CategoryOptionCombo coc, IdSchemes idSchemes)
      throws AdxException {
    Map<String, String> categoryAttributes = new HashMap<>();

    IdScheme cScheme = idSchemes.getCategoryIdScheme();
    IdScheme coScheme = idSchemes.getCategoryOptionIdScheme();

    if (!coc.isDefault()) {
      for (Category category : coc.getCategoryCombo().getCategories()) {
        String categoryId = category.getPropertyValue(cScheme);

        if (categoryId == null || !XMLChar.isValidName(categoryId)) {
          throw new AdxException(
              "Category "
                  + cScheme.name()
                  + " for "
                  + category.getName()
                  + " is missing or invalid: "
                  + categoryId);
        }

        String catOptId = category.getCategoryOption(coc).getPropertyValue(coScheme);

        if (catOptId == null || catOptId.isEmpty()) {
          throw new AdxException(
              "CategoryOption "
                  + coScheme.name()
                  + " for "
                  + category.getCategoryOption(coc).getName()
                  + " is missing");
        }

        categoryAttributes.put(categoryId, catOptId);
      }
    }

    categoryOptionMap.put(coc.getId(), categoryAttributes);
  }

  public Map<String, String> getExplodedCategoryAttributes(long cocId) {
    return this.categoryOptionMap.get(cocId);
  }
}
