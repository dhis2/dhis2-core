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
package org.hisp.dhis.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "userGroup", namespace = DxfNamespaces.DXF_2_0)
public class UserGroup extends BaseIdentifiableObject implements MetadataObject {
  public static final String AUTH_USER_ADD = "F_USER_ADD";
  public static final String AUTH_USER_ADD_IN_GROUP = "F_USER_ADD_WITHIN_MANAGED_GROUP";

  /** Global unique identifier for UserGroup (to be used for sharing etc) */
  private UUID uuid;

  /** Set of related users */
  private Set<User> members = new HashSet<>();

  /** User groups (if any) that members of this user group can manage the members within. */
  private Set<UserGroup> managedGroups = new HashSet<>();

  /** User groups (if any) whose members can manage the members of this user group. */
  private Set<UserGroup> managedByGroups = new HashSet<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public UserGroup() {
    this.setAutoFields();
  }

  public UserGroup(String name) {
    this();
    this.name = name;
  }

  public UserGroup(String name, Set<User> members) {
    this(name);
    this.members = members;
  }

  @Override
  public void setAutoFields() {
    if (uuid == null) {
      uuid = UUID.randomUUID();
    }

    super.setAutoFields();
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addUser(User user) {
    members.add(user);
    user.getGroups().add(this);
  }

  public void removeUser(User user) {
    members.remove(user);
    user.getGroups().remove(this);
  }

  public void updateUsers(Set<User> updates) {
    new HashSet<>(members)
        .stream().filter(user -> !updates.contains(user)).forEach(this::removeUser);

    updates.forEach(this::addUser);
  }

  public void addManagedGroup(UserGroup group) {
    managedGroups.add(group);
    group.getManagedByGroups().add(this);
  }

  public void removeManagedGroup(UserGroup group) {
    managedGroups.remove(group);
    group.getManagedByGroups().remove(this);
  }

  public void updateManagedGroups(Set<UserGroup> updates) {
    new HashSet<>(managedGroups)
        .stream().filter(group -> !updates.contains(group)).forEach(this::removeManagedGroup);

    updates.forEach(this::addManagedGroup);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  @OpenApi.Property(UserPropertyTransformer.UserDto[].class)
  @JsonProperty("users")
  @JsonSerialize(contentUsing = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(contentUsing = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlElementWrapper(localName = "users", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "user", namespace = DxfNamespaces.DXF_2_0)
  public Set<User> getMembers() {
    return members;
  }

  public void setMembers(Set<User> members) {
    this.members = members;
  }

  @JsonProperty("managedGroups")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "managedGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "managedGroup", namespace = DxfNamespaces.DXF_2_0)
  public Set<UserGroup> getManagedGroups() {
    return managedGroups;
  }

  public void setManagedGroups(Set<UserGroup> managedGroups) {
    this.managedGroups = managedGroups;
  }

  @JsonProperty("managedByGroups")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "managedByGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "managedByGroup", namespace = DxfNamespaces.DXF_2_0)
  public Set<UserGroup> getManagedByGroups() {
    return managedByGroups;
  }

  public void setManagedByGroups(Set<UserGroup> managedByGroups) {
    this.managedByGroups = managedByGroups;
  }
}
