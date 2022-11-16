ALTER TABLE IF EXISTS datadimensionitem
    ADD COLUMN expressiondimensionitemid bigint;
ALTER TABLE IF EXISTS datadimensionitem ADD CONSTRAINT fk_datadimensionitem_expressiondimensionitemid FOREIGN KEY (expressiondimensionitemid)
        REFERENCES expressiondimensionitem (expressiondimensionitemid) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
