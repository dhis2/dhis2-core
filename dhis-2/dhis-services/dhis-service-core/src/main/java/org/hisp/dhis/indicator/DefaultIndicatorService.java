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
package org.hisp.dhis.indicator;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.indicator.IndicatorService")
public class DefaultIndicatorService implements IndicatorService {
  private final IndicatorStore indicatorStore;

  @Qualifier("org.hisp.dhis.indicator.IndicatorTypeStore")
  private final IdentifiableObjectStore<IndicatorType> indicatorTypeStore;

  @Qualifier("org.hisp.dhis.indicator.IndicatorGroupStore")
  private final IdentifiableObjectStore<IndicatorGroup> indicatorGroupStore;

  @Qualifier("org.hisp.dhis.indicator.IndicatorGroupSetStore")
  private final IdentifiableObjectStore<IndicatorGroupSet> indicatorGroupSetStore;

  // -------------------------------------------------------------------------
  // Indicator
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addIndicator(Indicator indicator) {
    indicatorStore.save(indicator);

    return indicator.getId();
  }

  @Override
  @Transactional
  public void updateIndicator(Indicator indicator) {
    indicatorStore.update(indicator);
  }

  @Override
  @Transactional
  public void deleteIndicator(Indicator indicator) {
    indicatorStore.delete(indicator);
  }

  @Override
  @Transactional(readOnly = true)
  public Indicator getIndicator(long id) {
    return indicatorStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Indicator getIndicator(String uid) {
    return indicatorStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Indicator> getAllIndicators() {
    return indicatorStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Indicator> getIndicatorsWithGroupSets() {
    return indicatorStore.getIndicatorsWithGroupSets();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Indicator> getIndicatorsWithoutGroups() {
    return indicatorStore.getIndicatorsWithoutGroups();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Indicator> getIndicatorsWithDataSets() {
    return indicatorStore.getIndicatorsWithDataSets();
  }

  // -------------------------------------------------------------------------
  // IndicatorType
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addIndicatorType(IndicatorType indicatorType) {
    indicatorTypeStore.save(indicatorType);

    return indicatorType.getId();
  }

  @Override
  @Transactional
  public void updateIndicatorType(IndicatorType indicatorType) {
    indicatorTypeStore.update(indicatorType);
  }

  @Override
  @Transactional
  public void deleteIndicatorType(IndicatorType indicatorType) {
    indicatorTypeStore.delete(indicatorType);
  }

  @Override
  @Transactional(readOnly = true)
  public IndicatorType getIndicatorType(long id) {
    return indicatorTypeStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public IndicatorType getIndicatorType(String uid) {
    return indicatorTypeStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IndicatorType> getAllIndicatorTypes() {
    return indicatorTypeStore.getAll();
  }

  // -------------------------------------------------------------------------
  // IndicatorGroup
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addIndicatorGroup(IndicatorGroup indicatorGroup) {
    indicatorGroupStore.save(indicatorGroup);

    return indicatorGroup.getId();
  }

  @Override
  @Transactional
  public void updateIndicatorGroup(IndicatorGroup indicatorGroup) {
    indicatorGroupStore.update(indicatorGroup);
  }

  @Override
  @Transactional
  public void deleteIndicatorGroup(IndicatorGroup indicatorGroup) {
    indicatorGroupStore.delete(indicatorGroup);
  }

  @Override
  @Transactional(readOnly = true)
  public IndicatorGroup getIndicatorGroup(long id) {
    return indicatorGroupStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public IndicatorGroup getIndicatorGroup(String uid) {
    return indicatorGroupStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IndicatorGroup> getAllIndicatorGroups() {
    return indicatorGroupStore.getAll();
  }

  // -------------------------------------------------------------------------
  // IndicatorGroupSet
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addIndicatorGroupSet(IndicatorGroupSet groupSet) {
    indicatorGroupSetStore.save(groupSet);

    return groupSet.getId();
  }

  @Override
  @Transactional
  public void updateIndicatorGroupSet(IndicatorGroupSet groupSet) {
    indicatorGroupSetStore.update(groupSet);
  }

  @Override
  @Transactional
  public void deleteIndicatorGroupSet(IndicatorGroupSet groupSet) {
    indicatorGroupSetStore.delete(groupSet);
  }

  @Override
  @Transactional(readOnly = true)
  public IndicatorGroupSet getIndicatorGroupSet(long id) {
    return indicatorGroupSetStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public IndicatorGroupSet getIndicatorGroupSet(String uid) {
    return indicatorGroupSetStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IndicatorGroupSet> getAllIndicatorGroupSets() {
    return indicatorGroupSetStore.getAll();
  }
}
