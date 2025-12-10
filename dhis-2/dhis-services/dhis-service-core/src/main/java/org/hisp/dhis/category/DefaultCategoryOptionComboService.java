package org.hisp.dhis.category;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultCategoryOptionComboService implements CategoryOptionComboService {

  private final AclService aclService;
  private final IdentifiableObjectManager identifiableObjectManager;

  @Override
  public void updateCoc(CategoryOptionCombo persisted, CategoryOptionComboUpdateDto cocUpdate)
      throws ConflictException {

    validate(persisted, cocUpdate);

    if (cocUpdate.code() != null) {
      persisted.setCode(cocUpdate.code());
    }

    if (cocUpdate.ignoreApproval() != null) {
      persisted.setIgnoreApproval(cocUpdate.ignoreApproval());
    }

    identifiableObjectManager.save(persisted);
  }

  private void validate(CategoryOptionCombo persistedCoc, CategoryOptionComboUpdateDto updatedCoc)
      throws ConflictException {
    if (updatedCoc.categoryCombo() == null) {
      throw new ConflictException(ErrorCode.E1133);
    }
    if (updatedCoc.categoryOptions() == null || updatedCoc.categoryOptions().isEmpty()) {
      throw new ConflictException(ErrorCode.E1134);
    }

    if (!persistedCoc.getCategoryCombo().getUid().equals(updatedCoc.categoryCombo().getUid())) {
      throw new ConflictException(ErrorCode.E1135);
    }

    if (!IdentifiableObjectUtils.uidsMatch(
        updatedCoc.categoryOptions(), persistedCoc.getCategoryOptions())) {
      throw new ConflictException(ErrorCode.E1136);
    }
  }
}
