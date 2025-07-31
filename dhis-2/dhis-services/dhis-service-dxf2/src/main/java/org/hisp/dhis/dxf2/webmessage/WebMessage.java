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
package org.hisp.dhis.dxf2.webmessage;

import javax.annotation.Nonnull;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.common.ImportTypeSummary;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportReport;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.scheduling.JobConfigurationWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ApiTokenCreationResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ErrorReportsWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ImportCountWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ImportReportWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.MergeWebResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ObjectReportWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.SoftwareUpdateResponse;
import org.hisp.dhis.dxf2.webmessage.responses.TrackerJobWebMessageResponse;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.predictor.PredictionSummary;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.hisp.dhis.webmessage.WebResponse;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import lombok.Getter;

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
public class WebMessage extends WebResponse {

  /** HTTP status. */
  private HttpStatus httpStatus = HttpStatus.OK;

  /**
   * Technical message that should explain as much details as possible, mainly to be used for
   * debugging.
   */
  private String devMessage;

  /**
   * When a simple text feedback is not enough, you can use this interface to implement your own
   * response payload object.
   */
  @OpenApi.Property({
    ApiTokenCreationResponse.class,
    ErrorReportsWebMessageResponse.class,
    FileResourceWebMessageResponse.class,
    GeoJsonImportReport.class,
    ImportCountWebMessageResponse.class,
    ImportReportWebMessageResponse.class,
    ImportSummaries.class,
    ImportSummary.class,
    ImportTypeSummary.class,
    JobConfigurationWebMessageResponse.class,
    MergeWebResponse.class,
    MetadataSyncSummary.class,
    ObjectReportWebMessageResponse.class,
    PredictionSummary.class,
    SoftwareUpdateResponse.class,
    TrackerJobWebMessageResponse.class,
    TrackerJobWebMessageResponse.class
  })
  private WebMessageResponse response;

  @Getter private String location;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /** Constructor. Only for deserialisation. */
  public WebMessage() {}

  /**
   * Constructor.
   * 
   * @param status the {@link Status}.
   * @param httpStatus the {@link HttpStatus}.
   */
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

  public WebMessage setStatus(Status status) {
    this.status = status;
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

  public WebMessage setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
    return this;
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

  public WebMessage setLocation(String location) {
    this.location = location;
    return this;
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
