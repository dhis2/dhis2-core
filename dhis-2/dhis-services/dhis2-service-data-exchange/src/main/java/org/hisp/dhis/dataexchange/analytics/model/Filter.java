package org.hisp.dhis.dataexchange.analytics.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Filter
{
    @JsonProperty
    private String dimension;

    @JsonProperty
    private List<String> items;
}
