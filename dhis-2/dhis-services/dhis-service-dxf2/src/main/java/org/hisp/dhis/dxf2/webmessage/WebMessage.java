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
package org.hisp.dhis.dxf2.webmessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.springframework.http.HttpStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement(localName = "webMessage", namespace = DxfNamespaces.DXF_2_0)
@JsonPropertyOrder({
  "httpStatus",
  "httpStatusCode",
  "status",
  "code",
  "message",
  "devMessage",
  "response"
})
public class WebMessage implements WebMessageResponse {
  /**
   * Message status, currently two statuses are available: OK, ERROR. Default value is OK.
   *
   * @see Status
   */
  private Status status = Status.OK;

  /**
   * Internal code for this message. Should be used to help with third party clients which should
   * not have to resort to string parsing of message to know what is happening.
   */
  private Integer code;

  /** HTTP status. */
  private HttpStatus httpStatus = HttpStatus.OK;

  /**
   * The {@link ErrorCode} which describes a potential error. Only relevant for {@link
   * Status#ERROR}.
   */
  private ErrorCode errorCode;

  /**
   * Non-technical message, should be simple and could possibly be used to display message to an
   * end-user.
   */
  private String message;

  /**
   * Technical message that should explain as much details as possible, mainly to be used for
   * debugging.
   */
  private String devMessage;

  /**
   * When a simple text feedback is not enough, you can use this interface to implement your own
   * message responses.
   *
   * @see WebMessageResponse
   */
  private WebMessageResponse response;

  private DhisApiVersion plainResponseBefore;

  private String location;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /** Only for deserialisation */
  public WebMessage() {}

  public WebMessage(Status status, HttpStatus httpStatus) {
    this.status = status;
    this.httpStatus = httpStatus;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean isOk() {
    return Status.OK == status;
  }

  public boolean isWarning() {
    return Status.WARNING == status;
  }

  public boolean isError() {
    return Status.ERROR == status;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public Status getStatus() {
    return status;
  }

  public WebMessage setStatus(Status status) {
    this.status = status;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public Integer getCode() {
    return code;
  }

  public WebMessage setCode(Integer code) {
    this.code = code;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getHttpStatus() {
    return httpStatus.getReasonPhrase();
  }

  public WebMessage setHttpStatus(HttpStatus httpStatus) {
    this.httpStatus = httpStatus;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Nonnull
  public Integer getHttpStatusCode() {
    return httpStatus.value();
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public WebMessage setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getMessage() {
    return message;
  }

  public WebMessage setMessage(String message) {
    this.message = message;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDevMessage() {
    return devMessage;
  }

  public WebMessage setDevMessage(String devMessage) {
    this.devMessage = devMessage;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public WebMessageResponse getResponse() {
    return response;
  }

  public WebMessage setResponse(WebMessageResponse response) {
    this.response = response;
    return this;
  }

  public String getLocation() {
    return location;
  }

  public WebMessage setLocation(String location) {
    this.location = location;
    return this;
  }

  public WebMessage withPlainResponseBefore(DhisApiVersion version) {
    this.plainResponseBefore = version;
    return this;
  }

  @JsonIgnore
  public DhisApiVersion getPlainResponseBefore() {
    return plainResponseBefore;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("status", status)
        .add("code", code)
        .add("httpStatus", httpStatus)
        .add("message", message)
        .add("devMessage", devMessage)
        .add("response", response)
        .toString();
  }
}
