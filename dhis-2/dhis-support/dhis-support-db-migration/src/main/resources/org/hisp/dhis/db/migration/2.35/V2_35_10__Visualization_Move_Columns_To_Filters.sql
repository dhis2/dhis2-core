
-- This script moves all visualization columns of "type" 'YEAR_OVER_YEAR_COLUMN' and 'YEAR_OVER_YEAR_LINE'
-- and "dimension" 'dx' into visualization filters. This is done by the store procedure below.
-- This is achieved, basically, by moving records from visualization_columns table into visualization_filters table.
-- For more details, see the ticket: DHIS2-9106


-- Store procedure to move visualization columns into filters.
DO $$

DECLARE
	-- Used for syncing-up sort_order values in visualization_columns table.
	column_row record;
	filter_row record;
	sort_order int;

	-- Used for synching-up sort_order values in visualization_columns table.
 	current_column record;
	new_sort_order int := 0; -- Zero is the base value. See Visualization.hbm.xml.
	total_sort_orders_updated bigint := 0;
	total_sort_orders_refused bigint := 0;

	-- Debug variable
	debug bool := true;

BEGIN
	-- Iterate through each row, in columns table, that matches "type" equals 'YEAR_OVER_YEAR_COLUMN' or 'YEAR_OVER_YEAR_LINE', and dimension = "dx".
	FOR column_row IN (
		-- Selecting the columns to be moved to filters.
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

	        -- Once the columns is copied into filters, remove it from columns table. The "moving" process is concluded for this column.
	        DELETE FROM visualization_columns vc
	        WHERE vc.visualizationid = column_row.visualizationid
	        AND LOWER(COALESCE(vc.dimension, '')) = LOWER(column_row.dimension)
	        AND vc.sort_order = column_row.sort_order;
		END IF;
	END LOOP;

    -- Now reorganize all sort_order values, for each column, to fill-in eventual gaps of previously removed columns.
    -- For each visualization id present in the columns table.
    FOR column_row IN SELECT DISTINCT(visualizationid)
        FROM visualization_columns ORDER BY visualizationid ASC
    LOOP
    	-- For each column related to the current visualization id.
        FOR current_column IN SELECT vc.visualizationid, vc.sort_order, vc.dimension
            FROM visualization_columns vc WHERE visualizationid = column_row.visualizationid ORDER BY vc.sort_order ASC
        LOOP
        	-- Try to update the current column by setting the correct sort_order.
        	BEGIN
	        	UPDATE visualization_columns SET sort_order=new_sort_order WHERE visualizationid=current_column.visualizationid AND LOWER(COALESCE(dimension, '')) = LOWER(current_column.dimension);

	        	IF debug THEN
		        	RAISE INFO '%','Updating sort_order for visualization: '||column_row.visualizationid;
	        	END IF;

	            total_sort_orders_updated := total_sort_orders_updated + 1;
            EXCEPTION WHEN OTHERS THEN
            	-- If the update fails it's because the pair visualizationid/sort_order relation is already present, so skip it.
               	total_sort_orders_refused := total_sort_orders_refused + 1;

				IF debug THEN
	            	RAISE INFO '%','WARN: Relation already exists, skipping: '||'visualizationid: '||current_column.visualizationid||' | sort_order: '||new_sort_order;
	            END IF;
            END;

           	-- Increment the sort_order to be set in the next column row for the current visualization id.
            new_sort_order := new_sort_order + 1;
	    END LOOP;
	    -- Reset sort_oder to the base value so it can be used by the next visualization.
        new_sort_order := 0;
    END LOOP;

	IF debug THEN
		RAISE INFO '%', 'Total of sort_order updates: '||total_sort_orders_updated;
		RAISE INFO '%', 'Total of sort_order refused: '||total_sort_orders_refused;
	END IF;
END;

$$ LANGUAGE plpgsql;
