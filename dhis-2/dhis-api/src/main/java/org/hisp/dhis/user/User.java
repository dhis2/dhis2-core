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

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Nguyen Hong Duc
 */
@JacksonXmlRootElement(localName = "user", namespace = DxfNamespaces.DXF_2_0)
public class User extends BaseIdentifiableObject implements MetadataObject {

  /** Globally unique identifier for User. */
  private UUID uuid;

  /** Required and unique. */
  private String username;

  /**
   * Indicates whether this user can only be authenticated externally, such as through OpenID or
   * LDAP.
   */
  private boolean externalAuth;

  /** Unique OpenID. */
  private String openId;

  /** Unique LDAP distinguished name. */
  private String ldapId;

  /** Required. Will be stored as a hash. */
  private String password;

  private String secret;

  private TwoFactorType twoFactorType;

  /** Date when password was changed. */
  private Date passwordLastUpdated;

  /** Set of user roles. */
  private Set<UserRole> userRoles = new HashSet<>();

  /** Category option group set dimensions to constrain data analytics aggregation. */
  private Set<CategoryOptionGroupSet> cogsDimensionConstraints = new HashSet<>();

  /** Category dimensions to constrain data analytics aggregation. */
  private Set<Category> catDimensionConstraints = new HashSet<>();

  /** List of previously used passwords. */
  private List<String> previousPasswords = new ArrayList<>();

  /** Date of the user's last login. */
  private Date lastLogin;

  /** The token used for a user account restore. Will be stored as a hash. */
  private String restoreToken;

  /** The token used for a user lookup when sending restore and invite emails. */
  private String idToken;

  /** The timestamp representing when the restore window expires. */
  private Date restoreExpiry;

  /** Indicates whether this user was originally self registered. */
  private boolean selfRegistered;

  /** Indicates whether this user is currently an invitation. */
  private boolean invitation;

  /** Indicates whether this is user is disabled, which means the user cannot be authenticated. */
  private boolean disabled;

  private boolean isCredentialsNonExpired;

  private boolean isAccountNonLocked;

  /**
   * The timestamp representing when the user account expires. If not set the account does never
   * expire.
   */
  private Date accountExpiry;

  private String surname;

  private String firstName;

  private String email;

  private String phoneNumber;

  private String jobTitle;

  private String introduction;

  private String gender;

  private Date birthday;

  private String nationality;

  private String employer;

  private String education;

  private String interests;

  private String languages;

  private String welcomeMessage;

  private Date lastCheckedInterpretations;

  private Set<UserGroup> groups = new HashSet<>();

  private String whatsApp;

  private String facebookMessenger;

  private String skype;

  private String telegram;

  private String twitter;

  private FileResource avatar;

  /** Organisation units for data input and data capture operations. */
  private Set<OrganisationUnit> organisationUnits = new HashSet<>();

  /** Organisation units for data output and data analysis operations. */
  private Set<OrganisationUnit> dataViewOrganisationUnits = new HashSet<>();

  /** Organisation units for tracked entity search operations. */
  private Set<OrganisationUnit> teiSearchOrganisationUnits = new HashSet<>();

  /** Max organisation unit level for data output and data analysis operations, may be null. */
  private Integer dataViewMaxOrganisationUnitLevel;

  /** Ordered favorite apps. */
  private List<String> apps = new ArrayList<>();

  /**
   * OBS! This field will only be set when de-serialising a user with settings so the settings can
   * be updated/stored.
   *
   * <p>It is not initialised when loading a user from the database.
   */
  private transient Map<String, String> settings;

  /** User's verified email. */
  private String verifiedEmail;

  /** User's email verification token. */
  private String emailVerificationToken;

  public User() {
    this.lastLogin = null;
    this.passwordLastUpdated = new Date();
    if (uuid == null) {
      uuid = UUID.randomUUID();
    }
  }

  /**
   * Returns a concatenated String of the display names of all user authority groups for this user.
   */
  public String getUserRoleNames() {
    return IdentifiableObjectUtils.join(userRoles);
  }

