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
package org.hisp.dhis.visualization;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import org.hisp.dhis.common.FontStyle;

/**
 * Class representing text styling properties for various components of a visualization.
 *
 * @author Lars Helge Overland
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "visualizationFontStyle", namespace = DXF_2_0)
public class VisualizationFontStyle implements Serializable {
  private FontStyle visualizationTitle;

  private FontStyle visualizationSubtitle;

  private transient FontStyle horizontalAxisTitle;

  private transient FontStyle verticalAxisTitle;

  private transient FontStyle targetLineLabel;

  private transient FontStyle baseLineLabel;

  private transient FontStyle seriesAxisLabel;

  private transient FontStyle categoryAxisLabel;

  private transient FontStyle legend;

  public VisualizationFontStyle() {}

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getVisualizationTitle() {
    return visualizationTitle;
  }

  public void setVisualizationTitle(FontStyle visualizationTitle) {
    this.visualizationTitle = visualizationTitle;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getVisualizationSubtitle() {
    return visualizationSubtitle;
  }

  public void setVisualizationSubtitle(FontStyle visualizationSubtitle) {
    this.visualizationSubtitle = visualizationSubtitle;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getHorizontalAxisTitle() {
    return horizontalAxisTitle;
  }

  public void setHorizontalAxisTitle(FontStyle horizontalAxisTitle) {
    this.horizontalAxisTitle = horizontalAxisTitle;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getVerticalAxisTitle() {
    return verticalAxisTitle;
  }

  public void setVerticalAxisTitle(FontStyle verticalAxisTitle) {
    this.verticalAxisTitle = verticalAxisTitle;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getTargetLineLabel() {
    return targetLineLabel;
  }

  public void setTargetLineLabel(FontStyle targetLineLabel) {
    this.targetLineLabel = targetLineLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getBaseLineLabel() {
    return baseLineLabel;
  }

  public void setBaseLineLabel(FontStyle baseLineLabel) {
    this.baseLineLabel = baseLineLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getSeriesAxisLabel() {
    return seriesAxisLabel;
  }

  public void setSeriesAxisLabel(FontStyle seriesAxisLabel) {
    this.seriesAxisLabel = seriesAxisLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getCategoryAxisLabel() {
    return categoryAxisLabel;
  }

  public void setCategoryAxisLabel(FontStyle categoryAxisLabel) {
    this.categoryAxisLabel = categoryAxisLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public FontStyle getLegend() {
    return legend;
  }

  public void setLegend(FontStyle legend) {
    this.legend = legend;
  }
}
