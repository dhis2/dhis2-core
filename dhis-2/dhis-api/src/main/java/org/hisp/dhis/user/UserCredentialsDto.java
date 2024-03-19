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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.user.sharing.Sharing;

@Data
@JacksonXmlRootElement(localName = "userCredentialsDto", namespace = DxfNamespaces.DXF_2_0)
public class UserCredentialsDto {
  @JsonProperty private String uid;

  @JsonProperty private String id;

  @JsonProperty private String uuid;

  @JsonProperty private String username;

  @JsonProperty private boolean externalAuth;

  @JsonProperty private String openId;

  @JsonProperty private String ldapId;

  @JsonProperty private String password;

  @JsonProperty private boolean twoFA;

  @JsonProperty private Date passwordLastUpdated;

  @JsonProperty private Set<CategoryOptionGroupSet> cogsDimensionConstraints = new HashSet<>();

  @JsonProperty private Set<Category> catDimensionConstraints = new HashSet<>();

  @JsonProperty private List<String> previousPasswords = new ArrayList<>();

  @JsonProperty private Date lastLogin;

  @JsonProperty private String restoreToken;

  @JsonProperty private String idToken;

  @JsonProperty private Date restoreExpiry;

  @JsonProperty private boolean selfRegistered;

  @JsonProperty private boolean invitation;

  @JsonProperty private boolean disabled;

  @JsonProperty private Date accountExpiry;

  @JsonProperty private Access access;

  @JsonProperty private Sharing sharing = new Sharing();

  @JsonProperty private Set<UserRole> userRoles;

  @JsonSetter(nulls = Nulls.SET)
  public void setUserRoles(Set<UserRole> userRoles) {
    this.userRoles = userRoles;
  }
}
