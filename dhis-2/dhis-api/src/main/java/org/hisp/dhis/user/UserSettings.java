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
package org.hisp.dhis.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * User Settings transfer object for settings as defined by {@link UserSettingKey}.
 *
 * @author Jan Bernitt
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@JacksonXmlRootElement(localName = "settings", namespace = DxfNamespaces.DXF_2_0)
public class UserSettings {
  @JsonAlias("keyStyle")
  @JsonProperty
  private String style;

  @JsonAlias("keyMessageEmailNotification")
  @JsonProperty
  private Boolean messageEmailNotification;

  @JsonAlias("keyMessageSmsNotification")
  @JsonProperty
  private Boolean messageSmsNotification;

  @JsonAlias("keyUiLocale")
  @JsonProperty
  private Locale uiLocale;

  @JsonAlias("keyDbLocale")
  @JsonProperty
  private Locale dbLocale;

  @JsonAlias("keyAnalysisDisplayProperty")
  @JsonProperty
  private DisplayProperty analysisDisplayProperty;

  @JsonAlias("keyTrackerDashboardLayout")
  @JsonProperty
  private String trackerDashboardLayout;
}
