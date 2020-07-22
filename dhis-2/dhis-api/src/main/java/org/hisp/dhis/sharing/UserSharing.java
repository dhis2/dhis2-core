package org.hisp.dhis.sharing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSharing
    implements Serializable
{
    private static final long serialVersionUID = 518817889121954770L;

    @JsonProperty
    private String userUuid;

    @JsonProperty
    private String access;
}
