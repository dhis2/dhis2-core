package org.hisp.dhis.category;

import java.util.Set;

public record CategoryOptionComboUpdateDto(
    String code,
    Boolean ignoreApproval,
    CategoryCombo categoryCombo,
    Set<CategoryOption> categoryOptions) {}
