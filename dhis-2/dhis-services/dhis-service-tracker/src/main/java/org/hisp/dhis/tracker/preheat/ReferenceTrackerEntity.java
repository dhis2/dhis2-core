package org.hisp.dhis.tracker.preheat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class ReferenceTrackerEntity
{
    /**
     * Reference uid: this correspond to the UID of a TEI, PS or PSI from the Tracker Import payload
     */
    private final String uid;

    /**
     * Reference uid of the parent object of this Reference. This is only populated if uid references a ProgramStage
     * or a Program Stage Instance
     */
    private final String parentUid;
}
