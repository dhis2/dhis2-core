package org.hisp.dhis.tracker.preheat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class UnpersistedObject {
    private final String uid;
    private final String parentUid;
    @Setter
    private boolean valid = true;
}
