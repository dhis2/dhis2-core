package org.hisp.dhis.webapi.controller.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.user.UserAuthorityGroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement( localName = "userCredWrapperDto", namespace = DxfNamespaces.DXF_2_0 )
public class UserCredWrapperDto
{
    @JsonProperty
    private String uid;

    @JsonProperty
    private String id;

    @JsonProperty
    private String uuid;

    @JsonProperty
    private String username;

    @JsonProperty
    private boolean externalAuth;

    @JsonProperty
    private String openId;

    @JsonProperty
    private String ldapId;

    @JsonProperty
    private String password;

    @JsonProperty
    private boolean twoFA;

    @JsonProperty
    private String secret;

    @JsonProperty
    private Date passwordLastUpdated;

    @JsonProperty
    private Set<UserAuthorityGroup> userAuthorityGroups = new HashSet<>();

    @JsonProperty
    private Set<CategoryOptionGroupSet> cogsDimensionConstraints = new HashSet<>();

    @JsonProperty
    private Set<Category> catDimensionConstraints = new HashSet<>();

    @JsonProperty
    private List<String> previousPasswords = new ArrayList<>();

    @JsonProperty
    private Date lastLogin;

    @JsonProperty
    private String restoreToken;

    @JsonProperty
    private String idToken;

    @JsonProperty
    private Date restoreExpiry;

    @JsonProperty
    private boolean selfRegistered;

    @JsonProperty
    private boolean invitation;

    @JsonProperty
    private boolean disabled;

    @JsonProperty
    private Date accountExpiry;

    @JsonProperty()
    private Access access;

    @JsonProperty
    private Set<UserAuthorityGroup> userRoles;
}
