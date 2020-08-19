package org.hisp.dhis.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;

import java.io.Serializable;
import java.util.Map;

@Builder
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Sharing
    implements Serializable
{
    private static final long serialVersionUID = 6977793211734844477L;

    @JsonProperty
    private String owner;

    @JsonProperty("public")
    private String publicAccess;

    @JsonProperty
    private boolean external;

    @JsonProperty
    private Map<String, UserAccess> users;

    @JsonProperty
    private Map<String, UserGroupAccess> userGroups;

}

