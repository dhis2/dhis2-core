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

import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT_OPERAND;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * Holds the DimensionItemType of a DimensionalItemObject, and the identifier strings of the objects
 * that that make up the DimensionalItemObject
 *
 * @author Jim Grace
 */
public class DimensionalItemId {
  // -------------------------------------------------------------------------
  // Properties
  // -------------------------------------------------------------------------

  /** The type of DimensionalItemObject whose ids we have */
  private final DimensionItemType dimensionItemType;

  /** The first id for the DimensionalItemObject */
  private final String id0;

  /** The second id (if any) for the DimensionalItemObject */
  private String id1;

  /** The third id (if any) for the DimensionalItemObject */
  private String id2;

  /** The item as parsed from the expression */
  private String item;

  /** The query modifiers */
  private QueryModifiers queryMods;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DimensionalItemId(DimensionItemType dimensionItemType, String id0) {
    this.dimensionItemType = dimensionItemType;
    this.id0 = id0;
  }

  public DimensionalItemId(
      DimensionItemType dimensionItemType, String id0, QueryModifiers queryMods) {
    this.dimensionItemType = dimensionItemType;
    this.id0 = id0;
    this.queryMods = queryMods;
  }

  public DimensionalItemId(DimensionItemType dimensionItemType, String id0, String id1) {
    this.dimensionItemType = dimensionItemType;
    this.id0 = id0;
    this.id1 = id1;
  }

  public DimensionalItemId(
      DimensionItemType dimensionItemType, String id0, String id1, String id2) {
    this.dimensionItemType = dimensionItemType;
    this.id0 = id0;
    this.id1 = id1;
    this.id2 = id2;
  }

  public DimensionalItemId(
      DimensionItemType dimensionItemType, String id0, String id1, String id2, String item) {
    this.dimensionItemType = dimensionItemType;
    this.id0 = id0;
    this.id1 = id1;
    this.id2 = id2;
    this.item = item;
  }

  public DimensionalItemId(
      DimensionItemType dimensionItemType,
      String id0,
      String id1,
      String id2,
      String item,
      QueryModifiers queryMods) {
    this.dimensionItemType = dimensionItemType;
    this.id0 = id0;
    this.id1 = id1;
    this.id2 = id2;
    this.item = item;
    this.queryMods = queryMods;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasValidIds() {
    switch (dimensionItemType) {
      case DATA_ELEMENT:
      case INDICATOR:
      case PROGRAM_INDICATOR:
        return id0 != null && id1 == null && id2 == null;

      case DATA_ELEMENT_OPERAND:
        return id0 != null && (id1 != null || id2 != null);

      case REPORTING_RATE:
        return id0 != null
            && id1 != null
            && id2 == null
            && isValidEnum(ReportingRateMetric.class, id1);

      case PROGRAM_DATA_ELEMENT:
      case PROGRAM_ATTRIBUTE:
        return id0 != null && id1 != null && id2 == null;

      default:
        return false;
    }
  }

  public boolean isDataElementOrOperand() {
    return dimensionItemType == DATA_ELEMENT || dimensionItemType == DATA_ELEMENT_OPERAND;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DimensionalItemId that = (DimensionalItemId) o;

    return Objects.equals(this.dimensionItemType, that.dimensionItemType)
        && Objects.equals(this.id0, that.id0)
        && Objects.equals(this.id1, that.id1)
        && Objects.equals(this.id2, that.id2)
        && Objects.equals(this.queryMods, that.queryMods);
  }

  @Override
  public int hashCode() {
    int result = dimensionItemType.hashCode();

    result = 31 * result + (id0 == null ? 0 : id0.hashCode());
    result = 31 * result + (id1 == null ? 0 : id1.hashCode());
    result = 31 * result + (id2 == null ? 0 : id2.hashCode());
    result = 31 * result + (queryMods == null ? 0 : queryMods.hashCode());

    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("dimensionItemType", dimensionItemType)
        .add("id0", id0)
        .add("id1", id1)
        .add("id2", id2)
        .add("queryMods", queryMods)
        .toString();
  }

  // -------------------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------------------

  public DimensionItemType getDimensionItemType() {
    return dimensionItemType;
  }

  public String getId0() {
    return id0;
  }

  public String getId1() {
    return id1;
  }

  public String getId2() {
    return id2;
  }

  public QueryModifiers getQueryMods() {
    return queryMods;
  }

  public String getItem() {
    return item;
  }
}
