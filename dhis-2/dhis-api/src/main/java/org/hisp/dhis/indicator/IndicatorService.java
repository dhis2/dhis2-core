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
import org.hisp.dhis.common.UID;

/**
 * @author Lars Helge Overland
 */
public interface IndicatorService {
  // -------------------------------------------------------------------------
  // Indicator
  // -------------------------------------------------------------------------

  long addIndicator(Indicator indicator);

  void updateIndicator(Indicator indicator);

  void deleteIndicator(Indicator indicator);

  Indicator getIndicator(long id);

  Indicator getIndicator(String uid);

  List<Indicator> getAllIndicators();

  List<Indicator> getIndicatorsWithGroupSets();

  List<Indicator> getIndicatorsWithoutGroups();

  List<Indicator> getIndicatorsWithDataSets();

  List<Indicator> getAssociatedIndicators(List<IndicatorType> indicatorTypes);

  List<Indicator> getIndicatorsByUid(List<String> uids);

  /**
   * Get all {@link Indicator}s that have the uid contained in its numerator
   *
   * @param uid to search
   * @return matching {@link Indicator}s
   */
  List<Indicator> getIndicatorsWithNumeratorContaining(UID uid);

  /**
   * Get all {@link Indicator}s that have the uid contained in its denominator
   *
   * @param uid to search
   * @return matching {@link Indicator}s
   */
  List<Indicator> getIndicatorsWithDenominatorContaining(UID uid);

  // -------------------------------------------------------------------------
  // IndicatorType
  // -------------------------------------------------------------------------

  long addIndicatorType(IndicatorType indicatorType);

  void updateIndicatorType(IndicatorType indicatorType);

  void deleteIndicatorType(IndicatorType indicatorType);

  IndicatorType getIndicatorType(long id);

  IndicatorType getIndicatorType(String uid);

  List<IndicatorType> getAllIndicatorTypes();

  List<IndicatorType> getIndicatorTypesByUid(List<String> uids);

  // -------------------------------------------------------------------------
  // IndicatorGroup
  // -------------------------------------------------------------------------

  long addIndicatorGroup(IndicatorGroup indicatorGroup);

  void updateIndicatorGroup(IndicatorGroup indicatorGroup);

  void deleteIndicatorGroup(IndicatorGroup indicatorGroup);

  IndicatorGroup getIndicatorGroup(long id);

  IndicatorGroup getIndicatorGroup(String uid);

  List<IndicatorGroup> getAllIndicatorGroups();

  // -------------------------------------------------------------------------
  // IndicatorGroupSet
  // -------------------------------------------------------------------------

  long addIndicatorGroupSet(IndicatorGroupSet groupSet);

  void updateIndicatorGroupSet(IndicatorGroupSet groupSet);

  void deleteIndicatorGroupSet(IndicatorGroupSet groupSet);

  IndicatorGroupSet getIndicatorGroupSet(long id);

  IndicatorGroupSet getIndicatorGroupSet(String uid);

  List<IndicatorGroupSet> getAllIndicatorGroupSets();
}