  /** Returns a set of the aggregated authorities for all user authority groups of this user. */
  public Set<String> getAllAuthorities() {
    return userRoles == null
        ? Set.of()
        : userRoles.stream()
            .flatMap(role -> emptyIfNull(role.getAuthorities()).stream())
            .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * "Get all the restrictions from all the user roles."
   *
   * @return A set of all the restrictions for all the user roles.
   */
  public Set<String> getAllRestrictions() {
    return userRoles == null
        ? Set.of()
        : userRoles.stream()
            .flatMap(role -> emptyIfNull(role.getRestrictions()).stream())
            .collect(Collectors.toUnmodifiableSet());
  }

  /** Indicates whether this user has at least one authority through its user authority groups. */
  public boolean hasAuthorities() {
    return userRoles != null
        && userRoles.stream().anyMatch(role -> role != null && !role.getAuthorities().isEmpty());
  }

  /**
   * Tests whether this user has any of the authorities in the given set.
   *
   * @param auths the authorities to compare with.
   * @return true or false.
   */
  public boolean hasAnyAuthority(Collection<String> auths) {
    return getAllAuthorities().stream().anyMatch(auths::contains);
  }

  /**
   * Tests whether this user has any of the {@link Authorities} in the given set.
   *
   * @param auths the {@link Authorities} to compare with.
   * @return true or false.
   */
  public boolean hasAnyAuth(@Nonnull Collection<Authorities> auths) {
    return hasAnyAuthority(auths.stream().map(Authorities::toString).toList());
  }

  /**
   * "Return true if any of the restrictions in the collection are in the list of all restrictions."
   *
   * @param restrictions A collection of strings that represent the restrictions that are being
   *     checked for.
   * @return A boolean value.
   */
  public boolean hasAnyRestrictions(Collection<String> restrictions) {
    return getAllRestrictions().stream().anyMatch(restrictions::contains);
  }

  /**
   * Tests whether the user has the given authority. Returns true in any case if the user has the
   * ALL authority.
   */
  public boolean isAuthorized(String auth) {
    if (auth == null) {
      return false;
    }

    final Set<String> auths = getAllAuthorities();

    return auths.contains(Authorities.ALL.toString()) || auths.contains(auth);
  }

  /**
   * Indicates whether this user is a super user, implying that the ALL authority is present in at
   * least one of the user authority groups of this user.
   */
  public boolean isSuper() {
    return userRoles.stream().anyMatch(UserRole::isSuper);
  }

  /**
   * Indicates whether this user can issue the given user authority group. First the given authority
   * group must not be null. Second this user must not contain the given authority group. Third the
   * authority group must be a subset of the aggregated user authorities of this user, or this user
   * must have the ALL authority.
   *
   * @param group the user authority group.
   * @param canGrantOwnUserRole indicates whether this users can grant its own authority groups to
   *     others.
   */
  public boolean canIssueUserRole(UserRole group, boolean canGrantOwnUserRole) {
    if (group == null) {
      return false;
    }

    final Set<String> authorities = getAllAuthorities();

    if (authorities.contains(Authorities.ALL.toString())) {
      return true;
    }

    if (!canGrantOwnUserRole && userRoles.contains(group)) {
      return false;
    }

    return authorities.containsAll(group.getAuthorities());
  }

  /**
   * Indicates whether this user can issue all of the user authority groups in the given collection.
   *
   * @param groups the collection of user authority groups.
   * @param canGrantOwnUserRole indicates whether this users can grant its own authority groups to
   *     others.
   */
  public boolean canIssueUserRoles(Collection<UserRole> groups, boolean canGrantOwnUserRole) {
    for (UserRole group : groups) {
      if (!canIssueUserRole(group, canGrantOwnUserRole)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Indicates whether this user can modify the given user. This user must have the ALL authority or
   * possess all user authorities of the other user to do so.
   *
   * @param other the user to modify.
   */
  public boolean canModifyUser(User other) {
    if (other == null) {
      return false;
    }

    final Set<String> authorities = getAllAuthorities();

    if (authorities.contains(Authorities.ALL.toString())) {
      return true;
    }

    return authorities.containsAll(other.getAllAuthorities());
  }

  /** Sets the last login property to the current date. */
  public void updateLastLogin() {
    this.lastLogin = new Date();
  }

  /** Returns the dimensions to use as constrains (filters) in data analytics aggregation. */
  public Set<DimensionalObject> getDimensionConstraints() {
    Set<DimensionalObject> constraints = new HashSet<>();

    for (CategoryOptionGroupSet cogs : cogsDimensionConstraints) {
      cogs.setDimensionType(DimensionType.CATEGORY_OPTION_GROUP_SET);
      constraints.add(cogs);
    }

    for (Category cat : catDimensionConstraints) {
      constraints.add(cat);
    }

    return constraints;
  }

  /** Indicates whether this user has user authority groups. */
  public boolean hasUserRoles() {
    return userRoles != null && !userRoles.isEmpty();
  }

  /** Indicates whether this user has dimension constraints. */
  public boolean hasDimensionConstraints() {
    Set<DimensionalObject> constraints = getDimensionConstraints();
    return constraints != null && !constraints.isEmpty();
  }

  /** Indicates whether an LDAP identifier is set. */
  public boolean hasLdapId() {
    return ldapId != null && !ldapId.isEmpty();
  }

  /** Indicates whether a password is set. */
  public boolean hasPassword() {
    return password != null;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(value = PropertyType.PASSWORD, access = Property.Access.WRITE_ONLY)
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @JsonIgnore
  public boolean isTwoFactorEnabled() {
    return this.twoFactorType != null && this.twoFactorType.isEnabled();
  }

  @JsonIgnore
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  @JsonIgnore
  public TwoFactorType getTwoFactorType() {
    return this.twoFactorType == null ? TwoFactorType.NOT_ENABLED : this.twoFactorType;
  }

  public void setTwoFactorType(TwoFactorType twoFactorType) {
    this.twoFactorType = twoFactorType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isExternalAuth() {
    return externalAuth;
  }

  public void setExternalAuth(boolean externalAuth) {
    this.externalAuth = externalAuth;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getPasswordLastUpdated() {
    return passwordLastUpdated;
  }

  public void setPasswordLastUpdated(Date passwordLastUpdated) {
    this.passwordLastUpdated = passwordLastUpdated;
  }

  @JsonProperty("userRoles")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "userRoles", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "userRole", namespace = DxfNamespaces.DXF_2_0)
  public Set<UserRole> getUserRoles() {
    return userRoles;
  }

  public void setUserRoles(Set<UserRole> userRoles) {
    this.userRoles = userRoles;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(
      localName = "catDimensionConstraints",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "catDimensionConstraint", namespace = DxfNamespaces.DXF_2_0)
  public Set<Category> getCatDimensionConstraints() {
    return catDimensionConstraints;
  }

  public void setCatDimensionConstraints(Set<Category> catDimensionConstraints) {
    this.catDimensionConstraints = catDimensionConstraints;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(
      localName = "cogsDimensionConstraints",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "cogsDimensionConstraint", namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryOptionGroupSet> getCogsDimensionConstraints() {
    return cogsDimensionConstraints;
  }

  public void setCogsDimensionConstraints(Set<CategoryOptionGroupSet> cogsDimensionConstraints) {
    this.cogsDimensionConstraints = cogsDimensionConstraints;
  }

  @JsonIgnore
  public List<String> getPreviousPasswords() {
    return previousPasswords;
  }

  public void setPreviousPasswords(List<String> previousPasswords) {
    this.previousPasswords = previousPasswords;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(value = PropertyType.TEXT, required = FALSE)
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getOpenId() {
    return openId;
  }

  public void setOpenId(String openId) {
    this.openId = openId;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLdapId() {
    return ldapId;
  }

  public void setLdapId(String ldapId) {
    this.ldapId = ldapId;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(Date lastLogin) {
    this.lastLogin = lastLogin;
  }

  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }

  public String getRestoreToken() {
    return restoreToken;
  }

  public void setRestoreToken(String restoreToken) {
    this.restoreToken = restoreToken;
  }

  public Date getRestoreExpiry() {
    return restoreExpiry;
  }

  public void setRestoreExpiry(Date restoreExpiry) {
    this.restoreExpiry = restoreExpiry;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSelfRegistered() {
    return selfRegistered;
  }

  public void setSelfRegistered(boolean selfRegistered) {
    this.selfRegistered = selfRegistered;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isInvitation() {
    return invitation;
  }

  public void setInvitation(boolean invitation) {
    this.invitation = invitation;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getAccountExpiry() {
    return accountExpiry;
  }

  public void setAccountExpiry(Date accountExpiry) {
    this.accountExpiry = accountExpiry;
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(access = Property.Access.WRITE_ONLY)
  public Map<String, String> getSettings() {
    return settings;
  }

  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }

  public Collection<GrantedAuthority> getAuthorities() {
    Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();

    getAllAuthorities()
        .forEach(authority -> grantedAuthorities.add(new SimpleGrantedAuthority(authority)));

    return grantedAuthorities;
  }

  public boolean isAccountNonExpired() {
    return accountExpiry == null || accountExpiry.after(new Date());
  }

  public boolean isAccountNonLocked() {
    return isAccountNonLocked;
  }

  public void setAccountNonLocked(boolean isAccountNonLocked) {
    this.isAccountNonLocked = isAccountNonLocked;
  }

  public boolean isCredentialsNonExpired() {
    return isCredentialsNonExpired;
  }

  public void setCredentialsNonExpired(boolean isCredentialsNonExpired) {
    this.isCredentialsNonExpired = isCredentialsNonExpired;
  }

  public boolean isEnabled() {
    return !isDisabled();
  }

  public void addOrganisationUnit(OrganisationUnit unit) {
    organisationUnits.add(unit);
    unit.getUsers().add(this);
  }

  public void removeOrganisationUnit(OrganisationUnit unit) {
    organisationUnits.remove(unit);
    unit.getUsers().remove(this);
  }

  public void addOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::addOrganisationUnit);
  }

  public void removeOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::removeOrganisationUnit);
  }

  public void updateOrganisationUnits(Set<OrganisationUnit> updates) {
    for (OrganisationUnit unit : new HashSet<>(organisationUnits)) {
      if (!updates.contains(unit)) {
        removeOrganisationUnit(unit);
      }
    }

    for (OrganisationUnit unit : updates) {
      addOrganisationUnit(unit);
    }
  }

  /**
   * Note that setting read-only both ways seems needed when this is a DB field that is not null but
   * generated.
   */
  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Property(required = FALSE, access = Property.Access.READ_ONLY)
  public String getName() {
    // this is to maintain name for transient User objects initialized without setting name
    if (name == null) return firstName + " " + surname;
    return name;
  }

  /**
   * Checks whether the profile has been filled, which is defined as three not-null properties out
   * of all optional properties.
   */
  public boolean isProfileFilled() {
    Object[] props = {
      jobTitle,
      introduction,
      gender,
      birthday,
      nationality,
      employer,
      education,
      interests,
      languages
    };

    int count = 0;

    for (Object prop : props) {
      count = prop != null ? (count + 1) : count;
    }

    return count > 3;
  }

  /**
   * Returns the first of the organisation units associated with the user. Null is returned if the
   * user has no organisation units. Which organisation unit to return is undefined if the user has
   * multiple organisation units.
   */
  public OrganisationUnit getOrganisationUnit() {
    return CollectionUtils.isEmpty(organisationUnits) ? null : organisationUnits.iterator().next();
  }

  public boolean hasOrganisationUnit() {
    return !CollectionUtils.isEmpty(organisationUnits);
  }

  // -------------------------------------------------------------------------
  // Logic - data view organisation unit
  // -------------------------------------------------------------------------

  public boolean hasDataViewOrganisationUnit() {
    return !CollectionUtils.isEmpty(dataViewOrganisationUnits);
  }

  public OrganisationUnit getDataViewOrganisationUnit() {
    return CollectionUtils.isEmpty(dataViewOrganisationUnits)
        ? null
        : dataViewOrganisationUnits.iterator().next();
  }

  public boolean hasDataViewOrganisationUnitWithFallback() {
    return hasDataViewOrganisationUnit() || hasOrganisationUnit();
  }

  /**
   * Returns the first of the data view organisation units associated with the user. If none,
   * returns the first of the data capture organisation units. If none, return nulls.
   */
  public OrganisationUnit getDataViewOrganisationUnitWithFallback() {
    return hasDataViewOrganisationUnit() ? getDataViewOrganisationUnit() : getOrganisationUnit();
  }

  /** Returns the data view organisation units or organisation units if not exist. */
  public Set<OrganisationUnit> getDataViewOrganisationUnitsWithFallback() {
    return hasDataViewOrganisationUnit() ? dataViewOrganisationUnits : organisationUnits;
  }

  // -------------------------------------------------------------------------
  // Logic - tei search organisation unit
  // -------------------------------------------------------------------------

  private boolean hasTeiSearchOrganisationUnit() {
    return !CollectionUtils.isEmpty(teiSearchOrganisationUnits);
  }

  /**
   * Returns the tei search organisation units or organisation units if not exist. If you need both
   * org unit scopes, use {@link #getEffectiveSearchOrganisationUnits} instead.
   */
  public Set<OrganisationUnit> getTeiSearchOrganisationUnitsWithFallback() {
    return hasTeiSearchOrganisationUnit() ? teiSearchOrganisationUnits : organisationUnits;
  }

  /**
   * Users' capture scope and search scope org units can be entirely independent. The effective
   * search org units are the union of both scopes. This method is intended for use during data
   * import/export operations in the tracker.
   */
  public Set<OrganisationUnit> getEffectiveSearchOrganisationUnits() {
    return Stream.concat(teiSearchOrganisationUnits.stream(), organisationUnits.stream())
        .collect(Collectors.toSet());
  }

  public String getOrganisationUnitsName() {
    return IdentifiableObjectUtils.join(organisationUnits);
  }

  /**
   * Tests whether the user has the given authority. Returns true in any case if the user has the
   * ALL authority.
   *
   * @param auth the {@link Authorities}.
   */
  public boolean isAuthorized(@Nonnull Authorities auth) {
    return isAuthorized(auth.toString());
  }

  public Set<UserGroup> getManagedGroups() {
    return groups == null
        ? Set.of()
        : groups.stream()
            .flatMap(group -> emptyIfNull(group.getManagedGroups()).stream())
            .collect(Collectors.toUnmodifiableSet());
  }

  public boolean hasManagedGroups() {
    return groups != null
        && groups.stream().anyMatch(group -> group != null && !group.getManagedGroups().isEmpty());
  }

  /**
   * Indicates whether this user can manage the given user group.
   *
   * @param userGroup the user group to test.
   * @return true if the given user group can be managed by this user, false if not.
   */
  public boolean canManage(UserGroup userGroup) {
    return userGroup != null && CollectionUtils.containsAny(groups, userGroup.getManagedByGroups());
  }

  /**
   * Indicates whether this user can manage the given user.
   *
   * @param user the user to test.
   * @return true if the given user can be managed by this user, false if not.
   */
  public boolean canManage(User user) {
    if (user == null || user.getGroups() == null) {
      return false;
    }

    for (UserGroup group : user.getGroups()) {
      if (canManage(group)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Indicates whether this user is managed by the given user group.
   *
   * @param userGroup the user group to test.
   * @return true if the given user group is managed by this user, false if not.
   */
  public boolean isManagedBy(UserGroup userGroup) {
    return userGroup != null && CollectionUtils.containsAny(groups, userGroup.getManagedGroups());
  }

  /**
   * Indicates whether this user is managed by the given user.
   *
   * @param user the user to test.
   * @return true if the given user is managed by this user, false if not.
   */
  public boolean isManagedBy(User user) {
    if (user == null || user.getGroups() == null) {
      return false;
    }

    for (UserGroup group : user.getGroups()) {
      if (isManagedBy(group)) {
        return true;
      }
    }

    return false;
  }

  public static String getSafeUsername(String username) {
    return StringUtils.isEmpty(username) ? "[Unknown]" : username;
  }

  public boolean hasEmail() {
    return email != null && !email.isEmpty();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(PropertyType.EMAIL)
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getJobTitle() {
    return jobTitle;
  }

  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getIntroduction() {
    return introduction;
  }

  public void setIntroduction(String introduction) {
    this.introduction = introduction;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getBirthday() {
    return birthday;
  }

  public void setBirthday(Date birthday) {
    this.birthday = birthday;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getNationality() {
    return nationality;
  }

  public void setNationality(String nationality) {
    this.nationality = nationality;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getEmployer() {
    return employer;
  }

  public void setEmployer(String employer) {
    this.employer = employer;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getInterests() {
    return interests;
  }

  public void setInterests(String interests) {
    this.interests = interests;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLanguages() {
    return languages;
  }

  public void setLanguages(String languages) {
    this.languages = languages;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getWelcomeMessage() {
    return welcomeMessage;
  }

  public void setWelcomeMessage(String welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getLastCheckedInterpretations() {
    return lastCheckedInterpretations;
  }

  public void setLastCheckedInterpretations(Date lastCheckedInterpretations) {
    this.lastCheckedInterpretations = lastCheckedInterpretations;
  }

  @JsonProperty("userGroups")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "userGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "userGroup", namespace = DxfNamespaces.DXF_2_0)
  public Set<UserGroup> getGroups() {
    return groups;
  }

  public void setGroups(Set<UserGroup> groups) {
    this.groups = groups;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public Set<OrganisationUnit> getOrganisationUnits() {
    return organisationUnits;
  }

  public User setOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    this.organisationUnits = organisationUnits;
    return this;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(
      localName = "dataViewOrganisationUnits",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataViewOrganisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public Set<OrganisationUnit> getDataViewOrganisationUnits() {
    return dataViewOrganisationUnits;
  }

  public void setDataViewOrganisationUnits(Set<OrganisationUnit> dataViewOrganisationUnits) {
    this.dataViewOrganisationUnits = dataViewOrganisationUnits;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(
      localName = "teiSearchOrganisationUnits",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "teiSearchOrganisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public Set<OrganisationUnit> getTeiSearchOrganisationUnits() {
    return teiSearchOrganisationUnits;
  }

  public void setTeiSearchOrganisationUnits(Set<OrganisationUnit> teiSearchOrganisationUnits) {
    this.teiSearchOrganisationUnits = teiSearchOrganisationUnits;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Integer getDataViewMaxOrganisationUnitLevel() {
    return dataViewMaxOrganisationUnitLevel;
  }

  public void setDataViewMaxOrganisationUnitLevel(Integer dataViewMaxOrganisationUnitLevel) {
    this.dataViewMaxOrganisationUnitLevel = dataViewMaxOrganisationUnitLevel;
  }

  public List<String> getApps() {
    return apps;
  }

  public void setApps(List<String> apps) {
    this.apps = apps;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getWhatsApp() {
    return whatsApp;
  }

  public void setWhatsApp(String whatsapp) {
    this.whatsApp = whatsapp;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFacebookMessenger() {
    return facebookMessenger;
  }

  public void setFacebookMessenger(String facebookMessenger) {
    this.facebookMessenger = facebookMessenger;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getSkype() {
    return skype;
  }

  public void setSkype(String skype) {
    this.skype = skype;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getTelegram() {
    return telegram;
  }

  public void setTelegram(String telegram) {
    this.telegram = telegram;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getTwitter() {
    return twitter;
  }

  public void setTwitter(String twitter) {
    this.twitter = twitter;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FileResource getAvatar() {
    return avatar;
  }

  public void setAvatar(FileResource avatar) {
    this.avatar = avatar;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getVerifiedEmail() {
    return this.verifiedEmail;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isEmailVerified() {
    return this.getEmail() != null && Objects.equals(this.getEmail(), this.getVerifiedEmail());
  }

  public void setVerifiedEmail(String verifiedEmail) {
    this.verifiedEmail = verifiedEmail;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getEmailVerificationToken() {
    return this.emailVerificationToken;
  }

  public void setEmailVerificationToken(String emailVerificationToken) {
    this.emailVerificationToken = emailVerificationToken;
  }

  public static String username(User user) {
    // TODO: MAS get rid of this default value use of "system-process"
    return username(user, "system-process");
  }

  public static String username(UserDetails user) {
    return username(user, "system-process");
  }

  public static String username(User user, String defaultValue) {
    return user != null ? user.getUsername() : defaultValue;
  }

  public static String username(UserDetails user, String defaultValue) {
    return user != null ? user.getUsername() : defaultValue;
  }
}
