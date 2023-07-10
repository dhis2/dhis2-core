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

import static org.apache.commons.lang3.StringUtils.appendIfMissing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * Item part of meta data analytics response.
 *
 * @author Lars Helge Overland
 */
@Getter
@Setter
public class MetadataItem implements Serializable {
  @JsonProperty private String uid;

  @JsonProperty private String code;

  @JsonProperty private String name;

  @JsonProperty private String description;

  @JsonProperty private String legendSet;

  @JsonProperty private DimensionType dimensionType;

  @JsonProperty private DimensionItemType dimensionItemType;

  @JsonProperty private ValueType valueType;

  @JsonProperty private AggregationType aggregationType;

  @JsonProperty private TotalAggregationType totalAggregationType;

  @JsonProperty
  @JsonSerialize(using = IndicatorTypeSerializer.class)
  private IndicatorType indicatorType;

  @JsonProperty private Date startDate;

  @JsonProperty private Date endDate;

  @JsonProperty private String expression;

  @JsonProperty private ObjectStyle style;

  private transient String serverBaseUrl;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public MetadataItem(String name) {
    this.name = name;
  }

  public MetadataItem(String name, String uid, String code) {
    this.name = name;
    this.uid = uid;
    this.code = code;
  }

  public MetadataItem(String name, DimensionalItemObject dimensionalItemObject) {
    this.name = name;
    setDataItem(dimensionalItemObject);
  }

  public MetadataItem(
      String name, String serverBaseUrl, DimensionalItemObject dimensionalItemObject) {
    this.name = name;
    this.serverBaseUrl = serverBaseUrl;
    setDataItem(dimensionalItemObject);
  }

  public MetadataItem(String name, DimensionalObject dimensionalObject) {
    this.name = name;
    setDataItem(dimensionalObject);
  }

  public MetadataItem(String name, Program program) {
    this.name = name;

    if (program == null) {
      return;
    }

    this.uid = program.getUid();
    this.code = program.getCode();
    this.description = program.getDescription();
  }

  public MetadataItem(String name, ProgramStage programStage) {
    this.name = name;

    if (programStage == null) {
      return;
    }

    this.uid = programStage.getUid();
    this.code = programStage.getCode();
    this.description = programStage.getDescription();
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  private void setDataItem(DimensionalItemObject dimensionalItemObject) {
    if (dimensionalItemObject == null) {
      return;
    }

    this.uid = dimensionalItemObject.getUid();
    this.code = dimensionalItemObject.getCode();
    this.description = dimensionalItemObject.getDescription();
    this.dimensionItemType = dimensionalItemObject.getDimensionItemType();
    this.valueType = ValueType.NUMBER; // Default value
    this.aggregationType = dimensionalItemObject.getAggregationType();
    this.totalAggregationType = dimensionalItemObject.getTotalAggregationType();

    if (dimensionalItemObject.hasLegendSet()) {
      this.legendSet = dimensionalItemObject.getLegendSet().getUid();
    }

    // TODO common interface

    if (dimensionalItemObject instanceof DataElement dataElement) {
      this.valueType = dataElement.getValueType().toSimplifiedValueType();
    } else if (dimensionalItemObject instanceof DataElementOperand operand) {
      this.valueType = operand.getValueType().toSimplifiedValueType();
    } else if (dimensionalItemObject instanceof TrackedEntityAttribute attribute) {
      this.valueType = attribute.getValueType().toSimplifiedValueType();
    } else if (dimensionalItemObject instanceof Period period) {
      this.startDate = period.getStartDate();
      this.endDate = period.getEndDate();
    } else if (dimensionalItemObject instanceof Indicator indicator) {
      if (indicator.getIndicatorType() != null) {
        this.indicatorType = HibernateProxyUtils.unproxy(indicator.getIndicatorType());
      }
    } else if (dimensionalItemObject instanceof ExpressionDimensionItem expressionDimensionItem) {
      this.expression = expressionDimensionItem.getExpression();
    }

    addIconPathToStyle(getDimensionalItemObjectStyle(dimensionalItemObject));
  }

  /**
   * Returns Object Style
   *
   * @param dimensionalItemObject the {@link DimensionalItemObject}
   * @return the {@link ObjectStyle}
   */
  private ObjectStyle getDimensionalItemObjectStyle(DimensionalItemObject dimensionalItemObject) {
    if (dimensionalItemObject
        instanceof ProgramDataElementDimensionItem programDataElementDimensionItem) {
      if (programDataElementDimensionItem.hasDataElement()) {
        return programDataElementDimensionItem.getDataElement().getStyle();
      }
    } else if (dimensionalItemObject instanceof DataDimensionItem dataDimensionItem) {
      if (dataDimensionItem.hasDataElement()) {
        return dataDimensionItem.getDataElement().getStyle();
      }

      if (dataDimensionItem.hasIndicator()) {
        return dataDimensionItem.getIndicator().getStyle();
      }

      if (dataDimensionItem.hasDataElementOperand()
          && dataDimensionItem.getDataElementOperand().getDataElement() != null) {
        return dataDimensionItem.getDataElementOperand().getDataElement().getStyle();
      }

      if (dataDimensionItem.hasProgramIndicator()) {
        return dataDimensionItem.getProgramIndicator().getStyle();
      }

      if (dataDimensionItem.hasReportingRate()
          && dataDimensionItem.getReportingRate().getDataSet() != null) {
        return dataDimensionItem.getReportingRate().getDataSet().getStyle();
      }

      if (dataDimensionItem.hasProgramTrackedEntityAttributeDimensionItem()
          && dataDimensionItem.getProgramAttribute().getAttribute() != null) {
        return dataDimensionItem.getProgramAttribute().getAttribute().getStyle();
      }
    } else if (dimensionalItemObject instanceof ReportingRate reportingRate
        && reportingRate.getDataSet() != null) {
      return reportingRate.getDataSet().getStyle();
    } else if (dimensionalItemObject instanceof DataElement dataElement) {
      return dataElement.getStyle();
    } else if (dimensionalItemObject instanceof DataSet dataSet) {
      return dataSet.getStyle();
    } else if (dimensionalItemObject instanceof TrackedEntityAttribute attribute) {
      return attribute.getStyle();
    } else if (dimensionalItemObject instanceof Indicator indicator) {
      return indicator.getStyle();
    } else if (dimensionalItemObject instanceof ProgramIndicator programIndicator) {
      return programIndicator.getStyle();
    }

    return null;
  }

  /**
   * It adds icon path to style element if style exists
   *
   * @param style the {@link ObjectStyle}
   */
  private void addIconPathToStyle(ObjectStyle style) {
    if (style == null) {
      return;
    }
    // Override icon path.
    style.setIcon(getFullIconUrl(style.getIcon()));

    this.style = style;
  }

  /**
   * It returns the full icon URL for the given icon name. The full URL is based on the Icons' API.
   * See the controller {@link org.hisp.dhis.webapi.controller.IconController} for more details.
   *
   * @param iconName the icon name.
   * @return the icon's full path.
   */
  private String getFullIconUrl(String iconName) {
    String absoluteUrl = appendIfMissing(serverBaseUrl, "/");
    return absoluteUrl + "api/icons/" + iconName + "/icon.svg";
  }

  private void setDataItem(DimensionalObject dimensionalObject) {
    if (dimensionalObject == null) {
      return;
    }

    this.uid = dimensionalObject.getUid();
    this.code = dimensionalObject.getCode();
    this.dimensionType = dimensionalObject.getDimensionType();
    this.description = dimensionalObject.getDescription();
    this.aggregationType = dimensionalObject.getAggregationType();
  }
}
