/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Simple {
  private String string;

  private Integer integer;

  private Date date;

  private Boolean bool;

  private int anInt;

  private float aFloat;

  public Simple() {}

  public Simple(String string, Integer integer, Date date, Boolean bool, int anInt, float aFloat) {
    this.string = string;
    this.integer = integer;
    this.date = date;
    this.bool = bool;
    this.anInt = anInt;
    this.aFloat = aFloat;
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getString() {
    return string;
  }

  public void setString(String string) {
    this.string = string;
  }

  @JsonProperty
  @JacksonXmlProperty
  public Integer getInteger() {
    return integer;
  }

  public void setInteger(Integer integer) {
    this.integer = integer;
  }

  @JsonProperty
  @JacksonXmlProperty
  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @JsonProperty
  @JacksonXmlProperty
  public Boolean getBool() {
    return bool;
  }

  public void setBool(Boolean bool) {
    this.bool = bool;
  }

  @JsonProperty
  @JacksonXmlProperty
  public int getAnInt() {
    return anInt;
  }

  public void setAnInt(int anInt) {
    this.anInt = anInt;
  }

  @JsonProperty
  @JacksonXmlProperty
  public float getaFloat() {
    return aFloat;
  }

  public void setaFloat(float aFloat) {
    this.aFloat = aFloat;
  }
}
