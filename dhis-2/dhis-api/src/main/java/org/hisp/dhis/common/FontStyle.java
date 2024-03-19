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

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;

/**
 * Class representing various text styling properties.
 *
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "fontStyle", namespace = DXF_2_0)
public class FontStyle implements Serializable {
  private Font font;

  private Integer fontSize;

  private Boolean bold;

  private Boolean italic;

  private Boolean underline;

  private String textColor;

  private TextAlign textAlign;

  public FontStyle() {}

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Font getFont() {
    return font;
  }

  public void setFont(Font font) {
    this.font = font;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @PropertyRange(min = 1, max = 96)
  public Integer getFontSize() {
    return fontSize;
  }

  public void setFontSize(Integer fontSize) {
    this.fontSize = fontSize;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Boolean getBold() {
    return bold;
  }

  public void setBold(Boolean bold) {
    this.bold = bold;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Boolean getItalic() {
    return italic;
  }

  public void setItalic(Boolean italic) {
    this.italic = italic;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Boolean getUnderline() {
    return underline;
  }

  public void setUnderline(Boolean underline) {
    this.underline = underline;
  }

  /** Text color in hexadecimal notation, specified with {@code #RRGGBB}. */
  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Property(value = PropertyType.COLOR)
  public String getTextColor() {
    return textColor;
  }

  public void setTextColor(String textColor) {
    this.textColor = textColor;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public TextAlign getTextAlign() {
    return textAlign;
  }

  public void setTextAlign(TextAlign textAlign) {
    this.textAlign = textAlign;
  }
}
