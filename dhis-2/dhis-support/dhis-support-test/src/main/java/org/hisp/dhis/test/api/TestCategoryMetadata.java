package org.hisp.dhis.test.api;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;

public record TestCategoryMetadata(
    CategoryCombo cc1,
    CategoryCombo cc2,
    Category c1,
    Category c2,
    Category c3,
    Category c4,
    CategoryOption co1,
    CategoryOption co2,
    CategoryOption co3,
    CategoryOption co4,
    CategoryOption co5,
    CategoryOption co6,
    CategoryOption co7,
    CategoryOption co8,
    CategoryOptionCombo coc1,
    CategoryOptionCombo coc2,
    CategoryOptionCombo coc3,
    CategoryOptionCombo coc4) {}
