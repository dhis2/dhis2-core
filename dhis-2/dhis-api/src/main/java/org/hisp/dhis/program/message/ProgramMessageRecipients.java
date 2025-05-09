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
package org.hisp.dhis.program.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@JacksonXmlRootElement(localName = "programMessageRecipients", namespace = DxfNamespaces.DXF_2_0)
public class ProgramMessageRecipients implements Serializable {
  private static final long serialVersionUID = 1141462154959329242L;

  private TrackedEntity trackedEntity;

  private OrganisationUnit organisationUnit;

  private Set<String> phoneNumbers = new HashSet<>();

  private Set<String> emailAddresses = new HashSet<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramMessageRecipients() {}

  public ProgramMessageRecipients(Set<String> phoneNumbers, Set<String> emailAddresses) {
    this.phoneNumbers = phoneNumbers;
    this.emailAddresses = emailAddresses;
  }

  public ProgramMessageRecipients(
      TrackedEntity trackedEntity,
      OrganisationUnit organisationUnit,
      Set<String> phoneNumbers,
      Set<String> emailAddresses) {
    this.trackedEntity = trackedEntity;
    this.organisationUnit = organisationUnit;
    this.phoneNumbers = phoneNumbers;
    this.emailAddresses = emailAddresses;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasTrackedEntity() {
    return trackedEntity != null;
  }

  public boolean hasOrganisationUnit() {
    return organisationUnit != null;
  }

  // -------------------------------------------------------------------------
  // Setters and getters
  // -------------------------------------------------------------------------

  @JsonProperty(value = "trackedEntity")
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "trackedEntity")
  public TrackedEntity getTrackedEntity() {
    return trackedEntity;
  }

  public void setTrackedEntity(TrackedEntity trackedEntity) {
    this.trackedEntity = trackedEntity;
  }

  @JsonProperty(value = "organisationUnit")
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "organisationUnit")
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  public void setOrganisationUnit(OrganisationUnit organisationUnit) {
    this.organisationUnit = organisationUnit;
  }

  @JsonProperty(value = "phoneNumbers")
  @JacksonXmlProperty(localName = "phoneNumbers")
  public Set<String> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(Set<String> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  @JsonProperty(value = "emailAddresses")
  @JacksonXmlProperty(localName = "emailAddresses")
  public Set<String> getEmailAddresses() {
    return emailAddresses;
  }

  public void setEmailAddresses(Set<String> emailAddress) {
    this.emailAddresses = emailAddress;
  }

  @Override
  public String toString() {
    return "ProgramMessageRecipients[ "
        + (phoneNumbers != null
            ? " " + phoneNumbers + " "
            : " " + (emailAddresses != null ? " " + emailAddresses + " " : ""))
        + " ]";
  }
}
