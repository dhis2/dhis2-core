/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webmessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.Status;

/**
 * The core information of a standard web response message.
 *
 * <p>The main reason for this class is that it does not depend on web layer classes. Fields that
 * require these are first added by {@code WebMessage}.
 *
 * @author Jan Bernitt
 */
@Getter
@OpenApi.Kind("WebMessageResponse")
public class WebResponse {

  /**
   * Message status, currently two statuses are available: OK, ERROR. Default value is OK.
   *
   * @see Status
   */
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  protected Status status = Status.OK;

  /**
   * Internal code for this message. Should be used to help with third party clients which should
   * not have to resort to string parsing of message to know what is happening.
   */
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  protected Integer code;

  /**
   * The {@link ErrorCode} which describes a potential error. Only relevant for {@link
   * Status#ERROR}.
   */
  @JacksonXmlProperty(isAttribute = true)
  @JsonProperty
  protected ErrorCode errorCode;

  /**
   * Non-technical message, should be simple and could possibly be used to display message to an
   * end-user.
   */
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @JsonProperty
  protected String message;
}
