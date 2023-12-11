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
package org.hisp.dhis.datavalue;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Service("org.hisp.dhis.datavalue.DataAccessManager")
public class DefaultAggregateAccessManager implements AggregateAccessManager {
  private final Cache<List<String>> canDataWriteCocCache;

  private final AclService aclService;

  public DefaultAggregateAccessManager(AclService aclService, CacheProvider cacheProvider) {
    checkNotNull(aclService);
    checkNotNull(cacheProvider);

    this.aclService = aclService;
    this.canDataWriteCocCache = cacheProvider.createCanDataWriteCocCache();
  }

  // ---------------------------------------------------------------------
  // AggregateAccessManager implementation
  // ---------------------------------------------------------------------

  @Override
  public List<String> canRead(UserDetails currentUser, DataValue dataValue) {
    List<String> errors = new ArrayList<>();

    if (currentUser == null || currentUser.isSuper()) {
      return errors;
    }

    Set<CategoryOption> options = new HashSet<>();

    CategoryOptionCombo categoryOptionCombo = dataValue.getCategoryOptionCombo();

    if (categoryOptionCombo != null) {
      options.addAll(categoryOptionCombo.getCategoryOptions());
    }

    CategoryOptionCombo attributeOptionCombo = dataValue.getAttributeOptionCombo();

    if (attributeOptionCombo != null) {
      options.addAll(attributeOptionCombo.getCategoryOptions());
    }

    options.forEach(
        option -> {
          if (!aclService.canDataRead(currentUser, option)) {
            errors.add("User has no data read access for CategoryOption: " + option.getUid());
          }
        });

    return errors;
  }

  @Override
  public List<String> canWrite(UserDetails userDetails, DataSet dataSet) {
    List<String> errors = new ArrayList<>();

    if (userDetails == null || userDetails.isSuper()) {
      return errors;
    }

    if (!aclService.canDataWrite(userDetails, dataSet)) {
      errors.add("User does not have write access for DataSet: " + dataSet.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canRead(UserDetails currentUser, DataSet dataSet) {
    List<String> errors = new ArrayList<>();

    if (currentUser == null || currentUser.isSuper()) {
      return errors;
    }

    if (!aclService.canDataRead(currentUser, dataSet)) {
      errors.add("User does not have read access for DataSet: " + dataSet.getUid());
    }

    return errors;
  }

  @Override
  public List<String> canWrite(UserDetails userDetails, CategoryOptionCombo optionCombo) {
    if (userDetails == null || userDetails.isSuper()) {
      return emptyList();
    }

    Set<CategoryOption> options = optionCombo.getCategoryOptions();
    List<String> errors = new ArrayList<>();
    options.forEach(
        attrOption -> {
          if (!aclService.canDataWrite(userDetails, attrOption)) {
            errors.add("User has no data write access for CategoryOption: " + attrOption.getUid());
          }
        });

    return errors;
  }

  @Override
  public List<String> canWriteCached(UserDetails currentUser, CategoryOptionCombo optionCombo) {
    String cacheKey = CurrentUserUtil.getCurrentUserDetails().getUid() + "-" + optionCombo.getUid();

    return canDataWriteCocCache.get(cacheKey, key -> canWrite(currentUser, optionCombo));
  }

  @Override
  public List<String> canRead(UserDetails currentUser, CategoryOptionCombo optionCombo) {
    List<String> errors = new ArrayList<>();

    if (currentUser == null || currentUser.isSuper()) {
      return errors;
    }

    Set<CategoryOption> options = optionCombo.getCategoryOptions();

    options.forEach(
        attrOption -> {
          if (!aclService.canDataRead(currentUser, attrOption)) {
            errors.add("User has no data read access for CategoryOption: " + attrOption.getUid());
          }
        });

    return errors;
  }

  @Override
  public List<String> canWrite(UserDetails currentUser, DataElementOperand dataElementOperand) {
    List<String> errors = new ArrayList<>();

    if (currentUser == null || currentUser.isSuper()) {
      return errors;
    }

    Set<CategoryOption> options = new HashSet<>();

    CategoryOptionCombo categoryOptionCombo = dataElementOperand.getCategoryOptionCombo();

    if (categoryOptionCombo != null) {
      options.addAll(categoryOptionCombo.getCategoryOptions());
    }

    CategoryOptionCombo attributeOptionCombo = dataElementOperand.getAttributeOptionCombo();

    if (attributeOptionCombo != null) {
      options.addAll(attributeOptionCombo.getCategoryOptions());
    }

    options.forEach(
        option -> {
          if (!aclService.canDataWrite(currentUser, option)) {
            errors.add("User has no data write access for CategoryOption: " + option.getUid());
          }
        });

    return errors;
  }
}
