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
package org.hisp.dhis.sms.outbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;

@JacksonXmlRootElement(localName = "outboundsms")
public class OutboundSms extends BaseIdentifiableObject {
  public static final String DHIS_SYSTEM_SENDER = "DHIS-System";

  private String sender;

  private Set<String> recipients;

  private Date date;

  private String subject;

  private String message;

  private OutboundSmsStatus status = OutboundSmsStatus.OUTBOUND;

  public OutboundSms() {
    setAutoFields();
  }

  public OutboundSms(String subject, String message, Set<String> recipients) {
    this.subject = subject;
    this.message = message;
    this.recipients = recipients;
  }

  @JsonProperty(value = "recipients")
  @JacksonXmlProperty(localName = "recipients")
  public Set<String> getRecipients() {
    return recipients;
  }

  public void setRecipients(Set<String> recipients) {
    this.recipients = recipients;
  }

  @JsonProperty(value = "date")
  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @JsonProperty(value = "message")
  @JacksonXmlProperty(localName = "message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @JsonProperty(value = "sender")
  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  @JsonProperty(value = "status")
  public OutboundSmsStatus getStatus() {
    return status;
  }

  public void setStatus(OutboundSmsStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "OutboundSMS [recipients=" + getNumbers() + ", message=" + message + "]";
  }

  private String getNumbers() {
    if (this.recipients == null) {
      return null;
    }

    String numbers = "";

    for (String recipient : this.recipients) {
      numbers += recipient + ", ";
    }

    return numbers.substring(0, numbers.length() - 2);
  }

  @JsonProperty(value = "subject")
  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }
}
