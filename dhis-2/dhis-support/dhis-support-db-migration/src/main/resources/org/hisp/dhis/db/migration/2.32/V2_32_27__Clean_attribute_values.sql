WITH nonRepeatedIds AS (
    SELECT
        min(av2.attributevalueid) AS id
    FROM
        attributevalue av2
    LEFT JOIN dataelementattributevalues deav2 ON av2.attributevalueid = deav2.attributevalueid
GROUP BY
    deav2.dataelementid,
    av2.value,
    av2.attributeid
),
uniqueAttributeIds AS (
    SELECT
        av.attributevalueid AS id
    FROM
        attributevalue av
        JOIN dataelementattributevalues deav ON av.attributevalueid = deav.attributevalueid
    WHERE (dataelementid, attributeid, value)
    IN (
        SELECT
            deav2.dataelementid, av2.attributeid, max(av2.value) AS value
        FROM
            attributevalue av2
        LEFT JOIN dataelementattributevalues deav2 ON av2.attributevalueid = deav2.attributevalueid
    GROUP BY
        deav2.dataelementid,
        av2.attributeid)
),
orphanAttributeValues AS (
    SELECT
        av.attributevalueid AS id
    FROM
        attributevalue av
    LEFT JOIN dataelementattributevalues deav ON av.attributevalueid = deav.attributevalueid
WHERE
    deav.dataelementid IS NULL
),
relationsDeleted AS (
    DELETE FROM dataelementattributevalues deav
    WHERE deav.attributevalueid NOT IN (
            SELECT
                id
            FROM
                nonRepeatedIds
            UNION
            SELECT
                id
            FROM
                uniqueAttributeIds)
        RETURNING
            attributevalueid
),
categoryOptionComboDeleted AS (
    DELETE FROM categoryoptioncomboattributevalues
    WHERE attributevalueid IN (
            SELECT
                id
            FROM
                orphanAttributeValues)
),
indicatorDeleted AS (
    DELETE FROM indicatorattributevalues
    WHERE attributevalueid IN (
            SELECT
                id
            FROM
                orphanAttributeValues)
),
attributeValuesDeleted AS (
    DELETE FROM attributevalue
    WHERE attributevalueid IN (
            SELECT
                id
            FROM
                orphanAttributeValues)
) DELETE FROM attributevalue av
WHERE av.attributevalueid NOT IN (
        SELECT
            id
        FROM
            nonRepeatedIds
        UNION
        SELECT
            id
        FROM
            uniqueAttributeIds);

