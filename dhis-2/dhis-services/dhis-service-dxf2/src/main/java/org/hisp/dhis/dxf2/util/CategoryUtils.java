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
package org.hisp.dhis.dxf2.util;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
@Slf4j
@Component
public class CategoryUtils {

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired private CategoryService categoryService;

  public ImportSummaries addAndPruneOptionCombos(CategoryCombo categoryCombo) {
    ImportSummaries importSummaries = new ImportSummaries();

    if (categoryCombo == null || !categoryCombo.isValid()) {
      log.warn(
          "Category combo is null or invalid, could not update option combos: " + categoryCombo);
      importSummaries.addImportSummary(
          new ImportSummary(
              ImportStatus.ERROR,
              "Category combo is null or invalid, could not update option combos: "
                  + categoryCombo));
      return importSummaries;
    }

    List<CategoryOptionCombo> generatedOptionCombos = categoryCombo.generateOptionCombosList();
    Set<CategoryOptionCombo> persistedOptionCombos =
        Sets.newHashSet(categoryCombo.getOptionCombos());

    boolean modified = false;

    for (CategoryOptionCombo optionCombo : generatedOptionCombos) {
      if (!persistedOptionCombos.contains(optionCombo)) {
        categoryCombo.getOptionCombos().add(optionCombo);
        categoryService.addCategoryOptionCombo(optionCombo);

        log.info(
            "Added missing category option combo: "
                + optionCombo
                + " for category combo: "
                + categoryCombo.getName());

        ImportSummary importSummary = new ImportSummary();
        importSummary.setDescription(
            "Added missing category option combo: ("
                + optionCombo.getName()
                + ") for category combo: "
                + categoryCombo.getName());
        importSummary.incrementImported();

        importSummaries.addImportSummary(importSummary);

        modified = true;
      }
    }

    for (CategoryOptionCombo optionCombo : persistedOptionCombos) {
      if (!generatedOptionCombos.contains(optionCombo)) {
        try {
          categoryService.deleteCategoryOptionCombo(optionCombo);
          categoryCombo.getOptionCombos().remove(optionCombo);

          log.info(
              "Deleted obsolete category option combo: "
                  + optionCombo.getName()
                  + " for category combo: "
                  + categoryCombo.getName());

          ImportSummary importSummary = new ImportSummary();
          importSummary.setDescription(
              "Deleted obsolete category option combo: ("
                  + optionCombo.getName()
                  + ") for category combo: "
                  + categoryCombo.getName());
          importSummary.incrementDeleted();

          importSummaries.addImportSummary(importSummary);

          modified = true;
        } catch (Exception ex) {
          log.warn("Could not delete category option combo: " + optionCombo);

          ImportSummary importSummary = new ImportSummary();
          importSummary.setStatus(ImportStatus.WARNING);
          importSummary.setDescription(
              "Could not delete category option combo: (" + optionCombo.getName() + ")");
          importSummary.incrementIgnored();

          importSummaries.addImportSummary(importSummary);
        }
      }
    }

    if (modified) {
      categoryService.updateCategoryCombo(categoryCombo);
    }

    return importSummaries;
  }
}
