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
package org.hisp.dhis.webapi.webdomain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Locale;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "locale")
public class WebLocale {
  private String locale;

  private String name;

  public static WebLocale fromLocale(Locale locale) {
    WebLocale loc = new WebLocale();

    loc.setLocale(locale.toString());
    loc.setName(locale.getDisplayName());

    return loc;
  }

  public static WebLocale fromLocaleHandlingIndonesiaFormat(Locale locale) {
    WebLocale loc = new WebLocale();

    loc.setLocale(handleIndonesianLocale(locale));
    loc.setName(locale.getDisplayName());

    return loc;
  }

  /**
   * By default, for backwards compatibility reasons, Java maps Indonesian locales to the old 'in'
   * ISO format, even if we pass 'id' into a {@link Locale} constructor. See {@link
   * Locale#convertOldISOCodes(String)}. As we use our own {@link WebLocale} class, we can control
   * the codes being returned through the API. This method sets the Indonesian codes to the codes we
   * want to use in the API (which conform with the newer, universally-accepted ISO language formats
   * for Indonesia 'id'). <br>
   * <br>
   * JDK 17 does not have this issue and allows switching between both codes - see <a
   * href="https://bugs.openjdk.org/browse/JDK-8267069">JDK bug</a>. This is needed purely for DHIS2
   * versions running on a JDK below 17.
   *
   * @param locale locale loaded from resources
   * @return adjusted locale for Indonesian codes or standard for anything else
   */
  private static String handleIndonesianLocale(Locale locale) {
    switch (locale.toString()) {
      case "in":
        return "id";
      case "in_ID":
        return "id_ID";
      default:
        return locale.toString();
    }
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
