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

import static org.springframework.beans.BeanUtils.copyProperties;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Nguyen Hong Duc
 */
@JacksonXmlRootElement( localName = "user", namespace = DxfNamespaces.DXF_2_0 )
public class User
    extends BaseIdentifiableObject implements MetadataObject, UserDetails
{
    public static final int USERNAME_MAX_LENGTH = 255;

    /**
     * Globally unique identifier for User.
     */
    private UUID uuid;

    /**
     * Required and unique.
     */
    private String username;

    /**
     * Indicates whether this user can only be authenticated externally, such as
     * through OpenID or LDAP.
     */
    private boolean externalAuth;

    /**
     * Unique OpenID.
     */
    private String openId;

    /**
     * Unique LDAP distinguished name.
     */
    private String ldapId;

    /**
     * Required. Will be stored as a hash.
     */
    private String password;

    /**
     * Required. Does this user have two factor authentication
     */
    private boolean twoFA;

    /**
     * Required. Automatically set in constructor
     */
    private String secret;

    /**
     * Date when password was changed.
     */
    private Date passwordLastUpdated;

    /**
     * Set of user roles.
     */
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * Category option group set dimensions to constrain data analytics
     * aggregation.
     */
    private Set<CategoryOptionGroupSet> cogsDimensionConstraints = new HashSet<>();

    /**
     * Category dimensions to constrain data analytics aggregation.
     */
    private Set<Category> catDimensionConstraints = new HashSet<>();

    /**
     * List of previously used passwords.
     */
    private List<String> previousPasswords = new ArrayList<>();

    /**
     * Date of the user's last login.
     */
    private Date lastLogin;

    /**
     * The token used for a user account restore. Will be stored as a hash.
     */
    private String restoreToken;

    /**
     * The token used for a user lookup when sending restore and invite emails.
     */
    private String idToken;

    /**
     * The timestamp representing when the restore window expires.
     */
    private Date restoreExpiry;

    /**
     * Indicates whether this user was originally self registered.
     */
    private boolean selfRegistered;

    /**
     * Indicates whether this user is currently an invitation.
     */
    private boolean invitation;

    /**
     * Indicates whether this is user is disabled, which means the user cannot
     * be authenticated.
     */
    private boolean disabled;

    private boolean isCredentialsNonExpired;

    private boolean isAccountNonLocked;

    /**
     * The timestamp representing when the user account expires. If not set the
     * account does never expire.
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

    // Backward comp. field, will be removed when front-end has converted to new
    // User model
    private transient UserCredentialsDto userCredentialsRaw;

    /**
     * Organisation units for data input and data capture operations.
     */
    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    /**
     * Organisation units for data output and data analysis operations.
     */
    private Set<OrganisationUnit> dataViewOrganisationUnits = new HashSet<>();

    /**
     * Organisation units for tracked entity instance search operations.
     */
    private Set<OrganisationUnit> teiSearchOrganisationUnits = new HashSet<>();

    /**
     * Max organisation unit level for data output and data analysis operations,
     * may be null.
     */
    private Integer dataViewMaxOrganisationUnitLevel;

    /**
     * Ordered favorite apps.
     */
    private List<String> apps = new ArrayList<>();

    /**
     * OBS! This field will only be set when de-serialising a user with settings
     * so the settings can be updated/stored.
     *
     * It is not initialised when loading a user from the database.
     */
    private transient UserSettings settings;

    public User()
    {
        this.twoFA = false;
        this.lastLogin = null;
        this.passwordLastUpdated = new Date();
        if ( uuid == null )
        {
            uuid = UUID.randomUUID();
        }
    }

    /**
     * Returns a concatenated String of the display names of all user authority
     * groups for this user.
     */
    public String getUserRoleNames()
    {
        return IdentifiableObjectUtils.join( userRoles );
    }

    /**
     * Returns a set of the aggregated authorities for all user authority groups
     * of this user.
     */
    public Set<String> getAllAuthorities()
    {
        Set<String> authorities = new HashSet<>();

        for ( UserRole group : userRoles )
        {
            authorities.addAll( group.getAuthorities() );
        }

        authorities = Collections.unmodifiableSet( authorities );

        return authorities;
    }

    /**
     * Indicates whether this user has at least one authority through its user
     * authority groups.
     */
    public boolean hasAuthorities()
    {
        for ( UserRole group : userRoles )
        {
            if ( group != null && group.getAuthorities() != null && !group.getAuthorities().isEmpty() )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether this user has any of the authorities in the given set.
     *
     * @param auths the authorities to compare with.
     *
     * @return true or false.
     */
    public boolean hasAnyAuthority( Collection<String> auths )
    {
        return getAllAuthorities().stream().anyMatch( auths::contains );
    }

    /**
     * Tests whether the user has the given authority. Returns true in any case
     * if the user has the ALL authority.
     */
    public boolean isAuthorized( String auth )
    {
        if ( auth == null )
        {
            return false;
        }

        final Set<String> auths = getAllAuthorities();

        return auths.contains( UserRole.AUTHORITY_ALL ) || auths.contains( auth );
    }

    /**
     * Indicates whether this user is a super user, implying that the ALL
     * authority is present in at least one of the user authority groups of this
     * user.
     */
    public boolean isSuper()
    {
        return userRoles.stream().anyMatch( UserRole::isSuper );
    }

    /**
     * Indicates whether this user can issue the given user authority group.
     * First the given authority group must not be null. Second this user must
     * not contain the given authority group. Third the authority group must be
     * a subset of the aggregated user authorities of this user, or this user
     * must have the ALL authority.
     *
     * @param group the user authority group.
     * @param canGrantOwnUserRole indicates whether this users can grant its own
     *        authority groups to others.
     */
    public boolean canIssueUserRole( UserRole group, boolean canGrantOwnUserRole )
    {
        if ( group == null )
        {
            return false;
        }

        final Set<String> authorities = getAllAuthorities();

        if ( authorities.contains( UserRole.AUTHORITY_ALL ) )
        {
            return true;
        }

        if ( !canGrantOwnUserRole && userRoles.contains( group ) )
        {
            return false;
        }

        return authorities.containsAll( group.getAuthorities() );
    }

    /**
     * Indicates whether this user can issue all of the user authority groups in
     * the given collection.
     *
     * @param groups the collection of user authority groups.
     * @param canGrantOwnUserRole indicates whether this users can grant its own
     *        authority groups to others.
     */
    public boolean canIssueUserRoles( Collection<UserRole> groups, boolean canGrantOwnUserRole )
    {
        for ( UserRole group : groups )
        {
            if ( !canIssueUserRole( group, canGrantOwnUserRole ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Indicates whether this user can modify the given user. This user must
     * have the ALL authority or possess all user authorities of the other user
     * to do so.
     *
     * @param other the user to modify.
     */
    public boolean canModifyUser( User other )
    {
        if ( other == null )
        {
            return false;
        }

        final Set<String> authorities = getAllAuthorities();

        if ( authorities.contains( UserRole.AUTHORITY_ALL ) )
        {
            return true;
        }

        return authorities.containsAll( other.getAllAuthorities() );
    }

    /**
     * Sets the last login property to the current date.
     */
    public void updateLastLogin()
    {
        this.lastLogin = new Date();
    }

    /**
     * Returns the dimensions to use as constrains (filters) in data analytics
     * aggregation.
     */
    public Set<DimensionalObject> getDimensionConstraints()
    {
        Set<DimensionalObject> constraints = new HashSet<>();

        for ( CategoryOptionGroupSet cogs : cogsDimensionConstraints )
        {
            cogs.setDimensionType( DimensionType.CATEGORY_OPTION_GROUP_SET );
            constraints.add( cogs );
        }

        for ( Category cat : catDimensionConstraints )
        {
            cat.setDimensionType( DimensionType.CATEGORY );
            constraints.add( cat );
        }

        return constraints;
    }

    /**
     * Indicates whether this user has user authority groups.
     */
    public boolean hasUserRoles()
    {
        return userRoles != null && !userRoles.isEmpty();
    }

    /**
     * Indicates whether this user has dimension constraints.
     */
    public boolean hasDimensionConstraints()
    {
        Set<DimensionalObject> constraints = getDimensionConstraints();
        return constraints != null && !constraints.isEmpty();
    }

    /**
     * Indicates whether an LDAP identifier is set.
     */
    public boolean hasLdapId()
    {
        return ldapId != null && !ldapId.isEmpty();
    }

    /**
     * Indicates whether a password is set.
     */
    public boolean hasPassword()
    {
        return password != null;
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // ----------

    public UUID getUuid()
    {
        return uuid;
    }

    public void setUuid( UUID uuid )
    {
        this.uuid = uuid;
    }

    @Override
    @JsonProperty( access = JsonProperty.Access.WRITE_ONLY )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.PASSWORD, access = Property.Access.WRITE_ONLY )
    @PropertyRange( min = 8, max = 60 )
    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isTwoFA()
    {
        return twoFA;
    }

    /**
     * Set 2FA on user.
     *
     * @param twoFA true/false depending on activate or deactivate
     */
    public void setTwoFA( boolean twoFA )
    {
        this.twoFA = twoFA;
    }

    public boolean getTwoFA()
    {
        return twoFA;
    }

    @JsonIgnore
    public String getSecret()
    {
        return secret;
    }

    public void setSecret( String secret )
    {
        this.secret = secret;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isExternalAuth()
    {
        return externalAuth;
    }

    public void setExternalAuth( boolean externalAuth )
    {
        this.externalAuth = externalAuth;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getPasswordLastUpdated()
    {
        return passwordLastUpdated;
    }

    public void setPasswordLastUpdated( Date passwordLastUpdated )
    {
        this.passwordLastUpdated = passwordLastUpdated;
    }

    @JsonProperty( "userRoles" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "userRoles", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userRole", namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserRole> getUserRoles()
    {
        return userRoles;
    }

    public void setUserRoles( Set<UserRole> userRoles )
    {
        this.userRoles = userRoles;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "catDimensionConstraints", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "catDimensionConstraint", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Category> getCatDimensionConstraints()
    {
        return catDimensionConstraints;
    }

    public void setCatDimensionConstraints( Set<Category> catDimensionConstraints )
    {
        this.catDimensionConstraints = catDimensionConstraints;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "cogsDimensionConstraints", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "cogsDimensionConstraint", namespace = DxfNamespaces.DXF_2_0 )
    public Set<CategoryOptionGroupSet> getCogsDimensionConstraints()
    {
        return cogsDimensionConstraints;
    }

    public void setCogsDimensionConstraints( Set<CategoryOptionGroupSet> cogsDimensionConstraints )
    {
        this.cogsDimensionConstraints = cogsDimensionConstraints;
    }

    @JsonIgnore
    public List<String> getPreviousPasswords()
    {
        return previousPasswords;
    }

    public void setPreviousPasswords( List<String> previousPasswords )
    {
        this.previousPasswords = previousPasswords;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.TEXT, required = Property.Value.FALSE )
    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOpenId()
    {
        return openId;
    }

    public void setOpenId( String openId )
    {
        this.openId = openId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLdapId()
    {
        return ldapId;
    }

    public void setLdapId( String ldapId )
    {
        this.ldapId = ldapId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastLogin()
    {
        return lastLogin;
    }

    public void setLastLogin( Date lastLogin )
    {
        this.lastLogin = lastLogin;
    }

    public String getIdToken()
    {
        return idToken;
    }

    public void setIdToken( String idToken )
    {
        this.idToken = idToken;
    }

    public String getRestoreToken()
    {
        return restoreToken;
    }

    public void setRestoreToken( String restoreToken )
    {
        this.restoreToken = restoreToken;
    }

    public Date getRestoreExpiry()
    {
        return restoreExpiry;
    }

    public void setRestoreExpiry( Date restoreExpiry )
    {
        this.restoreExpiry = restoreExpiry;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSelfRegistered()
    {
        return selfRegistered;
    }

    public void setSelfRegistered( boolean selfRegistered )
    {
        this.selfRegistered = selfRegistered;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isInvitation()
    {
        return invitation;
    }

    public void setInvitation( boolean invitation )
    {
        this.invitation = invitation;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getAccountExpiry()
    {
        return accountExpiry;
    }

    public void setAccountExpiry( Date accountExpiry )
    {
        this.accountExpiry = accountExpiry;
    }

    @JsonProperty( access = JsonProperty.Access.WRITE_ONLY )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( access = Property.Access.WRITE_ONLY )
    public UserSettings getSettings()
    {
        return settings;
    }

    public void setSettings( UserSettings settings )
    {
        this.settings = settings;
    }

    // -------------------------------------------------------------------------
    // Two Factor Authentication methods
    // -------------------------------------------------------------------------
    @Override
    public Collection<GrantedAuthority> getAuthorities()
    {
        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        getAllAuthorities()
            .forEach( authority -> grantedAuthorities.add( new SimpleGrantedAuthority( authority ) ) );

        return grantedAuthorities;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return accountExpiry == null || accountExpiry.after( new Date() );
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return isAccountNonLocked;
    }

    public void setAccountNonLocked( boolean isAccountNonLocked )
    {
        this.isAccountNonLocked = isAccountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return isCredentialsNonExpired;
    }

    public void setCredentialsNonExpired( boolean isCredentialsNonExpired )
    {
        this.isCredentialsNonExpired = isCredentialsNonExpired;
    }

    @Override
    public boolean isEnabled()
    {
        return !isDisabled();
    }

    public void addOrganisationUnit( OrganisationUnit unit )
    {
        organisationUnits.add( unit );
        unit.getUsers().add( this );
    }

    public void removeOrganisationUnit( OrganisationUnit unit )
    {
        organisationUnits.remove( unit );
        unit.getUsers().remove( this );
    }

    public void addOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        organisationUnits.forEach( this::addOrganisationUnit );
    }

    public void removeOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        organisationUnits.forEach( this::removeOrganisationUnit );
    }

    public void updateOrganisationUnits( Set<OrganisationUnit> updates )
    {
        for ( OrganisationUnit unit : new HashSet<>( organisationUnits ) )
        {
            if ( !updates.contains( unit ) )
            {
                removeOrganisationUnit( unit );
            }
        }

        for ( OrganisationUnit unit : updates )
        {
            addOrganisationUnit( unit );
        }
    }

    /**
     * Returns the concatenated first name and surname.
     */
    @Override
    public String getName()
    {
        return firstName + " " + surname;
    }

    /**
     * Checks whether the profile has been filled, which is defined as three
     * not-null properties out of all optional properties.
     */
    public boolean isProfileFilled()
    {
        Object[] props = { jobTitle, introduction, gender, birthday,
            nationality, employer, education, interests, languages };

        int count = 0;

        for ( Object prop : props )
        {
            count = prop != null ? (count + 1) : count;
        }

        return count > 3;
    }

    /**
     * Returns the first of the organisation units associated with the user.
     * Null is returned if the user has no organisation units. Which
     * organisation unit to return is undefined if the user has multiple
     * organisation units.
     */
    public OrganisationUnit getOrganisationUnit()
    {
        return CollectionUtils.isEmpty( organisationUnits ) ? null : organisationUnits.iterator().next();
    }

    public boolean hasOrganisationUnit()
    {
        return !CollectionUtils.isEmpty( organisationUnits );
    }

    // -------------------------------------------------------------------------
    // Logic - data view organisation unit
    // -------------------------------------------------------------------------

    public boolean hasDataViewOrganisationUnit()
    {
        return !CollectionUtils.isEmpty( dataViewOrganisationUnits );
    }

    public OrganisationUnit getDataViewOrganisationUnit()
    {
        return CollectionUtils.isEmpty( dataViewOrganisationUnits ) ? null
            : dataViewOrganisationUnits.iterator().next();
    }

    public boolean hasDataViewOrganisationUnitWithFallback()
    {
        return hasDataViewOrganisationUnit() || hasOrganisationUnit();
    }

    /**
     * Returns the first of the data view organisation units associated with the
     * user. If none, returns the first of the data capture organisation units.
     * If none, return nulls.
     */
    public OrganisationUnit getDataViewOrganisationUnitWithFallback()
    {
        return hasDataViewOrganisationUnit() ? getDataViewOrganisationUnit() : getOrganisationUnit();
    }

    /**
     * Returns the data view organisation units or organisation units if not
     * exist.
     */
    public Set<OrganisationUnit> getDataViewOrganisationUnitsWithFallback()
    {
        return hasDataViewOrganisationUnit() ? dataViewOrganisationUnits : organisationUnits;
    }

    // -------------------------------------------------------------------------
    // Logic - tei search organisation unit
    // -------------------------------------------------------------------------

    public boolean hasTeiSearchOrganisationUnit()
    {
        return !CollectionUtils.isEmpty( teiSearchOrganisationUnits );
    }

    public OrganisationUnit getTeiSearchOrganisationUnit()
    {
        return CollectionUtils.isEmpty( teiSearchOrganisationUnits ) ? null
            : teiSearchOrganisationUnits.iterator().next();
    }

    public boolean hasTeiSearchOrganisationUnitWithFallback()
    {
        return hasTeiSearchOrganisationUnit() || hasOrganisationUnit();
    }

    /**
     * Returns the first of the tei search organisation units associated with
     * the user. If none, returns the first of the data capture organisation
     * units. If none, return nulls.
     */
    public OrganisationUnit getTeiSearchOrganisationUnitWithFallback()
    {
        return hasTeiSearchOrganisationUnit() ? getTeiSearchOrganisationUnit() : getOrganisationUnit();
    }

    /**
     * Returns the tei search organisation units or organisation units if not
     * exist.
     */
    public Set<OrganisationUnit> getTeiSearchOrganisationUnitsWithFallback()
    {
        return hasTeiSearchOrganisationUnit() ? teiSearchOrganisationUnits : organisationUnits;
    }

    public String getOrganisationUnitsName()
    {
        return IdentifiableObjectUtils.join( organisationUnits );
    }

    /**
     * Tests whether the user has the given authority. Returns true in any case
     * if the user has the ALL authority.
     *
     * @param auth the {@link Authorities}.
     */
    public boolean isAuthorized( Authorities auth )
    {
        return isAuthorized( auth.getAuthority() );
    }

    public Set<UserGroup> getManagedGroups()
    {
        Set<UserGroup> managedGroups = new HashSet<>();

        for ( UserGroup group : groups )
        {
            managedGroups.addAll( group.getManagedGroups() );
        }

        return managedGroups;
    }

    public boolean hasManagedGroups()
    {
        for ( UserGroup group : groups )
        {
            if ( group != null && group.getManagedGroups() != null && !group.getManagedGroups().isEmpty() )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Indicates whether this user can manage the given user group.
     *
     * @param userGroup the user group to test.
     *
     * @return true if the given user group can be managed by this user, false
     *         if not.
     */
    public boolean canManage( UserGroup userGroup )
    {
        return userGroup != null && CollectionUtils.containsAny( groups, userGroup.getManagedByGroups() );
    }

    /**
     * Indicates whether this user can manage the given user.
     *
     * @param user the user to test.
     *
     * @return true if the given user can be managed by this user, false if not.
     */
    public boolean canManage( User user )
    {
        if ( user == null || user.getGroups() == null )
        {
            return false;
        }

        for ( UserGroup group : user.getGroups() )
        {
            if ( canManage( group ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Indicates whether this user is managed by the given user group.
     *
     * @param userGroup the user group to test.
     *
     * @return true if the given user group is managed by this user, false if
     *         not.
     */
    public boolean isManagedBy( UserGroup userGroup )
    {
        return userGroup != null && CollectionUtils.containsAny( groups, userGroup.getManagedGroups() );
    }

    /**
     * Indicates whether this user is managed by the given user.
     *
     * @param user the user to test.
     *
     * @return true if the given user is managed by this user, false if not.
     */
    public boolean isManagedBy( User user )
    {
        if ( user == null || user.getGroups() == null )
        {
            return false;
        }

        for ( UserGroup group : user.getGroups() )
        {
            if ( isManagedBy( group ) )
            {
                return true;
            }
        }

        return false;
    }

    public static String getSafeUsername( String username )
    {
        return StringUtils.isEmpty( username ) ? "[Unknown]" : username;
    }

    public boolean hasEmail()
    {
        return email != null && !email.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName( String firstName )
    {
        this.firstName = firstName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getSurname()
    {
        return surname;
    }

    public void setSurname( String surname )
    {
        this.surname = surname;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.EMAIL )
    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getJobTitle()
    {
        return jobTitle;
    }

    public void setJobTitle( String jobTitle )
    {
        this.jobTitle = jobTitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getIntroduction()
    {
        return introduction;
    }

    public void setIntroduction( String introduction )
    {
        this.introduction = introduction;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getGender()
    {
        return gender;
    }

    public void setGender( String gender )
    {
        this.gender = gender;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getBirthday()
    {
        return birthday;
    }

    public void setBirthday( Date birthday )
    {
        this.birthday = birthday;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getNationality()
    {
        return nationality;
    }

    public void setNationality( String nationality )
    {
        this.nationality = nationality;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getEmployer()
    {
        return employer;
    }

    public void setEmployer( String employer )
    {
        this.employer = employer;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getEducation()
    {
        return education;
    }

    public void setEducation( String education )
    {
        this.education = education;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getInterests()
    {
        return interests;
    }

    public void setInterests( String interests )
    {
        this.interests = interests;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLanguages()
    {
        return languages;
    }

    public void setLanguages( String languages )
    {
        this.languages = languages;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getWelcomeMessage()
    {
        return welcomeMessage;
    }

    public void setWelcomeMessage( String welcomeMessage )
    {
        this.welcomeMessage = welcomeMessage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastCheckedInterpretations()
    {
        return lastCheckedInterpretations;
    }

    public void setLastCheckedInterpretations( Date lastCheckedInterpretations )
    {
        this.lastCheckedInterpretations = lastCheckedInterpretations;
    }

    @JsonProperty( "userGroups" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "userGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userGroup", namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserGroup> getGroups()
    {
        return groups;
    }

    public void setGroups( Set<UserGroup> groups )
    {
        this.groups = groups;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public User setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
        return this;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dataViewOrganisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataViewOrganisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getDataViewOrganisationUnits()
    {
        return dataViewOrganisationUnits;
    }

    public void setDataViewOrganisationUnits( Set<OrganisationUnit> dataViewOrganisationUnits )
    {
        this.dataViewOrganisationUnits = dataViewOrganisationUnits;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "teiSearchOrganisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "teiSearchOrganisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getTeiSearchOrganisationUnits()
    {
        return teiSearchOrganisationUnits;
    }

    public void setTeiSearchOrganisationUnits( Set<OrganisationUnit> teiSearchOrganisationUnits )
    {
        this.teiSearchOrganisationUnits = teiSearchOrganisationUnits;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getDataViewMaxOrganisationUnitLevel()
    {
        return dataViewMaxOrganisationUnitLevel;
    }

    public void setDataViewMaxOrganisationUnitLevel( Integer dataViewMaxOrganisationUnitLevel )
    {
        this.dataViewMaxOrganisationUnitLevel = dataViewMaxOrganisationUnitLevel;
    }

    public List<String> getApps()
    {
        return apps;
    }

    public void setApps( List<String> apps )
    {
        this.apps = apps;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getWhatsApp()
    {
        return whatsApp;
    }

    public void setWhatsApp( String whatsapp )
    {
        this.whatsApp = whatsapp;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFacebookMessenger()
    {
        return facebookMessenger;
    }

    public void setFacebookMessenger( String facebookMessenger )
    {
        this.facebookMessenger = facebookMessenger;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSkype()
    {
        return skype;
    }

    public void setSkype( String skype )
    {
        this.skype = skype;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTelegram()
    {
        return telegram;
    }

    public void setTelegram( String telegram )
    {
        this.telegram = telegram;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTwitter()
    {
        return twitter;
    }

    public void setTwitter( String twitter )
    {
        this.twitter = twitter;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FileResource getAvatar()
    {
        return avatar;
    }

    public void setAvatar( FileResource avatar )
    {
        this.avatar = avatar;
    }

    public static String username( User user )
    {
        return username( user, "system-process" );
    }

    public static String username( User user, String defaultValue )
    {
        return user != null ? user.getUsername() : defaultValue;
    }

    // This is a temporary fix to maintain backwards compatibility with the old
    // UserCredentials class. This method should not be used in new code!
    @JsonProperty
    public UserCredentialsDto getUserCredentials()
    {
        UserCredentialsDto userCredentialsDto = new UserCredentialsDto();
        copyProperties( this, userCredentialsDto, "uid", "userCredentials", "password", "userRoles", "secret",
            "previousPasswords" );
        Set<UserRole> roles = this.getUserRoles();
        if ( roles != null && !roles.isEmpty() )
        {
            userCredentialsDto.setUserRoles( roles );
        }
        return userCredentialsDto;
    }

    public UserCredentialsDto getUserCredentialsRaw()
    {
        return this.userCredentialsRaw;
    }

    // This is a temporary fix to maintain backwards compatibility with the old
    // UserCredentials class. This method should not be used in new code!
    protected void setUserCredentials( UserCredentialsDto user )
    {
        this.userCredentialsRaw = user;
    }

    public static void populateUserCredentialsDtoFields( User user )
    {
        UserCredentialsDto userCredentialsRaw = user.getUserCredentialsRaw();
        if ( userCredentialsRaw != null )
        {
            copyProperties( userCredentialsRaw, user, "uid", "password", "userRoles", "secret", "previousPasswords" );
            if ( userCredentialsRaw.getPassword() != null )
            {
                user.setPassword( userCredentialsRaw.getPassword() );
            }

            Set<UserRole> userRoles = userCredentialsRaw.getUserRoles();
            if ( userRoles != null )
            {
                user.setUserRoles( userRoles );
            }
        }
    }

    public static void populateUserCredentialsDtoFields3Way( User oldUser, User newUser )
    {
        UserCredentialsDto oldCredentialsRaw = oldUser.getUserCredentials();
        UserCredentialsDto newUserCredentialsRaw = newUser.getUserCredentialsRaw();

        if ( newUserCredentialsRaw != null )
        {
            UserCredentialsDto newUserCredentialsCopiedFromBase = newUser.getUserCredentials();

            copyOnlyChangedProperties( oldCredentialsRaw, newUserCredentialsRaw, newUser, "uid", "password", "userRoles",
                "secret", "previousPasswords" );
            copyOnlyChangedProperties( oldCredentialsRaw, newUserCredentialsCopiedFromBase, newUser, "uid", "password",
                "userRoles", "secret", "previousPasswords" );

            if ( newUserCredentialsRaw.getPassword() != null )
            {
                newUser.setPassword( newUserCredentialsRaw.getPassword() );
            }

            Set<UserRole> userRoles = newUserCredentialsRaw.getUserRoles();
            if ( userRoles != null )
            {
                newUser.setUserRoles( userRoles );
            }

            newUser.setUserCredentials( null );
        }
    }

    private static void copyOnlyChangedProperties( Object oldObject, Object source, Object target,
        @Nullable String... ignoreProperties )
    {
        Assert.notNull( oldObject, "Old object must not be null" );
        Assert.notNull( source, "Source must not be null" );
        Assert.notNull( target, "Target must not be null" );

        List<String> ignoreList = (ignoreProperties != null ? Arrays.asList( ignoreProperties ) : null);

        PropertyDescriptor[] targetPds;
        try
        {
            targetPds = Introspector.getBeanInfo( target.getClass() ).getPropertyDescriptors();
        }
        catch ( IntrospectionException e )
        {
            return;
        }

        for ( PropertyDescriptor targetPd : targetPds )
        {
            Method writeMethod = targetPd.getWriteMethod();
            if ( writeMethod != null && (ignoreList == null || !ignoreList.contains( targetPd.getName() )) )
            {
                PropertyDescriptor sourcePd;
                try
                {
                    sourcePd = new PropertyDescriptor( targetPd.getName(), source.getClass() );
                }
                catch ( IntrospectionException e )
                {
                    continue;
                }

                Method readMethod = sourcePd.getReadMethod();
                if ( readMethod != null )
                {
                    ResolvableType sourceResolvableType = ResolvableType.forMethodReturnType( readMethod );
                    ResolvableType targetResolvableType = ResolvableType.forMethodParameter( writeMethod, 0 );

                    boolean isAssignable = (sourceResolvableType.hasUnresolvableGenerics()
                        || targetResolvableType.hasUnresolvableGenerics()
                            ? ClassUtils.isAssignable( writeMethod.getParameterTypes()[0], readMethod.getReturnType() )
                            : targetResolvableType.isAssignableFrom( sourceResolvableType ));

                    if ( isAssignable )
                    {
                        try
                        {
                            if ( !Modifier.isPublic( readMethod.getDeclaringClass().getModifiers() ) )
                            {
                                readMethod.setAccessible( true );
                            }

                            Object oldValue = readMethod.invoke( oldObject );
                            Object value = readMethod.invoke( source );

                            if ( !Modifier.isPublic( writeMethod.getDeclaringClass().getModifiers() ) )
                            {
                                writeMethod.setAccessible( true );
                            }

                            if ( value != null && !value.equals( oldValue ) )
                            {
                                writeMethod.invoke( target, value );
                            }
                        }
                        catch ( Throwable ex )
                        {
                            // do nothing
                        }
                    }
                }
            }
        }
    }

}
