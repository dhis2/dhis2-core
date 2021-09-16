package org.hisp.dhis.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input when making a {@link org.hisp.dhis.metadata.MetadataProposal}.
 *
 * @author Jan Bernitt
 */
@Getter
@Setter
@Builder
@AllArgsConstructor( access = AccessLevel.PRIVATE )
@NoArgsConstructor
public class MetadataProposalParams
{
    @JsonProperty
    private MetadataProposalType type;

    @JsonProperty
    private MetadataProposalTarget target;

    @JsonProperty
    private String targetUid;

    @JsonProperty
    private ObjectNode change;

    @JsonProperty
    private String comment;
}
