
-- relationshipitementityattribute table
CREATE TABLE IF NOT EXISTS relationshipitementitytypeattribute
(
    relationshipitementitytypeattributeid               bigint            NOT NULL,
    relationshipitemid                                  integer            NOT NULL,
    trackedentitytypeattributeid                        bigint            NOT NULL,

    -- CONTRAINTS
    CONSTRAINT relationshipitementitytypeattributeid_pkey PRIMARY KEY (relationshipitementitytypeattributeid),
    CONSTRAINT fk_relationshipitem_typeattributeid FOREIGN KEY (relationshipitemid) REFERENCES relationshipitem (relationshipitemid),
    CONSTRAINT fk_relationshipitem_entitytypeattributeid FOREIGN KEY (trackedentitytypeattributeid) REFERENCES trackedentitytypeattribute (trackedentitytypeattributeid)

    );

-- relationshipitemtrackedentityattribute table
CREATE TABLE IF NOT EXISTS relationshipitemtrackedentityattribute
(
    relationshipitemtrackedentityattributeid               bigint            NOT NULL,
    relationshipitemid                                     integer            NOT NULL,
    trackedentityattributeid                               bigint            NOT NULL,

    -- CONTRAINTS
    CONSTRAINT relationshipitemtrackedentityattributeid_pkey PRIMARY KEY (relationshipitemtrackedentityattributeid),
    CONSTRAINT fk_relationshipitem_attributeid FOREIGN KEY (relationshipitemid) REFERENCES relationshipitem (relationshipitemid),
    CONSTRAINT fk_relationshipitem_entityattributeid FOREIGN KEY (trackedentityattributeid) REFERENCES trackedentityattribute (trackedentityattributeid)

    );

-- relationshipitemdataelement table
CREATE TABLE IF NOT EXISTS relationshipitemdataelement
(
    relationshipitemdataelementid               bigint            NOT NULL,
    relationshipitemid                          integer            NOT NULL,
    dataelementid                               bigint            NOT NULL,

    -- CONTRAINTS
    CONSTRAINT relationshipitemdataelement_pkey PRIMARY KEY (relationshipitemdataelementid),
    CONSTRAINT fk_relationshipitem_deid FOREIGN KEY (relationshipitemid) REFERENCES relationshipitem (relationshipitemid),
    CONSTRAINT fk_relationshipitem_dataelementid FOREIGN KEY (dataelementid) REFERENCES dataelement (dataelementid)

    );