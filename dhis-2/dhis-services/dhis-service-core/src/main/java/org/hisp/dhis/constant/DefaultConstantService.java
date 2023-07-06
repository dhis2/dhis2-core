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
package org.hisp.dhis.constant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Dang Duy Hieu
 * @version $Id DefaultConstantService.java July 29, 2011$
 */
@Service("org.hisp.dhis.constant.ConstantService")
public class DefaultConstantService implements ConstantService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private IdentifiableObjectStore<Constant> constantStore;

  public DefaultConstantService(
      @Qualifier("org.hisp.dhis.constant.ConstantStore")
          IdentifiableObjectStore<Constant> constantStore) {
    checkNotNull(constantStore);

    this.constantStore = constantStore;
  }

  // -------------------------------------------------------------------------
  // Constant
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long saveConstant(Constant constant) {
    constantStore.save(constant);
    return constant.getId();
  }

  @Override
  @Transactional
  public void updateConstant(Constant constant) {
    constantStore.update(constant);
  }

  @Override
  @Transactional
  public void deleteConstant(Constant constant) {
    constantStore.delete(constant);
  }

  @Override
  @Transactional(readOnly = true)
  public Constant getConstant(int constantId) {
    return constantStore.get(constantId);
  }

  @Override
  @Transactional(readOnly = true)
  public Constant getConstant(String uid) {
    return constantStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Constant> getAllConstants() {
    return constantStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Constant> getConstantMap() {
    return getAllConstants().stream()
        .collect(Collectors.toMap(c -> c.getUid(), Function.identity()));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Double> getConstantParameterMap() {
    Map<String, Double> map = new HashMap<>();

    for (Constant constant : getAllConstants()) {
      map.put(constant.getName(), constant.getValue());
    }

    return map;
  }

  // -------------------------------------------------------------------------
  // Constant expanding
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public int getConstantCount() {
    return constantStore.getCount();
  }

  @Override
  @Transactional(readOnly = true)
  public int getConstantCountByName(String name) {
    return constantStore.getCountLikeName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Constant> getConstantsBetween(int first, int max) {
    return constantStore.getAllOrderedName(first, max);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Constant> getConstantsBetweenByName(String name, int first, int max) {
    return constantStore.getAllLikeName(name, first, max);
  }
}
