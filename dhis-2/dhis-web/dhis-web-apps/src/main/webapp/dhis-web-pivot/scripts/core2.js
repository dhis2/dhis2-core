$(function() {
    var N = PT = {};

    // convenience TODO import
    (function() {
        var isString,
            isNumber,
            isNumeric,
            isBoolean,
            isArray,
            isObject,
            isFunction,
            isEmpty,
            isDefined,
            isIE,
            stringReplace,
            numberConstrain,
            numberToFixed,
            arrayFrom,
            arrayClean,
            arrayPluck,
            arrayUnique,
            arrayContains,
            arraySort,
            clone,
            uuid,
            enumerables = function() {
                var enu = ['valueOf', 'toLocaleString', 'toString', 'constructor'];

                for (var i in { toString: 1 }) {
                    enu = null;
                }

                return enu;
            }();

        isString = function(param) {
            return typeof param === 'string';
        };

        isNumber = function(param) {
            return typeof param === 'number' && isFinite(param);
        };

        isNumeric = function(param) {
            return !isNaN(parseFloat(param)) && isFinite(param);
        };

        isArray = (function() {
            return  ('isArray' in Array) ? Array.isArray : function(param) {
                return toString.call(param) === '[object Array]';
            };
        })();

        isObject = (function() {
            return (toString.call(null) === '[object Object]') ? function(param) {
                return param !== null && param !== undefined && toString.call(param) === '[object Object]' && param.ownerDocument === undefined;
            } : function(param) {
                return toString.call(param) === '[object Object]';
            };
        })();

        isFunction = (function() {
            return (typeof document !== 'undefined' && typeof document.getElementsByTagName('body') === 'function') ? function(value) {
                return !!value && toString.call(value) === '[object Function]';
            } : function(value) {
                return !!value && typeof value === 'function';
            };
        })();

        isBoolean = function(param) {
            return typeof param === 'boolean';
        };

        // dep: isArray
        isEmpty = function(array, allowEmptyString) {
            return (array == null) || (!allowEmptyString ? array === '' : false) || (isArray(array) && array.length === 0);
        };

        isDefined = function(param) {
            return typeof param !== 'undefined';
        };

        isPrimitive = function(param) {
            var type = typeof param;
            return type === 'string' || type === 'number' || type === 'boolean';
        };

        isIE = function() {
            var ua = window.navigator.userAgent;

            // test values
            // IE 10: ua = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)';
            // IE 11: ua = 'Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko';
            // IE 12 / Spartan: ua = 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0';

            var msie = ua.indexOf('MSIE ');
            if (msie > 0) {
                // IE 10 or older => return version number
                return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
            }

            var trident = ua.indexOf('Trident/');
            if (trident > 0) {
                // IE 11 => return version number
                var rv = ua.indexOf('rv:');
                return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
            }

            var edge = ua.indexOf('Edge/');
                if (edge > 0) {
                // IE 12 => return version number
                return parseInt(ua.substring(edge + 5, ua.indexOf('.', edge)), 10);
            }

            // other browser
            return false;
        };

        numberConstrain = function(number, min, max) {
            number = parseFloat(number);

            if (!isNaN(min)) {
                number = Math.max(number, min);
            }
            if (!isNaN(max)) {
                number = Math.min(number, max);
            }
            return number;
        };

        numberToFixed = function() {
            return ((0.9).toFixed() !== '1') ? function(value, precision) {
                precision = precision || 0;
                var pow = math.pow(10, precision);
                return (math.round(value * pow) / pow).toFixed(precision);
            } : function(value, precision) {
                return value.toFixed(precision);
            };
        }();

        // dep: isArray
        arrayFrom = function(param, isNewRef) {
            var toArray = function(iterable, start, end) {
                if (!iterable || !iterable.length) {
                    return [];
                }

                if (typeof iterable === 'string') {
                    iterable = iterable.split('');
                }

                if (supportsSliceOnNodeList) {
                    return slice.call(iterable, start || 0, end || iterable.length);
                }

                var array = [],
                    i;

                start = start || 0;
                end = end ? ((end < 0) ? iterable.length + end : end) : iterable.length;

                for (i = start; i < end; i++) {
                    array.push(iterable[i]);
                }

                return array;
            };

            if (param === undefined || param === null) {
                return [];
            }

            if (isArray(param)) {
                return (isNewRef) ? slice.call(param) : param;
            }

            var type = typeof param;
            if (param && param.length !== undefined && type !== 'string' && (type !== 'function' || !param.apply)) {
                return toArray(param);
            }

            return [param];
        };

        // dep: isEmpty
        arrayClean = function(array) {
            var results = [],
                i = 0,
                ln = array.length,
                item;

            for (; i < ln; i++) {
                item = array[i];

                if (!isEmpty(item)) {
                    results.push(item);
                }
            }

            return results;
        };

        arrayPluck = function(array, propertyName) {
            var newArray = [],
                i, len, item;

            for (i = 0, len = array.length; i < len; i++) {
                item = array[i];

                newArray.push(item[propertyName]);
            }

            return newArray;
        };

        arrayUnique = function(array) {
            var newArray = [],
                i = 0,
                len = array.length,
                item;

            for (; i < len; i++) {
                item = array[i];

                if (newArray.indexOf(item) === -1) {
                    newArray.push(item);
                }
            }

            return newArray;
        };

        arrayContains = function(array, item) {
            return Array.prototype.indexOf.call(array, item) !== -1;
        };

        // dep: isArray, isObject
        arraySort = function(array, direction, key, emptyFirst) {
            // supports [number], [string], [{key: number}], [{key: string}], [[string]], [[number]]

            if (!(N.isArray(array) && array.length)) {
                return;
            }

            key = !!key || N.isNumber(key) ? key : 'name';

            array.sort( function(a, b) {

                // if object, get the property values
                if (N.isObject(a) && N.isObject(b)) {
                    a = a[key];
                    b = b[key];
                }

                // if array, get from the right index
                if (N.isArray(a) && N.isArray(b)) {
                    a = a[key];
                    b = b[key];
                }

                // string
                if (N.isString(a) && N.isString(b)) {
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
                else if (N.isNumber(a) && N.isNumber(b)) {
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

        clone = function(item) {
            if (item === null || item === undefined) {
                return item;
            }

            if (item.nodeType && item.cloneNode) {
                return item.cloneNode(true);
            }

            var type = toString.call(item),
                i, j, k, clone, key;

            if (type === '[object Date]') {
                return new Date(item.getTime());
            }

            if (type === '[object Array]') {
                i = item.length;

                clone = [];

                while (i--) {
                    clone[i] = N.clone(item[i]);
                }
            }
            else if (type === '[object Object]' && item.constructor === Object) {
                clone = {};

                for (key in item) {
                    clone[key] = N.clone(item[key]);
                }

                if (enumerables) {
                    for (j = enumerables.length; j--;) {
                        k = enumerables[j];
                        if (item.hasOwnProperty(k)) {
                            clone[k] = item[k];
                        }
                    }
                }
            }

            return clone || item;
        };

        uuid = function() {
            var s4 = function() {
                return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
            };

            return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
        };

        // prototype
        String.prototype.replaceAll = function(str1, str2, ignore) {
            return this.replace(new RegExp(str1.replace(/([\/\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g,"\\$&"),(ignore?"gi":"g")),(typeof(str2)=="string")?str2.replace(/\$/g,"$$$$"):str2);
        };

        N.isString = isString;
        N.isNumber = isNumber;
        N.isNumeric = isNumeric;
        N.isBoolean = isBoolean;
        N.isArray = isArray;
        N.isObject = isObject;
        N.isFunction = isFunction;
        N.isEmpty = isEmpty;
        N.isDefined = isDefined;
        N.isIE = isIE;
        N.numberConstrain = numberConstrain;
        N.numberToFixed = numberToFixed;
        N.arrayFrom = arrayFrom;
        N.arrayClean = arrayClean;
        N.arrayPluck = arrayPluck;
        N.arrayUnique = arrayUnique;
        N.arrayContains = arrayContains;
        N.arraySort = arraySort;
        N.clone = clone;
        N.uuid = uuid;
    })();

    // AppManager
    (function() {
        var AppManager = function() {
            var t = this;

            // constants
            t.defaultUiLocale = 'en';
            t.defaultDisplayProperty = 'displayName';
            t.rootNodeId = 'root';

            t.valueTypes = {
                'numeric': ['NUMBER','UNIT_INTERVAL','PERCENTAGE','INTEGER','INTEGER_POSITIVE','INTEGER_NEGATIVE','INTEGER_ZERO_OR_POSITIVE'],
                'text': ['TEXT','LONG_TEXT','LETTER','PHONE_NUMBER','EMAIL'],
            	'boolean': ['BOOLEAN','TRUE_ONLY'],
            	'date': ['DATE','DATETIME'],
            	'aggregate': ['NUMBER','UNIT_INTERVAL','PERCENTAGE','INTEGER','INTEGER_POSITIVE','INTEGER_NEGATIVE','INTEGER_ZERO_OR_POSITIVE','BOOLEAN','TRUE_ONLY']
            };

            t.defaultAnalysisFields = [
                '*',
                'program[id,name]',
                'programStage[id,name]',
                'columns[dimension,filter,items[id,$]]',
                'rows[dimension,filter,items[id,$]]',
                'filters[dimension,filter,items[id,$]]',
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
            ];

            // uninitialized
            t.manifest;
            t.systemInfo;
            t.systemSettings;
            t.userAccount;
            t.calendar;
            t.periodGenerator;
            t.viewUnapprovedData;

            t.rootNodes = [];
            t.organisationUnitLevels = [];
            t.dimensions = [];
            t.legendSets = [];
            t.dataApprovalLevels = [];

            // transient
            t.path;
            t.dateFormat;
            t.relativePeriod;
            t.uiLocale;
            t.displayProperty;
            t.analysisFields;
        };

        AppManager.prototype.getPath = function() {
            return this.path ? this.path : (this.path = this.manifest.activities.dhis.href);
        };

        AppManager.prototype.getDateFormat = function() {
            return this.dateFormat ? this.dateFormat : (this.dateFormat = N.isString(this.systemSettings.keyDateFormat) ? this.systemSettings.keyDateFormat.toLowerCase() : 'yyyy-mm-dd');
        };

        AppManager.prototype.getRelativePeriod = function() {
            return this.relativePeriod ? this.relativePeriod : (this.relativePeriod = this.systemSettings.keyAnalysisRelativePeriod || 'LAST_12_MONTHS');
        };

        AppManager.prototype.getUiLocale = function() {
            return this.uiLocale ? this.uiLocale : (this.uiLocale = this.userAccount.settings.keyUiLocale || this.defaultUiLocale);
        };

        AppManager.prototype.getDisplayProperty = function() {
            if (this.displayProperty) {
                return this.displayProperty;
            }

            //var key = this.userAccount.settings.keyAnalysisDisplayProperty || this.defaultDisplayProperty; //todo
            var key = this.defaultDisplayProperty;
            return this.displayProperty = (key === 'name') ? key : (key + '|rename(name)');
        };

        AppManager.prototype.getValueTypesByType = function(type) {
            return this.valueTypes[type];
        };

        AppManager.prototype.getRootNodes = function() {
            return this.rootNodes;
        };

        AppManager.prototype.addRootNodes = function(param) {
            var t = this,
                nodes = N.arrayFrom(param);

            nodes.forEach(function(node) {
                node.expanded = true;
                node.path = '/' + t.rootId + '/' + node.id;
            });

            t.rootNodes = N.arrayClean(t.rootNodes.concat(nodes));
        };

        AppManager.prototype.addOrganisationUnitLevels = function(param) {
            this.organisationUnitLevels = N.arrayClean(this.organisationUnitLevels.concat(N.arrayFrom(param)));

            N.arraySort(this.organisationUnitLevels, 'ASC', 'level');
        };

        AppManager.prototype.addLegendSets = function(param) {
            this.legendSets = N.arrayClean(this.legendSets.concat(N.arrayFrom(param)));

            N.arraySort(this.legendSets, 'ASC', 'name');
        };

        AppManager.prototype.addDimensions = function(param) {
            this.dimensions = N.arrayClean(this.dimensions.concat(N.arrayFrom(param)));

            N.arraySort(this.dimensions, 'ASC', 'name');
        };

        AppManager.prototype.addDataApprovalLevels = function(param) {
            this.dataApprovalLevels = N.arrayClean(this.dataApprovalLevels.concat(N.arrayFrom(param)));

            N.arraySort(this.dataApprovalLevels, 'ASC', 'level');
        };

        // dep 1

        AppManager.prototype.isUiLocaleDefault = function() {
            return this.getUiLocale() === this.defaultUiLocale;
        };

        AppManager.prototype.getAnalysisFields = function() {
            return this.analysisFields ? this.analysisFields : (this.analysisFields = (this.defaultAnalysisFields.join(',').replaceAll('$', this.getDisplayProperty())));
        };

        AppManager.prototype.getRootNode = function() {
            return this.getRootNodes()[0];
        };

        N.AppManager = new AppManager();
    })();

    // DateManager TODO import
    (function() {
        var DateManager = function() {};

        DateManager.prototype.getYYYYMMDD = function(param) {
            if (!(Object.prototype.toString.call(param) === '[object Date]' && param.toString() !== 'Invalid date')) {
                return null;
            }

            var date = new Date(param),
                month = '' + (1 + date.getMonth()),
                day = '' + date.getDate();

            month = month.length === 1 ? '0' + month : month;
            day = day.length === 1 ? '0' + day : day;

            return date.getFullYear() + '-' + month + '-' + day;
        };

        N.DateManager = new DateManager();
    })();

    // CalendarManager
    (function() {
        var CalendarManager = function(config) {
            var t = this;

            config = N.isObject(config) ? config : {};

            // constructor
            t.baseUrl = config.baseUrl || '..';
            t.dateFormat = config.dateFormat || null;
            t.defaultCalendarId = 'gregorian';
            t.defaultCalendarIsoId = 'iso8601';
            t.calendarIds = ['coptic', 'ethiopian', 'islamic', 'julian', 'nepali', 'thai'];

            // uninitialized
            t.calendar;
            t.periodGenerator;

            // transient
            t.calendarIdMap;
        };

        CalendarManager.prototype.setBaseUrl = function(baseUrl) {
            this.baseUrl = baseUrl;
        };

        CalendarManager.prototype.setDateFormat = function(dateFormat) {
            this.dateFormat = dateFormat;
        };

        CalendarManager.prototype.getPeriodScriptUrl = function() {
            return this.baseUrl + '/dhis-web-commons/javascripts/dhis2/dhis2.period.js';
        };

        CalendarManager.prototype.getCalendarScriptUrl = function(calendarId) {
            return this.baseUrl + '/dhis-web-commons/javascripts/jQuery/calendars/jquery.calendars.' + calendarId + '.min.js';
        };

        CalendarManager.prototype.getCalendarIdMap = function() {
            if (this.calendarIdMap) {
                return this.calendarIdMap;
            }

            this.calendarIdMap = {};
            this.calendarIdMap[this.defaultCalendarIsoId] = this.defaultCalendarId;

            return this.calendarIdMap;
        };

        CalendarManager.prototype.createCalendar = function(calendarId) {
            this.calendar = $.calendars.instance(calendarId);
        };

        CalendarManager.prototype.createPeriodGenerator = function(calendarId) {
            this.periodGenerator = new dhis2.period.PeriodGenerator(this.calendar, this.dateFormat);

        };

        // dep 1

        CalendarManager.prototype.generate = function(calendarId, dateFormat) {
            calendarId = this.getCalendarIdMap()[calendarId] || calendarId || this.defaultCalendarId;

            if (this.calendar && this.periodGenerator) {
                return;
            }

            var t = this,
                periodUrl = t.getPeriodScriptUrl(),
                success = function() {
                    t.createCalendar(calendarId);
                    t.createPeriodGenerator(calendarId);
                };

            if (N.arrayContains(t.calendarIds, calendarId)) {
                var calendarUrl = t.getCalendarScriptUrl(calendarId);

                $.getScript(calendarUrl, function() {
                    $.getScript(periodUrl, function() {
                        success();
                    });
                });
            }
            else {
                $.getScript(periodUrl, function() {
                    success();
                });
            }
        };

        N.CalendarManager = new CalendarManager();
    })();

    // I18nManager
    (function() {
        var I18nManager = function(config) {
            this.map = config || {};
        };

        I18nManager.prototype.get = function(key) {
            return this.map[key];
        };

        I18nManager.prototype.add = function(obj) {
            $.extend(this.map, obj);
        };

        N.I18nManager = new I18nManager();
    })();

    // DimensionConfig
    (function() {
        var DimensionConfig = function() {
            var t = this;

            // private
            var dimensions = {
                data: {
                    value: 'data',
                    name: N.I18nManager.get('data') || 'Data',
                    dimensionName: 'dx',
                    objectName: 'dx'
                },
                category: {
                    name: N.I18nManager.get('assigned_categories') || 'Assigned categories',
                    dimensionName: 'co',
                    objectName: 'co',
                },
                indicator: {
                    value: 'indicators',
                    name: N.I18nManager.get('indicators') || 'Indicators',
                    dimensionName: 'dx',
                    objectName: 'in'
                },
                dataElement: {
                    value: 'dataElements',
                    name: N.I18nManager.get('data_elements') || 'Data elements',
                    dimensionName: 'dx',
                    objectName: 'de'
                },
                operand: {
                    value: 'operand',
                    name: N.I18nManager.get('operand') || 'Operand',
                    dimensionName: 'dx',
                    objectName: 'dc'
                },
                dataSet: {
                    value: 'dataSets',
                    name: N.I18nManager.get('data_sets') || 'Data sets',
                    dimensionName: 'dx',
                    objectName: 'ds'
                },
                eventDataItem: {
                    value: 'eventDataItem',
                    name: N.I18nManager.get('event_data_items') || 'Event data items',
                    dimensionName: 'dx',
                    objectName: 'di'
                },
                programIndicator: {
                    value: 'programIndicator',
                    name: N.I18nManager.get('program_indicators') || 'Program indicators',
                    dimensionName: 'dx',
                    objectName: 'pi'
                },
                period: {
                    value: 'period',
                    name: N.I18nManager.get('periods') || 'Periods',
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
                    name: N.I18nManager.get('N.i18n.organisation_units') || 'Organisation units',
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
            };

            // prototype
            t.add = function(param) {
                N.arrayFrom(param).forEach(function(dimension) {
                    dimension.dimensionName = dimension.id;
                    dimension.objectName = dimension.id;

                    dimensions[dimension.id] = dimension;
                });
            };

            t.get = function(name) {
                return dimensions[name];
            };

            t.getDimensionNameMap = function() {
                var map = {};

                for (var name in dimensions) {
                    if (dimensions.hasOwnProperty(name)) {
                        map[dimensions[name].dimensionName] = dimensions[name];
                    }
                }

                return map;
            };

            t.getObjectNameMap = function() {
                var map = {};

                for (var name in dimensions) {
                    if (dimensions.hasOwnProperty(name)) {
                        map[dimensions[name].objectName] = dimensions[name];
                    }
                }

                return map;
            };
        };

        N.DimConf = new DimensionConfig();
    })();

    // PeriodConfig
    (function() {
        var PeriodConfig = function() {
            var t = this;

            // private
            var periodTypes = {
                'Daily': N.I18nManager.get('daily') || 'Daily',
                'Weekly': N.I18nManager.get('weekly') || 'Weekly',
                'Monthly': N.I18nManager.get('monthly') || 'Monthly',
                'BiMonthly': N.I18nManager.get('bimonthly') || 'BiMonthly',
                'Quarterly': N.I18nManager.get('quarterly') || 'Quarterly',
                'SixMonthly': N.I18nManager.get('sixmonthly') || 'SixMonthly',
                'SixMonthlyApril': N.I18nManager.get('sixmonthly_april') || 'SixMonthly April',
                'Yearly': N.I18nManager.get('yearly') || 'Yearly',
                'FinancialOct': N.I18nManager.get('financial_oct') || 'Financial October',
                'FinancialJuly': N.I18nManager.get('financial_july') || 'Financial July',
                'FinancialApril': N.I18nManager.get('financial_april') || 'Financial April'
            };

            // uninitialized
            var periodTypeRecords;

            // prototype
            t.getPeriodTypeRecords = function() {
                if (periodTypeRecords) {
                    return periodTypeRecords;
                }

                var records = [];

                for (var type in periodTypes) {
                    if (periodTypes.hasOwnProperty(type)) {
                        records.push({
                            id: type,
                            name: periodTypes[type]
                        });
                    }
                }

                return periodTypeRecords = records;
            };
        };

        N.PeriodConf = new PeriodConfig();
    })();

    // OptionConfig
    (function() {
        var OptionConfig = function() {
            var t = this;

            // private
            var displayDensity = {
                'comfortable': {
                    index: 1,
                    id: 'COMFORTABLE',
                    name: N.I18nManager.get('comfortable') || 'Comfortable'
                },
                'normal': {
                    index: 2,
                    id: 'NORMAL',
                    name: N.I18nManager.get('normal') || 'Normal'
                },
                'compact': {
                    index: 3,
                    id: 'COMPACT',
                    name: N.I18nManager.get('compact') || 'Compact'
                }
            };

            var fontSize = {
                'large': {
                    index: 1,
                    id: 'LARGE',
                    name: N.I18nManager.get('large') || 'Large'
                },
                'normal': {
                    index: 2,
                    id: 'NORMAL',
                    name: N.I18nManager.get('normal') || 'Normal'
                },
                'small': {
                    index: 3,
                    id: 'SMALL',
                    name: N.I18nManager.get('small') || 'Small'
                }
            };

            var digitGroupSeparator = {
                'none': {
                    index: 1,
                    id: 'NONE',
                    name: N.I18nManager.get('none') || 'None',
                    value: ''
                },
                'space': {
                    index: 2,
                    id: 'SPACE',
                    name: N.I18nManager.get('space') || 'Space',
                    value: '&nbsp;'
                },
                'comma': {
                    index: 3,
                    id: 'COMMA',
                    name: N.I18nManager.get('comma') || 'Comma',
                    value: ','
                }
            };

            var aggregationType = {
                'def': {
                    index: 1,
                    id: 'DEFAULT',
                    name: N.I18nManager.get('by_data_element') || 'By data element'
                },
                'count': {
                    index: 2,
                    id: 'COUNT',
                    name: N.I18nManager.get('count') || 'Count'
                },
                'sum': {
                    index: 3,
                    id: 'SUM',
                    name: N.I18nManager.get('sum') || 'Sum'
                },
                'average': {
					index: 4,
					id: 'AVERAGE',
					name: N.I18nManager.get('average') || 'Average'
				},
                'stddev': {
                    index: 5,
                    id: 'STDDEV',
                    name: N.I18nManager.get('stddev') || 'Standard deviation'
                },
                'variance': {
                    index: 6,
                    id: 'VARIANCE',
                    name: N.I18nManager.get('variance') || 'Variance'
                },
                'min': {
                    index: 7,
                    id: 'MIN',
                    name: N.I18nManager.get('min') || 'Min'
                },
                'max': {
                    index: 8,
                    id: 'MAX',
                    name: N.I18nManager.get('max') || 'Max'
                }
            };

            // uninitialized
            var displayDensityRecords;
            var fontSizeRecords;
            var digitGroupSeparatorRecords;
            var aggregationTypeRecords;

            var digitGroupSeparatorIdMap;

            // prototype
            t.getDisplayDensity = function(key) {
                return displayDensity[key];
            };

            t.getFontSize = function(key) {
                return fontSize[key];
            };

            t.getDigitGroupSeparator = function(key) {
                return digitGroupSeparator[key];
            };

            t.getAggregationType = function(key) {
                return aggregationType[key];
            };

            t.getDisplayDensityRecords = function() {
                if (displayDensityRecords) {
                    return displayDensityRecords;
                }

                var records = [];

                for (var option in displayDensity) {
                    if (displayDensity.hasOwnProperty(option)) {
                        records.push(displayDensity[option]);
                    }
                }

                N.arraySort(records, 'ASC', 'index');

                return displayDensityRecords = records;
            };

            t.getFontSizeRecords = function() {
                if (fontSizeRecords) {
                    return fontSizeRecords;
                }

                var records = [];

                for (var option in fontSize) {
                    if (fontSize.hasOwnProperty(option)) {
                        records.push(fontSize[option]);
                    }
                }

                N.arraySort(records, 'ASC', 'index');

                return fontSizeRecords = records;
            };

            t.getDigitGroupSeparatorRecords = function() {
                if (digitGroupSeparatorRecords) {
                    return digitGroupSeparatorRecords;
                }

                var records = [];

                for (var option in digitGroupSeparator) {
                    if (digitGroupSeparator.hasOwnProperty(option)) {
                        records.push(digitGroupSeparator[option]);
                    }
                }

                N.arraySort(records, 'ASC', 'index');

                return digitGroupSeparatorRecords = records;
            };

            t.getAggregationTypeRecords = function() {
                if (aggregationTypeRecords) {
                    return aggregationTypeRecords;
                }

                var records = [];

                for (var option in aggregationType) {
                    if (aggregationType.hasOwnProperty(option)) {
                        records.push(aggregationType[option]);
                    }
                }

                N.arraySort(records, 'ASC', 'index');

                return aggregationTypeRecords = records;
            };

            t.getDigitGroupSeparatorIdMap = function() {
                if (digitGroupSeparatorIdMap) {
                    return digitGroupSeparatorIdMap;
                }

                var map = {};

                for (var separator in digitGroupSeparator) {
                    if (digitGroupSeparator.hasOwnProperty(separator)) {
                        map[digitGroupSeparator[separator].id] = digitGroupSeparator[separator];
                    }
                }

                return digitGroupSeparatorIdMap = map;
            };

            // dep 1

            t.getDigitGroupSeparatorValueById = function(id) {
                var separator = t.getDigitGroupSeparatorIdMap()[id];
                return separator ? separator.value : '';
            };
        };

        N.OptionConf = new OptionConfig();
    })();

    // UiConfig
    (function() {
        var UiConfig = function() {
            var t = this;

            $.extend(t, {
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
			});
        };

        N.UiConf = new UiConfig();
    })();

    // UrlConfig
    (function() {
        var UrlConfig = function() {
            var t = this;

            t.analysisFields = [
                '*',
                'program[id,name]',
                'programStage[id,name]',
                'columns[dimension,filter,items[id,' + init.namePropertyUrl + ']]',
                'rows[dimension,filter,items[id,' + init.namePropertyUrl + ']]',
                'filters[dimension,filter,items[id,' + init.namePropertyUrl + ']]',
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
            ];
        };

        N.UrlConf = new UrlConfig();
    })();

    // Api
    (function() {
        N.Api = {};

        // Record
        (function() {
            var Record = N.Api.Record = function(config) {
                var t = this;

                config = N.isObject(config) ? config : {};

                // constructor
                t.id = config.id;
                t.name = config.name;
            };

            Record.prototype.log = function(text, noError) {
                if (!noError) {
                    console.log(text, this);
                }
            };

            Record.prototype.val = function(noError) {
                if (!N.isString(this.id)) {
                    this.log('(Record) Id is not a string', noError);
                    return null;
                }

                return this;
            };
        })();

        // Dimension
        (function() {
            var Dimension = N.Api.Dimension = function(config) {
                var t = this,
                    items = [];

                config = N.isObject(config) ? config : {};
                config.items = N.arrayFrom(config.items);

                // constructor
                t.dimension = config.dimension;

                t.items = config.items.map(function(record) {
                    return (new N.Api.Record(record)).val();
                });
            };

            Dimension.prototype.log = function(text, noError) {
                if (!noError) {
                    console.log(text, this);
                }
            };

            Dimension.prototype.val = function(noError) {
                if (!N.isString(this.dimension)) {
                    this.log('(Dimension) Dimension is not a string', noError);
                    return null;
                }

                if (!this.items.length && this.dimension !== 'co') {
                    this.log('(Dimension) No items', noError);
                    return null;
                }

                return this;
            };

            Dimension.prototype.getRecords = function(sortProperty, response) {
                var records = response ? response.getRecordsByDimensionName(this.dimension) : this.items;

                sortProperty = N.arrayContains(['id', 'name'], sortProperty) ? sortProperty : null;

                return sortProperty ? records.sort(function(a, b) { return a[sortProperty] > b[sortProperty];}) : records;
            };

            // dep 1

            Dimension.prototype.getRecordIds = function(isSorted, response) {
                return N.arrayPluck(this.getRecords((isSorted ? 'id' : null), response), 'id');
            };

            Dimension.prototype.getRecordNames = function(isSorted, response) {
                return N.arrayPluck(this.getRecords((isSorted ? 'name' : null), response), 'name');
            };

            // dep 2

            Dimension.prototype.url = function(isSorted, response) {
                return 'dimension=' + this.dimension + ':' + N.arrayUnique(this.getRecordIds(false, response)).join(';');
            };
        })();

        // Axis (array)
        (function() {
            var Axis = N.Api.Axis = function(config) {
                var t = [];

                config = N.arrayFrom(config);

                // constructor
                config.forEach(function(dimension) {
                    t.push((new N.Api.Dimension(dimension)).val());
                });

                // prototype
                t.log = function(text, noError) {
                    if (!noError) {
                        console.log(text, this);
                    }
                };

                t.val = function(noError) {
                    if (!this.length) {
                        this.log('(Axis) No dimensions', noError);
                        return null;
                    }

                    return this;
                };

                t.has = function(dimensionName) {
                    return this.some(function(dimension) {
                        return dimension.dimension === dimensionName;
                    });
                };

                t.getDimensionNames = function() {
                    var names = [];

                    this.forEach(function(dimension) {
                        names.push(dimension.dimension);
                    });

                    return names;
                };

                t.sorted = function() {
                    return N.clone(this).sort(function(a, b) { return a.dimension > b.dimension;});
                };

                return t;
            };
        })();

        // Layout
        (function() {
            var Layout = N.Api.Layout = function(config, applyConfig, forceApplyConfig) {
                var t = this;

                config = N.isObject(config) ? config : {};
                $.extend(config, applyConfig);

                // constructor
                t.columns = (N.Api.Axis(config.columns)).val();
                t.rows = (N.Api.Axis(config.rows)).val();
                t.filters = (N.Api.Axis(config.filters)).val();

                t.showColTotals = N.isBoolean(config.colTotals) ? config.colTotals : (N.isBoolean(config.showColTotals) ? config.showColTotals : true);
                t.showRowTotals = N.isBoolean(config.rowTotals) ? config.rowTotals : (N.isBoolean(config.showRowTotals) ? config.showRowTotals : true);
                t.showColSubTotals = N.isBoolean(config.colSubTotals) ? config.colSubTotals : (N.isBoolean(config.showColSubTotals) ? config.showColSubTotals : true);
                t.showRowSubTotals = N.isBoolean(config.rowSubTotals) ? config.rowSubTotals : (N.isBoolean(config.showRowSubTotals) ? config.showRowSubTotals : true);
                t.showDimensionLabels = N.isBoolean(config.showDimensionLabels) ? config.showDimensionLabels : (N.isBoolean(config.showDimensionLabels) ? config.showDimensionLabels : true);
                t.hideEmptyRows = N.isBoolean(config.hideEmptyRows) ? config.hideEmptyRows : false;
                t.skipRounding = N.isBoolean(config.skipRounding) ? config.skipRounding : false;
                t.aggregationType = N.isString(config.aggregationType) ? config.aggregationType : N.OptionConf.getAggregationType('def').id;
                t.dataApprovalLevel = N.isObject(config.dataApprovalLevel) && N.isString(config.dataApprovalLevel.id) ? config.dataApprovalLevel : null;
                t.showHierarchy = N.isBoolean(config.showHierarchy) ? config.showHierarchy : false;
                t.completedOnly = N.isBoolean(config.completedOnly) ? config.completedOnly : false;
                t.displayDensity = N.isString(config.displayDensity) && !N.isEmpty(config.displayDensity) ? config.displayDensity : N.OptionConf.getDisplayDensity('normal').id;
                t.fontSize = N.isString(config.fontSize) && !N.isEmpty(config.fontSize) ? config.fontSize : N.OptionConf.getFontSize('normal').id;
                t.digitGroupSeparator = N.isString(config.digitGroupSeparator) && !N.isEmpty(config.digitGroupSeparator) ? config.digitGroupSeparator : N.OptionConf.getDigitGroupSeparator('space').id;

                t.legendSet = (new N.Api.Record(config.legendSet)).val(true);

                t.parentGraphMap = N.isObject(config.parentGraphMap) ? config.parentGraphMap : null;

                if (N.isObject(config.program)) {
                    t.program = config.program;
                }

                    // report table
                t.reportingPeriod = N.isObject(config.reportParams) && N.isBoolean(config.reportParams.paramReportingPeriod) ? config.reportParams.paramReportingPeriod : (N.isBoolean(config.reportingPeriod) ? config.reportingPeriod : false);
                t.organisationUnit =  N.isObject(config.reportParams) && N.isBoolean(config.reportParams.paramOrganisationUnit) ? config.reportParams.paramOrganisationUnit : (N.isBoolean(config.organisationUnit) ? config.organisationUnit : false);
                t.parentOrganisationUnit = N.isObject(config.reportParams) && N.isBoolean(config.reportParams.paramParentOrganisationUnit) ? config.reportParams.paramParentOrganisationUnit : (N.isBoolean(config.parentOrganisationUnit) ? config.parentOrganisationUnit : false);

                t.regression = N.isBoolean(config.regression) ? config.regression : false;
                t.cumulative = N.isBoolean(config.cumulative) ? config.cumulative : false;
                t.sortOrder = N.isNumber(config.sortOrder) ? config.sortOrder : 0;
                t.topLimit = N.isNumber(config.topLimit) ? config.topLimit : 0;

                    // non model

                    // id
                if (N.isString(config.id)) {
                    t.id = config.id;
                }

                    // name
                if (N.isString(config.name)) {
                    t.name = config.name;
                }

                    // sorting
                if (N.isObject(config.sorting) && N.isDefined(config.sorting.id) && N.isString(config.sorting.direction)) {
                    t.sorting = config.sorting;
                }

                    // displayProperty
                if (N.isString(config.displayProperty)) {
                    t.displayProperty = config.displayProperty;
                }

                    // userOrgUnit
                if (N.arrayFrom(config.userOrgUnit).length) {
                    t.userOrgUnit = N.arrayFrom(config.userOrgUnit);
                }

                    // relative period date
                if (N.DateManager.getYYYYMMDD(config.relativePeriodDate)) {
                    t.relativePeriodDate = N.DateManager.getYYYYMMDD(config.relativePeriodDate);
                }

                $.extend(t, forceApplyConfig);

                // uninitialized
                t.dimensionNameRecordIdsMap;
            };

            Layout.prototype.log = function(text, noError) {
                if (!noError) {
                    console.log(text, this);
                }
            };

            Layout.prototype.alert = function(text, noError) {
                if (!noError) {
                    alert(text);
                }
            };

            Layout.prototype.getAxes = function(includeFilter) {
                return N.arrayClean([this.columns, this.rows, (includeFilter ? this.filters : null)]);
            };

            Layout.prototype.getUserOrgUnitUrl = function() {
                if (N.isArray(this.userOrgUnit) && this.userOrgUnit.length) {
                    return 'userOrgUnit=' + this.userOrgUnit.join(';');
                }
            };

            // dep 1

            Layout.prototype.hasDimension = function(dimensionName, includeFilter) {
                return this.getAxes(includeFilter).some(function(axis) {
                    return axis.has(dimensionName);
                });
            };

            Layout.prototype.getDimensions = function(includeFilter, isSorted) {
                var dimensions = [];

                this.getAxes(includeFilter).forEach(function(axis) {
                    dimensions = dimensions.concat(axis);
                });

                return isSorted ? dimensions.sort(function(a, b) {return a.dimension > b.dimension;}) : dimensions;
            };

            Layout.prototype.getDimensionNameRecordIdsMap = function(response) {
                //if (this.dimensionNameRecordIdsMap) {
                    //return this.dimensionNameRecordIdsMap;
                //}

                var map = {};

                this.getDimensions(true).forEach(function(dimension) {
                    map[dimension.dimension] = dimension.getRecordIds(false, response);
                });

                return this.dimensionNameRecordIdsMap = map;
            };

            // dep 2

            Layout.prototype.getDimensionNames = function(includeFilter, isSorted) {
                var names = N.arrayPluck(this.getDimensions(includeFilter), 'dimension');

                return isSorted ? names.sort() : names;
            };

            Layout.prototype.val = function(noError) {

                if (!(this.columns || this.rows)) {
                    this.alert(N.I18nManager.get('at_least_one_dimension_must_be_specified_as_row_or_column'), noError); //todo alert
                    return null;
                }

                if (!this.hasDimension(N.DimConf.get('period').dimensionName)) {
                    this.alert(N.I18nManager.get('at_least_one_period_must_be_specified_as_column_row_or_filter'), noError); //todo alert
                    return null;
                }

                return this;
            };

            Layout.prototype.req = function(baseUrl, isSorted) {
                var aggTypes = ['COUNT', 'SUM', 'AVERAGE', 'STDDEV', 'VARIANCE', 'MIN', 'MAX'],
                    //displayProperty = this.displayProperty || init.userAccount.settings.keyAnalysisDisplayProperty || 'name',
                    displayProperty = this.displayProperty || 'name',
                    request = new N.Api.Request(),
                    i;

                // dimensions
                this.getDimensions(false, isSorted).forEach(function(dimension) {
                    request.add(dimension.url(isSorted));
                });

                // filters
                if (this.filters) {
                    this.filters.forEach(function(dimension) {
                        request.add(dimension.url(isSorted));
                    });
                }

                // hierarchy
                if (this.showHierarchy) {
                    request.add('hierarchyMeta=true');
                }

                // completed only
                if (this.completedOnly) {
                    request.add('completedOnly=true');
                }

                // aggregation type
                if (N.arrayContains(aggTypes, this.aggregationType)) {
                    request.add('aggregationType=' + this.aggregationType);
                }

                // user org unit
                if (N.isArray(this.userOrgUnit) && this.userOrgUnit.length) {
                    request.add(this.getUserOrgUnitUrl());
                }

                // data approval level
                if (N.isObject(this.dataApprovalLevel) && N.isString(this.dataApprovalLevel.id) && this.dataApprovalLevel.id !== 'DEFAULT') {
                    request.add('approvalLevel=' + this.dataApprovalLevel.id);
                }

                // TODO program
                if (N.isObject(this.program) && N.isString(this.program.id)) {
                    request.add('program=' + this.program.id);
                }

                // relative period date
                if (this.relativePeriodDate) {
                    request.add('relativePeriodDate=' + this.relativePeriodDate);
                }

                // skip rounding
                if (this.skipRounding) {
                    request.add('skipRounding=true');
                }

                // display property
                request.add('displayProperty=' + displayProperty.toUpperCase());

                // base url
                if (baseUrl) {
                    request.setBaseUrl(baseUrl);
                }

                return request;
            };

            // dep 3

            Layout.prototype.data = function(request) {
                if (request) {
                    return $.getJSON('/api/analytics.json' + request.url());
                }

                var baseUrl = '/api/analytics.json',
                    metaDataRequest = this.req(baseUrl, true),
                    dataRequest = this.req(baseUrl);

                return {
                    metaData: $.getJSON(metaDataRequest.url('skipData=true')),
                    data: $.getJSON(dataRequest.url('skipMeta=true'))
                };
            };
        })();

        // Request
        (function() {
            var Request = N.Api.Request = function(config) {
                var t = this;

                config = N.isObject(config) ? config : {};

                // constructor
                t.baseUrl = N.isString(config.baseUrl) ? config.baseUrl : '';
                t.params = N.arrayFrom(config.params);
                t.manager = config.manager || null;
                t.type = N.isString(config.type) ? config.type : 'json';
                t.success = N.isFunction(config.success) ? config.success : function() { t.defaultSuccess(); };
                t.error = N.isFunction(config.error) ? config.error : function() { t.defaultError(); };
                t.complete = N.isFunction(config.complete) ? config.complete : function() { t.defaultComplete(); };

                // defaults
                t.defaultSuccess = function() {
                    var t = this;

                    if (t.manager) {
                        t.manager.ok(t);
                    }
                };

                t.defaultError = function() {};
                t.defaultComplete = function() {};
            };

            Request.prototype.log = function(text, noError) {
                if (!noError) {
                    console.log(text, this);
                }
            };

            Request.prototype.alert = function(text, noError) {
                if (!noError) {
                    alert(text);
                }
            };

            Request.prototype.handle = function(statusCode, noError) {
                var url = this.url(),
                    text;

                if (N.arrayContains([413, 414], statusCode)) {
                    if (N.isIE()) {
                        text = 'Too many items selected (url has ' + url.length + ' characters). Internet Explorer accepts maximum 2048 characters.';
                    }
                    else {
                        var len = url.length > 8000 ? '8000' : (url.length > 4000 ? '4000' : '2000');
                        text = 'Too many items selected (url has ' + url.length + ' characters). Please reduce to less than ' + len + ' characters.';
                    }
                }

                if (text) {
                    text += '\n\n' + 'Hint: A good way to reduce the number of items is to use relative periods and level/group organisation unit selection modes.';

                    this.alert(text);
                }
            };

            Request.prototype.add = function(param) {
                var t = this;

                if (N.isString(param)) {
                    t.params.push(param);
                }
                else if (N.isArray(param)) {
                    param.forEach(function(item) {
                        if (N.isString(item)) {
                            t.params.push(item);
                        }
                    });
                }
                else if (N.isObject(param)) {
                    for (var key in param) {
                        if (param.hasOwnProperty(key)) {
                            t.params.push(key + '=' + param[key]);
                        }
                    }
                }

                return this;
            };

            Request.prototype.setBaseUrl = function(baseUrl) {
                if (N.isString(baseUrl)) {
                    this.baseUrl = baseUrl;
                }
            };

            Request.prototype.setType = function(type) {
                if (N.isString(type)) {
                    this.type = type;
                }
            };

            Request.prototype.setManager = function(manager) {
                this.manager = manager;
            };

            Request.prototype.setSuccess = function(fn) {
                if (N.isFunction(fn)) {
                    this.success = fn;
                }
            };

            Request.prototype.setError = function(fn) {
                if (N.isFunction(fn)) {
                    this.error = fn;
                }
            };

            Request.prototype.setComplete = function(fn) {
                if (N.isFunction(fn)) {
                    this.complete = fn;
                }
            };

            Request.prototype.url = function(extraParams) {
                extraParams = N.arrayFrom(extraParams);

                return this.baseUrl + '?' + ([].concat(this.params, N.arrayFrom(extraParams))).join('&');
            };

            // dep 1

            Request.prototype.run = function(config) {
                var t = this;

                config = N.isObject(config) ? config : {};

                if (this.type === 'ajax') {
                    return $.ajax({
                        url: this.url(),
                        success: config.success || t.success,
                        error: config.error || t.error,
                        complete: config.complete || t.complete
                    });
                }
                else {
                    return $.getJSON(this.url(), config.success || t.success).error(config.error || t.error).complete(config.complete || t.complete);
                }
            };
        })();

        // RequestManager
        (function() {
            var RequestManager = N.Api.RequestManager = function(config) {
                var t = this;

                config = N.isObject(config) ? config : {};

                // constructor
                t.requests = N.isArray(config.requests) ? config.requests : [];

                t.responses = [];

                t.fn = N.isFunction(config.fn) ? config.fn : function() { console.log("Request manager is done"); };
            };

            RequestManager.prototype.add = function(param) {
                var t = this,
                    requests = N.arrayFrom(param);

                requests.forEach(function(request) {
                    request.setManager(t);
                });

                this.requests = [].concat(this.requests, requests);
            };

            RequestManager.prototype.set = function(fn) {
                this.fn = fn;
            };

            RequestManager.prototype.ok = function(xhr, suppress) {
                this.responses.push(xhr);

                if (!suppress) {
                    this.resolve();
                }
            };

            RequestManager.prototype.run = function() {
                this.requests.forEach(function(request) {
                    request.run();
                });
            };

            RequestManager.prototype.resolve = function() {
                if (this.responses.length === this.requests.length) {
                    this.fn();
                }
            };
        })();

        // ResponseHeader
        (function() {
            var ResponseHeader = N.Api.ResponseHeader = function(config) {
                var t = this;

                config = N.isObject(config) ? config : {};

                // constructor
                $.extend(this, config);

                // uninitialized
                t.index;
            };

            ResponseHeader.prototype.setIndex = function(index) {
                if (N.isNumeric(index)) {
                    this.index = parseInt(index);
                }
            };

            ResponseHeader.prototype.getIndex = function(index) {
                return this.index;
            };
        })();

        // ResponseRow (array)
        (function() {
            var ResponseRow = N.Api.ResponseRow = function(config) {
                var t = N.arrayFrom(config);

                t.getAt = function(index) {
                    return this[index];
                };

                t.setIdCombination = function(idCombination) {
                    this.idCombination = idCombination;
                };

                // uninitialized
                t.idCombination;

                return t;
            };
        })();

        // ResponseRowIdCombination
        (function() {
            var ResponseRowIdCombination = N.Api.ResponseRowIdCombination = function(config) {
                var t = this;

                config = N.isArray(config) ? config : (N.isString(config) ? config.split('-') : null);

                // constructor
                t.ids = config || [];
            };

            ResponseRowIdCombination.prototype.add = function(id) {
                this.ids.push(id);
            };

            ResponseRowIdCombination.prototype.get = function() {
                return this.ids.join('-');
            };
        })();

        // Response
        (function() {
            var Response = N.Api.Response = function(config) {
                var t = this;

                config = N.isObject(config) ? config : {};

                // constructor
                t.headers = (config.headers || []).map(function(header) {
                    return new N.Api.ResponseHeader(header);
                });

                t.metaData = config.metaData;

                t.rows = (config.rows || []).map(function(row) {
                    return N.Api.ResponseRow(row);
                });

                // transient
                t.nameHeaderMap = function() {
                    var map = {};

                    t.headers.forEach(function(header) {
                        map[header.name] = header;
                    });

                    return map;
                }();

                // uninitialized
                t.idValueMap;

                // ResponseHeader: index
                t.headers.forEach(function(header, index) {
                    header.setIndex(index);
                });
            };

            Response.prototype.getHeaderByName = function(name) {
                return this.nameHeaderMap[name];
            };

            Response.prototype.getHeaderIndexByName = function(name) {
                return this.nameHeaderMap[name].index;
            };

            Response.prototype.getNameById = function(id) {
                return this.metaData.names[id] || '';
            };

            Response.prototype.getHierarchyNameById = function(id, isHierarchy, isHtml) {
				var metaData = response.metaData,
					name = '';

				if (isHierarchy) {
					var a = N.arrayClean(metaData.ouHierarchy[id].split('/'));
					a.shift();

					for (var i = 0; i < a.length; i++) {
						name += (isHtml ? '<span class="text-weak">' : '') + metaData.names[a[i]] + (isHtml ? '</span>' : '') + ' / ';
					}
				}

                return name;
            };

            Response.prototype.getIdsByDimensionName = function(dimensionName) {
                return this.metaData[dimensionName] || [];
            };

            // dep 1

            Response.prototype.getHeaderIndexOrder = function(dimensionNames) {
                var t = this,
                    headerIndexOrder = [];

                dimensionNames.forEach(function(name) {
                    headerIndexOrder.push(t.getHeaderIndexByName(name));
                });

                return headerIndexOrder;
            };

            Response.prototype.getItemName = function(id, isHierarchy, isHtml) {
                return this.getHierarchyNameById(id, isHierarchy) + this.getNameById(id);
            };

            Response.prototype.getRecordsByDimensionName = function(dimensionName) {
                var metaData = this.metaData,
                    ids = metaData[dimensionName],
                    records = [];

                ids.forEach(function(id) {
                    records.push((new N.Api.Record({
                        id: id,
                        name: metaData.names[id]
                    })).val());
                });

                return records;
            };

            Response.prototype.getValueHeader = function() {
                return this.getHeaderByName('value');
            };

            // dep 2

            Response.prototype.getValueHeaderIndex = function() {
                return this.getValueHeader().getIndex();
            };

            // dep 3

            Response.prototype.getIdValueMap = function(layout) {
                if (this.idValueMap) {
                    return this.idValueMap;
                }

                var t = this,
                    headerIndexOrder = response.getHeaderIndexOrder(layout.getDimensionNames()),
                    idValueMap = {},
                    idCombination;

                this.rows.forEach(function(responseRow) {
                    idCombination = new N.Api.ResponseRowIdCombination();

                    headerIndexOrder.forEach(function(index) {
                        idCombination.add(responseRow.getAt(index));
                    });

                    responseRow.setIdCombination(idCombination);

                    idValueMap[idCombination.get()] = responseRow.getAt(t.getValueHeaderIndex());
                });

                return this.idValueMap = idValueMap;
            };

            // dep 4

            Response.prototype.getValue = function(param, layout) {
                var id = param instanceof N.Api.ResponseRowIdCombination ? param.get() : param;

                return this.getIdValueMap(layout)[param];
            };

            // dep 5

            Response.prototype.getValues = function(paramArray, layout) {
                var t = this,
                    values = [],
                    id;

                paramArray = N.arrayFrom(paramArray);

                paramArray.forEach(function(param) {
                    values.push(t.getValue(param, layout));
                });

                return values;
            };
        })();

        //todo TableAxis
        (function() {
            var TableAxis = N.Api.TableAxis = function(layout, response, type) {
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
					dimensionNames = layout.columns.getDimensionNames(response);
					spanType = 'colSpan';
				}
				else if (type === 'row') {
					dimensionNames = layout.rows.getDimensionNames(response);
					spanType = 'rowSpan';
				}

				if (!(N.isArray(dimensionNames) && dimensionNames.length)) {
					return;
				}
	//dimensionNames = ['pe', 'ou'];

				// aDimensions: array of dimension objects with dimensionName property
                dimensionNames.forEach(function(name) {
                    aDimensions.push({
						dimensionName: name
					});
                });
	//aDimensions = [{
		//dimensionName: 'pe'
	//}]

				// aaUniqueFloorIds: array of arrays with unique ids for each dimension floor
				aaUniqueFloorIds = function() {
					var a = [];

                    aDimensions.forEach(function(dimension) {
                        a.push(layout.getDimensionNameRecordIdsMap(response)[dimension.dimensionName]);
                    });

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
							if (layout.hideEmptyRows && type === 'row') {
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
				for (var i = 0, ids; i < nAxisWidth; i++) {
					ids = [];

					for (var j = 0; j < nAxisHeight; j++) {
						ids.push(aaAllFloorIds[j][i]);
					}

					if (ids.length) {
						aCondoId.push(ids.join('-'));
					}
				}
	//aCondoId = [ id11+id21+id31, id12+id22+id32, ... ]


				// allObjects
				for (var i = 0, allFloor; i < aaAllFloorIds.length; i++) {
					allFloor = [];

					for (var j = 0, obj; j < aaAllFloorIds[i].length; j++) {
						obj = {
							id: aaAllFloorIds[i][j],
							uuid: N.uuid(),
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
                        uniqueDoorIds = N.arrayUnique(doorIds);

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
					var nSpan = nAxisHeight > 1 ? N.arraySort(N.clone(aFloorSpan))[1] : nAxisWidth,
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
						leaf.uuids = N.clone(parentUuids);

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
        })();

        //todo Table
        (function() {
            var Table = N.Api.Table = function(layout, response, colAxis, rowAxis) {
                var t = this;

                // init
				var getRoundedHtmlValue,
					getTdHtml,
                    getValue,
                    roundIf,
                    getNumberOfDecimals,
                    prettyPrint,
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
					colUniqueFactor = getUniqueFactor(colAxis),
					rowUniqueFactor = getUniqueFactor(rowAxis),
					valueItems = [],
					valueObjects = [],
					totalColObjects = [],
					uuidDimUuidsMap = {},
					//isLegendSet = N.isObject(xLayout.legendSet) && N.isArray(xLayout.legendSet.legends) && xLayout.legendSet.legends.length,
					isLegendSet = false,
                    tdCount = 0,
                    htmlArray,
                    dimensionNameMap = N.DimConf.getDimensionNameMap(),
                    objectNameMap = N.DimConf.getObjectNameMap(),
                    idValueMap = response.getIdValueMap(layout);

				response.sortableIdObjects = []; //todo

				getRoundedHtmlValue = function(value, dec) {
					dec = dec || 2;
					return parseFloat(roundIf(value, 2)).toString();
				};

				getTdHtml = function(config, metaDataId) {
					var bgColor,
						legends,
						colSpan,
						rowSpan,
						htmlValue,
						displayDensity,
						fontSize,
						isNumeric = N.isObject(config) && N.isString(config.type) && config.type.substr(0,5) === 'value' && !config.empty,
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
                            if (N.isBoolean(str)) {
                                return str;
                            }

                            //if (!N.isNumber(n) || n != str || new Date(str).toString() !== 'Invalid Date') {
                            if (!N.isNumber(n) || n != str) {
                                return str;
                            }

                            return n;
                        }

                        return str || '';
                    }

					if (!N.isObject(config)) {
						return '';
					}

                    if (config.hidden || config.collapsed) {
                        return '';
                    }

                    // number of cells
                    tdCount = tdCount + 1;

					// background color from legend set
					if (isValue && layout.legendSet) {
						var value = parseFloat(config.value);
						legends = layout.legendSet.legends;

						for (var i = 0; i < legends.length; i++) {
							if (N.numberConstrain(value, legends[i].startValue, legends[i].endValue) === value) {
								bgColor = legends[i].color;
							}
						}
					}

					colSpan = config.colSpan ? 'colspan="' + config.colSpan + '" ' : '';
					rowSpan = config.rowSpan ? 'rowspan="' + config.rowSpan + '" ' : '';
                    htmlValue = getHtmlValue(config);
					htmlValue = config.type !== 'dimension' ? prettyPrint(htmlValue, layout.digitGroupSeparator) : htmlValue;

					cls += config.hidden ? ' td-hidden' : '';
					cls += config.collapsed ? ' td-collapsed' : '';
					cls += isValue ? ' pointer' : '';
					//cls += bgColor ? ' legend' : (config.cls ? ' ' + config.cls : '');
                    cls += config.cls ? ' ' + config.cls : '';

					// if sorting
					if (N.isString(metaDataId)) {
						cls += ' td-sortable';

						response.sortableIdObjects.push({
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

                getValue = function(str) {
                    var n = parseFloat(str);

                    if (N.isBoolean(str)) {
                        return 1;
                    }

                    // return string if
                    // - parsefloat(string) is not a number
                    // - string is just starting with a number
                    // - string is a valid date
                    //if (!N.isNumber(n) || n != str || new Date(str).toString() !== 'Invalid Date') {
                    if (!N.isNumber(n) || n != str) {
                        return 0;
                    }

                    return n;
                };

                roundIf = function(number, precision) {
                    number = parseFloat(number);
                    precision = parseFloat(precision);

                    if (N.isNumber(number) && N.isNumber(precision)) {
                        var numberOfDecimals = getNumberOfDecimals(number);
                        return numberOfDecimals > precision ? N.numberToFixed(number, precision) : number;
                    }

                    return number;
                };

                getNumberOfDecimals = function(number) {
                    var str = new String(number);
                    return (str.indexOf('.') > -1) ? (str.length - str.indexOf('.') - 1) : 0;
                };

                prettyPrint = function(number, separator) {
                    var oc = N.OptionConf,
                        spaceId = oc.getDigitGroupSeparator('space').id,
                        noneId = oc.getDigitGroupSeparator('none').id;

                    separator = separator || spaceId;

                    if (separator === noneId) {
                        return number;
                    }

                    return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, oc.getDigitGroupSeparatorValueById(separator) || '');
                };

                doColTotals = function() {
					return !!layout.showColTotals;
				};

				doRowTotals = function() {
					return !!layout.showRowTotals;
				};

				doColSubTotals = function() {
					return !!layout.showColSubTotals && rowAxis && rowAxis.dims > 1;
				};

				doRowSubTotals = function() {
					return !!layout.showRowSubTotals && colAxis && colAxis.dims > 1;
				};

				doSortableColumnHeaders = function() {
					return (rowAxis && rowAxis.dims === 1);
				};

				getColAxisHtmlArray = function() {
					var a = [],
                        columnDimensionNames = colAxis ? layout.columns.getDimensionNames(response) : [],
                        rowDimensionNames = rowAxis ? layout.rows.getDimensionNames(response) : [],
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
                        if (i < colAxis.dims - 1) {
                            if (rowAxis && rowAxis.dims) {
                                for (var j = 0; j < rowAxis.dims - 1; j++) {
                                    a.push(getEmptyNameTdConfig({
                                        cls: 'pivot-dim-label'
                                    }));
                                }
                            }

                            a.push(getEmptyNameTdConfig({
                                cls: 'pivot-dim-label',
                                htmlValue: response.getNameById(columnDimensionNames[i]) // objectNameMap[columnDimensionNames[i]].name
                            }));
                        }
                        else {
                            if (rowAxis && rowAxis.dims) {
                                for (var j = 0; j < rowAxis.dims - 1; j++) {
                                    a.push(getEmptyNameTdConfig({
                                        cls: 'pivot-dim-label',
                                        htmlValue: response.getNameById(rowDimensionNames[j]) //(objectNameMap[rowDimensionNames[j]] || {}).name
                                    }));
                                }
                            }

                            a.push(getEmptyNameTdConfig({
                                cls: 'pivot-dim-label',
                                htmlValue: response.getNameById(rowDimensionNames[j]) + (colAxis && rowAxis ? '&nbsp;/&nbsp;' : '') + response.getNameById(columnDimensionNames[i])
                            }));
                        }

                        return a;
                    };

					if (!colAxis) {

                        // show row dimension labels
                        if (rowAxis && layout.showDimensionLabels) {
                            var dimLabelHtml = [];

                            // labels from row object names
                            for (var i = 0; i < rowDimensionNames.length; i++) {
                                dimLabelHtml.push(getEmptyNameTdConfig({
                                    cls: 'pivot-dim-label',
                                    htmlValue: response.getNameById(rowDimensionNames[i])
                                }));
                            }

                            // pivot-transparent-column unnecessary

                            a.push(dimLabelHtml);
                        }

						return a;
					}

					// for each col dimension
					for (var i = 0, dimHtml; i < colAxis.dims; i++) {
						dimHtml = [];

                        if (layout.showDimensionLabels) {
                            dimHtml = dimHtml.concat(getEmptyHtmlArray(i));
                        }
                        else if (i === 0) {
							dimHtml.push(colAxis && rowAxis ? getEmptyNameTdConfig({
                                colSpan: rowAxis.dims,
                                rowSpan: colAxis.dims
                            }) : '');
						}

						for (var j = 0, obj, spanCount = 0, condoId, totalId; j < colAxis.size; j++) {
							spanCount++;
							condoId = null;
							totalId = null;

							obj = colAxis.objects.all[i][j];
							obj.type = 'dimension';
							obj.cls = 'pivot-dim';
							obj.noBreak = false;
							obj.hidden = !(obj.rowSpan || obj.colSpan);
							obj.htmlValue = response.getItemName(obj.id, layout.showHierarchy, true);

							// sortable column headers. last dim only.
							if (i === colAxis.dims - 1 && doSortableColumnHeaders()) {

								//condoId = colAxis.ids[j].split('-').join('');
								condoId = colAxis.ids[j];
							}

							dimHtml.push(getTdHtml(obj, condoId));

							if (i === 0 && spanCount === colAxis.span[i] && doRowSubTotals() ) {
								dimHtml.push(getTdHtml({
									type: 'dimensionSubtotal',
									cls: 'pivot-dim-subtotal cursor-default',
									rowSpan: colAxis.dims,
									htmlValue: '&nbsp;'
								}));

								spanCount = 0;
							}

							if (i === 0 && (j === colAxis.size - 1) && doRowTotals()) {
								totalId = doSortableColumnHeaders() ? 'total_' : null;

								dimHtml.push(getTdHtml({
									uuid: N.uuid(),
									type: 'dimensionTotal',
									cls: 'pivot-dim-total',
									rowSpan: colAxis.dims,
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
						colAxisSize = colAxis ? colAxis.size : 1,
						rowAxisSize = rowAxis ? rowAxis.size : 1,
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
					if (rowAxis) {
						for (var i = 0, row; i < rowAxis.size; i++) {
							row = [];

							for (var j = 0, obj, newObj; j < rowAxis.dims; j++) {
								obj = rowAxis.objects.all[j][i];
								obj.type = 'dimension';
								obj.cls = 'pivot-dim td-nobreak' + (layout.showHierarchy ? ' align-left' : '');
								obj.noBreak = true;
								obj.hidden = !(obj.rowSpan || obj.colSpan);
								obj.htmlValue = response.getItemName(obj.id, layout.showHierarchy, true);

								row.push(obj);
							}

							axisAllObjects.push(row);
						}
					}
                    else {
                        if (layout.showDimensionLabels) {
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
					for (var i = 0, valueItemsRow, valueObjectsRow; i < rowAxisSize; i++) {
						valueItemsRow = [];
						valueObjectsRow = [];

						for (var j = 0, id, value, responseValue, htmlValue, empty, uuid, uuids; j < colAxisSize; j++) {
							empty = false;
							uuids = [];

							// meta data uid
							id = [(colAxis ? colAxis.ids[j] : ''), (rowAxis ? rowAxis.ids[i] : '')].join('-');


                            // value html element id
							uuid = N.uuid();

							// get uuids array from colaxis/rowaxis leaf
							if (colAxis) {
								uuids = uuids.concat(colAxis.objects.all[colAxis.dims - 1][j].uuids);
							}
							if (rowAxis) {
								uuids = uuids.concat(rowAxis.objects.all[rowAxis.dims - 1][i].uuids);
							}

                            // value, htmlValue
                            responseValue = idValueMap[id];

							if (N.isDefined(responseValue)) {
                                value = getValue(responseValue);
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
					if (colAxis && doRowTotals()) {
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
								htmlValue: N.arrayContains(empty, false) ? getRoundedHtmlValue(total) : '',
								empty: !N.arrayContains(empty, false)
							});

							// add row totals to idValueMap to make sorting on totals possible
							if (doSortableColumnHeaders()) {
								var totalId = 'total_' + rowAxis.ids[i],
									isEmpty = !N.arrayContains(empty, false);

								idValueMap[totalId] = isEmpty ? null : total;
							}

							empty = [];
							total = 0;
						}
					}

					// hide empty rows (dims/values/totals)
					if (colAxis && rowAxis) {
						if (layout.hideEmptyRows) {
							for (var i = 0, valueRow, isValueRowEmpty, dimLeaf; i < valueObjects.length; i++) {
								valueRow = valueObjects[i];
								isValueRowEmpty = !N.arrayContains(N.arrayPluck(valueRow, 'empty'), false);

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
									dimLeaf = axisAllObjects[i][rowAxis.dims-1];
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
									var isEmpty = !N.arrayContains(empty, false);
									row.push({
										type: 'valueSubtotal',
										cls: 'pivot-value-subtotal' + (isEmpty ? ' cursor-default' : ''),
										value: rowSubTotal,
										htmlValue: isEmpty ? '&nbsp;' : getRoundedHtmlValue(rowSubTotal),
										empty: isEmpty,
										collapsed: !N.arrayContains(collapsed, false)
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

							for (var i = 0, obj; i < rowAxis.dims; i++) {
								obj = {};
								obj.type = 'dimensionSubtotal';
								obj.cls = 'pivot-dim-subtotal cursor-default';
								obj.collapsed = N.arrayContains(collapsed, true);

								if (i === 0) {
									obj.htmlValue = '&nbsp;';
									obj.colSpan = rowAxis.dims;
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
							if (!N.isArray(axisAllObjects[i+1]) || !!axisAllObjects[i+1][0].root) {
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

								if (!N.isArray(axisAllObjects[j+1]) || axisAllObjects[j+1][0].root) {
									var isEmpty = !N.arrayContains(empty, false);

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

							if (count === rowAxis.span[0]) {
								var isEmpty = !N.arrayContains(empty, false);

								tmpTotalValueObjects.push({
									type: 'valueTotalSubgrandtotal',
									cls: 'pivot-value-total-subgrandtotal' + (isEmpty ? ' cursor-default' : ''),
									value: subTotal,
									htmlValue: isEmpty ? '&nbsp;' : getRoundedHtmlValue(subTotal),
									empty: isEmpty,
									collapsed: !N.arrayContains(collapsed, false)
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

						//if (rowAxis) {
							row = row.concat(axisAllObjects[i]);
						//}

						row = row.concat(xValueObjects[i]);

						if (colAxis) {
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

					if (rowAxis && doColTotals()) {
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
								htmlValue: N.arrayContains(empty, false) ? getRoundedHtmlValue(total) : '',
								empty: !N.arrayContains(empty, false),
								cls: 'pivot-value-total'
							});

							total = 0;
							empty = [];
						}

						xTotalColObjects = totalColObjects;

						if (colAxis && doRowSubTotals()) {
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
										htmlValue: N.arrayContains(empty, false) ? getRoundedHtmlValue(subTotal) : '',
										empty: !N.arrayContains(empty, false),
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

						if (colAxis && rowAxis) {
							a.push(getTdHtml({
								type: 'valueGrandTotal',
								cls: 'pivot-value-grandtotal',
								value: total,
								htmlValue: N.arrayContains(empty, false) ? getRoundedHtmlValue(total) : '',
								empty: !N.arrayContains(empty, false)
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
						if (rowAxis) {
							dimTotalArray = [getTdHtml({
								type: 'dimensionSubtotal',
								cls: 'pivot-dim-total',
								colSpan: rowAxis.dims,
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

                    cls += layout.displayDensity && layout.displayDensity !== N.OptionConf.getDisplayDensity('normal').id ? ' displaydensity-' + layout.displayDensity : '';
                    cls += layout.fontSize && layout.fontSize !== N.OptionConf.getFontSize('normal').id ? ' fontsize-' + layout.fontSize : '';

					table = '<table class="' + cls + '">';

					for (var i = 0; i < htmlArray.length; i++) {
						table += '<tr>' + htmlArray[i].join('') + '</tr>';
					}

					return table += '</table>';
				};

				// get html
                htmlArray = N.arrayClean([].concat(getColAxisHtmlArray() || [], getRowHtmlArray() || [], getTotalHtmlArray() || []));

                // constructor
                t.html = getHtml(htmlArray);
                t.uuidDimUuidsMap = uuidDimUuidsMap;
                t.colAxis = colAxis;
                t.rowAxis = rowAxis;
                t.tdCount = tdCount;
			};
        })();
    })();
});
