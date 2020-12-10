package org.hisp.dhis.outlierdetection;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OutlierValue
{
    @JsonProperty
    private String de;

    @JsonProperty
    private String pe;

    @JsonProperty
    private String ou;

    @JsonProperty
    private String ouName;

    @JsonProperty
    private String coc;

    @JsonProperty
    private String aoc;

    @JsonProperty
    private String comment;

    @JsonProperty
    private Date lastUpdated;

    @JsonProperty
    private Double value;

    @JsonProperty
    private Double mean;

    @JsonProperty
    private Double stdDev;

    @JsonProperty
    private Double meanAbsDev;

    @JsonProperty
    private Double zScore;

    @JsonProperty
    private Double lowerBound;

    @JsonProperty
    private Double upperBound;
}
