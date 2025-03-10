-- Support program indicator disaggregation

ALTER TABLE program ADD COLUMN IF NOT EXISTS categorymappings jsonb default '[]'::jsonb;

ALTER TABLE programindicator ADD COLUMN IF NOT EXISTS aggregateexportdataelement varchar(255);
ALTER TABLE programindicator ADD COLUMN IF NOT EXISTS categorycomboid bigint;
ALTER TABLE programindicator ADD COLUMN IF NOT EXISTS attributecomboid bigint;
ALTER TABLE programindicator ADD COLUMN IF NOT EXISTS categorymappingids jsonb default '[]'::jsonb;
ALTER TABLE programindicator DROP CONSTRAINT IF EXISTS fk_programindicator_categorycomboid;
ALTER TABLE programindicator DROP CONSTRAINT IF EXISTS fk_programindicator_attributecomboid;
ALTER TABLE programindicator
    ADD CONSTRAINT fk_programindicator_categorycomboid FOREIGN KEY (categorycomboid)
        REFERENCES categorycombo(categorycomboid);
ALTER TABLE programindicator
    ADD CONSTRAINT fk_programindicator_attributecomboid FOREIGN KEY (attributecomboid)
        REFERENCES categorycombo(categorycomboid);

UPDATE programindicator
SET categorycomboid = (SELECT categorycomboid FROM categorycombo WHERE name = 'default' LIMIT 1)
WHERE categorycomboid IS NULL;

UPDATE programindicator
SET attributecomboid = (SELECT categorycomboid FROM categorycombo WHERE name = 'default' LIMIT 1)
WHERE attributecomboid IS NULL;

ALTER TABLE programindicator ALTER COLUMN categorycomboid SET NOT NULL;
ALTER TABLE programindicator ALTER COLUMN attributecomboid SET NOT NULL;
