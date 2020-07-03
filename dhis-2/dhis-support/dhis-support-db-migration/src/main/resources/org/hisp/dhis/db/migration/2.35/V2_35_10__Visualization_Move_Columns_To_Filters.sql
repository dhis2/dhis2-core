
-- This script moves all visualization columns of "types" 'YEAR_OVER_YEAR_COLUMN' and 'YEAR_OVER_YEAR_LINE'
-- and "dimension" 'dx' into visualization filters. This is done by the store procedure below.
-- This is achieved, basically, by moving records from visualization_columns table into visualization_filters table.
-- For more details, see the ticket: DHIS2-9106


-- Store procedure to move visualization columns into filters.
DO $$

DECLARE
	column_row record;
	filter_row record;
	sort_order int;
	debug bool := false;

BEGIN
	-- Iterate through each row, in columns table, that matches "type" equals 'YEAR_OVER_YEAR_COLUMN' or 'YEAR_OVER_YEAR_LINE', and dimension = "dx".
	FOR column_row IN (
		-- Selecting the columns to be moved into filters.
		SELECT vc.visualizationid, vc.dimension, vc.sort_order FROM visualization_columns vc WHERE vc.visualizationid IN
		(SELECT v.visualizationid FROM visualization v
			WHERE UPPER(COALESCE(v.type, '')) = 'YEAR_OVER_YEAR_COLUMN' OR UPPER(COALESCE(v.type, '')) = 'YEAR_OVER_YEAR_LINE') AND LOWER(COALESCE(vc.dimension)) = 'dx'
	) LOOP
		IF debug THEN
			RAISE INFO '%','column: '||column_row;
		END IF;

		-- Get the greater sort_order, in filters table, for the current visualization id.
		SELECT MAX(vf.sort_order) INTO sort_order FROM visualization_filters vf WHERE vf.visualizationid = column_row.visualizationid;

		IF debug THEN
			RAISE INFO '%','sort_order: '||sort_order;
		END IF;

		-- Incrementing sort_order so it can be used during the visualization_filters insert.
		sort_order := sort_order + 1;

		-- Before inserting the current column, check if this column isn't already present in filters table.
		SELECT * INTO filter_row FROM visualization_filters WHERE visualizationid = column_row.visualizationid AND LOWER(dimension) = LOWER(column_row.dimension);
		IF NOT FOUND THEN -- it refers to the 'filter_row' var
			IF debug THEN
				RAISE INFO '%','found and sort_order++: '||sort_order;
			END IF;

            -- Insert the current column into filters table.
			INSERT INTO visualization_filters (visualizationid, dimension, sort_order)
			VALUES (column_row.visualizationid, LOWER(column_row.dimension), sort_order );

	        -- Once this column is copied into filters, remove it from columns table. The "moving" process is finally concluded for this particular column.
	        DELETE FROM visualization_columns vc
	        WHERE vc.visualizationid = column_row.visualizationid
	        AND LOWER(COALESCE(vc.dimension, '')) = LOWER(column_row.dimension)
	        AND vc.sort_order = column_row.sort_order;
		END IF;
	END LOOP;
END;

$$ LANGUAGE plpgsql;
