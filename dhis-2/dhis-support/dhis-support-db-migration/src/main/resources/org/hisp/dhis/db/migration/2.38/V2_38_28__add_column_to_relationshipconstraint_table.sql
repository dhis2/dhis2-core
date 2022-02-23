-- trackerdataview table
CREATE TABLE IF NOT EXISTS trackerdataview
(
    trackerdataviewid                               bigint                          NOT NULL,
    trackedentitytypeid                             bigint,
    programid                                       bigint,
    programstageid                                  bigint,

    -- CONTRAINTS
    CONSTRAINT trackerdataviewid_pkey PRIMARY KEY (trackerdataviewid),
    CONSTRAINT fk_trackerdataview_trackedentitytypeid    FOREIGN KEY (trackedentitytypeid)       REFERENCES trackedentitytype (trackedentitytypeid),
    CONSTRAINT fk_trackerdataview_programid              FOREIGN KEY (programid)                 REFERENCES program (programid),
    CONSTRAINT fk_trackerdataview_programstageid         FOREIGN KEY (programstageid)            REFERENCES programstage (programstageid)
);

-- trackerdataviewitem table
CREATE TABLE IF NOT EXISTS trackerdataviewitem
(
    trackerdataviewitemid                           bigint             NOT NULL,
    trackerdataviewid                               bigint             NOT NULL,
    trackedentityattributeid                        bigint,
    dataelementid                                   bigint,

    -- CONTRAINTS
    CONSTRAINT trackerdataviewitemid_pkey PRIMARY KEY (trackerdataviewitemid),
    CONSTRAINT trackerdataviewid                                 FOREIGN KEY (trackerdataviewid)         REFERENCES trackerdataview (trackerdataviewid),
    CONSTRAINT fk_trackerdataviewitem_trackedentityattributeid   FOREIGN KEY (trackedentityattributeid)  REFERENCES trackedentityattribute (trackedentityattributeid),
    CONSTRAINT fk_trackerdataviewitem_dataelementid              FOREIGN KEY (dataelementid)             REFERENCES dataelement (dataelementid)
);


alter table relationshipconstraint
    add column if not exists trackerdataviewid bigint;

alter table relationshipconstraint
    add constraint fk_relationshipconstraint_trackerdataviewid
        foreign key (trackerdataviewid) references trackerdataview(trackerdataviewid);


