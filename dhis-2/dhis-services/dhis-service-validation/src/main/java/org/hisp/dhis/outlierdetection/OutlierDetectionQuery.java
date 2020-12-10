package org.hisp.dhis.outlierdetection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OutlierDetectionQuery
{
    @JsonProperty
    private List<String> ds = new ArrayList<>();

    @JsonProperty
    private Date startDate;

    @JsonProperty
    private Date endDate;

    @JsonProperty
    private List<String> ou = new ArrayList<>();

    @JsonProperty
    private Double threshold;

    @JsonProperty
    private Order orderBy;

    @JsonProperty
    private Integer maxResults;
}
