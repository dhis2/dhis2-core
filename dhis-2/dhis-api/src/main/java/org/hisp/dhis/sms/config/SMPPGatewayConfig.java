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
package org.hisp.dhis.sms.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import org.hisp.dhis.sms.config.views.SmsConfigurationViews;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;

/**
 * @author Zubair Asghar
 */
@JsonTypeName("smpp")
public class SMPPGatewayConfig extends SmsGatewayConfig {
  @JsonView(SmsConfigurationViews.Public.class)
  private String systemType;

  @JsonView(SmsConfigurationViews.Public.class)
  private NumberingPlanIndicator numberPlanIndicator = NumberingPlanIndicator.UNKNOWN;

  @JsonView(SmsConfigurationViews.Public.class)
  private TypeOfNumber typeOfNumber = TypeOfNumber.UNKNOWN;

  @JsonView(SmsConfigurationViews.Public.class)
  private BindType bindType = BindType.BIND_TX;

  @JsonView(SmsConfigurationViews.Public.class)
  private int port;

  @JsonView(SmsConfigurationViews.Public.class)
  private boolean compressed;

  @Override
  @JsonProperty(value = "host")
  public String getUrlTemplate() {
    return super.getUrlTemplate();
  }

  @Override
  @JsonProperty(value = "systemId")
  public String getUsername() {
    return super.getUsername();
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getSystemType() {
    return systemType;
  }

  public void setSystemType(String systemType) {
    this.systemType = systemType;
  }

  public NumberingPlanIndicator getNumberPlanIndicator() {
    return numberPlanIndicator;
  }

  public void setNumberPlanIndicator(NumberingPlanIndicator numberPlanIndicator) {
    this.numberPlanIndicator = numberPlanIndicator;
  }

  public TypeOfNumber getTypeOfNumber() {
    return typeOfNumber;
  }

  public void setTypeOfNumber(TypeOfNumber typeOfNumber) {
    this.typeOfNumber = typeOfNumber;
  }

  public BindType getBindType() {
    return bindType;
  }

  public void setBindType(BindType bindType) {
    this.bindType = bindType;
  }

  public boolean isCompressed() {
    return compressed;
  }

  public void setCompressed(boolean compressed) {
    this.compressed = compressed;
  }
}
