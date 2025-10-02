/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.period;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

@Getter
@Accessors(chain = true)
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PeriodDimension extends BaseDimensionalItemObject {

  @Nonnull @EqualsAndHashCode.Include @JsonIgnore private final Period period;

  /** date field this period refers to */
  @Setter @EqualsAndHashCode.Include @JsonProperty private String dateField;

  /**
   * Creates a period that is not bound to the persistent layer. It represents a detached Period
   * that is mainly used for displaying purposes.
   *
   * @param isoRelativePeriod the ISO relative period
   */
  public PeriodDimension(RelativePeriodEnum isoRelativePeriod) {
    this.period = PeriodType.getPeriodFromIsoString(isoRelativePeriod.toString());
    this.name = isoRelativePeriod.toString();
    this.code = isoRelativePeriod.toString();
  }

  @JsonIgnore
  public String getIsoDate() {
    return period.getIsoDate();
  }

  @JsonProperty
  public Date getStartDate() {
    return period.getStartDate();
  }

  @JsonProperty
  public Date getEndDate() {
    return period.getEndDate();
  }

  @JsonProperty
  @JsonSerialize(using = JacksonPeriodTypeSerializer.class)
  @JsonDeserialize(using = JacksonPeriodTypeDeserializer.class)
  @Property(PropertyType.TEXT)
  public PeriodType getPeriodType() {
    return period.getPeriodType();
  }

  public String getStartDateString() {
    return period.getStartDateString();
  }

  public String getEndDateString() {
    return period.getEndDateString();
  }

  @Override
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.PERIOD;
  }

  @Override
  public void setAutoFields() {}

  @Override
  public String getDimensionItem() {
    return getIsoDate();
  }

  @Override
  @JsonProperty("id")
  public String getUid() {
    return uid != null ? uid : getIsoDate();
  }

  @Override
  public String getCode() {
    return getIsoDate();
  }

  @Override
  public String getName() {
    return name != null ? name : getIsoDate();
  }

  @Override
  public String getShortName() {
    return shortName != null ? shortName : getIsoDate();
  }

  public boolean isDefault() {
    return Objects.isNull(dateField);
  }

  @Override
  public String toString() {
    return getIsoDate() + (isDefault() ? "" : ":" + dateField);
  }
}
