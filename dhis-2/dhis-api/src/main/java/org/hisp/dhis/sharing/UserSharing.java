package org.hisp.dhis.sharing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserSharing
    implements Serializable
{
    private static final long serialVersionUID = 518817889121954770L;

    @JsonProperty("id")
    private String userUuid;

    @JsonProperty
    private String access;
}
