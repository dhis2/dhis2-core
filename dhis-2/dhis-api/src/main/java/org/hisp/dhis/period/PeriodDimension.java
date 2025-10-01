package org.hisp.dhis.period;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Objects;

@Accessors(chain = true)
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PeriodDimension extends BaseDimensionalItemObject {

  @Getter
  @Nonnull
  @EqualsAndHashCode.Include
  @JsonIgnore
  private final Period period;

  /** date field this period refers to */
  @Getter
  @Setter
  @EqualsAndHashCode.Include
  @JsonProperty
  private String dateField;

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
