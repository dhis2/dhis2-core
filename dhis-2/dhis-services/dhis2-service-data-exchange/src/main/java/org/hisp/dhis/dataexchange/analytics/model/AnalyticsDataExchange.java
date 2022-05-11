package org.hisp.dhis.dataexchange.analytics.model;

import org.hisp.dhis.common.BaseIdentifiableObject;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyticsDataExchange
    extends BaseIdentifiableObject
{
    @JsonProperty
    private Source source;

    @JsonProperty
    private Target target;
}
