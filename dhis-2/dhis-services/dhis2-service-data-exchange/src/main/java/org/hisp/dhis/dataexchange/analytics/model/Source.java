package org.hisp.dhis.dataexchange.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Source
{
    @JsonProperty
    private SourceRequest request;
}
