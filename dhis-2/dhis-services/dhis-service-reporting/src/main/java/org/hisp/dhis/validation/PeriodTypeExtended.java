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
package org.hisp.dhis.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * Holds information for each period type that is needed during a validation run (either interactive
 * or a scheduled run).
 *
 * <p>By computing these values once at the start of a validation run, we avoid the overhead of
 * having to compute them during the processing of every organisation unit. For some of these
 * properties this is also important because they should be copied from Hibernate lazy collections
 * before the multithreaded part of the run starts, otherwise the threads may not be able to access
 * these values.
 *
 * @author Jim Grace
 */
public class PeriodTypeExtended {
  private final PeriodType periodType;

  private final Set<Period> periods = new HashSet<>();

  private final Set<ValidationRuleExtended> ruleXs = new HashSet<>();

  private final Set<PeriodType> allowedPeriodTypes = new HashSet<>();

  private final Set<DimensionalItemObject> eventItems = new HashSet<>();

  private final Set<DimensionalItemObject> eventItemsWithoutAttributeOptions = new HashSet<>();

  private final Set<DimensionalItemObject> indicators = new HashSet<>();

  private final Set<DataElement> dataElements = new HashSet<>();

  private final Set<DataElementOperand> dataElementOperands = new HashSet<>();

  private final Map<Long, DataElement> dataElementIdMap = new HashMap<>();

  private final Map<String, DataElementOperand> dataElementOperandIdMap = new HashMap<>();

  private final Set<DimensionalItemId> leftSideItemIds = new HashSet<>();

  private final Set<DimensionalItemId> rightSideItemIds = new HashSet<>();

  private boolean slidingWindowsNeeded = false;

  private boolean nonSlidingWindowsNeeded = false;

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  public PeriodTypeExtended(PeriodType periodType) {
    this.periodType = periodType;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("periodType", periodType)
        .append("periods", periods)
        .append("ruleXs", ruleXs.toArray())
        .append("eventItems", eventItems)
        .append("eventItemsWithoutAttributeOptions", eventItemsWithoutAttributeOptions)
        .append("indicators", indicators)
        .append("dataElements", dataElements)
        .append("dataElementOperands", dataElementOperands)
        .append("allowedPeriodTypes", allowedPeriodTypes)
        .toString();
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addPeriod(Period p) {
    periods.add(p);
  }

  public void addIndicator(DimensionalItemObject indicator) {
    indicators.add(indicator);
  }

  public void addDataElement(DataElement de) {
    dataElements.add(de);

    dataElementIdMap.put(de.getId(), de);
  }

  public void addDataElementOperand(DataElementOperand deo) {
    dataElementOperands.add(deo);

    String deoIdKey = getDeoIds(deo.getDataElement().getId(), deo.getCategoryOptionCombo().getId());

    dataElementOperandIdMap.put(deoIdKey, deo);
  }

  public String getDeoIds(long dataElementId, long categoryOptionComboId) {
    return dataElementId + "." + categoryOptionComboId;
  }

  public void setSlidingWindows(boolean slidingWindows) {
    if (slidingWindows) {
      slidingWindowsNeeded = true;
    } else {
      nonSlidingWindowsNeeded = true;
    }
  }

  public boolean areSlidingWindowsNeeded() {
    return slidingWindowsNeeded;
  }

  public boolean areNonSlidingWindowsNeeded() {
    return nonSlidingWindowsNeeded;
  }

  // -------------------------------------------------------------------------
  // Get methods
  // -------------------------------------------------------------------------

  public PeriodType getPeriodType() {
    return periodType;
  }

  public Set<Period> getPeriods() {
    return periods;
  }

  public Set<ValidationRuleExtended> getRuleXs() {
    return ruleXs;
  }

  public Set<DimensionalItemObject> getEventItems() {
    return eventItems;
  }

  public Set<DimensionalItemObject> getEventItemsWithoutAttributeOptions() {
    return eventItemsWithoutAttributeOptions;
  }

  public Set<DimensionalItemObject> getIndicators() {
    return indicators;
  }

  public Set<DataElement> getDataElements() {
    return dataElements;
  }

  public Set<DataElementOperand> getDataElementOperands() {
    return dataElementOperands;
  }

  public Map<Long, DataElement> getDataElementIdMap() {
    return dataElementIdMap;
  }

  public Map<String, DataElementOperand> getDataElementOperandIdMap() {
    return dataElementOperandIdMap;
  }

  public Set<PeriodType> getAllowedPeriodTypes() {
    return allowedPeriodTypes;
  }

  public Set<DimensionalItemId> getLeftSideItemIds() {
    return leftSideItemIds;
  }

  public Set<DimensionalItemId> getRightSideItemIds() {
    return rightSideItemIds;
  }
}
