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
package org.hisp.dhis.option;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.option.OptionService")
public class DefaultOptionService implements OptionService {
  @Qualifier("org.hisp.dhis.option.OptionSetStore")
  private final IdentifiableObjectStore<OptionSet> optionSetStore;

  private final OptionStore optionStore;

  private final OptionGroupStore optionGroupStore;

  private final OptionGroupSetStore optionGroupSetStore;

  // -------------------------------------------------------------------------
  // OptionService implementation
  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------
  // Option Set
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long saveOptionSet(OptionSet optionSet) throws ConflictException {
    validateOptionSet(optionSet);
    optionSetStore.save(optionSet);
    return optionSet.getId();
  }

  @Override
  @Transactional
  public void updateOptionSet(OptionSet optionSet) throws ConflictException {
    validateOptionSet(optionSet);
    optionSetStore.update(optionSet);
  }

  @Override
  @Transactional(readOnly = true)
  public void validateOptionSet(OptionSet optionSet) throws ConflictException {
    if (optionSet.getValueType() != ValueType.MULTI_TEXT) {
      return;
    }
    for (Option option : optionSet.getOptions()) {
      if (option.getCode() == null) {
        String uid = option.getUid();
        if (uid != null) {
          option = optionStore.getByUid(uid);
          if (option == null) {
            throw new ConflictException(ErrorCode.E1113, Option.class.getSimpleName(), uid);
          }
        }
      }
      if (option.getCode() == null) {
        throw new ConflictException(ErrorCode.E4000, "code");
      }
      validateOption(optionSet, option);
    }
  }

  @Override
  public void validateOption(OptionSet optionSet, Option option) throws ConflictException {
    if (optionSet != null
        && optionSet.getValueType() == ValueType.MULTI_TEXT
        && option.getCode().contains(ValueType.MULTI_TEXT_SEPARATOR)) {
      throw new ConflictException(ErrorCode.E1118, optionSet.getUid(), option.getCode());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public OptionSet getOptionSet(String uid) {
    return optionSetStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public OptionSet getOptionSetByCode(String code) {
    return optionSetStore.getByCode(code);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OptionSet> getAllOptionSets() {
    return optionSetStore.getAll();
  }

  // -------------------------------------------------------------------------
  // Option
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<Option> findOptionsByNamePattern(
      @Nonnull String optionSet, @CheckForNull String infix, @CheckForNull Integer maxResults) {
    return optionStore.findOptionsByNamePattern(UID.of(optionSet), infix, maxResults);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsAllOptions(@Nonnull String optionSet, @Nonnull Collection<String> codes) {
    return optionStore.existsAllOptions(UID.of(optionSet), codes);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Option> findOptionByCode(@Nonnull String optionSet, @Nonnull String code) {
    return optionStore.findOptionByCode(UID.of(optionSet), code);
  }

  // -------------------------------------------------------------------------
  // OptionGroup
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void saveOptionGroup(OptionGroup group) {
    optionGroupStore.save(group);
  }

  @Override
  @Transactional(readOnly = true)
  public OptionGroup getOptionGroup(String uid) {
    return optionGroupStore.getByUid(uid);
  }

  // -------------------------------------------------------------------------
  // OptionGroupSet
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void saveOptionGroupSet(OptionGroupSet group) {
    optionGroupSetStore.save(group);
  }

  @Override
  @Transactional(readOnly = true)
  public OptionGroupSet getOptionGroupSet(String uid) {
    return optionGroupSetStore.getByUid(uid);
  }
}
