package org.hisp.dhis.category;

import org.hisp.dhis.feedback.ConflictException;

public interface CategoryOptionComboService {
  void updateCoc(CategoryOptionCombo persisted, CategoryOptionComboUpdateDto cocUpdate)
      throws ConflictException;

  //  void validate(CategoryOptionCombo persisted, CategoryOptionCombo cocUpdate)
  //      throws ConflictException;
}
