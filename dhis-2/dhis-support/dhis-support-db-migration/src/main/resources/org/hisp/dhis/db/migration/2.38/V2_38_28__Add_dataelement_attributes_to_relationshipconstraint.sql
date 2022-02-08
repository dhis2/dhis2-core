
-- relationshipconstraintentityattribute table
CREATE TABLE IF NOT EXISTS relationshipconstraintentitytypeattribute
(
    relationshipconstraintentitytypeattributeid                 bigint             NOT NULL,
    relationshipconstraintid                                    integer            NOT NULL,
    trackedentitytypeattributeid                                bigint             NOT NULL,

    -- CONTRAINTS
    CONSTRAINT relationshipconstraintentitytypeattributeid_pkey PRIMARY KEY (relationshipconstraintentitytypeattributeid),
    CONSTRAINT fk_relationshipconstraint_typeattributeid FOREIGN KEY (relationshipconstraintid) REFERENCES relationshipconstraint (relationshipconstraintid),
    CONSTRAINT fk_relationshipconstraint_entitytypeattributeid FOREIGN KEY (trackedentitytypeattributeid) REFERENCES trackedentitytypeattribute (trackedentitytypeattributeid)

    );

-- relationshipconstrainttrackedentityattribute table
CREATE TABLE IF NOT EXISTS relationshipconstrainttrackedentityattribute
(
    relationshipconstrainttrackedentityattributeid               bigint             NOT NULL,
    relationshipconstraintid                                     integer            NOT NULL,
    trackedentityattributeid                                     bigint             NOT NULL,

    -- CONTRAINTS
    CONSTRAINT relationshipconstrainttrackedentityattributeid_pkey PRIMARY KEY (relationshipconstrainttrackedentityattributeid),
    CONSTRAINT fk_relationshipconstraint_attributeid FOREIGN KEY (relationshipconstraintid) REFERENCES relationshipconstraint (relationshipconstraintid),
    CONSTRAINT fk_relationshipconstraint_entityattributeid FOREIGN KEY (trackedentityattributeid) REFERENCES trackedentityattribute (trackedentityattributeid)

    );

-- relationshipconstraintdataelement table
CREATE TABLE IF NOT EXISTS relationshipconstraintdataelement
(
    relationshipconstraintdataelementid               bigint             NOT NULL,
    relationshipconstraintid                          integer            NOT NULL,
    dataelementid                                     bigint             NOT NULL,

    -- CONTRAINTS
    CONSTRAINT relationshipconstraintdataelement_pkey PRIMARY KEY (relationshipconstraintdataelementid),
    CONSTRAINT fk_relationshipconstraint_deid FOREIGN KEY (relationshipconstraintid) REFERENCES relationshipconstraint (relationshipconstraintid),
    CONSTRAINT fk_relationshipconstraint_dataelementid FOREIGN KEY (dataelementid) REFERENCES dataelement (dataelementid)

    );