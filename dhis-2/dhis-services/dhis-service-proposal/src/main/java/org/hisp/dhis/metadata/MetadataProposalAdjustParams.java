package org.hisp.dhis.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

/**
 * Input when an existing {@link MetadataProposal} is adjusted.
 *
 * @author Jan Bernitt
 */
@Getter
@Setter
public class MetadataProposalAdjustParams
{
    @JsonProperty
    private String targetUid;

    @JsonProperty
    private JsonNode change;

}
