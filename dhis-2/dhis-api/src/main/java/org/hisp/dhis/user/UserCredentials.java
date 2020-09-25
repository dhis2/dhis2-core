package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Access;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Nguyen Hong Duc
 */
@JacksonXmlRootElement( localName = "userCredentials", namespace = DxfNamespaces.DXF_2_0 )
public class UserCredentials
    extends BaseIdentifiableObject
    implements UserDetails
{
    public static final int USERNAME_MAX_LENGTH = 255;

    /**
     * Globally unique identifier for User.
     */
    private UUID uuid;

    /**
     * Required and unique.
     */
    private User userInfo;

    /**
     * Required and unique.
     */
    private String username;

    /**
     * Indicates whether this credentials can only be authenticated externally,
     * such as through OpenID or LDAP.
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
    private Set<UserAuthorityGroup> userAuthorityGroups = new HashSet<>();

    /**
     * Category option group set dimensions to constrain data analytics aggregation.
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
     * The timestamp representing when the restore window expires.
     */
    private Date restoreExpiry;

    /**
     * Indicates whether this user was originally self registered.
     */
    private boolean selfRegistered;

    /**
     * Indicates whether this credentials is currently an invitation.
     */
    private boolean invitation;

    /**
     * Indicates whether this is user is disabled, which means the user cannot
     * be authenticated.
     */
    private boolean disabled;

    /**
     * Cached all authorities {@link #getAllAuthorities()}.
     */
    @JsonIgnore
    @Transient
    private transient volatile Set<String> cachedAllAuthorities;

    /**
     * Cached state if user is super user {@link #isSuper()}.
     */
    @JsonIgnore
    @Transient
    private transient volatile Boolean cachedSuper;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public UserCredentials()
    {
        this.twoFA = false;
        this.lastLogin = null;
        this.passwordLastUpdated = new Date();
        this.setAutoFields(); // Needed to support user credentials uniqueness
        this.setSecret();
    }

    @Override
    public void setAutoFields()
    {
        if ( uuid == null )
        {
            uuid = UUID.randomUUID();
        }

        super.setAutoFields();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Returns a concatenated String of the display names of all user authority
     * groups for this user credentials.
     */
    public String getUserAuthorityGroupsName()
    {
        return IdentifiableObjectUtils.join( userAuthorityGroups );
    }

    /**
     * Returns a set of the aggregated authorities for all user authority groups
     * of this user credentials.
     */
    public Set<String> getAllAuthorities()
    {
        // cached all authorities can be reset to null by different thread and must be assigned before evaluation
        final Set<String> resultingAuthorities = cachedAllAuthorities;

        if ( resultingAuthorities != null )
        {
            return resultingAuthorities;
        }

        Set<String> authorities = new HashSet<>();

        for ( UserAuthorityGroup group : userAuthorityGroups )
        {
            authorities.addAll( group.getAuthorities() );
        }

        authorities = Collections.unmodifiableSet( authorities );
        cachedAllAuthorities = authorities;

        return authorities;
    }

    /**
     * Indicates whether this user credentials has at least one authority through
     * its user authority groups.
     */
    public boolean hasAuthorities()
    {
        for ( UserAuthorityGroup group : userAuthorityGroups )
        {
            if ( group != null && group.getAuthorities() != null && !group.getAuthorities().isEmpty() )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether this user credentials has any of the authorities in the
     * given set.
     *
     * @param auths the authorities to compare with.
     * @return true or false.
     */
    public boolean hasAnyAuthority( Collection<String> auths )
    {
        Set<String> all = new HashSet<>( getAllAuthorities() );
        return all.removeAll( auths );
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

        return auths.contains( UserAuthorityGroup.AUTHORITY_ALL ) || auths.contains( auth );
    }

    /**
     * Indicates whether this user credentials is a super user, implying that the
     * ALL authority is present in at least one of the user authority groups of
     * this user credentials.
     */
    public boolean isSuper()
    {
        final Boolean superUser = cachedSuper;

        if ( superUser != null )
        {
            return superUser;
        }

        final boolean resultingSuper = userAuthorityGroups.stream().anyMatch( UserAuthorityGroup::isSuper );

        cachedSuper = resultingSuper;

        return resultingSuper;
    }

    /**
     * Indicates whether this user credentials can issue the given user authority
     * group. First the given authority group must not be null. Second this
     * user credentials must not contain the given authority group. Third
     * the authority group must be a subset of the aggregated user authorities
     * of this user credentials, or this user credentials must have the ALL
     * authority.
     *
     * @param group                          the user authority group.
     * @param canGrantOwnUserAuthorityGroups indicates whether this users can grant
     *                                       its own authority groups to others.
     */
    public boolean canIssueUserRole( UserAuthorityGroup group, boolean canGrantOwnUserAuthorityGroups )
    {
        if ( group == null )
        {
            return false;
        }

        final Set<String> authorities = getAllAuthorities();

        if ( authorities.contains( UserAuthorityGroup.AUTHORITY_ALL ) )
        {
            return true;
        }

        if ( !canGrantOwnUserAuthorityGroups && userAuthorityGroups.contains( group ) )
        {
            return false;
        }

        return authorities.containsAll( group.getAuthorities() );
    }

    /**
     * Indicates whether this user credentials can issue all of the user authority
     * groups in the given collection.
     *
     * @param groups                         the collection of user authority groups.
     * @param canGrantOwnUserAuthorityGroups indicates whether this users can grant
     *                                       its own authority groups to others.
     */
    public boolean canIssueUserRoles( Collection<UserAuthorityGroup> groups, boolean canGrantOwnUserAuthorityGroups )
    {
        for ( UserAuthorityGroup group : groups )
        {
            if ( !canIssueUserRole( group, canGrantOwnUserAuthorityGroups ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Indicates whether this user credentials can modify the given user
     * credentials. This user credentials must have the ALL authority or possess
     * all user authorities of the other user credentials to do so.
     *
     * @param other the user credentials to modify.
     */
    public boolean canModifyUser( UserCredentials other )
    {
        if ( other == null )
        {
            return false;
        }

        final Set<String> authorities = getAllAuthorities();

        if ( authorities.contains( UserAuthorityGroup.AUTHORITY_ALL ) )
        {
            return true;
        }

        return authorities.containsAll( other.getAllAuthorities() );
    }

    /**
     * Return the name of this user credentials. More specifically, if this
     * credentials has a user it will return the first name and surname of that
     * user, if not it returns the username of this credentials.
     *
     * @return the name.
     */
    @Override
    public String getName()
    {
        return userInfo != null ? userInfo.getName() : username;
    }

    /**
     * Sets the last login property to the current date.
     */
    public void updateLastLogin()
    {
        this.lastLogin = new Date();
    }

    /**
     * Tests whether the credentials contain all needed parameters to
     * perform an account restore.
     * If a parameter is missing a descriptive error string is returned.
     *
     * @return null on success, a descriptive error string on failure.
     */
    public String isRestorable()
    {
        if ( restoreToken == null )
        {
            return "account_restoreToken_is_null";
        }

        if ( restoreExpiry == null )
        {
            return "account_restoreExpiry_is_null";
        }

        return null; // Success.
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
     * Indicates whether this user credentials has user authority groups.
     */
    public boolean hasUserAuthorityGroups()
    {
        return userAuthorityGroups != null && !userAuthorityGroups.isEmpty();
    }

    /**
     * Indicates whether this user credentials has dimension constraints.
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
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return username.hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null )
        {
            return false;
        }

        if ( !(o instanceof UserCredentials) )
        {
            return false;
        }

        final UserCredentials other = (UserCredentials) o;

        return username.equals( other.getUsername() );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UUID getUuid()
    {
        return uuid;
    }

    public void setUuid( UUID uuid )
    {
        this.uuid = uuid;
    }

    /**
     * Refers to the user associated with this user credentials.
     */
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getUserInfo()
    {
        return userInfo;
    }

    public void setUserInfo( User userInfo )
    {
        this.userInfo = userInfo;
    }

    @Override
    @JsonProperty( access = JsonProperty.Access.WRITE_ONLY )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.PASSWORD, access = Access.WRITE_ONLY )
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

    @JsonIgnore
    public String getSecret()
    {
        return secret;
    }

    public void setSecret( String secret )
    {
        if ( secret == null )
        {
            setSecret();
        }
        else
        {
            this.secret = secret;
        }
    }

    private void setSecret()
    {
        if ( this.secret == null )
        {
            this.secret = Base32.random();
        }
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
    public Set<UserAuthorityGroup> getUserAuthorityGroups()
    {
        return userAuthorityGroups;
    }

    public void setUserAuthorityGroups( Set<UserAuthorityGroup> userAuthorityGroups )
    {
        this.userAuthorityGroups = userAuthorityGroups;
        this.cachedSuper = null;
        this.cachedAllAuthorities = null;
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

    @Override
    public String toString()
    {
        return "{" +
            "\"username\":\"" + username + "\", " +
            "\"openId\":\"" + openId + "\", " +
            "\"password\":\"" + password + "\", " +
            "\"passwordLastUpdated\":\"" + passwordLastUpdated + "\", " +
            "\"lastLogin\":\"" + lastLogin + "\", " +
            "\"restoreToken\":\"" + restoreToken + "\", " +
            "\"restoreExpiry\":\"" + restoreExpiry + "\", " +
            "\"selfRegistered\":\"" + selfRegistered + "\", " +
            "\"disabled\":\"" + disabled + "\" " +
            "}";
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
        return false;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return false;
    }

    @Override
    public boolean isEnabled()
    {
        return !isDisabled();
    }
}
