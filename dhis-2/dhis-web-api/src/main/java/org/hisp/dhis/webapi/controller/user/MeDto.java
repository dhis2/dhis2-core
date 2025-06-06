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
package org.hisp.dhis.webapi.controller.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;

@Getter
@Setter
@OpenApi.Identifiable(as = User.class)
public class MeDto {
  public MeDto(
      User user,
      JsonMap<JsonMixed> settings,
      List<String> programs,
      List<String> dataSets,
      List<ApiToken> patTokens) {
    this.id = user.getUid();
    this.username = user.getUsername();
    this.surname = user.getSurname();
    this.firstName = user.getFirstName();
    this.employer = user.getEmployer();
    this.languages = user.getLanguages();
    this.gender = user.getGender();
    this.jobTitle = user.getJobTitle();
    this.avatar = user.getAvatar();
    this.created = user.getCreated();
    this.lastUpdated = user.getLastUpdated();
    this.dataViewOrganisationUnits = user.getDataViewOrganisationUnits();
    this.favorites = user.getFavorites();
    this.userGroups = user.getGroups();
    this.translations = user.getTranslations();
    this.teiSearchOrganisationUnits = user.getTeiSearchOrganisationUnits();
    this.organisationUnits = user.getOrganisationUnits();
    this.displayName = user.getDisplayName();
    this.access = user.getAccess();
    this.name = user.getName();
    this.email = user.getEmail();
    this.emailVerified = user.isEmailVerified();
    this.twoFactorType = user.getTwoFactorType();
    this.phoneNumber = user.getPhoneNumber();
    this.introduction = user.getIntroduction();
    this.birthday = user.getBirthday();
    this.nationality = user.getNationality();
    this.education = user.getEducation();
    this.interests = user.getInterests();
    this.whatsApp = user.getWhatsApp();
    this.facebookMessenger = user.getFacebookMessenger();
    this.skype = user.getSkype();
    this.telegram = user.getTelegram();
    this.twitter = user.getTwitter();

    this.userRoles = user.getUserRoles();
    this.authorities = new ArrayList<>(user.getAllAuthorities());

    this.settings = settings;
    this.programs = programs;
    this.dataSets = dataSets;
    this.patTokens = patTokens;

    this.attributeValues = user.getAttributeValues();
  }

  @JsonProperty private String id;

  @JsonProperty private String username;

  @JsonProperty private String surname;

  @JsonProperty private String firstName;

  @JsonProperty private String employer;

  @JsonProperty private String languages;

  @JsonProperty private String gender;

  @JsonProperty private String jobTitle;

  @JsonProperty private FileResource avatar;

  @JsonProperty private Date created;

  @JsonProperty private Date lastUpdated;

  @OpenApi.Property(BaseIdentifiableObject[].class)
  @JsonProperty
  private Set<OrganisationUnit> dataViewOrganisationUnits;

  @JsonProperty protected Set<String> favorites;

  @JsonProperty protected Sharing sharing;

  @JsonProperty private Set<UserGroupAccess> userGroupAccesses;

  @JsonProperty private Set<UserAccess> userAccesses;

  @OpenApi.Property(BaseIdentifiableObject[].class)
  @JsonProperty
  private Set<UserGroup> userGroups;

  @JsonProperty private Set<Translation> translations;

  @OpenApi.Property(BaseIdentifiableObject[].class)
  @JsonProperty
  private Set<OrganisationUnit> teiSearchOrganisationUnits;

  @OpenApi.Property(BaseIdentifiableObject[].class)
  @JsonProperty
  private Set<OrganisationUnit> organisationUnits;

  @JsonProperty private Boolean externalAccess;

  @JsonProperty private String displayName;

  @JsonProperty private Access access;

  @JsonProperty private String name;

  @JsonProperty private String email;

  @JsonProperty private boolean emailVerified;

  @JsonProperty private String phoneNumber;

  @JsonProperty private String introduction;

  @JsonProperty private Date birthday;

  @JsonProperty private String nationality;

  @JsonProperty private String education;

  @JsonProperty private String interests;

  @JsonProperty private String whatsApp;

  @JsonProperty private String facebookMessenger;

  @JsonProperty private String skype;

  @JsonProperty private String telegram;

  @JsonProperty private String twitter;

  @OpenApi.Property(BaseIdentifiableObject[].class)
  @JsonProperty
  private Set<UserRole> userRoles;

  @JsonProperty private JsonMap<JsonMixed> settings;

  @JsonProperty private List<String> programs;

  @JsonProperty private List<String> authorities;

  @JsonProperty private List<String> dataSets;

  @JsonProperty private String impersonation;

  @JsonProperty private List<ApiToken> patTokens;

  @JsonProperty private TwoFactorType twoFactorType;

  @JsonProperty
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  private AttributeValues attributeValues;
}
