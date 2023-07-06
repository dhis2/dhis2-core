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
package org.hisp.dhis.common;

/**
 * @author Lars Helge Overland
 */
public abstract class AggregatedValue {
  public static final Double ZERO = 0d;

  // ----------------------------------------------------------------------
  // Properties
  // ----------------------------------------------------------------------

  protected long periodId;

  protected long periodTypeId;

  protected long organisationUnitId;

  protected long organisationUnitGroupId;

  protected int level;

  protected double value;

  protected transient String periodName;

  protected transient String organisationUnitName;

  protected transient double trendValue;

  // ----------------------------------------------------------------------
  // Abstract methods
  // ----------------------------------------------------------------------

  public abstract long getElementId();

  // ----------------------------------------------------------------------
  // Getters and setters
  // ----------------------------------------------------------------------

  public long getPeriodId() {
    return periodId;
  }

  public void setPeriodId(long periodId) {
    this.periodId = periodId;
  }

  public long getPeriodTypeId() {
    return periodTypeId;
  }

  public void setPeriodTypeId(long periodTypeId) {
    this.periodTypeId = periodTypeId;
  }

  public long getOrganisationUnitId() {
    return organisationUnitId;
  }

  public void setOrganisationUnitId(long organisationUnitId) {
    this.organisationUnitId = organisationUnitId;
  }

  public long getOrganisationUnitGroupId() {
    return organisationUnitGroupId;
  }

  public void setOrganisationUnitGroupId(long organisationUnitGroupId) {
    this.organisationUnitGroupId = organisationUnitGroupId;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public String getPeriodName() {
    return periodName;
  }

  public void setPeriodName(String periodName) {
    this.periodName = periodName;
  }

  public String getOrganisationUnitName() {
    return organisationUnitName;
  }

  public void setOrganisationUnitName(String organisationUnitName) {
    this.organisationUnitName = organisationUnitName;
  }

  public double getTrendValue() {
    return trendValue;
  }

  public void setTrendValue(double trendValue) {
    this.trendValue = trendValue;
  }
}
