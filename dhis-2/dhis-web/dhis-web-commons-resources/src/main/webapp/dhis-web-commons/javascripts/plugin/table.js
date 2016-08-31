Ext.onReady( function() {

	// CORE

	// ext config
	Ext.Ajax.method = 'GET';

    Ext.isIE = (/trident/.test(Ext.userAgent));

    Ext.isIE11 = Ext.isIE && (/rv:11.0/.test(Ext.userAgent));

    Ext.util.CSS.createStyleSheet = function(cssText, id) {
        var ss,
            head = document.getElementsByTagName("head")[0],
            styleEl = document.createElement("style");

        styleEl.setAttribute("type", "text/css");

        if (id) {
           styleEl.setAttribute("id", id);
        }

        if (Ext.isIE && !Ext.isIE11) {
           head.appendChild(styleEl);
           ss = styleEl.styleSheet;
           ss.cssText = cssText;
        }
        else {
            try {
                styleEl.appendChild(document.createTextNode(cssText));
            }
            catch(e) {
               styleEl.cssText = cssText;
            }
            head.appendChild(styleEl);
            ss = styleEl.styleSheet ? styleEl.styleSheet : (styleEl.sheet || document.styleSheets[document.styleSheets.length-1]);
        }
        this.cacheStyleSheet(ss);
        return ss;
    };

	// namespace
	PT = {};
	var PT = PT;

	PT.instances = [];
	PT.i18n = {};
	PT.isDebug = false;
	PT.isSessionStorage = ('sessionStorage' in window && window['sessionStorage'] !== null);

	PT.getCore = function(init, appConfig) {
        var conf = {},
            api = {},
            support = {},
            service = {},
            web = {},
            app = {},
            webAlert,
            dimConf;

        appConfig = appConfig || {};

        // alert
        webAlert = function() {};

        // app
        app.getViewportWidth = function() {};
        app.getViewportHeight = function() {};
        app.getCenterRegionWidth = function() {};
        app.getCenterRegionHeight = function() {};

		// conf
		(function() {
			conf.finals = {
				dimension: {
					data: {
						value: 'data',
						name: PT.i18n.data || 'Data',
						dimensionName: 'dx',
						objectName: 'dx'
					},
					category: {
						name: PT.i18n.assigned_categories || 'Assigned categories',
						dimensionName: 'co',
						objectName: 'co',
					},
					indicator: {
						value: 'indicators',
						name: PT.i18n.indicators || 'Indicators',
						dimensionName: 'dx',
						objectName: 'in'
					},
					dataElement: {
						value: 'dataElements',
						name: PT.i18n.data_elements || 'Data elements',
						dimensionName: 'dx',
						objectName: 'de'
					},
					operand: {
						value: 'operand',
						name: 'Operand',
						dimensionName: 'dx',
						objectName: 'dc'
					},
					dataSet: {
						value: 'dataSets',
						name: PT.i18n.data_sets || 'Data sets',
						dimensionName: 'dx',
						objectName: 'ds'
					},
					eventDataItem: {
						value: 'eventDataItem',
						name: PT.i18n.event_data_items || 'Event data items',
						dimensionName: 'dx',
						objectName: 'di'
					},
					programIndicator: {
						value: 'programIndicator',
						name: PT.i18n.program_indicators || 'Program indicators',
						dimensionName: 'dx',
						objectName: 'pi'
					},
					period: {
						value: 'period',
						name: PT.i18n.periods || 'Periods',
						dimensionName: 'pe',
						objectName: 'pe'
					},
					fixedPeriod: {
						value: 'periods'
					},
					relativePeriod: {
						value: 'relativePeriods'
					},
					organisationUnit: {
						value: 'organisationUnits',
						name: PT.i18n.organisation_units || 'Organisation units',
						dimensionName: 'ou',
						objectName: 'ou'
					},
					dimension: {
						value: 'dimension'
						//objectName: 'di'
					},
					value: {
						value: 'value'
					}
				},
				root: {
					id: 'root'
				},
                style: {
                    'normal': 'NORMAL',
                    'compact': 'COMPACT',
                    'xcompact': 'XCOMPACT',
                    'comfortable': 'COMFORTABLE',
                    'xcomfortable': 'XCOMFORTABLE',
                    'small': 'SMALL',
                    'xsmall': 'XSMALL',
                    'large': 'LARGE',
                    'xlarge': 'XLARGE',
                    'space': 'SPACE',
                    'comma': 'COMMA',
                    'none': 'NONE',
                    'default_': 'DEFAULT'
                }
			};

            (function() {
                dimConf = conf.finals.dimension;

                dimConf.objectNameMap = {};
                dimConf.objectNameMap[dimConf.data.objectName] = dimConf.data;
                dimConf.objectNameMap[dimConf.indicator.objectName] = dimConf.indicator;
                dimConf.objectNameMap[dimConf.dataElement.objectName] = dimConf.dataElement;
                dimConf.objectNameMap[dimConf.operand.objectName] = dimConf.operand;
                dimConf.objectNameMap[dimConf.dataSet.objectName] = dimConf.dataSet;
                dimConf.objectNameMap[dimConf.category.objectName] = dimConf.category;
                dimConf.objectNameMap[dimConf.period.objectName] = dimConf.period;
                dimConf.objectNameMap[dimConf.organisationUnit.objectName] = dimConf.organisationUnit;
                dimConf.objectNameMap[dimConf.dimension.objectName] = dimConf.dimension;
            })();

			conf.period = {
				periodTypes: [
					{id: 'Daily', name: PT.i18n.daily},
					{id: 'Weekly', name: PT.i18n.weekly},
					{id: 'Monthly', name: PT.i18n.monthly},
					{id: 'BiMonthly', name: PT.i18n.bimonthly},
					{id: 'Quarterly', name: PT.i18n.quarterly},
					{id: 'SixMonthly', name: PT.i18n.sixmonthly},
					{id: 'SixMonthlyApril', name: PT.i18n.sixmonthly_april},
					{id: 'Yearly', name: PT.i18n.yearly},
					{id: 'FinancialOct', name: PT.i18n.financial_oct},
					{id: 'FinancialJuly', name: PT.i18n.financial_july},
					{id: 'FinancialApril', name: PT.i18n.financial_april}
				],
                relativePeriods: []
			};

            conf.valueType = {
            	numericTypes: ['NUMBER','UNIT_INTERVAL','PERCENTAGE','INTEGER','INTEGER_POSITIVE','INTEGER_NEGATIVE','INTEGER_ZERO_OR_POSITIVE'],
            	textTypes: ['TEXT','LONG_TEXT','LETTER','PHONE_NUMBER','EMAIL'],
            	booleanTypes: ['BOOLEAN','TRUE_ONLY'],
            	dateTypes: ['DATE','DATETIME'],
            	aggregateTypes: ['NUMBER','UNIT_INTERVAL','PERCENTAGE','INTEGER','INTEGER_POSITIVE','INTEGER_NEGATIVE','INTEGER_ZERO_OR_POSITIVE','BOOLEAN','TRUE_ONLY']
            };

			conf.layout = {
				west_width: 424,
				west_fieldset_width: 420,
				west_width_padding: 2,
				west_fill: 2,
				west_fill_accordion_indicator: 81,
				west_fill_accordion_dataelement: 81,
				west_fill_accordion_dataset: 56,
                west_fill_accordion_eventdataitem: 81,
                west_fill_accordion_programindicator: 81,
				west_fill_accordion_period: 310,
				west_fill_accordion_organisationunit: 58,
                west_fill_accordion_group: 31,
				west_maxheight_accordion_indicator: 400,
				west_maxheight_accordion_dataelement: 400,
				west_maxheight_accordion_dataset: 400,
				west_maxheight_accordion_period: 513,
				west_maxheight_accordion_organisationunit: 900,
				west_maxheight_accordion_group: 340,
				west_maxheight_accordion_options: 449,
				west_scrollbarheight_accordion_indicator: 300,
				west_scrollbarheight_accordion_dataelement: 300,
				west_scrollbarheight_accordion_dataset: 300,
				west_scrollbarheight_accordion_period: 450,
				west_scrollbarheight_accordion_organisationunit: 450,
				west_scrollbarheight_accordion_group: 300,
				east_tbar_height: 31,
				east_gridcolumn_height: 30,
				form_label_width: 55,
				window_favorite_ypos: 100,
				window_confirm_width: 250,
				window_share_width: 500,
				grid_favorite_width: 420,
				grid_row_height: 27,
				treepanel_minheight: 135,
				treepanel_maxheight: 400,
				treepanel_fill_default: 310,
				treepanel_toolbar_menu_width_group: 140,
				treepanel_toolbar_menu_width_level: 120,
				multiselect_minheight: 100,
				multiselect_maxheight: 250,
				multiselect_fill_default: 345,
				multiselect_fill_reportingrates: 315
			};

			conf.style = {
				displayDensity: {},
				fontSize: {},
				digitGroupSeparator: {}
            };

            (function() {
                var map = conf.finals.style,
                    displayDensity = conf.style.displayDensity,
                    fontSize = conf.style.fontSize,
                    digitGroupSeparator = conf.style.digitGroupSeparator;

                displayDensity[map.xcompact] = '2px';
                displayDensity[map.compact] = '4px';
                displayDensity[map.normal] = '6px';
                displayDensity[map.comfortable] = '8px';
                displayDensity[map.xcomfortable] = '10px';

                fontSize[map.xsmall] = '9px';
                fontSize[map.small] = '10px';
                fontSize[map.normal] = '11px';
                fontSize[map.large] = '12px';
                fontSize[map.xlarge] = '14px';

                digitGroupSeparator[map.space] = '&nbsp;';
                digitGroupSeparator[map.comma] = ',';
                digitGroupSeparator[map.none] = '';
            })();

            conf.url = {
                analysisFields: [
                    '*',
                    'program[id,displayName|rename(name)]',
                    'programStage[id,displayName|rename(name)]',
                    'columns[dimension,filter,items[dimensionItem|rename(id),' + init.namePropertyUrl + ']]',
                    'rows[dimension,filter,items[dimensionItem|rename(id),' + init.namePropertyUrl + ']]',
                    'filters[dimension,filter,items[dimensionItem|rename(id),' + init.namePropertyUrl + ']]',
                    '!lastUpdated',
                    '!href',
                    '!created',
                    '!publicAccess',
                    '!rewindRelativePeriods',
                    '!userOrganisationUnit',
                    '!userOrganisationUnitChildren',
                    '!userOrganisationUnitGrandChildren',
                    '!externalAccess',
                    '!access',
                    '!relativePeriods',
                    '!columnDimensions',
                    '!rowDimensions',
                    '!filterDimensions',
                    '!user',
                    '!organisationUnitGroups',
                    '!itemOrganisationUnitGroups',
                    '!userGroupAccesses',
                    '!indicators',
                    '!dataElements',
                    '!dataElementOperands',
                    '!dataElementGroups',
                    '!dataSets',
                    '!periods',
                    '!organisationUnitLevels',
                    '!organisationUnits'
                ]
            };
		}());

		// api
		(function() {
			api.layout = {};

			api.layout.Record = function(config) {
				var config = Ext.clone(config);

				// id: string

				return function() {
					if (!Ext.isObject(config)) {
						console.log('Record: config is not an object: ' + config);
						return;
					}

					if (!Ext.isString(config.id)) {
						console.log('api.layout.Record: id is not text: ' + config);
						return;
					}

					return config;
				}();
			};

			api.layout.Dimension = function(config) {
				var config = Ext.clone(config);

				// dimension: string

				// items: [Record]

				return function() {
					if (!Ext.isObject(config)) {
						console.log('Dimension: config is not an object: ' + config);
						return;
					}

					if (!Ext.isString(config.dimension)) {
						console.log('Dimension: name is not a string: ' + config);
						return;
					}

					if (config.dimension !== conf.finals.dimension.category.objectName) {
						var records = [];

						if (!Ext.isArray(config.items)) {
							//console.log('Dimension: items is not an array: ' + config);
							return;
						}

						for (var i = 0; i < config.items.length; i++) {
							records.push(api.layout.Record(config.items[i]));
						}

						config.items = Ext.Array.clean(records);

						if (!config.items.length) {
							//console.log('Dimension: has no valid items: ' + config);
							return;
						}
					}

					return config;
				}();
			};

			api.layout.Layout = function(config, applyConfig, forceApplyConfig) {
                config = Ext.apply(config, applyConfig);

				var layout = {},
					getValidatedDimensionArray,
					validateSpecialCases;

				// columns: [Dimension]

				// rows: [Dimension]

				// filters: [Dimension]

				// showRowTotals: boolean (true)

				// showColTotals: boolean (true)

				// showColSubTotals: boolean (true)

				// showRowSubTotals: boolean (true)

                // showDimensionLabels: boolean (false)

				// hideEmptyRows: boolean (false)

                // skipRounding: boolean (false)

                // aggregationType: string ('DEFAULT') - 'DEFAULT', 'COUNT', 'SUM', 'STDDEV', 'VARIANCE', 'MIN', 'MAX'

                // dataApprovalLevel: object

				// showHierarchy: boolean (false)

				// completedOnly: boolean (false)

				// displayDensity: string ('NORMAL') - 'COMPACT', 'NORMAL', 'COMFORTABLE'

				// fontSize: string ('NORMAL') - 'SMALL', 'NORMAL', 'LARGE'

				// digitGroupSeparator: string ('SPACE') - 'NONE', 'COMMA', 'SPACE'

				// legendSet: object

				// parentGraphMap: object

				// sorting: transient object

				// reportingPeriod: boolean (false) //report tables only

				// organisationUnit: boolean (false) //report tables only

				// parentOrganisationUnit: boolean (false) //report tables only

				// regression: boolean (false)

				// cumulative: boolean (false)

				// sortOrder: integer (0) //-1, 0, 1

				// topLimit: integer (100) //5, 10, 20, 50, 100

                // displayProperty: string ('name') // 'name', 'shortname', null

                // userOrgUnit: string

				getValidatedDimensionArray = function(dimensionArray) {
					var dimensionArray = Ext.clone(dimensionArray);

					if (!(dimensionArray && Ext.isArray(dimensionArray) && dimensionArray.length)) {
						return;
					}

					for (var i = 0; i < dimensionArray.length; i++) {
						dimensionArray[i] = api.layout.Dimension(dimensionArray[i]);
					}

					dimensionArray = Ext.Array.clean(dimensionArray);

					return dimensionArray.length ? dimensionArray : null;
				};

				validateSpecialCases = function() {
					var dimConf = conf.finals.dimension,
						dimensions,
						objectNameDimensionMap = {};

					if (!layout) {
						return;
					}

					dimensions = Ext.Array.clean([].concat(layout.columns || [], layout.rows || [], layout.filters || []));

					for (var i = 0; i < dimensions.length; i++) {
						objectNameDimensionMap[dimensions[i].dimension] = dimensions[i];
					}

					if (layout.filters && layout.filters.length) {
						for (var i = 0; i < layout.filters.length; i++) {

							// Indicators as filter
							if (layout.filters[i].dimension === dimConf.indicator.objectName) {
								webAlert(PT.i18n.indicators_cannot_be_specified_as_filter || 'Indicators cannot be specified as filter');
								return;
							}

							// Categories as filter
							if (layout.filters[i].dimension === dimConf.category.objectName) {
								webAlert(PT.i18n.categories_cannot_be_specified_as_filter || 'Categories cannot be specified as filter');
								return;
							}

							// Data sets as filter
							if (layout.filters[i].dimension === dimConf.dataSet.objectName) {
								webAlert(PT.i18n.data_sets_cannot_be_specified_as_filter || 'Data sets cannot be specified as filter');
								return;
							}
						}
					}

					// dc and in
					if (objectNameDimensionMap[dimConf.operand.objectName] && objectNameDimensionMap[dimConf.indicator.objectName]) {
						webAlert('Indicators and detailed data elements cannot be specified together');
						return;
					}

					// dc and de
					if (objectNameDimensionMap[dimConf.operand.objectName] && objectNameDimensionMap[dimConf.dataElement.objectName]) {
						webAlert('Detailed data elements and totals cannot be specified together');
						return;
					}

					// dc and ds
					if (objectNameDimensionMap[dimConf.operand.objectName] && objectNameDimensionMap[dimConf.dataSet.objectName]) {
						webAlert('Data sets and detailed data elements cannot be specified together');
						return;
					}

					// dc and co
					if (objectNameDimensionMap[dimConf.operand.objectName] && objectNameDimensionMap[dimConf.category.objectName]) {
						webAlert('Assigned categories and detailed data elements cannot be specified together');
						return;
					}

                    // in and aggregation type
                    if (objectNameDimensionMap[dimConf.indicator.objectName] && config.aggregationType !== conf.finals.style.default_) {
                        webAlert('Indicators and aggregation types cannot be specified together', true);
                        return;
                    }

					return true;
				};

				return function() {
					var objectNames = [],
						dimConf = conf.finals.dimension;

					// config must be an object
					if (!(config && Ext.isObject(config))) {
						console.log('Layout: config is not an object (' + init.el + ')');
						return;
					}

					//config.columns = getValidatedDimensionArray(config.columns);
					//config.rows = getValidatedDimensionArray(config.rows);
					//config.filters = getValidatedDimensionArray(config.filters);

					// at least one dimension specified as column or row
					if (!(config.columns || config.rows)) {
						webAlert(PT.i18n.at_least_one_dimension_must_be_specified_as_row_or_column);
						return;
					}

					// get object names
					for (var i = 0, dims = Ext.Array.clean([].concat(config.columns || [], config.rows || [], config.filters || [])); i < dims.length; i++) {

						// Object names
						if (api.layout.Dimension(dims[i])) {
							objectNames.push(dims[i].dimension);
						}
					}

					// at least one period
					if (!Ext.Array.contains(objectNames, dimConf.period.objectName)) {
						webAlert(PT.i18n.at_least_one_period_must_be_specified_as_column_row_or_filter);
						return;
					}

					// favorite
					if (config.id) {
						layout.id = config.id;
					}

					if (config.name) {
						layout.name = config.name;
					}

					// layout
					layout.columns = config.columns;
					layout.rows = config.rows;
					layout.filters = config.filters;

					// properties
					layout.showColTotals = Ext.isBoolean(config.colTotals) ? config.colTotals : (Ext.isBoolean(config.showColTotals) ? config.showColTotals : true);
					layout.showRowTotals = Ext.isBoolean(config.rowTotals) ? config.rowTotals : (Ext.isBoolean(config.showRowTotals) ? config.showRowTotals : true);
					layout.showColSubTotals = Ext.isBoolean(config.colSubTotals) ? config.colSubTotals : (Ext.isBoolean(config.showColSubTotals) ? config.showColSubTotals : true);
					layout.showRowSubTotals = Ext.isBoolean(config.rowSubTotals) ? config.rowSubTotals : (Ext.isBoolean(config.showRowSubTotals) ? config.showRowSubTotals : true);
					layout.showDimensionLabels = Ext.isBoolean(config.showDimensionLabels) ? config.showDimensionLabels : (Ext.isBoolean(config.showDimensionLabels) ? config.showDimensionLabels : true);
					layout.hideEmptyRows = Ext.isBoolean(config.hideEmptyRows) ? config.hideEmptyRows : false;
                    layout.skipRounding = Ext.isBoolean(config.skipRounding) ? config.skipRounding : false;
                    layout.aggregationType = Ext.isString(config.aggregationType) ? config.aggregationType : conf.finals.style.default_;
					layout.dataApprovalLevel = Ext.isObject(config.dataApprovalLevel) && Ext.isString(config.dataApprovalLevel.id) ? config.dataApprovalLevel : null;

					layout.showHierarchy = Ext.isBoolean(config.showHierarchy) ? config.showHierarchy : false;

                    layout.completedOnly = Ext.isBoolean(config.completedOnly) ? config.completedOnly : false;

					layout.displayDensity = Ext.isString(config.displayDensity) && !Ext.isEmpty(config.displayDensity) ? config.displayDensity : conf.finals.style.normal;
					layout.fontSize = Ext.isString(config.fontSize) && !Ext.isEmpty(config.fontSize) ? config.fontSize : conf.finals.style.normal;
					layout.digitGroupSeparator = Ext.isString(config.digitGroupSeparator) && !Ext.isEmpty(config.digitGroupSeparator) ? config.digitGroupSeparator : conf.finals.style.space;
					layout.legendSet = Ext.isObject(config.legendSet) && Ext.isString(config.legendSet.id) ? config.legendSet : null;

					layout.parentGraphMap = Ext.isObject(config.parentGraphMap) ? config.parentGraphMap : null;

					layout.sorting = Ext.isObject(config.sorting) && Ext.isDefined(config.sorting.id) && Ext.isString(config.sorting.direction) ? config.sorting : null;

					layout.reportingPeriod = Ext.isObject(config.reportParams) && Ext.isBoolean(config.reportParams.paramReportingPeriod) ? config.reportParams.paramReportingPeriod : (Ext.isBoolean(config.reportingPeriod) ? config.reportingPeriod : false);
					layout.organisationUnit =  Ext.isObject(config.reportParams) && Ext.isBoolean(config.reportParams.paramOrganisationUnit) ? config.reportParams.paramOrganisationUnit : (Ext.isBoolean(config.organisationUnit) ? config.organisationUnit : false);
					layout.parentOrganisationUnit =  Ext.isObject(config.reportParams) && Ext.isBoolean(config.reportParams.paramParentOrganisationUnit) ? config.reportParams.paramParentOrganisationUnit : (Ext.isBoolean(config.parentOrganisationUnit) ? config.parentOrganisationUnit : false);

					layout.regression = Ext.isBoolean(config.regression) ? config.regression : false;
					layout.cumulative = Ext.isBoolean(config.cumulative) ? config.cumulative : false;
					layout.sortOrder = Ext.isNumber(config.sortOrder) ? config.sortOrder : 0;
					layout.topLimit = Ext.isNumber(config.topLimit) ? config.topLimit : 0;

                    if (Ext.isString(config.displayProperty)) {
                        layout.displayProperty = config.displayProperty;
                    }

                    if (Ext.Array.from(config.userOrgUnit).length) {
                        layout.userOrgUnit = Ext.Array.from(config.userOrgUnit);
                    }

                    // TODO program
                    if (Ext.isObject(config.program)) {
                        layout.program = config.program;
                    }

                    // relative period date
                    if (support.prototype.date.getYYYYMMDD(config.relativePeriodDate)) {
                        layout.relativePeriodDate = support.prototype.date.getYYYYMMDD(config.relativePeriodDate);
                    }

                    // validate
					if (!validateSpecialCases()) {
						return;
					}

                    return Ext.apply(layout, forceApplyConfig);
				}();
			};

			api.response = {};

			api.response.Header = function(config) {
				var config = Ext.clone(config);

				// name: string

				// meta: boolean

				return function() {
					if (!Ext.isObject(config)) {
						console.log('Header: config is not an object: ' + config);
						return;
					}

					if (!Ext.isString(config.name)) {
						console.log('Header: name is not a string: ' + config);
						return;
					}

					if (!Ext.isBoolean(config.meta)) {
						console.log('Header: meta is not boolean: ' + config);
						return;
					}

					return config;
				}();
			};

			api.response.Response = function(config) {
				var config = Ext.clone(config);

				// headers: [Header]

				return function() {
					if (!(config && Ext.isObject(config))) {
						console.log('Response: config is not an object');
						return;
					}

					if (!(config.headers && Ext.isArray(config.headers))) {
						console.log('Response: headers is not an array');
						return;
					}

					for (var i = 0, header; i < config.headers.length; i++) {
						config.headers[i] = api.response.Header(config.headers[i]);
					}

					config.headers = Ext.Array.clean(config.headers);

					if (!config.headers.length) {
						console.log('Response: no valid headers');
						return;
					}

					if (!(Ext.isArray(config.rows) && config.rows.length > 0)) {
						//console.log('No values found');
						//return;
					}

					if (config.rows.length > 0 && config.headers.length !== config.rows[0].length) {
						console.log('Response: headers.length !== rows[0].length');
					}

					return config;
				}();
			};
		}());

		// support
		(function() {

			// prototype
			support.prototype = {};

				// array
			support.prototype.array = {};

			support.prototype.array.getLength = function(array, suppressWarning) {
				if (!Ext.isArray(array)) {
					if (!suppressWarning) {
						console.log('support.prototype.array.getLength: not an array');
					}

					return null;
				}

				return array.length;
			};

			support.prototype.array.sort = function(array, direction, key, emptyFirst) {
				// supports [number], [string], [{key: number}], [{key: string}], [[string]], [[number]]

				if (!support.prototype.array.getLength(array)) {
					return;
				}

				key = !!key || Ext.isNumber(key) ? key : 'name';

				array.sort( function(a, b) {

					// if object, get the property values
					if (Ext.isObject(a) && Ext.isObject(b)) {
						a = a[key];
						b = b[key];
					}

					// if array, get from the right index
					if (Ext.isArray(a) && Ext.isArray(b)) {
						a = a[key];
						b = b[key];
					}

					// string
					if (Ext.isString(a) && Ext.isString(b)) {
						a = a.toLowerCase();
						b = b.toLowerCase();

						if (direction === 'DESC') {
							return a < b ? 1 : (a > b ? -1 : 0);
						}
						else {
							return a < b ? -1 : (a > b ? 1 : 0);
						}
					}

					// number
					else if (Ext.isNumber(a) && Ext.isNumber(b)) {
						return direction === 'DESC' ? b - a : a - b;
					}

                    else if (a === undefined || a === null) {
                        return emptyFirst ? -1 : 1;
                    }

                    else if (b === undefined || b === null) {
                        return emptyFirst ? 1 : -1;
                    }

					return -1;
				});

				return array;
			};

				// object
			support.prototype.object = {};

			support.prototype.object.getLength = function(object, suppressWarning) {
				if (!Ext.isObject(object)) {
					if (!suppressWarning) {
						console.log('support.prototype.object.getLength: not an object');
					}

					return null;
				}

				var size = 0;

				for (var key in object) {
					if (object.hasOwnProperty(key)) {
						size++;
					}
				}

				return size;
			};

			support.prototype.object.hasObject = function(object, property, value) {
				if (!support.prototype.object.getLength(object)) {
					return null;
				}

				for (var key in object) {
					var record = object[key];

					if (object.hasOwnProperty(key) && record[property] === value) {
						return true;
					}
				}

				return null;
			};

				// str
			support.prototype.str = {};

			support.prototype.str.replaceAll = function(str, find, replace) {
				return str.replace(new RegExp(find, 'g'), replace);
			};

			support.prototype.str.toggleDirection = function(direction) {
				return direction === 'DESC' ? 'ASC' : 'DESC';
			};

				// number
			support.prototype.number = {};

			support.prototype.number.getNumberOfDecimals = function(number) {
				var str = new String(number);
				return (str.indexOf('.') > -1) ? (str.length - str.indexOf('.') - 1) : 0;
			};

			support.prototype.number.roundIf = function(number, precision) {
				number = parseFloat(number);
				precision = parseFloat(precision);

				if (Ext.isNumber(number) && Ext.isNumber(precision)) {
					var numberOfDecimals = support.prototype.number.getNumberOfDecimals(number);
					return numberOfDecimals > precision ? Ext.Number.toFixed(number, precision) : number;
				}

				return number;
			};

			support.prototype.number.prettyPrint = function(number, separator) {
				separator = conf.style.digitGroupSeparator[separator] ? separator : conf.finals.style.space;

				if (separator === conf.finals.style.none) {
					return number;
				}

				return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, conf.style.digitGroupSeparator[separator]);
			};

                // date
            support.prototype.date = {};

            support.prototype.date.getYYYYMMDD = function(param) {
                if (!Ext.isString(param)) {
                    if (!(Object.prototype.toString.call(param) === '[object Date]' && param.toString() !== 'Invalid date')) {
                        return null;
                    }
                }

                var date = new Date(param),
                    month = '' + (1 + date.getMonth()),
                    day = '' + date.getDate();

                month = month.length === 1 ? '0' + month : month;
                day = day.length === 1 ? '0' + day : day;

                return date.getFullYear() + '-' + month + '-' + day;
            };

			// color
			support.color = {};

			support.color.hexToRgb = function(hex) {
				var shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i,
					result;

				hex = hex.replace(shorthandRegex, function(m, r, g, b) {
					return r + r + g + g + b + b;
				});

				result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);

				return result ? {
					r: parseInt(result[1], 16),
					g: parseInt(result[2], 16),
					b: parseInt(result[3], 16)
				} : null;
			};

            // connection
            support.connection = {};

            support.connection.ajax = function(requestConfig, authConfig) {
                if (authConfig.crossDomain && Ext.isString(authConfig.username) && Ext.isString(authConfig.password)) {
                    requestConfig.headers = Ext.isObject(authConfig.headers) ? authConfig.headers : {};
                    requestConfig.headers['Authorization'] = 'Basic ' + btoa(authConfig.username + ':' + authConfig.password);
                }

                Ext.Ajax.request(requestConfig);
            };
		}());

		// service
		(function() {

			// layout
			service.layout = {};

			service.layout.cleanDimensionArray = function(dimensionArray) {
				if (!support.prototype.array.getLength(dimensionArray)) {
					return null;
				}

				var array = [];

				for (var i = 0; i < dimensionArray.length; i++) {
					array.push(api.layout.Dimension(dimensionArray[i]));
				}

				array = Ext.Array.clean(array);

				return array.length ? array : null;
			};

			service.layout.sortDimensionArray = function(dimensionArray, key) {
				if (!support.prototype.array.getLength(dimensionArray, true)) {
					return null;
				}

				// Clean dimension array
				dimensionArray = service.layout.cleanDimensionArray(dimensionArray);

				if (!dimensionArray) {
					console.log('service.layout.sortDimensionArray: no valid dimensions');
					return null;
				}

				key = key || 'dimensionName';

				// Dimension order
				Ext.Array.sort(dimensionArray, function(a,b) {
					if (a[key] < b[key]) {
						return -1;
					}
					if (a[key] > b[key]) {
						return 1;
					}
					return 0;
				});

				// Sort object items, ids
				for (var i = 0, items; i < dimensionArray.length; i++) {
					support.prototype.array.sort(dimensionArray[i].items, 'ASC', 'id');

					if (support.prototype.array.getLength(dimensionArray[i].ids)) {
						support.prototype.array.sort(dimensionArray[i].ids);
					}
				}

				return dimensionArray;
			};

			service.layout.getObjectNameDimensionMapFromDimensionArray = function(dimensionArray) {
				var map = {};

				if (!support.prototype.array.getLength(dimensionArray)) {
					return null;
				}

				for (var i = 0, dimension; i < dimensionArray.length; i++) {
					dimension = api.layout.Dimension(dimensionArray[i]);

					if (dimension) {
						map[dimension.dimension] = dimension;
					}
				}

				return support.prototype.object.getLength(map) ? map : null;
			};

			service.layout.getObjectNameDimensionItemsMapFromDimensionArray = function(dimensionArray) {
				var map = {};

				if (!support.prototype.array.getLength(dimensionArray)) {
					return null;
				}

				for (var i = 0, dimension; i < dimensionArray.length; i++) {
					dimension = api.layout.Dimension(dimensionArray[i]);

					if (dimension) {
						map[dimension.dimension] = dimension.items;
					}
				}

				return support.prototype.object.getLength(map) ? map : null;
			};

			service.layout.getItemName = function(layout, response, id, isHtml) {
				var metaData = response.metaData,
					name = '';

				if (service.layout.isHierarchy(layout, response, id)) {
					var a = Ext.Array.clean(metaData.ouHierarchy[id].split('/'));
					a.shift();

					for (var i = 0; i < a.length; i++) {
						name += (isHtml ? '<span class="text-weak">' : '') + metaData.names[a[i]] + (isHtml ? '</span>' : '') + ' / ';
					}
				}

				name += metaData.names[id];

				return name;
			};

			service.layout.getExtendedLayout = function(layout) {
				var layout = Ext.clone(layout),
					xLayout;

				xLayout = {
					columns: [],
					rows: [],
					filters: [],

					columnObjectNames: [],
					columnDimensionNames: [],
					rowObjectNames: [],
					rowDimensionNames: [],

					// axis
					axisDimensions: [],
					axisObjectNames: [],
					axisDimensionNames: [],

						// for param string
					sortedAxisDimensionNames: [],

					// Filter
					filterDimensions: [],
					filterObjectNames: [],
					filterDimensionNames: [],

						// for param string
					sortedFilterDimensions: [],

					// all
					dimensions: [],
					objectNames: [],
					dimensionNames: [],

					// oject name maps
					objectNameDimensionsMap: {},
					objectNameItemsMap: {},
					objectNameIdsMap: {},

					// dimension name maps
					dimensionNameDimensionsMap: {},
					dimensionNameItemsMap: {},
					dimensionNameIdsMap: {},

						// for param string
					dimensionNameSortedIdsMap: {},

					// sort table by column
					//sortableIdObjects: []

                    dimensionNameAxisMap: {}
				};

				Ext.applyIf(xLayout, layout);

				// columns, rows, filters
				if (layout.columns) {
					for (var i = 0, dim, items, xDim; i < layout.columns.length; i++) {
						dim = layout.columns[i];
						items = dim.items;
						xDim = {};

						xDim.dimension = dim.dimension;
						xDim.objectName = dim.dimension;
						xDim.dimensionName = dimConf.objectNameMap[dim.dimension].dimensionName;

						if (items) {
							xDim.items = items;
							xDim.ids = [];

							for (var j = 0; j < items.length; j++) {
								xDim.ids.push(items[j].id);
							}
						}

						xLayout.columns.push(xDim);

						xLayout.columnObjectNames.push(xDim.objectName);
						xLayout.columnDimensionNames.push(xDim.dimensionName);

						xLayout.axisDimensions.push(xDim);
						xLayout.axisObjectNames.push(xDim.objectName);
						xLayout.axisDimensionNames.push(dimConf.objectNameMap[xDim.objectName].dimensionName);

						xLayout.objectNameDimensionsMap[xDim.objectName] = xDim;
						xLayout.objectNameItemsMap[xDim.objectName] = xDim.items;
						xLayout.objectNameIdsMap[xDim.objectName] = xDim.ids;

                        xLayout.dimensionNameAxisMap[xDim.dimensionName] = xLayout.columns;
					}
				}

				if (layout.rows) {
					for (var i = 0, dim, items, xDim; i < layout.rows.length; i++) {
						dim = Ext.clone(layout.rows[i]);
						items = dim.items;
						xDim = {};

						xDim.dimension = dim.dimension;
						xDim.objectName = dim.dimension;
						xDim.dimensionName = dimConf.objectNameMap[dim.dimension].dimensionName;

						if (items) {
							xDim.items = items;
							xDim.ids = [];

							for (var j = 0; j < items.length; j++) {
								xDim.ids.push(items[j].id);
							}
						}

						xLayout.rows.push(xDim);

						xLayout.rowObjectNames.push(xDim.objectName);
						xLayout.rowDimensionNames.push(xDim.dimensionName);

						xLayout.axisDimensions.push(xDim);
						xLayout.axisObjectNames.push(xDim.objectName);
						xLayout.axisDimensionNames.push(dimConf.objectNameMap[xDim.objectName].dimensionName);

						xLayout.objectNameDimensionsMap[xDim.objectName] = xDim;
						xLayout.objectNameItemsMap[xDim.objectName] = xDim.items;
						xLayout.objectNameIdsMap[xDim.objectName] = xDim.ids;

                        xLayout.dimensionNameAxisMap[xDim.dimensionName] = xLayout.rows;
					}
				}

				if (layout.filters) {
					for (var i = 0, dim, items, xDim; i < layout.filters.length; i++) {
						dim = layout.filters[i];
						items = dim.items;
						xDim = {};

						xDim.dimension = dim.dimension;
						xDim.objectName = dim.dimension;
						xDim.dimensionName = dimConf.objectNameMap[dim.dimension].dimensionName;

						if (items) {
							xDim.items = items;
							xDim.ids = [];

							for (var j = 0; j < items.length; j++) {
								xDim.ids.push(items[j].id);
							}
						}

						xLayout.filters.push(xDim);

						xLayout.filterDimensions.push(xDim);
						xLayout.filterObjectNames.push(xDim.objectName);
						xLayout.filterDimensionNames.push(dimConf.objectNameMap[xDim.objectName].dimensionName);

						xLayout.objectNameDimensionsMap[xDim.objectName] = xDim;
						xLayout.objectNameItemsMap[xDim.objectName] = xDim.items;
						xLayout.objectNameIdsMap[xDim.objectName] = xDim.ids;

                        xLayout.dimensionNameAxisMap[xDim.dimensionName] = xLayout.filters;
					}
				}

				// legend set
				xLayout.legendSet = layout.legendSet ? init.idLegendSetMap[layout.legendSet.id] : null;

				if (layout.legendSet) {
					xLayout.legendSet = init.idLegendSetMap[layout.legendSet.id];
					support.prototype.array.sort(xLayout.legendSet.legends, 'ASC', 'startValue');
				}

				// unique dimension names
				xLayout.axisDimensionNames = Ext.Array.unique(xLayout.axisDimensionNames);
				xLayout.filterDimensionNames = Ext.Array.unique(xLayout.filterDimensionNames);

				xLayout.columnDimensionNames = Ext.Array.unique(xLayout.columnDimensionNames);
				xLayout.rowDimensionNames = Ext.Array.unique(xLayout.rowDimensionNames);
				xLayout.filterDimensionNames = Ext.Array.unique(xLayout.filterDimensionNames);

					// for param string
				xLayout.sortedAxisDimensionNames = Ext.clone(xLayout.axisDimensionNames).sort();
				xLayout.sortedFilterDimensions = service.layout.sortDimensionArray(Ext.clone(xLayout.filterDimensions));

				// all
				xLayout.dimensions = [].concat(xLayout.axisDimensions, xLayout.filterDimensions);
				xLayout.objectNames = [].concat(xLayout.axisObjectNames, xLayout.filterObjectNames);
				xLayout.dimensionNames = [].concat(xLayout.axisDimensionNames, xLayout.filterDimensionNames);

				// dimension name maps
				for (var i = 0, dimName; i < xLayout.dimensionNames.length; i++) {
					dimName = xLayout.dimensionNames[i];

					xLayout.dimensionNameDimensionsMap[dimName] = [];
					xLayout.dimensionNameItemsMap[dimName] = [];
					xLayout.dimensionNameIdsMap[dimName] = [];
				}

				for (var i = 0, xDim; i < xLayout.dimensions.length; i++) {
					xDim = xLayout.dimensions[i];

					xLayout.dimensionNameDimensionsMap[xDim.dimensionName].push(xDim);
					xLayout.dimensionNameItemsMap[xDim.dimensionName] = xLayout.dimensionNameItemsMap[xDim.dimensionName].concat(xDim.items);
					xLayout.dimensionNameIdsMap[xDim.dimensionName] = xLayout.dimensionNameIdsMap[xDim.dimensionName].concat(xDim.ids);
				}

					// for param string
				for (var key in xLayout.dimensionNameIdsMap) {
					if (xLayout.dimensionNameIdsMap.hasOwnProperty(key)) {
						xLayout.dimensionNameSortedIdsMap[key] = Ext.clone(xLayout.dimensionNameIdsMap[key]).sort();
					}
				}

				// Uuid
				xLayout.tableUuid = init.el + '_' + Ext.data.IdGenerator.get('uuid').generate();

				return xLayout;
			};

			service.layout.getSyncronizedXLayout = function(xLayout, response) {
				var removeDimensionFromXLayout,
					dimensions = Ext.Array.clean([].concat(xLayout.columns || [], xLayout.rows || [], xLayout.filters || [])),
                    xOuDimension = xLayout.objectNameDimensionsMap[dimConf.organisationUnit.objectName],
                    isUserOrgunit = xOuDimension && Ext.Array.contains(xOuDimension.ids, 'USER_ORGUNIT'),
                    isUserOrgunitChildren = xOuDimension && Ext.Array.contains(xOuDimension.ids, 'USER_ORGUNIT_CHILDREN'),
                    isUserOrgunitGrandChildren = xOuDimension && Ext.Array.contains(xOuDimension.ids, 'USER_ORGUNIT_GRANDCHILDREN'),
                    isLevel = function() {
                        if (xOuDimension && Ext.isArray(xOuDimension.ids)) {
                            for (var i = 0; i < xOuDimension.ids.length; i++) {
                                if (xOuDimension.ids[i].substr(0,5) === 'LEVEL') {
                                    return true;
                                }
                            }
                        }

                        return false;
                    }(),
                    isGroup = function() {
                        if (xOuDimension && Ext.isArray(xOuDimension.ids)) {
                            for (var i = 0; i < xOuDimension.ids.length; i++) {
                                if (xOuDimension.ids[i].substr(0,8) === 'OU_GROUP') {
                                    return true;
                                }
                            }
                        }

                        return false;
                    }(),
                    co = dimConf.category.objectName,
                    ou = dimConf.organisationUnit.objectName,
                    headerNames = function() {
                        var headerNames = [];

                        for (var i = 0; i < response.headers.length; i++) {
                            headerNames.push(response.headers[i].name);
                        }

                        return headerNames;
                    }(),
                    layout;

				removeDimensionFromXLayout = function(objectName) {
					var getUpdatedAxis;

					getUpdatedAxis = function(axis) {
						var dimension;
						axis = Ext.clone(axis);

						for (var i = 0; i < axis.length; i++) {
							if (axis[i].dimension === objectName) {
								dimension = axis[i];
							}
						}

						if (dimension) {
							Ext.Array.remove(axis, dimension);
						}

						return axis;
					};

					if (xLayout.columns) {
						xLayout.columns = getUpdatedAxis(xLayout.columns);
					}
					if (xLayout.rows) {
						xLayout.rows = getUpdatedAxis(xLayout.rows);
					}
					if (xLayout.filters) {
						xLayout.filters = getUpdatedAxis(xLayout.filters);
					}
				};

                // set items from init/metaData/xLayout
                for (var i = 0, dim, metaDataDim, items; i < dimensions.length; i++) {
                    dim = dimensions[i];
                    dim.items = [];
                    metaDataDim = response.metaData[dim.objectName];

                    if (Ext.isArray(metaDataDim) && metaDataDim.length) {
                        var ids = Ext.clone(response.metaData[dim.dimensionName]);
                        for (var j = 0; j < ids.length; j++) {
                            dim.items.push({
                                id: ids[j],
                                name: response.metaData.names[ids[j]]
                            });
                        }
                    }
                    else {
                        dim.items = Ext.clone(xLayout.objectNameItemsMap[dim.objectName]);
                    }
                }

                // add missing names
                dimensions = Ext.Array.clean([].concat(xLayout.columns || [], xLayout.rows || [], xLayout.filters || []));

                for (var i = 0, idNameMap = response.metaData.names, dimItems; i < dimensions.length; i++) {
                    dimItems = dimensions[i].items;

                    if (Ext.isArray(dimItems) && dimItems.length) {
                        for (var j = 0, item; j < dimItems.length; j++) {
                            item = dimItems[j];

                            if (Ext.isObject(item) && Ext.isString(idNameMap[item.id]) && !Ext.isString(item.name)) {
                                item.name = idNameMap[item.id] || '';
                            }
                        }
                    }
                }

                // remove dimensions from layout that do not exist in response
                for (var i = 0, dimensionName; i < xLayout.axisDimensionNames.length; i++) {
                    dimensionName = xLayout.axisDimensionNames[i];
                    if (!Ext.Array.contains(headerNames, dimensionName)) {
                        removeDimensionFromXLayout(dimensionName);
                    }
                }

                // Add ou hierarchy dimensions
                //if (xOuDimension && xLayout.showHierarchy) {
                    //addOuHierarchyDimensions();
                //}

                // Re-layout
                layout = api.layout.Layout(xLayout);

                if (layout) {
                    return service.layout.getExtendedLayout(layout);
                }

                return null;
			};

			service.layout.getExtendedAxis = function(xLayout, type) {
				var dimensionNames,
					spanType,
					aDimensions = [],
					nAxisWidth = 1,
					nAxisHeight,
					aaUniqueFloorIds,
					aUniqueFloorWidth = [],
					aAccFloorWidth = [],
					aFloorSpan = [],
					aaGuiFloorIds = [],
					aaAllFloorIds = [],
					aCondoId = [],
					aaAllFloorObjects = [],
					uuidObjectMap = {};

				if (type === 'col') {
					dimensionNames = Ext.clone(xLayout.columnDimensionNames);
					spanType = 'colSpan';
				}
				else if (type === 'row') {
					dimensionNames = Ext.clone(xLayout.rowDimensionNames);
					spanType = 'rowSpan';
				}

				if (!(Ext.isArray(dimensionNames) && dimensionNames.length)) {
					return;
				}
	//dimensionNames = ['pe', 'ou'];

				// aDimensions: array of dimension objects with dimensionName property
				for (var i = 0; i < dimensionNames.length; i++) {
					aDimensions.push({
						dimensionName: dimensionNames[i]
					});
				}
	//aDimensions = [{
		//dimensionName: 'pe'
	//}]

				// aaUniqueFloorIds: array of arrays with unique ids for each dimension floor
				aaUniqueFloorIds = function() {
					var a = [];

					for (var i = 0; i < aDimensions.length; i++) {
						a.push(xLayout.dimensionNameIdsMap[aDimensions[i].dimensionName]);
					}

					return a;
				}();
	//aaUniqueFloorIds	= [ [de-id1, de-id2, de-id3],
	//					    [pe-id1],
	//					    [ou-id1, ou-id2, ou-id3, ou-id4] ]

				// nAxisHeight
				nAxisHeight = aaUniqueFloorIds.length;
	//nAxisHeight = 3


				// aUniqueFloorWidth, nAxisWidth, aAccFloorWidth
				for (var i = 0, nUniqueFloorWidth; i < nAxisHeight; i++) {
					nUniqueFloorWidth = aaUniqueFloorIds[i].length;

					aUniqueFloorWidth.push(nUniqueFloorWidth);
					nAxisWidth = nAxisWidth * nUniqueFloorWidth;
					aAccFloorWidth.push(nAxisWidth);
				}
	//aUniqueFloorWidth	= [3, 1, 4]
	//nAxisWidth		= 12 (3 * 1 * 4)
	//aAccFloorWidth	= [3, 3, 12]

				// aFloorSpan
				for (var i = 0; i < nAxisHeight; i++) {
					if (aUniqueFloorWidth[i] === 1) {
						if (i === 0) { // if top floor, set maximum span
							aFloorSpan.push(nAxisWidth);
						}
						else {
							if (xLayout.hideEmptyRows && type === 'row') {
								aFloorSpan.push(nAxisWidth / aAccFloorWidth[i]);
							}
							else { //if just one item and not top level, use same span as top level
								aFloorSpan.push(aFloorSpan[0]);
							}
						}
					}
					else {
						aFloorSpan.push(nAxisWidth / aAccFloorWidth[i]);
					}
				}
	//aFloorSpan = [4, 12, 1]


				// aaGuiFloorIds
				aaGuiFloorIds.push(aaUniqueFloorIds[0]);

				if (nAxisHeight.length > 1) {
					for (var i = 1, a, n; i < nAxisHeight; i++) {
						a = [];
						n = aUniqueFloorWidth[i] === 1 ? aUniqueFloorWidth[0] : aAccFloorWidth[i-1];

						for (var j = 0; j < n; j++) {
							a = a.concat(aaUniqueFloorIds[i]);
						}

						aaGuiFloorIds.push(a);
					}
				}
	//aaGuiFloorIds	= [ [d1, d2, d3], (3)
	//					[p1, p2, p3, p4, p5, p1, p2, p3, p4, p5, p1, p2, p3, p4, p5], (15)
	//					[o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2...] (30)
	//		  	  	  ]

				// aaAllFloorIds
				for (var i = 0, aAllFloorIds, aUniqueFloorIds, span, factor; i < nAxisHeight; i++) {
					aAllFloorIds = [];
					aUniqueFloorIds = aaUniqueFloorIds[i];
					span = aFloorSpan[i];
					factor = nAxisWidth / (span * aUniqueFloorIds.length);

					for (var j = 0; j < factor; j++) {
						for (var k = 0; k < aUniqueFloorIds.length; k++) {
							for (var l = 0; l < span; l++) {
								aAllFloorIds.push(aUniqueFloorIds[k]);
							}
						}
					}

					aaAllFloorIds.push(aAllFloorIds);
				}
	//aaAllFloorIds	= [ [d1, d1, d1, d1, d1, d1, d1, d1, d1, d1, d2, d2, d2, d2, d2, d2, d2, d2, d2, d2, d3, d3, d3, d3, d3, d3, d3, d3, d3, d3], (30)
	//					[p1, p2, p3, p4, p5, p1, p2, p3, p4, p5, p1, p2, p3, p4, p5, p1, p2, p3, p4, p5, p1, p2, p3, p4, p5, p1, p2, p3, p4, p5], (30)
	//					[o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2, o1, o2] (30)
	//		  	  	  ]

				// aCondoId
				for (var i = 0, id; i < nAxisWidth; i++) {
					id = '';

					for (var j = 0; j < nAxisHeight; j++) {
						id += aaAllFloorIds[j][i];
					}

					if (id) {
						aCondoId.push(id);
					}
				}
	//aCondoId = [ id11+id21+id31, id12+id22+id32, ... ]


				// allObjects
				for (var i = 0, allFloor; i < aaAllFloorIds.length; i++) {
					allFloor = [];

					for (var j = 0, obj; j < aaAllFloorIds[i].length; j++) {
						obj = {
							id: aaAllFloorIds[i][j],
							uuid: Ext.data.IdGenerator.get('uuid').generate(),
							dim: i,
							axis: type
						};

						// leaf?
						if (i === aaAllFloorIds.length - 1) {
							obj.leaf = true;
						}

						allFloor.push(obj);
					}

					aaAllFloorObjects.push(allFloor);
				}

				// add span and children
				for (var i = 0, aAboveFloorObjects, doorIds, uniqueDoorIds; i < aaAllFloorObjects.length; i++) {
                    doorIds = [];

					for (var j = 0, obj, doorCount = 0, oldestObj; j < aaAllFloorObjects[i].length; j++) {

						obj = aaAllFloorObjects[i][j];
                        doorIds.push(obj.id);

						if (doorCount === 0) {

							// span
							obj[spanType] = aFloorSpan[i];

							// children
                            if (obj.leaf) {
                                obj.children = 0;
                            }

							// first sibling
							obj.oldest = true;

							// root?
							if (i === 0) {
								obj.root = true;
							}

							// tmp oldest uuid
							oldestObj = obj;
						}

						obj.oldestSibling = oldestObj;

						if (++doorCount === aFloorSpan[i]) {
							doorCount = 0;
						}
					}

                    // set above floor door children to number of unique door ids on this floor
                    if (i > 0) {
                        aAboveFloorObjects = aaAllFloorObjects[i-1];
                        uniqueDoorIds = Ext.Array.unique(doorIds);

                        for (var j = 0; j < aAboveFloorObjects.length; j++) {
                            aAboveFloorObjects[j].children = uniqueDoorIds.length;
                        }
                    }
				}

				// add parents if more than 1 floor
				if (nAxisHeight > 1) {
					for (var i = 1, aAllFloor; i < nAxisHeight; i++) {
						aAllFloor = aaAllFloorObjects[i];

						//for (var j = 0, obj, doorCount = 0, span = aFloorSpan[i - 1], parentObj = aaAllFloorObjects[i - 1][0]; j < aAllFloor.length; j++) {
						for (var j = 0, doorCount = 0, span = aFloorSpan[i - 1]; j < aAllFloor.length; j++) {
							aAllFloor[j].parent = aaAllFloorObjects[i - 1][j];

							//doorCount++;

							//if (doorCount === span) {
								//parentObj = aaAllFloorObjects[i - 1][j + 1];
								//doorCount = 0;
							//}
						}
					}
				}

				// add uuids array to leaves
				if (aaAllFloorObjects.length) {

					// set span to second lowest span number: if aFloorSpan == [15,3,15,1], set span to 3
					var nSpan = nAxisHeight > 1 ? support.prototype.array.sort(Ext.clone(aFloorSpan))[1] : nAxisWidth,
						aAllFloorObjectsLast = aaAllFloorObjects[aaAllFloorObjects.length - 1];

					for (var i = 0, leaf, parentUuids, obj, leafUuids = []; i < aAllFloorObjectsLast.length; i++) {
						leaf = aAllFloorObjectsLast[i];
						leafUuids.push(leaf.uuid);
						parentUuids = [];
						obj = leaf;

						// get the uuid of the oldest sibling
						while (obj.parent) {
							obj = obj.parent;
							parentUuids.push(obj.oldestSibling.uuid);
						}

						// add parent uuids to leaf
						leaf.uuids = Ext.clone(parentUuids);

						// add uuid for all leaves
						if (leafUuids.length === nSpan) {
							for (var j = (i - nSpan) + 1, leaf; j <= i; j++) {
								leaf = aAllFloorObjectsLast[j];
								leaf.uuids = leaf.uuids.concat(leafUuids);
							}

							leafUuids = [];
						}
					}
				}

				// populate uuidObject map
				for (var i = 0; i < aaAllFloorObjects.length; i++) {
					for (var j = 0, object; j < aaAllFloorObjects[i].length; j++) {
						object = aaAllFloorObjects[i][j];

						uuidObjectMap[object.uuid] = object;
					}
				}

				return {
					type: type,
					items: aDimensions,
					xItems: {
						unique: aaUniqueFloorIds,
						gui: aaGuiFloorIds,
						all: aaAllFloorIds
					},
					objects: {
						all: aaAllFloorObjects
					},
					ids: aCondoId,
					span: aFloorSpan,
					dims: nAxisHeight,
					size: nAxisWidth,
					uuidObjectMap: uuidObjectMap
				};
			};

			service.layout.isHierarchy = function(layout, response, id) {
				return layout.showHierarchy && Ext.isObject(response.metaData.ouHierarchy) && response.metaData.ouHierarchy.hasOwnProperty(id);
			};

			service.layout.layout2plugin = function(layout, el) {
				var layout = Ext.clone(layout),
					dimensions = Ext.Array.clean([].concat(layout.columns || [], layout.rows || [], layout.filters || []));

				layout.url = init.contextPath;

				if (el) {
					layout.el = el;
				}

				if (Ext.isString(layout.id)) {
					return {id: layout.id};
				}

				for (var i = 0, dimension, item; i < dimensions.length; i++) {
					dimension = dimensions[i];

					delete dimension.id;
					delete dimension.ids;
					delete dimension.type;
					delete dimension.dimensionName;
					delete dimension.objectName;

					for (var j = 0, item; j < dimension.items.length; j++) {
						item = dimension.items[j];

						delete item.name;
						delete item.code;
						delete item.created;
						delete item.lastUpdated;
						delete item.value;
					}
				}

				if (layout.showRowTotals) {
					delete layout.showRowTotals;
				}

                if (layout.showColTotals) {
					delete layout.showColTotals;
				}

				if (layout.showColSubTotals) {
					delete layout.showColSubTotals;
				}

				if (layout.showRowSubTotals) {
					delete layout.showRowSubTotals;
				}

				if (layout.showDimensionLabels) {
					delete layout.showDimensionLabels;
				}

				if (!layout.hideEmptyRows) {
					delete layout.hideEmptyRows;
				}

				if (!layout.skipRounding) {
					delete layout.skipRounding;
				}

				if (!layout.showHierarchy) {
					delete layout.showHierarchy;
				}

				if (!layout.completedOnly) {
					delete layout.completedOnly;
				}

				if (layout.displayDensity === conf.finals.style.normal) {
					delete layout.displayDensity;
				}

				if (layout.fontSize === conf.finals.style.normal) {
					delete layout.fontSize;
				}

				if (layout.digitGroupSeparator === conf.finals.style.space) {
					delete layout.digitGroupSeparator;
				}

				if (!layout.legendSet) {
					delete layout.legendSet;
				}

				if (!layout.sorting) {
					delete layout.sorting;
				}

				if (layout.aggregationType === conf.finals.style.default_) {
					delete layout.aggregationType;
				}

				if (layout.dataApprovalLevel && layout.dataApprovalLevel.id === conf.finals.style.default_) {
					delete layout.dataApprovalLevel;
				}

				delete layout.parentGraphMap;
				delete layout.reportingPeriod;
				delete layout.organisationUnit;
				delete layout.parentOrganisationUnit;
				delete layout.regression;
				delete layout.cumulative;
				delete layout.sortOrder;
				delete layout.topLimit;

				return layout;
			};

			// response
			service.response = {};

			service.response.getExtendedResponse = function(xLayout, response) {
				var ids = [];

				response = Ext.clone(response);

				response.nameHeaderMap = {};
				response.idValueMap = {};

				// extend headers
				(function() {

					// extend headers: index, ids, size
					for (var i = 0, header; i < response.headers.length; i++) {
						header = response.headers[i];

						// index
						header.index = i;

						if (header.meta) {

							// ids
							header.ids = Ext.clone(xLayout.dimensionNameIdsMap[header.name]) || [];

							// size
							header.size = header.ids.length;

							// collect ids, used by extendMetaData
							ids = ids.concat(header.ids);
						}
					}

					// nameHeaderMap (headerName: header)
					for (var i = 0, header; i < response.headers.length; i++) {
						header = response.headers[i];

						response.nameHeaderMap[header.name] = header;
					}
				}());

				// create value id map
				(function() {
					var valueHeaderIndex = response.nameHeaderMap[conf.finals.dimension.value.value].index,
						coHeader = response.nameHeaderMap[conf.finals.dimension.category.dimensionName],
						dx = dimConf.data.dimensionName,
						co = dimConf.category.dimensionName,
						axisDimensionNames = xLayout.axisDimensionNames,
						idIndexOrder = [];

					// idIndexOrder
					for (var i = 0; i < axisDimensionNames.length; i++) {
						idIndexOrder.push(response.nameHeaderMap[axisDimensionNames[i]].index);

						// If co exists in response and is not added in layout, add co after dx
						if (coHeader && !Ext.Array.contains(axisDimensionNames, co) && axisDimensionNames[i] === dx) {
							idIndexOrder.push(coHeader.index);
						}
					}

					// idValueMap
					for (var i = 0, row, id; i < response.rows.length; i++) {
						row = response.rows[i];
						id = '';

						for (var j = 0, index; j < idIndexOrder.length; j++) {
							index = idIndexOrder[j];

							//id += response.headers[index].name === co ? '.' : '';
							id += row[index];
						}

						response.idValueMap[id] = row[valueHeaderIndex];
					}
				}());

				return response;
			};

            service.response.addOuHierarchyDimensions = function(response) {
                var headers = response.headers,
                    ouHierarchy = response.metaData.ouHierarchy,
                    rows = response.rows,
                    ouIndex,
                    numLevels = 0,
                    initArray = [],
                    newHeaders = [],
                    a;

                if (!ouHierarchy) {
                    return;
                }

                // get ou index
                for (var i = 0; i < headers.length; i++) {
                    if (headers[i].name === 'ou') {
                        ouIndex = i;
                        break;
                    }
                }

                // get numLevels
                for (var i = 0; i < rows.length; i++) {
                    numLevels = Math.max(numLevels, Ext.Array.clean(ouHierarchy[rows[i][ouIndex]].split('/')).length);
                }

                // init array
                for (var i = 0; i < numLevels; i++) {
                    initArray.push('');
                }

                // extend rows
                for (var i = 0, row, ouArray; i < rows.length; i++) {
                    row = rows[i];
                    ouArray = Ext.applyIf(Ext.Array.clean(ouHierarchy[row[ouIndex]].split('/')), Ext.clone(initArray));

                    Ext.Array.insert(row, ouIndex, ouArray);
                }

                // create new headers
                for (var i = 0; i < numLevels; i++) {
                    newHeaders.push({
                        column: 'Organisation unit',
                        hidden: false,
                        meta: true,
                        name: 'ou',
                        type: 'java.lang.String'
                    });
                }

                Ext.Array.insert(headers, ouIndex, newHeaders);

                return response;
            };

            service.response.getValue = function(str) {
				var n = parseFloat(str);

                if (Ext.isBoolean(str)) {
                    return 1;
                }

                // return string if
                // - parsefloat(string) is not a number
                // - string is just starting with a number
                // - string is a valid date
				//if (!Ext.isNumber(n) || n != str || new Date(str).toString() !== 'Invalid Date') {
				if (!Ext.isNumber(n) || n != str) {
					return 0;
				}

                return n;
			};
        }());

		// web
		(function() {

			// mask
			web.mask = {};

			web.mask.show = function(component, message) {
				if (!Ext.isObject(component)) {
					console.log('support.gui.mask.show: component not an object');
					return null;
				}

				message = message || 'Loading..';

				if (component.mask && component.mask.destroy) {
					component.mask.destroy();
					component.mask = null;
				}

				component.mask = new Ext.create('Ext.LoadMask', component, {
					shadow: false,
					msg: message,
					style: 'box-shadow:0',
					bodyStyle: 'box-shadow:0'
				});

				component.mask.show();
			};

			web.mask.hide = function(component) {
				if (!Ext.isObject(component)) {
					console.log('support.gui.mask.hide: component not an object');
					return null;
				}

				if (component.mask && component.mask.destroy) {
					component.mask.destroy();
					component.mask = null;
				}
			};

			// window
			web.window = web.window || {};

			web.window.setAnchorPosition = function(w, target) {
				var vpw = app.getViewportWidth(),
					targetx = target ? target.getPosition()[0] : 4,
					winw = w.getWidth(),
					y = target ? target.getPosition()[1] + target.getHeight() + 4 : 33;

				if ((targetx + winw) > vpw) {
					w.setPosition((vpw - winw - 2), y);
				}
				else {
					w.setPosition(targetx, y);
				}
			};

			web.window.addHideOnBlurHandler = function(w) {
				var el = Ext.get(Ext.query('.x-mask')[0]);

				el.on('click', function() {
					if (w.hideOnBlur) {
						w.hide();
					}
				});

				w.hasHideOnBlurHandler = true;
			};

			web.window.addDestroyOnBlurHandler = function(w) {
				var maskElements = Ext.query('.x-mask'),
                    el = Ext.get(maskElements[0]);

				el.on('click', function() {
					if (w.destroyOnBlur) {
						w.destroy();
					}
				});

				w.hasDestroyOnBlurHandler = true;
			};

			// message
			web.message = {};

			web.message.alert = function(obj) {
                var config = {},
                    type,
                    window;

                if (!obj || (Ext.isObject(obj) && !obj.message && !obj.responseText)) {
                    return;
                }

                // if response object
                if (Ext.isObject(obj) && obj.responseText && !obj.message) {
                    obj = Ext.decode(obj.responseText);
                }

                // if string
                if (Ext.isString(obj)) {
                    obj = {
                        status: 'ERROR',
                        message: obj
                    };
                }

                // web message
                type = (obj.status || 'INFO').toLowerCase();

				config.title = obj.status;
				config.iconCls = 'ns-window-title-messagebox ' + type;

                // html
                config.html = '';
                config.html += obj.httpStatusCode ? 'Code: ' + obj.httpStatusCode + '<br>' : '';
                config.html += obj.httpStatus ? 'Status: ' + obj.httpStatus + '<br><br>' : '';
                config.html += obj.message + (obj.message.substr(obj.message.length - 1) === '.' ? '' : '.');

                // bodyStyle
                config.bodyStyle = 'padding: 12px; background: #fff; max-width: 600px; max-height: ' + app.getCenterRegionHeight() / 2 + 'px';

                // destroy handler
                config.modal = true;
                config.destroyOnBlur = true;

                // listeners
                config.listeners = {
                    show: function(w) {
                        w.setPosition(w.getPosition()[0], w.getPosition()[1] / 2);

						if (!w.hasDestroyOnBlurHandler) {
							web.window.addDestroyOnBlurHandler(w);
						}
                    }
                };

                window = Ext.create('Ext.window.Window', config);

                window.show();
            };

			// analytics
			web.analytics = {};

			web.analytics.getParamString = function(xLayout, isSorted) {
				var axisDimensionNames = isSorted ? xLayout.sortedAxisDimensionNames : xLayout.axisDimensionNames,
					filterDimensions = isSorted ? xLayout.sortedFilterDimensions : xLayout.filterDimensions,
					dimensionNameIdsMap = isSorted ? xLayout.dimensionNameSortedIdsMap : xLayout.dimensionNameIdsMap,
					paramString = '?',
					addCategoryDimension = false,
					map = xLayout.dimensionNameItemsMap,
					dx = dimConf.indicator.dimensionName,
					co = dimConf.category.dimensionName,
                    aggTypes = ['COUNT', 'SUM', 'STDDEV', 'VARIANCE', 'MIN', 'MAX'],
                    propertyMap = {
                        'name': 'name',
                        'displayName': 'name',
                        'shortName': 'shortName',
                        'displayShortName': 'shortName'
                    },
                    keyAnalysisDisplayProperty = init.userAccount.settings.keyAnalysisDisplayProperty,
                    displayProperty = propertyMap[keyAnalysisDisplayProperty] || propertyMap[xLayout.displayProperty] || 'name';

				for (var i = 0, dimName, items; i < axisDimensionNames.length; i++) {
					dimName = axisDimensionNames[i];

					paramString += 'dimension=' + dimName;

					items = Ext.clone(dimensionNameIdsMap[dimName]);

					if (dimName === dx) {
						items = Ext.Array.unique(items);
					}

					if (dimName !== co) {
						paramString += ':' + items.join(';');
					}

					if (i < (axisDimensionNames.length - 1)) {
						paramString += '&';
					}
				}

				if (addCategoryDimension) {
					paramString += '&dimension=' + conf.finals.dimension.category.dimensionName;
				}

				if (Ext.isArray(filterDimensions) && filterDimensions.length) {
					for (var i = 0, dim; i < filterDimensions.length; i++) {
						dim = filterDimensions[i];

						paramString += '&filter=' + dim.dimensionName + ':' + dim.ids.join(';');
					}
				}

				if (xLayout.showHierarchy) {
					paramString += '&hierarchyMeta=true';
				}

				if (xLayout.completedOnly) {
					paramString += '&completedOnly=true';
				}

				// aggregation type
				if (Ext.Array.contains(aggTypes, xLayout.aggregationType)) {
					paramString += '&aggregationType=' + xLayout.aggregationType;
				}

                // display property
                paramString += '&displayProperty=' + displayProperty.toUpperCase();

                // user org unit
                if (Ext.isArray(xLayout.userOrgUnit) && xLayout.userOrgUnit.length) {
                    paramString += '&userOrgUnit=';

                    for (var i = 0; i < xLayout.userOrgUnit.length; i++) {
                        paramString += xLayout.userOrgUnit[i] + (i < xLayout.userOrgUnit.length - 1 ? ';' : '');
                    }
				}

				// data approval level
				if (Ext.isObject(xLayout.dataApprovalLevel) && Ext.isString(xLayout.dataApprovalLevel.id) && xLayout.dataApprovalLevel.id !== conf.finals.style.default_) {
					paramString += '&approvalLevel=' + xLayout.dataApprovalLevel.id;
				}

                // relative period date
                if (xLayout.relativePeriodDate) {
                    paramString += '&relativePeriodDate=' + xLayout.relativePeriodDate;
                }

                // skip rounding
                if (xLayout.skipRounding) {
                    paramString += '&skipRounding=true';
                }

				return paramString.replace(/#/g, '.');
			};

			web.analytics.validateUrl = function(url) {
				var msg;

                if (Ext.isIE) {
                    msg = 'Too many items selected (url has ' + url.length + ' characters). Internet Explorer accepts maximum 2048 characters.';
                }
                else {
					var len = url.length > 8000 ? '8000' : (url.length > 4000 ? '4000' : '2000');
					msg = 'Too many items selected (url has ' + url.length + ' characters). Please reduce to less than ' + len + ' characters.';
                }

                msg += '\n\n' + 'Hint: A good way to reduce the number of items is to use relative periods and level/group organisation unit selection modes.';

                webAlert(msg, 'warning');
			};

			// pivot
			web.pivot = {};

			web.pivot.sort = function(xLayout, xResponse, xColAxis) {
				var xResponse = Ext.clone(xResponse),
					id = xLayout.sorting.id,
					dim = xLayout.rows[0],
					valueMap = xResponse.idValueMap,
					direction = xLayout.sorting ? xLayout.sorting.direction : 'DESC',
					layout;

				dim.ids = [];

				// relative id?
				if (Ext.isString(id)) {
					id = id.toLowerCase() === 'total' ? 'total_' : id;
				}
				else if (Ext.isNumber(id)) {
					if (id === 0) {
						id = 'total_';
					}
					else {
						id = xColAxis.ids[parseInt(id) - 1];
					}
				}
				else {
					return xLayout;
				}

				// collect values
				for (var i = 0, item, key, value; i < dim.items.length; i++) {
					item = dim.items[i];
					key = id + item.id;
					value = parseFloat(valueMap[key]);

					item.value = Ext.isNumber(value) ? value : (Number.MAX_VALUE * -1);
				}

				// sort
				support.prototype.array.sort(dim.items, direction, 'value');

				// new id order
				for (var i = 0; i < dim.items.length; i++) {
					dim.ids.push(dim.items[i].id);
				}

				// update id
				if (id !== xLayout.sorting.id) {
					xLayout.sorting.id = id;
				}

				return xLayout;
			};

			web.pivot.getHtml = function(xLayout, xResponse, xColAxis, xRowAxis) {
				var getRoundedHtmlValue,
					getTdHtml,
					doSubTotals,
					doRowTotals,
                    doColTotals,
                    doSortableColumnHeaders,
					getColAxisHtmlArray,
					getRowHtmlArray,
					rowAxisHtmlArray,
					getColTotalHtmlArray,
					getGrandTotalHtmlArray,
					getTotalHtmlArray,
					getHtml,
					getUniqueFactor = function(xAxis) {
                        var unique;

						if (!xAxis) {
							return null;
						}

						unique = xAxis.xItems.unique;

						if (unique) {
							return unique.length < 2 ? 1 : (xAxis.size / unique[0].length);
						}

						return null;
					},
					colUniqueFactor = getUniqueFactor(xColAxis),
					rowUniqueFactor = getUniqueFactor(xRowAxis),
					valueItems = [],
					valueObjects = [],
					totalColObjects = [],
					uuidDimUuidsMap = {},
					isLegendSet = Ext.isObject(xLayout.legendSet) && Ext.isArray(xLayout.legendSet.legends) && xLayout.legendSet.legends.length,
                    tdCount = 0,
                    htmlArray;

				xResponse.sortableIdObjects = [];

				getRoundedHtmlValue = function(value, dec) {
					dec = dec || 2;
					return parseFloat(support.prototype.number.roundIf(value, 2)).toString();
				};

				getTdHtml = function(config, metaDataId) {
					var bgColor,
						legends,
						colSpan,
						rowSpan,
						htmlValue,
						displayDensity,
						fontSize,
						isNumeric = Ext.isObject(config) && Ext.isString(config.type) && config.type.substr(0,5) === 'value' && !config.empty,
						isValue = isNumeric && config.type === 'value',
						cls = '',
						html = '',
                        getHtmlValue;

                    getHtmlValue = function(config) {
                        var str = config.htmlValue,
                            n = parseFloat(config.htmlValue);

                        if (config.collapsed) {
                            return '';
                        }

                        if (isValue) {
                            if (Ext.isBoolean(str)) {
                                return str;
                            }

                            //if (!Ext.isNumber(n) || n != str || new Date(str).toString() !== 'Invalid Date') {
                            if (!Ext.isNumber(n) || n != str) {
                                return str;
                            }

                            return n;
                        }

                        return str || '';
                    }

					if (!Ext.isObject(config)) {
						return '';
					}

                    if (config.hidden || config.collapsed) {
                        return '';
                    }

                    // number of cells
                    tdCount = tdCount + 1;

					// background color from legend set
					if (isValue && xLayout.legendSet) {
						var value = parseFloat(config.value);
						legends = xLayout.legendSet.legends;

						for (var i = 0; i < legends.length; i++) {
							if (Ext.Number.constrain(value, legends[i].startValue, legends[i].endValue) === value) {
								bgColor = legends[i].color;
							}
						}
					}

					colSpan = config.colSpan ? 'colspan="' + config.colSpan + '" ' : '';
					rowSpan = config.rowSpan ? 'rowspan="' + config.rowSpan + '" ' : '';
                    htmlValue = getHtmlValue(config);
					htmlValue = config.type !== 'dimension' ? support.prototype.number.prettyPrint(htmlValue, xLayout.digitGroupSeparator) : htmlValue;

					cls += config.hidden ? ' td-hidden' : '';
					cls += config.collapsed ? ' td-collapsed' : '';
					cls += isValue ? ' pointer' : '';
					//cls += bgColor ? ' legend' : (config.cls ? ' ' + config.cls : '');
                    cls += config.cls ? ' ' + config.cls : '';

					// if sorting
					if (Ext.isString(metaDataId)) {
						cls += ' td-sortable';

						xResponse.sortableIdObjects.push({
							id: metaDataId,
							uuid: config.uuid
						});
					}

					html += '<td ' + (config.uuid ? ('id="' + config.uuid + '" ') : '');
					html += ' class="' + cls + '" ' + colSpan + rowSpan;

					//if (bgColor && isValue) {
                        //html += 'style="color:' + bgColor + ';padding:' + displayDensity + '; font-size:' + fontSize + ';"' + '>' + htmlValue + '</td>';
						//html += '>';
						//html += '<div class="legendCt">';
						//html += '<div class="number ' + config.cls + '" style="padding:' + displayDensity + '; padding-right:3px; font-size:' + fontSize + '">' + htmlValue + '</div>';
						//html += '<div class="arrowCt ' + config.cls + '">';
						//html += '<div class="arrow" style="border-bottom:8px solid transparent; border-right:8px solid ' + bgColor + '">&nbsp;</div>';
						//html += '</div></div></div></td>';
					//}
					//else {
						html += 'style="' + (bgColor && isValue ? 'color:' + bgColor + '; ' : '') + '">' + htmlValue + '</td>';
					//}

					return html;
				};

                doColTotals = function() {
					return !!xLayout.showColTotals;
				};

				doRowTotals = function() {
					return !!xLayout.showRowTotals;
				};

				doColSubTotals = function() {
					return !!xLayout.showColSubTotals && xRowAxis && xRowAxis.dims > 1;
				};

				doRowSubTotals = function() {
					return !!xLayout.showRowSubTotals && xColAxis && xColAxis.dims > 1;
				};

				doSortableColumnHeaders = function() {
					return (xRowAxis && xRowAxis.dims === 1);
				};

				getColAxisHtmlArray = function() {
					var a = [],
						getEmptyHtmlArray;

                    getEmptyNameTdConfig = function(config) {
                        config = config || {};

                        return getTdHtml({
                            cls: config.cls ? ' ' + config.cls : 'pivot-empty',
                            colSpan: config.colSpan ? config.colSpan : 1,
                            rowSpan: config.rowSpan ? config.rowSpan : 1,
                            htmlValue: config.htmlValue ? config.htmlValue : '&nbsp;'
                        });
                    };

                    getEmptyHtmlArray = function(i) {
                        var a = [];

                        // if not the intersection cell
                        if (i < xColAxis.dims - 1) {
                            if (xRowAxis && xRowAxis.dims) {
                                for (var j = 0; j < xRowAxis.dims - 1; j++) {
                                    a.push(getEmptyNameTdConfig({
                                        cls: 'pivot-dim-label'
                                    }));
                                }
                            }

                            a.push(getEmptyNameTdConfig({
                                cls: 'pivot-dim-label',
                                htmlValue: dimConf.objectNameMap[xLayout.columnObjectNames[i]].name
                            }));
                        }
                        else {
                            if (xRowAxis && xRowAxis.dims) {
                                for (var j = 0; j < xRowAxis.dims - 1; j++) {
                                    a.push(getEmptyNameTdConfig({
                                        cls: 'pivot-dim-label',
                                        htmlValue: dimConf.objectNameMap[xLayout.rowObjectNames[j]].name
                                    }));
                                }
                            }

                            a.push(getEmptyNameTdConfig({
                                cls: 'pivot-dim-label',
                                htmlValue: (xRowAxis ? dimConf.objectNameMap[xLayout.rowObjectNames[j]].name : '') + (xColAxis && xRowAxis ? '&nbsp;/&nbsp;' : '') + (xColAxis ? dimConf.objectNameMap[xLayout.columnObjectNames[i]].name : '')
                            }));
                        }

                        return a;
                    };

					if (!xColAxis) {

                        // show row dimension labels
                        if (xRowAxis && xLayout.showDimensionLabels) {
                            var dimLabelHtml = [];

                            // labels from row object names
                            for (var i = 0; i < xLayout.rowObjectNames.length; i++) {
                                dimLabelHtml.push(getEmptyNameTdConfig({
                                    cls: 'pivot-dim-label',
                                    htmlValue: dimConf.objectNameMap[xLayout.rowObjectNames[i]].name
                                }));
                            }

                            // pivot-transparent-column unnecessary

                            a.push(dimLabelHtml);
                        }

						return a;
					}

					// for each col dimension
					for (var i = 0, dimHtml; i < xColAxis.dims; i++) {
						dimHtml = [];

                        if (xLayout.showDimensionLabels) {
                            dimHtml = dimHtml.concat(getEmptyHtmlArray(i));
                        }
                        else if (i === 0) {
							dimHtml.push(xColAxis && xRowAxis ? getEmptyNameTdConfig({
                                colSpan: xRowAxis.dims,
                                rowSpan: xColAxis.dims
                            }) : '');
						}

						for (var j = 0, obj, spanCount = 0, condoId, totalId; j < xColAxis.size; j++) {
							spanCount++;
							condoId = null;
							totalId = null;

							obj = xColAxis.objects.all[i][j];
							obj.type = 'dimension';
							obj.cls = 'pivot-dim';
							obj.noBreak = false;
							obj.hidden = !(obj.rowSpan || obj.colSpan);
							obj.htmlValue = service.layout.getItemName(xLayout, xResponse, obj.id, true);

							// sortable column headers. last dim only.
							if (i === xColAxis.dims - 1 && doSortableColumnHeaders()) {

								//condoId = xColAxis.ids[j].split('-').join('');
								condoId = xColAxis.ids[j];
							}

							dimHtml.push(getTdHtml(obj, condoId));

							if (i === 0 && spanCount === xColAxis.span[i] && doRowSubTotals() ) {
								dimHtml.push(getTdHtml({
									type: 'dimensionSubtotal',
									cls: 'pivot-dim-subtotal cursor-default',
									rowSpan: xColAxis.dims,
									htmlValue: '&nbsp;'
								}));

								spanCount = 0;
							}

							if (i === 0 && (j === xColAxis.size - 1) && doRowTotals()) {
								totalId = doSortableColumnHeaders() ? 'total_' : null;

								dimHtml.push(getTdHtml({
									uuid: Ext.data.IdGenerator.get('uuid').generate(),
									type: 'dimensionTotal',
									cls: 'pivot-dim-total',
									rowSpan: xColAxis.dims,
									htmlValue: 'Total'
								}, totalId));
							}
						}

						a.push(dimHtml);
					}

					return a;
				};

				getRowHtmlArray = function() {
					var a = [],
						axisAllObjects = [],
						xValueObjects,
						totalValueObjects = [],
						mergedObjects = [],
						valueItemsCopy,
						colAxisSize = xColAxis ? xColAxis.size : 1,
						rowAxisSize = xRowAxis ? xRowAxis.size : 1,
						recursiveReduce;

					recursiveReduce = function(obj) {
						if (!obj.children) {
							obj.collapsed = true;

							if (obj.parent) {
								obj.parent.oldestSibling.children--;
							}
						}

						if (obj.parent) {
							recursiveReduce(obj.parent.oldestSibling);
						}
					};

					// dimension
					if (xRowAxis) {
						for (var i = 0, row; i < xRowAxis.size; i++) {
							row = [];

							for (var j = 0, obj, newObj; j < xRowAxis.dims; j++) {
								obj = xRowAxis.objects.all[j][i];
								obj.type = 'dimension';
								obj.cls = 'pivot-dim td-nobreak' + (service.layout.isHierarchy(xLayout, xResponse, obj.id) ? ' align-left' : '');
								obj.noBreak = true;
								obj.hidden = !(obj.rowSpan || obj.colSpan);
								obj.htmlValue = service.layout.getItemName(xLayout, xResponse, obj.id, true);

								row.push(obj);
							}

							axisAllObjects.push(row);
						}
					}
                    else {
                        if (xLayout.showDimensionLabels) {
                            axisAllObjects.push([{
                                type: 'transparent',
                                cls: 'pivot-transparent-row'
                            }]);
                        }
                    }


	//axisAllObjects = [ [ dim, dim ]
	//				     [ dim, dim ]
	//				     [ dim, dim ]
	//				     [ dim, dim ] ];

					// value
					for (var i = 0, valueItemsRow, valueObjectsRow, idValueMap = xResponse.idValueMap; i < rowAxisSize; i++) {
						valueItemsRow = [];
						valueObjectsRow = [];

						for (var j = 0, id, value, responseValue, htmlValue, empty, uuid, uuids; j < colAxisSize; j++) {
							empty = false;
							uuids = [];

							// meta data uid
							id = ((xColAxis ? xColAxis.ids[j] : '') + (xRowAxis ? xRowAxis.ids[i] : ''));

                            // value html element id
							uuid = Ext.data.IdGenerator.get('uuid').generate();

							// get uuids array from colaxis/rowaxis leaf
							if (xColAxis) {
								uuids = uuids.concat(xColAxis.objects.all[xColAxis.dims - 1][j].uuids);
							}
							if (xRowAxis) {
								uuids = uuids.concat(xRowAxis.objects.all[xRowAxis.dims - 1][i].uuids);
							}

                            // value, htmlValue
                            responseValue = idValueMap[id];

							if (Ext.isDefined(responseValue)) {
                                value = service.response.getValue(responseValue);
                                htmlValue = responseValue;
							}
							else {
								value = 0;
								htmlValue = '&nbsp;';
								empty = true;
							}

							valueItemsRow.push(value);
							valueObjectsRow.push({
								uuid: uuid,
								type: 'value',
								cls: 'pivot-value' + (empty ? ' cursor-default' : ''),
								value: value,
								htmlValue: htmlValue,
								empty: empty,
								uuids: uuids
							});

							// map element id to dim element ids
							uuidDimUuidsMap[uuid] = uuids;
						}

						valueItems.push(valueItemsRow);
						valueObjects.push(valueObjectsRow);
					}

					// totals
					if (xColAxis && doRowTotals()) {
						for (var i = 0, empty = [], total = 0; i < valueObjects.length; i++) {
							for (j = 0, obj; j < valueObjects[i].length; j++) {
								obj = valueObjects[i][j];

								empty.push(obj.empty);
								total += obj.value;
							}

							// row totals
							totalValueObjects.push({
								type: 'valueTotal',
								cls: 'pivot-value-total',
								value: total,
								htmlValue: Ext.Array.contains(empty, false) ? getRoundedHtmlValue(total) : '',
								empty: !Ext.Array.contains(empty, false)
							});

							// add row totals to idValueMap to make sorting on totals possible
							if (doSortableColumnHeaders()) {
								var totalId = 'total_' + xRowAxis.ids[i],
									isEmpty = !Ext.Array.contains(empty, false);

								xResponse.idValueMap[totalId] = isEmpty ? null : total;
							}

							empty = [];
							total = 0;
						}
					}

					// hide empty rows (dims/values/totals)
					if (xColAxis && xRowAxis) {
						if (xLayout.hideEmptyRows) {
							for (var i = 0, valueRow, isValueRowEmpty, dimLeaf; i < valueObjects.length; i++) {
								valueRow = valueObjects[i];
								isValueRowEmpty = !Ext.Array.contains(Ext.Array.pluck(valueRow, 'empty'), false);

								// if value row is empty
								if (isValueRowEmpty) {

									// hide values by adding collapsed = true to all items
									for (var j = 0; j < valueRow.length; j++) {
										valueRow[j].collapsed = true;
									}

									// hide totals by adding collapsed = true to all items
									if (doRowTotals()) {
										totalValueObjects[i].collapsed = true;
									}

									// hide/reduce parent dim span
									dimLeaf = axisAllObjects[i][xRowAxis.dims-1];
									recursiveReduce(dimLeaf);
								}
							}
						}
					}

                    xValueObjects = valueObjects;

					// col subtotals
					if (doRowSubTotals()) {
						var tmpValueObjects = [];

						for (var i = 0, row, rowSubTotal, colCount; i < xValueObjects.length; i++) {
							row = [];
							rowSubTotal = 0;
							colCount = 0;

							for (var j = 0, item, collapsed = [], empty = []; j < xValueObjects[i].length; j++) {
								item = xValueObjects[i][j];
								rowSubTotal += item.value;
								empty.push(!!item.empty);
								collapsed.push(!!item.collapsed);
								colCount++;

								row.push(item);

								if (colCount === colUniqueFactor) {
									var isEmpty = !Ext.Array.contains(empty, false);
									row.push({
										type: 'valueSubtotal',
										cls: 'pivot-value-subtotal' + (isEmpty ? ' cursor-default' : ''),
										value: rowSubTotal,
										htmlValue: isEmpty ? '&nbsp;' : getRoundedHtmlValue(rowSubTotal),
										empty: isEmpty,
										collapsed: !Ext.Array.contains(collapsed, false)
									});

									colCount = 0;
									rowSubTotal = 0;
									empty = [];
									collapsed = [];
								}
							}

							tmpValueObjects.push(row);
						}

						xValueObjects = tmpValueObjects;
					}

					// row subtotals
					if (doColSubTotals()) {
						var tmpAxisAllObjects = [],
							tmpValueObjects = [],
							tmpTotalValueObjects = [],
							getAxisSubTotalRow;

						getAxisSubTotalRow = function(collapsed) {
							var row = [];

							for (var i = 0, obj; i < xRowAxis.dims; i++) {
								obj = {};
								obj.type = 'dimensionSubtotal';
								obj.cls = 'pivot-dim-subtotal cursor-default';
								obj.collapsed = Ext.Array.contains(collapsed, true);

								if (i === 0) {
									obj.htmlValue = '&nbsp;';
									obj.colSpan = xRowAxis.dims;
								}
								else {
									obj.hidden = true;
								}

								row.push(obj);
							}

							return row;
						};

						// tmpAxisAllObjects
						for (var i = 0, row, collapsed = []; i < axisAllObjects.length; i++) {
							tmpAxisAllObjects.push(axisAllObjects[i]);
							collapsed.push(!!axisAllObjects[i][0].collapsed);

							// insert subtotal after last objects
							if (!Ext.isArray(axisAllObjects[i+1]) || !!axisAllObjects[i+1][0].root) {
								tmpAxisAllObjects.push(getAxisSubTotalRow(collapsed));

								collapsed = [];
							}
						}

						// tmpValueObjects
						for (var i = 0; i < tmpAxisAllObjects.length; i++) {
							tmpValueObjects.push([]);
						}

						for (var i = 0; i < xValueObjects[0].length; i++) {
							for (var j = 0, rowCount = 0, tmpCount = 0, subTotal = 0, empty = [], collapsed, item; j < xValueObjects.length; j++) {
								item = xValueObjects[j][i];
								tmpValueObjects[tmpCount++].push(item);
								subTotal += item.value;
								empty.push(!!item.empty);
								rowCount++;

								if (axisAllObjects[j][0].root) {
									collapsed = !!axisAllObjects[j][0].collapsed;
								}

								if (!Ext.isArray(axisAllObjects[j+1]) || axisAllObjects[j+1][0].root) {
									var isEmpty = !Ext.Array.contains(empty, false);

									tmpValueObjects[tmpCount++].push({
										type: item.type === 'value' ? 'valueSubtotal' : 'valueSubtotalTotal',
										value: subTotal,
										htmlValue: isEmpty ? '&nbsp;' : getRoundedHtmlValue(subTotal),
										collapsed: collapsed,
										cls: (item.type === 'value' ? 'pivot-value-subtotal' : 'pivot-value-subtotal-total') + (isEmpty ? ' cursor-default' : '')
									});
									rowCount = 0;
									subTotal = 0;
									empty = [];
								}
							}
						}

						// tmpTotalValueObjects
						for (var i = 0, obj, collapsed = [], empty = [], subTotal = 0, count = 0; i < totalValueObjects.length; i++) {
							obj = totalValueObjects[i];
							tmpTotalValueObjects.push(obj);

							collapsed.push(!!obj.collapsed);
							empty.push(!!obj.empty);
							subTotal += obj.value;
							count++;

							if (count === xRowAxis.span[0]) {
								var isEmpty = !Ext.Array.contains(empty, false);

								tmpTotalValueObjects.push({
									type: 'valueTotalSubgrandtotal',
									cls: 'pivot-value-total-subgrandtotal' + (isEmpty ? ' cursor-default' : ''),
									value: subTotal,
									htmlValue: isEmpty ? '&nbsp;' : getRoundedHtmlValue(subTotal),
									empty: isEmpty,
									collapsed: !Ext.Array.contains(collapsed, false)
								});

								collapsed = [];
								empty = [];
								subTotal = 0;
								count = 0;
							}
						}

						axisAllObjects = tmpAxisAllObjects;
						xValueObjects = tmpValueObjects;
						totalValueObjects = tmpTotalValueObjects;
					}

					// merge dim, value, total
					for (var i = 0, row; i < xValueObjects.length; i++) {
						row = [];

						//if (xRowAxis) {
							row = row.concat(axisAllObjects[i]);
						//}

						row = row.concat(xValueObjects[i]);

						if (xColAxis) {
							row = row.concat(totalValueObjects[i]);
						}

						mergedObjects.push(row);
					}

					// create html items
					for (var i = 0, row; i < mergedObjects.length; i++) {
						row = [];

						for (var j = 0; j < mergedObjects[i].length; j++) {
							row.push(getTdHtml(mergedObjects[i][j]));
						}

						a.push(row);
					}

					return a;
				};

				getColTotalHtmlArray = function() {
					var a = [];

					if (xRowAxis && doColTotals()) {
						var xTotalColObjects;

						// total col items
						for (var i = 0, total = 0, empty = []; i < valueObjects[0].length; i++) {
							for (var j = 0, obj; j < valueObjects.length; j++) {
								obj = valueObjects[j][i];

								total += obj.value;
								empty.push(!!obj.empty);
							}

							// col total
							totalColObjects.push({
								type: 'valueTotal',
								value: total,
								htmlValue: Ext.Array.contains(empty, false) ? getRoundedHtmlValue(total) : '',
								empty: !Ext.Array.contains(empty, false),
								cls: 'pivot-value-total'
							});

							total = 0;
							empty = [];
						}

						xTotalColObjects = totalColObjects;

						if (xColAxis && doRowSubTotals()) {
							var tmp = [];

							for (var i = 0, item, subTotal = 0, empty = [], colCount = 0; i < xTotalColObjects.length; i++) {
								item = xTotalColObjects[i];
								tmp.push(item);
								subTotal += item.value;
								empty.push(!!item.empty);
								colCount++;

								if (colCount === colUniqueFactor) {
									tmp.push({
										type: 'valueTotalSubgrandtotal',
										value: subTotal,
										htmlValue: Ext.Array.contains(empty, false) ? getRoundedHtmlValue(subTotal) : '',
										empty: !Ext.Array.contains(empty, false),
										cls: 'pivot-value-total-subgrandtotal'
									});

									subTotal = 0;
									colCount = 0;
								}
							}

							xTotalColObjects = tmp;
						}

						// total col html items
						for (var i = 0; i < xTotalColObjects.length; i++) {
							a.push(getTdHtml(xTotalColObjects[i]));
						}
					}

					return a;
				};

				getGrandTotalHtmlArray = function() {
					var total = 0,
						empty = [],
						a = [];

					if (doRowTotals() && doColTotals()) {
						for (var i = 0, obj; i < totalColObjects.length; i++) {
							obj = totalColObjects[i];

							total += obj.value;
							empty.push(obj.empty);
						}

						if (xColAxis && xRowAxis) {
							a.push(getTdHtml({
								type: 'valueGrandTotal',
								cls: 'pivot-value-grandtotal',
								value: total,
								htmlValue: Ext.Array.contains(empty, false) ? getRoundedHtmlValue(total) : '',
								empty: !Ext.Array.contains(empty, false)
							}));
						}
					}

					return a;
				};

				getTotalHtmlArray = function() {
					var dimTotalArray,
						colTotal = getColTotalHtmlArray(),
						grandTotal = getGrandTotalHtmlArray(),
						row,
						a = [];

					if (doColTotals()) {
						if (xRowAxis) {
							dimTotalArray = [getTdHtml({
								type: 'dimensionSubtotal',
								cls: 'pivot-dim-total',
								colSpan: xRowAxis.dims,
								htmlValue: 'Total'
							})];
						}

						row = [].concat(dimTotalArray || [], colTotal || [], grandTotal || []);

						a.push(row);
					}

					return a;
				};

				getHtml = function() {
                    var cls = 'pivot',
                        table;

                    cls += xLayout.displayDensity && xLayout.displayDensity !== conf.finals.style.normal ? ' displaydensity-' + xLayout.displayDensity : '';
                    cls += xLayout.fontSize && xLayout.fontSize !== conf.finals.style.normal ? ' fontsize-' + xLayout.fontSize : '';

					table = '<table id="' + xLayout.tableUuid + '" class="' + cls + '">';

					for (var i = 0; i < htmlArray.length; i++) {
						table += '<tr>' + htmlArray[i].join('') + '</tr>';
					}

					return table += '</table>';
				};

				// get html
				return function() {
					htmlArray = Ext.Array.clean([].concat(getColAxisHtmlArray() || [], getRowHtmlArray() || [], getTotalHtmlArray() || []));

					return {
						html: getHtml(htmlArray),
						uuidDimUuidsMap: uuidDimUuidsMap,
						xColAxis: xColAxis,
						xRowAxis: xRowAxis,
                        tdCount: tdCount
					};
				}();
			};
		}());

		// extend init
		(function() {

			// sort and extend dynamic dimensions
			if (Ext.isArray(init.dimensions)) {
				support.prototype.array.sort(init.dimensions);

				for (var i = 0, dim; i < init.dimensions.length; i++) {
					dim = init.dimensions[i];
					dim.dimensionName = dim.id;
					dim.objectName = conf.finals.dimension.dimension.objectName;
					conf.finals.dimension.objectNameMap[dim.id] = dim;
				}
			}

			// sort ouc
			if (init.user && init.user.ouc) {
				support.prototype.array.sort(init.user.ouc);
			}

			// legend set map
			init.idLegendSetMap = {};

			for (var i = 0, set; i < init.legendSets.length; i++) {
				set = init.legendSets[i];
				init.idLegendSetMap[set.id] = set;
			}
		}());

		// alert
		webAlert = web.message.alert;

		return {
            init: init,
            conf: conf,
            api: api,
            support: support,
            service: service,
            web: web,
            app: app,
            webAlert: webAlert
        };
	};

	// PLUGIN

	PT.plugin = {};

	var init = {
			user: {}
		},
		configs = [],
		isInitStarted = false,
		isInitComplete = false,
		getInit,
        applyCss,
		execute;

	getInit = function(config) {
		var isInit = false,
			requests = [],
			callbackCount = 0,
            type = 'json',
            ajax,
			fn;

        init.contextPath = config.url;

		fn = function() {
			if (++callbackCount === requests.length) {
				isInitComplete = true;

				for (var i = 0; i < configs.length; i++) {
					execute(configs[i]);
				}

				configs = [];
			}
		};

        ajax = function(requestConfig, authConfig) {
            if (authConfig.crossDomain && Ext.isString(authConfig.username) && Ext.isString(authConfig.password)) {
                requestConfig.headers = Ext.isObject(authConfig.headers) ? authConfig.headers : {};
                requestConfig.headers['Authorization'] = 'Basic ' + btoa(authConfig.username + ':' + authConfig.password);
            }

            Ext.Ajax.request(requestConfig);
        };

        // user-account
        requests.push({
            url: init.contextPath + '/api/me/user-account.' + type,
            disableCaching: false,
            success: function(r) {
                init.userAccount = r.responseText ? Ext.decode(r.responseText) : r;

                // init
                var defaultKeyUiLocale = 'en',
                    defaultKeyAnalysisDisplayProperty = 'displayName',
                    displayPropertyMap = {
                        'name': 'displayName',
                        'displayName': 'displayName',
                        'shortName': 'displayShortName',
                        'displayShortName': 'displayShortName'
                    },
                    namePropertyUrl,
                    contextPath,
                    keyUiLocale;

                init.userAccount.settings.keyUiLocale = init.userAccount.settings.keyUiLocale || defaultKeyUiLocale;
                init.userAccount.settings.keyAnalysisDisplayProperty = displayPropertyMap[init.userAccount.settings.keyAnalysisDisplayProperty] || defaultKeyAnalysisDisplayProperty;

                // local vars
                contextPath = init.contextPath;
                keyUiLocale = init.userAccount.settings.keyUiLocale;
                keyAnalysisDisplayProperty = init.userAccount.settings.keyAnalysisDisplayProperty;
                namePropertyUrl = keyAnalysisDisplayProperty + '|rename(name)';

                init.namePropertyUrl = namePropertyUrl;

                fn();
            }
        });

        // user orgunit
		//requests.push({
			//url: init.contextPath + '/api/organisationUnits.' + type + '?userOnly=true&fields=id,name,children[id,name]&paging=false',
            //disableCaching: false,
			//success: function(r) {
				//var organisationUnits = (r.responseText ? Ext.decode(r.responseText).organisationUnits : r) || [],
                    //ou = [],
                    //ouc = [];

                //if (organisationUnits.length) {
                    //for (var i = 0, org; i < organisationUnits.length; i++) {
                        //org = organisationUnits[i];

                        //ou.push(org.id);

                        //if (org.children) {
                            //ouc = Ext.Array.clean(ouc.concat(Ext.Array.pluck(org.children, 'id') || []));
                        //}
                    //}

                    //init.user = init.user || {};
                    //init.user.ou = ou;
                    //init.user.ouc = ouc;
                //}
                //else {
                    //alert('User is not assigned to any organisation units');
                //}

                //fn();
			//}
		//});

        // dimensions
		requests.push({
			url: init.contextPath + '/api/dimensions.' + type + '?fields=id,displayName|rename(name)&paging=false',
            disableCaching: false,
			success: function(r) {
				init.dimensions = r.responseText ? Ext.decode(r.responseText).dimensions : r.dimensions;
				fn();
			}
		});

        // legend sets
        requests.push({
            url: init.contextPath + '/api/legendSets.json?fields=id,displayName|rename(name),legends[id,displayName|rename(name),startValue,endValue,color]&paging=false',
            success: function(r) {
                init.legendSets = Ext.decode(r.responseText).legendSets || [];
                fn();
            }
        });

		for (var i = 0; i < requests.length; i++) {
            ajax(requests[i], config);
		}
	};

    applyCss = function(config) {
        var css = '',
            arrowUrl = config.dashboard ? init.contextPath + '/dhis-web-commons/javascripts/plugin/images/arrowupdown.png' : '//dhis2-cdn.org/v220/plugin/images/arrowupdown.png',
            errorUrl = config.dashboard ? init.contextPath + '/dhis-web-commons/javascripts/plugin/images/error_m.png' : '//dhis2-cdn.org/v220/plugin/images/error_m.png';

        css += 'table.pivot { font-family: arial,sans-serif,ubuntu,consolas; border-collapse: collapse; border-spacing: 0px; border: 0 none; } \n';
        css += '.td-nobreak { white-space: nowrap; } \n';
        css += '.td-hidden { display: none; } \n';
        css += '.td-collapsed { display: none; } \n';
        css += 'table.pivot.displaydensity-COMFORTABLE td { padding: 7px } \n';
        css += 'table.pivot.displaydensity-COMPACT td { padding: 3px } \n';
        css += 'table.pivot.fontsize-LARGE td { font-size: 13px } \n';
        css += 'table.pivot.fontsize-SMALL td { font-size: 10px } \n';
        css += '.pivot td { font-family: arial, sans-serif, helvetica neue, helvetica !important; padding: 5px; border: 1px solid #b2b2b2; font-size: 11px } \n';
        css += '.pivot-dim { background-color: #dae6f8; text-align: center; } \n';
        css += '.pivot-dim.highlighted { background-color: #c5d8f6; } \n';
        css += '.pivot-dim-subtotal { background-color: #cad6e8; text-align: center; } \n';
        css += '.pivot-dim-total { background-color: #bac6d8; text-align: center; } \n';
        css += '.pivot-dim-total.highlighted { background-color: #adb8c9; } \n';
        css += '.pivot-dim-empty { background-color: #dae6f8; text-align: center; } \n';
        css += '.pivot-value { background-color: #fff; white-space: nowrap; text-align: right; } \n';
        css += '.pivot-value-subtotal { background-color: #f4f4f4; white-space: nowrap; text-align: right; } \n';
        css += '.pivot-value-subtotal-total { background-color: #e7e7e7; white-space: nowrap; text-align: right; } \n';
        css += '.pivot-value-total { background-color: #e4e4e4; white-space: nowrap; text-align: right; } \n';
        css += '.pivot-value-total-subgrandtotal { background-color: #d8d8d8; white-space: nowrap; text-align: right; } \n';
        css += '.pivot-value-grandtotal { background-color: #c8c8c8; white-space: nowrap; text-align: right; } \n';
        css += '.pivot-dim-label { background-color: #cddaed; white-space: nowrap; text-align: center; } \n';
        css += '.pivot-empty { background-color: #cddaed; } \n';
        css += '.pivot-transparent-column { background-color: #fff; border-top-color: #fff !important; border-right-color: #fff !important; } \n';
        css += '.pivot-transparent-row { background-color: #fff; border-bottom-color: #fff !important; border-left-color: #fff !important; } \n';

        css += '.x-mask-msg { padding: 0; border: 0 none; background-image: none; background-color: transparent; } \n';
        css += '.x-mask-msg div { background-position: 11px center; } \n';
        css += '.x-mask-msg .x-mask-loading { border: 0 none; background-color: #000; color: #fff; border-radius: 2px; padding: 12px 14px 12px 30px; opacity: 0.65; } \n';
        css += '.x-mask { opacity: 0 } \n';

        css += '.pivot td.legend { padding: 0; } \n';
        css += '.pivot div.legendCt { display: table; float: right; width: 100%; } \n';
        css += '.pivot div.arrowCt { display: table-cell; vertical-align: top; width: 8px; } \n';
        css += '.pivot div.arrow { width: 0; height: 0; } \n';
        css += '.pivot div.number { display: table-cell; } \n',
        css += '.pivot div.legendColor { display: table-cell; width: 2px; } \n';

        css += '.pointer { cursor: pointer; } \n';
        css += '.td-sortable { background-image: url("' + arrowUrl + '"); background-repeat: no-repeat; background-position: right center; padding-right: 15px !important; } \n';

        // alert
        css += '.ns-plugin-alert { width: 90%; padding: 5%; color: #777 } \n';

        css += '.x-window-body { font-size: 13px; } \n';
        css += '.ns-window-title-messagebox { padding-left: 16px; background-position-y: 1px; } \n';
        css += '.ns-window-title-messagebox.error { background-image: url("' + errorUrl + '"); } \n';

        Ext.util.CSS.createStyleSheet(css);
    };

	execute = function(config) {
		var validateConfig,
            extendInstance,
			createViewport,
			initialize,
			ns = {
				core: {},
				app: {}
			};

		validateConfig = function(config) {
			if (!Ext.isObject(config)) {
				console.log('Report table configuration is not an object');
				return;
			}

			if (!Ext.isString(config.el)) {
				console.log('No valid element id provided');
				return;
			}

			config.id = config.id || config.uid;

			return true;
		};

        extendInstance = function(ns, appConfig) {
            var init = ns.core.init,
				api = ns.core.api,
                conf = ns.core.conf,
				support = ns.core.support,
				service = ns.core.service,
				web = ns.core.web,
                type = 'json',
                headerMap = {
                    json: 'application/json'
                },
                headers = {
                    'Content-Type': headerMap[type],
                    'Accepts': headerMap[type]
                },
                el = Ext.get(init.el);

			init.el = config.el;

			// ns
            ns.plugin = appConfig.plugin;
            ns.dashboard = appConfig.dashboard;
            ns.crossDomain = appConfig.crossDomain;
            ns.skipMask = appConfig.skipMask;
            ns.skipFade = appConfig.skipFade;
            ns.el = appConfig.el;
            ns.username = appConfig.username;
            ns.password = appConfig.password;
            ns.ajax = support.connection.ajax;

			// message
			web.message = web.message || {};

			web.message.alert = function(text) {
                var div = Ext.get(init.el);

                if (div) {
                    div.setStyle('opacity', 1);
                    div.update('<div class="ns-plugin-alert">' + text + '</div>');
                }
            };

			// mouse events
			web.events = web.events || {};

			web.events.setColumnHeaderMouseHandlers = function(layout, xLayout, response, xResponse) {
				if (Ext.isArray(xResponse.sortableIdObjects)) {
					for (var i = 0, obj, el; i < xResponse.sortableIdObjects.length; i++) {
						obj = xResponse.sortableIdObjects[i];
						el = Ext.get(obj.uuid);

                        if (el && el.dom) {
                            el.dom.layout = layout;
                            el.dom.xLayout = xLayout;
                            el.dom.response = response;
                            el.dom.xResponse = xResponse;
                            el.dom.metaDataId = obj.id;
                            el.dom.onColumnHeaderMouseClick = web.events.onColumnHeaderMouseClick;
                            el.dom.onColumnHeaderMouseOver = web.events.onColumnHeaderMouseOver;
                            el.dom.onColumnHeaderMouseOut = web.events.onColumnHeaderMouseOut;

                            el.dom.setAttribute('onclick', 'this.onColumnHeaderMouseClick(this.layout, this.xLayout, this.response, this.xResponse, this.metaDataId)');
                            el.dom.setAttribute('onmouseover', 'this.onColumnHeaderMouseOver(this)');
                            el.dom.setAttribute('onmouseout', 'this.onColumnHeaderMouseOut(this)');
                        }
					}
				}
			};

			web.events.onColumnHeaderMouseClick = function(layout, xLayout, response, xResponse, id) {
				if (layout.sorting && layout.sorting.id === id) {
					layout.sorting.direction = support.prototype.str.toggleDirection(layout.sorting.direction);
				}
				else {
					layout.sorting = {
						id: id,
						direction: 'DESC'
					};
				}

                web.mask.show(ns.app.centerRegion, 'Sorting...');

                Ext.defer(function() {
                    web.pivot.createTable(layout, response, null, false);
                }, 10);
			};

			web.events.onColumnHeaderMouseOver = function(el) {
                var div = Ext.get(el);

                if (div) {
                    div.addCls('pointer highlighted');
                }
			};

			web.events.onColumnHeaderMouseOut = function(el) {
                var div = Ext.get(el);

                if (div) {
                    div.removeCls('pointer highlighted');
                }
			};

			// pivot
			web.pivot = web.pivot || {};

            web.pivot.loadTable = function(obj) {
                var success,
                    failure,
                    config = {};

                if (!(obj && obj.id)) {
                    console.log('Error, no report table id');
                    return;
                }

                success = function(r) {
                    var layout = api.layout.Layout((r.responseText ? Ext.decode(r.responseText) : r), obj);

                    if (layout) {
                        web.pivot.getData(layout, true);
                    }
                };

                failure = function(r) {
                    console.log(obj.id, (r.responseText ? Ext.decode(r.responseText) : r));
                };

                config.url = init.contextPath + '/api/reportTables/' + obj.id + '.' + type + '?fields=' + ns.core.conf.url.analysisFields.join(',');
                config.disableCaching = false;
                config.headers = headers;
                config.success = success;
                config.failure = failure;

                ns.ajax(config, ns);
			};

			web.pivot.getData = function(layout, isUpdateGui) {
				var xLayout,
					paramString,
                    sortedParamString,
                    onFailure;

				if (!layout) {
					return;
				}

                onFailure = function(r) {
                    if (!appConfig.skipMask) {
                        web.mask.hide(ns.app.centerRegion);
                    }
                };

				xLayout = service.layout.getExtendedLayout(layout);
				paramString = web.analytics.getParamString(xLayout) + '&skipData=true';
				sortedParamString = web.analytics.getParamString(xLayout, true) + '&skipMeta=true';

				// mask
                if (!appConfig.skipMask) {
                    web.mask.show(ns.app.centerRegion);
                }

                ns.ajax({
					url: init.contextPath + '/api/analytics.json' + paramString,
					timeout: 60000,
					headers: {
						'Content-Type': 'application/json',
						'Accepts': 'application/json'
					},
					disableCaching: false,
					failure: function(r) {
                        onFailure(r);
					},
					success: function(r) {
                        var metaData = Ext.decode(r.responseText).metaData;

                        ns.ajax({
                            url: init.contextPath + '/api/analytics.json' + sortedParamString,
                            timeout: 60000,
                            headers: {
                                'Content-Type': 'application/json',
                                'Accepts': 'application/json'
                            },
                            disableCaching: false,
                            failure: function(r) {
                                onFailure(r);
                            },
                            success: function(r) {
                                ns.app.dateCreate = new Date();

                                var response = api.response.Response(Ext.decode(r.responseText));

                                if (!response) {
                                    onFailure();
                                    return;
                                }

                                response.metaData = metaData;

                                ns.app.paramString = sortedParamString;

                                web.pivot.createTable(layout, response, null, isUpdateGui);
                            }
                        }, ns);
                    }
                }, ns);
			};

			web.pivot.createTable = function(layout, response, xResponse, isUpdateGui) {
				var xLayout,
					xColAxis,
					xRowAxis,
					table,
					getHtml,
					getXLayout = service.layout.getExtendedLayout,
					getSXLayout = service.layout.getSyncronizedXLayout,
					getXResponse = service.response.getExtendedResponse,
					getXAxis = service.layout.getExtendedAxis,
                    getTitleHtml = function(title) {
                        return appConfig.dashboard && title ? '<div style="height: 19px; line-height: 14px; width: 100%; font: bold 12px LiberationSans; color: #333; text-align: center; letter-spacing: -0.1px"">' + title + '</div>' : '';
                    };

				getHtml = function(xLayout, xResponse) {
					xColAxis = getXAxis(xLayout, 'col');
					xRowAxis = getXAxis(xLayout, 'row');

					return web.pivot.getHtml(xLayout, xResponse, xColAxis, xRowAxis);
				};

				xLayout = getSXLayout(getXLayout(layout), xResponse || response);

                ns.app.dateSorting = new Date();

				if (layout.sorting) {
					if (!xResponse) {
						xResponse = getXResponse(xLayout, response);
						getHtml(xLayout, xResponse);
					}

					web.pivot.sort(xLayout, xResponse, xColAxis || ns.app.xColAxis);
					xLayout = getXLayout(api.layout.Layout(xLayout));
				}
				else {
					xResponse = service.response.getExtendedResponse(xLayout, response);
				}

				table = getHtml(xLayout, xResponse);

                // timing
                ns.app.dateRender = new Date();

				//ns.app.centerRegion.removeAll(true);
				ns.app.centerRegion.update(getTitleHtml(layout.name) + table.html);

                // fade
                //if (!ns.skipFade) {
                    //Ext.defer(function() {
                        //var el = Ext.get(init.el);

                        //if (el) {
                            //el.fadeIn({
                                //duration: 400
                            //});
                        //}
                    //}, 300 );
                //}

				// after render
				ns.app.layout = layout;
				ns.app.xLayout = xLayout;
				ns.app.response = response;
				ns.app.xResponse = xResponse;
				ns.app.uuidDimUuidsMap = config.uuidDimUuidsMap;
				ns.app.uuidObjectMap = Ext.applyIf((xColAxis ? xColAxis.uuidObjectMap : {}), (xRowAxis ? xRowAxis.uuidObjectMap : {}));

				// sorting
				web.events.setColumnHeaderMouseHandlers(layout, xLayout, response, xResponse);

				web.mask.hide(ns.app.centerRegion);

                if (PT.isDebug) {
                    console.log("layout", layout);
                    console.log("response", response);
                    console.log("xResponse", xResponse);
                    console.log("xLayout", xLayout);
                    console.log("core", ns.core);
                    console.log("app", ns.app);
                }
			};

			//web.pivot.sort = function(xLayout, response, id) {
				//var xLayout = Ext.clone(xLayout),
					//response = Ext.clone(response),
					//dim = xLayout.rows[0],
					//valueMap = response.idValueMap,
					//direction = xLayout.sorting ? xLayout.sorting.direction : 'DESC',
					//layout;

				//dim.ids = [];

				//// collect values
				//for (var i = 0, item, key, value; i < dim.items.length; i++) {
					//item = dim.items[i];
					//key = id + item.id;
					//value = parseFloat(valueMap[key]);

					//item.value = Ext.isNumber(value) ? value : (Number.MAX_VALUE * -1);
				//}

				//// sort
				//support.prototype.array.sort(dim.items, direction, 'value');

				//// new id order
				//for (var i = 0; i < dim.items.length; i++) {
					//dim.ids.push(dim.items[i].id);
				//}

				//// re-layout
				//layout = api.layout.Layout(xLayout);

				//// re-create table
				//web.pivot.createTable(layout, null, response, false);
			//};

            //if (!ns.skipFade && el) {
				//el.setStyle('opacity', 0);
            //}
		};

		createViewport = function() {
			return {
				centerRegion: Ext.get(config.el)
			};
		};

		initialize = function() {
            var el = Ext.get(config.el),
                appConfig;

			if (!validateConfig(config)) {
				return;
			}

            appConfig = {
                plugin: true,
                dashboard: Ext.isBoolean(config.dashboard) ? config.dashboard : false,
                crossDomain: Ext.isBoolean(config.crossDomain) ? config.crossDomain : true,
                skipMask: Ext.isBoolean(config.skipMask) ? config.skipMask : false,
                skipFade: Ext.isBoolean(config.skipFade) ? config.skipFade : false,
                el: Ext.isString(config.el) ? config.el : null,
                username: Ext.isString(config.username) ? config.username : null,
                password: Ext.isString(config.password) ? config.password : null
            };

            // css
            applyCss(config);

			// core
			ns.core = PT.getCore(init, appConfig);
			extendInstance(ns, appConfig);

			ns.app.viewport = createViewport();
			ns.app.centerRegion = ns.app.viewport.centerRegion;

            PT.instances.push(ns);

            if (el) {
                el.setViewportWidth = function(width) {
                    ns.app.centerRegion.setWidth(width);
                };
            }

			if (config && config.id) {
				ns.core.web.pivot.loadTable(config);
			}
			else {
				layout = ns.core.api.layout.Layout(config);

				if (!layout) {
					return;
				}

				ns.core.web.pivot.getData(layout);
			}
		}();
	};

	PT.plugin.getTable = function(config) {
		if (Ext.isString(config.url) && config.url.split('').pop() === '/') {
			config.url = config.url.substr(0, config.url.length - 1);
		}

		if (isInitComplete) {
			execute(config);
		}
		else {
			configs.push(config);

			if (!isInitStarted) {
				isInitStarted = true;
				getInit(config);
			}
		}
	};

	DHIS = Ext.isObject(window['DHIS']) ? DHIS : {};
	DHIS.getTable = PT.plugin.getTable;
});
