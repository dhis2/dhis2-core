-- This script is responsible for migrating and unifying existing axes and legend data into new JSONB columns.
-- See Feature DHIS2-10054.

-- What it does?
-- 1) creates new JSONB columns("legend" and "axes") into visualization table;
-- 2) runs a store procedure to move data/columns into the new JSONB columns("legend" and "axes").

-- Check if columns do not exist yet. Execute the migration ONLY if the columns are not present.
DO
$$
    DECLARE
        has_legend_column bool;
        has_axes_column bool;
        r record;
        legendJson json;
        axesJson json;
        axisIndex int;
        targetLineJson json;
        baseLineJson json;
        has_legend bool;
        has_range bool;
        has_domain bool;
        debug bool := FALSE;

    BEGIN
        has_legend_column := (SELECT EXISTS (SELECT 1
                                FROM information_schema.columns
                                WHERE table_name='visualization' AND column_name='legend'));

        has_axes_column := (SELECT EXISTS (SELECT 1
                                FROM information_schema.columns
                                WHERE table_name='visualization' AND column_name='axes'));

        IF has_legend_column = FALSE AND has_axes_column = FALSE THEN
            -- Step 1) creates new JSONB columns("legend" and "axes") into visualization table;
            alter table visualization add column if not exists "legend" jsonb;
            alter table visualization add column if not exists "axes" jsonb;

            -- Step 2) runs a store procedure to move data/columns into the new JSONB columns("legend" and "axes").
            FOR r IN (
                SELECT visualizationid, baseLineLabel, baseLineValue, domainAxisLabel,
                       rangeAxisDecimals, rangeAxisLabel, rangeAxisMaxValue, rangeAxisMinValue,
                       rangeAxisSteps, targetLineLabel, targetLineValue, subtitle, title, hideLegend,
                       fontStyle -> 'baseLineLabel' AS baseLineLabelFontStyle,
                       fontStyle -> 'targetLineLabel' AS targetLineLabelFontStyle,
                       fontStyle -> 'seriesAxisLabel' AS seriesAxisLabelFontStyle,
                       fontStyle -> 'verticalAxisTitle' AS verticalAxisTitleFontStyle,
                       fontStyle -> 'categoryAxisLabel' AS categoryAxisLabelFontStyle,
                       fontStyle -> 'horizontalAxisTitle' AS horizontalAxisTitleFontStyle,
                       fontStyle -> 'legend' AS legendFontStyle
                FROM visualization v
            ) LOOP
                -- Ensure global FLAGS are reset.
                axisIndex := 0;
                axesJson := '[{},{}]';
                targetLineJson := NULL;
                baseLineJson := NULL;
                has_legend := FALSE;
                has_range := FALSE;
                has_domain := FALSE;

                IF debug THEN
                    RAISE INFO '%','Migrating data for visualization id: ' || r.visualizationid;
                END IF;

                -- Migrate "legend"
                legendJson := '{}';

                IF r.legendFontStyle IS NOT NULL THEN
                    legendJson := jsonb_set(legendJson::jsonb, '{label}', format('{"fontStyle":%s}', r.legendFontStyle)::jsonb);
                    has_legend := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Legend:label: ', legendJson);
                    END IF;
                END IF;

                IF r.hideLegend IS NOT NULL THEN
                    legendJson := jsonb_set(legendJson::jsonb, '{hidden}'::TEXT[], r.hideLegend::TEXT::jsonb);
                    legendJson := json_strip_nulls(legendJson);
                    has_legend := TRUE;

                    IF debug THEN
                        RAISE INFO '%', 'Legend:hidden: ' || legendJson;
                    END IF;
                END IF;

                IF has_legend THEN
                    IF debug THEN
                        RAISE INFO '%', CONCAT('Updating legend column with: ', legendJson);
                    END IF;

                    UPDATE visualization SET legend = legendJson WHERE visualizationid = r.visualizationid;
                END IF;


                -- Migrate "axes"

                -- Axis RANGE
                IF r.seriesAxisLabelFontStyle IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',label}')::TEXT[], format('{"fontStyle":%s}', r.seriesAxisLabelFontStyle)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, label: ', axesJson);
                    END IF;
                END IF;

                IF (COALESCE(r.rangeAxisLabel, '') != '' AND r.verticalAxisTitleFontStyle IS NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s"}', r.rangeAxisLabel)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, rangeAxisLabel: ', axesJson);
                    END IF;
                ELSEIF (COALESCE(r.rangeAxisLabel, '') = '' AND r.verticalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"fontStyle":%s}', r.verticalAxisTitleFontStyle)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, verticalAxisTitleFontStyle: ', axesJson);
                    END IF;
                ELSEIF (COALESCE(r.rangeAxisLabel, '') != '' AND r.verticalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s", "fontStyle":%s}', r.rangeAxisLabel, r.verticalAxisTitleFontStyle)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, rangeAxisLabel, verticalAxisTitleFontStyle: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisDecimals IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',decimals}')::TEXT[], to_jsonb(r.rangeAxisDecimals));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, decimals: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisMaxValue IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',maxValue}')::TEXT[], to_jsonb(r.rangeAxisMaxValue));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, maxValue: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisMinValue IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',minValue}')::TEXT[], to_jsonb(r.rangeAxisMinValue));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, minValue: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisSteps IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',steps}')::TEXT[], to_jsonb(r.rangeAxisSteps));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, steps: ', axesJson);
                    END IF;
                END IF;

                IF (COALESCE(r.baseLineLabel, '') != '' AND r.baseLineLabelFontStyle IS NULL) THEN
                    baseLineJson := format('{"title": {"text":"%s"}}', r.baseLineLabel)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineLabel: ', baseLineJson);
                    END IF;
                ELSEIF (COALESCE(r.baseLineLabel, '') = '' AND r.baseLineLabelFontStyle IS NOT NULL) THEN
                    baseLineJson := format('{"title": {"fontStyle":%s}}', r.baseLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineLabelFontStyle: ', baseLineJson);
                    END IF;
                ELSEIF (COALESCE(r.baseLineLabel, '') != '' AND r.baseLineLabelFontStyle IS NOT NULL) THEN
                    baseLineJson := format('{"title": {"text":"%s", "fontStyle":%s}}', r.baseLineLabel, r.baseLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineLabel, baseLineLabelFontStyle: ', baseLineJson);
                    END IF;
                END IF;

                IF r.baseLineValue IS NOT NULL THEN
                    IF baseLineJson IS NOT NULL THEN
                        baseLineJson := jsonb_insert(baseLineJson::jsonb, ('{value}')::TEXT[], to_jsonb(r.baseLineValue));
                    ELSE
                        baseLineJson := format('{"value":%s}', r.baseLineValue)::jsonb;
                    END IF;

                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineValue: ', baseLineJson);
                    END IF;
                END IF;

                IF (COALESCE(r.targetLineLabel, '') != '' AND r.targetLineLabelFontStyle IS NULL) THEN
                    targetLineJson := format('{"title": {"text":"%s"}}', r.targetLineLabel)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineLabel: ', targetLineJson);
                    END IF;
                ELSEIF (COALESCE(r.targetLineLabel, '') = '' AND r.targetLineLabelFontStyle IS NOT NULL) THEN
                    targetLineJson := format('{"title": {"fontStyle":%s}}', r.targetLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineLabelFontStyle: ', targetLineJson);
                    END IF;
                ELSEIF (COALESCE(r.targetLineLabel, '') != '' AND r.targetLineLabelFontStyle IS NOT NULL) THEN
                    targetLineJson := format('{"title": {"text":"%s", "fontStyle":%s}}', r.targetLineLabel, r.targetLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineLabel, targetLineLabelFontStyle: ', targetLineJson);
                    END IF;
                END IF;

                IF r.targetLineValue IS NOT NULL THEN
                    IF targetLineJson IS NOT NULL THEN
                        targetLineJson := jsonb_insert(targetLineJson::jsonb, ('{value}')::TEXT[], to_jsonb(r.targetLineValue));
                    ELSE
                        targetLineJson := format('{"value":%s}', r.targetLineValue)::jsonb;
                    END IF;

                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineValue: ', targetLineJson);
                    END IF;
                END IF;

                IF has_range THEN
                    IF targetLineJson IS NOT NULL THEN
                        IF debug THEN
                            RAISE INFO '%', CONCAT('Target line body: ', targetLineJson);
                        END IF;

                        axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',targetLine}')::TEXT[], targetLineJson::jsonb);
                    END IF;

                    IF baseLineJson IS NOT NULL THEN
                        IF debug THEN
                            RAISE INFO '%', CONCAT('Base line body: ', baseLineJson);
                        END IF;

                        axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',baseLine}')::TEXT[], baseLineJson::jsonb);
                    END IF;

                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',index}')::TEXT[], to_jsonb(axisIndex));
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',type}')::TEXT[], to_jsonb('RANGE'::TEXT));

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes json RANGE: ', axesJson);
                    END IF;

                    axisIndex := axisIndex + 1;
                END IF;

                -- Axis DOMAIN
                IF r.categoryAxisLabelFontStyle IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',label}')::TEXT[], format('{"fontStyle":%s}', r.categoryAxisLabelFontStyle)::jsonb);
                    has_domain := TRUE;
                END IF;

                IF (COALESCE(r.domainAxisLabel, '') != '' AND r.horizontalAxisTitleFontStyle IS NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s"}', r.domainAxisLabel)::jsonb);
                    has_domain := TRUE;
                ELSEIF (COALESCE(r.domainAxisLabel, '') = '' AND r.horizontalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"fontStyle":%s}', r.horizontalAxisTitleFontStyle)::jsonb);
                    has_domain := TRUE;
                ELSEIF (COALESCE(r.domainAxisLabel, '') != '' AND r.horizontalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s", "fontStyle":%s}', r.domainAxisLabel, r.horizontalAxisTitleFontStyle)::jsonb);
                    has_domain := TRUE;
                END IF;

                IF has_domain THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',index}')::TEXT[], to_jsonb(axisIndex));
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',type}')::TEXT[], to_jsonb('DOMAIN'::TEXT));

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes json DOMAIN: ', axesJson);
                    END IF;
                END IF;

                IF (has_domain OR has_range) THEN
                    IF axesJson IS NOT NULL AND axesJson::TEXT != '[{},{}]' THEN
                        -- It means we have some axis to persist.

                        axesJson := json_strip_nulls(axesJson);

                        -- Remove empty elements if any.
                        IF ((axesJson::json->0)::TEXT = '{}') THEN
                            axesJson := (axesJson::jsonb - 0);
                        END IF;

                        IF ((axesJson::json->1)::TEXT = '{}') THEN
                            axesJson := (axesJson::jsonb - 1);
                        END IF;

                        IF debug THEN
                            RAISE INFO '%', CONCAT('Updating axes column with: ', axesJson);
                        END IF;

                        UPDATE visualization SET axes = axesJson WHERE visualizationid = r.visualizationid;
                    END IF;
                END IF;
            END LOOP;
        END IF;
    END
$$ LANGUAGE plpgsql
