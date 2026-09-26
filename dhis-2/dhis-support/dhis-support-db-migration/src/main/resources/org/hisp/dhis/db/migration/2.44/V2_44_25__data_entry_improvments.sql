-- create a view of the valid COC for a DS+DE combination
CREATE OR REPLACE VIEW v_dataentry_cocs_of_ds_de AS
SELECT
    ds.uid AS ds_uid,
    de.uid AS de_uid,
    COALESCE(
            array_agg(DISTINCT coc.uid),
            '{}'::varchar(11)[]
    ) AS coc_uids
FROM datasetelement dse
JOIN dataelement de ON de.dataelementid = dse.dataelementid
JOIN dataset     ds ON ds.datasetid     = dse.datasetid
JOIN categorycombos_optioncombos coc_cc ON coc_cc.categorycomboid = COALESCE(dse.categorycomboid, de.categorycomboid)
JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid = coc_cc.categoryoptioncomboid
GROUP BY ds.uid, de.uid;