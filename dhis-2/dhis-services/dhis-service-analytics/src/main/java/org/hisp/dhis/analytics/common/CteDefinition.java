package org.hisp.dhis.analytics.common;

import lombok.Getter;
import org.apache.commons.text.RandomStringGenerator;

import java.util.ArrayList;
import java.util.List;

public class CteDefinition {

    // Query item id
    @Getter
    private String itemId;
    // The program stage uid
    private final String programStageUid;
    // The program indicator uid
    private String programIndicatorUid;
    // The CTE definition (the SQL query)
    @Getter
    private final String cteDefinition;
    // The calculated offset
    @Getter
    private final List<Integer> offsets = new ArrayList<>();
    // The alias of the CTE
    private final String alias;
    // Whether the CTE is a row context (TODO this need a better explanation)
    @Getter
    private boolean rowContext;
    // Whether the CTE is a program indicator
    @Getter
    private boolean programIndicator = false;
    // Whether the CTE is a filter
    @Getter
    private boolean filter = false;
    // Whether the CTE is a exists, used for checking if the enrollment exists
    private boolean isExists = false;

    private static final String PS_PREFIX = "ps";
    private static final String PI_PREFIX = "pi";

    public CteDefinition setExists(boolean exists) {
        this.isExists = exists;
        return this;
    }

    public String getAlias() {
        if (offsets.size() <= 1) {
            return alias;
        }
        return computeAlias(offsets.get(0));
    }

    public String getAlias(int offset) {
        return computeAlias(offset);
    }

    private String computeAlias(int offset) {
        return alias + "_" + offset;
    }

    public CteDefinition(String programStageUid, String queryItemId, String cteDefinition, int offset) {
        this.programStageUid = programStageUid;
        this.itemId = queryItemId;
        this.cteDefinition = cteDefinition;
        this.offsets.add(offset);
        // one alias per offset
        this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(5);
        this.rowContext = false;
    }

    public CteDefinition(
            String programStageUid, String queryItemId, String cteDefinition, int offset, boolean isRowContext) {
        this(programStageUid, queryItemId, cteDefinition, offset);
        this.rowContext = isRowContext;
    }

    public CteDefinition(String programIndicatorUid, String cteDefinition) {
        this.cteDefinition = cteDefinition;
        this.programIndicatorUid = programIndicatorUid;
        this.programStageUid = null;
        // ignore offset
        this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(5);
        this.rowContext = false;
        this.programIndicator = true;
    }

    public CteDefinition(String queryItemId, String programStageUid, String cteDefinition, boolean isFilter) {
        this.itemId = queryItemId;
        this.cteDefinition = cteDefinition;
        this.programIndicatorUid = null;
        this.programStageUid = programStageUid;
        // ignore offset
        this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(5);
        this.rowContext = false;
        this.programIndicator = false;
        this.filter = isFilter;
    }

    /**
     * @param uid the uid of an dimension item or ProgramIndicator
     * @return the name of the CTE
     */
    public String asCteName(String uid) {
        if (isExists) {
            return uid.toLowerCase();
        }
        if (programIndicator) {
            return "%s_%s".formatted(PI_PREFIX, programIndicatorUid.toLowerCase());
        }
        if (filter) {
            return uid.toLowerCase();
        }

        return "%s_%s_%s".formatted(PS_PREFIX, programStageUid.toLowerCase(), uid.toLowerCase());
    }

    public boolean isProgramStage() {
        return !filter && !programIndicator && !isExists;
    }

    public boolean isExists() {
        return isExists;
    }

}
