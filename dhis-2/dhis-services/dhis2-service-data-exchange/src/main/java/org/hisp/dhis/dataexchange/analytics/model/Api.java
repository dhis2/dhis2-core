package org.hisp.dhis.dataexchange.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Api
{
    @JsonProperty
    private String url;

    @JsonProperty
    private String username;

    @JsonProperty
    private String password; // TODO Custom encryption
}
