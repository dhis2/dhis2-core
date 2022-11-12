ALTER TABLE visualization ADD COLUMN IF NOT EXISTS expressiondimensionitemid bigint;
ALTER TABLE visualization ADD CONSTRAINT fk_visualization_expressiondimensionitem FOREIGN KEY (expressiondimensionitemid)
REFERENCES public.expressiondimensionitem (expressionid) MATCH SIMPLE
ON UPDATE NO ACTION
ON DELETE NO ACTION;

CREATE UNIQUE INDEX uq_visualization_expressiondimensionitemid
    ON public.visualization USING btree
    (expressiondimensionitemid ASC NULLS LAST);