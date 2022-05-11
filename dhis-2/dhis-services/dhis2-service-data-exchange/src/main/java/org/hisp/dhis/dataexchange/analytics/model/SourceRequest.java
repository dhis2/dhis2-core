package org.hisp.dhis.dataexchange.analytics.model;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.IdScheme;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceRequest
{
    @JsonProperty
    private List<String> dx = new ArrayList<>();

    @JsonProperty
    private List<String> pe = new ArrayList<>();

    @JsonProperty
    private List<String> ou = new ArrayList<>();

    @JsonProperty
    private List<Filter> filters = new ArrayList<>();

    @JsonProperty
    private IdScheme outputIdScheme;

    @JsonProperty
    private IdScheme inputIdScheme;
}
