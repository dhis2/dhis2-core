package org.hisp.dhis.dataexchange.analytics.model;

import org.hisp.dhis.common.IdScheme;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TargetRequest
{
    @JsonProperty
    private IdScheme dataElementIdScheme;

    @JsonProperty
    private IdScheme orgUnitIdScheme;

    @JsonProperty
    private IdScheme categoryOptionComboIdScheme;

    @JsonProperty
    private IdScheme idScheme;
}
