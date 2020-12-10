package org.hisp.dhis.outlierdetection;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OutlierValueResponse
{
    @JsonProperty
    private List<OutlierValue> outlierValues = new ArrayList<>();
}
