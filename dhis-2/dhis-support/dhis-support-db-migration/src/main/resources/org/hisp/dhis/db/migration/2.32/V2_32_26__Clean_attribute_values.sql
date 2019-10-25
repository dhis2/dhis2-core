
WITH nonRepeatedIds AS (select min(av2.attributevalueid) as id from attributevalue av2 LEFT JOIN dataelementattributevalues deav2 ON av2.attributevalueid = deav2.attributevalueid
        group by deav2.dataelementid, av2.value, av2.attributeid),
    uniqueAttributeIds AS (select av.attributevalueid as id from attributevalue av JOIN dataelementattributevalues deav ON av.attributevalueid = deav.attributevalueid WHERE (dataelementid, attributeid, value) IN
                            (select deav2.dataelementid, av2.attributeid, max(av2.value) as value from attributevalue av2 LEFT JOIN dataelementattributevalues deav2 ON av2.attributevalueid = deav2.attributevalueid
                    group by deav2.dataelementid, av2.attributeid)),
    orphanAttributeValues AS (select av.attributevalueid as id from attributevalue av LEFT JOIN dataelementattributevalues deav ON av.attributevalueid = deav.attributevalueid WHERE deav.dataelementid IS NULL),
    relationsDeleted AS (delete from dataelementattributevalues deav where deav.attributevalueid NOT IN (SELECT id FROM nonRepeatedIds UNION SELECT id FROM uniqueAttributeIds) returning attributevalueid),
    categoryOptionComboDeleted AS (delete from categoryoptioncomboattributevalues where attributevalueid in (select id from orphanAttributeValues)),
    indicatorDeleted AS (delete from indicatorattributevalues where attributevalueid in (select id from orphanAttributeValues)),
    attributeValuesDeleted AS (delete from attributevalue where attributevalueid in (select id from orphanAttributeValues))
delete from attributevalue av
where av.attributevalueid NOT IN (SELECT id FROM nonRepeatedIds UNION SELECT id FROM uniqueAttributeIds);