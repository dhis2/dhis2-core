package org.hisp.dhis.sharing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupSharing
    implements Serializable
{
    private static final long serialVersionUID = -3710960740973504220L;

    @JsonProperty
    private String userGroupUuid;

    @JsonProperty
    private String access;
}
