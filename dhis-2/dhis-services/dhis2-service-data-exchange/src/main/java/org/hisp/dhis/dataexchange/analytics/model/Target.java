package org.hisp.dhis.dataexchange.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Target
{
    @JsonProperty
    private TargetType type;

    /**
     * Only relevant for {@link TargetType#EXTERNAL}.
     */
    @JsonProperty
    private Api api;

    @JsonProperty
    private TargetRequest request;
}
