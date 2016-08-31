Ext.onReady( function() {
	var NS = DV,

        LayoutWindow,
        OptionsWindow,
        FavoriteWindow,
        SharingWindow,
        InterpretationWindow,
        AboutWindow,

        extendCore,
        createViewport,
        dimConf,
        chartConf,

        ns = {
            core: {},
            app: {}
        };

	// set app config
	(function() {

		// ext configuration
		Ext.QuickTips.init();

		Ext.override(Ext.LoadMask, {
			onHide: function() {
				this.callParent();
			}
		});

        Ext.override(Ext.grid.Scroller, {
            afterRender: function() {
                var me = this;
                me.callParent();
                me.mon(me.scrollEl, 'scroll', me.onElScroll, me);
                Ext.cache[me.el.id].skipGarbageCollection = true;
                // add another scroll event listener to check, if main listeners is active
                Ext.EventManager.addListener(me.scrollEl, 'scroll', me.onElScrollCheck, me);
                // ensure this listener doesn't get removed
                Ext.cache[me.scrollEl.id].skipGarbageCollection = true;
            },

            // flag to check, if main listeners is active
            wasScrolled: false,

            // synchronize the scroller with the bound gridviews
            onElScroll: function(event, target) {
                this.wasScrolled = true; // change flag -> show that listener is alive
                this.fireEvent('bodyscroll', event, target);
            },

            // executes just after main scroll event listener and check flag state
            onElScrollCheck: function(event, target, options) {
                var me = this;

                if (!me.wasScrolled) {
                    // Achtung! Event listener was disappeared, so we'll add it again
                    me.mon(me.scrollEl, 'scroll', me.onElScroll, me);
                }
                me.wasScrolled = false; // change flag to initial value
            }

        });

        Ext.override(Ext.data.TreeStore, {
            load: function(options) {
                options = options || {};
                options.params = options.params || {};

                var me = this,
                    node = options.node || me.tree.getRootNode(),
                    root;

                // If there is not a node it means the user hasnt defined a rootnode yet. In this case lets just
                // create one for them.
                if (!node) {
                    node = me.setRootNode({
                        expanded: true
                    });
                }

                if (me.clearOnLoad) {
                    node.removeAll(true);
                }

                options.records = [node];

                Ext.applyIf(options, {
                    node: node
                });
                //options.params[me.nodeParam] = node ? node.getId() : 'root';

                if (node) {
                    node.set('loading', true);
                }

                return me.callParent([options]);
            }
        });

        Ext.override(Ext.chart.axis.Axis, {
            drawHorizontalLabels: function() {
                var me = this,
                    labelConf = me.label,
                    floor = Math.floor,
                    max = Math.max,
                    axes = me.chart.axes,
                    position = me.position,
                    inflections = me.inflections,
                    ln = inflections.length,
                    labels = me.labels,
                    labelGroup = me.labelGroup,
                    maxHeight = 0,
                    ratio,
                    gutterY = me.chart.maxGutter[1],
                    ubbox, bbox, point, prevX, prevLabel,
                    projectedWidth = 0,
                    textLabel, attr, textRight, text,
                    label, last, x, y, i, firstLabel;

                last = ln - 1;
                // get a reference to the first text label dimensions
                point = inflections[0];
                firstLabel = me.getOrCreateLabel(0, me.label.renderer(labels[0]));
                ratio = Math.floor(Math.abs(Math.sin(labelConf.rotate && (labelConf.rotate.degrees * Math.PI / 180) || 0)));

                for (i = 0; i < ln; i++) {
                    point = inflections[i];
                    text = me.label.renderer(labels[i]) || '';
                    textLabel = me.getOrCreateLabel(i, text);
                    bbox = textLabel._bbox;
                    maxHeight = max(maxHeight, bbox.height + me.dashSize + me.label.padding);
                    x = floor(point[0] - (ratio? bbox.height : bbox.width) / 2);
                    if (me.chart.maxGutter[0] == 0) {
                        if (i == 0 && axes.findIndex('position', 'left') == -1) {
                            x = point[0];
                        }
                        else if (i == last && axes.findIndex('position', 'right') == -1) {
                            x = point[0] - bbox.width;
                        }
                    }
                    if (position == 'top') {
                        y = point[1] - (me.dashSize * 2) - me.label.padding - (bbox.height / 2);
                    }
                    else {
                        y = point[1] + (me.dashSize * 2) + me.label.padding + (bbox.height / 2);
                    }

                    var moveLabels = labelConf.rotate && labelConf.rotate.degrees && !Ext.Array.contains([0,90,180,270,360], labelConf.rotate.degrees),
                        adjust = Math.floor((textLabel.text.length - 12) * -1 * 0.75),
                        newX = moveLabels ? point[0] - textLabel._bbox.width + adjust: x;

                    textLabel.setAttributes({
                        hidden: false,
                        x: newX,
                        y: y
                    }, true);

                    // skip label if there isn't available minimum space
                    if (i != 0 && (me.intersect(textLabel, prevLabel)
                        || me.intersect(textLabel, firstLabel))) {
                        textLabel.hide(true);
                        continue;
                    }

                    prevLabel = textLabel;
                }

                return maxHeight;
            }
        });

		// right click handler
		document.body.oncontextmenu = function() {
			return false;
		};
	}());

	// constructors
	LayoutWindow = function() {
		var dimension,
			dimensionStore,
			row,
			rowStore,
			col,
			colStore,
			filter,
			filterStore,
			value,

			getStore,
			getStoreKeys,
            addDimension,
            removeDimension,
            hasDimension,
            saveState,
            resetData,
            reset,
            dimensionStoreMap = {},

			dimensionPanel,
			selectPanel,
			window,

			margin = 1,
			defaultWidth = 200,
			defaultHeight = 220;

		getStore = function(data) {
			var config = {};

			config.fields = ['id', 'name'];

			if (data) {
				config.data = data;
			}

			config.getDimensionNames = function() {
				var dimensionNames = [];

				this.each(function(r) {
					dimensionNames.push(r.data.id);
				});

				return Ext.clone(dimensionNames);
			};

            config.hasDimension = function(id) {
                return Ext.isString(id) && this.findExact('id', id) != -1 ? true : false;
            };

            config.removeDimension = function(id) {
                var index = this.findExact('id', id);

                if (index != -1) {
                    this.remove(this.getAt(index));
                }
            };

			return Ext.create('Ext.data.Store', config);
		};

		getStoreKeys = function(store) {
			var keys = [],
				items = store.data.items;

			if (items) {
				for (var i = 0; i < items.length; i++) {
					keys.push(items[i].data.id);
				}
			}

			return keys;
		};

		dimensionStore = getStore();
        ns.app.stores.dimension = dimensionStore;

		colStore = getStore();
        ns.app.stores.col = colStore;

		rowStore = getStore();
        ns.app.stores.row = rowStore;

        filterStore = getStore();
        ns.app.stores.filter = filterStore;

		dimension = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-leftright',
			width: defaultWidth - 50,
			height: (defaultHeight * 2) + margin,
			style: 'margin-right:' + margin + 'px; margin-bottom:0px',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'layoutDD',
			dropGroup: 'layoutDD',
			ddReorder: false,
			store: dimensionStore,
			tbar: {
				height: 25,
				items: {
					xtype: 'label',
					text: NS.i18n.excluded_dimensions,
					cls: 'ns-toolbar-multiselect-leftright-label'
				}
			},
			listeners: {
				afterrender: function(ms) {
					ms.store.on('add', function() {
						Ext.defer( function() {
							ms.boundList.getSelectionModel().deselectAll();
						}, 10);
					});
				}
			}
		});

		col = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-leftright',
			width: defaultWidth,
			height: defaultHeight,
			style: 'margin-bottom:' + margin + 'px',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'layoutDD',
			dropGroup: 'layoutDD',
			store: colStore,
			tbar: {
				height: 25,
				items: {
					xtype: 'label',
					text: NS.i18n.series_dimensions,
					cls: 'ns-toolbar-multiselect-leftright-label'
				}
			},
			listeners: {
				afterrender: function(ms) {
					ms.boundList.on('itemdblclick', function(view, record) {
						ms.store.remove(record);
						dimensionStore.add(record);
					});

					ms.store.on('add', function(store, addedRecords) {
                        var range = store.getRange();

                        if (range.length > 1) {
                            var addedIds = Ext.Array.pluck(addedRecords, 'internalId'),
                                records = Ext.clone(range);

                            store.removeAll();

                            for (var i = 0; i < range.length; i++) {
                                if (Ext.Array.contains(addedIds, range[i].internalId)) {
                                    store.add(range[i]);
                                }
                                else {
                                    filterStore.add(range[i]);
                                }
                            }
                        }

						Ext.defer( function() {
							ms.boundList.getSelectionModel().deselectAll();
						}, 10);
					});
				}
			}
		});

		row = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-leftright',
			width: defaultWidth,
			height: defaultHeight,
			style: 'margin-bottom:0px',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'layoutDD',
			dropGroup: 'layoutDD',
			store: rowStore,
			tbar: {
				height: 25,
				items: {
					xtype: 'label',
					text: NS.i18n.category_dimensions,
					cls: 'ns-toolbar-multiselect-leftright-label'
				}
			},
			listeners: {
				afterrender: function(ms) {
					ms.boundList.on('itemdblclick', function(view, record) {
						ms.store.remove(record);
						dimensionStore.add(record);
					});

					ms.store.on('add', function(store, addedRecords) {
                        var range = store.getRange();

                        if (range.length > 1) {
                            var addedIds = Ext.Array.pluck(addedRecords, 'internalId'),
                                records = Ext.clone(range);

                            store.removeAll();

                            for (var i = 0; i < range.length; i++) {
                                if (Ext.Array.contains(addedIds, range[i].internalId)) {
                                    store.add(range[i]);
                                }
                                else {
                                    filterStore.add(range[i]);
                                }
                            }
                        }

						Ext.defer( function() {
							ms.boundList.getSelectionModel().deselectAll();
						}, 10);
					});
				}
			}
		});

		filter = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-leftright',
			width: defaultWidth,
			height: defaultHeight,
			style: 'margin-right:' + margin + 'px; margin-bottom:' + margin + 'px',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'layoutDD',
			dropGroup: 'layoutDD',
			store: filterStore,
			tbar: {
				height: 25,
				items: {
					xtype: 'label',
					text: NS.i18n.report_filter,
					cls: 'ns-toolbar-multiselect-leftright-label'
				}
			},
			listeners: {
				afterrender: function(ms) {
					ms.boundList.on('itemdblclick', function(view, record) {
						ms.store.remove(record);
						dimensionStore.add(record);
					});

					ms.store.on('add', function() {
						Ext.defer( function() {
							ms.boundList.getSelectionModel().deselectAll();
						}, 10);
					});
				}
			}
		});

		selectPanel = Ext.create('Ext.panel.Panel', {
			bodyStyle: 'border:0 none',
			items: [
				{
					layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
						filter,
						col
					]
				},
				{
					layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
						row
					]
				}
			]
		});

        addDimension = function(record, store) {
            var store = dimensionStoreMap[record.id] || store || colStore;

            if (!hasDimension(record.id)) {
                store.add(record);
            }
        };

        removeDimension = function(dataElementId) {
            var stores = [colStore, rowStore, filterStore, dimensionStore];

            for (var i = 0, store, index; i < stores.length; i++) {
                store = stores[i];

                if (store.hasDimension(dataElementId)) {
                    store.removeDimension(dataElementId);
                    dimensionStoreMap[dataElementId] = store;
                }
            }
        };

        hasDimension = function(id) {
            var stores = [colStore, rowStore, filterStore, dimensionStore];

            for (var i = 0, store, index; i < stores.length; i++) {
                if (stores[i].hasDimension(id)) {
                    return true;
                }
            }

            return false;
        };

        saveState = function(map) {
			map = map || dimensionStoreMap;

            colStore.each(function(record) {
                map[record.data.id] = colStore;
            });

            rowStore.each(function(record) {
                map[record.data.id] = rowStore;
            });

            filterStore.each(function(record) {
                map[record.data.id] = filterStore;
            });

            return map;
        };

		resetData = function() {
			var map = saveState({}),
				keys = ['dx', 'ou', 'pe', 'dates'];

			for (var key in map) {
				if (map.hasOwnProperty(key) && !Ext.Array.contains(keys, key)) {
					removeDimension(key);
				}
			}
		};

		reset = function(isAll) {
			colStore.removeAll();
			rowStore.removeAll();
			filterStore.removeAll();
            dimensionStore.removeAll();

			if (!isAll) {
				colStore.add({id: dimConf.data.dimensionName, name: dimConf.data.name});
				rowStore.add({id: dimConf.period.dimensionName, name: dimConf.period.name});
				filterStore.add({id: dimConf.organisationUnit.dimensionName, name: dimConf.organisationUnit.name});
				dimensionStore.add({id: dimConf.category.dimensionName, name: dimConf.category.name});
			}
		};

		getSetup = function() {
			return {
				col: getStoreKeys(colStore),
				row: getStoreKeys(rowStore),
				filter: getStoreKeys(filterStore)
			};
		};

		window = Ext.create('Ext.window.Window', {
			title: NS.i18n.chart_layout,
			bodyStyle: 'background-color:#fff; padding:' + margin + 'px',
			closeAction: 'hide',
			autoShow: true,
			modal: true,
			resizable: false,
			getSetup: getSetup,
			dimensionStore: dimensionStore,
			rowStore: rowStore,
			colStore: colStore,
			filterStore: filterStore,
            addDimension: addDimension,
            removeDimension: removeDimension,
            hasDimension: hasDimension,
			hideOnBlur: true,
			items: {
				layout: 'column',
				bodyStyle: 'border:0 none',
				items: [
					dimension,
					selectPanel
				]
			},
			bbar: [
				'->',
				{
					text: NS.i18n.hide,
					listeners: {
						added: function(b) {
							b.on('click', function() {
								window.hide();
							});
						}
					}
				},
				{
					text: '<b>' + NS.i18n.update + '</b>',
					listeners: {
						added: function(b) {
							b.on('click', function() {
                                ns.app.viewport.update();

								window.hide();
							});
						}
					}
				}
			],
			listeners: {
				show: function(w) {
					if (ns.app.layoutButton.rendered) {
						ns.core.web.window.setAnchorPosition(w, ns.app.layoutButton);

						if (!w.hasHideOnBlurHandler) {
							ns.core.web.window.addHideOnBlurHandler(w);
						}
					}
				},
                render: function() {
					reset();
                }
			}
		});

		return window;
	};

	OptionsWindow = function() {
		var showValues,
            hideEmptyRows,
            showTrendLine,
			targetLineValue,
			targetLineTitle,
			baseLineValue,
			baseLineTitle,
            sortOrder,

            rangeAxisMinValue,
            rangeAxisMaxValue,
            rangeAxisSteps,
            rangeAxisDecimals,
			rangeAxisTitle,
			domainAxisTitle,

			hideLegend,
			hideTitle,
			title,

            completedOnly,

			data,
			axes,
			general,
            events,
			window,

            comboBottomMargin = 1,
            checkboxBottomMargin = 2,
            separatorTopMargin = 6,
			cmpWidth = 340,
			labelWidth = 125,
			numberWidth = 80;

        // data
		showValues = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.show_values,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
			checked: true
		});

		hideEmptyRows = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.hide_empty_category_items,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px'
		});

		showTrendLine = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.trend_line,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px'
		});

		targetLineValue = Ext.create('Ext.form.field.Number', {
			width: numberWidth,
			height: 18,
			listeners: {
				change: function(nf) {
					targetLineTitle.xable();
				}
			}
		});

		targetLineTitle = Ext.create('Ext.form.field.Text', {
			style: 'margin-left:1px; margin-bottom:1px',
			fieldStyle: 'padding-left:3px',
			emptyText: NS.i18n.target,
			width: cmpWidth - labelWidth - 5 - numberWidth - 1,
			maxLength: 100,
			enforceMaxLength: true,
			disabled: true,
			xable: function() {
				this.setDisabled(!targetLineValue.getValue() && !Ext.isNumber(targetLineValue.getValue()));
			}
		});

		baseLineValue = Ext.create('Ext.form.field.Number', {
			//cls: 'gis-numberfield',
			width: numberWidth,
			height: 18,
			listeners: {
				change: function(nf) {
					baseLineTitle.xable();
				}
			}
		});

		baseLineTitle = Ext.create('Ext.form.field.Text', {
			style: 'margin-left:1px; margin-bottom:1px',
			fieldStyle: 'padding-left:3px',
			emptyText: NS.i18n.base,
			width: cmpWidth - labelWidth - 5 - numberWidth - 1,
			maxLength: 100,
			enforceMaxLength: true,
			disabled: true,
			xable: function() {
				this.setDisabled(!baseLineValue.getValue() && !Ext.isNumber(baseLineValue.getValue()));
			}
		});

		sortOrder = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:' + comboBottomMargin + 'px',
			width: cmpWidth,
			labelWidth: 125,
			fieldLabel: NS.i18n.sort_order,
			labelStyle: 'color:#333',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
			value: 0,
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: 0, text: NS.i18n.none},
					{id: -1, text: NS.i18n.low_to_high},
					{id: 1, text: NS.i18n.high_to_low}
				]
			})
		});

		aggregationType = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:' + comboBottomMargin + 'px',
			width: cmpWidth,
			labelWidth: 125,
			fieldLabel: NS.i18n.aggregation_type,
			labelStyle: 'color:#333',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
			value: 'DEFAULT',
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: 'DEFAULT', text: NS.i18n.by_data_element},
					{id: 'COUNT', text: NS.i18n.count},
					{id: 'SUM', text: NS.i18n.sum},
					{id: 'STDDEV', text: NS.i18n.stddev},
					{id: 'VARIANCE', text: NS.i18n.variance},
					{id: 'MIN', text: NS.i18n.min},
					{id: 'MAX', text: NS.i18n.max}
				]
			})
		});

        // axes
		rangeAxisMinValue = Ext.create('Ext.form.field.Number', {
			width: numberWidth,
			height: 18,
			labelWidth: 125
		});

		rangeAxisMaxValue = Ext.create('Ext.form.field.Number', {
			width: numberWidth,
			height: 18,
			labelWidth: 125,
            style: 'margin-left:1px'
		});

		rangeAxisSteps = Ext.create('Ext.form.field.Number', {
			width: labelWidth + 5 + numberWidth,
			height: 18,
			fieldLabel: 'Range axis tick steps',
			labelStyle: 'color:#333',
			labelWidth: 125,
			minValue: 1
		});

		rangeAxisDecimals = Ext.create('Ext.form.field.Number', {
			width: labelWidth + 5 + numberWidth,
			height: 18,
			fieldLabel: 'Range axis decimals',
			labelStyle: 'color:#333',
			labelWidth: 125,
			minValue: 0,
            maxValue: 20
		});

		rangeAxisTitle = Ext.create('Ext.form.field.Text', {
			width: cmpWidth,
			fieldLabel: NS.i18n.range_axis_label,
			labelStyle: 'color:#333',
			labelWidth: 125,
			maxLength: 100,
			enforceMaxLength: true,
			style: 'margin-bottom:1px'
		});

		domainAxisTitle = Ext.create('Ext.form.field.Text', {
			width: cmpWidth,
			fieldLabel: NS.i18n.domain_axis_label,
			labelStyle: 'color:#333',
			labelWidth: 125,
			maxLength: 100,
			enforceMaxLength: true,
			style: 'margin-bottom:1px'
		});

        // general
		hideLegend = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.hide_legend,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px'
		});

		hideTitle = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.hide_chart_title,
			style: 'margin-bottom:7px',
			listeners: {
				change: function() {
					title.xable();
				}
			}
		});

		title = Ext.create('Ext.form.field.Text', {
			style: 'margin-bottom:0',
			width: cmpWidth,
			fieldLabel: NS.i18n.chart_title,
			labelStyle: 'color:#333',
			labelWidth: 125,
			maxLength: 100,
			enforceMaxLength: true,
			xable: function() {
				this.setDisabled(hideTitle.getValue());
			}
		});

        // events
		completedOnly = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.include_only_completed_events_only,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
		});

        data = {
			xtype: 'container',
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
				showValues,
				hideEmptyRows,
				showTrendLine,
				{
					xtype: 'container',
					layout: 'column',
					bodyStyle: 'border:0 none',
                    style: 'margin-top:' + (separatorTopMargin + 1) + 'px',
					items: [
						{
							bodyStyle: 'border:0 none; padding-top:3px; margin-right:5px; color:#333',
							width: 130,
							html: 'Target value / title:'
						},
						targetLineValue,
						targetLineTitle
					]
				},
				{
					xtype: 'container',
					layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
						{
							bodyStyle: 'border:0 none; padding-top:3px; margin-right:5px; color:#333',
							width: 130,
							html: 'Base value / title:'
						},
						baseLineValue,
						baseLineTitle
					]
				},
                sortOrder,
                aggregationType
			]
		};

		axes = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
				{
					layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
						{
							bodyStyle: 'border:0 none; padding-top:3px; margin-right:5px; color:#333',
							width: 130,
							html: 'Range axis min/max:'
						},
						rangeAxisMinValue,
						rangeAxisMaxValue
					]
				},
				rangeAxisSteps,
				rangeAxisDecimals,
				rangeAxisTitle,
				domainAxisTitle
			]
		};

		general = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
				hideLegend,
				hideTitle,
				title
			]
		};

		events = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
				completedOnly
			]
		};

		window = Ext.create('Ext.window.Window', {
			title: NS.i18n.chart_options,
			bodyStyle: 'background-color:#fff; padding:3px',
			closeAction: 'hide',
			autoShow: true,
			modal: true,
			resizable: false,
			hideOnBlur: true,
			getOptions: function() {
				return {
					showValues: showValues.getValue(),
                    hideEmptyRows: hideEmptyRows.getValue(),
					showTrendLine: showTrendLine.getValue(),
					completedOnly: completedOnly.getValue(),
					targetLineValue: targetLineValue.getValue(),
					targetLineTitle: targetLineTitle.getValue(),
					baseLineValue: baseLineValue.getValue(),
					baseLineTitle: baseLineTitle.getValue(),
                    sortOrder: sortOrder.getValue(),
                    aggregationType: aggregationType.getValue(),
					rangeAxisMaxValue: rangeAxisMaxValue.getValue(),
					rangeAxisMinValue: rangeAxisMinValue.getValue(),
					rangeAxisSteps: rangeAxisSteps.getValue(),
					rangeAxisDecimals: rangeAxisDecimals.getValue(),
					rangeAxisTitle: rangeAxisTitle.getValue(),
					domainAxisTitle: domainAxisTitle.getValue(),
					hideLegend: hideLegend.getValue(),
					hideTitle: hideTitle.getValue(),
					title: title.getValue()
				};
			},
			setOptions: function(layout) {
				showValues.setValue(Ext.isBoolean(layout.showValues) ? layout.showValues : false);
				hideEmptyRows.setValue(Ext.isBoolean(layout.hideEmptyRows) ? layout.hideEmptyRows : false);
				showTrendLine.setValue(Ext.isBoolean(layout.showTrendLine) ? layout.showTrendLine : false);

                completedOnly.setValue(Ext.isBoolean(layout.completedOnly) ? layout.completedOnly : false);

				// target line
				if (Ext.isNumber(layout.targetLineValue)) {
					targetLineValue.setValue(layout.targetLineValue);
				}
				else {
					targetLineValue.reset();
				}

				if (Ext.isString(layout.targetLineTitle)) {
					targetLineTitle.setValue(layout.targetLineTitle);
				}
				else {
					targetLineTitle.reset();
				}

				// base line
				if (Ext.isNumber(layout.baseLineValue)) {
					baseLineValue.setValue(layout.baseLineValue);
				}
				else {
					baseLineValue.reset();
				}

				if (Ext.isString(layout.baseLineTitle)) {
					baseLineTitle.setValue(layout.baseLineTitle);
				}
				else {
					baseLineTitle.reset();
				}

                sortOrder.setValue(Ext.isNumber(layout.sortOrder) ? layout.sortOrder : 0);
                aggregationType.setValue(Ext.isString(layout.aggregationType) ? layout.aggregationType : 'default');

				// rangeAxisMaxValue
				if (Ext.isNumber(layout.rangeAxisMaxValue)) {
					rangeAxisMaxValue.setValue(layout.rangeAxisMaxValue);
				}
				else {
					rangeAxisMaxValue.reset();
				}

				// rangeAxisMinValue
				if (Ext.isNumber(layout.rangeAxisMinValue)) {
					rangeAxisMinValue.setValue(layout.rangeAxisMinValue);
				}
				else {
					rangeAxisMinValue.reset();
				}

				// rangeAxisSteps
				if (Ext.isNumber(layout.rangeAxisSteps)) {
					rangeAxisSteps.setValue(layout.rangeAxisSteps);
				}
				else {
					rangeAxisSteps.reset();
				}

				// rangeAxisDecimals
				if (Ext.isNumber(layout.rangeAxisDecimals)) {
					rangeAxisDecimals.setValue(layout.rangeAxisDecimals);
				}
				else {
					rangeAxisDecimals.reset();
				}

				// range axis title
				if (Ext.isString(layout.rangeAxisTitle)) {
					rangeAxisTitle.setValue(layout.rangeAxisTitle);
				}
				else {
					rangeAxisTitle.reset();
				}

				// domain axis title
				if (Ext.isString(layout.domainAxisTitle)) {
					domainAxisTitle.setValue(layout.domainAxisTitle);
				}
				else {
					domainAxisTitle.reset();
				}

				hideLegend.setValue(Ext.isBoolean(layout.hideLegend) ? layout.hideLegend : false);
				hideTitle.setValue(Ext.isBoolean(layout.hideTitle) ? layout.hideTitle : false);

				// title
				if (Ext.isString(layout.title)) {
					title.setValue(layout.title);
				}
				else {
					title.reset();
				}
			},
			items: [
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-bottom:6px; margin-left:2px',
					html: NS.i18n.data
				},
				data,
				{
					bodyStyle: 'border:0 none; padding:5px'
				},
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-bottom:6px; margin-left:2px',
					html: NS.i18n.events
				},
				events,
				{
					bodyStyle: 'border:0 none; padding:5px'
				},
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-bottom:6px; margin-left:2px',
					html: NS.i18n.axes
				},
				axes,
				{
					bodyStyle: 'border:0 none; padding:5px'
				},
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-bottom:6px; margin-left:2px',
					html: NS.i18n.general
				},
				general
			],
			bbar: [
				'->',
				{
					text: NS.i18n.hide,
					handler: function() {
						window.hide();
					}
				},
				{
					text: '<b>' + NS.i18n.update + '</b>',
					handler: function() {
                        ns.app.viewport.update();

						window.hide();
					}
				}
			],
			listeners: {
				show: function(w) {
					if (ns.app.optionsButton.rendered) {
						ns.core.web.window.setAnchorPosition(w, ns.app.optionsButton);

						if (!w.hasHideOnBlurHandler) {
							ns.core.web.window.addHideOnBlurHandler(w);
						}
					}

					// cmp
					w.showValues = showValues;
                    w.hideEmptyRows = hideEmptyRows;
					w.showTrendLine = showTrendLine;
                    w.completedOnly = completedOnly;
					w.targetLineValue = targetLineValue;
					w.targetLineTitle = targetLineTitle;
					w.baseLineValue = baseLineValue;
					w.baseLineTitle = baseLineTitle;
                    w.sortOrder = sortOrder;
                    w.aggregationType = aggregationType;
					w.rangeAxisMaxValue = rangeAxisMaxValue;
					w.rangeAxisMinValue = rangeAxisMinValue;
					w.rangeAxisSteps = rangeAxisSteps;
					w.rangeAxisDecimals = rangeAxisDecimals;
					w.rangeAxisTitle = rangeAxisTitle;
					w.domainAxisTitle = domainAxisTitle;
					w.hideLegend = hideLegend;
					w.hideTitle = hideTitle;
					w.title = title;
				}
			}
		});

		return window;
	};

	FavoriteWindow = function() {

		// Objects
		var NameWindow,

		// Instances
			nameWindow,

		// Functions
			getBody,

		// Components
			addButton,
			searchTextfield,
			grid,
			prevButton,
			nextButton,
			tbar,
			bbar,
			info,
			nameTextfield,
			createButton,
			updateButton,
			cancelButton,
			favoriteWindow,

		// Vars
			windowWidth = 500,
			windowCmpWidth = windowWidth - 14;

		ns.app.stores.chart.on('load', function(store, records) {
			var pager = store.proxy.reader.jsonData.pager;

			info.setText('Page ' + pager.page + ' of ' + pager.pageCount);

			prevButton.enable();
			nextButton.enable();

			if (pager.page === 1) {
				prevButton.disable();
			}

			if (pager.page === pager.pageCount) {
				nextButton.disable();
			}
		});

		getBody = function() {
			var favorite,
				dimensions;

			if (ns.app.layout) {
				favorite = Ext.clone(ns.app.layout);
				dimensions = [].concat(favorite.columns || [], favorite.rows || [], favorite.filters || []);

				// server sync
                favorite.type = chartConf.c2s[favorite.type];

				favorite.showData = favorite.showValues;
				delete favorite.showValues;

				favorite.regression = favorite.showTrendLine;
				delete favorite.showTrendLine;

				favorite.targetLineLabel = favorite.targetLineTitle;
				delete favorite.targetLineTitle;

				favorite.baseLineLabel = favorite.baseLineTitle;
				delete favorite.baseLineTitle;

				favorite.domainAxisLabel = favorite.domainAxisTitle;
				delete favorite.domainAxisTitle;

				favorite.rangeAxisLabel = favorite.rangeAxisTitle;
				delete favorite.rangeAxisTitle;

				delete favorite.id;
				delete favorite.parentGraphMap;

                delete favorite.program;
			}

			return favorite;
		};

		NameWindow = function(id) {
			var window,
				record = ns.app.stores.chart.getById(id);

			nameTextfield = Ext.create('Ext.form.field.Text', {
				height: 26,
				width: 371,
				fieldStyle: 'padding-left: 4px; border-radius: 1px; border-color: #bbb; font-size:11px',
				style: 'margin-bottom:0',
				emptyText: 'Favorite name',
				value: id ? record.data.name : '',
				listeners: {
					afterrender: function() {
						this.focus();
					}
				}
			});

			createButton = Ext.create('Ext.button.Button', {
				text: NS.i18n.create,
				handler: function() {
					var favorite = getBody();
					favorite.name = nameTextfield.getValue();

					//tmp
					//delete favorite.legendSet;

					if (favorite && favorite.name) {
						Ext.Ajax.request({
							url: ns.core.init.contextPath + '/api/charts/',
							method: 'POST',
							headers: {'Content-Type': 'application/json'},
							params: Ext.encode(favorite),
							failure: function(r) {
								ns.core.web.mask.show();
                                ns.alert(r);
							},
							success: function(r) {
								var id = r.getAllResponseHeaders().location.split('/').pop();

								ns.app.layout.id = id;
								ns.app.xLayout.id = id;

								ns.app.layout.name = name;
								ns.app.xLayout.name = name;

								ns.app.stores.chart.loadStore();

								window.destroy();
							}
						});
					}
				}
			});

			updateButton = Ext.create('Ext.button.Button', {
				text: NS.i18n.update,
				handler: function() {
					var name = nameTextfield.getValue(),
						chart;

					if (id && name) {
						Ext.Ajax.request({
							url: ns.core.init.contextPath + '/api/charts/' + id + '.json?fields=' + ns.core.conf.url.analysisFields.join(','),
							method: 'GET',
							failure: function(r) {
								ns.core.web.mask.show();
                                ns.alert(r);
							},
							success: function(r) {
								chart = Ext.decode(r.responseText);
								chart.name = name;

								Ext.Ajax.request({
									url: ns.core.init.contextPath + '/api/charts/' + chart.id + '?mergeStrategy=REPLACE',
									method: 'PUT',
									headers: {'Content-Type': 'application/json'},
									params: Ext.encode(chart),
									failure: function(r) {
										ns.core.web.mask.show();
                                        ns.alert(r);
									},
									success: function(r) {
										if (ns.app.layout && ns.app.layout.id && ns.app.layout.id === id) {
											ns.app.layout.name = name;
											ns.app.xLayout.name = name;
										}

										ns.app.stores.chart.loadStore();
										window.destroy();
									}
								});
							}
						});
					}
				}
			});

			cancelButton = Ext.create('Ext.button.Button', {
				text: NS.i18n.cancel,
				handler: function() {
					window.destroy();
				}
			});

			window = Ext.create('Ext.window.Window', {
				title: id ? 'Rename favorite' : 'Create new favorite',
				bodyStyle: 'padding:1px; background:#fff',
				resizable: false,
				modal: true,
				items: nameTextfield,
				destroyOnBlur: true,
				bbar: [
					cancelButton,
					'->',
					id ? updateButton : createButton
				],
				listeners: {
					show: function(w) {
						ns.core.web.window.setAnchorPosition(w, addButton);

						if (!w.hasDestroyBlurHandler) {
							ns.core.web.window.addDestroyOnBlurHandler(w);
						}

						ns.app.favoriteWindow.destroyOnBlur = false;

						nameTextfield.focus(false, 500);
					},
					destroy: function() {
						ns.app.favoriteWindow.destroyOnBlur = true;
					}
				}
			});

			return window;
		};

		addButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.add_new,
			width: 67,
			height: 26,
			style: 'border-radius: 1px;',
			menu: {},
			disabled: !Ext.isObject(ns.app.xLayout),
			handler: function() {
				nameWindow = new NameWindow(null, 'create');
				nameWindow.show();
			}
		});

		searchTextfield = Ext.create('Ext.form.field.Text', {
			width: windowCmpWidth - addButton.width - 3,
			height: 26,
			fieldStyle: 'padding-right: 0; padding-left: 4px; border-radius: 1px; border-color: #bbb; font-size:11px',
			emptyText: NS.i18n.search_for_favorites,
			enableKeyEvents: true,
			currentValue: '',
			listeners: {
				keyup: {
					fn: function() {
						if (this.getValue() !== this.currentValue) {
							this.currentValue = this.getValue();

							var value = this.getValue(),
								url = value ? ns.core.init.contextPath + '/api/charts.json?fields=id,name,access' + (value ? '&filter=name:ilike:' + value : '') : null;
								store = ns.app.stores.chart;

							store.page = 1;
							store.loadStore(url);
						}
					},
					buffer: 100
				}
			}
		});

		prevButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.prev,
			handler: function() {
				var value = searchTextfield.getValue(),
					url = value ? ns.core.init.contextPath + '/api/charts.json?fields=id,name,access' + (value ? '&filter=name:ilike:' + value : '') : null;
					store = ns.app.stores.chart;

				store.page = store.page <= 1 ? 1 : store.page - 1;
				store.loadStore(url);
			}
		});

		nextButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.next,
			handler: function() {
				var value = searchTextfield.getValue(),
					url = value ? ns.core.init.contextPath + '/api/charts.json?fields=id,name,access' + (value ? '&filter=name:ilike:' + value : '') : null;
					store = ns.app.stores.chart;

				store.page = store.page + 1;
				store.loadStore(url);
			}
		});

		info = Ext.create('Ext.form.Label', {
			cls: 'ns-label-info',
			width: 300,
			height: 22
		});

		grid = Ext.create('Ext.grid.Panel', {
			cls: 'ns-grid',
			scroll: false,
			hideHeaders: true,
			columns: [
				{
					dataIndex: 'name',
					sortable: false,
					width: windowCmpWidth - 88,
					renderer: function(value, metaData, record) {
						var fn = function() {
							var element = Ext.get(record.data.id);

							if (element) {
								element = element.parent('td');
								element.addClsOnOver('link');
								element.load = function() {
									favoriteWindow.hide();
									ns.core.web.chart.loadChart(record.data.id);
								};
								element.dom.setAttribute('onclick', 'Ext.get(this).load();');
							}
						};

						Ext.defer(fn, 100);

						return '<div id="' + record.data.id + '">' + value + '</div>';
					}
				},
				{
					xtype: 'actioncolumn',
					sortable: false,
					width: 80,
					items: [
						{
							iconCls: 'ns-grid-row-icon-edit',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-edit' + (!record.data.access.update ? ' disabled' : '');
							},
							handler: function(grid, rowIndex, colIndex, col, event) {
								var record = this.up('grid').store.getAt(rowIndex);

								if (record.data.access.update) {
									nameWindow = new NameWindow(record.data.id);
									nameWindow.show();
								}
							}
						},
						{
							iconCls: 'ns-grid-row-icon-overwrite',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-overwrite' + (!record.data.access.update ? ' disabled' : '');
							},
							handler: function(grid, rowIndex, colIndex, col, event) {
								var record = this.up('grid').store.getAt(rowIndex),
									message,
									favorite;

								if (record.data.access.update) {
									message = NS.i18n.overwrite_favorite + '?\n\n' + record.data.name;
									favorite = getBody();

									if (favorite) {
										favorite.name = record.data.name;

										if (confirm(message)) {
											Ext.Ajax.request({
												url: ns.core.init.contextPath + '/api/charts/' + record.data.id + '?mergeStrategy=REPLACE',
												method: 'PUT',
												headers: {'Content-Type': 'application/json'},
												params: Ext.encode(favorite),
												success: function(r) {
													ns.app.layout.id = record.data.id;
													ns.app.xLayout.id = record.data.id;

													ns.app.layout.name = true;
													ns.app.xLayout.name = true;

													ns.app.stores.chart.loadStore();
												}
											});
										}
									}
									else {
										ns.alert(NS.i18n.please_create_a_table_first);
									}
								}
							}
						},
						{
							iconCls: 'ns-grid-row-icon-sharing',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-sharing' + (!record.data.access.manage ? ' disabled' : '');
							},
							handler: function(grid, rowIndex) {
								var record = this.up('grid').store.getAt(rowIndex);

								if (record.data.access.manage) {
									Ext.Ajax.request({
										url: ns.core.init.contextPath + '/api/sharing?type=chart&id=' + record.data.id,
										method: 'GET',
										failure: function(r) {
											ns.app.viewport.mask.hide();
                                            ns.alert(r);
										},
										success: function(r) {
											var sharing = Ext.decode(r.responseText),
												window = SharingWindow(sharing);
											window.show();
										}
									});
								}
							}
						},
						{
							iconCls: 'ns-grid-row-icon-delete',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-delete' + (!record.data.access['delete'] ? ' disabled' : '');
							},
							handler: function(grid, rowIndex, colIndex, col, event) {
								var record = this.up('grid').store.getAt(rowIndex),
									message;

								if (record.data.access['delete']) {
									message = NS.i18n.delete_favorite + '?\n\n' + record.data.name;

									if (confirm(message)) {
										Ext.Ajax.request({
											url: ns.core.init.contextPath + '/api/charts/' + record.data.id,
											method: 'DELETE',
											success: function() {
												ns.app.stores.chart.loadStore();
											}
										});
									}
								}
							}
						}
					]
				},
				{
					sortable: false,
					width: 6
				}
			],
			store: ns.app.stores.chart,
			bbar: [
				info,
				'->',
				prevButton,
				nextButton
			],
			listeners: {
				render: function() {
					var size = Math.floor((ns.app.centerRegion.getHeight() - 155) / ns.core.conf.layout.grid_row_height);
					this.store.pageSize = size;
					this.store.page = 1;
					this.store.loadStore();

					ns.app.stores.chart.on('load', function() {
						if (this.isVisible()) {
							this.fireEvent('afterrender');
						}
					}, this);
				},
				afterrender: function() {
					var fn = function() {
						var editArray = Ext.query('.tooltip-favorite-edit'),
							overwriteArray = Ext.query('.tooltip-favorite-overwrite'),
							//dashboardArray = Ext.query('.tooltip-favorite-dashboard'),
							sharingArray = Ext.query('.tooltip-favorite-sharing'),
							deleteArray = Ext.query('.tooltip-favorite-delete'),
							el;

						for (var i = 0; i < editArray.length; i++) {
							var el = editArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: NS.i18n.rename,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}

						for (var i = 0; i < overwriteArray.length; i++) {
							el = overwriteArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: NS.i18n.overwrite,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}

						for (var i = 0; i < sharingArray.length; i++) {
							el = sharingArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: NS.i18n.share_with_other_people,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}

						for (var i = 0; i < deleteArray.length; i++) {
							el = deleteArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: NS.i18n.delete_,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}
					};

					Ext.defer(fn, 100);
				},
				itemmouseenter: function(grid, record, item) {
					this.currentItem = Ext.get(item);
					this.currentItem.removeCls('x-grid-row-over');
				},
				select: function() {
					this.currentItem.removeCls('x-grid-row-selected');
				},
				selectionchange: function() {
					this.currentItem.removeCls('x-grid-row-focused');
				}
			}
		});

		favoriteWindow = Ext.create('Ext.window.Window', {
			title: NS.i18n.favorites + (ns.app.layout && ns.app.layout.name ? '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + ns.app.layout.name + '</span>' : ''),
			bodyStyle: 'padding:1px; background-color:#fff',
			resizable: false,
			modal: true,
			width: windowWidth,
			destroyOnBlur: true,
			items: [
				{
					xtype: 'panel',
					layout: 'hbox',
					bodyStyle: 'border:0 none',
					height: 27,
					items: [
						addButton,
						{
							height: 26,
							width: 1,
							style: 'width:1px; margin-left:1px; margin-right:1px',
							bodyStyle: 'border-left: 1px solid #aaa'
						},
						searchTextfield
					]
				},
				grid
			],
			listeners: {
				show: function(w) {
					ns.core.web.window.setAnchorPosition(w, ns.app.favoriteButton);

					if (!w.hasDestroyOnBlurHandler) {
						ns.core.web.window.addDestroyOnBlurHandler(w);
					}

					searchTextfield.focus(false, 500);
				}
			}
		});

		return favoriteWindow;
	};

	SharingWindow = function(sharing) {

		// Objects
		var UserGroupRow,

		// Functions
			getBody,

		// Components
			userGroupStore,
			userGroupField,
			userGroupButton,
			userGroupRowContainer,
			externalAccess,
			publicGroup,
			window;

		UserGroupRow = function(obj, isPublicAccess, disallowPublicAccess) {
			var getData,
				store,
				getItems,
				combo,
				getAccess,
				panel;

			getData = function() {
				var data = [
					{id: 'r-------', name: NS.i18n.can_view},
					{id: 'rw------', name: NS.i18n.can_edit_and_view}
				];

				if (isPublicAccess) {
					data.unshift({id: '--------', name: NS.i18n.none});
				}

				return data;
			}

			store = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				data: getData()
			});

			getItems = function() {
				var items = [];

				combo = Ext.create('Ext.form.field.ComboBox', {
                    style: 'margin-bottom:2px',
					fieldLabel: isPublicAccess ? NS.i18n.public_access : obj.name,
					labelStyle: 'color:#333',
					cls: 'ns-combo',
					width: 380,
					labelWidth: 250,
					queryMode: 'local',
					valueField: 'id',
					displayField: 'name',
					labelSeparator: null,
					editable: false,
					disabled: !!disallowPublicAccess,
					value: obj.access || 'rw------',
					store: store
				});

				items.push(combo);

				if (!isPublicAccess) {
					items.push(Ext.create('Ext.Img', {
						src: 'images/grid-delete_16.png',
						style: 'margin-top:2px; margin-left:7px',
						overCls: 'pointer',
						width: 16,
						height: 16,
						listeners: {
							render: function(i) {
								i.getEl().on('click', function(e) {
									i.up('panel').destroy();
									window.doLayout();
								});
							}
						}
					}));
				}

				return items;
			};

			getAccess = function() {
				return {
					id: obj.id,
					name: obj.name,
					access: combo.getValue()
				};
			};

			panel = Ext.create('Ext.panel.Panel', {
				layout: 'column',
				bodyStyle: 'border:0 none',
				getAccess: getAccess,
				items: getItems()
			});

			return panel;
		};

		getBody = function() {
			if (!ns.core.init.user) {
				ns.alert('User is not assigned to any organisation units');
				return;
			}

			var body = {
				object: {
					id: sharing.object.id,
					name: sharing.object.name,
					publicAccess: publicGroup.down('combobox').getValue(),
					externalAccess: externalAccess ? externalAccess.getValue() : false,
					user: {
						id: ns.core.init.user.id,
						name: ns.core.init.user.name
					}
				}
			};

			if (userGroupRowContainer.items.items.length > 1) {
				body.object.userGroupAccesses = [];
				for (var i = 1, item; i < userGroupRowContainer.items.items.length; i++) {
					item = userGroupRowContainer.items.items[i];
					body.object.userGroupAccesses.push(item.getAccess());
				}
			}

			return body;
		};

		// Initialize
		userGroupStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: ns.core.init.contextPath + '/api/sharing/search',
                extraParams: {
                    pageSize: 50
                },
                startParam: false,
				limitParam: false,
				reader: {
					type: 'json',
					root: 'userGroups'
				}
			}
		});

		userGroupField = Ext.create('Ext.form.field.ComboBox', {
			valueField: 'id',
			displayField: 'name',
			emptyText: NS.i18n.search_for_user_groups,
			queryParam: 'key',
			queryDelay: 200,
			minChars: 1,
			hideTrigger: true,
			fieldStyle: 'height:26px; padding-left:6px; border-radius:1px; font-size:11px',
			style: 'margin-bottom:5px',
			width: 380,
			store: userGroupStore,
			listeners: {
				beforeselect: function(cb) { // beforeselect instead of select, fires regardless of currently selected item
					userGroupButton.enable();
				},
				afterrender: function(cb) {
					cb.inputEl.on('keyup', function() {
						userGroupButton.disable();
					});
				}
			}
		});

		userGroupButton = Ext.create('Ext.button.Button', {
			text: '+',
			style: 'margin-left:2px; padding-right:4px; padding-left:4px; border-radius:1px',
			disabled: true,
			height: 26,
			handler: function(b) {
				userGroupRowContainer.add(UserGroupRow({
					id: userGroupField.getValue(),
					name: userGroupField.getRawValue(),
					access: 'r-------'
				}));

				userGroupField.clearValue();
				b.disable();
			}
		});

		userGroupRowContainer = Ext.create('Ext.container.Container', {
			bodyStyle: 'border:0 none'
		});

		if (sharing.meta.allowExternalAccess) {
			externalAccess = userGroupRowContainer.add({
				xtype: 'checkbox',
				fieldLabel: NS.i18n.allow_external_access,
				labelSeparator: '',
				labelWidth: 250,
				checked: !!sharing.object.externalAccess
			});
		}

		publicGroup = userGroupRowContainer.add(UserGroupRow({
			id: sharing.object.id,
			name: sharing.object.name,
			access: sharing.object.publicAccess
		}, true, !sharing.meta.allowPublicAccess));

		if (Ext.isArray(sharing.object.userGroupAccesses)) {
			for (var i = 0, userGroupRow; i < sharing.object.userGroupAccesses.length; i++) {
				userGroupRow = UserGroupRow(sharing.object.userGroupAccesses[i]);
				userGroupRowContainer.add(userGroupRow);
			}
		}

		window = Ext.create('Ext.window.Window', {
			title: NS.i18n.sharing_settings,
			bodyStyle: 'padding:5px 5px 3px; background-color:#fff',
			resizable: false,
			modal: true,
			destroyOnBlur: true,
			items: [
				{
					html: sharing.object.name,
					bodyStyle: 'border:0 none; font-weight:bold; color:#333',
					style: 'margin-bottom:7px'
				},
				{
					xtype: 'container',
					layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
						userGroupField,
						userGroupButton
					]
				},
				{
					html: NS.i18n.created_by + ' ' + sharing.object.user.name,
					bodyStyle: 'border:0 none; color:#777',
					style: 'margin-top:2px;margin-bottom:7px'
				},
				userGroupRowContainer
			],
			bbar: [
				'->',
				{
					text: NS.i18n.save,
					handler: function() {
						Ext.Ajax.request({
							url: ns.core.init.contextPath + '/api/sharing?type=chart&id=' + sharing.object.id,
							method: 'POST',
							headers: {
								'Content-Type': 'application/json'
							},
							params: Ext.encode(getBody())
						});

						window.destroy();
					}
				}
			],
			listeners: {
				show: function(w) {
					var pos = ns.app.favoriteWindow.getPosition();
					w.setPosition(pos[0] + 5, pos[1] + 5);

					if (!w.hasDestroyOnBlurHandler) {
						ns.core.web.window.addDestroyOnBlurHandler(w);
					}

					ns.app.favoriteWindow.destroyOnBlur = false;
				},
				destroy: function() {
					ns.app.favoriteWindow.destroyOnBlur = true;
				}
			}
		});

		return window;
	};

	InterpretationWindow = function() {
		var textArea,
			shareButton,
			window;

		if (Ext.isString(ns.app.layout.id)) {
			textArea = Ext.create('Ext.form.field.TextArea', {
				cls: 'ns-textarea',
				height: 130,
				fieldStyle: 'padding-left: 3px; padding-top: 3px',
				emptyText: NS.i18n.write_your_interpretation + '..',
				enableKeyEvents: true,
				listeners: {
					keyup: function() {
						shareButton.xable();
					}
				}
			});

			shareButton = Ext.create('Ext.button.Button', {
				text: NS.i18n.share,
				disabled: true,
				xable: function() {
					this.setDisabled(!textArea.getValue());
				},
				handler: function() {
					if (textArea.getValue()) {
						Ext.Ajax.request({
							url: ns.core.init.contextPath + '/api/interpretations/chart/' + ns.app.layout.id,
							method: 'POST',
							params: textArea.getValue(),
							headers: {'Content-Type': 'text/html'},
							success: function() {
								textArea.reset();
								window.hide();
							}
						});
					}
				}
			});

			window = Ext.create('Ext.window.Window', {
				title: 'Write interpretation' + '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + ns.app.layout.name + '</span>',
				layout: 'fit',
				//iconCls: 'ns-window-title-interpretation',
				width: 500,
				bodyStyle: 'padding:1px; background-color:#fff',
				resizable: false,
				destroyOnBlur: true,
				modal: true,
				items: [
					textArea
				],
				bbar: {
					cls: 'ns-toolbar-bbar',
					defaults: {
						height: 24
					},
					items: [
						'->',
						shareButton
					]
				},
				listeners: {
					show: function(w) {
						ns.core.web.window.setAnchorPosition(w, ns.app.shareButton);

						document.body.oncontextmenu = true;

						if (!w.hasDestroyOnBlurHandler) {
							ns.core.web.window.addDestroyOnBlurHandler(w);
						}
					},
					hide: function() {
						document.body.oncontextmenu = function(){return false;};
					},
					destroy: function() {
						ns.app.interpretationWindow = null;
					}
				}
			});

			return window;
		}

		return;
	};

	AboutWindow = function() {
		return Ext.create('Ext.window.Window', {
			title: NS.i18n.about,
			bodyStyle: 'background:#fff; padding:6px',
			modal: true,
            resizable: false,
			hideOnBlur: true,
			listeners: {
				show: function(w) {
					Ext.Ajax.request({
						url: ns.core.init.contextPath + '/api/system/info.json',
						success: function(r) {
							var info = Ext.decode(r.responseText),
								divStyle = 'padding:3px',
								html = '<div class="user-select">';

							if (Ext.isObject(info)) {
								html += '<div style="' + divStyle + '"><b>' + NS.i18n.time_since_last_data_update + ': </b>' + info.intervalSinceLastAnalyticsTableSuccess + '</div>';
								html += '<div style="' + divStyle + '"><b>' + NS.i18n.version + ': </b>' + info.version + '</div>';
								html += '<div style="' + divStyle + '"><b>' + NS.i18n.revision + ': </b>' + info.revision + '</div>';
                                html += '<div style="' + divStyle + '"><b>' + NS.i18n.username + ': </b>' + ns.core.init.userAccount.username + '</div>';
                                html += '</div>';
							}
							else {
								html += 'No system info found';
							}

							w.update(html);
						},
						failure: function(r) {
							html += r.status + '\n' + r.statusText + '\n' + r.responseText;

							w.update(html);
						},
                        callback: function() {
                            document.body.oncontextmenu = true;

                            if (ns.app.aboutButton.rendered) {
                                ns.core.web.window.setAnchorPosition(w, ns.app.aboutButton);

                                if (!w.hasHideOnBlurHandler) {
                                    ns.core.web.window.addHideOnBlurHandler(w);
                                }
                            }
                        }
					});
				},
                hide: function() {
                    document.body.oncontextmenu = function() {
                        return false;
                    };
                },
                destroy: function() {
                    document.body.oncontextmenu = function() {
                        return false;
                    };
                }
			}
		});
	};

	// core
    extendCore = function(core) {
        var init = core.init,
            conf = core.conf,
			api = core.api,
			support = core.support,
			service = core.service,
			web = core.web;

        // init
        (function() {

			// root nodes
			for (var i = 0; i < init.rootNodes.length; i++) {
				init.rootNodes[i].expanded = true;
				init.rootNodes[i].path = '/' + conf.finals.root.id + '/' + init.rootNodes[i].id;
			}

			// sort organisation unit levels
			if (Ext.isArray(init.organisationUnitLevels)) {
				support.prototype.array.sort(init.organisationUnitLevels, 'ASC', 'level');
			}
		}());

		// support
		(function() {

			// svg
			support.svg = support.svg || {};

			support.svg.submitForm = function(type) {
				var svg = Ext.query('svg'),
					form = Ext.query('#exportForm')[0];

				if (!(Ext.isArray(svg) && svg.length)) {
					ns.alert('Browser does not support SVG');
					return;
				}

				svg = Ext.get(svg[0]);
				svg = svg.parent().dom.innerHTML;

				Ext.query('#svgField')[0].value = svg;
				Ext.query('#filenameField')[0].value = 'test';

				form.action = ns.core.init.contextPath + '/api/svg.' + type;
				form.submit();
			};
		}());

		// web
		(function() {

			// multiSelect
			web.multiSelect = web.multiSelect || {};

			web.multiSelect.select = function(a, s) {
				var selected = a.getValue();
				if (selected.length) {
					var array = [];
					Ext.Array.each(selected, function(item) {
                        array.push(a.store.getAt(a.store.findExact('id', item)));
					});
					s.store.add(array);
				}
				this.filterAvailable(a, s);
			};

			web.multiSelect.selectAll = function(a, s, isReverse) {
                var array = a.store.getRange();
				if (isReverse) {
					array.reverse();
				}
				s.store.add(array);
				this.filterAvailable(a, s);
			};

			web.multiSelect.unselect = function(a, s) {
				var selected = s.getValue();
				if (selected.length) {
					Ext.Array.each(selected, function(id) {
						a.store.add(s.store.getAt(s.store.findExact('id', id)));
						s.store.remove(s.store.getAt(s.store.findExact('id', id)));
					});
					this.filterAvailable(a, s);
                    a.store.sortStore();
				}
			};

			web.multiSelect.unselectAll = function(a, s) {
				a.store.add(s.store.getRange());
				s.store.removeAll();
				this.filterAvailable(a, s);
                a.store.sortStore();
			};

			web.multiSelect.filterAvailable = function(a, s) {
				if (a.store.getRange().length && s.store.getRange().length) {
					var recordsToRemove = [];

					a.store.each( function(ar) {
						var removeRecord = false;

						s.store.each( function(sr) {
							if (sr.data.id === ar.data.id) {
								removeRecord = true;
							}
						});

						if (removeRecord) {
							recordsToRemove.push(ar);
						}
					});

					a.store.remove(recordsToRemove);
				}
			};

			web.multiSelect.setHeight = function(ms, panel, fill) {
				for (var i = 0, height; i < ms.length; i++) {
					height = panel.getHeight() - fill - (ms[i].hasToolbar ? 25 : 0);
					ms[i].setHeight(height);
				}
			};

			// url
			web.url = web.url || {};

			web.url.getParam = function(s) {
				var output = '';
				var href = window.location.href;
				if (href.indexOf('?') > -1 ) {
					var query = href.substr(href.indexOf('?') + 1);
					var query = query.split('&');
					for (var i = 0; i < query.length; i++) {
						if (query[i].indexOf('=') > -1) {
							var a = query[i].split('=');
							if (a[0].toLowerCase() === s) {
								output = a[1];
								break;
							}
						}
					}
				}
				return unescape(output);
			};

			// storage
			web.storage = web.storage || {};

				// internal
			web.storage.internal = web.storage.internal || {};

			web.storage.internal.add = function(store, storage, parent, records) {
				if (!Ext.isObject(store)) {
					console.log('support.storeage.add: store is not an object');
					return null;
				}

				storage = storage || store.storage;
				parent = parent || store.parent;

				if (!Ext.isObject(storage)) {
					console.log('support.storeage.add: storage is not an object');
					return null;
				}

				store.each( function(r) {
					if (storage[r.data.id]) {
						storage[r.data.id] = {id: r.data.id, name: r.data.name, parent: parent};
					}
				});

				if (support.prototype.array.getLength(records, true)) {
					Ext.Array.each(records, function(r) {
						if (storage[r.data.id]) {
							storage[r.data.id] = {id: r.data.id, name: r.data.name, parent: parent};
						}
					});
				}
			};

			web.storage.internal.load = function(store, storage, parent, records) {
				var a = [];

				if (!Ext.isObject(store)) {
					console.log('support.storeage.load: store is not an object');
					return null;
				}

				storage = storage || store.storage;
				parent = parent || store.parent;

				store.removeAll();

				for (var key in storage) {
					var record = storage[key];

					if (storage.hasOwnProperty(key) && record.parent === parent) {
						a.push(record);
					}
				}

				if (support.prototype.array.getLength(records)) {
					a = a.concat(records);
				}

				store.add(a);
				store.sort('name', 'ASC');
			};

				// session
			web.storage.session = web.storage.session || {};

			web.storage.session.set = function(layout, session, url) {
				if (NS.isSessionStorage) {
					var dhis2 = JSON.parse(sessionStorage.getItem('dhis2')) || {};
					dhis2[session] = layout;
					sessionStorage.setItem('dhis2', JSON.stringify(dhis2));

					if (Ext.isString(url)) {
						window.location.href = url;
					}
				}
			};

			// chart
			web.chart = web.chart || {};

			web.chart.getLayoutConfig = function() {
				var panels = ns.app.accordion.panels,
					columnDimNames = ns.app.stores.col.getDimensionNames(),
					rowDimNames = ns.app.stores.row.getDimensionNames(),
					filterDimNames = ns.app.stores.filter.getDimensionNames(),
					config = ns.app.optionsWindow.getOptions(),
					dx = dimConf.data.dimensionName,
					co = dimConf.category.dimensionName,
					nameDimArrayMap = {};

                config.type = ns.app.viewport.chartType.getChartType();

                config.columns = [];
                config.rows = [];
                config.filters = [];

                // panel data
                for (var i = 0, dim, dimName; i < panels.length; i++) {
                    dim = panels[i].getDimension();

                    if (dim) {
                        nameDimArrayMap[dim.dimension] = [dim];
                    }
                }

				// columns, rows, filters
				for (var i = 0, nameArrays = [columnDimNames, rowDimNames, filterDimNames], axes = [config.columns, config.rows, config.filters], dimNames; i < nameArrays.length; i++) {
					dimNames = nameArrays[i];

					for (var j = 0, dimName, dim; j < dimNames.length; j++) {
						dimName = dimNames[j];

						if (dimName === co) {
							axes[i].push({
								dimension: co,
								items: []
							});
						}
						else if (dimName === dx && nameDimArrayMap.hasOwnProperty(dimName) && nameDimArrayMap[dimName]) {
							for (var k = 0; k < nameDimArrayMap[dx].length; k++) {
								axes[i].push(Ext.clone(nameDimArrayMap[dx][k]));

                                // TODO program
                                if (nameDimArrayMap[dx][k].program) {
                                    config.program = nameDimArrayMap[dx][k].program;
                                }
							}
						}
						else if (nameDimArrayMap.hasOwnProperty(dimName) && nameDimArrayMap[dimName]) {
							for (var k = 0; k < nameDimArrayMap[dimName].length; k++) {
								axes[i].push(Ext.clone(nameDimArrayMap[dimName][k]));
							}
						}
					}
				}

				return config;
            };

            web.chart.loadChart = function(id) {
				if (!Ext.isString(id)) {
					console.log('Invalid chart id');
					return;
				}

				Ext.Ajax.request({
					url: init.contextPath + '/api/charts/' + id + '.json?fields=' + ns.core.conf.url.analysisFields.join(','),
					failure: function(r) {
						web.mask.hide(ns.app.centerRegion);

                        r = Ext.decode(r.responseText);

                        if (Ext.Array.contains([403], parseInt(r.httpStatusCode))) {
                            r.message = NS.i18n.you_do_not_have_access_to_all_items_in_this_favorite || r.message;
                        }

                        ns.alert(r);
					},
					success: function(r) {
						var layoutConfig = Ext.decode(r.responseText),
							layout = api.layout.Layout(layoutConfig);

						if (layout) {
							web.chart.getData(layout, true);
						}
					}
				});
			};

			web.chart.getData = function(layout, isUpdateGui) {
				var xLayout,
					paramString,
                    sortedParamString,
                    onFailure;

				if (!layout) {
					return;
				}

                onFailure = function(r) {
                    ns.app.viewport.setGui(layout, xLayout, isUpdateGui);
                    web.mask.hide(ns.app.centerRegion);

                    if (r) {
                        r = Ext.decode(r.responseText);

                        if (Ext.Array.contains([413, 414], parseInt(r.httpStatusCode))) {
                            web.analytics.validateUrl(init.contextPath + '/api/analytics.json' + paramString);
                        }
                        else {
                            ns.alert(r);
                        }
                    }
                };

				xLayout = service.layout.getExtendedLayout(layout);
				paramString = web.analytics.getParamString(xLayout) + '&skipData=true';
				sortedParamString = web.analytics.getParamString(xLayout, true) + '&skipMeta=true';

				// show mask
				web.mask.show(ns.app.centerRegion);

				Ext.Ajax.request({
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

                        Ext.Ajax.request({
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
                                var response = api.response.Response(Ext.decode(r.responseText));

                                if (!response) {
                                    onFailure();
                                    return;
                                }

                                response.metaData = metaData;

                                // sync xLayout with response
                                xLayout = service.layout.getSyncronizedXLayout(xLayout, response);

                                if (!xLayout) {
                                    return;
                                }

                                ns.app.paramString = sortedParamString;

                                web.chart.getChart(layout, xLayout, response, isUpdateGui);
                            }
                        });
					}
				});
			};

			web.chart.getChart = function(layout, xLayout, response, isUpdateGui) {
				var xResponse,
					xColAxis,
					xRowAxis,
					config,
                    dx = dimConf.data.objectName,
                    ind = dimConf.indicator.objectName,
                    legendSet,
                    fn;

                fn = function() {

                    // create chart
                    ns.app.chart = ns.core.web.chart.createChart(xLayout, xResponse, legendSet);

                    // update viewport
                    ns.app.centerRegion.update();
                    ns.app.centerRegion.removeAll();
                    ns.app.centerRegion.add(response.rows.length ? ns.app.chart : {
                        bodyStyle: 'padding:20px; border:0 none; background:transparent; color: #555; font-size:11px',
                        html: NS.i18n.no_values_found_for_current_selection + '.'
                    });

                    // after render
                    if (NS.isSessionStorage) {
                        web.storage.session.set(layout, 'chart');
                    }

                    ns.app.viewport.setGui(layout, xLayout, isUpdateGui);

                    web.mask.hide(ns.app.centerRegion);

                    if (NS.isDebug) {
                        console.log("layout", ns.app.layout);
                        console.log("xLayout", ns.app.xLayout);
                        console.log("response", ns.app.response);
                        console.log("xResponse", ns.app.xResponse);
                        console.log("core", ns.core);
                        console.log("app", ns.app);
                    }

                    // data statistics
                    Ext.Ajax.request({
						url: ns.core.init.contextPath + '/api/dataStatistics?eventType=CHART_VIEW' + (ns.app.layout.id ? '&favorite=' + ns.app.layout.id : ''),
						method: 'POST'
					});
                };

				if (!xLayout) {
					xLayout = service.layout.getExtendedLayout(layout);
				}

				// extend response
				xResponse = service.response.getExtendedResponse(xLayout, response);

				// references
				ns.app.layout = layout;
				ns.app.xLayout = xLayout;
				ns.app.response = response;
				ns.app.xResponse = xResponse;

                // legend set
                if (xLayout.type === 'gauge' && Ext.Array.contains(xLayout.axisObjectNames, dx) && xLayout.dimensionNameIdsMap[dx].length) {
                    Ext.Ajax.request({
                        url: ns.core.init.contextPath + '/api/indicators/' + xLayout.dimensionNameIdsMap[dx][0] + '.json?fields=legendSet[legends[id,name,startValue,endValue,color]]',
                        disableCaching: false,
                        success: function(r) {
                            legendSet = Ext.decode(r.responseText).legendSet;
                        },
                        callback: function() {
                            fn();
                        }
                    });
                }
                else {
                    fn();
                }
			};
		}());
    };

	// viewport
    createViewport = function() {
        var buttons = [],
            buttonAddedListener,
            column,
            stackedcolumn,
            bar,
            stackedbar,
            line,
            area,
            pie,
            radar,
            gauge,
            chartType,
            getDimensionStore,
            colStore,
            rowStore,
            filterStore,
            series,
            category,
            filter,
            layout,

            indicatorAvailableStore,
            indicatorGroupStore,
			dataElementAvailableStore,
			dataElementGroupStore,
			dataSetAvailableStore,
            eventDataItemAvailableStore,
            programIndicatorAvailableStore,
            programStore,
            dataSelectedStore,
			periodTypeStore,
			fixedPeriodAvailableStore,
			fixedPeriodSelectedStore,
			chartStore,
			organisationUnitLevelStore,
			organisationUnitGroupStore,

            isScrolled,
            onDataTypeSelect,
            dataType,
            dataSelected,

            indicatorLabel,
            indicatorSearch,
            indicatorFilter,
            indicatorGroup,
            indicatorAvailable,
            indicatorSelected,
            indicator,
			dataElementLabel,
            dataElementSearch,
            dataElementFilter,
            dataElementAvailable,
            dataElementSelected,
            dataElementGroup,
            dataElementDetailLevel,
            dataElement,
            dataSetLabel,
            dataSetSearch,
            dataSetFilter,
            dataSetAvailable,
            dataSetSelected,
            dataSet,
            onEventDataItemProgramSelect,
            eventDataItemProgram,
            eventDataItemLabel,
            eventDataItemSearch,
            eventDataItemFilter,
            eventDataItemAvailable,
            eventDataItemSelected,
            eventDataItem,
            onProgramIndicatorProgramSelect,
            programIndicatorProgram,
            programIndicatorLabel,
            programIndicatorSearch,
            programIndicatorFilter,
            programIndicatorAvailable,
            programIndicatorSelected,
            programIndicator,
            data,

			rewind,
            relativePeriodDefaults,
            relativePeriod,
            fixedPeriodAvailable,
            fixedPeriodSelected,
            onPeriodTypeSelect,
            periodType,
            period,
            treePanel,
            userOrganisationUnit,
            userOrganisationUnitChildren,
            userOrganisationUnitGrandChildren,
            organisationUnitLevel,
            organisationUnitGroup,
            toolMenu,
            tool,
            toolPanel,
            organisationUnit,
            dimensionPanelMap = {},
			getDimensionPanel,
			getDimensionPanels,
			update,

			accordionBody,
            accordion,
            westRegion,
            layoutButton,
            optionsButton,
            favoriteButton,
            getParamString,
            openTableLayoutTab,
            openPlainDataSource,
            downloadButton,
            interpretationItem,
            pluginItem,
            favoriteUrlItem,
            apiUrlItem,
            shareButton,
            aboutButton,
            defaultButton,
            centerRegion,
            setGui,
            viewport,

			accordionPanels = [],
            namePropertyUrl = ns.core.init.namePropertyUrl,
            nameProperty = ns.core.init.userAccount.settings.keyAnalysisDisplayProperty,

            dimConf = ns.core.conf.finals.dimension;

		ns.app.stores = ns.app.stores || {};

        buttonAddedListener = function(b) {
            buttons.push(b);
        };

        column = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.column,
            icon: 'images/column.png',
            name: chartConf.client.column,
            tooltipText: NS.i18n.column_chart,
            pressed: true,
            listeners: {
                added: buttonAddedListener
            }
        });

        stackedcolumn = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.stackedcolumn,
            icon: 'images/column-stacked.png',
            name: chartConf.client.stackedcolumn,
            tooltipText: NS.i18n.stacked_column_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        bar = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.bar,
            icon: 'images/bar.png',
            name: chartConf.client.bar,
            tooltipText: NS.i18n.bar_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        stackedbar = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.stackedbar,
            icon: 'images/bar-stacked.png',
            name: chartConf.client.stackedbar,
            tooltipText: NS.i18n.stacked_bar_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        line = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.line,
            icon: 'images/line.png',
            name: chartConf.client.line,
            tooltipText: NS.i18n.line_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        area = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.area,
            icon: 'images/area.png',
            name: chartConf.client.area,
            tooltipText: NS.i18n.area_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        pie = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.pie,
            icon: 'images/pie.png',
            name: chartConf.client.pie,
            tooltipText: NS.i18n.pie_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        radar = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.radar,
            icon: 'images/radar.png',
            name: chartConf.client.radar,
            tooltipText: NS.i18n.radar_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        gauge = Ext.create('Ext.button.Button', {
            xtype: 'button',
            chartType: chartConf.client.gauge,
            icon: 'images/gauge.png',
            name: chartConf.client.gauge,
            tooltipText: NS.i18n.meter_chart,
            listeners: {
                added: buttonAddedListener
            }
        });

        chartType = Ext.create('Ext.toolbar.Toolbar', {
            height: 45,
            style: 'padding-top:1px; border:0 none; border-bottom:1px solid #ddd',
            getChartType: function() {
                for (var i = 0; i < buttons.length; i++) {
                    if (buttons[i].pressed) {
                        return buttons[i].chartType;
                    }
                }
            },
            setChartType: function(type) {
                for (var i = 0; i < buttons.length; i++) {
                    if (buttons[i].chartType === type) {
                        buttons[i].toggle(true);
                    }
                }
            },
            defaults: {
                height: 40,
                toggleGroup: 'charttype',
                handler: function(b) {
					if (!b.pressed) {
						b.toggle();
					}
				},
                listeners: {
                    afterrender: function(b) {
                        if (b.xtype === 'button') {
                            Ext.create('Ext.tip.ToolTip', {
                                target: b.getEl(),
                                html: b.tooltipText,
                                'anchor': 'bottom'
                            });
                        }
                    }
                }
            },
            items: [
                {
                    xtype: 'label',
                    text: NS.i18n.type,
                    style: 'font-size:11px; font-weight:bold; padding:13px 8px 0 6px'
                },
                column,
                stackedcolumn,
                bar,
                stackedbar,
                line,
                area,
                pie,
                radar,
                gauge
            ]
        });

		getDimensionStore = function() {
			return Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				data: function() {
					var data = [
							{id: dimConf.data.dimensionName, name: dimConf.data.name},
							{id: dimConf.period.dimensionName, name: dimConf.period.name},
							{id: dimConf.organisationUnit.dimensionName, name: dimConf.organisationUnit.name}
						];

					return data.concat(Ext.clone(ns.core.init.dimensions));
				}()
			});
		};

		indicatorAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
            lastPage: null,
            nextPage: 1,
            isPending: false,
            reset: function() {
                this.removeAll();
                this.lastPage = null;
                this.nextPage = 1;
                this.isPending = false;
                indicatorSearch.hideFilter();
            },
            loadDataAndUpdate: function(data, append) {
                this.clearFilter(); // work around

                if (!append) {
                    this.removeAll();
                }

                this.loadData(data, append);

                this.updateFilter();
            },
            getRecordsByIds: function(ids) {
                var records = [];

                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        records.push(this.getAt(index));
                    }
                }

                return records;
            },
            updateFilter: function() {
                var selectedStoreIds = dataSelectedStore.getIds();

                this.clearFilter();

                this.filterBy(function(record) {
                    return !Ext.Array.contains(selectedStoreIds, record.data.id);
                });
            },
            loadPage: function(uid, filter, append, noPaging, fn) {
                var store = this,
					params = {},
                    path;

                uid = (Ext.isString(uid) || Ext.isNumber(uid)) ? uid : indicatorGroup.getValue();
                filter = filter || indicatorFilter.getValue() || null;

                if (!append) {
                    this.lastPage = null;
                    this.nextPage = 1;
                }

                if (store.nextPage === store.lastPage) {
                    return;
                }

				if (Ext.isString(uid)) {
					path = '/indicators.json?fields=id,' + namePropertyUrl + '&filter=indicatorGroups.id:eq:' + uid + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');
				}
				else if (uid === 0) {
					path = '/indicators.json?fields=id,' + namePropertyUrl + '' + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');
				}

				if (!path) {
					return;
				}

				if (noPaging) {
					params.paging = false;
				}
				else {
					params.page = store.nextPage;
					params.pageSize = 50;
				}

                store.isPending = true;
                ns.core.web.mask.show(indicatorAvailable.boundList);

                Ext.Ajax.request({
                    url: ns.core.init.contextPath + '/api' + path,
                    params: params,
                    success: function(r) {
                        var response = Ext.decode(r.responseText),
                            data = response.indicators || [],
                            pager = response.pager;

                        store.loadStore(data, pager, append, fn);
                    },
                    callback: function() {
                        store.isPending = false;
                        ns.core.web.mask.hide(indicatorAvailable.boundList);
                    }
                });
            },
            loadStore: function(data, pager, append, fn) {
				pager = pager || {};

                this.loadDataAndUpdate(data, append);
                this.sortStore();

                this.lastPage = this.nextPage;

                if (pager.pageCount > this.nextPage) {
                    this.nextPage++;
                }

                this.isPending = false;

                //ns.core.web.multiSelect.filterAvailable({store: indicatorAvailableStore}, {store: indicatorSelectedStore});

                if (fn) {
					fn();
				}
            },
			storage: {},
			parent: null,
			sortStore: function() {
				this.sort('name', 'ASC');
			}
		});
		ns.app.stores.indicatorAvailable = indicatorAvailableStore;

		indicatorGroupStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'index'],
			proxy: {
				type: 'ajax',
				url: ns.core.init.contextPath + '/api/indicatorGroups.json?fields=id,displayName|rename(name)&paging=false',
				reader: {
					type: 'json',
					root: 'indicatorGroups'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			},
			listeners: {
				load: function(s) {
					s.add({
						id: 0,
						name: '[ ' + NS.i18n.all_indicators + ' ]',
						index: -1
					});
					s.sort([
						{
							property: 'index',
							direction: 'ASC'
						},
						{
							property: 'name',
							direction: 'ASC'
						}
					]);
				}
			}
		});
		ns.app.stores.indicatorGroup = indicatorGroupStore;

		dataElementAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
            lastPage: null,
            nextPage: 1,
            isPending: false,
            reset: function() {
                this.removeAll();
                this.lastPage = null;
                this.nextPage = 1;
                this.isPending = false;
                dataElementSearch.hideFilter();
            },
            loadDataAndUpdate: function(data, append) {
                this.clearFilter(); // work around

                if (!append) {
                    this.removeAll();
                }

                this.loadData(data, append);

                this.updateFilter();
            },
            getRecordsByIds: function(ids) {
                var records = [];

                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        records.push(this.getAt(index));
                    }
                }

                return records;
            },
            updateFilter: function() {
                var selectedStoreIds = dataSelectedStore.getIds();

                this.clearFilter();

                if (selectedStoreIds.length) {
                    this.filterBy(function(record) {
                        return !Ext.Array.contains(selectedStoreIds, record.data.id);
                    });
                }
            },
            loadPage: function(uid, filter, append, noPaging, fn) {
                uid = (Ext.isString(uid) || Ext.isNumber(uid)) ? uid : dataElementGroup.getValue();
                filter = filter || dataElementFilter.getValue() || null;

                if (!append) {
                    this.lastPage = null;
                    this.nextPage = 1;
                }

                if (dataElementDetailLevel.getValue() === dimConf.dataElement.objectName) {
                    this.loadTotalsPage(uid, filter, append, noPaging, fn);
                }
                else if (dataElementDetailLevel.getValue() === dimConf.operand.objectName) {
                    this.loadDetailsPage(uid, filter, append, noPaging, fn);
                }
            },
            loadTotalsPage: function(uid, filter, append, noPaging, fn) {
                var store = this,
					params = {},
                    types = ns.core.conf.valueType.aAggregateTypes.join(','),
                    path;

                if (store.nextPage === store.lastPage) {
                    return;
                }

				if (Ext.isString(uid)) {
					path = '/dataElements.json?fields=dimensionItem|rename(id),' + namePropertyUrl + '&filter=valueType:in:[' + types + ']&filter=dataElementGroups.id:eq:' + uid + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');
				}
				else if (uid === 0) {
					path = '/dataElements.json?fields=dimensionItem|rename(id),' + namePropertyUrl + '&filter=valueType:in:[' + types + ']&filter=domainType:eq:AGGREGATE' + '' + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');
				}

				if (!path) {
					return;
				}

				if (noPaging) {
					params.paging = false;
				}
				else {
					params.page = store.nextPage;
					params.pageSize = 50;
				}

                store.isPending = true;
                ns.core.web.mask.show(dataElementAvailable.boundList);

                Ext.Ajax.request({
                    url: ns.core.init.contextPath + '/api' + path,
                    params: params,
                    success: function(r) {
                        var response = Ext.decode(r.responseText),
                            data = response.dataElements || [],
                            pager = response.pager;

                        store.loadStore(data, pager, append, fn);
                    },
                    callback: function() {
                        store.isPending = false;
                        ns.core.web.mask.hide(dataElementAvailable.boundList);
                    }
                });
            },
			loadDetailsPage: function(uid, filter, append, noPaging, fn) {
                var store = this,
					params = {},
                    types = ns.core.conf.valueType.aAggregateTypes.join(','),
                    path;

                if (store.nextPage === store.lastPage) {
                    return;
                }

				if (Ext.isString(uid)) {
					path = '/dataElementOperands.json?fields=dimensionItem|rename(id),' + namePropertyUrl + '&filter=valueType:in:[' + types + ']&filter=dataElement.dataElementGroups.id:eq:' + uid + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');
				}
				else if (uid === 0) {
					path = '/dataElementOperands.json?fields=dimensionItem|rename(id),' + namePropertyUrl + '&filter=valueType:in:[' + types + ']' + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');
				}

				if (!path) {
					return;
				}

				if (noPaging) {
					params.paging = false;
				}
				else {
					params.page = store.nextPage;
					params.pageSize = 50;
				}

                store.isPending = true;
                ns.core.web.mask.show(dataElementAvailable.boundList);

                Ext.Ajax.request({
                    url: ns.core.init.contextPath + '/api' + path,
                    params: params,
                    success: function(r) {
                        var response = Ext.decode(r.responseText),
							data = response.objects || response.dataElementOperands || [],
                            pager = response.pager;

                        store.loadStore(data, pager, append, fn);
                    },
                    callback: function() {
                        store.isPending = false;
                        ns.core.web.mask.hide(dataElementAvailable.boundList);
                    }
                });
			},
            loadStore: function(data, pager, append, fn) {
				pager = pager || {};

                this.loadDataAndUpdate(data, append);
                this.sortStore();

                this.lastPage = this.nextPage;

                if (pager.pageCount > this.nextPage) {
                    this.nextPage++;
                }

                this.isPending = false;

				//ns.core.web.multiSelect.filterAvailable({store: dataElementAvailableStore}, {store: dataElementSelectedStore});

                if (fn) {
					fn();
				}
            },
            sortStore: function() {
				this.sort('name', 'ASC');
			}
		});
		ns.app.stores.dataElementAvailable = dataElementAvailableStore;

		dataElementGroupStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'index'],
			proxy: {
				type: 'ajax',
				url: ns.core.init.contextPath + '/api/dataElementGroups.json?fields=id,' + ns.core.init.namePropertyUrl + '&paging=false',
				reader: {
					type: 'json',
					root: 'dataElementGroups'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			},
			listeners: {
				load: function(s) {
                    s.add({
                        id: 0,
                        name: '[ ' + NS.i18n.all_data_elements + ' ]',
                        index: -1
                    });

					s.sort([
						{property: 'index', direction: 'ASC'},
						{property: 'name', direction: 'ASC'}
					]);
				}
			}
		});
		ns.app.stores.dataElementGroup = dataElementGroupStore;

		dataSetAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
            lastPage: null,
            nextPage: 1,
            isPending: false,
            reset: function() {
                this.removeAll();
                this.lastPage = null;
                this.nextPage = 1;
                this.isPending = false;
                dataSetSearch.hideFilter();
            },
            loadDataAndUpdate: function(data, append) {
                this.clearFilter(); // work around
                this.loadData(data, append);
                this.updateFilter();
            },
            getRecordsByIds: function(ids) {
                var records = [];

                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        records.push(this.getAt(index));
                    }
                }

                return records;
            },
            updateFilter: function() {
                var selectedStoreIds = dataSelectedStore.getIds();

                this.clearFilter();

                this.filterBy(function(record) {
                    return !Ext.Array.contains(selectedStoreIds, record.data.id);
                });
            },
            loadPage: function(filter, append, noPaging, fn) {
                var store = this,
					params = {},
                    path;

                filter = filter || dataSetFilter.getValue() || null;

                if (!append) {
                    this.lastPage = null;
                    this.nextPage = 1;
                }

                if (store.nextPage === store.lastPage) {
                    return;
                }

                path = '/dataSets.json?fields=id,' + namePropertyUrl + '' + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');

				if (noPaging) {
					params.paging = false;
				}
				else {
					params.page = store.nextPage;
					params.pageSize = 50;
				}

                store.isPending = true;
                ns.core.web.mask.show(dataSetAvailable.boundList);

                Ext.Ajax.request({
                    url: ns.core.init.contextPath + '/api' + path,
                    params: params,
                    success: function(r) {
                        var response = Ext.decode(r.responseText),
                            data = response.dataSets || [],
                            pager = response.pager;

                        store.loadStore(data, pager, append, fn);
                    },
                    callback: function() {
                        store.isPending = false;
                        ns.core.web.mask.hide(dataSetAvailable.boundList);
                    }
                });
            },
            loadStore: function(data, pager, append, fn) {
				pager = pager || {};

                this.loadDataAndUpdate(data, append);
                this.sortStore();

                this.lastPage = this.nextPage;

                if (pager.pageCount > this.nextPage) {
                    this.nextPage++;
                }

                this.isPending = false;

				//ns.core.web.multiSelect.filterAvailable({store: dataSetAvailableStore}, {store: dataSetSelectedStore});

                if (fn) {
					fn();
				}
            },
			storage: {},
			parent: null,
            isLoaded: false,
			sortStore: function() {
				this.sort('name', 'ASC');
			}
		});
		ns.app.stores.dataSetAvailable = dataSetAvailableStore;

        eventDataItemAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: [],
			sortStore: function() {
				this.sort('name', 'ASC');
			},
            loadDataAndUpdate: function(data, append) {
                this.clearFilter(); // work around

                if (!append) {
                    this.removeAll();
                }

                this.loadData(data, append);

                this.updateFilter();
            },
            getRecordsByIds: function(ids) {
                var records = [];

                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        records.push(this.getAt(index));
                    }
                }

                return records;
            },
            updateFilter: function() {
                var selectedStoreIds = dataSelectedStore.getIds();

                this.clearFilter();

                this.filterBy(function(record) {
                    return !Ext.Array.contains(selectedStoreIds, record.data.id);
                });
            }
		});
		ns.app.stores.eventDataItemAvailable = eventDataItemAvailableStore;

        programIndicatorAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: [],
			sortStore: function() {
				this.sort('name', 'ASC');
			},
            loadDataAndUpdate: function(data, append) {
                this.clearFilter(); // work around

                if (!append) {
                    this.removeAll();
                }

                this.loadData(data, append);

                this.updateFilter();
            },
            getRecordsByIds: function(ids) {
                var records = [];

                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        records.push(this.getAt(index));
                    }
                }

                return records;
            },
            updateFilter: function() {
                var selectedStoreIds = dataSelectedStore.getIds();

                this.clearFilter();

                this.filterBy(function(record) {
                    return !Ext.Array.contains(selectedStoreIds, record.data.id);
                });
            }
		});
		ns.app.stores.programIndicatorAvailable = programIndicatorAvailableStore;

		programStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: ns.core.init.contextPath + '/api/programs.json?fields=id,displayName|rename(name)&paging=false',
				reader: {
					type: 'json',
					root: 'programs'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			}
		});
		ns.app.stores.program = programStore;

        dataSelectedStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: [],
            getIds: function() {
                var records = this.getRange(),
                    ids = [];

                for (var i = 0; i < records.length; i++) {
                    ids.push(records[i].data.id);
                }

                return ids;
            },
            addRecords: function(records, objectName) {
                var prop = 'objectName',
                    recordsToAdd = [],
                    objectsToAdd = [];

                records = Ext.Array.from(records);

                if (records.length) {
                    for (var i = 0, record; i < records.length; i++) {
                        record = records[i];

                        // record
                        if (record.data) {
                            if (objectName) {
                                record.set(prop, objectName);
                            }
                            recordsToAdd.push(record);
                        }
                        // object
                        else {
                            if (objectName) {
                                record[prop] = objectName;
                            }
                            objectsToAdd.push(record);
                        }
                    }

                    if (recordsToAdd.length) {
                        this.add(recordsToAdd);
                    }

                    if (objectsToAdd.length) {
                        this.loadData(objectsToAdd, true);
                    }
                }
            },
            removeByIds: function(ids) {
                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        this.removeAt(index);
                    }
                }
            },
            removeByProperty: function(property, values) {
                if (!(property && values)) {
                    return;
                }

                var recordsToRemove = [];

                values = Ext.Array.from(values);

                this.each(function(record) {
                    if (Ext.Array.contains(values, record.data[property])) {
                        recordsToRemove.push(record);
                    }
                });

                this.remove(recordsToRemove);
            },
            listeners: {
                add: function() {
                    data.updateStoreFilters();
                },
                remove: function() {
                    data.updateStoreFilters();
                },
                clear: function() {
                    data.updateStoreFilters();
                }
            }
		});
		ns.app.stores.dataSelected = dataSelectedStore;

		periodTypeStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: ns.core.conf.period.periodTypes
		});
		ns.app.stores.periodType = periodTypeStore;

		fixedPeriodAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'index'],
			data: [],
			setIndex: function(periods) {
				for (var i = 0; i < periods.length; i++) {
					periods[i].index = i;
				}
			},
			sortStore: function() {
				this.sort('index', 'ASC');
			}
		});
		ns.app.stores.fixedPeriodAvailable = fixedPeriodAvailableStore;

		fixedPeriodSelectedStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'index'],
			data: []
		});
		ns.app.stores.fixedPeriodSelected = fixedPeriodSelectedStore;

		chartStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'lastUpdated', 'access'],
			proxy: {
				type: 'ajax',
				reader: {
					type: 'json',
					root: 'charts'
				},
				startParam: false,
				limitParam: false
			},
			isLoaded: false,
			pageSize: 10,
			page: 1,
			defaultUrl: ns.core.init.contextPath + '/api/charts.json?fields=id,displayName|rename(name),access',
			loadStore: function(url) {
				this.proxy.url = url || this.defaultUrl;

				this.load({
					params: {
						pageSize: this.pageSize,
						page: this.page
					}
				});
			},
			loadFn: function(fn) {
				if (this.isLoaded) {
					fn.call();
				}
				else {
					this.load(fn);
				}
			},
			listeners: {
				load: function(s) {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}

					this.sort('name', 'ASC');
				}
			}
		});
		ns.app.stores.chart = chartStore;

		organisationUnitLevelStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'level'],
			data: ns.core.init.organisationUnitLevels
		});
		ns.app.stores.organisationUnitLevel = organisationUnitLevelStore;

		organisationUnitGroupStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: ns.core.init.contextPath + '/api/organisationUnitGroups.json?fields=id,' + ns.core.init.namePropertyUrl + '&paging=false',
				reader: {
					type: 'json',
					root: 'organisationUnitGroups'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			}
		});
		ns.app.stores.organisationUnitGroup = organisationUnitGroupStore;

		// data

		isScrolled = function(e) {
			var el = e.srcElement,
				scrollBottom = el.scrollTop + ((el.clientHeight / el.scrollHeight) * el.scrollHeight);

			return scrollBottom / el.scrollHeight > 0.9;
		};

        onDataTypeSelect = function(type) {
            type = type || 'in';

            if (type === 'in') {
                indicator.show();
                dataElement.hide();
                dataSet.hide();
                eventDataItem.hide();
                programIndicator.hide();
            }
            else if (type === 'de') {
                indicator.hide();
                dataElement.show();
                dataSet.hide();
                eventDataItem.hide();
                programIndicator.hide();
            }
            else if (type === 'ds') {
                indicator.hide();
                dataElement.hide();
                dataSet.show();
                eventDataItem.hide();
                programIndicator.hide();

				if (!dataSetAvailableStore.isLoaded) {
                    dataSetAvailableStore.isLoaded = true;
					dataSetAvailableStore.loadPage(null, false);
                }
            }
            else if (type === 'di') {
                indicator.hide();
                dataElement.hide();
                dataSet.hide();
                eventDataItem.show();
                programIndicator.hide();

                if (!programStore.isLoaded) {
                    programStore.isLoaded = true;
                    programStore.load();
                }
            }
            else if (type === 'pi') {
                indicator.hide();
                dataElement.hide();
                dataSet.hide();
                eventDataItem.hide();
                programIndicator.show();

                if (!programStore.isLoaded) {
                    programStore.isLoaded = true;
                    programStore.load();
                }
            }
        };

        dataType = Ext.create('Ext.form.field.ComboBox', {
            cls: 'ns-combo',
            style: 'margin-bottom:1px',
            width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding,
            valueField: 'id',
            displayField: 'name',
            //emptyText: NS.i18n.data_type,
            editable: false,
            queryMode: 'local',
            value: 'in',
            store: {
                fields: ['id', 'name'],
                data: [
                     {id: 'in', name: NS.i18n.indicators},
                     {id: 'de', name: NS.i18n.data_elements},
                     {id: 'ds', name: NS.i18n.reporting_rates},
                     {id: 'di', name: NS.i18n.event_data_items},
                     {id: 'pi', name: NS.i18n.program_indicators}
                ]
            },
            listeners: {
                select: function(cb) {
                    onDataTypeSelect(cb.getValue());
                }
            }
        });

        dataSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			ddReorder: true,
			store: dataSelectedStore,
			tbar: [
				{
					xtype: 'button',
					icon: 'images/arrowleftdouble.png',
					width: 22,
					handler: function() {
						//ns.core.web.multiSelect.unselectAll(programIndicatorAvailable, programIndicatorSelected);
                        dataSelectedStore.removeAll();
                        data.updateStoreFilters();
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
						//ns.core.web.multiSelect.unselect(programIndicatorAvailable, programIndicatorSelected);
                        dataSelectedStore.removeByIds(dataSelected.getValue());
                        data.updateStoreFilters();
					}
				},
				'->',
				{
					xtype: 'label',
					text: NS.i18n.selected,
					cls: 'ns-toolbar-multiselect-right-label'
				}
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function() {
						//ns.core.web.multiSelect.unselect(programIndicatorAvailable, this);
                        dataSelectedStore.removeByIds(dataSelected.getValue());
                        data.updateStoreFilters();
					}, this);
				}
			}
		});

        // indicator
        indicatorLabel = Ext.create('Ext.form.Label', {
            text: NS.i18n.available,
            cls: 'ns-toolbar-multiselect-left-label',
            style: 'margin-right:5px'
        });

        indicatorSearch = Ext.create('Ext.button.Button', {
            width: 22,
            height: 22,
            cls: 'ns-button-icon',
            disabled: true,
            style: 'background: url(images/search_14.png) 3px 3px no-repeat',
            showFilter: function() {
                indicatorLabel.hide();
                this.hide();
                indicatorFilter.show();
                indicatorFilter.reset();
            },
            hideFilter: function() {
                indicatorLabel.show();
                this.show();
                indicatorFilter.hide();
                indicatorFilter.reset();
            },
            handler: function() {
                this.showFilter();
            }
        });

        indicatorFilter = Ext.create('Ext.form.field.Trigger', {
            cls: 'ns-trigger-filter',
            emptyText: 'Filter available..',
            height: 22,
            hidden: true,
            enableKeyEvents: true,
            fieldStyle: 'height:22px; border-right:0 none',
            style: 'height:22px',
            onTriggerClick: function() {
				if (this.getValue()) {
					this.reset();
					this.onKeyUpHandler();
				}
            },
            onKeyUpHandler: function() {
                var value = indicatorGroup.getValue(),
                    store = indicatorAvailableStore;

                if (Ext.isString(value) || Ext.isNumber(value)) {
                    store.loadPage(null, this.getValue(), false);
                }
            },
            listeners: {
                keyup: {
                    fn: function(cmp) {
                        cmp.onKeyUpHandler();
                    },
                    buffer: 100
                },
                show: function(cmp) {
                    cmp.focus(false, 50);
                },
                focus: function(cmp) {
                    cmp.addCls('ns-trigger-filter-focused');
                },
                blur: function(cmp) {
                    cmp.removeCls('ns-trigger-filter-focused');
                }
            }
        });

        indicatorGroup = Ext.create('Ext.form.field.ComboBox', {
            cls: 'ns-combo',
            style: 'margin-bottom:1px; margin-top:0px',
            width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding,
            valueField: 'id',
            displayField: 'name',
            emptyText: NS.i18n.select_indicator_group,
            editable: false,
            store: indicatorGroupStore,
			loadAvailable: function(reset) {
				var store = indicatorAvailableStore,
					id = this.getValue();

				if (id !== null) {
                    if (reset) {
                        store.reset();
                    }

                    store.loadPage(id, null, false);
				}
			},
			listeners: {
				select: function(cb) {
					cb.loadAvailable(true);

                    indicatorSearch.enable();
				}
			}
        });

		indicatorAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			store: indicatorAvailableStore,
			tbar: [
				indicatorLabel,
                indicatorSearch,
                indicatorFilter,
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowright.png',
					width: 22,
					handler: function() {
                        if (indicatorAvailable.getValue().length) {
                            var records = indicatorAvailableStore.getRecordsByIds(indicatorAvailable.getValue());
                            dataSelectedStore.addRecords(records, 'in');
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowrightdouble.png',
					width: 22,
					handler: function() {
						indicatorAvailableStore.loadPage(null, null, null, true, function() {
                            dataSelectedStore.addRecords(indicatorAvailableStore.getRange(), 'in');
						});
					}
				}
			],
			listeners: {
				render: function(ms) {
                    var el = Ext.get(ms.boundList.getEl().id + '-listEl').dom;

                    el.addEventListener('scroll', function(e) {
                        if (isScrolled(e) && !indicatorAvailableStore.isPending) {
                            indicatorAvailableStore.loadPage(null, null, true);
                        }
                    });

					ms.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.addRecords(record, 'in');
					}, ms);
				}
			}
		});

		indicatorSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			ddReorder: true,
			store: dataSelectedStore,
			tbar: [
				{
					xtype: 'button',
					icon: 'images/arrowleftdouble.png',
					width: 22,
					handler: function() {
                        if (dataSelectedStore.getRange().length) {
                            dataSelectedStore.removeAll();
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
                        if (indicatorSelected.getValue().length) {
                            dataSelectedStore.removeByIds(indicatorSelected.getValue());
                        }
					}
				},
				'->',
				{
					xtype: 'label',
					text: NS.i18n.selected,
					cls: 'ns-toolbar-multiselect-right-label'
				}
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.removeByIds(record.data.id);
					}, this);
				}
			}
		});

		indicator = Ext.create('Ext.panel.Panel', {
			xtype: 'panel',
			//title: '<div class="ns-panel-title-data">' + NS.i18n.indicators + '</div>',
            preventHeader: true,
			hideCollapseTool: true,
            dimension: dimConf.indicator.objectName,
            bodyStyle: 'border:0 none',
			items: [
				indicatorGroup,
				{
					xtype: 'panel',
					layout: 'column',
					bodyStyle: 'border-style:none',
					items: [
                        indicatorAvailable,
						indicatorSelected
					]
				}
			],
			listeners: {
				added: function() {
					//accordionPanels.push(this);
				},
				expand: function(p) {
					//p.onExpand();
				}
			}
		});

        // data element
        dataElementLabel = Ext.create('Ext.form.Label', {
            text: NS.i18n.available,
            cls: 'ns-toolbar-multiselect-left-label',
            style: 'margin-right:5px'
        });

        dataElementSearch = Ext.create('Ext.button.Button', {
            width: 22,
            height: 22,
            cls: 'ns-button-icon',
            disabled: true,
            style: 'background: url(images/search_14.png) 3px 3px no-repeat',
            showFilter: function() {
                dataElementLabel.hide();
                this.hide();
                dataElementFilter.show();
                dataElementFilter.reset();
            },
            hideFilter: function() {
                dataElementLabel.show();
                this.show();
                dataElementFilter.hide();
                dataElementFilter.reset();
            },
            handler: function() {
                this.showFilter();
            }
        });

        dataElementFilter = Ext.create('Ext.form.field.Trigger', {
            cls: 'ns-trigger-filter',
            emptyText: 'Filter available..',
            height: 22,
            hidden: true,
            enableKeyEvents: true,
            fieldStyle: 'height:22px; border-right:0 none',
            style: 'height:22px',
            onTriggerClick: function() {
				if (this.getValue()) {
					this.reset();
					this.onKeyUpHandler();
				}
            },
            onKeyUpHandler: function() {
                var value = dataElementGroup.getValue(),
                    store = dataElementAvailableStore;

                if (Ext.isString(value) || Ext.isNumber(value)) {
                    store.loadPage(null, this.getValue(), false);
                }
            },
            listeners: {
                keyup: {
                    fn: function(cmp) {
                        cmp.onKeyUpHandler();
                    },
                    buffer: 100
                },
                show: function(cmp) {
                    cmp.focus(false, 50);
                },
                focus: function(cmp) {
                    cmp.addCls('ns-trigger-filter-focused');
                },
                blur: function(cmp) {
                    cmp.removeCls('ns-trigger-filter-focused');
                }
            }
        });

		dataElementAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
            isPending: false,
            page: 1,
			store: dataElementAvailableStore,
			tbar: [
				dataElementLabel,
                dataElementSearch,
                dataElementFilter,
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowright.png',
					width: 22,
					handler: function() {
                        if (dataElementAvailable.getValue().length) {
                            var records = dataElementAvailableStore.getRecordsByIds(dataElementAvailable.getValue());
                            dataSelectedStore.addRecords(records, 'de');
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowrightdouble.png',
					width: 22,
					handler: function() {
						dataElementAvailableStore.loadPage(null, null, null, true, function() {
                            dataSelectedStore.addRecords(dataElementAvailableStore.getRange(), 'de');
						});
					}
				}
			],
			listeners: {
				render: function(ms) {
                    var el = Ext.get(ms.boundList.getEl().id + '-listEl').dom;

                    el.addEventListener('scroll', function(e) {
                        if (isScrolled(e) && !dataElementAvailableStore.isPending) {
                            dataElementAvailableStore.loadPage(null, null, true);
                        }
                    });

					ms.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.addRecords(record, 'de');
					}, ms);
				}
			}
		});

		dataElementSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			ddReorder: true,
			store: dataSelectedStore,
			tbar: [
				{
					xtype: 'button',
					icon: 'images/arrowleftdouble.png',
					width: 22,
					handler: function() {
                        if (dataSelectedStore.getRange().length) {
                            dataSelectedStore.removeAll();
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
                        if (dataElementSelected.getValue().length) {
                            dataSelectedStore.removeByIds(dataElementSelected.getValue());
                        }
					}
				},
				'->',
				{
					xtype: 'label',
					text: NS.i18n.selected,
					cls: 'ns-toolbar-multiselect-right-label'
				}
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.removeByIds(record.data.id);
					}, this);
				}
			}
		});

		dataElementGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin:0 1px 1px 0',
			width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding - 90,
			valueField: 'id',
			displayField: 'name',
			emptyText: NS.i18n.select_data_element_group,
			editable: false,
			store: dataElementGroupStore,
			loadAvailable: function(reset) {
				var store = dataElementAvailableStore,
					id = this.getValue();

				if (id !== null) {
                    if (reset) {
                        store.reset();
                    }

                    store.loadPage(id, null, false);
				}
			},
			listeners: {
				select: function(cb) {
					cb.loadAvailable(true);

                    dataElementSearch.enable();
				}
			}
		});

		dataElementDetailLevel = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:1px',
			baseBodyCls: 'small',
			queryMode: 'local',
			editable: false,
			valueField: 'id',
			displayField: 'text',
			width: 90 - 1,
			value: dimConf.dataElement.objectName,
			store: {
				fields: ['id', 'text'],
				data: [
					{id: ns.core.conf.finals.dimension.dataElement.objectName, text: NS.i18n.totals},
					{id: ns.core.conf.finals.dimension.operand.objectName, text: NS.i18n.details}
				]
			},
			listeners: {
				select: function(cb) {
					dataElementGroup.loadAvailable(true);
                    dataSelectedStore.removeByProperty('objectName', 'de');
				}
			}
		});

		dataElement = Ext.create('Ext.panel.Panel', {
			xtype: 'panel',
			//title: '<div class="ns-panel-title-data">' + NS.i18n.data_elements + '</div>',
            preventHeader: true,
            hidden: true,
			hideCollapseTool: true,
            bodyStyle: 'border:0 none',
            dimension: dimConf.dataElement.objectName,
			items: [
				{
					xtype: 'container',
					layout: 'column',
					items: [
						dataElementGroup,
						dataElementDetailLevel
					]
				},
				{
					xtype: 'panel',
					layout: 'column',
					bodyStyle: 'border-style:none',
					items: [
                        dataElementAvailable,
						dataElementSelected
					]
				}
			],
			listeners: {
				added: function() {
					//accordionPanels.push(this);
				},
				expand: function(p) {
					//p.onExpand();
				}
			}
		});

        // data set
        dataSetLabel = Ext.create('Ext.form.Label', {
            text: NS.i18n.available,
            cls: 'ns-toolbar-multiselect-left-label',
            style: 'margin-right:5px'
        });

        dataSetSearch = Ext.create('Ext.button.Button', {
            width: 22,
            height: 22,
            cls: 'ns-button-icon',
            style: 'background: url(images/search_14.png) 3px 3px no-repeat',
            showFilter: function() {
                dataSetLabel.hide();
                this.hide();
                dataSetFilter.show();
                dataSetFilter.reset();
            },
            hideFilter: function() {
                dataSetLabel.show();
                this.show();
                dataSetFilter.hide();
                dataSetFilter.reset();
            },
            handler: function() {
                this.showFilter();
            }
        });

        dataSetFilter = Ext.create('Ext.form.field.Trigger', {
            cls: 'ns-trigger-filter',
            emptyText: 'Filter available..',
            height: 22,
            hidden: true,
            enableKeyEvents: true,
            fieldStyle: 'height:22px; border-right:0 none',
            style: 'height:22px',
            onTriggerClick: function() {
				if (this.getValue()) {
					this.reset();
					this.onKeyUpHandler();
				}
            },
            onKeyUpHandler: function() {
                var store = dataSetAvailableStore;
                store.loadPage(this.getValue(), false);
            },
            listeners: {
                keyup: {
                    fn: function(cmp) {
                        cmp.onKeyUpHandler();
                    },
                    buffer: 100
                },
                show: function(cmp) {
                    cmp.focus(false, 50);
                },
                focus: function(cmp) {
                    cmp.addCls('ns-trigger-filter-focused');
                },
                blur: function(cmp) {
                    cmp.removeCls('ns-trigger-filter-focused');
                }
            }
        });

		dataSetAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			store: dataSetAvailableStore,
			tbar: [
				dataSetLabel,
                dataSetSearch,
                dataSetFilter,
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowright.png',
					width: 22,
					handler: function() {
                        if (dataSetAvailable.getValue().length) {
                            var records = dataSetAvailableStore.getRecordsByIds(dataSetAvailable.getValue());
                            dataSelectedStore.addRecords(records, 'ds');
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowrightdouble.png',
					width: 22,
					handler: function() {
						dataSetAvailableStore.loadPage(null, null, true, function() {
                            dataSelectedStore.addRecords(dataSetAvailableStore.getRange(), 'ds');
						});
					}
				}
			],
			listeners: {
				render: function(ms) {
                    var el = Ext.get(ms.boundList.getEl().id + '-listEl').dom;

                    el.addEventListener('scroll', function(e) {
                        if (isScrolled(e) && !dataSetAvailableStore.isPending) {
                            dataSetAvailableStore.loadPage(null, true);
                        }
                    });

					ms.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.addRecords(record, 'ds');
					}, ms);
				}
			}
		});

		dataSetSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			ddReorder: true,
			store: dataSelectedStore,
			tbar: [
				{
					xtype: 'button',
					icon: 'images/arrowleftdouble.png',
					width: 22,
					handler: function() {
                        if (dataSelectedStore.getRange().length) {
                            dataSelectedStore.removeAll();
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
                        if (dataSetSelected.getValue().length) {
                            dataSelectedStore.removeByIds(dataSetSelected.getValue());
                        }
					}
				},
				'->',
				{
					xtype: 'label',
					text: NS.i18n.selected,
					cls: 'ns-toolbar-multiselect-right-label'
				}
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.removeByIds(record.data.id);
					}, this);
				}
			}
		});

		dataSet = Ext.create('Ext.panel.Panel', {
			xtype: 'panel',
			//title: '<div class="ns-panel-title-data">' + NS.i18n.reporting_rates + '</div>',
            preventHeader: true,
            hidden: true,
			hideCollapseTool: true,
            bodyStyle: 'border:0 none',
            dimension: dimConf.dataSet.objectName,
			items: [
				{
					xtype: 'panel',
					layout: 'column',
					bodyStyle: 'border-style:none',
					items: [
						dataSetAvailable,
						dataSetSelected
					]
				}
			],
			listeners: {
				added: function() {
					//accordionPanels.push(this);
				},
				expand: function(p) {
					//p.onExpand();
				}
			}
		});

        // event data item
        onEventDataItemProgramSelect = function(programId, skipSync) {
            if (!skipSync) {
                //dataSelectedStore.removeByProperty('objectName', ['di','pi']);
                programIndicatorProgram.setValue(programId);
                onProgramIndicatorProgramSelect(programId, true);
            }

            var types = ns.core.conf.valueType.tAggregateTypes.join(',');

            Ext.Ajax.request({
                url: ns.core.init.contextPath + '/api/programDataElements.json?program=' + programId + '&filter=valueType:in:[' + types + ']&fields=dimensionItem|rename(id),name,valueType&paging=false',
                disableCaching: false,
                success: function(r) {
                    var elements = Ext.decode(r.responseText).programDataElements,
                        isA = Ext.isArray,
                        isO = Ext.isObject;

                    Ext.Ajax.request({
                        url: ns.core.init.contextPath + '/api/programs.json?filter=id:eq:' + programId + '&filter=programTrackedEntityAttributes.trackedEntityAttribute.confidential:eq:false&filter=programTrackedEntityAttributes.valueType:in:[' + types + ']&fields=programTrackedEntityAttributes[dimensionItem|rename(id),' + namePropertyUrl + '|rename(name),valueType]&paging=false',
                        disableCaching: false,
                        success: function(r) {
                            var attributes = (Ext.decode(r.responseText).programs[0] || {}).programTrackedEntityAttributes || [],
                                data = ns.core.support.prototype.array.sort(Ext.Array.clean([].concat(elements, attributes))) || [];

                            eventDataItemAvailableStore.loadDataAndUpdate(data);
                        }
                    });
                }
            });
        };

		eventDataItemProgram = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin:0 1px 1px 0',
			width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding,
			valueField: 'id',
			displayField: 'name',
			emptyText: NS.i18n.select_program,
			editable: false,
            queryMode: 'local',
			store: programStore,
			listeners: {
				select: function(cb) {
                    onEventDataItemProgramSelect(cb.getValue());
				}
			}
		});

        eventDataItemLabel = Ext.create('Ext.form.Label', {
            text: NS.i18n.available,
            cls: 'ns-toolbar-multiselect-left-label',
            style: 'margin-right:5px'
        });

        eventDataItemSearch = Ext.create('Ext.button.Button', {
            width: 22,
            height: 22,
            cls: 'ns-button-icon',
            //disabled: true,
            style: 'background: url(images/search_14.png) 3px 3px no-repeat',
            showFilter: function() {
                eventDataItemLabel.hide();
                this.hide();
                eventDataItemFilter.show();
                eventDataItemFilter.reset();
            },
            hideFilter: function() {
                eventDataItemLabel.show();
                this.show();
                eventDataItemFilter.hide();
                eventDataItemFilter.reset();
            },
            handler: function() {
                this.showFilter();
            }
        });

        eventDataItemFilter = Ext.create('Ext.form.field.Trigger', {
            cls: 'ns-trigger-filter',
            emptyText: 'Filter available..',
            height: 22,
            hidden: true,
            enableKeyEvents: true,
            fieldStyle: 'height:22px; border-right:0 none',
            style: 'height:22px',
            onTriggerClick: function() {
				if (this.getValue()) {
					this.reset();
					this.onKeyUpHandler();

                    eventDataItemAvailableStore.clearFilter();
				}
            },
            onKeyUpHandler: function() {
                var value = this.getValue() || '',
                    store = eventDataItemAvailableStore,
                    str;

                //if (Ext.isString(value) || Ext.isNumber(value)) {
                    //store.loadPage(null, this.getValue(), false);
                //}

                store.filterBy(function(record) {
                    str = record.data.name || '';

                    return str.toLowerCase().indexOf(value.toLowerCase()) !== -1;
                });
            },
            listeners: {
                keyup: {
                    fn: function(cmp) {
                        cmp.onKeyUpHandler();
                    },
                    buffer: 100
                },
                show: function(cmp) {
                    cmp.focus(false, 50);
                },
                focus: function(cmp) {
                    cmp.addCls('ns-trigger-filter-focused');
                },
                blur: function(cmp) {
                    cmp.removeCls('ns-trigger-filter-focused');
                }
            }
        });

		eventDataItemAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			store: eventDataItemAvailableStore,
			tbar: [
				eventDataItemLabel,
                eventDataItemSearch,
                eventDataItemFilter,
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowright.png',
					width: 22,
					handler: function() {
                        if (eventDataItemAvailable.getValue().length) {
                            var records = eventDataItemAvailableStore.getRecordsByIds(eventDataItemAvailable.getValue());
                            dataSelectedStore.addRecords(records, 'di');
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowrightdouble.png',
					width: 22,
					handler: function() {
						//eventDataItemAvailableStore.loadPage(null, null, null, true, function() {
                            dataSelectedStore.addRecords(eventDataItemAvailableStore.getRange(), 'di');
						//});
					}
				}
			],
			listeners: {
				render: function(ms) {
                    var el = Ext.get(ms.boundList.getEl().id + '-listEl').dom;

                    //el.addEventListener('scroll', function(e) {
                        //if (isScrolled(e) && !eventDataItemAvailableStore.isPending) {
                            //eventDataItemAvailableStore.loadPage(null, null, true);
                        //}
                    //});

					ms.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.addRecords(record, 'di');
					}, ms);
				}
			}
		});

		eventDataItemSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			ddReorder: true,
			store: dataSelectedStore,
			tbar: [
				{
					xtype: 'button',
					icon: 'images/arrowleftdouble.png',
					width: 22,
					handler: function() {
                        if (dataSelectedStore.getRange().length) {
                            dataSelectedStore.removeAll();
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
                        if (eventDataItemSelected.getValue().length) {
                            dataSelectedStore.removeByIds(eventDataItemSelected.getValue());
                        }
					}
				},
				'->',
				{
					xtype: 'label',
					text: NS.i18n.selected,
					cls: 'ns-toolbar-multiselect-right-label'
				}
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.removeByIds(record.data.id);
					}, this);
				}
			}
		});

		eventDataItem = Ext.create('Ext.panel.Panel', {
			xtype: 'panel',
			//title: '<div class="ns-panel-title-data">' + NS.i18n.eventDataItems + '</div>',
            preventHeader: true,
            hidden: true,
			hideCollapseTool: true,
            dimension: dimConf.eventDataItem.objectName,
            bodyStyle: 'border:0 none',
			items: [
				eventDataItemProgram,
				{
					xtype: 'panel',
					layout: 'column',
					bodyStyle: 'border-style:none',
					items: [
                        eventDataItemAvailable,
						eventDataItemSelected
					]
				}
			],
			listeners: {
				added: function() {
					//accordionPanels.push(this);
				},
				expand: function(p) {
					//p.onExpand();
				}
			}
		});

        // program indicator
        onProgramIndicatorProgramSelect = function(programId, skipSync) {
            if (!skipSync) {
                //dataSelectedStore.removeByProperty('objectName', ['di','pi']);
                eventDataItemProgram.setValue(programId);
                onEventDataItemProgramSelect(programId, true);
            }

            Ext.Ajax.request({
                url: ns.core.init.contextPath + '/api/programs.json?filter=id:eq:' + programId + '&fields=programIndicators[dimensionItem|rename(id),' + namePropertyUrl + ']&paging=false',
				disableCaching: false,
				success: function(r) {
                    var indicators = (Ext.decode(r.responseText).programs[0] || {}).programIndicators || [],
                        data = ns.core.support.prototype.array.sort(indicators);

                    programIndicatorAvailableStore.loadDataAndUpdate(data);
                }
            });
        };

		programIndicatorProgram = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin:0 1px 1px 0',
			width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding,
			valueField: 'id',
			displayField: 'name',
			emptyText: NS.i18n.select_program,
			editable: false,
            queryMode: 'local',
			store: programStore,
			listeners: {
				select: function(cb) {
                    onProgramIndicatorProgramSelect(cb.getValue());
				}
			}
		});

        programIndicatorLabel = Ext.create('Ext.form.Label', {
            text: NS.i18n.available,
            cls: 'ns-toolbar-multiselect-left-label',
            style: 'margin-right:5px'
        });

        programIndicatorSearch = Ext.create('Ext.button.Button', {
            width: 22,
            height: 22,
            cls: 'ns-button-icon',
            //disabled: true,
            style: 'background: url(images/search_14.png) 3px 3px no-repeat',
            showFilter: function() {
                programIndicatorLabel.hide();
                this.hide();
                programIndicatorFilter.show();
                programIndicatorFilter.reset();
            },
            hideFilter: function() {
                programIndicatorLabel.show();
                this.show();
                programIndicatorFilter.hide();
                programIndicatorFilter.reset();
            },
            handler: function() {
                this.showFilter();
            }
        });

        programIndicatorFilter = Ext.create('Ext.form.field.Trigger', {
            cls: 'ns-trigger-filter',
            emptyText: 'Filter available..',
            height: 22,
            hidden: true,
            enableKeyEvents: true,
            fieldStyle: 'height:22px; border-right:0 none',
            style: 'height:22px',
            onTriggerClick: function() {
				if (this.getValue()) {
					this.reset();
					this.onKeyUpHandler();

                    programIndicatorAvailableStore.clearFilter();
				}
            },
            onKeyUpHandler: function() {
                var value = this.getValue() || '',
                    store = programIndicatorAvailableStore,
                    str;

                //if (Ext.isString(value) || Ext.isNumber(value)) {
                    //store.loadPage(null, this.getValue(), false);
                //}

                store.filterBy(function(record) {
                    str = record.data.name || '';

                    return str.toLowerCase().indexOf(value.toLowerCase()) !== -1;
                });
            },
            listeners: {
                keyup: {
                    fn: function(cmp) {
                        cmp.onKeyUpHandler();
                    },
                    buffer: 100
                },
                show: function(cmp) {
                    cmp.focus(false, 50);
                },
                focus: function(cmp) {
                    cmp.addCls('ns-trigger-filter-focused');
                },
                blur: function(cmp) {
                    cmp.removeCls('ns-trigger-filter-focused');
                }
            }
        });

		programIndicatorAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			store: programIndicatorAvailableStore,
			tbar: [
				programIndicatorLabel,
                programIndicatorSearch,
                programIndicatorFilter,
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowright.png',
					width: 22,
					handler: function() {
                        if (programIndicatorAvailable.getValue().length) {
                            var records = programIndicatorAvailableStore.getRecordsByIds(programIndicatorAvailable.getValue());
                            dataSelectedStore.addRecords(records, 'pi');
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowrightdouble.png',
					width: 22,
					handler: function() {
						//programIndicatorAvailableStore.loadPage(null, null, null, true, function() {
                            dataSelectedStore.addRecords(programIndicatorAvailableStore.getRange(), 'pi');
						//});
					}
				}
			],
			listeners: {
				render: function(ms) {
                    var el = Ext.get(ms.boundList.getEl().id + '-listEl').dom;

                    //el.addEventListener('scroll', function(e) {
                        //if (isScrolled(e) && !programIndicatorAvailableStore.isPending) {
                            //programIndicatorAvailableStore.loadPage(null, null, true);
                        //}
                    //});

					ms.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.addRecords(record, 'pi');
					}, ms);
				}
			}
		});

		programIndicatorSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			valueField: 'id',
			displayField: 'name',
			ddReorder: true,
			store: dataSelectedStore,
			tbar: [
				{
					xtype: 'button',
					icon: 'images/arrowleftdouble.png',
					width: 22,
					handler: function() {
                        if (dataSelectedStore.getRange().length) {
                            dataSelectedStore.removeAll();
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
                        if (programIndicatorSelected.getValue().length) {
                            dataSelectedStore.removeByIds(programIndicatorSelected.getValue());
                        }
					}
				},
				'->',
				{
					xtype: 'label',
					text: NS.i18n.selected,
					cls: 'ns-toolbar-multiselect-right-label'
				}
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function(bl, record) {
                        dataSelectedStore.removeByIds(record.data.id);
					}, this);
				}
			}
		});

		programIndicator = Ext.create('Ext.panel.Panel', {
			xtype: 'panel',
			//title: '<div class="ns-panel-title-data">' + NS.i18n.programIndicators + '</div>',
            preventHeader: true,
            hidden: true,
			hideCollapseTool: true,
            dimension: dimConf.programIndicator.objectName,
            bodyStyle: 'border:0 none',
			items: [
				programIndicatorProgram,
				{
					xtype: 'panel',
					layout: 'column',
					bodyStyle: 'border-style:none',
					items: [
                        programIndicatorAvailable,
						programIndicatorSelected
					]
				}
			],
			listeners: {
				added: function() {
					//accordionPanels.push(this);
				},
				expand: function(p) {
					//p.onExpand();
				}
			}
		});

        data = {
			xtype: 'panel',
			title: '<div class="ns-panel-title-data">' + NS.i18n.data + '</div>',
			hideCollapseTool: true,
            dimension: dimConf.data.objectName,
            updateStoreFilters: function() {
                indicatorAvailableStore.updateFilter();
                dataElementAvailableStore.updateFilter();
                dataSetAvailableStore.updateFilter();
                eventDataItemAvailableStore.updateFilter();
                programIndicatorAvailableStore.updateFilter();
            },
			getDimension: function() {
				var config = {
					dimension: dimConf.data.objectName,
					items: []
				};

				dataSelectedStore.each( function(r) {
					config.items.push({
						id: r.data.id,
						name: r.data.name
					});
				});

                // TODO program
                if (eventDataItemProgram.getValue() || programIndicatorProgram.getValue()) {
                    config.program = {id: eventDataItemProgram.getValue() || programIndicatorProgram.getValue()};
                }

				return config.items.length ? config : null;
			},
			onExpand: function() {
                var conf = ns.core.conf.layout,
                    h = westRegion.hasScrollbar ? conf.west_scrollbarheight_accordion_indicator : conf.west_maxheight_accordion_indicator;

				accordion.setThisHeight(h);

				ns.core.web.multiSelect.setHeight([indicatorAvailable, indicatorSelected], this, conf.west_fill_accordion_indicator);
                ns.core.web.multiSelect.setHeight([dataElementAvailable, dataElementSelected], this, conf.west_fill_accordion_dataelement);
                ns.core.web.multiSelect.setHeight([dataSetAvailable, dataSetSelected], this, conf.west_fill_accordion_dataset);
                ns.core.web.multiSelect.setHeight([eventDataItemAvailable, eventDataItemSelected], this, conf.west_fill_accordion_eventdataitem);
                ns.core.web.multiSelect.setHeight([programIndicatorAvailable, programIndicatorSelected], this, conf.west_fill_accordion_programindicator);
			},
			items: [
                dataType,
                indicator,
                dataElement,
                dataSet,
                eventDataItem,
                programIndicator
			],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				},
				expand: function(p) {
					p.onExpand();
				}
			}
		};

		// period

		rewind = Ext.create('Ext.form.field.Checkbox', {
			relativePeriodId: 'rewind',
			boxLabel: 'Rewind one period',
			xable: function() {
				this.setDisabled(period.isNoRelativePeriods());
			}
		});

        relativePeriodDefaults = {
            labelSeparator: '',
            style: 'margin-bottom:0',
            listeners: {
                added: function(chb) {
                    if (chb.xtype === 'checkbox') {
                        period.checkboxes.push(chb);
                        relativePeriod.valueComponentMap[chb.relativePeriodId] = chb;
                    }

                    if (chb.relativePeriodId === ns.core.init.systemInfo.analysisRelativePeriod) {
                        chb.setValue(true);
                    }
                }
            }
        };

		relativePeriod = {
			xtype: 'panel',
            layout: 'column',
			hideCollapseTool: true,
			autoScroll: true,
			bodyStyle: 'border:0 none',
			valueComponentMap: {},
			items: [
				{
					xtype: 'container',
                    columnWidth: 0.34,
					bodyStyle: 'border-style:none',
					items: [
						{
							xtype: 'panel',
							//columnWidth: 0.34,
							bodyStyle: 'border-style:none; padding:0 0 0 8px',
							defaults: relativePeriodDefaults,
							items: [
								{
									xtype: 'label',
									text: NS.i18n.weeks,
									cls: 'ns-label-period-heading'
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'THIS_WEEK',
									boxLabel: NS.i18n.this_week
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_WEEK',
									boxLabel: NS.i18n.last_week
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_4_WEEKS',
									boxLabel: NS.i18n.last_4_weeks
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_12_WEEKS',
									boxLabel: NS.i18n.last_12_weeks
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_52_WEEKS',
									boxLabel: NS.i18n.last_52_weeks
								}
							]
						},
						{
							xtype: 'panel',
							//columnWidth: 0.34,
							bodyStyle: 'border-style:none; padding:5px 0 0 8px',
							defaults: relativePeriodDefaults,
							items: [
								{
									xtype: 'label',
									text: NS.i18n.quarters,
									cls: 'ns-label-period-heading'
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'THIS_QUARTER',
									boxLabel: NS.i18n.this_quarter
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_QUARTER',
									boxLabel: NS.i18n.last_quarter
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_4_QUARTERS',
									boxLabel: NS.i18n.last_4_quarters
								}
							]
						},
						{
							xtype: 'panel',
							//columnWidth: 0.35,
							bodyStyle: 'border-style:none; padding:5px 0 0 8px',
							defaults: relativePeriodDefaults,
							items: [
								{
									xtype: 'label',
									text: NS.i18n.years,
									cls: 'ns-label-period-heading'
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'THIS_YEAR',
									boxLabel: NS.i18n.this_year
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_YEAR',
									boxLabel: NS.i18n.last_year
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_5_YEARS',
									boxLabel: NS.i18n.last_5_years
								}
							]
						}
					]
				},
				{
					xtype: 'container',
                    columnWidth: 0.33,
					bodyStyle: 'border-style:none',
					items: [
						{
							xtype: 'panel',
							//columnWidth: 0.33,
							bodyStyle: 'border-style:none',
							defaults: relativePeriodDefaults,
							items: [
								{
									xtype: 'label',
									text: NS.i18n.months,
									cls: 'ns-label-period-heading'
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'THIS_MONTH',
									boxLabel: NS.i18n.this_month
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_MONTH',
									boxLabel: NS.i18n.last_month
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_3_MONTHS',
									boxLabel: NS.i18n.last_3_months
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_6_MONTHS',
									boxLabel: NS.i18n.last_6_months
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_12_MONTHS',
									boxLabel: NS.i18n.last_12_months
								}
							]
						},
						{
							xtype: 'panel',
							//columnWidth: 0.33,
							bodyStyle: 'border-style:none; padding:5px 0 0',
							defaults: relativePeriodDefaults,
							items: [
								{
									xtype: 'label',
									text: NS.i18n.sixmonths,
									cls: 'ns-label-period-heading'
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'THIS_SIX_MONTH',
									boxLabel: NS.i18n.this_sixmonth
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_SIX_MONTH',
									boxLabel: NS.i18n.last_sixmonth
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_2_SIXMONTHS',
									boxLabel: NS.i18n.last_2_sixmonths
								}
							]
						}
					]
				},
				{
					xtype: 'container',
                    columnWidth: 0.33,
					bodyStyle: 'border-style:none',
					items: [
						{
							xtype: 'panel',
							//columnWidth: 0.33,
							bodyStyle: 'border-style:none',
                            style: 'margin-bottom: 32px',
							defaults: relativePeriodDefaults,
							items: [
								{
									xtype: 'label',
									text: NS.i18n.bimonths,
									cls: 'ns-label-period-heading'
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'THIS_BIMONTH',
									boxLabel: NS.i18n.this_bimonth
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_BIMONTH',
									boxLabel: NS.i18n.last_bimonth
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_6_BIMONTHS',
									boxLabel: NS.i18n.last_6_bimonths
								}
							]
						},
						{
							xtype: 'panel',
							//columnWidth: 0.33,
							bodyStyle: 'border-style:none; padding:5px 0 0',
							defaults: relativePeriodDefaults,
							items: [
								{
									xtype: 'label',
									text: NS.i18n.financial_years,
									cls: 'ns-label-period-heading'
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'THIS_FINANCIAL_YEAR',
									boxLabel: NS.i18n.this_financial_year
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_FINANCIAL_YEAR',
									boxLabel: NS.i18n.last_financial_year
								},
								{
									xtype: 'checkbox',
									relativePeriodId: 'LAST_5_FINANCIAL_YEARS',
									boxLabel: NS.i18n.last_5_financial_years
								}
							]
						}
					]
				}
			]
		};

		fixedPeriodAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			height: 180,
			valueField: 'id',
			displayField: 'name',
			store: fixedPeriodAvailableStore,
			tbar: [
				{
					xtype: 'label',
					text: NS.i18n.available,
					cls: 'ns-toolbar-multiselect-left-label'
				},
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowright.png',
					width: 22,
					handler: function() {
						ns.core.web.multiSelect.select(fixedPeriodAvailable, fixedPeriodSelected);
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowrightdouble.png',
					width: 22,
					handler: function() {
						ns.core.web.multiSelect.selectAll(fixedPeriodAvailable, fixedPeriodSelected, true);
					}
				},
				' '
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function() {
						ns.core.web.multiSelect.select(fixedPeriodAvailable, fixedPeriodSelected);
					}, this);
				}
			}
		});

		fixedPeriodSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
			width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
			height: 180,
			valueField: 'id',
			displayField: 'name',
			ddReorder: false,
			store: fixedPeriodSelectedStore,
			tbar: [
				' ',
				{
					xtype: 'button',
					icon: 'images/arrowleftdouble.png',
					width: 22,
					handler: function() {
						ns.core.web.multiSelect.unselectAll(fixedPeriodAvailable, fixedPeriodSelected);
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
						ns.core.web.multiSelect.unselect(fixedPeriodAvailable, fixedPeriodSelected);
					}
				},
				'->',
				{
					xtype: 'label',
					text: NS.i18n.selected,
					cls: 'ns-toolbar-multiselect-right-label'
				}
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function() {
						ns.core.web.multiSelect.unselect(fixedPeriodAvailable, fixedPeriodSelected);
					}, this);
				}
			}
		});

        onPeriodTypeSelect = function() {
            var type = periodType.getValue(),
                periodOffset = periodType.periodOffset,
                generator = ns.core.init.periodGenerator,
                periods = generator.generateReversedPeriods(type, type === 'Yearly' ? periodOffset - 5 : periodOffset);

            for (var i = 0; i < periods.length; i++) {
                periods[i].id = periods[i].iso;
            }

            fixedPeriodAvailableStore.setIndex(periods);
            fixedPeriodAvailableStore.loadData(periods);
            ns.core.web.multiSelect.filterAvailable(fixedPeriodAvailable, fixedPeriodSelected);
        };

        periodType = Ext.create('Ext.form.field.ComboBox', {
            cls: 'ns-combo',
            style: 'margin-bottom:1px',
            width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding - 62 - 62 - 2,
            valueField: 'id',
            displayField: 'name',
            emptyText: NS.i18n.select_period_type,
            editable: false,
            queryMode: 'remote',
            store: periodTypeStore,
            periodOffset: 0,
            listeners: {
                select: function() {
                    periodType.periodOffset = 0;
                    onPeriodTypeSelect();
                }
            }
        });

		period = {
			xtype: 'panel',
			title: '<div class="ns-panel-title-period">Periods</div>',
			hideCollapseTool: true,
			checkboxes: [],
			getDimension: function() {
				var config = {
						dimension: dimConf.period.objectName,
						items: []
					};

				fixedPeriodSelectedStore.each( function(r) {
					config.items.push({
						id: r.data.id,
						name: r.data.name
					});
				});

				for (var i = 0; i < this.checkboxes.length; i++) {
					if (this.checkboxes[i].getValue()) {
						config.items.push({
							id: this.checkboxes[i].relativePeriodId,
							name: ''
						});
					}
				}

				return config.items.length ? config : null;
			},
			onExpand: function() {
				var h = ns.app.westRegion.hasScrollbar ?
					ns.core.conf.layout.west_scrollbarheight_accordion_period : ns.core.conf.layout.west_maxheight_accordion_period;
				accordion.setThisHeight(h);
				ns.core.web.multiSelect.setHeight(
					[fixedPeriodAvailable, fixedPeriodSelected],
					this,
					ns.core.conf.layout.west_fill_accordion_period
				);
			},
			resetRelativePeriods: function() {
				var a = this.checkboxes;
				for (var i = 0; i < a.length; i++) {
					a[i].setValue(false);
				}
			},
			isNoRelativePeriods: function() {
				var a = this.checkboxes;
				for (var i = 0; i < a.length; i++) {
					if (a[i].getValue()) {
						return false;
					}
				}
				return true;
			},
			items: [
				{
					xtype: 'panel',
					layout: 'column',
					bodyStyle: 'border-style:none',
					style: 'margin-top:0px',
					items: [
                        periodType,
						{
							xtype: 'button',
							text: NS.i18n.prev_year,
							style: 'margin-left:1px; border-radius:2px',
							height: 24,
                            width: 62,
							handler: function() {
								if (periodType.getValue()) {
									periodType.periodOffset--;
                                    onPeriodTypeSelect();
								}
							}
						},
						{
							xtype: 'button',
							text: NS.i18n.next_year,
							style: 'margin-left:1px; border-radius:2px',
							height: 24,
                            width: 62,
							handler: function() {
								if (periodType.getValue()) {
									periodType.periodOffset++;
                                    onPeriodTypeSelect();
								}
							}
						}
					]
				},
				{
					xtype: 'panel',
					layout: 'column',
					bodyStyle: 'border-style:none; padding-bottom:2px',
					items: [
						fixedPeriodAvailable,
						fixedPeriodSelected
					]
				},
				relativePeriod
			],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				},
				expand: function(p) {
					p.onExpand();
				}
			}
		};

		// organisation unit

		treePanel = Ext.create('Ext.tree.Panel', {
			cls: 'ns-tree',
			style: 'border-top: 1px solid #ddd; padding-top: 1px',
			width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding,
			displayField: 'name',
			rootVisible: false,
			autoScroll: true,
			multiSelect: true,
			rendered: false,
			reset: function() {
				var rootNode = this.getRootNode().findChild('id', ns.core.init.rootNodes[0].id);
				this.collapseAll();
				this.expandPath(rootNode.getPath());
				this.getSelectionModel().select(rootNode);
			},
			selectRootIf: function() {
				if (this.getSelectionModel().getSelection().length < 1) {
					var node = this.getRootNode().findChild('id', ns.core.init.rootNodes[0].id);
					if (this.rendered) {
						this.getSelectionModel().select(node);
					}
					return node;
				}
			},
			isPending: false,
			recordsToSelect: [],
			recordsToRestore: [],
			multipleSelectIf: function(map, doUpdate) {
                this.recordsToSelect = Ext.Array.clean(this.recordsToSelect);

				if (this.recordsToSelect.length === ns.core.support.prototype.object.getLength(map)) {
					this.getSelectionModel().select(this.recordsToSelect);
					this.recordsToSelect = [];
					this.isPending = false;

					if (doUpdate) {
						update();
					}
				}
			},
			multipleExpand: function(id, map, doUpdate) {
				var that = this,
					rootId = ns.core.conf.finals.root.id,
					path = map[id];

				if (path.substr(0, rootId.length + 1) !== ('/' + rootId)) {
					path = '/' + rootId + path;
				}

				that.expandPath(path, 'id', '/', function() {
					record = Ext.clone(that.getRootNode().findChild('id', id, true));
					that.recordsToSelect.push(record);
					that.multipleSelectIf(map, doUpdate);
				});
			},
            select: function(url, params) {
                if (!params) {
                    params = {};
                }
                Ext.Ajax.request({
                    url: url,
                    method: 'GET',
                    params: params,
                    scope: this,
                    success: function(r) {
                        var a = Ext.decode(r.responseText).organisationUnits;
                        this.numberOfRecords = a.length;
                        for (var i = 0; i < a.length; i++) {
                            this.multipleExpand(a[i].id, a[i].path);
                        }
                    }
                });
            },
			getParentGraphMap: function() {
				var selection = this.getSelectionModel().getSelection(),
					map = {};

				if (Ext.isArray(selection) && selection.length) {
					for (var i = 0, pathArray, key; i < selection.length; i++) {
						pathArray = selection[i].getPath().split('/');
						map[pathArray.pop()] = pathArray.join('/');
					}
				}

				return map;
			},
			selectGraphMap: function(map, update) {
				if (!ns.core.support.prototype.object.getLength(map)) {
					return;
				}

				this.isPending = true;

				for (var key in map) {
					if (map.hasOwnProperty(key)) {
						treePanel.multipleExpand(key, map, update);
					}
				}
			},
			store: Ext.create('Ext.data.TreeStore', {
				fields: ['id', 'name', 'hasChildren'],
				proxy: {
					type: 'rest',
					format: 'json',
					noCache: false,
					extraParams: {
						fields: 'children[id,' + namePropertyUrl + ',children::isNotEmpty|rename(hasChildren)&paging=false'
					},
					url: ns.core.init.contextPath + '/api/organisationUnits',
					reader: {
						type: 'json',
						root: 'children'
					},
					sortParam: false
				},
				sorters: [{
					property: 'name',
					direction: 'ASC'
				}],
				root: {
					id: ns.core.conf.finals.root.id,
					expanded: true,
					children: ns.core.init.rootNodes
				},
				listeners: {
					load: function(store, node, records) {
						Ext.Array.each(records, function(record) {
                            if (Ext.isBoolean(record.data.hasChildren)) {
                                record.set('leaf', !record.data.hasChildren);
                            }
                        });
					}
				}
			}),
			xable: function(values) {
				for (var i = 0; i < values.length; i++) {
					if (!!values[i]) {
						this.disable();
						return;
					}
				}

				this.enable();
			},
			listeners: {
				beforeitemexpand: function() {
					if (!treePanel.isPending) {
						treePanel.recordsToRestore = treePanel.getSelectionModel().getSelection();
					}
				},
				itemexpand: function() {
					if (!treePanel.isPending && treePanel.recordsToRestore.length) {
						treePanel.getSelectionModel().select(treePanel.recordsToRestore);
						treePanel.recordsToRestore = [];
					}
				},
				render: function() {
					this.rendered = true;
				},
				afterrender: function() {
					this.getSelectionModel().select(0);
				},
				itemcontextmenu: function(v, r, h, i, e) {
					v.getSelectionModel().select(r, false);

					if (v.menu) {
						v.menu.destroy();
					}
					v.menu = Ext.create('Ext.menu.Menu', {
						id: 'treepanel-contextmenu',
						showSeparator: false,
						shadow: false
					});
					if (!r.data.leaf) {
						v.menu.add({
							id: 'treepanel-contextmenu-item',
							text: NS.i18n.select_sub_units,
							icon: 'images/node-select-child.png',
							handler: function() {
								r.expand(false, function() {
									v.getSelectionModel().select(r.childNodes, true);
									v.getSelectionModel().deselect(r);
								});
							}
						});
					}
					else {
						return;
					}

					v.menu.showAt(e.xy);
				}
			}
		});

		userOrganisationUnit = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.25,
			style: 'padding-top: 3px; padding-left: 5px; margin-bottom: 0',
			boxLabel: NS.i18n.user_organisation_unit,
			labelWidth: ns.core.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnitChildren.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.26,
			style: 'padding-top: 3px; margin-bottom: 0',
			boxLabel: NS.i18n.user_sub_units,
			labelWidth: ns.core.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitGrandChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.4,
			style: 'padding-top: 3px; margin-bottom: 0',
			boxLabel: NS.i18n.user_sub_x2_units,
			labelWidth: ns.core.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitChildren.getValue()]);
			}
		});

		organisationUnitLevel = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding - 37,
			valueField: 'level',
			displayField: 'name',
			emptyText: NS.i18n.select_organisation_unit_levels,
			editable: false,
			hidden: true,
			store: organisationUnitLevelStore
		});

		organisationUnitGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding - 37,
			valueField: 'id',
			displayField: 'name',
			emptyText: NS.i18n.select_organisation_unit_groups,
			editable: false,
			hidden: true,
			store: organisationUnitGroupStore
		});

		toolMenu = Ext.create('Ext.menu.Menu', {
			shadow: false,
			showSeparator: false,
			menuValue: 'orgunit',
			clickHandler: function(param) {
				if (!param) {
					return;
				}

				var items = this.items.items;
				this.menuValue = param;

				// Menu item icon cls
				for (var i = 0; i < items.length; i++) {
					if (items[i].setIconCls) {
						if (items[i].param === param) {
							items[i].setIconCls('ns-menu-item-selected');
						}
						else {
							items[i].setIconCls('ns-menu-item-unselected');
						}
					}
				}

				// Gui
				if (param === 'orgunit') {
					userOrganisationUnit.show();
					userOrganisationUnitChildren.show();
					userOrganisationUnitGrandChildren.show();
					organisationUnitLevel.hide();
					organisationUnitGroup.hide();

					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue()) {
						treePanel.disable();
					}
				}
				else if (param === 'level') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.show();
					organisationUnitGroup.hide();
					treePanel.enable();
				}
				else if (param === 'group') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.hide();
					organisationUnitGroup.show();
					treePanel.enable();
				}
			},
			items: [
				{
					xtype: 'label',
					text: 'Selection mode',
					style: 'padding:7px 5px 5px 7px; font-weight:bold; border:0 none'
				},
				{
					text: NS.i18n.select_organisation_units + '&nbsp;&nbsp;',
					param: 'orgunit',
					iconCls: 'ns-menu-item-selected'
				},
				{
					text: 'Select levels' + '&nbsp;&nbsp;',
					param: 'level',
					iconCls: 'ns-menu-item-unselected'
				},
				{
					text: 'Select groups' + '&nbsp;&nbsp;',
					param: 'group',
					iconCls: 'ns-menu-item-unselected'
				}
			],
			listeners: {
				afterrender: function() {
					this.getEl().addCls('ns-btn-menu');
				},
				click: function(menu, item) {
					this.clickHandler(item.param);
				}
			}
		});

		tool = Ext.create('Ext.button.Button', {
			cls: 'ns-button-organisationunitselection',
			iconCls: 'ns-button-icon-gear',
			width: 36,
			height: 24,
			menu: toolMenu
		});

		toolPanel = Ext.create('Ext.panel.Panel', {
			width: 36,
			bodyStyle: 'border:0 none; text-align:right',
			style: 'margin-right:1px',
			items: tool
		});

		organisationUnit = {
			xtype: 'panel',
			title: '<div class="ns-panel-title-organisationunit">' + NS.i18n.organisation_units + '</div>',
			bodyStyle: 'padding:1px',
			hideCollapseTool: true,
			collapsed: false,
			getDimension: function() {
				var r = treePanel.getSelectionModel().getSelection(),
					config = {
						dimension: ns.core.conf.finals.dimension.organisationUnit.objectName,
						items: []
					};

				if (toolMenu.menuValue === 'orgunit') {
					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue() || userOrganisationUnitGrandChildren.getValue()) {
						if (userOrganisationUnit.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT',
								name: ''
							});
						}
						if (userOrganisationUnitChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_CHILDREN',
								name: ''
							});
						}
						if (userOrganisationUnitGrandChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_GRANDCHILDREN',
								name: ''
							});
						}
					}
					else {
						for (var i = 0; i < r.length; i++) {
							config.items.push({id: r[i].data.id});
						}
					}
				}
				else if (toolMenu.menuValue === 'level') {
					var levels = organisationUnitLevel.getValue();

					for (var i = 0; i < levels.length; i++) {
						config.items.push({
							id: 'LEVEL-' + levels[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}
				else if (toolMenu.menuValue === 'group') {
					var groupIds = organisationUnitGroup.getValue();

					for (var i = 0; i < groupIds.length; i++) {
						config.items.push({
							id: 'OU_GROUP-' + groupIds[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}

				return config.items.length ? config : null;
			},
            onExpand: function() {
                var h = ns.app.westRegion.hasScrollbar ?
                    ns.core.conf.layout.west_scrollbarheight_accordion_organisationunit : ns.core.conf.layout.west_maxheight_accordion_organisationunit;
                accordion.setThisHeight(h);
                treePanel.setHeight(this.getHeight() - ns.core.conf.layout.west_fill_accordion_organisationunit);
            },
            items: [
                {
                    layout: 'column',
                    bodyStyle: 'border:0 none',
                    style: 'padding-bottom:1px',
                    items: [
                        toolPanel,
                        {
                            width: ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding - 37,
                            layout: 'column',
                            bodyStyle: 'border:0 none',
                            items: [
                                userOrganisationUnit,
                                userOrganisationUnitChildren,
                                userOrganisationUnitGrandChildren,
                                organisationUnitLevel,
                                organisationUnitGroup
                            ]
                        }
                    ]
                },
                treePanel
            ],
            listeners: {
                added: function() {
                    accordionPanels.push(this);
                },
                expand: function(p) {
                    p.onExpand();
                }
            }
        };

		// dimensions

		getDimensionPanel = function(dimension, iconCls) {
			var	onSelect,
                availableStore,
				selectedStore,
				dataLabel,
				dataSearch,
				dataFilter,
                selectedAll,
				available,
				selected,
                onSelectAll,
				panel,

				createPanel,
				getPanels;

            onSelect = function() {
                var win = ns.app.layoutWindow;

                if (selectedStore.getRange().length || selectedAll.getValue()) {
                    win.addDimension({id: dimension.id, name: dimension.name});
                }
                else if (win.hasDimension(dimension.id)) {
                    win.removeDimension(dimension.id);
                }
            };

			availableStore = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				lastPage: null,
				nextPage: 1,
				isPending: false,
				isLoaded: false,
				reset: function() {
					this.removeAll();
					this.lastPage = null;
					this.nextPage = 1;
					this.isPending = false;
					dataSearch.hideFilter();
				},
                storage: {},
                addToStorage: function(dimensionId, filter, data) {
                    filter = 'cache_' + (Ext.isString(filter) || Ext.isNumber(filter) ? filter : '');

                    if (!dimensionId) {
                        return;
                    }

                    if (!this.storage.hasOwnProperty(dimensionId)) {
                        this.storage[dimensionId] = {};
                    }

                    if (!this.storage[dimensionId][filter]) {
                        this.storage[dimensionId][filter] = data;
                    }
                },
                getFromStorage: function(dimensionId, filter) {
                    filter = 'cache_' + (Ext.isString(filter) || Ext.isNumber(filter) ? filter : '');

                    if (this.storage.hasOwnProperty(dimensionId)) {
                        if (this.storage[dimensionId].hasOwnProperty(filter)) {
                            return this.storage[dimensionId][filter];
                        }
                    }

                    return;
                },
				loadPage: function(filter, append, noPaging, fn) {
					var store = this,
						params = {},
						path,
                        cacheData;

					filter = filter || dataFilter.getValue() || null;

                    // check session cache
                    cacheData = store.getFromStorage(dimension.id, filter);

                    if (!append && cacheData) {
                        store.loadStore(cacheData, {}, append, fn);
                    }
                    else {
						if (!append) {
							this.lastPage = null;
							this.nextPage = 1;
						}

						if (store.nextPage === store.lastPage) {
							return;
						}

						path = '/dimensions/' + dimension.id + '/items.json?fields=id,' + namePropertyUrl + (filter ? '&filter=' + nameProperty + ':ilike:' + filter : '');

						if (noPaging) {
							params.paging = false;
						}
						else {
							params.page = store.nextPage;
							params.pageSize = 50;
						}

						store.isPending = true;
						ns.core.web.mask.show(available.boundList);

						Ext.Ajax.request({
							url: ns.core.init.contextPath + '/api' + path,
							params: params,
							success: function(r) {
								var response = Ext.decode(r.responseText),
									data = response.items || [],
									pager = response.pager;

                                // add to session cache
                                store.addToStorage(dimension.id, filter, data);

								store.loadStore(data, pager, append, fn);
							},
							callback: function() {
								store.isPending = false;
								ns.core.web.mask.hide(available.boundList);
							}
						});
					}
				},
				loadStore: function(data, pager, append, fn) {
					pager = pager || {};

					this.loadData(data, append);
					this.lastPage = this.nextPage;

					if (pager.pageCount > this.nextPage) {
						this.nextPage++;
					}

					this.isPending = false;

					ns.core.web.multiSelect.filterAvailable({store: availableStore}, {store: selectedStore});

					if (fn) {
						fn();
					}
				},
				sortStore: function() {
					this.sort('name', 'ASC');
				}
			});

			selectedStore = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				data: [],
                listeners: {
                    add: function() {
                        onSelect();
                    },
                    remove: function() {
                        onSelect();
                    },
                    clear: function() {
                        onSelect();
                    }
                }
			});

			dataLabel = Ext.create('Ext.form.Label', {
				text: NS.i18n.available,
				cls: 'ns-toolbar-multiselect-left-label',
				style: 'margin-right:5px'
			});

			dataSearch = Ext.create('Ext.button.Button', {
				width: 22,
				height: 22,
				cls: 'ns-button-icon',
				style: 'background: url(images/search_14.png) 3px 3px no-repeat',
				showFilter: function() {
					dataLabel.hide();
					this.hide();
					dataFilter.show();
					dataFilter.reset();
				},
				hideFilter: function() {
					dataLabel.show();
					this.show();
					dataFilter.hide();
					dataFilter.reset();
				},
				handler: function() {
					this.showFilter();
				}
			});

			dataFilter = Ext.create('Ext.form.field.Trigger', {
				cls: 'ns-trigger-filter',
				emptyText: 'Filter available..',
				height: 22,
				hidden: true,
				enableKeyEvents: true,
				fieldStyle: 'height:22px; border-right:0 none',
				style: 'height:22px',
				onTriggerClick: function() {
					if (this.getValue()) {
						this.reset();
						this.onKeyUpHandler();
					}
				},
				onKeyUpHandler: function() {
					var value = this.getValue(),
						store = availableStore;

					if (Ext.isString(value) || Ext.isNumber(value)) {
						store.loadPage(value, false, true);
					}
				},
				listeners: {
					keyup: {
						fn: function(cmp) {
							cmp.onKeyUpHandler();
						},
						buffer: 100
					},
					show: function(cmp) {
						cmp.focus(false, 50);
					},
					focus: function(cmp) {
						cmp.addCls('ns-trigger-filter-focused');
					},
					blur: function(cmp) {
						cmp.removeCls('ns-trigger-filter-focused');
					}
				}
			});

            selectedAll = Ext.create('Ext.form.field.Checkbox', {
                cls: 'ns-checkbox',
                style: 'margin-left: 2px; margin-right: 5px',
                boxLabel: 'All',
                listeners: {
                    change: function(chb, newVal) {
                        onSelectAll(newVal);
                    }
                }
            });

			available = Ext.create('Ext.ux.form.MultiSelect', {
				cls: 'ns-toolbar-multiselect-left',
				width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
				valueField: 'id',
				displayField: 'name',
				store: availableStore,
				tbar: [
                    dataLabel,
                    dataSearch,
                    dataFilter,
					'->',
					{
						xtype: 'button',
						icon: 'images/arrowright.png',
						width: 22,
						handler: function() {
							ns.core.web.multiSelect.select(available, selected);
						}
					},
					{
						xtype: 'button',
						icon: 'images/arrowrightdouble.png',
						width: 22,
						handler: function() {
							availableStore.loadPage(null, null, true, function() {
								ns.core.web.multiSelect.selectAll(available, selected);
							});
						}
					}
				],
				listeners: {
					render: function(ms) {
						var el = Ext.get(ms.boundList.getEl().id + '-listEl').dom;

						el.addEventListener('scroll', function(e) {
							if (isScrolled(e) && !availableStore.isPending) {
								availableStore.loadPage(null, true);
							}
						});

						ms.boundList.on('itemdblclick', function() {
							ns.core.web.multiSelect.select(available, selected);
						}, ms);
					}
				}
			});

			selected = Ext.create('Ext.ux.form.MultiSelect', {
				cls: 'ns-toolbar-multiselect-right',
				width: (ns.core.conf.layout.west_fieldset_width - ns.core.conf.layout.west_width_padding) / 2,
				valueField: 'id',
				displayField: 'name',
				ddReorder: true,
				store: selectedStore,
				tbar: [
					{
						xtype: 'button',
						icon: 'images/arrowleftdouble.png',
						width: 22,
						handler: function() {
							ns.core.web.multiSelect.unselectAll(available, selected);
						}
					},
					{
						xtype: 'button',
						icon: 'images/arrowleft.png',
						width: 22,
						handler: function() {
							ns.core.web.multiSelect.unselect(available, selected);
						}
					},
					'->',
					{
						xtype: 'label',
						text: NS.i18n.selected,
						cls: 'ns-toolbar-multiselect-right-label'
					},
                    selectedAll
				],
				listeners: {
					afterrender: function() {
						this.boundList.on('itemdblclick', function() {
							ns.core.web.multiSelect.unselect(available, selected);
						}, this);
					}
				}
			});

            onSelectAll = function(value) {
                if (available.boundList && selected.boundList) {
                    if (value) {
                        available.boundList.disable();
                        selected.boundList.disable();
                    }
                    else {
                        available.boundList.enable();
                        selected.boundList.enable();
                    }
                }

                onSelect();
            };

			//availableStore.on('load', function() {
				//ns.core.web.multiSelect.filterAvailable(available, selected);
			//});

			panel = {
				xtype: 'panel',
				title: '<div class="' + iconCls + '">' + dimension.name + '</div>',
				hideCollapseTool: true,
                dimension: dimension.id,
				availableStore: availableStore,
				selectedStore: selectedStore,
                selectedAll: selectedAll,
				getDimension: function() {
					var config = {};

                    if (dimension.id) {
						config.dimension = dimension.id;
                    }

                    if (selectedStore.getRange().length) {
                        config.items = [];

                        selectedStore.each( function(r) {
                            config.items.push({id: r.data.id});
                        });
                    }

					return config.dimension ? config : null;
				},
				onExpand: function() {

                    // load items
					if (!availableStore.isLoaded) {
						availableStore.loadPage();
					}

                    // enable/disable ui
                    if (selectedAll.getValue()) {
                        available.boundList.disable();
                        selected.boundList.disable();
                    }

                    // set height
					var h = ns.app.westRegion.hasScrollbar ?
						ns.core.conf.layout.west_scrollbarheight_accordion_group : ns.core.conf.layout.west_maxheight_accordion_group;
					accordion.setThisHeight(h);
					ns.core.web.multiSelect.setHeight(
						[available, selected],
						this,
						ns.core.conf.layout.west_fill_accordion_group
					);
				},
				items: [
					{
						xtype: 'panel',
						layout: 'column',
						bodyStyle: 'border-style:none',
						items: [
							available,
							selected
						]
					}
				],
				listeners: {
					added: function() {
						accordionPanels.push(this);
					},
					expand: function(p) {
						p.onExpand();
					}
				}
			};

			return panel;
		};

		getDimensionPanels = function(dimensions, iconCls) {
			var panels = [];

			for (var i = 0, panel; i < dimensions.length; i++) {
                panels.push(getDimensionPanel(dimensions[i], iconCls));
			}

			return panels;
		};

		// viewport

		update = function() {
			var config = ns.core.web.chart.getLayoutConfig(),
				layout = ns.core.api.layout.Layout(config);

			if (!layout) {
				return;
			}

			ns.core.web.chart.getData(layout, false);
		};

		accordionBody = Ext.create('Ext.panel.Panel', {
			layout: 'accordion',
			activeOnTop: true,
			cls: 'ns-accordion',
			bodyStyle: 'border:0 none; margin-bottom:2px',
			height: 700,
			items: function() {
				var panels = [
                    data,
					period,
					organisationUnit
				],
				dims = Ext.clone(ns.core.init.dimensions),
                dimPanels = getDimensionPanels(dims, 'ns-panel-title-dimension');

                // idPanelMap
                for (var i = 0, dimPanel; i < dimPanels.length; i++) {
                    dimPanel = dimPanels[i];

                    dimensionPanelMap[dimPanel.dimension] = dimPanel;
                }

                // panels
				panels = panels.concat(dimPanels);

				last = panels[panels.length - 1];
				last.cls = 'ns-accordion-last';

				return panels;
			}()
		});

		accordion = Ext.create('Ext.panel.Panel', {
			bodyStyle: 'border-style:none; padding:1px; padding-bottom:0; overflow-y:scroll;',
			items: accordionBody,
			panels: accordionPanels,
			setThisHeight: function(mx) {
				var settingsHeight = 46,
					panelHeight = settingsHeight + this.panels.length * 28,
					height;

				if (westRegion.hasScrollbar) {
					height = panelHeight + mx;
					this.setHeight(viewport.getHeight() - settingsHeight - 2);
					accordionBody.setHeight(height - settingsHeight - 2);
				}
				else {
					height = westRegion.getHeight() - ns.core.conf.layout.west_fill - settingsHeight;
					mx += panelHeight;
					accordion.setHeight((height > mx ? mx : height) - 2);
					accordionBody.setHeight((height > mx ? mx : height) - 2);
				}
			},
			getExpandedPanel: function() {
				for (var i = 0, panel; i < this.panels.length; i++) {
					if (!this.panels[i].collapsed) {
						return this.panels[i];
					}
				}

				return null;
			},
			getFirstPanel: function() {
				return this.panels[0];
			},
			listeners: {
				added: function() {
					ns.app.accordion = this;
				}
			}
		});

		westRegion = Ext.create('Ext.panel.Panel', {
			region: 'west',
			preventHeader: true,
			collapsible: true,
			collapseMode: 'mini',
			width: function() {
				if (Ext.isWebKit) {
					return ns.core.conf.layout.west_width + 8;
				}
				else {
					if (Ext.isLinux && Ext.isGecko) {
						return ns.core.conf.layout.west_width + 13;
					}
					return ns.core.conf.layout.west_width + 17;
				}
			}(),
			items: [
                chartType,
                accordion
			],
			listeners: {
				added: function() {
					ns.app.westRegion = this;
				}
			}
		});

		layoutButton = Ext.create('Ext.button.Button', {
			text: 'Layout',
			menu: {},
			handler: function() {
				if (!ns.app.layoutWindow) {
					ns.app.layoutWindow = LayoutWindow();
				}

				ns.app.layoutWindow.show();
			},
			listeners: {
				added: function() {
					ns.app.layoutButton = this;
				}
			}
		});

		optionsButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.options,
			menu: {},
			handler: function() {
				if (!ns.app.optionsWindow) {
					ns.app.optionsWindow = OptionsWindow();
				}

				ns.app.optionsWindow.show();
			},
			listeners: {
				added: function() {
					ns.app.optionsButton = this;
				}
			}
		});

		favoriteButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.favorites,
			menu: {},
			handler: function() {
				if (ns.app.favoriteWindow) {
					ns.app.favoriteWindow.destroy();
					ns.app.favoriteWindow = null;
				}

				ns.app.favoriteWindow = FavoriteWindow();
				ns.app.favoriteWindow.show();
			},
			listeners: {
				added: function() {
					ns.app.favoriteButton = this;
				}
			}
		});

		getParamString = function() {
			var paramString = ns.core.web.analytics.getParamString(ns.core.service.layout.getExtendedLayout(ns.app.layout));

			//if (ns.app.layout.showHierarchy) {
				//paramString += '&showHierarchy=true';
			//}

			return paramString;
		};

		openTableLayoutTab = function(type, isNewTab) {
			if (ns.core.init.contextPath && ns.app.paramString) {
				var url = ns.core.init.contextPath + '/api/analytics.' + type + ns.core.web.analytics.getParamString(ns.app.xLayout);
				url += '&tableLayout=true&columns=' + ns.app.xLayout.columnDimensionNames.join(';') + '&rows=' + ns.app.xLayout.rowDimensionNames.join(';');

				window.open(url, isNewTab ? '_blank' : '_top');
			}
		};

        openPlainDataSource = function(url, isNewTab) {
            if (url) {
                if (ns.core.init.contextPath && ns.app.paramString) {
                    window.open(url, isNewTab ? '_blank' : '_top');
                }
            }
        };

		downloadButton = Ext.create('Ext.button.Button', {
            text: NS.i18n.download,
            disabled: true,
			menu: {},
            handler: function(b) {
                b.menu = Ext.create('Ext.menu.Menu', {
                    closeAction: 'destroy',
                    //cls: 'ns-menu',
                    shadow: false,
                    showSeparator: false,
                    items: function() {
                        var items = [
                            {
                                xtype: 'label',
                                text: NS.i18n.graphics,
                                style: 'padding:7px 5px 5px 7px; font-weight:bold'
                            },
                            {
                                text: NS.i18n.image_png + ' (.png)',
                                iconCls: 'ns-menu-item-image',
                                handler: function() {
                                    ns.core.support.svg.submitForm('png');
                                }
                            },
                            {
                                text: 'PDF (.pdf)',
                                iconCls: 'ns-menu-item-image',
                                handler: function() {
                                    ns.core.support.svg.submitForm('pdf');
                                }
                            },
                            {
                                xtype: 'label',
                                text: NS.i18n.plain_data_sources,
                                style: 'padding:7px 5px 5px 7px; font-weight:bold'
                            },
                            {
                                text: 'JSON',
                                iconCls: 'ns-menu-item-datasource',
                                handler: function() {
                                    openPlainDataSource(ns.core.init.contextPath + '/api/analytics.json' + getParamString(), true);
                                },
                                menu: [
                                    {
                                        xtype: 'label',
                                        text: NS.i18n.metadata_id_scheme,
                                        style: 'padding:7px 18px 5px 7px; font-weight:bold; color:#333'
                                    },
                                    {
                                        text: 'ID',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.json' + getParamString() + '&outputIdScheme=UID', true);
                                        }
                                    },
                                    {
                                        text: 'Code',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.json' + getParamString() + '&outputIdScheme=CODE', true);
                                        }
                                    },
                                    {
                                        text: 'Name',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.json' + getParamString() + '&outputIdScheme=NAME', true);
                                        }
                                    }
                                ]
                            },
                            {
                                text: 'XML',
                                iconCls: 'ns-menu-item-datasource',
                                handler: function() {
                                    openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xml' + getParamString(), true);
                                },
                                menu: [
                                    {
                                        xtype: 'label',
                                        text: NS.i18n.metadata_id_scheme,
                                        style: 'padding:7px 18px 5px 7px; font-weight:bold; color:#333'
                                    },
                                    {
                                        text: 'ID',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xml' + getParamString() + '&outputIdScheme=UID', true);
                                        }
                                    },
                                    {
                                        text: 'Code',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xml' + getParamString() + '&outputIdScheme=CODE', true);
                                        }
                                    },
                                    {
                                        text: 'Name',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xml' + getParamString() + '&outputIdScheme=NAME', true);
                                        }
                                    }
                                ]
                            },
                            {
                                text: 'Microsoft Excel',
                                iconCls: 'ns-menu-item-datasource',
                                handler: function() {
                                    openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xls' + getParamString());
                                },
                                menu: [
                                    {
                                        xtype: 'label',
                                        text: NS.i18n.metadata_id_scheme,
                                        style: 'padding:7px 18px 5px 7px; font-weight:bold; color:#333'
                                    },
                                    {
                                        text: 'ID',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xls' + getParamString() + '&outputIdScheme=UID');
                                        }
                                    },
                                    {
                                        text: 'Code',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xls' + getParamString() + '&outputIdScheme=CODE');
                                        }
                                    },
                                    {
                                        text: 'Name',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.xls' + getParamString() + '&outputIdScheme=NAME');
                                        }
                                    }
                                ]
                            },
                            {
                                text: 'CSV',
                                iconCls: 'ns-menu-item-datasource',
                                handler: function() {
                                    openPlainDataSource(ns.core.init.contextPath + '/api/analytics.csv' + getParamString());
                                },
                                menu: [
                                    {
                                        xtype: 'label',
                                        text: NS.i18n.metadata_id_scheme,
                                        style: 'padding:7px 18px 5px 7px; font-weight:bold; color:#333'
                                    },
                                    {
                                        text: 'ID',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.csv' + getParamString() + '&outputIdScheme=UID');
                                        }
                                    },
                                    {
                                        text: 'Code',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.csv' + getParamString() + '&outputIdScheme=CODE');
                                        }
                                    },
                                    {
                                        text: 'Name',
                                        iconCls: 'ns-menu-item-scheme',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.csv' + getParamString() + '&outputIdScheme=NAME');
                                        }
                                    }
                                ]
                            },
                            {
                                text: 'Advanced',
                                iconCls: 'ns-menu-item-advanced',
                                menu: [
                                    {
                                        text: 'JRXML',
                                        iconCls: 'ns-menu-item-datasource',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics.jrxml' + getParamString(), true);
                                        }
                                    },
                                    {
                                        text: 'Raw data SQL',
                                        iconCls: 'ns-menu-item-datasource',
                                        handler: function() {
                                            openPlainDataSource(ns.core.init.contextPath + '/api/analytics/debug/sql' + getParamString(), true);
                                        }
                                    }
                                ]
                            }
                        ];

                        return items;
                    }(),
                    listeners: {
                        added: function() {
                            ns.app.downloadButton = this;
                        },
                        show: function() {
                            ns.core.web.window.setAnchorPosition(b.menu, b);
                        },
                        hide: function() {
                            b.menu.destroy();
                        },
                        destroy: function(m) {
                            b.menu = null;
                        }
                    }
                });

                this.menu.show();
			}
		});

		interpretationItem = Ext.create('Ext.menu.Item', {
			text: 'Write interpretation' + '&nbsp;&nbsp;',
			iconCls: 'ns-menu-item-tablelayout',
			disabled: true,
			xable: function() {
				if (ns.app.layout.id) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
			handler: function() {
				if (ns.app.interpretationWindow) {
					ns.app.interpretationWindow.destroy();
					ns.app.interpretationWindow = null;
				}

				ns.app.interpretationWindow = InterpretationWindow();
				ns.app.interpretationWindow.show();
			}
		});

		pluginItem = Ext.create('Ext.menu.Item', {
			text: 'Embed in web page' + '&nbsp;&nbsp;',
			iconCls: 'ns-menu-item-datasource',
			disabled: true,
			xable: function() {
				if (ns.app.layout) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
			handler: function() {
				var textArea,
					window,
					text = '',
                    version = 'v' + parseFloat(ns.core.init.systemInfo.version.split('.').join(''));

				text += '<html>\n<head>\n';
				text += '<link rel="stylesheet" href="http://dhis2-cdn.org/' + version + '/ext/resources/css/ext-plugin-gray.css" />\n';
				text += '<script src="http://dhis2-cdn.org/' + version + '/ext/ext-all.js"></script>\n';
				text += '<script src="http://dhis2-cdn.org/' + version + '/plugin/chart.js"></script>\n';
				text += '</head>\n\n<body>\n';
				text += '<div id="chart1" style="width:700px; height:400px"></div>\n\n';
				text += '<script>\n\n';
				text += 'Ext.onReady(function() {\n\n';
				text += 'DHIS.getChart(' + JSON.stringify(ns.core.service.layout.layout2plugin(ns.app.layout, 'chart1'), null, 2) + ');\n\n';
				text += '});\n\n';
				text += '</script>\n\n';
				text += '</body>\n</html>';

				textArea = Ext.create('Ext.form.field.TextArea', {
					width: 700,
					height: 400,
					readOnly: true,
					cls: 'ns-textarea monospaced',
					value: text
				});

				window = Ext.create('Ext.window.Window', {
                    title: 'Embed in web page' + '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + ns.app.layout.name + '</span>',
					layout: 'fit',
					modal: true,
					resizable: false,
					items: textArea,
					destroyOnBlur: true,
					bbar: [
						'->',
						{
							text: 'Select',
							handler: function() {
								textArea.selectText();
							}
						}
					],
					listeners: {
						show: function(w) {
							ns.core.web.window.setAnchorPosition(w, ns.app.shareButton);

							document.body.oncontextmenu = true;

							if (!w.hasDestroyOnBlurHandler) {
								ns.core.web.window.addDestroyOnBlurHandler(w);
							}
						},
						hide: function() {
							document.body.oncontextmenu = function(){return false;};
						}
					}
				});

				window.show();
			}
		});

        favoriteUrlItem = Ext.create('Ext.menu.Item', {
			text: 'Favorite link' + '&nbsp;&nbsp;',
			iconCls: 'ns-menu-item-datasource',
			disabled: true,
			xable: function() {
				if (ns.app.layout.id) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
            handler: function() {
                var url = ns.core.init.contextPath + '/dhis-web-visualizer/index.html?id=' + ns.app.layout.id,
                    textField,
                    window;

                textField = Ext.create('Ext.form.field.Text', {
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>'
                });

				window = Ext.create('Ext.window.Window', {
                    title: 'Favorite link' + '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + ns.app.layout.name + '</span>',
					layout: 'fit',
					modal: true,
					resizable: false,
					destroyOnBlur: true,
                    bodyStyle: 'padding: 12px 18px; background-color: #fff; font-size: 11px',
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>',
					listeners: {
						show: function(w) {
							ns.core.web.window.setAnchorPosition(w, ns.app.shareButton);

							document.body.oncontextmenu = true;

							if (!w.hasDestroyOnBlurHandler) {
								ns.core.web.window.addDestroyOnBlurHandler(w);
							}
						},
						hide: function() {
							document.body.oncontextmenu = function(){return false;};
						}
					}
				});

				window.show();
            }
        });

        apiUrlItem = Ext.create('Ext.menu.Item', {
			text: 'API link' + '&nbsp;&nbsp;',
			iconCls: 'ns-menu-item-datasource',
			disabled: true,
			xable: function() {
				if (ns.app.layout.id) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
            handler: function() {
                var url = ns.core.init.contextPath + '/api/charts/' + ns.app.layout.id + '/data',
                    textField,
                    window;

                textField = Ext.create('Ext.form.field.Text', {
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>'
                });

				window = Ext.create('Ext.window.Window', {
                    title: 'API link' + '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + ns.app.layout.name + '</span>',
					layout: 'fit',
					modal: true,
					resizable: false,
					destroyOnBlur: true,
                    bodyStyle: 'padding: 12px 18px; background-color: #fff; font-size: 11px',
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>',
					listeners: {
						show: function(w) {
							ns.core.web.window.setAnchorPosition(w, ns.app.shareButton);

							document.body.oncontextmenu = true;

							if (!w.hasDestroyOnBlurHandler) {
								ns.core.web.window.addDestroyOnBlurHandler(w);
							}
						},
						hide: function() {
							document.body.oncontextmenu = function(){return false;};
						}
					}
				});

				window.show();
            }
        });

		shareButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.share,
            disabled: true,
			xableItems: function() {
				interpretationItem.xable();
				pluginItem.xable();
				favoriteUrlItem.xable();
				apiUrlItem.xable();
			},
			menu: {
				cls: 'ns-menu',
				shadow: false,
				showSeparator: false,
				items: [
					interpretationItem,
					pluginItem,
                    favoriteUrlItem,
                    apiUrlItem
				],
				listeners: {
					afterrender: function() {
						this.getEl().addCls('ns-toolbar-btn-menu');
					},
					show: function() {
						shareButton.xableItems();
					}
				}
			},
			listeners: {
				added: function() {
					ns.app.shareButton = this;
				}
			}
		});

		aboutButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.about,
            menu: {},
			handler: function() {
				if (ns.app.aboutWindow && ns.app.aboutWindow.destroy) {
					ns.app.aboutWindow.destroy();
				}

				ns.app.aboutWindow = AboutWindow();
				ns.app.aboutWindow.show();
			},
			listeners: {
				added: function() {
					ns.app.aboutButton = this;
				}
			}
		});

		defaultButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.chart,
			iconCls: 'ns-button-icon-chart',
			toggleGroup: 'module',
			pressed: true,
            menu: {},
            handler: function(b) {
                b.menu = Ext.create('Ext.menu.Menu', {
                    closeAction: 'destroy',
                    shadow: false,
                    showSeparator: false,
                    items: [
                        {
                            text: NS.i18n.clear_chart + '&nbsp;&nbsp;', //i18n
                            cls: 'ns-menu-item-noicon',
                            handler: function() {
                                window.location.href = ns.core.init.contextPath + '/dhis-web-visualizer';
                            }
                        }
                    ],
                    listeners: {
                        show: function() {
                            ns.core.web.window.setAnchorPosition(b.menu, b);
                        },
                        hide: function() {
                            b.menu.destroy();
                            defaultButton.toggle();
                        },
                        destroy: function(m) {
                            b.menu = null;
                        }
                    }
                });

                b.menu.show();
            }
		});

		centerRegion = Ext.create('Ext.panel.Panel', {
			region: 'center',
			bodyStyle: 'padding:1px',
			autoScroll: true,
			fullSize: true,
			cmp: [defaultButton],
			toggleCmp: function(show) {
				for (var i = 0; i < this.cmp.length; i++) {
					if (show) {
						this.cmp[i].show();
					}
					else {
						this.cmp[i].hide();
					}
				}
			},
			tbar: {
				defaults: {
					height: 26
				},
				items: [
					{
						text: '<<<',
						handler: function(b) {
							var text = b.getText();
							text = text === '<<<' ? '>>>' : '<<<';
							b.setText(text);

							westRegion.toggleCollapse();
						}
					},
					{
						text: '<b>' + NS.i18n.update + '</b>',
						handler: function() {
							update();
						}
					},
					layoutButton,
					optionsButton,
					{
						xtype: 'tbseparator',
						height: 18,
						style: 'border-color:transparent; border-right-color:#d1d1d1; margin-right:4px',
					},
					favoriteButton,
					downloadButton,
					shareButton,
					'->',
					{
						text: NS.i18n.table,
						iconCls: 'ns-button-icon-table',
						toggleGroup: 'module',
						menu: {},
						handler: function(b) {
							b.menu = Ext.create('Ext.menu.Menu', {
								closeAction: 'destroy',
								shadow: false,
								showSeparator: false,
								items: [
									{
										text: NS.i18n.go_to_pivot_tables + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-pivot';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-pivot', '_blank');
														}
													}
												});
											}
										}
									},
									'-',
									{
										text: NS.i18n.open_this_chart_as_table + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										disabled: !(NS.isSessionStorage && ns.app.layout),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled && NS.isSessionStorage) {
														ns.app.layout.parentGraphMap = treePanel.getParentGraphMap();
														ns.core.web.storage.session.set(ns.app.layout, 'analytical');

														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-pivot/index.html?s=analytical';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-pivot/index.html?s=analytical', '_blank');
														}
													}
												});
											}
										}
									},
									{
										text: NS.i18n.open_last_pivot_table + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										disabled: !(NS.isSessionStorage && JSON.parse(sessionStorage.getItem('dhis2')) && JSON.parse(sessionStorage.getItem('dhis2'))['table']),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-pivot/index.html?s=table';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-pivot/index.html?s=table', '_blank');
														}
													}
												});
											}
										}
									}
								],
								listeners: {
									show: function() {
										ns.core.web.window.setAnchorPosition(b.menu, b);
									},
									hide: function() {
										b.menu.destroy();
										defaultButton.toggle();
									},
									destroy: function(m) {
										b.menu = null;
									}
								}
							});

							b.menu.show();
						},
						listeners: {
							render: function() {
								centerRegion.cmp.push(this);
							}
						}
					},
					defaultButton,
					{
						text: NS.i18n.map,
						iconCls: 'ns-button-icon-map',
						toggleGroup: 'module',
						menu: {},
						handler: function(b) {
							b.menu = Ext.create('Ext.menu.Menu', {
								closeAction: 'destroy',
								shadow: false,
								showSeparator: false,
								items: [
									{
										text: NS.i18n.go_to_maps + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-mapping';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-mapping', '_blank');
														}
													}
												});
											}
										}
									},
									'-',
									{
										text: NS.i18n.open_this_chart_as_map + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										disabled: !(NS.isSessionStorage && ns.app.layout),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled && NS.isSessionStorage) {
														ns.app.layout.parentGraphMap = treePanel.getParentGraphMap();
														ns.core.web.storage.session.set(ns.app.layout, 'analytical');

														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-mapping/index.html?s=analytical';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-mapping/index.html?s=analytical', '_blank');
														}
													}
												});
											}
										}
									},
									{
										text: NS.i18n.open_last_map + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										disabled: !(NS.isSessionStorage && JSON.parse(sessionStorage.getItem('dhis2')) && JSON.parse(sessionStorage.getItem('dhis2'))['chart']),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-mapping/index.html?s=chart';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-mapping/index.html?s=chart', '_blank');
														}
													}
												});
											}
										}
									}
								],
								listeners: {
									show: function() {
										ns.core.web.window.setAnchorPosition(b.menu, b);
									},
									hide: function() {
										b.menu.destroy();
										defaultButton.toggle();
									},
									destroy: function(m) {
										b.menu = null;
									}
								}
							});

							b.menu.show();
						},
						listeners: {
							render: function() {
								centerRegion.cmp.push(this);
							}
						}
					},
					{
						xtype: 'tbseparator',
						height: 18,
						style: 'border-color:transparent; border-right-color:#d1d1d1; margin-right:4px',
						listeners: {
							render: function() {
								centerRegion.cmp.push(this);
							}
						}
					},
                    aboutButton,
					{
						xtype: 'button',
						text: NS.i18n.home,
						handler: function() {
							window.location.href = ns.core.init.contextPath + '/dhis-web-commons-about/redirect.action';
						}
					}
				]
			},
			listeners: {
				added: function() {
					ns.app.centerRegion = this;
				},
                resize: function(p) {
                    if (ns.app.xLayout && ns.app.chart) {
                        ns.app.chart.onViewportResize();
                    }

                    // toolbar
					var width = this.getWidth();

					if (width < 768 && this.fullSize) {
						this.toggleCmp(false);
						this.fullSize = false;
					}
					else if (width >= 768 && !this.fullSize) {
						this.toggleCmp(true);
						this.fullSize = true;
					}
                },
				afterrender: function(p) {
					var html = '';

					html += '<div class="ns-viewport-text" style="padding:20px">';
					html += '<h3>' + NS.i18n.example1 + '</h3>';
					html += '<div>- ' + NS.i18n.example2 + '</div>';
					html += '<div>- ' + NS.i18n.example3 + '</div>';
					html += '<div>- ' + NS.i18n.example4 + '</div>';
					html += '<h3 style="padding-top:20px">' + NS.i18n.example5 + '</h3>';
					html += '<div>- ' + NS.i18n.example6 + '</div>';
					html += '<div>- ' + NS.i18n.example7 + '</div>';
					html += '<div>- ' + NS.i18n.example8 + '</div>';
					html += '</div>';

					p.update(html);
				}
			}
		});

		setGui = function(layout, xLayout, updateGui) {
			var dimensions = Ext.Array.clean([].concat(layout.columns || [], layout.rows || [], layout.filters || [])),
				dimMap = ns.core.service.layout.getObjectNameDimensionMapFromDimensionArray(dimensions),
				recMap = ns.core.service.layout.getObjectNameDimensionItemsMapFromDimensionArray(dimensions),
				graphMap = layout.parentGraphMap,
				objectName,
				periodRecords,
				fixedPeriodRecords = [],
				dimNames = [],
				isOu = false,
				isOuc = false,
				isOugc = false,
				levels = [],
				groups = [],
				orgunits = [];

			// State
			downloadButton.enable();
            shareButton.enable();

			// Set gui
			if (!updateGui) {
				return;
			}

            // dx
            dataSelectedStore.removeAll();

			indicatorAvailableStore.removeAll();
            indicatorGroup.clearValue();

			dataElementAvailableStore.removeAll();
            dataElementGroup.clearValue();
            dataElementDetailLevel.reset();

			dataSetAvailableStore.removeAll();

			eventDataItemAvailableStore.removeAll();
			programIndicatorAvailableStore.removeAll();

            if (Ext.isObject(xLayout.program) && Ext.isString(xLayout.program.id)) {
                eventDataItemProgram.setValue(xLayout.program.id);
                onEventDataItemProgramSelect(xLayout.program.id)
            }

            if (dimMap['dx']) {
                dataSelectedStore.addRecords(recMap['dx']);
            }

			// periods
			fixedPeriodSelectedStore.removeAll();
			period.resetRelativePeriods();
			periodRecords = recMap[dimConf.period.objectName] || [];
			for (var i = 0, periodRecord, checkbox; i < periodRecords.length; i++) {
				periodRecord = periodRecords[i];
				checkbox = relativePeriod.valueComponentMap[periodRecord.id];
				if (checkbox) {
					checkbox.setValue(true);
				}
				else {
					fixedPeriodRecords.push(periodRecord);
				}
			}
			fixedPeriodSelectedStore.add(fixedPeriodRecords);
			ns.core.web.multiSelect.filterAvailable({store: fixedPeriodAvailableStore}, {store: fixedPeriodSelectedStore});

			// group sets
			for (var key in dimensionPanelMap) {
				if (dimensionPanelMap.hasOwnProperty(key)) {
					var panel = dimensionPanelMap[key],
                        a = panel.availableStore,
						s = panel.selectedStore;

                    // reset
                    a.reset();
                    s.removeAll();
                    panel.selectedAll.setValue(false);

                    // add
                    if (Ext.Array.contains(xLayout.objectNames, key)) {
                        if (recMap[key]) {
                            s.add(recMap[key]);
                            ns.core.web.multiSelect.filterAvailable({store: a}, {store: s});
                        }
                        else {
                            panel.selectedAll.setValue(true);
                        }
                    }
				}
			}

			// layout
			ns.app.viewport.chartType.setChartType(layout.type);

			ns.app.stores.dimension.removeAll();
			ns.app.stores.col.removeAll();
			ns.app.stores.row.removeAll();
			ns.app.stores.filter.removeAll();

			if (layout.columns) {
				dimNames = [];

				for (var i = 0, dim; i < layout.columns.length; i++) {
					dim = dimConf.objectNameMap[layout.columns[i].dimension];

					if (!Ext.Array.contains(dimNames, dim.dimensionName)) {
						ns.app.stores.col.add({
							id: dim.dimensionName,
							name: dimConf.objectNameMap[dim.dimensionName].name
						});

						dimNames.push(dim.dimensionName);
					}

					ns.app.stores.dimension.remove(ns.app.stores.dimension.getById(dim.dimensionName));
				}
			}

			if (layout.rows) {
				dimNames = [];

				for (var i = 0, dim; i < layout.rows.length; i++) {
					dim = dimConf.objectNameMap[layout.rows[i].dimension];

					if (!Ext.Array.contains(dimNames, dim.dimensionName)) {
						ns.app.stores.row.add({
							id: dim.dimensionName,
							name: dimConf.objectNameMap[dim.dimensionName].name
						});

						dimNames.push(dim.dimensionName);
					}

					ns.app.stores.dimension.remove(ns.app.stores.dimension.getById(dim.dimensionName));
				}
			}

			if (layout.filters) {
				dimNames = [];

				for (var i = 0, dim; i < layout.filters.length; i++) {
					dim = dimConf.objectNameMap[layout.filters[i].dimension];

					if (!Ext.Array.contains(dimNames, dim.dimensionName)) {
						ns.app.stores.filter.add({
							id: dim.dimensionName,
							name: dimConf.objectNameMap[dim.dimensionName].name
						});

						dimNames.push(dim.dimensionName);
					}

					ns.app.stores.dimension.remove(ns.app.stores.dimension.getById(dim.dimensionName));
				}
			}

            // add assigned categories as dimension
            if (!ns.app.layoutWindow.hasDimension(dimConf.category.dimensionName)) {
                ns.app.stores.dimension.add({id: dimConf.category.dimensionName, name: dimConf.category.name});
            }

            // add data as dimension
            if (!ns.app.layoutWindow.hasDimension(dimConf.data.dimensionName)) {
                ns.app.stores.dimension.add({id: dimConf.data.dimensionName, name: dimConf.data.name});
            }

            // add orgunit as dimension
            if (!ns.app.layoutWindow.hasDimension(dimConf.organisationUnit.dimensionName)) {
                ns.app.stores.dimension.add({id: dimConf.organisationUnit.dimensionName, name: dimConf.organisationUnit.name});
            }

			// options
			if (ns.app.optionsWindow) {
				ns.app.optionsWindow.setOptions(layout);
			}

			// organisation units
			if (recMap[dimConf.organisationUnit.objectName]) {
				for (var i = 0, ouRecords = recMap[dimConf.organisationUnit.objectName]; i < ouRecords.length; i++) {
					if (ouRecords[i].id === 'USER_ORGUNIT') {
						isOu = true;
					}
					else if (ouRecords[i].id === 'USER_ORGUNIT_CHILDREN') {
						isOuc = true;
					}
					else if (ouRecords[i].id === 'USER_ORGUNIT_GRANDCHILDREN') {
						isOugc = true;
					}
					else if (ouRecords[i].id.substr(0,5) === 'LEVEL') {
						levels.push(parseInt(ouRecords[i].id.split('-')[1]));
					}
					else if (ouRecords[i].id.substr(0,8) === 'OU_GROUP') {
						groups.push(parseInt(ouRecords[i].id.split('-')[1]));
					}
					else {
						orgunits.push(ouRecords[i].id);
					}
				}

				if (levels.length) {
					toolMenu.clickHandler('level');
					organisationUnitLevel.setValue(levels);
				}
				else if (groups.length) {
					toolMenu.clickHandler('group');
					organisationUnitGroup.setValue(groups);
				}
				else {
					toolMenu.clickHandler('orgunit');
					userOrganisationUnit.setValue(isOu);
					userOrganisationUnitChildren.setValue(isOuc);
					userOrganisationUnitGrandChildren.setValue(isOugc);
				}

				if (!(isOu || isOuc || isOugc)) {
					if (Ext.isObject(graphMap)) {
						treePanel.selectGraphMap(graphMap);
					}
				}
			}
			else {
				treePanel.reset();
			}
		};

		viewport = Ext.create('Ext.container.Viewport', {
			layout: 'border',
			chartType: chartType,
			period: period,
			treePanel: treePanel,
			setGui: setGui,
            westRegion: westRegion,
            centerRegion: centerRegion,
            update: update,
			items: [
				westRegion,
				centerRegion
			],
			listeners: {
				render: function() {
					ns.app.viewport = this;

                    ns.app.layoutWindow = LayoutWindow();
                    ns.app.layoutWindow.hide();

					ns.app.optionsWindow = OptionsWindow();
					ns.app.optionsWindow.hide();
				},
                afterrender: function() {

					// resize event handler
					westRegion.on('resize', function() {
						var panel = accordion.getExpandedPanel();

						if (panel) {
							panel.onExpand();
						}
					});

					// left gui
					var viewportHeight = westRegion.getHeight(),
						numberOfTabs = ns.core.init.dimensions.length + 3,
						tabHeight = 28,
						minPeriodHeight = 380,
						settingsHeight = 46;

					if (viewportHeight > numberOfTabs * tabHeight + minPeriodHeight + settingsHeight) {
						if (!Ext.isIE) {
							accordion.setAutoScroll(false);
							westRegion.setWidth(ns.core.conf.layout.west_width);
							accordion.doLayout();
						}
					}
					else {
						westRegion.hasScrollbar = true;
					}

					// expand first panel
					accordion.getFirstPanel().expand();

					// look for url params
					var id = ns.core.web.url.getParam('id'),
						session = ns.core.web.url.getParam('s'),
						layout;

					if (id) {
						ns.core.web.chart.loadChart(id);
					}
					else if (Ext.isString(session) && NS.isSessionStorage && Ext.isObject(JSON.parse(sessionStorage.getItem('dhis2'))) && session in JSON.parse(sessionStorage.getItem('dhis2'))) {
						layout = ns.core.api.layout.Layout(ns.core.service.layout.analytical2layout(JSON.parse(sessionStorage.getItem('dhis2'))[session]));

						if (layout) {
							ns.core.web.chart.getData(layout, true);
						}
					}

                    // remove params from url
                    if (id || session) {
                        history.pushState(null, null, '.')
                    }

                    var initEl = document.getElementById('init');
                    initEl.parentNode.removeChild(initEl);

                    Ext.getBody().setStyle('background', '#fff');
                    Ext.getBody().setStyle('opacity', 0);

					// fade in
					Ext.defer( function() {
						Ext.getBody().fadeIn({
							duration: 600
						});
					}, 300 );
                }
            }
        });

		// add listeners
		(function() {
			ns.app.stores.indicatorAvailable.on('load', function() {
				ns.core.web.multiSelect.filterAvailable(indicatorAvailable, indicatorSelected);
			});

			ns.app.stores.dataElementAvailable.on('load', function() {
				ns.core.web.multiSelect.filterAvailable(dataElementAvailable, dataElementSelected);
			});

			ns.app.stores.dataSetAvailable.on('load', function(s) {
				ns.core.web.multiSelect.filterAvailable(dataSetAvailable, dataSetSelected);
				s.sort('name', 'ASC');
			});
		}());

        return viewport;
    };

	// initialize
	(function() {
		var requests = [],
			callbacks = 0,
			init = {},
            appConfig,
			fn;

		fn = function() {
			if (++callbacks === requests.length) {

				ns.core = NS.getCore(init);
                ns.alert = ns.core.webAlert;
				extendCore(ns.core);

				dimConf = ns.core.conf.finals.dimension;
                chartConf = ns.core.conf.finals.chart;
				ns.app.viewport = createViewport();

                ns.core.app.getViewportWidth = function() { return ns.app.viewport.getWidth(); };
                ns.core.app.getViewportHeight = function() { return ns.app.viewport.getHeight(); };
                ns.core.app.getCenterRegionWidth = function() { return ns.app.viewport.centerRegion.getWidth(); };
                ns.core.app.getCenterRegionHeight = function() { return ns.app.viewport.centerRegion.getHeight(); };

                NS.instances.push(ns);
			}
		};

		// requests
		Ext.Ajax.request({
			url: 'manifest.webapp',
			success: function(r) {
				init.contextPath = Ext.decode(r.responseText).activities.dhis.href;

                // system info
                Ext.Ajax.request({
                    url: init.contextPath + '/api/system/info.json',
                    success: function(r) {
                        init.systemInfo = Ext.decode(r.responseText);
                        init.contextPath = init.systemInfo.contextPath || init.contextPath;

                        // date, calendar
                        Ext.Ajax.request({
                            url: init.contextPath + '/api/systemSettings.json?key=keyCalendar&key=keyDateFormat&key=keyAnalysisRelativePeriod',
                            success: function(r) {
                                var systemSettings = Ext.decode(r.responseText);
                                init.systemInfo.dateFormat = Ext.isString(systemSettings.keyDateFormat) ? systemSettings.keyDateFormat.toLowerCase() : 'yyyy-mm-dd';
                                init.systemInfo.calendar = systemSettings.keyCalendar;
                                init.systemInfo.analysisRelativePeriod = systemSettings.keyAnalysisRelativePeriod || 'LAST_12_MONTHS';

                                // user-account
                                Ext.Ajax.request({
                                    url: init.contextPath + '/api/me/user-account.json',
                                    success: function(r) {
                                        init.userAccount = Ext.decode(r.responseText);

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
                                            keyUiLocale,
                                            dateFormat;

                                        init.userAccount.settings.keyUiLocale = init.userAccount.settings.keyUiLocale || defaultKeyUiLocale;
                                        init.userAccount.settings.keyAnalysisDisplayProperty = displayPropertyMap[init.userAccount.settings.keyAnalysisDisplayProperty] || defaultKeyAnalysisDisplayProperty;

                                        // local vars
                                        contextPath = init.contextPath;
                                        keyUiLocale = init.userAccount.settings.keyUiLocale;
                                        keyAnalysisDisplayProperty = init.userAccount.settings.keyAnalysisDisplayProperty;
                                        namePropertyUrl = keyAnalysisDisplayProperty + '|rename(name)';
                                        dateFormat = init.systemInfo.dateFormat;

                                        init.namePropertyUrl = namePropertyUrl;

                                        // calendar
                                        (function() {
                                            var dhis2PeriodUrl = '../dhis-web-commons/javascripts/dhis2/dhis2.period.js',
                                                defaultCalendarId = 'gregorian',
                                                calendarIdMap = {'iso8601': defaultCalendarId},
                                                calendarId = calendarIdMap[init.systemInfo.calendar] || init.systemInfo.calendar || defaultCalendarId,
                                                calendarIds = ['coptic', 'ethiopian', 'islamic', 'julian', 'nepali', 'thai'],
                                                calendarScriptUrl,
                                                createGenerator;

                                            // calendar
                                            createGenerator = function() {
                                                init.calendar = $.calendars.instance(calendarId);
                                                init.periodGenerator = new dhis2.period.PeriodGenerator(init.calendar, init.systemInfo.dateFormat);
                                            };

                                            if (Ext.Array.contains(calendarIds, calendarId)) {
                                                calendarScriptUrl = '../dhis-web-commons/javascripts/jQuery/calendars/jquery.calendars.' + calendarId + '.min.js';

                                                Ext.Loader.injectScriptElement(calendarScriptUrl, function() {
                                                    Ext.Loader.injectScriptElement(dhis2PeriodUrl, createGenerator);
                                                });
                                            }
                                            else {
                                                Ext.Loader.injectScriptElement(dhis2PeriodUrl, createGenerator);
                                            }
                                        }());

                                        // i18n
                                        requests.push({
                                            url: 'i18n/i18n_app.properties',
                                            success: function(r) {
                                                NS.i18n = dhis2.util.parseJavaProperties(r.responseText);

                                                if (keyUiLocale === defaultKeyUiLocale) {
                                                    fn();
                                                }
                                                else {
                                                    Ext.Ajax.request({
                                                        url: 'i18n/i18n_app_' + keyUiLocale + '.properties',
                                                        success: function(r) {
                                                            Ext.apply(NS.i18n, dhis2.util.parseJavaProperties(r.responseText));
                                                        },
                                                        failure: function() {
                                                            console.log('No translations found for system locale (' + keyUiLocale + ')');
                                                        },
                                                        callback: function() {
                                                            fn();
                                                        }
                                                    });
                                                }
                                            },
                                            failure: function() {
                                                Ext.Ajax.request({
                                                    url: 'i18n/i18n_app_' + keyUiLocale + '.properties',
                                                    success: function(r) {
                                                        NS.i18n = dhis2.util.parseJavaProperties(r.responseText);
                                                    },
                                                    failure: function() {
                                                        alert('No translations found for system locale (' + keyUiLocale + ') or default locale (' + defaultKeyUiLocale + ').');
                                                    },
                                                    callback: fn
                                                });
                                            }
                                        });

                                        // root nodes
                                        requests.push({
                                            url: contextPath + '/api/organisationUnits.json?userDataViewFallback=true&paging=false&fields=id,' + namePropertyUrl,
                                            success: function(r) {
                                                init.rootNodes = Ext.decode(r.responseText).organisationUnits || [];
                                                fn();
                                            }
                                        });

                                        // organisation unit levels
                                        requests.push({
                                            url: contextPath + '/api/organisationUnitLevels.json?fields=id,displayName|rename(name),level&paging=false',
                                            success: function(r) {
                                                init.organisationUnitLevels = Ext.decode(r.responseText).organisationUnitLevels || [];

                                                if (!init.organisationUnitLevels.length) {
                                                    console.log('Info: No organisation unit levels defined');
                                                }

                                                fn();
                                            }
                                        });

                                        // user orgunits and children
                                        requests.push({
                                            url: contextPath + '/api/organisationUnits.json?userOnly=true&fields=id,' + namePropertyUrl + ',children[id,' + namePropertyUrl + ']&paging=false',
                                            success: function(r) {
                                                var organisationUnits = Ext.decode(r.responseText).organisationUnits || [],
                                                    ou = [],
                                                    ouc = [];

                                                if (organisationUnits.length) {
                                                    for (var i = 0, org; i < organisationUnits.length; i++) {
                                                        org = organisationUnits[i];

                                                        ou.push(org.id);

                                                        if (org.children) {
                                                            ouc = Ext.Array.clean(ouc.concat(Ext.Array.pluck(org.children, 'id') || []));
                                                        }
                                                    }

                                                    init.user = init.user || {};
                                                    init.user.ou = ou;
                                                    init.user.ouc = ouc;
                                                }
                                                else {
                                                    alert('User is not assigned to any organisation units');
                                                }

                                                fn();
                                            }
                                        });

                                        // dimensions
                                        requests.push({
                                            url: contextPath + '/api/dimensions.json?fields=id,' + namePropertyUrl + '&paging=false',
                                            success: function(r) {
                                                init.dimensions = Ext.decode(r.responseText).dimensions || [];
                                                fn();
                                            }
                                        });

                                        for (var i = 0; i < requests.length; i++) {
                                            Ext.Ajax.request(requests[i]);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
			}
		});
	}());
});
