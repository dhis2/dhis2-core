Ext.onReady( function() {
	var NS = ER,

		AggregateLayoutWindow,
        QueryLayoutWindow,
		AggregateOptionsWindow,
		FavoriteWindow,
		SharingWindow,
		InterpretationWindow,
        AboutWindow,

		extendCore,
		createViewport,
		dimConf,
        styleConf,
        finalsStyleConf,
        finalsDataTypeConf,

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

		// right click handler
		document.body.oncontextmenu = function() {
			return false;
		};
	}());

	// extensions

		// data items
	(function() {
        var scrollbarWidth = /\bchrome\b/.test(navigator.userAgent.toLowerCase()) ? 8 : 17,
            nameCmpWidth = 440 - scrollbarWidth,
            buttonCmpWidth = 20,
            operatorCmpWidth = 70,
            searchCmpWidth = 70,
            triggerCmpWidth = 17,
            valueCmpWidth = 235,
            rangeSetWidth = 135,
            namePadding = '3px 3px',
            margin = '3px 0 1px',
            removeCmpStyle = 'padding: 0; margin-left: 3px',
            defaultRangeSetId = 'default';

        Ext.define('Ext.ux.panel.DataElementIntegerContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementintegerpanel',
            cls: 'ns-dxselector',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var record = {},
                    isRange = this.rangeSetCmp.getValue() !== defaultRangeSetId;

                record.dimension = this.dataElement.id;
                record.name = this.dataElement.name;

                if (isRange) {
                    record.legendSet = {
                        id: this.rangeSetCmp.getValue()
                    };

                    if (this.rangeValueCmp.getValue().length) {
                        record.filter = 'IN:' + this.rangeValueCmp.getValue().join(';');
                    }
                }
                else {
                    if (this.valueCmp.getValue()) {
                        record.filter = this.operatorCmp.getValue() + ':' + this.valueCmp.getValue();
                    }
                }

				return record;
            },
            setRecord: function(record) {
                if (Ext.isObject(record.legendSet) && record.legendSet.id) {
                    this.rangeSetCmp.pendingValue = record.legendSet.id;
                    this.onRangeSetSelect(record.legendSet.id);

                    if (record.filter) {
                        var a = record.filter.split(':');

                        if (a.length > 1 && Ext.isString(a[1])) {
                            this.onRangeSearchSelect(a[1].split(';'), true);
                        }
                    }
                }
                else if (record.filter) {
                    //this.rangeSetCmp.pendingValue = defaultRangeSetId;
                    this.rangeSetCmp.setValue(defaultRangeSetId); //todo?
                    this.onRangeSetSelect(defaultRangeSetId);

					var a = record.filter.split(':');

                    if (a.length > 1) {
                        this.operatorCmp.setValue(a[0]);
                        this.valueCmp.setValue(a[1]);
                    }
				}
			},
            initComponent: function() {
                var container = this,
                    idProperty = 'id',
                    nameProperty = 'name',
                    displayProperty = 'displayName';

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    flex: 1,
                    style: 'padding:' + namePadding
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: 'padding: 0',
                    height: 18,
                    text: NS.i18n.duplicate,
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: removeCmpStyle,
                    height: 18,
                    text: NS.i18n.remove,
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: idProperty,
                    displayField: nameProperty,
                    queryMode: 'local',
                    editable: false,
                    width: operatorCmpWidth,
					style: 'margin-bottom:0',
                    value: 'EQ',
                    store: {
                        fields: [idProperty, nameProperty],
                        data: [
                            {id: 'EQ', name: '='},
                            {id: 'GT', name: '>'},
                            {id: 'GE', name: '>='},
                            {id: 'LT', name: '<'},
                            {id: 'LE', name: '<='},
                            {id: 'NE', name: '!='}
                        ]
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.Number', {
                    width: nameCmpWidth - operatorCmpWidth - rangeSetWidth,
					style: 'margin-bottom:0'
                });

                this.rangeSearchStore = Ext.create('Ext.data.Store', {
                    fields: [idProperty, nameProperty]
                });

                // function
                this.filterSearchStore = function(isLayout) {
                    var selected = container.rangeValueCmp.getValue();

                    // hack, using internal method to activate dropdown before filtering
                    if (isLayout) {
                        container.rangeSearchCmp.onTriggerClick();
                        container.rangeSearchCmp.collapse();
                    }

                    // filter
                    container.rangeSearchStore.clearFilter();

                    container.rangeSearchStore.filterBy(function(record) {
                        return !Ext.Array.contains(selected, record.data[idProperty]);
                    });
                };

                // function
                this.onRangeSearchSelect = function(ids, isLayout) {
                    ids = Ext.Array.from(ids);

                    // store
                    for (var i = 0, id; i < ids.length; i++) {
                        id = ids[i];

                        if (container.rangeValueStore.findExact(idProperty, id) === -1) {
                            container.rangeValueStore.add(container.rangeSearchStore.getAt(container.rangeSearchStore.findExact(idProperty, id)).data);
                        }
                    }

                    // search cmp
                    container.rangeSearchCmp.select([]);

                    // filter
                    container.filterSearchStore(isLayout);
                };

                this.rangeSearchCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    width: operatorCmpWidth,
                    style: 'margin-bottom: 0',
                    emptyText: NS.i18n.select + '..',
                    valueField: idProperty,
                    displayField: displayProperty,
                    editable: false,
                    queryMode: 'local',
                    hidden: true,
                    store: this.rangeSearchStore,
                    listConfig: {
                        minWidth: operatorCmpWidth + (nameCmpWidth - operatorCmpWidth - rangeSetWidth)
                    },
                    listeners: {
						select: function() {
                            container.onRangeSearchSelect(Ext.Array.from(this.getValue())[0]);
						},
                        expand: function() {
                            container.filterSearchStore();
                        }
					}
                });

                this.rangeValueStore = Ext.create('Ext.data.Store', {
					fields: [idProperty, nameProperty],
                    listeners: {
                        add: function() {
                            container.rangeValueCmp.select(this.getRange());
                        },
                        remove: function() {
                            container.rangeValueCmp.select(this.getRange());
                        }
                    }
                });

                this.rangeValueCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    style: 'margin-bottom: 0',
                    width: nameCmpWidth - operatorCmpWidth - rangeSetWidth,
                    valueField: idProperty,
                    displayField: nameProperty,
                    emptyText: 'No selected items',
                    editable: false,
                    hideTrigger: true,
                    queryMode: 'local',
                    hidden: true,
                    store: container.rangeValueStore,
                    listConfig: {
                        minWidth: valueCmpWidth,
                        cls: 'ns-optionselector'
                    },
                    setOptionValues: function(records) {
                        var me = this;

                        container.rangeValueStore.removeAll();
                        container.rangeValueStore.loadData(records);

                        me.setValue(records);
                    },
					listeners: {
                        change: function(cmp, newVal, oldVal) {
                            newVal = Ext.Array.from(newVal);
                            oldVal = Ext.Array.from(oldVal);

                            if (newVal.length < oldVal.length) {
                                var id = Ext.Array.difference(oldVal, newVal)[0];
                                container.rangeValueStore.removeAt(container.rangeValueStore.findExact(idProperty, id));
                            }
                        }
                    }
                });

                // function
                this.onRangeSetSelect = function(id) {
                    if (!id || id === defaultRangeSetId) {
                        container.operatorCmp.show();
                        container.valueCmp.show();
                        container.rangeSearchCmp.hide();
                        container.rangeValueCmp.hide();
                    }
                    else {
                        var ranges;

                        container.operatorCmp.hide();
                        container.valueCmp.hide();
                        container.rangeSearchCmp.show();
                        container.rangeValueCmp.show();

                        ranges = Ext.clone(ns.core.init.idLegendSetMap[id].legends);

                        // display name
                        for (var i = 0; i < ranges.length; i++) {
                            range = ranges[i];
                            range.displayName = range.name + ' (' + range.startValue + ' - ' + range.endValue + ')';
                        }

                        container.rangeSearchStore.loadData(ranges);
                        container.rangeSearchStore.sort('startValue', 'ASC');
                    }
                };

                this.rangeSetCmp = Ext.create('Ext.form.field.ComboBox', {
                    cls: 'ns-combo h22',
					style: 'margin-bottom: 0',
                    width: rangeSetWidth,
                    height: 22,
                    fieldStyle: 'height: 22px',
                    queryMode: 'local',
                    valueField: idProperty,
                    displayField: nameProperty,
                    editable: false,
                    storage: {},
                    pendingValue: null,
                    setPendingValue: function() {
                        if (this.pendingValue) {
                            this.setValue(this.pendingValue);
                            container.onRangeSetSelect(this.pendingValue);

                            this.pendingValue = null;
                        }

                        if (!this.getValue()) {
                            this.pendingValue = defaultRangeSetId;
                            this.setPendingValue();
                        }
                    },
                    store: Ext.create('Ext.data.Store', {
                        fields: [idProperty, nameProperty]
                    }),
                    listeners: {
                        added: function(cb) {
                            cb.store.add({
                                id: defaultRangeSetId,
                                name: 'No range set'
                            });

                            var de = container.dataElement;

                            if (de.legendSet || de.storageLegendSet) {
                                var id = de.legendSet ? de.legendSet.id : (de.storageLegendSet ? de.storageLegendSet.id : null),
                                    legendSet = ns.core.init.idLegendSetMap[id];

                                if (Ext.isObject(legendSet)) {
                                    cb.store.add(legendSet);

                                    cb.setValue(legendSet.id);
                                    container.onRangeSetSelect(legendSet.id);
                                }
                            }

                            cb.setPendingValue();
                        },
                        select: function(cb, r) {
                            var id = Ext.Array.from(r)[0].data.id;
                            container.onRangeSetSelect(id);
                        }
                    }
                });

                this.items = [
                    {
                        xtype: 'container',
                        layout: 'hbox',
                        width: nameCmpWidth,
                        items: [
                            this.nameCmp,
                            this.addCmp,
                            this.removeCmp
                        ]
                    },
                    this.rangeSearchCmp,
                    this.rangeValueCmp,
                    this.operatorCmp,
                    this.valueCmp,
                    this.rangeSetCmp
                ];

                this.callParent();
            }
        });

        Ext.define('Ext.ux.panel.DataElementStringContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementstringpanel',
            cls: 'ns-dxselector',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var record = {};

                record.dimension = this.dataElement.id;
                record.name = this.dataElement.name;

                if (this.valueCmp.getValue()) {
					record.filter = this.operatorCmp.getValue() + ':' + this.valueCmp.getValue();
				}

				return record;
            },
            setRecord: function(record) {
                this.operatorCmp.setValue(record.operator);
                this.valueCmp.setValue(record.filter);
            },
            initComponent: function() {
                var container = this;

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    flex: 1,
                    style: 'padding:' + namePadding
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: 'padding: 0',
                    height: 18,
                    text: 'Duplicate',
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: removeCmpStyle,
                    height: 18,
                    text: 'Remove',
                    handler: function() {
                        container.removeDataElement();
                    }
                });


                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    width: operatorCmpWidth,
					style: 'margin-bottom:0',
                    value: 'LIKE',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'LIKE', name: 'Contains'},
                            {id: 'EQ', name: 'Is exact'}
                        ]
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.Text', {
                    width: nameCmpWidth - operatorCmpWidth,
					style: 'margin-bottom:0'
                });

                this.items = [
                    {
                        xtype: 'container',
                        layout: 'hbox',
                        width: nameCmpWidth,
                        items: [
                            this.nameCmp,
                            this.addCmp,
                            this.removeCmp
                        ]
                    },
                    this.operatorCmp,
                    this.valueCmp
                ];

                this.callParent();
            }
        });

        Ext.define('Ext.ux.panel.DataElementDateContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementdatepanel',
            cls: 'ns-dxselector',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var record = {};

                record.dimension = this.dataElement.id;
                record.name = this.dataElement.name;

                if (this.valueCmp.getValue()) {
					record.filter = this.operatorCmp.getValue() + ':' + this.valueCmp.getSubmitValue();
				}

				return record;
            },
            setRecord: function(record) {
				if (record.filter && Ext.isString(record.filter)) {
					var a = record.filter.split(':');

					this.operatorCmp.setValue(a[0]);
					this.valueCmp.setValue(a[1]);
				}
            },
            initComponent: function() {
                var container = this;

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    flex: 1,
                    style: 'padding:' + namePadding
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: 'padding: 0',
                    height: 18,
                    text: 'Duplicate',
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: removeCmpStyle,
                    height: 18,
                    text: 'Remove',
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    width: operatorCmpWidth,
                    style: 'margin-bottom:0',
                    value: 'EQ',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'EQ', name: '='},
                            {id: 'GT', name: '>'},
                            {id: 'GE', name: '>='},
                            {id: 'LT', name: '<'},
                            {id: 'LE', name: '<='},
                            {id: 'NE', name: '!='}
                        ]
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.Date', {
					width: nameCmpWidth - operatorCmpWidth,
					style: 'margin-bottom:0',
					format: 'Y-m-d'
				});

                this.items = [
                    {
                        xtype: 'container',
                        layout: 'hbox',
                        width: nameCmpWidth,
                        items: [
                            this.nameCmp,
                            this.addCmp,
                            this.removeCmp
                        ]
                    },
                    this.operatorCmp,
                    this.valueCmp
                ];

                this.callParent();
            }
        });

		Ext.define('Ext.ux.panel.DataElementBooleanContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementbooleanpanel',
            cls: 'ns-dxselector',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var items = this.valueCmp.getValue(),
					record = {
                        dimension: this.dataElement.id,
                        name: this.dataElement.name
                    };

                // array or object
                for (var i = 0; i < items.length; i++) {
                    if (Ext.isObject(items[i])) {
                        items[i] = items[i].code;
                    }
                }

                if (items.length) {
                    record.filter = 'IN:' + items.join(';');
                }

                return record;
            },
            setRecord: function(record) {
				if (Ext.isString(record.filter) && record.filter.length) {
					var a = record.filter.split(':');
					this.valueCmp.setOptionValues(a[1].split(';'));
				}
            },
            getRecordsByCode: function(options, codeArray) {
                var records = [];

                for (var i = 0; i < options.length; i++) {
                    for (var j = 0; j < codeArray.length; j++) {
                        if (options[i].code === codeArray[j]) {
                            records.push(options[i]);
                        }
                    }
                }

                return records;
            },
            initComponent: function() {
                var container = this,
                    idProperty = 'id',
                    nameProperty = 'name';

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    flex: 1,
                    style: 'padding:' + namePadding
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: 'padding: 0',
                    height: 18,
                    text: 'Duplicate',
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: removeCmpStyle,
                    height: 18,
                    text: 'Remove',
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    style: 'margin-bottom:0',
                    width: operatorCmpWidth,
                    value: 'IN',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'IN', name: 'One of'}
                        ]
                    }
                });

                this.getData = function(idArray) {
                    var data = [], yes = {}, no = {};

                    yes[idProperty] = '1';
                    yes[nameProperty] = NS.i18n.yes;
                    no[idProperty] = '0';
                    no[nameProperty] = NS.i18n.no;

                    for (var i = 0; i < idArray.length; i++) {
                        if (idArray[i] === '1' || idArray[i] === 1) {
                            data.push(yes);
                        }
                        else if (idArray[i] === '0' || idArray[i] === 0) {
                            data.push(no);
                        }
                    }

                    return data;
                };

                this.searchStore = Ext.create('Ext.data.Store', {
					fields: [idProperty, nameProperty],
					data: container.getData(['1', '0'])
				});

                // function
                this.filterSearchStore = function(isLayout) {
                    var selected = container.valueCmp.getValue();

                    // hack, using internal method to activate dropdown before filtering
                    if (isLayout) {
                        container.searchCmp.onTriggerClick();
                        container.searchCmp.collapse();
                    }

                    // filter
                    container.searchStore.clearFilter();

                    container.searchStore.filterBy(function(record) {
                        return !Ext.Array.contains(selected, record.data[idProperty]);
                    });
                };

                this.searchCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    width: operatorCmpWidth,
                    style: 'margin-bottom:0',
                    emptyText: 'Select..',
                    valueField: idProperty,
                    displayField: nameProperty,
                    queryMode: 'local',
                    listConfig: {
                        minWidth: nameCmpWidth - operatorCmpWidth
                    },
                    store: this.searchStore,
                    listeners: {
						select: function() {
                            var id = Ext.Array.from(this.getValue())[0];

                            // value
                            if (container.valueStore.findExact(idProperty, id) === -1) {
                                container.valueStore.add(container.searchStore.getAt(container.searchStore.findExact(idProperty, id)).data);
                            }

                            // search
                            this.select([]);

                            // filter
                            container.filterSearchStore();
						},
                        expand: function() {
                            container.filterSearchStore();
                        }
					}
                });

                this.valueStore = Ext.create('Ext.data.Store', {
					fields: [idProperty, nameProperty],
                    listeners: {
                        add: function() {
                            container.valueCmp.select(this.getRange());
                        },
                        remove: function() {
                            container.valueCmp.select(this.getRange());
                        }
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    style: 'margin-bottom:0',
					width: nameCmpWidth - operatorCmpWidth - operatorCmpWidth,
                    valueField: idProperty,
                    displayField: nameProperty,
                    emptyText: 'No selected items',
                    editable: false,
                    hideTrigger: true,
                    store: container.valueStore,
                    queryMode: 'local',
                    listConfig: {
                        minWidth: 266,
                        cls: 'ns-optionselector'
                    },
                    setOptionValues: function(codeArray) {
                        container.valueStore.removeAll();
                        container.valueStore.loadData(container.getData(codeArray));

                        this.setValue(codeArray);
                        container.filterSearchStore(true);
                        container.searchCmp.blur();
                    },
					listeners: {
                        change: function(cmp, newVal, oldVal) {
                            newVal = Ext.Array.from(newVal);
                            oldVal = Ext.Array.from(oldVal);

                            if (newVal.length < oldVal.length) {
                                var id = Ext.Array.difference(oldVal, newVal)[0];
                                container.valueStore.removeAt(container.valueStore.findExact(idProperty, id));
                            }
                        }
                    }
                });

                this.items = [
                    {
                        xtype: 'container',
                        layout: 'hbox',
                        width: nameCmpWidth,
                        items: [
                            this.nameCmp,
                            this.addCmp,
                            this.removeCmp
                        ]
                    },
                    this.operatorCmp,
                    this.searchCmp,
                    this.valueCmp
                ];

                this.callParent();
            }
        });

		Ext.define('Ext.ux.panel.OrganisationUnitGroupSetContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.organisationunitgroupsetpanel',
            cls: 'ns-dxselector',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var items = this.valueCmp.getValue(),
					record = {
                        dimension: this.dataElement.id,
                        name: this.dataElement.name
                    };

                // array or object
                for (var i = 0; i < items.length; i++) {
                    if (Ext.isObject(items[i])) {
                        items[i] = items[i].code;
                    }
                }

                if (items.length) {
                    record.filter = 'IN:' + items.join(';');
                }

                return record;
            },
            setRecord: function(record) {
				if (Ext.isString(record.filter) && record.filter.length) {
					var a = record.filter.split(':');
					this.valueCmp.setOptionValues(a[1].split(';'));
				}
            },
            getRecordsByCode: function(options, codeArray) {
                var records = [];

                for (var i = 0; i < options.length; i++) {
                    for (var j = 0; j < codeArray.length; j++) {
                        if (options[i].code === codeArray[j]) {
                            records.push(options[i]);
                        }
                    }
                }

                return records;
            },
            initComponent: function() {
                var container = this,
                    idProperty = 'code',
                    nameProperty = 'name';

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    flex: 1,
                    style: 'padding:' + namePadding
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: 'padding: 0',
                    height: 18,
                    text: 'Duplicate',
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-linkbutton',
                    style: removeCmpStyle,
                    height: 18,
                    text: 'Remove',
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    style: 'margin-bottom:0',
                    width: operatorCmpWidth,
                    value: 'IN',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'IN', name: 'One of'}
                        ]
                    }
                });

                this.searchStore = Ext.create('Ext.data.Store', {
					fields: [idProperty, nameProperty],
					data: [],
					loadOptionSet: function(optionSetId, key, pageSize) {
						var store = this;

                        optionSetId = optionSetId || container.dataElement.optionSet.id;
                        pageSize = pageSize || 100;

                        dhis2.er.store.get('optionSets', optionSetId).done( function(obj) {
                            if (Ext.isObject(obj) && Ext.isArray(obj.options) && obj.options.length) {
                                var data = [];

                                if (key) {
                                    var re = new RegExp(key, 'gi');

                                    for (var i = 0, name, match; i < obj.options.length; i++) {
                                        name = obj.options[i].name;
                                        match = name.match(re);

                                        if (Ext.isArray(match) && match.length) {
                                            data.push(obj.options[i]);

                                            if (data.length === pageSize) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                else {
                                    data = obj.options;
                                }

                                store.removeAll();
                                store.loadData(data.slice(0, pageSize));

                            }
                        });
					},
                    listeners: {
						datachanged: function(s) {
							if (container.searchCmp && s.getRange().length) {
								container.searchCmp.expand();
							}
						}
					}
				});

                // function
                this.filterSearchStore = function() {
                    var selected = container.valueCmp.getValue();

                    container.searchStore.clearFilter();

                    container.searchStore.filterBy(function(record) {
                        return !Ext.Array.contains(selected, record.data[idProperty]);
                    });
                };

                this.searchCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    width: operatorCmpWidth - triggerCmpWidth,
                    style: 'margin-bottom:0',
                    emptyText: 'Search..',
                    valueField: idProperty,
                    displayField: nameProperty,
                    hideTrigger: true,
                    enableKeyEvents: true,
                    queryMode: 'local',
                    listConfig: {
                        minWidth: nameCmpWidth - operatorCmpWidth
                    },
                    store: this.searchStore,
                    listeners: {
						keyup: function() {
                            var value = this.getValue(),
                                optionSetId = container.dataElement.optionSet.id;

                            // search
                            container.searchStore.loadOptionSet(optionSetId, value);

                            // trigger
                            if (!value || (Ext.isString(value) && value.length === 1)) {
                                container.triggerCmp.setDisabled(!!value);
                            }
                        },
						select: function() {
                            var id = Ext.Array.from(this.getValue())[0];

                            // value
                            if (container.valueStore.findExact(idProperty, id) === -1) {
                                container.valueStore.add(container.searchStore.getAt(container.searchStore.findExact(idProperty, id)).data);
                            }

                            // search
                            this.select([]);

                            // filter
                            container.filterSearchStore();

                            // trigger
                            container.triggerCmp.enable();
						},
                        expand: function() {
                            container.filterSearchStore();
                        }
					}
                });

                this.triggerCmp = Ext.create('Ext.button.Button', {
                    cls: 'ns-button-combotrigger',
                    disabledCls: 'ns-button-combotrigger-disabled',
                    width: triggerCmpWidth,
                    height: 22,
                    handler: function(b) {
                        container.searchStore.loadOptionSet();
                    }
                });

                this.valueStore = Ext.create('Ext.data.Store', {
					fields: [idProperty, nameProperty],
                    listeners: {
                        add: function() {
                            container.valueCmp.select(this.getRange());
                        },
                        remove: function() {
                            container.valueCmp.select(this.getRange());
                        }
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    style: 'margin-bottom:0',
					width: nameCmpWidth - operatorCmpWidth - operatorCmpWidth,
                    valueField: idProperty,
                    displayField: nameProperty,
                    emptyText: 'No selected items',
                    editable: false,
                    hideTrigger: true,
                    store: container.valueStore,
                    queryMode: 'local',
                    listConfig: {
                        minWidth: 266,
                        cls: 'ns-optionselector'
                    },
                    setOptionValues: function(codeArray) {
                        var me = this,
                            records = [];

                        dhis2.er.store.get('optionSets', container.dataElement.optionSet.id).done( function(obj) {
                            if (Ext.isObject(obj) && Ext.isArray(obj.options) && obj.options.length) {
                                records = container.getRecordsByCode(obj.options, codeArray);

                                container.valueStore.removeAll();
                                container.valueStore.loadData(records);

                                me.setValue(records);
                            }
                        });
                    },
					listeners: {
                        change: function(cmp, newVal, oldVal) {
                            newVal = Ext.Array.from(newVal);
                            oldVal = Ext.Array.from(oldVal);

                            if (newVal.length < oldVal.length) {
                                var id = Ext.Array.difference(oldVal, newVal)[0];
                                container.valueStore.removeAt(container.valueStore.findExact(idProperty, id));
                            }
                        }
                    }
                });

                this.items = [
                    {
                        xtype: 'container',
                        layout: 'hbox',
                        width: nameCmpWidth,
                        items: [
                            this.nameCmp,
                            this.addCmp,
                            this.removeCmp
                        ]
                    },
                    this.operatorCmp,
                    this.searchCmp,
                    this.triggerCmp,
                    this.valueCmp
                ];

                this.callParent();
            }
        });

    }());

		// toolbar
    (function() {
        Ext.define('Ext.ux.toolbar.StatusBar', {
			extend: 'Ext.toolbar.Toolbar',
			alias: 'widget.statusbar',
            queryCmps: [],
            showHideQueryCmps: function(fnName) {
				Ext.Array.each(this.queryCmps, function(cmp) {
					cmp[fnName]();
				});
			},
            setStatus: function(layout, response) {
                this.pager = response.metaData.pager;

                this.reset(layout.dataType);

                if (layout.dataType === finalsDataTypeConf.aggregated_values) {
                    this.statusCmp.setText(response.rows.length + ' values');
                    return;
                }

                if (layout.dataType === finalsDataTypeConf.individual_cases) {
                    var maxVal = this.pager.page * this.pager.pageSize,
						from = maxVal - this.pager.pageSize + 1,
						to = Ext.Array.min([maxVal, this.pager.total]);

                    this.pageCmp.setValue(this.pager.page);
                    this.pageCmp.setMaxValue(this.pager.pageCount);
                    this.totalPageCmp.setText(' of ' + this.pager.pageCount);
                    this.statusCmp.setText(from + '-' + to + ' of ' + this.pager.total + ' cases');
                    return;
                }
            },
            reset: function(dataType) {
                if (!dataType || dataType === finalsDataTypeConf.aggregated_values) {
					this.showHideQueryCmps('hide');
                    this.pageCmp.setValue(1);
                    this.totalPageCmp.setText('');
                    this.statusCmp.setText('');
                    return;
                }

                if (dataType === finalsDataTypeConf.individual_cases) {
					this.showHideQueryCmps('show');
                    this.pageCmp.setValue(1);
                    this.totalPageCmp.setText(' of 1');
                    this.statusCmp.setText('');
                }
            },
            getCurrentPage: function() {
                return this.pageCmp.getValue();
            },
            getPageCount: function() {
				return this.pageCount;
			},
            onPageChange: function(page, currentPage) {
				currentPage = currentPage || this.getCurrentPage();

				if (page && page >= 1 && page <= this.pager.pageCount && page != currentPage) {
					ns.app.layout.paging.page = page;
					this.pageCmp.setValue(page);
					ns.core.web.report.getData(ns.app.layout);
				}
            },
            initComponent: function() {
                var container = this,
                    size = this.pageSize;

                this.firstCmp = Ext.create('Ext.button.Button', {
					text: '<<',
					handler: function() {
						container.onPageChange(1);
					}
				});
				this.queryCmps.push(this.firstCmp);

                this.prevCmp = Ext.create('Ext.button.Button', {
					text: '<',
					handler: function() {
						container.onPageChange(container.getCurrentPage() - 1);
					}
				});
				this.queryCmps.push(this.prevCmp);

                this.pageTextCmp = Ext.create('Ext.toolbar.TextItem', {
                    text: 'Page ',
                    style: 'line-height:21px',
                });
				this.queryCmps.push(this.pageTextCmp);

                this.pageCmp = Ext.create('Ext.form.field.Number', {
                    width: 34,
                    height: 21,
                    minValue: 1,
                    value: 1,
                    hideTrigger: true,
                    enableKeyEvents: true,
                    currentPage: 1,
                    listeners: {
						render: function() {
							Ext.get(this.getInputId()).setStyle('padding-top', '2px');
						},
						keyup: {
							fn: function(cmp) {
								var currentPage = cmp.currentPage;

								cmp.currentPage = cmp.getValue();

								container.onPageChange(cmp.getValue(), currentPage);
							},
							buffer: 200
						}
					}
                });
				this.queryCmps.push(this.pageCmp);

                this.totalPageCmp = Ext.create('Ext.toolbar.TextItem', {
                    text: '',
                    style: 'line-height:21px'
                });
				this.queryCmps.push(this.totalPageCmp);

                this.nextCmp = Ext.create('Ext.button.Button', {
					text: '>',
					handler: function() {
						container.onPageChange(container.getCurrentPage() + 1);
					}
				});
				this.queryCmps.push(this.nextCmp);

                this.lastCmp = Ext.create('Ext.button.Button', {
					text: '>>',
					handler: function() {
						container.onPageChange(container.pager.pageCount);
					}
				});
				this.queryCmps.push(this.lastCmp);

                this.statusCmp = Ext.create('Ext.toolbar.TextItem', {
                    text: '',
                    style: 'line-height:21px',
                });

                this.items = [
                    this.statusCmp,
					this.firstCmp,
					this.prevCmp,
					this.pageTextCmp,
                    this.pageCmp,
                    this.totalPageCmp,
                    this.nextCmp,
                    this.lastCmp,
                    '->',
                    this.statusCmp
                ];

                this.callParent();
            }
        });
    }());

        // sort, limit
    (function() {
        Ext.define('Ext.ux.container.LimitContainer', {
            extend: 'Ext.container.Container',
            alias: 'widget.limitcontainer',
            layout: 'hbox',
            onCheckboxChange: function(value) {
                this.sortOrderCmp.setDisabled(!value);
                this.topLimitCmp.setDisabled(!value);
            },
            getSortOrder: function() {
                return this.activateCmp.getValue() ? this.sortOrderCmp.getValue() : 0;
            },
            getTopLimit: function() {
                return this.activateCmp.getValue() ? this.topLimitCmp.getValue() : 0;
            },
            setValues: function(sortOrder, topLimit) {
                sortOrder = parseInt(sortOrder);
                topLimit = parseInt(topLimit);

                if (Ext.isNumber(sortOrder)) {
                    this.sortOrderCmp.setValue(sortOrder);
                }
                else {
                    this.sortOrderCmp.reset();
                }

                if (Ext.isNumber(topLimit)) {
                    this.topLimitCmp.setValue(topLimit);
                }
                else {
                    this.topLimitCmp.reset();
                }

                this.activateCmp.setValue(!!(sortOrder > 0 && topLimit > 0));
            },
            initComponent: function() {
                var container = this,
                    activateWidth = 135,
                    sortWidth = (this.comboboxWidth - activateWidth) / 2;


                this.activateCmp = Ext.create('Ext.form.field.Checkbox', {
                    boxLabel: container.boxLabel,
                    width: activateWidth,
                    style: 'margin-bottom:4px',
                    listeners: {
                        change: function(cmp, newValue) {
                            container.onCheckboxChange(newValue);
                        }
                    }
                });

                this.sortOrderCmp = Ext.create('Ext.form.field.ComboBox', {
                    cls: 'ns-combo',
                    style: 'margin-bottom:' + container.comboBottomMargin + 'px',
                    width: sortWidth,
                    queryMode: 'local',
                    valueField: 'id',
                    editable: false,
                    value: container.sortOrder,
                    store: Ext.create('Ext.data.Store', {
                        fields: ['id', 'text'],
                        data: [
                            {id: -1, text: NS.i18n.bottom},
                            {id: 1, text: NS.i18n.top}
                        ]
                    })
                });

                this.topLimitCmp = Ext.create('Ext.form.field.Number', {
                    width: sortWidth - 1,
                    style: 'margin-bottom:' + container.comboBottomMargin + 'px; margin-left:1px',
                    minValue: 1,
                    maxValue: 10000,
                    value: container.topLimit,
                    allowBlank: false
                });

                this.items = [
                    this.activateCmp,
                    this.sortOrderCmp,
                    this.topLimitCmp
                ];

                this.callParent();
            },
            listeners: {
                render: function() {
                    this.onCheckboxChange(false);
                }
            }
        });
    }());

	// constructors

	AggregateLayoutWindow = function() {
		var row,
			rowStore,
			col,
			colStore,
            fixedFilter,
            fixedFilterStore,
			filter,
			filterStore,
            onValueSelect,
			value,
            val,
            onCollapseDataDimensionsChange,
            collapseDataDimensions,
            aggregationType,

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
			defaultWidth = 210,
			defaultHeight = 220,
			maxHeight = (ns.app.viewport.getHeight() - 100) / 2,

			dataType = finalsDataTypeConf.aggregated_values,
            defaultValueId = 'default';

		getStore = function(applyConfig) {
			var config = {},
                store;

			config.fields = ['id', 'name'];

			Ext.apply(config, applyConfig);

			config.getDimensionNames = function() {
				var dimensionNames = [];

				this.each(function(r) {
					dimensionNames.push(r.data.id);
				});

				return Ext.clone(dimensionNames);
			};

			store = Ext.create('Ext.data.Store', config);

            return store;
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

		colStore = getStore({name: 'colStore'});
		rowStore = getStore({name: 'rowStore'});
        fixedFilterStore = getStore({name: 'fixedFilterStore'});
        filterStore = getStore({name: 'filterStore'});
        valueStore = getStore({name: 'valueStore'});

        // store functions
        valueStore.addDefaultData = function() {
            if (!this.getById(defaultValueId)) {
                this.insert(0, {
                    id: defaultValueId,
                    name: NS.i18n.number_of_events
                });
            }
        };

        fixedFilterStore.setListHeight = function() {
            var fixedFilterHeight = 26 + (this.getRange().length * 21) + 1;
            fixedFilter.setHeight(fixedFilterHeight);
            filter.setHeight(defaultHeight - fixedFilterHeight);
        };

        // gui
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
					text: NS.i18n.column_dimensions,
					cls: 'ns-toolbar-multiselect-leftright-label'
				}
			},
			listeners: {
				afterrender: function(ms) {
					ms.boundList.on('itemdblclick', function(view, record) {
						ms.store.remove(record);
						filterStore.add(record);
					});

					ms.store.on('add', function() {
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
			style: 'margin-right:' + margin + 'px; margin-bottom:0px',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'layoutDD',
			dropGroup: 'layoutDD',
			store: rowStore,
			tbar: {
				height: 25,
				items: {
					xtype: 'label',
					text: NS.i18n.row_dimensions,
					cls: 'ns-toolbar-multiselect-leftright-label'
				}
			},
			listeners: {
				afterrender: function(ms) {
					ms.boundList.on('itemdblclick', function(view, record) {
						ms.store.remove(record);
						filterStore.add(record);
					});

					ms.store.on('add', function() {
						Ext.defer( function() {
							ms.boundList.getSelectionModel().deselectAll();
						}, 10);
					});
				}
			}
		});

        fixedFilter = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-leftright ns-multiselect-fixed',
			width: defaultWidth,
			height: 26,
			style: 'margin-right:' + margin + 'px; margin-bottom:0',
			valueField: 'id',
			displayField: 'name',
			store: fixedFilterStore,
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
                    ms.on('change', function() {
                        ms.boundList.getSelectionModel().deselectAll();
                    });
				}
			}
		});

		filter = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-leftright ns-multiselect-dynamic',
			width: defaultWidth,
			height: defaultHeight - 26,
			style: 'margin-right:' + margin + 'px; margin-bottom:' + margin + 'px',
            bodyStyle: 'border-top:0 none',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'layoutDD',
			dropGroup: 'layoutDD',
			store: filterStore,
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

        aggregationType = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo h22',
			width: 80,
			height: 22,
			style: 'margin: 0',
            fieldStyle: 'height: 22px',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
            disabled: true,
            value: 'COUNT',
            disabledValue: 'COUNT',
            defaultValue: 'AVERAGE',
            setDisabled: function() {
                this.setValue(this.disabledValue);
                this.disable();
            },
            setEnabled: function() {
                this.setValue(this.defaultValue);
                this.enable();
            },
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: ns.core.conf.aggregationType.data
			}),
            resetData: function() {
                this.setDisabled();
            }
		});

        onValueSelect = function(id) {
            id = id || value.getValue();

            if (id === defaultValueId) {
                aggregationType.setDisabled();
            }
            else {
                aggregationType.setEnabled();

                // remove ux and layout item
                if (hasDimension(id, valueStore)) {
                    var uxArray = ns.app.accordion.getUxArray(id);

                    for (var i = 0; i < uxArray.length; i++) {
                        uxArray[i].removeDataElement();
                    }
                }
            }
        };

		value = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo h24',
			width: defaultWidth - 4,
			height: 24,
            fieldStyle: 'height: 24px',
			queryMode: 'local',
			valueField: 'id',
            displayField: 'name',
			editable: false,
			store: valueStore,
            value: defaultValueId,
            setDefaultData: function() {
                valueStore.addDefaultData();
                this.setValue(defaultValueId);
                aggregationType.resetData();
            },
            setDefaultDataIf: function() {
                if (!value.getValue()) {
                    this.setDefaultData();
                }
            },
            resetData: function() {
                valueStore.removeAll();
                this.clearValue();
                aggregationType.resetData();
            },
            listeners: {
                select: function(cb, r) {
                    onValueSelect(r[0].data.id);
                }
            }
		});

        val = Ext.create('Ext.panel.Panel', {
            bodyStyle: 'padding: 1px',
            width: defaultWidth,
            height: 220,
            items: value,
            tbar: {
                height: 25,
                style: 'padding: 1px',
                items: [
                    {
                        xtype: 'label',
                        height: 22,
                        style: 'padding-left: 6px; line-height: 22px',
                        text: NS.i18n.value
                    },
                    '->',
                    aggregationType
                ]
            }
        });

        onCollapseDataDimensionsChange = function(value) {
            toggleDataItems(value);
            toggleValueGui(value);
        };

        collapseDataDimensions = Ext.create('Ext.form.field.Checkbox', {
            boxLabel: NS.i18n.collapse_data_dimensions,
            style: 'margin-left: 3px',
            listeners: {
                change: function(chb, value) {
                    onCollapseDataDimensionsChange(value);
                }
            }
        });

		selectPanel = Ext.create('Ext.panel.Panel', {
			bodyStyle: 'border:0 none',
			items: [
				{
                    xtype: 'container',
					layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
                        {
                            xtype: 'container',
                            bodyStyle: 'border:0 none',
                            items: [
                                fixedFilter,
                                filter
                            ]
                        },
						col
					]
				},
				{
					xtype: 'container',
                    layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
						row,
                        val
					]
				}
			]
		});

        addDimension = function(record, store, excludedStores, force) {
            if (record.isProgramIndicator) {
                return;
            }

            store = store && force ? store : dimensionStoreMap[record.id] || store || filterStore;

            if (hasDimension(record.id, excludedStores)) {
                if (force) {
                    removeDimension(record.id);
                    store.add(record);
                }
            }
            else {
                if (record.id !== value.getValue()) {
                    store.add(record);
                }
            }

            onCollapseDataDimensionsChange(collapseDataDimensions.getValue());
        };

        removeDimension = function(id, excludedStores) {
            var stores = Ext.Array.difference([colStore, rowStore, filterStore, fixedFilterStore, valueStore], Ext.Array.from(excludedStores));

            for (var i = 0, store, index; i < stores.length; i++) {
                store = stores[i];
                index = store.findExact('id', id);

                if (index != -1) {
                    store.remove(store.getAt(index));
                    dimensionStoreMap[id] = store;
                }
            }
        };

        hasDimension = function(id, excludedStores) {
            var stores = Ext.Array.difference([colStore, rowStore, filterStore, fixedFilterStore, valueStore], Ext.Array.from(excludedStores));

            for (var i = 0, store, index; i < stores.length; i++) {
                store = stores[i];
                index = store.findExact('id', id);

                if (index != -1) {
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

            fixedFilterStore.each(function(record) {
                map[record.data.id] = fixedFilterStore;
            });

            //valueStore.each(function(record) {
                //map[record.data.id] = valueStore;
            //});

            return map;
        };

		resetData = function() {
			var map = saveState({}),
				keys = ['ou', 'pe', 'dates'];

			for (var key in map) {
				if (map.hasOwnProperty(key) && !Ext.Array.contains(keys, key)) {
					removeDimension(key);
				}
			}
		};

		reset = function(isAll, skipValueStore) {
			colStore.removeAll();
			rowStore.removeAll();
			fixedFilterStore.removeAll();
			filterStore.removeAll();

            if (!skipValueStore) {
                valueStore.removeAll();
                valueStore.addDefaultData();
            }

            value.clearValue();

			if (!isAll) {
				colStore.add({id: dimConf.organisationUnit.dimensionName, name: dimConf.organisationUnit.name});
				colStore.add({id: dimConf.period.dimensionName, name: dimConf.period.name});
			}

			fixedFilterStore.setListHeight();
		};

        toggleDataItems = function(param) {
            var stores = [colStore, rowStore, filterStore, fixedFilterStore],
                collapse = Ext.isObject(param) && Ext.isDefined(param.collapseDataItems) ? param.collapseDataItems : param,
                keys = ['ou', 'pe', 'dates'],
                dimensionKeys = Ext.Array.pluck(ns.core.init.dimensions || [], 'id'),
                dy = ['dy'],
                keys;

            // clear filters
            for (var i = 0, store; i < stores.length; i++) {
                stores[i].clearFilter();
            }

            // add dy if it does not exist
            if (!hasDimension('dy')) {
                addDimension({
                    id: 'dy',
                    dimension: 'dy',
                    name: NS.i18n.data
                }, rowStore);
            }

            // keys
            if (collapse) { // included keys
                keys = ['ou', 'pe', 'dates', 'dy'].concat(dimensionKeys);
            }
            else { // excluded keys
                keys = ['dy'];
            }

            // data items
            for (var i = 0, store, include; i < stores.length; i++) {
                store = stores[i];

                if (collapse) {
                    store.filterBy(function(record, id) {
                        return Ext.Array.contains(keys, record.data.id);
                    });
                }
                else {
                    store.filterBy(function(record, id) {
                        return !Ext.Array.contains(keys, record.data.id);
                    });
                }
            }
        };

        toggleValueGui = function(param) {
            var collapse = Ext.isObject(param) && param.collapseDataItems ? param.collapseDataItems : param;

            val.setDisabled(collapse);
        };

		window = Ext.create('Ext.window.Window', {
			title: NS.i18n.table_layout,
			bodyStyle: 'background-color:#fff; padding:' + margin + 'px',
			closeAction: 'hide',
			autoShow: true,
			modal: true,
			resizable: false,
			dataType: dataType,
			colStore: colStore,
			rowStore: rowStore,
            fixedFilterStore: fixedFilterStore,
			filterStore: filterStore,
            valueStore: valueStore,
            value: value,
            addDimension: addDimension,
            removeDimension: removeDimension,
            hasDimension: hasDimension,
            dimensionStoreMap: dimensionStoreMap,
            saveState: saveState,
            resetData: resetData,
            reset: reset,
            onCollapseDataDimensionsChange: onCollapseDataDimensionsChange,
            collapseDataDimensions: collapseDataDimensions,
            toggleDataItems: toggleDataItems,
            toggleValueGui: toggleValueGui,
            getValueConfig: function() {
                var config = {},
                    valueId = value.getValue();

                if (valueId && valueId !== defaultValueId) {
                    config.value = {id: valueId};
                    config.aggregationType = aggregationType.getValue();
                }

                return config;
            },
            setValueConfig: function(valueId, aggType) {
                value.setValue(valueId);
                onValueSelect();

                aggregationType.setValue(aggType);
            },
            getOptions: function() {
                return {
                    collapseDataDimensions: collapseDataDimensions.getValue()
                };
            },
            hideOnBlur: true,
			items: selectPanel,
			bbar: [
                collapseDataDimensions,
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
								var config = ns.core.web.report.getLayoutConfig();

								if (!config) {
									return;
								}

								ns.core.web.report.getData(config, false);

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

                    // value
                    value.setDefaultDataIf();
				},
                render: function() {
					reset();

                    fixedFilterStore.on('add', function() {
                        this.setListHeight();
                    });
                    fixedFilterStore.on('remove', function() {
                        this.setListHeight();
                    });
                }
			}
		});

		return window;
	};

    QueryLayoutWindow = function() {
		var dimension,
			dimensionStore,
			col,
			colStore,

			getStore,
			getStoreKeys,
			getCmpHeight,
			getSetup,
            addDimension,
            removeDimension,
            hasDimension,
            saveState,
            resetData,
            reset,
            dimensionStoreMap = {},

			dimensionPanel,
			window,

			margin = 1,
			defaultWidth = 210,
			defaultHeight = 158,
			maxHeight = (ns.app.viewport.getHeight() - 100) / 2,

			dataType = finalsDataTypeConf.individual_cases;

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
		dimensionStore.reset = function(all) {
			dimensionStore.removeAll();
		};

		colStore = getStore();

		getCmpHeight = function() {
			var size = dimensionStore.totalCount,
				expansion = 10,
				height = defaultHeight,
				diff;

			if (size > 10) {
				diff = size - 10;
				height += (diff * expansion);
			}

			height = height > maxHeight ? maxHeight : height;

			return height;
		};

		dimension = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-leftright',
			width: defaultWidth - 50,
			height: (getCmpHeight() * 2) + margin,
			style: 'margin-right:' + margin + 'px; margin-bottom:0px',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'querylayoutDD',
			dropGroup: 'querylayoutDD',
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
					ms.boundList.on('itemdblclick', function(view, record) {
						ms.store.remove(record);
						colStore.add(record);
					});

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
			height: (getCmpHeight() * 2) + margin,
			style: 'margin-bottom: 0px',
			valueField: 'id',
			displayField: 'name',
			dragGroup: 'querylayoutDD',
			dropGroup: 'querylayoutDD',
			store: colStore,
			tbar: {
				height: 25,
				items: {
					xtype: 'label',
					text: NS.i18n.column_dimensions,
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

		getSetup = function() {
			return {
				col: getStoreKeys(colStore)
			};
		};

        addDimension = function(record, store) {
            var store = dimensionStoreMap[record.id] || store || dimensionStore;

            if (!hasDimension(record.id)) {
                store.add(record);
            }
        };

        removeDimension = function(dataElementId) {
            var stores = [dimensionStore, colStore];

            for (var i = 0, store, index; i < stores.length; i++) {
                store = stores[i];
                index = store.findExact('id', dataElementId);

                if (index != -1) {
                    store.remove(store.getAt(index));
                    dimensionStoreMap[dataElementId] = store;
                }
            }
        };

        hasDimension = function(id) {
            var stores = [colStore, dimensionStore];

            for (var i = 0, store, index; i < stores.length; i++) {
                store = stores[i];
                index = store.findExact('id', id);

                if (index != -1) {
                    return true;
                }
            }

            return false;
        };

		saveState = function(map) {
            map = map || dimensionStoreMap;

            dimensionStore.each(function(record) {
                map[record.data.id] = dimensionStore;
            });

            colStore.each(function(record) {
                map[record.data.id] = colStore;
            });

            return map;
        };

		resetData = function() {
			var map = saveState({}),
				keys = ['pe', 'latitude', 'longitude', 'ou'];

			for (var key in map) {
				if (map.hasOwnProperty(key) && !Ext.Array.contains(keys, key)) {
					removeDimension(key);
				}
			}
		};

		reset = function() {
			colStore.removeAll();
			dimensionStore.removeAll();

			colStore.add({id: 'pe', name: 'Event date'});
			colStore.add({id: 'ou', name: 'Organisation unit'});

			dimensionStore.add({id: 'longitude', name: 'Longitude'});
			dimensionStore.add({id: 'latitude', name: 'Latitude'});
		};

		window = Ext.create('Ext.window.Window', {
			title: NS.i18n.table_layout,
            layout: 'column',
			bodyStyle: 'background-color:#fff; padding:' + margin + 'px',
			closeAction: 'hide',
			autoShow: true,
			modal: true,
			resizable: false,
			getSetup: getSetup,
			dimensionStore: dimensionStore,
			dataType: dataType,
			colStore: colStore,
            addDimension: addDimension,
            removeDimension: removeDimension,
            hasDimension: hasDimension,
            saveState: saveState,
            resetData: resetData,
            reset: reset,
            getValueConfig: function() {
                return {};
            },
			hideOnBlur: true,
			items: [
                dimension,
                col
            ],
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
								var config = ns.core.web.report.getLayoutConfig();

								if (!config) {
									return;
								}

								// keep sorting
								if (ns.app.layout && ns.app.layout.sorting) {
									config.sorting = Ext.clone(ns.app.layout.sorting);
								}

								window.hide();

								ns.core.web.report.getData(config, false);
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

    AggregateOptionsWindow = function() {
		var showColTotals,
            showRowTotals,
			showColSubTotals,
            showRowSubTotals,
			showDimensionLabels,
			hideEmptyRows,
            hideNaData,
            completedOnly,
            limit,
            outputType,
            aggregationType,
			showHierarchy,
			digitGroupSeparator,
			displayDensity,
			fontSize,
			reportingPeriod,
			organisationUnit,
			parentOrganisationUnit,

			data,
			style,
			parameters,

			comboboxWidth = 280,
            comboBottomMargin = 1,
            checkboxBottomMargin = 2,
            separatorTopMargin = 10,
			window;

        showColTotals = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.show_col_totals,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
			checked: true
		});

		showColSubTotals = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.show_col_subtotals,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
			checked: true
		});

		showRowTotals = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.show_row_totals,
			style: 'margin-top:' + separatorTopMargin + 'px; margin-bottom:' + checkboxBottomMargin + 'px',
			checked: true
		});

		showRowSubTotals = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.show_row_subtotals,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
			checked: true
		});

		showDimensionLabels = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.show_dimension_labels,
			style: 'margin-top:' + separatorTopMargin + 'px; margin-bottom:' + checkboxBottomMargin + 'px',
			checked: true
		});

		hideEmptyRows = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.hide_empty_rows,
			style: 'margin-top:' + separatorTopMargin + 'px; margin-bottom:' + checkboxBottomMargin + 'px',
		});

		hideNaData = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.hide_na_data,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
		});

		completedOnly = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.include_only_completed_events_only,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
		});

        limit = Ext.create('Ext.ux.container.LimitContainer', {
            boxLabel: NS.i18n.limit,
            sortOrder: 1,
            topLimit: 10,
            comboboxWidth: comboboxWidth,
            comboBottomMargin: comboBottomMargin,
            style: 'margin-top:' + separatorTopMargin + 'px'
        });

        outputType = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:' + comboBottomMargin + 'px',
			width: comboboxWidth,
			labelWidth: 130,
			fieldLabel: NS.i18n.output_type,
			labelStyle: 'color:#333',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
			value: 'EVENT',
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: 'EVENT', text: NS.i18n.event},
					{id: 'ENROLLMENT', text: NS.i18n.enrollment},
					{id: 'TRACKED_ENTITY_INSTANCE', text: NS.i18n.tracked_entity_instance}
				]
			})
		});

		showHierarchy = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.show_hierarchy,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
		});

		displayDensity = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:' + comboBottomMargin + 'px',
			width: comboboxWidth,
			labelWidth: 130,
			fieldLabel: NS.i18n.display_density,
			labelStyle: 'color:#333',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
            value: finalsStyleConf.normal,
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: finalsStyleConf.compact, text: NS.i18n.compact},
					{id: finalsStyleConf.normal, text: NS.i18n.normal},
					{id: finalsStyleConf.comfortable, text: NS.i18n.comfortable}
				]
			})
		});

		fontSize = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:' + comboBottomMargin + 'px',
			width: comboboxWidth,
			labelWidth: 130,
			fieldLabel: NS.i18n.font_size,
			labelStyle: 'color:#333',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
			value: finalsStyleConf.normal,
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: finalsStyleConf.large, text: NS.i18n.large},
					{id: finalsStyleConf.normal, text: NS.i18n.normal},
					{id: finalsStyleConf.small, text: NS.i18n.small_}
				]
			})
		});

		digitGroupSeparator = Ext.create('Ext.form.field.ComboBox', {
			labelStyle: 'color:#333',
			cls: 'ns-combo',
			style: 'margin-bottom:0',
			width: comboboxWidth,
			labelWidth: 130,
			fieldLabel: NS.i18n.digit_group_separator,
			queryMode: 'local',
			valueField: 'id',
			editable: false,
			value: finalsStyleConf.space,
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: finalsStyleConf.none, text: NS.i18n.none},
					{id: finalsStyleConf.comma, text: NS.i18n.comma},
					{id: finalsStyleConf.space, text: NS.i18n.space}
				]
			})
		});

		data = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
                showColTotals,
				showColSubTotals,
				showRowTotals,
                showRowSubTotals,
                showDimensionLabels,
				hideEmptyRows,
                hideNaData,
                completedOnly,
                limit,
                outputType
                //aggregationType
			]
		};

		organisationUnits = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
				showHierarchy
			]
		};

		style = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
				displayDensity,
				fontSize,
				digitGroupSeparator
				//legendSet
			]
		};

		window = Ext.create('Ext.window.Window', {
			title: NS.i18n.table_options,
			bodyStyle: 'background-color:#fff; padding:2px',
			closeAction: 'hide',
			autoShow: true,
			modal: true,
			resizable: false,
			hideOnBlur: true,
			getOptions: function() {
				return {
					showRowTotals: showRowTotals.getValue(),
                    showColTotals: showColTotals.getValue(),
					showColSubTotals: showColSubTotals.getValue(),
                    showRowSubTotals: showRowSubTotals.getValue(),
                    showDimensionLabels: showDimensionLabels.getValue(),
					hideEmptyRows: hideEmptyRows.getValue(),
                    hideNaData: hideNaData.getValue(),
					completedOnly: completedOnly.getValue(),
					outputType: outputType.getValue(),
                    sortOrder: limit.getSortOrder(),
                    topLimit: limit.getTopLimit(),
					showHierarchy: showHierarchy.getValue(),
                    showDimensionLabels: showDimensionLabels.getValue(),
					displayDensity: displayDensity.getValue(),
					fontSize: fontSize.getValue(),
					digitGroupSeparator: digitGroupSeparator.getValue()
					//legendSet: {id: legendSet.getValue()}
				};
			},
			setOptions: function(layout) {
				showRowTotals.setValue(Ext.isBoolean(layout.showRowTotals) ? layout.showRowTotals : true);
				showColTotals.setValue(Ext.isBoolean(layout.showColTotals) ? layout.showColTotals : true);
				showColSubTotals.setValue(Ext.isBoolean(layout.showColSubTotals) ? layout.showColSubTotals : true);
				showRowSubTotals.setValue(Ext.isBoolean(layout.showRowSubTotals) ? layout.showRowSubTotals : true);
				showDimensionLabels.setValue(Ext.isBoolean(layout.showDimensionLabels) ? layout.showDimensionLabels : true);
				hideEmptyRows.setValue(Ext.isBoolean(layout.hideEmptyRows) ? layout.hideEmptyRows : false);
				hideNaData.setValue(Ext.isBoolean(layout.hideNaData) ? layout.hideNaData : false);
                completedOnly.setValue(Ext.isBoolean(layout.completedOnly) ? layout.completedOnly : false);
				outputType.setValue(Ext.isString(layout.outputType) ? layout.outputType : 'EVENT');
				limit.setValues(layout.sortOrder, layout.topLimit);
                //aggregationType.setValue(Ext.isString(layout.aggregationType) ? layout.aggregationType : 'default');
				showHierarchy.setValue(Ext.isBoolean(layout.showHierarchy) ? layout.showHierarchy : false);
				displayDensity.setValue(Ext.isString(layout.displayDensity) ? layout.displayDensity : finalsStyleConf.normal);
				fontSize.setValue(Ext.isString(layout.fontSize) ? layout.fontSize : finalsStyleConf.normal);
				digitGroupSeparator.setValue(Ext.isString(layout.digitGroupSeparator) ? layout.digitGroupSeparator : finalsStyleConf.space);
				//legendSet.setValue(Ext.isObject(layout.legendSet) && Ext.isString(layout.legendSet.id) ? layout.legendSet.id : 0);
				//reportingPeriod.setValue(Ext.isBoolean(layout.reportingPeriod) ? layout.reportingPeriod : false);
				//organisationUnit.setValue(Ext.isBoolean(layout.organisationUnit) ? layout.organisationUnit : false);
				//parentOrganisationUnit.setValue(Ext.isBoolean(layout.parentOrganisationUnit) ? layout.parentOrganisationUnit : false);
				//regression.setValue(Ext.isBoolean(layout.regression) ? layout.regression : false);
				//cumulative.setValue(Ext.isBoolean(layout.cumulative) ? layout.cumulative : false);
				//sortOrder.setValue(Ext.isNumber(layout.sortOrder) ? layout.sortOrder : 0);
				//topLimit.setValue(Ext.isNumber(layout.topLimit) ? layout.topLimit : 0);
			},
			items: [
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-top:4px; margin-bottom:6px; margin-left:5px',
					html: NS.i18n.data
				},
				data,
				{
					bodyStyle: 'border:0 none; padding:7px'
				},
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-bottom:6px; margin-left:5px',
					html: NS.i18n.organisation_units
				},
				organisationUnits,
				{
					bodyStyle: 'border:0 none; padding:7px'
				},
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-bottom:6px; margin-left:5px',
					html: NS.i18n.style
				},
				style
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
						var config = ns.core.web.report.getLayoutConfig();
							//layout = ns.core.api.layout.Layout(config);

						if (!config) {
							return;
						}

						ns.core.web.report.getData(config, false);

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

					//if (!legendSet.store.isLoaded) {
						//legendSet.store.load();
					//}

					// cmp
                    w.showColTotals = showColTotals;
					w.showRowTotals = showRowTotals;
					w.showColSubTotals = showColSubTotals
					w.showRowSubTotals = showRowSubTotals;
                    w.showDimensionLabels = showDimensionLabels;
					w.hideEmptyRows = hideEmptyRows;
                    w.hideNaData = hideNaData;
                    w.completedOnly = completedOnly;
                    w.limit = limit;
					w.outputType = outputType;
					w.showHierarchy = showHierarchy;
					w.displayDensity = displayDensity;
					w.fontSize = fontSize;
					w.digitGroupSeparator = digitGroupSeparator;
				}
			}
		});

		return window;
	};

    QueryOptionsWindow = function() {
		var completedOnly,
			digitGroupSeparator,
			displayDensity,
			fontSize,

			data,
			style,
			parameters,

			comboboxWidth = 280,
            comboBottomMargin = 1,
            checkboxBottomMargin = 2,
			window;

		completedOnly = Ext.create('Ext.form.field.Checkbox', {
			boxLabel: NS.i18n.include_only_completed_events_only,
			style: 'margin-bottom:' + checkboxBottomMargin + 'px',
		});

		displayDensity = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:' + comboBottomMargin + 'px',
			width: comboboxWidth,
			labelWidth: 130,
			fieldLabel: NS.i18n.display_density,
			labelStyle: 'color:#333',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
            value: finalsStyleConf.normal,
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: finalsStyleConf.compact, text: NS.i18n.compact},
					{id: finalsStyleConf.normal, text: NS.i18n.normal},
					{id: finalsStyleConf.comfortable, text: NS.i18n.comfortable}
				]
			})
		});

		fontSize = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			style: 'margin-bottom:' + comboBottomMargin + 'px',
			width: comboboxWidth,
			labelWidth: 130,
			fieldLabel: NS.i18n.font_size,
			labelStyle: 'color:#333',
			queryMode: 'local',
			valueField: 'id',
			editable: false,
			value: finalsStyleConf.normal,
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: finalsStyleConf.large, text: NS.i18n.large},
					{id: finalsStyleConf.normal, text: NS.i18n.normal},
					{id: finalsStyleConf.small, text: NS.i18n.small_}
				]
			})
		});

		digitGroupSeparator = Ext.create('Ext.form.field.ComboBox', {
			labelStyle: 'color:#333',
			cls: 'ns-combo',
			style: 'margin-bottom:0',
			width: comboboxWidth,
			labelWidth: 130,
			fieldLabel: NS.i18n.digit_group_separator,
			queryMode: 'local',
			valueField: 'id',
			editable: false,
			value: finalsStyleConf.space,
			store: Ext.create('Ext.data.Store', {
				fields: ['id', 'text'],
				data: [
					{id: finalsStyleConf.none, text: NS.i18n.none},
					{id: finalsStyleConf.space, text: NS.i18n.space},
					{id: finalsStyleConf.comma, text: NS.i18n.comma}
				]
			})
		});

		data = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
                completedOnly
			]
		};

		style = {
			bodyStyle: 'border:0 none',
			style: 'margin-left:14px',
			items: [
				displayDensity,
				fontSize,
				digitGroupSeparator
			]
		};

		window = Ext.create('Ext.window.Window', {
			title: NS.i18n.table_options,
			bodyStyle: 'background-color:#fff; padding:3px',
			closeAction: 'hide',
			autoShow: true,
			modal: true,
			resizable: false,
			hideOnBlur: true,
			getOptions: function() {
				return {
                    showColTotals: true,
                    showColSubTotals: true,
                    showRowTotals: false,
                    showRowSubTotals: false,
                    showDimensionLabels: true,
                    showHierarchy: false,
					hideEmptyRows: false,
                    hideNaData: false,
					completedOnly: completedOnly.getValue(),
                    sortOrder: 0,
                    topLimit: 0,
					displayDensity: displayDensity.getValue(),
					fontSize: fontSize.getValue(),
					digitGroupSeparator: digitGroupSeparator.getValue()
					//legendSet: {id: legendSet.getValue()}
				};
			},
			setOptions: function(layout) {
                completedOnly.setValue(Ext.isBoolean(layout.completedOnly) ? layout.completedOnly : false);
				displayDensity.setValue(Ext.isString(layout.displayDensity) ? layout.displayDensity : finalsStyleConf.normal);
				fontSize.setValue(Ext.isString(layout.fontSize) ? layout.fontSize : finalsStyleConf.normal);
				digitGroupSeparator.setValue(Ext.isString(layout.digitGroupSeparator) ? layout.digitGroupSeparator : finalsStyleConf.space);
			},
			items: [
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-top:4px; margin-bottom:6px; margin-left:5px',
					html: NS.i18n.data
				},
				data,
				{
					bodyStyle: 'border:0 none; padding:7px'
				},
				{
					bodyStyle: 'border:0 none; color:#222; font-size:12px; font-weight:bold',
					style: 'margin-top:2px; margin-bottom:6px; margin-left:3px',
					html: NS.i18n.style
				},
				style
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
						var config = ns.core.web.report.getLayoutConfig();
							//layout = ns.core.api.layout.Layout(config);

						if (!config) {
							return;
						}

						// keep sorting
						if (ns.app.layout && ns.app.layout.sorting) {
							config.sorting = Ext.clone(ns.app.layout.sorting);
						}

						window.hide();

						ns.core.web.report.getData(config, false);
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
                    w.completedOnly = completedOnly;
					w.displayDensity = displayDensity;
					w.fontSize = fontSize;
					w.digitGroupSeparator = digitGroupSeparator;
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

		ns.app.stores.eventReport.on('load', function(store, records) {
			var pager;

			if (store.proxy.reader && store.proxy.reader.jsonData && store.proxy.reader.jsonData.pager) {
				pager = store.proxy.reader.jsonData.pager;

				info.setText('Page ' + pager.page + ' of ' + pager.pageCount);

				prevButton.enable();
				nextButton.enable();

				if (pager.page === 1) {
					prevButton.disable();
				}

				if (pager.page === pager.pageCount) {
					nextButton.disable();
				}
			}
		});

		getBody = function() {
			var favorite,
				dimensions;

			if (ns.app.layout) {
				favorite = Ext.clone(ns.app.layout);

				// sync
				favorite.rowTotals = favorite.showRowTotals;
				delete favorite.showRowTotals;

				favorite.colTotals = favorite.showColTotals;
				delete favorite.showColTotals;

				favorite.rowSubTotals = favorite.showRowSubTotals;
				delete favorite.showRowSubTotals;

				favorite.colSubTotals = favorite.showColSubTotals;
				delete favorite.showColSubTotals;

				delete favorite.type;
				delete favorite.parentGraphMap;
                delete favorite.id;
                delete favorite.displayName;
                delete favorite.access;
                delete favorite.lastUpdated;
                delete favorite.created;

                if (favorite.dataType === "individual_cases") {
                    delete favorite.colSubTotals;
                    delete favorite.rowSubTotals;
                }
			}

			return favorite;
		};

		NameWindow = function(id) {
			var window,
				record = ns.app.stores.eventReport.getById(id);

			nameTextfield = Ext.create('Ext.form.field.Text', {
				height: 26,
				width: 350,
				fieldStyle: 'padding-left: 4px; border-color: #bbb; font-size:11px',
				style: 'margin-bottom:0',
				emptyText: 'Name of favorite',
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
							url: ns.core.init.contextPath + '/api/eventReports/',
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
								ns.app.layout.name = name;

								ns.app.stores.eventReport.loadStore();

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
						eventReport;

					if (id && name) {
						Ext.Ajax.request({
                            url: ns.core.init.contextPath + '/api/eventReports/' + id + '.json?fields=' + ns.core.conf.url.analysisFields.join(','),
							method: 'GET',
							failure: function(r) {
								ns.core.web.mask.show();
                                ns.alert(r);
							},
							success: function(r) {
								eventReport = Ext.decode(r.responseText);
								eventReport.name = name;

								Ext.Ajax.request({
									url: ns.core.init.contextPath + '/api/eventReports/' + eventReport.id + '?mergeStrategy=REPLACE',
									method: 'PUT',
									headers: {'Content-Type': 'application/json'},
									params: Ext.encode(eventReport),
									failure: function(r) {
										ns.core.web.mask.show();
                                        ns.alert(r);
									},
									success: function(r) {
										if (ns.app.layout && ns.app.layout.id === id) {
											ns.app.layout.name = name;

											if (ns.app.xLayout) {
												ns.app.xLayout.name = name;
											}
										}

										ns.app.stores.eventReport.loadStore();
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
				//iconCls: 'ns-window-title-icon-favorite',
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
			menu: {},
			disabled: !Ext.isObject(ns.app.layout),
			handler: function() {
				nameWindow = new NameWindow(null, 'create');
				nameWindow.show();
			}
		});

		searchTextfield = Ext.create('Ext.form.field.Text', {
			width: windowCmpWidth - addButton.width - 3,
			height: 26,
			fieldStyle: 'padding-right: 0; padding-left: 4px; border-color: #bbb; font-size:11px',
			emptyText: NS.i18n.search_for_favorites,
			enableKeyEvents: true,
			currentValue: '',
			listeners: {
				keyup: {
					fn: function() {
						if (this.getValue() !== this.currentValue) {
							this.currentValue = this.getValue();

							var value = this.getValue(),
								url = value ? ns.core.init.contextPath + '/api/eventReports.json?fields=id,displayName|rename(name),access&filter=displayName:ilike:' + value : null;
								store = ns.app.stores.eventReport;

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
					url = value ? ns.core.init.contextPath + '/api/eventReports?fields=id,displayName|rename(name),access' + (value ? '&filter=displayName:ilike:' + value : '') : null,
					store = ns.app.stores.eventReport;

				store.page = store.page <= 1 ? 1 : store.page - 1;
				store.loadStore(url);
			}
		});

		nextButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.next,
			handler: function() {
				var value = searchTextfield.getValue(),
					url = value ? ns.core.init.contextPath + '/api/eventReports.json?fields=id,displayName|rename(name),access' + (value ? '&filter=displayName:ilike:' + value : '') : null,
					store = ns.app.stores.eventReport;

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
									ns.core.web.report.loadReport(record.data.id);
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
												url: ns.core.init.contextPath + '/api/eventReports/' + record.data.id + '?mergeStrategy=REPLACE',
												method: 'PUT',
												headers: {'Content-Type': 'application/json'},
												params: Ext.encode(favorite),
												success: function(r) {
													ns.app.layout.id = record.data.id;
													ns.app.layout.name = true;

                                                    if (ns.app.xLayout) {
                                                        ns.app.xLayout.id = record.data.id;
                                                        ns.app.xLayout.name = true;
                                                    }

													ns.app.stores.eventReport.loadStore();
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
										url: ns.core.init.contextPath + '/api/sharing?type=eventReport&id=' + record.data.id,
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
											url: ns.core.init.contextPath + '/api/eventReports/' + record.data.id,
											method: 'DELETE',
											success: function() {
												ns.app.stores.eventReport.loadStore();
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
			store: ns.app.stores.eventReport,
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

					ns.app.stores.eventReport.on('load', function() {
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
							style: 'width:1px; margin-left:1px; margin-right:1px; margin-top:0',
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
							url: ns.core.init.contextPath + '/api/sharing?type=eventReport&id=' + sharing.object.id,
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
			linkPanel,
			shareButton,
			window;

		if (Ext.isString(ns.app.layout.id)) {
			textArea = Ext.create('Ext.form.field.TextArea', {
				cls: 'ns-textarea',
				height: 130,
				fieldStyle: 'padding-left: 3px; padding-top: 3px',
				emptyText: NS.i18n.write_your_interpretation,
				enableKeyEvents: true,
				listeners: {
					keyup: function() {
						shareButton.xable();
					}
				}
			});

			linkPanel = Ext.create('Ext.panel.Panel', {
				html: function() {
					var url = ns.core.init.contextPath + '/dhis-web-event-reports/index.html?id=' + ns.app.layout.id,
						apiUrl = ns.core.init.contextPath + '/api/eventReports/' + ns.app.layout.id + '/data.html',
						html = '';

					html += '<div><b>Report link: </b><span class="user-select"><a href="' + url + '" target="_blank">' + url + '</a></span></div>';
					//html += '<div style="padding-top:3px"><b>API link: </b><span class="user-select"><a href="' + apiUrl + '" target="_blank">' + apiUrl + '</a></span></div>';
					return html;
				}(),
				style: 'padding:3px',
				bodyStyle: 'border: 0 none'
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
							url: ns.core.init.contextPath + '/api/interpretations/eventReports/' + ns.app.layout.id,
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
				title: ns.app.layout.name,
				layout: 'fit',
				//width: 500,
				bodyStyle: 'padding:5px; background-color:#fff',
				resizable: false,
				destroyOnBlur: true,
				modal: true,
				items: [
					//textArea,
					linkPanel
				],
				//bbar: {
					//cls: 'ns-toolbar-bbar',
					//defaults: {
						//height: 24
					//},
					//items: [
						//'->',
						//shareButton
					//]
				//},
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

	LayerWidgetEvent = function(layer) {

		// stores
		var programStore,
			stagesByProgramStore,
            //dataElementsByStageStore,
            organisationUnitGroupStore,
            periodTypeStore,
            fixedPeriodAvailableStore,
            fixedPeriodSelectedStore,

        // cache
            stageStorage = {},
            attributeStorage = {},
            programIndicatorStorage = {},
            dataElementStorage = {},

		// gui
            onTypeClick,
            setLayout,
			program,
            onProgramSelect,
			stage,
            onStageSelect,
            loadDataElements,
			dataElementLabel,
            dataElementSearch,
            dataElementFilter,
            dataElementAvailable,
            dataElementSelected,
            addUxFromDataElement,
            selectDataElements,
            data,

            periodMode,
            onPeriodModeSelect,
            getDateLink,
            onDateFieldRender,
			startDate,
			endDate,
            startEndDate,

            onPeriodChange,
            onCheckboxAdd,
            intervalListeners,
            relativePeriodCmpMap = {},
            weeks,
            months,
            biMonths,
            quarters,
            sixMonths,
            financialYears,
            years,
            relativePeriod,
            checkboxes = [],

            fixedPeriodAvailable,
            fixedPeriodSelected,
            onPeriodTypeSelect,
            periodType,
            prevYear,
            nextYear,
            fixedPeriodSettings,
            fixedPeriodAvailableSelected,
            periods,
			period,

			treePanel,
			userOrganisationUnit,
			userOrganisationUnitChildren,
			userOrganisationUnitGrandChildren,
			organisationUnitLevel,
			organisationUnitGroup,
            organisationUnitPanel,
			toolMenu,
			tool,
			toolPanel,
            organisationUnit,
			dimensionIdAvailableStoreMap = {},
			dimensionIdSelectedStoreMap = {},

			getDimensionPanel,
			getDimensionPanels,

            accordionBody,
			accordionPanels = [],
            accordion,

			reset,
			setGui,
			getView,
			validateView,

            baseWidth = 446,
            toolWidth = 36,
            accBaseWidth = baseWidth - 2,

            conf = ns.core.conf,
            rp = conf.period.relativePeriods,

            namePropertyUrl = ns.core.init.namePropertyUrl,
            nameProperty = ns.core.init.userAccount.settings.keyAnalysisDisplayProperty;

		// stores

		programStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: ns.core.init.contextPath + '/api/programs.json?fields=id,' + namePropertyUrl + '&paging=false',
				reader: {
					type: 'json',
					root: 'programs'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			},
			sortInfo: {field: 'name', direction: 'ASC'},
			isLoaded: false,
			listeners: {
				load: function() {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}
				}
			}
		});

		stagesByProgramStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			isLoaded: false,
			listeners: {
				load: function() {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}
					this.sort('name', 'ASC');
				}
			}
		});

		dataElementsByStageStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'isAttribute', 'isProgramIndicator'],
			data: [],
			sorters: [{
                property: 'name',
                direction: 'ASC'
            }],
            onLoadData: function() {

                // layout window
                var layoutWindow = ns.app.aggregateLayoutWindow;

                this.each(function(record) {
                    if (Ext.Array.contains(ns.core.conf.valueType.numericTypes, record.data.valueType)) {
                        layoutWindow.valueStore.add(record.data);
                    }
                });

                //this.toggleProgramIndicators();
            },
            toggleProgramIndicators: function(type) {
                type = type || ns.app.typeToolbar.getType();

                this.clearFilter();

                if (type === finalsDataTypeConf.aggregated_values) {
                    this.filterBy(function(record) {
                        return !record.data.isProgramIndicator;
                    });
                }
            }
		});

		organisationUnitGroupStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: ns.core.init.contextPath + '/api/organisationUnitGroups.json?fields=id,' + ns.core.init.namePropertyUrl + '&paging=false',
				reader: {
					type: 'json',
					root: 'organisationUnitGroups'
				}
			}
		});

        periodTypeStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: ns.core.conf.period.periodTypes
		});

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

		fixedPeriodSelectedStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: []
		});

        // components

            // data element
        onTypeClick = function(type) {

            // available
            dataElementsByStageStore.toggleProgramIndicators(type);

            // selected
            dataElementSelected.toggleProgramIndicators(type);
        };

        setLayout = function(layout) {
			var dimensions = Ext.Array.clean([].concat(layout.columns || [], layout.rows || [], layout.filters || [])),
				recMap = ns.core.service.layout.getObjectNameDimensionItemsMapFromDimensionArray(dimensions),

				periodRecords = recMap[dimConf.period.objectName] || [],
				fixedPeriodRecords = [],

				ouRecords = recMap[dimConf.organisationUnit.objectName],
				graphMap = layout.parentGraphMap,
				isOu = false,
				isOuc = false,
				isOugc = false,
				levels = [],
				groups = [],
				winMap = {},
				optionsWindow;

            winMap[finalsDataTypeConf.aggregated_values] = ns.app.aggregateOptionsWindow;
            winMap[finalsDataTypeConf.individual_cases] = ns.app.queryOptionsWindow;

            optionsWindow = winMap[layout.dataType];

            // set layout

            reset();

            ns.app.typeToolbar.setType(layout.dataType);
            ns.app.aggregateLayoutWindow.reset();
            ns.app.queryLayoutWindow.reset();

			// data
            programStore.add(layout.program);
            program.setValue(layout.program.id);

            // periods
			period.reset();

			if (layout.startDate && layout.endDate) {
				onPeriodModeSelect('dates');
				startDate.setValue(layout.startDate);
				endDate.setValue(layout.endDate);
			}
			else {
				onPeriodModeSelect('periods');
			}

			for (var i = 0, periodRecord, checkbox; i < periodRecords.length; i++) {
				periodRecord = periodRecords[i];
				checkbox = relativePeriodCmpMap[periodRecord.id];
				if (checkbox) {
					checkbox.setValue(true);
				}
				else {
					fixedPeriodRecords.push(periodRecord);
				}
			}

			fixedPeriodSelectedStore.add(fixedPeriodRecords);

			// organisation units
			if (ouRecords) {
				for (var i = 0; i < ouRecords.length; i++) {
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
						groups.push(ouRecords[i].id.split('-')[1]);
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

            // dimensions
			for (var key in dimensionIdSelectedStoreMap) {
				if (dimensionIdSelectedStoreMap.hasOwnProperty(key)) {
					var a = dimensionIdAvailableStoreMap[key],
						s = dimensionIdSelectedStoreMap[key];

					if (s.getCount() > 0) {
						a.reset();
						s.removeAll();
					}

					if (recMap[key]) {
						s.add(recMap[key]);
						ns.core.web.multiSelect.filterAvailable({store: a}, {store: s});
					}
				}
			}

			// options
			if (optionsWindow) {
				optionsWindow.setOptions(layout);
			}

			// data items
            onProgramSelect(layout.program.id, layout);
        };

		program = Ext.create('Ext.form.field.ComboBox', {
			editable: false,
			valueField: 'id',
			displayField: 'name',
			fieldLabel: 'Program',
			labelAlign: 'top',
			labelCls: 'ns-form-item-label-top',
			labelSeparator: '',
			emptyText: 'Select program',
			forceSelection: true,
			queryMode: 'remote',
			columnWidth: 0.5,
			style: 'margin:1px 1px 1px 0',
			storage: {},
			store: programStore,
            getRecord: function() {
                return this.getValue ? {
                    id: this.getValue(),
                    name: this.getRawValue()
                } : null;
            },
			listeners: {
				select: function(cb) {
					onProgramSelect(cb.getValue());
				}
			}
		});

		onProgramSelect = function(programId, layout) {
            var load;

            programId = layout ? layout.program.id : programId;

            // reset
			stage.clearValue();
			dataElementsByStageStore.removeAll();
			dataElementSelected.removeAllDataElements(true);
            ns.app.aggregateLayoutWindow.value.resetData();

            load = function(stages) {
                stage.enable();
                stage.clearValue();

                stagesByProgramStore.removeAll();
                stagesByProgramStore.loadData(stages);

                stageId = (layout ? layout.programStage.id : null) || (stages.length === 1 ? stages[0].id : null);

                if (stageId) {
                    stage.setValue(stageId);
                    onStageSelect(stageId, layout);
                }
            };

            if (stageStorage.hasOwnProperty(programId)) {
                load(stageStorage[programId]);
            }
            else {
                Ext.Ajax.request({
                    url: ns.core.init.contextPath + '/api/programs.json?filter=id:eq:' + programId + '&fields=programStages[id,displayName|rename(name)],programIndicators[id,' + namePropertyUrl + '],programTrackedEntityAttributes[trackedEntityAttribute[id,' + namePropertyUrl + ',valueType,confidential,optionSet[id,displayName|rename(name)],legendSet[id,displayName|rename(name)]]]&paging=false',
                    success: function(r) {
                        var program = Ext.decode(r.responseText).programs[0],
                            stages,
                            attributes,
                            programIndicators,
                            stageId;

                        if (!program) {
                            return;
                        }

                        stages = program.programStages;
                        attributes = Ext.Array.pluck(program.programTrackedEntityAttributes, 'trackedEntityAttribute');
                        programIndicators = program.programIndicators;

                        // filter confidential, mark as attribute
                        attributes.filter(function(item) {
                            item.isAttribute = true;
                            return !item.confidential;
                        });

                        // attributes cache
                        if (Ext.isArray(attributes) && attributes.length) {
                            attributeStorage[programId] = attributes;
                        }

                        // mark as program indicator
                        programIndicators.forEach(function(item) {
                            item.isProgramIndicator = true;
                        });

                        // program indicator cache
                        if (Ext.isArray(programIndicators) && programIndicators.length) {
                            programIndicatorStorage[programId] = programIndicators;
                        }

                        if (Ext.isArray(stages) && stages.length) {

                            // stages cache
                            stageStorage[programId] = stages;

                            load(stages);
                        }
                    }
                });
            }
		};

		stage = Ext.create('Ext.form.field.ComboBox', {
			editable: false,
			valueField: 'id',
			displayField: 'name',
			fieldLabel: 'Stage',
			labelAlign: 'top',
			labelCls: 'ns-form-item-label-top',
			labelSeparator: '',
			emptyText: 'Select stage',
			queryMode: 'local',
			forceSelection: true,
			columnWidth: 0.5,
			style: 'margin:1px 0 1px 0',
			disabled: true,
			listConfig: {loadMask: false},
			store: stagesByProgramStore,
            getRecord: function() {
                return this.getValue() ? {
                    id: this.getValue(),
                    name: this.getRawValue()
                } : null;
            },
			listeners: {
				select: function(cb) {
					onStageSelect(cb.getValue());
				}
			}
		});

		onStageSelect = function(stageId, layout) {
            if (!layout) {
                dataElementSelected.removeAllDataElements(true);
                ns.app.aggregateLayoutWindow.value.resetData();
            }

            dataElementSearch.enable();
            dataElementSearch.hideFilter();

			loadDataElements(stageId, layout);
		};

		loadDataElements = function(stageId, layout) {
			var programId = layout ? layout.program.id : (program.getValue() || null),
                load;

            stageId = stageId || layout.programStage.id;

			load = function(dataElements) {
                var attributes = attributeStorage[programId],
                    programIndicators = programIndicatorStorage[programId],
                    data = Ext.Array.clean([].concat(attributes || [], programIndicators || [], dataElements || []));

				dataElementsByStageStore.loadData(data);
                dataElementsByStageStore.onLoadData();

                if (layout) {
                    var dataDimensions = ns.core.service.layout.getDataDimensionsFromLayout(layout),
                        records = [];

                    for (var i = 0, dim, row; i < dataDimensions.length; i++) {
                        dim = dataDimensions[i];
                        row = dataElementsByStageStore.getById(dim.dimension);

                        if (row) {
                            records.push(Ext.applyIf(dim, row.data));
                        }
                    }

                    selectDataElements(records, layout);
                }
			};

            // data elements
            if (dataElementStorage.hasOwnProperty(stageId)) {
                load(dataElementStorage[stageId]);
            }
            else {
                Ext.Ajax.request({
                    url: ns.core.init.contextPath + '/api/programStages.json?filter=id:eq:' + stageId + '&fields=programStageDataElements[dataElement[id,' + namePropertyUrl + ',valueType,optionSet[id,displayName|rename(name)],legendSet|rename(storageLegendSet)[id,displayName|rename(name)]]]',
                    success: function(r) {
                        var objects = Ext.decode(r.responseText).programStages,
                            types = ns.core.conf.valueType.tAggregateTypes,
                            dataElements;

                        if (!objects.length) {
                            load();
                            return;
                        }

                        dataElements = Ext.Array.pluck(objects[0].programStageDataElements, 'dataElement');

                        // filter non-aggregatable types
                        dataElements.filter(function(item) {
                            item.isDataElement = true;
                            return Ext.Array.contains(types, item.valueType);
                        });

                        // data elements cache
                        dataElementStorage[stageId] = dataElements;

                        load(dataElements);
                    }
                });
            }
		};

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
            width: 170,
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
                var store = dataElementsByStageStore,
                    value = this.getValue(),
                    name;

                if (value === '') {
                    store.clearFilter();
                    return;
                }

                store.filterBy(function(r) {
                    name = r.data.name || '';
                    return name.toLowerCase().indexOf(value.toLowerCase()) !== -1;
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

		dataElementAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: accBaseWidth,
            height: 180,
			valueField: 'id',
			displayField: 'name',
            style: 'margin-bottom:1px',
			store: dataElementsByStageStore,
			tbar: [
				dataElementLabel,
                dataElementSearch,
                dataElementFilter,
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowdown.png',
					width: 22,
					height: 22,
					handler: function() {
                        if (dataElementAvailable.getValue().length) {
                            selectDataElements(dataElementAvailable.getValue());
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowdowndouble.png',
					width: 22,
					height: 22,
					handler: function() {
                        if (dataElementsByStageStore.getRange().length) {
                            selectDataElements(dataElementsByStageStore.getRange());
                        }
					}
				}
			],
			listeners: {
				afterrender: function(ms) {
					this.boundList.on('itemdblclick', function() {
                        if (ms.getValue().length) {
                            selectDataElements(ms.getValue());
                        }
					});
				}
			}
		});

        dataElementSelected = Ext.create('Ext.panel.Panel', {
			width: accBaseWidth,
            height: 242,
            bodyStyle: 'padding-left:1px',
            autoScroll: true,
            tbar: [
				{
					xtype: 'label',
                    text: 'Selected data items',
                    style: 'padding-left:6px; color:#333',
					cls: 'ns-toolbar-multiselect-left-label'
				},
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowupdouble.png',
					width: 22,
					height: 22,
					handler: function() {
						dataElementSelected.removeAllDataElements();
					}
				}
			],
            getChildIndex: function(child) {
				var items = this.items.items;

				for (var i = 0; i < items.length; i++) {
					if (items[i].id === child.id) {
						return i;
					}
				}

				return items.length;
			},
			hasDataElement: function(dataElementId) {
				var hasDataElement = false;

				this.items.each(function(item) {
					if (item.dataElement.id === dataElementId) {
						hasDataElement = true;
					}
				});

				return hasDataElement;
			},
            getUxArrayById: function(dataElementId) {
                var uxArray = [];

                this.items.each(function(item) {
					if (item.dataElement.id === dataElementId) {
						uxArray.push(item);
					}
				});

                return uxArray;
            },
			removeAllDataElements: function(reset) {
				var items = this.items.items,
					len = items.length;

				for (var i = 0; i < len; i++) {
					items[0].removeDataElement(reset);
				}
			},
            toggleProgramIndicators: function(type) {
				var items = this.items.items,
					len = items.length;

				for (var i = 0, item; i < len; i++) {
					item = items[i];

                    if (type === finalsDataTypeConf.aggregated_values && item.isProgramIndicator) {
                        item.disable();
                    }
                    else {
                        item.enable();
                    }
				}
            }
        });

        addUxFromDataElement = function(element, index) {
			var getUxType,
				ux;

			index = index || dataElementSelected.items.items.length;

			getUxType = function(element) {

				if (Ext.isObject(element.optionSet) && Ext.isString(element.optionSet.id)) {
					return 'Ext.ux.panel.OrganisationUnitGroupSetContainer';
				}

				if (Ext.Array.contains(ns.core.conf.valueType.numericTypes, element.valueType)) {
					return 'Ext.ux.panel.DataElementIntegerContainer';
				}

				if (Ext.Array.contains(ns.core.conf.valueType.textTypes, element.valueType)) {
					return 'Ext.ux.panel.DataElementStringContainer';
				}

				if (Ext.Array.contains(ns.core.conf.valueType.dateTypes, element.valueType)) {
					return 'Ext.ux.panel.DataElementDateContainer';
				}

				if (Ext.Array.contains(ns.core.conf.valueType.booleanTypes, element.valueType)) {
					return 'Ext.ux.panel.DataElementBooleanContainer';
				}

				return 'Ext.ux.panel.DataElementIntegerContainer';
			};

			// add
			ux = dataElementSelected.insert(index, Ext.create(getUxType(element), {
				dataElement: element
			}));

            ux.isAttribute = element.isAttribute;
            ux.isProgramIndicator = element.isProgramIndicator;

			ux.removeDataElement = function(reset) {
				dataElementSelected.remove(ux);

				if (!dataElementSelected.hasDataElement(element.id)) {
                    if (!reset) {
                        dataElementsByStageStore.add(element);
                        dataElementsByStageStore.sort();
                    }

                    ns.app.aggregateLayoutWindow.removeDimension(element.id, ns.app.aggregateLayoutWindow.valueStore);
                    ns.app.queryLayoutWindow.removeDimension(element.id);
				}
			};

			ux.duplicateDataElement = function() {
				var index = dataElementSelected.getChildIndex(ux) + 1;
				addUxFromDataElement(element, index);
			};

			dataElementsByStageStore.removeAt(dataElementsByStageStore.findExact('id', element.id));

            return ux;
		};

        selectDataElements = function(items, layout) {
            var dataElements = [],
				allElements = [],
                aggWindow = ns.app.aggregateLayoutWindow,
                queryWindow = ns.app.queryLayoutWindow,
                includeKeys = ns.core.conf.valueType.tAggregateTypes,
                ignoreKeys = ['pe', 'ou'],
                recordMap = {
					'pe': {id: 'pe', name: 'Periods'},
					'ou': {id: 'ou', name: 'Organisation units'}
				},
                extendDim = function(dim) {
                    var md = ns.app.response.metaData,
                        dimConf = ns.core.conf.finals.dimension;

                    dim.id = dim.id || dim.dimension;
                    dim.name = dim.name || md.names[dim.dimension] || dimConf.objectNameMap[dim.dimension].name;

                    return dim;
                };

			// data element objects
            for (var i = 0, item; i < items.length; i++) {
				item = items[i];

                if (Ext.isString(item)) {
                    dataElements.push(dataElementsByStageStore.getById(item).data);
                }
                else if (Ext.isObject(item)) {
                    if (item.data) {
                        dataElements.push(item.data);
                    }
                    else {
                        dataElements.push(item);
                    }
                }
            }

            // expand if multiple filter
            for (var i = 0, element, a, numberOfElements; i < dataElements.length; i++) {
				element = dataElements[i];
				allElements.push(element);

				if (Ext.Array.contains(ns.core.conf.valueType.numericTypes, element.valueType) && element.filter) {
					a = element.filter.split(':');
					numberOfElements = a.length / 2;

					if (numberOfElements > 1) {
						a.shift();
						a.shift();

						for (var j = 1, newElement; j < numberOfElements; j++) {
							newElement = Ext.clone(element);
							newElement.filter = a.shift();
							newElement.filter += ':' + a.shift();

							allElements.push(newElement);
						}
					}
				}
			}

			// panel, store
            for (var i = 0, element, ux, store; i < allElements.length; i++) {
				element = allElements[i];
                element.name = element.name || element.displayName;
                recordMap[element.id] = element;

                // dont create ux if dim is selected as value
                if (element.id !== aggWindow.value.getValue()) {
                    ux = addUxFromDataElement(element);

                    if (layout) {
                        ux.setRecord(element);
                    }
                }

                store = Ext.Array.contains(includeKeys, element.valueType) || element.optionSet ? aggWindow.rowStore : aggWindow.fixedFilterStore;

                aggWindow.addDimension(element, store, valueStore);
                queryWindow.colStore.add(element);
			}

            // favorite
			if (layout && layout.dataType === finalsDataTypeConf.aggregated_values) {

                aggWindow.reset(true, true);

                // start end dates
				if (layout.startDate && layout.endDate) {
					aggWindow.fixedFilterStore.add({id: dimConf.startEndDate.value, name: dimConf.startEndDate.name});
				}

                // columns
				if (layout.columns) {
					for (var i = 0, record, dim; i < layout.columns.length; i++) {
                        dim = layout.columns[i];
                        record = recordMap[dim.dimension];

						//aggWindow.addDimension(record || extendDim(Ext.clone(dim)), aggWindow.colStore, null, true);
                        aggWindow.colStore.add(record || extendDim(Ext.clone(dim)));
					}
				}

                // rows
				if (layout.rows) {
					for (var i = 0, record, dim; i < layout.rows.length; i++) {
                        dim = layout.rows[i];
                        record = recordMap[dim.dimension];

						//aggWindow.addDimension(record || extendDim(Ext.clone(dim)), aggWindow.rowStore, null, true);
                        aggWindow.rowStore.add(record || extendDim(Ext.clone(dim)));
					}
				}

                // filters
				if (layout.filters) {
					for (var i = 0, store, record, dim; i < layout.filters.length; i++) {
                        dim = layout.filters[i];
						record = recordMap[dim.dimension];
						store = Ext.Array.contains(includeKeys, element.valueType) || element.optionSet ? aggWindow.filterStore : aggWindow.fixedFilterStore;

                        //aggWindow.addDimension(record || extendDim(Ext.clone(dim)), store, null, true);
                        store.add(record || extendDim(Ext.clone(dim)));
					}
				}

                // value
                if (layout.value && layout.aggregationType) {
                    aggWindow.setValueConfig(layout.value.id, layout.aggregationType);
                }

                // collapse data dimensions
                aggWindow.collapseDataDimensions.setValue(layout.collapseDataDimensions);
                aggWindow.onCollapseDataDimensionsChange(layout.collapseDataDimensions);
			}
        };

        programStagePanel = Ext.create('Ext.panel.Panel', {
            layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin-top:2px',
            items: [
                program,
                stage
            ]
        });

        data = Ext.create('Ext.panel.Panel', {
            title: '<div class="ns-panel-title-data">Data</div>',
            bodyStyle: 'padding:1px',
            hideCollapseTool: true,
            items: [
                programStagePanel,
                dataElementAvailable,
                dataElementSelected
            ],
            onExpand: function() {
				var h = ns.app.westRegion.hasScrollbar ?
					ns.core.conf.layout.west_scrollbarheight_accordion_indicator : ns.core.conf.layout.west_maxheight_accordion_indicator;

				accordion.setThisHeight(h);

                var msHeight = this.getHeight() - 28 - programStagePanel.getHeight() - 6;

                dataElementAvailable.setHeight(msHeight * 0.4);
                dataElementSelected.setHeight(msHeight * 0.6);

            },
            listeners: {
				added: function(cmp) {
					accordionPanels.push(cmp);
				},
                expand: function(cmp) {
                    cmp.onExpand();
                }
			}
        });

            // dates
        periodMode = Ext.create('Ext.form.field.ComboBox', {
            editable: false,
            valueField: 'id',
            displayField: 'name',
            queryMode: 'local',
            width: accBaseWidth,
            listConfig: {loadMask: false},
            style: 'padding-bottom:1px; border-bottom:1px solid #ddd; margin-bottom:1px',
            value: 'periods',
            store: {
                fields: ['id', 'name'],
                data: [
                    {id: 'periods', name: 'Fixed and relative periods'},
                    {id: 'dates', name: 'Start/end dates'}
                ]
            },
            reset: function() {
				onPeriodModeSelect('periods');
			},
            listeners: {
                select: function(cmp) {
                    onPeriodModeSelect(cmp.getValue());
                }
            }
        });

        onPeriodModeSelect = function(mode) {
			periodMode.setValue(mode);

            if (mode === 'dates') {
                startEndDate.show();
                periods.hide();

                ns.app.aggregateLayoutWindow.addDimension({id: dimConf.startEndDate.value, name: dimConf.startEndDate.name}, ns.app.aggregateLayoutWindow.fixedFilterStore);
                ns.app.aggregateLayoutWindow.removeDimension(dimConf.period.dimensionName);
            }
            else if (mode === 'periods') {
                startEndDate.hide();
                periods.show();

                ns.app.aggregateLayoutWindow.addDimension({id: dimConf.period.dimensionName, name: dimConf.period.name}, ns.app.aggregateLayoutWindow.colStore);
                ns.app.aggregateLayoutWindow.removeDimension(dimConf.startEndDate.value);
            }
        };

        getDateLink = function(text, fn, style) {
            return Ext.create('Ext.form.Label', {
                text: text,
                style: 'padding-left: 5px; width: 100%; ' + style || '',
                cls: 'ns-label-date',
                updateValue: fn,
                listeners: {
                    render: function(cmp) {
                        cmp.getEl().on('click', function() {
                            cmp.updateValue();
                        });
                    }
                }
            });
        };

        onDateFieldRender = function(c) {
            $('#' + c.inputEl.id).calendarsPicker({
                calendar: ns.core.init.calendar,
                dateFormat: ns.core.init.systemInfo.dateFormat
            });
        };

        startDate = Ext.create('Ext.form.field.Text', {
			fieldLabel: 'Start date',
			labelAlign: 'top',
			labelCls: 'ns-form-item-label-top ns-form-item-label-top-padding',
			labelSeparator: '',
            columnWidth: 0.5,
            height: 44,
            value: ns.core.init.calendar.formatDate(ns.core.init.systemInfo.dateFormat, ns.core.init.calendar.today().add(-3, 'm')),
            listeners: {
                render: function(c) {
                    onDateFieldRender(c);
                }
            }
        });

        endDate = Ext.create('Ext.form.field.Text', {
			fieldLabel: 'End date',
			labelAlign: 'top',
			labelCls: 'ns-form-item-label-top ns-form-item-label-top-padding',
			labelSeparator: '',
            columnWidth: 0.5,
            height: 44,
            style: 'margin-left: 1px',
            value: ns.core.init.calendar.formatDate(ns.core.init.systemInfo.dateFormat, ns.core.init.calendar.today()),
            listeners: {
                render: function(c) {
                    onDateFieldRender(c);
                }
            }
        });

        startEndDate = Ext.create('Ext.container.Container', {
            cls: 'ns-container-default',
            layout: 'column',
            hidden: true,
            items: [
                startDate,
                endDate
            ]
        });

            // relative periods
        onPeriodChange = function() {
            if ((period.isRelativePeriods() || fixedPeriodSelectedStore.getRange().length)) {
                ns.app.aggregateLayoutWindow.addDimension({id: dimConf.period.dimensionName, name: dimConf.period.name}, ns.app.aggregateLayoutWindow.colStore);
            }
            else {
                ns.app.aggregateLayoutWindow.removeDimension(dimConf.period.dimensionName);
            }
        };

        onCheckboxAdd = function(cmp) {
            if (cmp.xtype === 'checkbox') {
                checkboxes.push(cmp);
                relativePeriodCmpMap[cmp.relativePeriodId] = cmp;
            }
        };

        intervalListeners = {
            added: function(cmp) {
                onCheckboxAdd(cmp);
            },
            change: function() {
                if (relativePeriod.getRecords().length < 2) {
                    onPeriodChange();
                }
            }
        };

        weeks = Ext.create('Ext.container.Container', {
            defaults: {
                labelSeparator: '',
                style: 'margin-bottom:0',
                listeners: intervalListeners
            },
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
        });

        months = Ext.create('Ext.container.Container', {
            defaults: {
                labelSeparator: '',
                style: 'margin-bottom:0',
                listeners: intervalListeners
            },
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
                    boxLabel: NS.i18n.last_12_months,
                    checked: true
                }
            ]
        });

        biMonths = Ext.create('Ext.container.Container', {
            defaults: {
                labelSeparator: '',
                style: 'margin-bottom:0',
                listeners: intervalListeners
            },
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
        });

        quarters = Ext.create('Ext.container.Container', {
            defaults: {
                labelSeparator: '',
                style: 'margin-bottom:0',
                listeners: intervalListeners
            },
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
        });

        sixMonths = Ext.create('Ext.container.Container', {
            defaults: {
                labelSeparator: '',
                style: 'margin-bottom:0',
                listeners: intervalListeners
            },
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
        });

        financialYears = Ext.create('Ext.container.Container', {
            style: 'margin-top: 36px',
            defaults: {
                labelSeparator: '',
                style: 'margin-bottom:0',
                listeners: intervalListeners
            },
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
        });

        years = Ext.create('Ext.container.Container', {
            defaults: {
                labelSeparator: '',
                style: 'margin-bottom:0',
                listeners: intervalListeners
            },
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
        });

        relativePeriod = Ext.create('Ext.container.Container', {
            layout: 'column',
			hideCollapseTool: true,
			autoScroll: true,
			style: 'border:0 none',
			items: [
				{
					xtype: 'container',
                    columnWidth: 0.34,
                    style: 'margin-left: 8px',
                    defaults: {
                        style: 'margin-top: 4px'
                    },
					items: [
                        weeks,
						quarters,
                        years
					]
				},
				{
					xtype: 'container',
                    columnWidth: 0.33,
                    defaults: {
                        style: 'margin-top: 4px'
                    },
					items: [
						months,
						sixMonths
					]
				},
				{
					xtype: 'container',
                    columnWidth: 0.33,
                    defaults: {
                        style: 'margin-top: 4px'
                    },
					items: [
                        biMonths,
						financialYears
					]
				}
			],
            getRecords: function() {
                var a = [];

                for (var i = 0; i < checkboxes.length; i++) {
                    if (checkboxes[i].getValue()) {
                        a.push(checkboxes[i].relativePeriodId);
                    }
                }

                return a;
            }
		});

            // fixed periods
		fixedPeriodAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
            width: accBaseWidth / 2,
            height: 160,
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
                        onPeriodChange();
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowrightdouble.png',
					width: 22,
					handler: function() {
						ns.core.web.multiSelect.selectAll(fixedPeriodAvailable, fixedPeriodSelected, true);
                        onPeriodChange();
					}
				},
				' '
			],
			listeners: {
				afterrender: function() {
					this.boundList.on('itemdblclick', function() {
						ns.core.web.multiSelect.select(fixedPeriodAvailable, fixedPeriodSelected);
                        onPeriodChange();
					}, this);
				}
			}
		});

		fixedPeriodSelected = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-right',
            width: accBaseWidth / 2,
			height: 160,
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
                        onPeriodChange();
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowleft.png',
					width: 22,
					handler: function() {
						ns.core.web.multiSelect.unselect(fixedPeriodAvailable, fixedPeriodSelected);
                        onPeriodChange();
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
                        onPeriodChange();
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
            style: 'margin-right:1px; margin-bottom:1px',
            width: accBaseWidth - 62 - 62 - 2,
            valueField: 'id',
            displayField: 'name',
            emptyText: NS.i18n.select_period_type,
            editable: false,
            queryMode: 'remote',
            store: periodTypeStore,
            periodOffset: 0,
            listeners: {
                select: function(cmp) {
                    periodType.periodOffset = 0;
                    onPeriodTypeSelect();
                }
            }
        });

        prevYear = Ext.create('Ext.button.Button', {
            text: NS.i18n.prev_year,
            style: 'border-radius:1px; margin-right:1px',
            height: 24,
            width: 62,
            handler: function() {
                if (periodType.getValue()) {
                    periodType.periodOffset--;
                    onPeriodTypeSelect();
                }
            }
        });

        nextYear = Ext.create('Ext.button.Button', {
            text: NS.i18n.next_year,
            style: 'border-radius:1px',
            height: 24,
            width: 62,
            handler: function() {
                if (periodType.getValue()) {
                    periodType.periodOffset++;
                    onPeriodTypeSelect();
                }
            }
        });

        fixedPeriodSettings = Ext.create('Ext.container.Container', {
            layout: 'column',
            bodyStyle: 'border-style:none',
            style: 'margin-top:0px',
            items: [
                periodType,
                prevYear,
                nextYear
            ]
        });

        fixedPeriodAvailableSelected = Ext.create('Ext.container.Container', {
            layout: 'column',
            bodyStyle: 'border-style:none; padding-bottom:2px',
            items: [
                fixedPeriodAvailable,
                fixedPeriodSelected
            ]
        });

        periods = Ext.create('Ext.container.Container', {
            bodyStyle: 'border-style:none',
            getRecords: function() {
                var map = relativePeriodCmpMap,
                    selectedPeriods = [],
					records = [];

				fixedPeriodSelectedStore.each( function(r) {
					selectedPeriods.push(r.data.id);
				});

                for (var i = 0; i < selectedPeriods.length; i++) {
                    records.push({id: selectedPeriods[i]});
                }

				for (var rp in map) {
					if (map.hasOwnProperty(rp) && map[rp].getValue()) {
						records.push({id: map[rp].relativePeriodId});
					}
				}

				return records.length ? records : null;
            },
            getDimension: function() {
				return {
					dimension: 'pe',
					items: this.getRecords()
				};
			},
            items: [
                fixedPeriodSettings,
                fixedPeriodAvailableSelected,
                relativePeriod
            ]
        });

		period = Ext.create('Ext.panel.Panel', {
            title: '<div class="ns-panel-title-period">Periods</div>',
            bodyStyle: 'padding:1px',
            hideCollapseTool: true,
            width: accBaseWidth,
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
            reset: function() {
				this.resetRelativePeriods();
				this.resetFixedPeriods();
				this.resetStartEndDates();

				periodMode.reset();
			},
            isRelativePeriods: function() {
				var a = checkboxes;
				for (var i = 0; i < a.length; i++) {
					if (a[i].getValue()) {
						return true;
					}
				}
				return false;
			},
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

				for (var i = 0; i < checkboxes.length; i++) {
					if (checkboxes[i].getValue()) {
						config.items.push({
							id: checkboxes[i].relativePeriodId,
							name: ''
						});
					}
				}

				return config.items.length ? config : null;
			},
			resetRelativePeriods: function() {
				var a = checkboxes;
				for (var i = 0; i < a.length; i++) {
					a[i].setValue(false);
				}
			},
			resetFixedPeriods: function() {
				fixedPeriodAvailableStore.removeAll();
				fixedPeriodSelectedStore.removeAll();
				periodType.clearValue();
			},
			resetStartEndDates: function() {
				startDate.reset();
				endDate.reset();
			},
			isNoRelativePeriods: function() {
				var a = checkboxes;
				for (var i = 0; i < a.length; i++) {
					if (a[i].getValue()) {
						return false;
					}
				}
				return true;
			},
            items: [
                periodMode,
                startEndDate,
                periods
			],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				},
                expand: function(cmp) {
                    cmp.onExpand();
                }
			}
		});

            // organisation unit
		treePanel = Ext.create('Ext.tree.Panel', {
			cls: 'ns-tree',
			height: 436,
			width: accBaseWidth,
            bodyStyle: 'border:0 none',
			style: 'border-top: 1px solid #ddd; padding-top: 1px',
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
						fields: 'children[id,' + ns.core.init.namePropertyUrl + ',children::isNotEmpty|rename(hasChildren)&paging=false'
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
			listeners: {
				beforeitemexpand: function() {
					var rts = treePanel.recordsToSelect;

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

                    Ext.defer(function() {
                        data.expand();
                    }, 20);
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
			boxLabel: 'User org unit',
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
			width: accBaseWidth - toolWidth - 1,
			valueField: 'level',
			displayField: 'name',
			emptyText: NS.i18n.select_organisation_unit_levels,
			editable: false,
			hidden: true,
			store: {
				fields: ['id', 'name', 'level'],
				data: ns.core.init.organisationUnitLevels
			}
		});

		organisationUnitGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'ns-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: accBaseWidth - toolWidth - 1,
			valueField: 'id',
			displayField: 'name',
			emptyText: NS.i18n.select_organisation_unit_groups,
			editable: false,
			hidden: true,
			store: organisationUnitGroupStore
		});

        organisationUnitPanel = Ext.create('Ext.panel.Panel', {
			width: accBaseWidth - toolWidth - 1,
            layout: 'column',
            bodyStyle: 'border:0 none',
            items: [
                userOrganisationUnit,
                userOrganisationUnitChildren,
                userOrganisationUnitGrandChildren,
                organisationUnitLevel,
                organisationUnitGroup
            ]
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
			width: toolWidth,
			height: 24,
			menu: toolMenu
		});

		toolPanel = Ext.create('Ext.panel.Panel', {
			width: toolWidth,
			bodyStyle: 'border:0 none; text-align:right',
			style: 'margin-right:1px',
			items: tool
		});

        organisationUnit = Ext.create('Ext.panel.Panel', {
            title: '<div class="ns-panel-title-organisationunit">' + NS.i18n.organisation_units + '</div>',
            bodyStyle: 'padding:1px',
            hideCollapseTool: true,
            items: [
                {
                    layout: 'column',
                    bodyStyle: 'border:0 none;',
                    style: 'padding-bottom:1px',
                    items: [
                        toolPanel,
                        organisationUnitPanel
                    ]
                },
                treePanel
            ],
            onExpand: function() {
                var h = ns.app.westRegion.hasScrollbar ?
                    ns.core.conf.layout.west_scrollbarheight_accordion_organisationunit : ns.core.conf.layout.west_maxheight_accordion_organisationunit;
                accordion.setThisHeight(h);
                treePanel.setHeight(this.getHeight() - ns.core.conf.layout.west_fill_accordion_organisationunit);
            },
            listeners: {
				added: function(cmp) {
					accordionPanels.push(cmp);
				},
                expand: function(cmp) {
                    cmp.onExpand();
                }
			}
        });

		// dimensions

		getDimensionPanel = function(dimension, iconCls) {
			var	onSelect,
                availableStore,
				selectedStore,
				available,
				selected,
				panel,

				createPanel,
				getPanels;

            onSelect = function() {
                var aggWin = ns.app.aggregateLayoutWindow,
                    queryWin = ns.app.queryLayoutWindow;

                if (selectedStore.getRange().length) {
                    aggWin.addDimension({id: dimension.id, name: dimension.name}, aggWin.rowStore);
                    queryWin.addDimension({id: dimension.id, name: dimension.name}, queryWin.colStore);
                }
                else if (!selectedStore.getRange().length && aggWin.hasDimension(dimension.id)) {
                    aggWin.removeDimension(dimension.id);
                }
                else if (!selectedStore.getRange().length && queryWin.hasDimension(dimension.id)) {
                    queryWin.removeDimension(dimension.id);
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
					//indicatorSearch.hideFilter();
				},
				loadPage: function(filter, append) {
					var store = this,
						path;

					filter = filter || null;

					if (!append) {
						this.lastPage = null;
						this.nextPage = 1;
					}

					if (store.nextPage === store.lastPage) {
						return;
					}

					path = '/organisationUnitGroups.json?fields=id,' + ns.core.init.namePropertyUrl + '&filter=organisationUnitGroupSet.id:eq:' + dimension.id + (filter ? '&filter=name:ilike:' + filter : '');

					store.isPending = true;

					Ext.Ajax.request({
						url: ns.core.init.contextPath + '/api' + path,
						params: {
							page: store.nextPage,
							pageSize: 50
						},
						failure: function() {
							store.isPending = false;
						},
						success: function(r) {
							var response = Ext.decode(r.responseText),
								data = response.organisationUnitGroups || [],
								pager = response.pager;

							store.loadStore(data, pager, append);
						}
					});
				},
				loadStore: function(data, pager, append) {
					this.loadData(data, append);
					this.lastPage = this.nextPage;

					if (pager.pageCount > this.nextPage) {
						this.nextPage++;
					}

					this.isPending = false;
					ns.core.web.multiSelect.filterAvailable({store: availableStore}, {store: selectedStore});
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

			available = Ext.create('Ext.ux.form.MultiSelect', {
				cls: 'ns-toolbar-multiselect-left',
                width: accBaseWidth / 2,
				valueField: 'id',
				displayField: 'name',
				store: availableStore,
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
							ns.core.web.multiSelect.select(available, selected);
						}
					},
					{
						xtype: 'button',
						icon: 'images/arrowrightdouble.png',
						width: 22,
						handler: function() {
							ns.core.web.multiSelect.selectAll(available, selected);
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
                width: accBaseWidth / 2,
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
					}
				],
				listeners: {
					afterrender: function() {
						this.boundList.on('itemdblclick', function() {
							ns.core.web.multiSelect.unselect(available, selected);
						}, this);
					}
				}
			});

			dimensionIdAvailableStoreMap[dimension.id] = availableStore;
			dimensionIdSelectedStoreMap[dimension.id] = selectedStore;

			//availableStore.on('load', function() {
				//ns.core.web.multiSelect.filterAvailable(available, selected);
			//});

			panel = {
				xtype: 'panel',
				title: '<div class="' + iconCls + '">' + dimension.name + '</div>',
				hideCollapseTool: true,
				availableStore: availableStore,
				selectedStore: selectedStore,
				getDimension: function() {
					var config = {
						dimension: dimension.id,
						items: []
					};

					selectedStore.each( function(r) {
						config.items.push({id: r.data.id});
					});

					return config.items.length ? config : null;
				},
				onExpand: function() {
					if (!availableStore.isLoaded) {
						availableStore.loadPage();
					}

					var h = ns.app.westRegion.hasScrollbar ?
						ns.core.conf.layout.west_scrollbarheight_accordion_dataset : ns.core.conf.layout.west_maxheight_accordion_dataset;
					accordion.setThisHeight(h);
					ns.core.web.multiSelect.setHeight(
						[available, selected],
						this,
						ns.core.conf.layout.west_fill_accordion_dataset
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

            // accordion
        accordionBody = Ext.create('Ext.panel.Panel', {
			layout: 'accordion',
			activeOnTop: true,
			cls: 'ns-accordion',
			bodyStyle: 'border:0 none',
			height: 700,
			items: function() {
                var panels = [
                    data,
                    period,
                    organisationUnit
                ],
				dims = Ext.clone(ns.core.init.dimensions);

				panels = panels.concat(getDimensionPanels(dims, 'ns-panel-title-dimension'));

				last = panels[panels.length - 1];
				last.cls = 'ns-accordion-last';

				return panels;
            }(),
            listeners: {
                afterrender: function() { // nasty workaround, should be fixed
                    //organisationUnit.expand();
                    //period.expand();
                    //data.expand();
                }
            }
		});

		// functions

		reset = function(skipTree) {

			// components
            program.clearValue();
            stage.clearValue();

            dataElementsByStageStore.removeAll();
            dataElementSelected.removeAll();

            dataElementSearch.hideFilter();

            startDate.reset();
            endDate.reset();

			toolMenu.clickHandler(toolMenu.menuValue);

			if (!skipTree) {
				treePanel.reset();
			}

			userOrganisationUnit.setValue(false);
			userOrganisationUnitChildren.setValue(false);
			userOrganisationUnitGrandChildren.setValue(false);

			organisationUnitLevel.clearValue();
			organisationUnitGroup.clearValue();

			// layer options
			//if (layer.labelWindow) {
				//layer.labelWindow.destroy();
				//layer.labelWindow = null;
			//}
		};

        setGui = function(layout, response, updateGui) {

			// state
			ns.app.downloadButton.enable();

			if (layout.id) {
				ns.app.shareButton.enable();
			}

            ns.app.statusBar.setStatus(layout, response);

			// set gui
			if (updateGui) {
				setLayout(layout);
			}
		};

		getView = function(config) {
			var panels = ns.app.accordion.panels,
                view = {},
				dataType = ns.app.typeToolbar.getType(),
				layoutWindow = ns.app.viewport.getLayoutWindow(dataType),
				map = {},
				columns = [],
				rows = [],
				filters = [],
                values = [],
                addAxisDimension,
                store,
                data,
				a;

			view.dataType = dataType;
            view.program = program.getRecord();
            view.programStage = stage.getRecord();

            if (!(view.dataType && view.program && view.programStage)) {
                return;
            }

            // dy
            map['dy'] = [{dimension: 'dy'}];

			// pe
            if (periodMode.getValue() === 'dates') {
                view.startDate = startDate.getSubmitValue();
                view.endDate = endDate.getSubmitValue();

                if (!(view.startDate && view.endDate)) {
                    return;
                }

                map['pe'] = [{dimension: 'pe'}];
            }
            else if (periodMode.getValue() === 'periods') {
				map['pe'] = [periods.getDimension()];
			}

			// ou
			map['ou'] = [treePanel.getDimension()];

            // data items
            for (var i = 0, record; i < dataElementSelected.items.items.length; i++) {
                record = dataElementSelected.items.items[i].getRecord();

                map[record.dimension] = map[record.dimension] || [];

                map[record.dimension].push(record);
            }

            // dynamic dimensions data
            for (var i = 0, panel, dim, dimName; i < panels.length; i++) {
                panel = panels[i];

                if (panel.getDimension) {
                    dim = panel.getDimension();

                    if (dim && !map.hasOwnProperty(dim.dimension)) {
                        map[dim.dimension] = [dim];
                    }
                }
            }

            // other
            map['longitude'] = [{dimension: 'longitude'}];
            map['latitude'] = [{dimension: 'latitude'}];

            addAxisDimension = function(a, axis) {
                if (a.length) {
                    if (a.length === 1) {
                        axis.push(a[0]);
                    }
                    else {
                        var dim;

                        for (var i = 0; i < a.length; i++) {
                            if (!dim) { //todo ??
                                dim = a[i];
                            }
                            else {
                                dim.filter += ':' + a[i].filter;
                            }
                        }

                        axis.push(dim);
                    }
                }
            };

            // columns
            store = layoutWindow.colStore;

            if (store) {
                data = store.snapshot || store.data;

                data.each(function(item) {
                    addAxisDimension(map[item.data.id] || [], columns);
                });
            }

            // rows
            store = layoutWindow.rowStore;

            if (store) {
                data = store.snapshot || store.data;

                data.each(function(item) {
                    addAxisDimension(map[item.data.id] || [], rows);
                });
            }

            // filters
            store = layoutWindow.filterStore;

            if (store) {
                data = store.snapshot || store.data;

                data.each(function(item) {
                    addAxisDimension(map[item.data.id] || [], filters);
                });
            }

            // fixed filters
            store = layoutWindow.fixedFilterStore;

            if (store) {
                data = store.snapshot || store.data;

                data.each(function(item) {
                    addAxisDimension(map[item.data.id] || [], filters);
                });
            }

			if (columns.length) {
				view.columns = columns;
			}
			if (rows.length) {
				view.rows = rows;
			}
			if (filters.length) {
				view.filters = filters;
			}

            // value, aggregation type
            Ext.apply(view, layoutWindow.getValueConfig());

			return view;
		};

		validateView = function(view) {
			if (!(Ext.isArray(view.rows) && view.rows.length && Ext.isString(view.rows[0].dimension) && Ext.isArray(view.rows[0].items) && view.rows[0].items.length)) {
				NS.logg.push([view.rows, layer.id + '.rows: dimension array']);
				ns.alert('No organisation units selected');
				return false;
			}

			return view;
		};

		accordion = Ext.create('Ext.panel.Panel', {
			bodyStyle: 'border-style:none; padding:1px; padding-bottom:0; overflow-y:scroll;',
            accordionBody: accordionBody,
			items: accordionBody,
			panels: accordionPanels,
            expandInitPanels: function() {
                organisationUnit.expand();
                //period.expand();
                //data.expand();
            },
			map: layer ? layer.map : null,
			layer: layer ? layer : null,
			menu: layer ? layer.menu : null,

			setThisHeight: function(mx) {
				var settingsHeight = 41,
					containerHeight = settingsHeight + (this.panels.length * 28) + mx,
					accordionHeight = ns.app.westRegion.getHeight() - settingsHeight - ns.core.conf.layout.west_fill,
                    accordionBodyHeight;

				if (ns.app.westRegion.hasScrollbar) {
                    accordionBodyHeight = containerHeight - settingsHeight - ns.core.conf.layout.west_fill;
				}
				else {
                    accordionBodyHeight = (accordionHeight > containerHeight ? containerHeight : accordionHeight) - ns.core.conf.layout.west_fill;
				}

                this.setHeight(accordionHeight);
                accordionBody.setHeight(accordionBodyHeight);
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
			getParentGraphMap: function() {
				return treePanel.getParentGraphMap();
			},

			accordionBody: accordionBody,
			panels: accordionPanels,
            treePanel: treePanel,

			reset: reset,
			setGui: setGui,
			getView: getView,

            onTypeClick: onTypeClick,

            getUxArray: function(id) {
                return dataElementSelected.getUxArrayById(id);
            },

            listeners: {
                added: function() {
					ns.app.accordion = this;
				}
            }
		});

		return accordion;
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

			// mouse events
			web.events = web.events || {};

			web.events.setValueMouseHandlers = function(layout, response, uuidDimUuidsMap, uuidObjectMap) {
				var valueEl;

				for (var key in uuidDimUuidsMap) {
					if (uuidDimUuidsMap.hasOwnProperty(key)) {
						valueEl = Ext.get(key);

						if (parseFloat(valueEl.dom.textContent)) {
							valueEl.dom.onValueMouseClick = web.events.onValueMouseClick;
							valueEl.dom.onValueMouseOver = web.events.onValueMouseOver;
							valueEl.dom.onValueMouseOut = web.events.onValueMouseOut;
							valueEl.dom.layout = layout;
							valueEl.dom.response = response;
							valueEl.dom.uuidDimUuidsMap = uuidDimUuidsMap;
							valueEl.dom.uuidObjectMap = uuidObjectMap;
							valueEl.dom.setAttribute('onclick', 'this.onValueMouseClick(this.layout, this.response, this.uuidDimUuidsMap, this.uuidObjectMap, this.id);');
							valueEl.dom.setAttribute('onmouseover', 'this.onValueMouseOver(this);');
							valueEl.dom.setAttribute('onmouseout', 'this.onValueMouseOut(this);');
						}
					}
				}
			};

			web.events.onValueMouseClick = function(layout, response, uuidDimUuidsMap, uuidObjectMap, uuid) {
				var uuids = uuidDimUuidsMap[uuid],
					layoutConfig = Ext.clone(layout),
					parentGraphMap = ns.app.viewport.treePanel.getParentGraphMap(),
					objects = [],
					menu;

				// modify layout dimension items based on uuid objects

				// get objects
				for (var i = 0; i < uuids.length; i++) {
					objects.push(uuidObjectMap[uuids[i]]);
				}

				// clear layoutConfig dimension items
				for (var i = 0, a = Ext.Array.clean([].concat(layoutConfig.columns || [], layoutConfig.rows || [])); i < a.length; i++) {
					a[i].items = [];
				}

				// add new items
				for (var i = 0, obj, axis; i < objects.length; i++) {
					obj = objects[i];

					axis = obj.axis === 'col' ? layoutConfig.columns || [] : layoutConfig.rows || [];

					if (axis.length) {
						axis[obj.dim].items.push({
							id: obj.id,
							name: response.metaData.names[obj.id]
						});
					}
				}

				// parent graph map
				layoutConfig.parentGraphMap = {};

				for (var i = 0, id; i < objects.length; i++) {
					id = objects[i].id;

					if (parentGraphMap.hasOwnProperty(id)) {
						layoutConfig.parentGraphMap[id] = parentGraphMap[id];
					}
				}

				// menu
				menu = Ext.create('Ext.menu.Menu', {
					shadow: true,
					showSeparator: false,
					items: [
						{
							text: 'Open selection as chart' + '&nbsp;&nbsp;', //i18n
							iconCls: 'ns-button-icon-chart',
							param: 'chart',
							handler: function() {
								web.storage.session.set(layoutConfig, 'analytical', init.contextPath + '/dhis-web-visualizer/index.html?s=analytical');
							},
							listeners: {
								render: function() {
									this.getEl().on('mouseover', function() {
										web.events.onValueMenuMouseHover(uuidDimUuidsMap, uuid, 'mouseover', 'chart');
									});

									this.getEl().on('mouseout', function() {
										web.events.onValueMenuMouseHover(uuidDimUuidsMap, uuid, 'mouseout', 'chart');
									});
								}
							}
						},
						{
							text: 'Open selection as map' + '&nbsp;&nbsp;', //i18n
							iconCls: 'ns-button-icon-map',
							param: 'map',
							disabled: true,
							handler: function() {
								web.storage.session.set(layoutConfig, 'analytical', init.contextPath + '/dhis-web-mapping/index.html?s=analytical');
							},
							listeners: {
								render: function() {
									this.getEl().on('mouseover', function() {
										web.events.onValueMenuMouseHover(uuidDimUuidsMap, uuid, 'mouseover', 'map');
									});

									this.getEl().on('mouseout', function() {
										web.events.onValueMenuMouseHover(uuidDimUuidsMap, uuid, 'mouseout', 'map');
									});
								}
							}
						}
					]
				});

				menu.showAt(function() {
					var el = Ext.get(uuid),
						xy = el.getXY();

					xy[0] += el.getWidth() - 5;
					xy[1] += el.getHeight() - 5;

					return xy;
				}());
			};

			web.events.onValueMouseOver = function(uuid) {
				Ext.get(uuid).addCls('highlighted');
			};

			web.events.onValueMouseOut = function(uuid) {
				Ext.get(uuid).removeCls('highlighted');
			};

			web.events.onValueMenuMouseHover = function(uuidDimUuidsMap, uuid, event, param) {
				var dimUuids;

				// dimension elements
				if (param === 'chart') {
					if (Ext.isString(uuid) && Ext.isArray(uuidDimUuidsMap[uuid])) {
						dimUuids = uuidDimUuidsMap[uuid];

						for (var i = 0, el; i < dimUuids.length; i++) {
							el = Ext.get(dimUuids[i]);

							if (el) {
								if (event === 'mouseover') {
									el.addCls('highlighted');
								}
								else if (event === 'mouseout') {
									el.removeCls('highlighted');
								}
							}
						}
					}
				}
			};

			web.events.setColumnHeaderMouseHandlers = function(layout, response, xResponse) {
				if (Ext.isArray(xResponse.sortableIdObjects)) {
					for (var i = 0, obj, el; i < xResponse.sortableIdObjects.length; i++) {
						obj = xResponse.sortableIdObjects[i];
						el = Ext.get(obj.uuid);

						el.dom.layout = layout;
						el.dom.response = response;
						el.dom.xResponse = xResponse;
						el.dom.metaDataId = obj.id;
						el.dom.onColumnHeaderMouseClick = web.events.onColumnHeaderMouseClick;
						el.dom.onColumnHeaderMouseOver = web.events.onColumnHeaderMouseOver;
						el.dom.onColumnHeaderMouseOut = web.events.onColumnHeaderMouseOut;

						el.dom.setAttribute('onclick', 'this.onColumnHeaderMouseClick(this.layout, this.response, this.metaDataId)');
						el.dom.setAttribute('onmouseover', 'this.onColumnHeaderMouseOver(this)');
						el.dom.setAttribute('onmouseout', 'this.onColumnHeaderMouseOut(this)');
					}
				}
			};

			web.events.onColumnHeaderMouseClick = function(layout, response, id) {
				if (layout.sorting && layout.sorting.id === id) {
					layout.sorting.direction = support.prototype.str.toggleDirection(layout.sorting.direction);
				}
				else {
					layout.sorting = {
						id: id,
						direction: 'ASC'
					};
				}

                if (layout.dataType === finalsDataTypeConf.aggregated_values) {
                    web.report.createReport(layout, response);
                }
                else if (layout.dataType === finalsDataTypeConf.individual_cases) {
                    web.report.getData(layout);
                }
			};

			web.events.onColumnHeaderMouseOver = function(el) {
				Ext.get(el).addCls('pointer highlighted');
			};

			web.events.onColumnHeaderMouseOut = function(el) {
				Ext.get(el).removeCls('pointer highlighted');
			};

			// report
			web.report = web.report || {};

			web.report.getLayoutConfig = function() {
                var view = ns.app.accordion.getView();

                if (!view) {
                    return;
                }

                if (view.dataType === finalsDataTypeConf.aggregated_values) {
                    Ext.applyIf(view, ns.app.aggregateOptionsWindow.getOptions());
                    Ext.applyIf(view, ns.app.aggregateLayoutWindow.getOptions());

                    // if order and limit -> sort
                    if (view.sortOrder && view.topLimit) {
                        view.sorting = {
                            id: 1,
                            direction: view.sortOrder == 1 ? 'DESC' : 'ASC'
                        };
                    }
                }

                if (view.dataType === finalsDataTypeConf.individual_cases) {
                    Ext.applyIf(view, ns.app.queryOptionsWindow.getOptions());

                    view.paging = {
                        page: ns.app.statusBar.getCurrentPage(),
                        pageSize: 100
                    };
                }

                return view;
            };

			web.report.loadReport = function(id) {
				if (!Ext.isString(id)) {
					ns.alert('Invalid report id');
					return;
				}

				Ext.Ajax.request({
					url: init.contextPath + '/api/eventReports/' + id + '.json?fields=' + conf.url.analysisFields.join(','),
					failure: function(r) {
						web.mask.hide(ns.app.centerRegion);

                        r = Ext.decode(r.responseText);

                        if (Ext.Array.contains([403], parseInt(r.httpStatusCode))) {
                            r.message = NS.i18n.you_do_not_have_access_to_all_items_in_this_favorite || r.message;
                        }

                        ns.alert(r);
					},
					success: function(r) {
						var config = Ext.decode(r.responseText);

						// sync
						config.showRowTotals = config.rowTotals;
						delete config.rowTotals;

						config.showColTotals = config.colTotals;
						delete config.colTotals;

						config.showColSubTotals = config.colSubTotals;
						delete config.colSubTotals;

						config.showRowSubTotals = config.rowSubTotals;
						delete config.rowSubTotals;

						if (config.startDate) {
							config.startDate = config.startDate.substr(0,10);
						}

						if (config.endDate) {
							config.endDate = config.endDate.substr(0,10);
						}

						config.paging = {
							page: 1,
							pageSize: 100
						};

						if (config.topLimit && config.sortOrder) {
							config.sorting = {
								id: 1,
								direction: config.sortOrder == 1 ? 'DESC' : 'ASC'
							};
						}

						web.report.getData(config, true);
					}
				});
			};

			web.report.getData = function(layout, isUpdateGui) {
				var paramString = web.analytics.getParamString(layout);

				// show mask
				web.mask.show(ns.app.centerRegion);

                // timing
                ns.app.dateData = new Date();

				Ext.Ajax.request({
					url: ns.core.init.contextPath + paramString,
					disableCaching: false,
					scope: this,
					failure: function(r) {
						web.mask.hide(ns.app.centerRegion);

                        ns.alert(r);
					},
					success: function(r) {
                        ns.app.dateCreate = new Date();

                        var response = api.response.Response(Ext.decode(r.responseText));

                        // add to dimConf, TODO
                        for (var i = 0, map = dimConf.objectNameMap, header; i < response.headers.length; i++) {
                            header = response.headers[i];

                            map[header.name] = map[header.name] || {
                                id: header.name,
                                dimensionName: header.name,
                                name: header.column
                            };
                        }

                        web.mask.show(ns.app.centerRegion, 'Creating table..');

                        ns.app.paramString = paramString;

                        web.report.createReport(layout, response, isUpdateGui);
					}
				});
			};

			web.report.createReport = function(layout, response, isUpdateGui) {
				var map = {},
                    getOptionSets;

                getOptionSets = function(xResponse, callbackFn) {
                    var optionSetHeaders = [];

                    for (var i = 0; i < xResponse.headers.length; i++) {
                        if (xResponse.headers[i].optionSet) {
                            optionSetHeaders.push(xResponse.headers[i]);
                        }
                    }

                    if (optionSetHeaders.length) {
                        var callbacks = 0,
                            optionMap = {},
                            getOptions,
                            fn;

                        fn = function() {
                            if (++callbacks === optionSetHeaders.length) {
                                xResponse.metaData.optionNames = optionMap;
                                callbackFn();
                            }
                        };

                        getOptions = function(optionSetId, dataElementId) {
                            dhis2.er.store.get('optionSets', optionSetId).done( function(obj) {
                                Ext.apply(optionMap, support.prototype.array.getObjectMap(obj.options, 'code', 'name', dataElementId));
                                fn();
                            });
                        };

                        // execute
                        for (var i = 0, header, optionSetId, dataElementId; i < optionSetHeaders.length; i++) {
                            header = optionSetHeaders[i];
                            optionSetIds = Ext.Array.from(header.optionSet);
                            dataElementId = header.name;

                            for (var j = 0; j < optionSetIds.length; j++) {
                                getOptions(optionSetIds[j], dataElementId);
                            }
                        }
                    }
                    else {
                        callbackFn();
                    }
                };

				map[finalsDataTypeConf.aggregated_values] = function() {
					var xLayout,
                        xResponse,
						xColAxis,
						xRowAxis,
						table,
						getSXLayout,
						getXResponse,
                        getReport;

                    getReport = function() {
                        var getHtml = function(xLayout, xResponse) {
                            xColAxis = service.layout.getExtendedAxis(xLayout, 'col');
                            xRowAxis = service.layout.getExtendedAxis(xLayout, 'row');

                            return web.report.aggregate.getHtml(xLayout, xResponse, xColAxis, xRowAxis);
                        };

                        table = getHtml(xLayout, xResponse);

                        if (table.tdCount > 20000 || (layout.hideEmptyRows && table.tdCount > 10000)) {
                            ns.alert('Table has too many cells. Please reduce the table and try again.');
                            web.mask.hide(ns.app.centerRegion);
                            return;
                        }

                        if (layout.sorting) {
                            xResponse = web.report.aggregate.sort(xLayout, xResponse, xColAxis);
                            xLayout = service.layout.getSyncronizedXLayout(layout, xLayout, xResponse);
                            table = getHtml(xLayout, xResponse);
                        }

                        web.mask.show(ns.app.centerRegion, 'Rendering table..');

                        // timing
                        ns.app.dateRender = new Date();

                        ns.app.centerRegion.removeAll(true);
                        ns.app.centerRegion.update(table.html);

                        // timing
                        ns.app.dateTotal = new Date();

                        // after render
                        //ns.app.layout = layout;
                        ns.app.xLayout = xLayout;
                        //ns.app.response = response;
                        ns.app.xResponse = xResponse;
                        ns.app.xColAxis = xColAxis;
                        ns.app.xRowAxis = xRowAxis;
                        ns.app.uuidDimUuidsMap = table.uuidDimUuidsMap;
                        ns.app.uuidObjectMap = Ext.applyIf((xColAxis ? xColAxis.uuidObjectMap : {}), (xRowAxis ? xRowAxis.uuidObjectMap : {}));

                        if (NS.isSessionStorage) {
                            //web.events.setValueMouseHandlers(layout, response || xResponse, ns.app.uuidDimUuidsMap, ns.app.uuidObjectMap);
                            web.events.setColumnHeaderMouseHandlers(layout, response, xResponse);
                            web.storage.session.set(layout, 'eventtable');
                        }

                        web.mask.hide(ns.app.centerRegion);

                        if (NS.isDebug) {
                            console.log("Number of cells", table.tdCount);
                            console.log("DATA", (ns.app.dateCreate - ns.app.dateData) / 1000);
                            console.log("CREATE", (ns.app.dateRender - ns.app.dateCreate) / 1000);
                            console.log("RENDER", (ns.app.dateTotal - ns.app.dateRender) / 1000);
                            console.log("TOTAL", (ns.app.dateTotal - ns.app.dateData) / 1000);
                            console.log("layout", layout);
                            console.log("response", response);
                            console.log("xResponse", xResponse);
                            console.log("xLayout", xLayout);
                            console.log("core", ns.core);
                            console.log("app", ns.app);
                        }

                        // data statistics
                        Ext.Ajax.request({
                            url: ns.core.init.contextPath + '/api/dataStatistics?eventType=EVENT_REPORT_VIEW' + (ns.app.layout.id ? '&favorite=' + ns.app.layout.id : ''),
                            method: 'POST'
                        });
                    };

                    getSXLayout = function() {
                        xLayout = service.layout.getSyncronizedXLayout(layout, xLayout, xResponse);

                        getReport();
                    };

                    getXResponse = function() {
                        xLayout = service.layout.getExtendedLayout(layout);
                        xResponse = service.response.aggregate.getExtendedResponse(xLayout, response);

                        getOptionSets(xResponse, getSXLayout);
                    };

                    // execute
					response = response || ns.app.response;

                    getXResponse();
				};

				map[finalsDataTypeConf.individual_cases] = function() {
					var xResponse,
                        getReport;

                    getReport = function() {
                        table = web.report.query.getHtml(layout, xResponse);

                        //if (layout.sorting) {
                            //xResponse = web.report.query.sort(layout, xResponse);
                            //table = web.report.query.getHtml(layout, xResponse);
                        //}

                        ns.app.centerRegion.removeAll(true);
                        ns.app.centerRegion.update(table.html);

                        // after render
                        //ns.app.layout = layout;
                        //ns.app.response = response;
                        ns.app.xResponse = xResponse;

                        if (NS.isSessionStorage) {
                            web.events.setColumnHeaderMouseHandlers(layout, response, xResponse);
                        }

                        web.mask.hide(ns.app.centerRegion);
                    };

                    // execute
                    xResponse = service.response.query.getExtendedResponse(layout, response);

                    getOptionSets(xResponse, getReport);
				};

                // success
                ns.app.layout = layout;
                ns.app.response = response;

                ns.app.accordion.setGui(layout, response, isUpdateGui);

                // no data
                if (!response.rows.length) {
                    ns.app.centerRegion.removeAll(true);
                    ns.app.centerRegion.update('');
                    ns.app.centerRegion.add({
                        bodyStyle: 'padding:20px; border:0 none; background:transparent; color: #555; font-size:11px',
                        html: NS.i18n.no_values_found_for_current_selection + '.'
                    });

                    web.mask.hide(ns.app.centerRegion);

                    return;
                }

				map[layout.dataType]();
			};
		}());
	};

	// viewport
	createViewport = function() {
        var eventReportStore,

            caseButton,
			aggregateButton,
			paramButtonMap = {},
			typeToolbar,
            onTypeClick,

			widget,
			accordion,
			westRegion,
            layoutButton,
            optionsButton,
            favoriteButton,
            getParamString,
            openTableLayoutTab,
            downloadButton,
            interpretationItem,
            pluginItem,
            shareButton,
            aboutButton,
            statusBar,
            defaultButton,
            centerRegion,
            getLayoutWindow,
            viewport,

            scrollbarWidth = Ext.isWebKit ? 8 : (Ext.isLinux && Ext.isGecko ? 13 : 17);

		ns.app.stores = ns.app.stores || {};

		eventReportStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'lastUpdated', 'access'],
			proxy: {
				type: 'ajax',
				reader: {
					type: 'json',
					root: 'eventReports'
				}
			},
			isLoaded: false,
			pageSize: 10,
			page: 1,
			defaultUrl: ns.core.init.contextPath + '/api/eventReports.json?fields=id,displayName|rename(name),access',
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
		ns.app.stores.eventReport = eventReportStore;

		// viewport

        aggregateButton = Ext.create('Ext.button.Button', {
            width: 223,
			param: finalsDataTypeConf.aggregated_values,
            text: '<b>Aggregated values</b><br/>Show aggregated event report',
            style: 'margin-right:1px',
            pressed: true,
            listeners: {
				mouseout: function(cmp) {
					cmp.addCls('x-btn-default-toolbar-small-over');
				}
			}
        });
        paramButtonMap[aggregateButton.param] = aggregateButton;

		caseButton = Ext.create('Ext.button.Button', {
            width: 224,
			param: finalsDataTypeConf.individual_cases,
            text: '<b>Events</b><br/>Show individual event overview',
            style: 'margin-right:1px',
			listeners: {
				mouseout: function(cmp) {
					cmp.addCls('x-btn-default-toolbar-small-over');
				}
			}
        });
        paramButtonMap[caseButton.param] = caseButton;

		typeToolbar = Ext.create('Ext.toolbar.Toolbar', {
			style: 'padding:1px; background:#fbfbfb; border:0 none',
            height: 41,
            getType: function() {
				return aggregateButton.pressed ? aggregateButton.param : caseButton.param;
			},
            setType: function(dataType) {
                var button = paramButtonMap[dataType];

                if (button) {
                    button.toggle(true);
                }
            },
            defaults: {
                height: 40,
                toggleGroup: 'mode',
				cls: 'x-btn-default-toolbar-small-over',
                handler: function(b) {
					onTypeClick(b);
				}
			},
			items: [
				aggregateButton,
				caseButton
			],
			listeners: {
				added: function() {
					ns.app.typeToolbar = this;
				}
			}
		});

		onTypeClick = function(button) {
			if (!button.pressed) {
				button.toggle();
			}

            ns.app.accordion.onTypeClick(typeToolbar.getType());

			update();
		};

		accordion = LayerWidgetEvent();

		update = function() {
			var config = ns.core.web.report.getLayoutConfig();

			if (!config) {
				return;
			}

			// state
            ns.app.viewport.getLayoutWindow(config.dataType).saveState();

			ns.core.web.report.getData(config, false);
		};

		westRegion = Ext.create('Ext.panel.Panel', {
			region: 'west',
			preventHeader: true,
			collapsible: true,
			collapseMode: 'mini',
			width: ns.core.conf.layout.west_width + scrollbarWidth,
			items: [
				typeToolbar,
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
                getLayoutWindow(typeToolbar.getType()).show();
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
                getOptionsWindow(typeToolbar.getType()).show();
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

		openTableLayoutTab = function(type, isNewTab) {
			if (ns.core.init.contextPath && ns.app.paramString) {
				var colDimNames = Ext.clone(ns.app.xLayout.columnDimensionNames),
					colObjNames = ns.app.xLayout.columnObjectNames,
					rowDimNames = Ext.clone(ns.app.xLayout.rowDimensionNames),
					rowObjNames = ns.app.xLayout.rowObjectNames,
					dc = ns.core.conf.finals.dimension.operand.objectName,
					co = ns.core.conf.finals.dimension.category.dimensionName,
					columnNames = Ext.Array.clean([].concat(colDimNames, (Ext.Array.contains(colObjNames, dc) ? co : []))),
					rowNames = Ext.Array.clean([].concat(rowDimNames, (Ext.Array.contains(rowObjNames, dc) ? co : []))),
					url = '';

				url += ns.core.init.contextPath + '/api/analytics.' + type + getParamString();
				url += '&tableLayout=true';
				url += '&columns=' + columnNames.join(';');
				url += '&rows=' + rowNames.join(';');
				url += ns.app.layout.hideEmptyRows ? '&hideEmptyRows=true' : '';
				url += ns.app.layout.hideNaData ? '&hideNaData=true' : '';

				window.open(url, isNewTab ? '_blank' : '_top');
			}
		};

		downloadButton = Ext.create('Ext.button.Button', {
			text: 'Download',
			disabled: true,
			menu: {
				cls: 'ns-menu',
				shadow: false,
				showSeparator: false,
				items: [
					{
						xtype: 'label',
						text: NS.i18n.plain_data_sources,
						style: 'padding:7px 5px 5px 7px; font-weight:bold'
					},
					{
						text: 'HTML',
						iconCls: 'ns-menu-item-datasource',
						handler: function() {
							if (ns.core.init.contextPath && ns.app.paramString) {
								window.open(ns.core.init.contextPath + ns.core.web.analytics.getParamString(ns.app.layout, 'html+css', true), '_blank');
							}
						}
					},
					{
						text: 'JSON',
						iconCls: 'ns-menu-item-datasource',
						handler: function() {
							if (ns.core.init.contextPath && ns.app.paramString) {
								window.open(ns.core.init.contextPath + ns.core.web.analytics.getParamString(ns.app.layout, 'json', true), '_blank');
							}
						}
					},
					{
						text: 'XML',
						iconCls: 'ns-menu-item-datasource',
						handler: function() {
							if (ns.core.init.contextPath && ns.app.paramString) {
								window.open(ns.core.init.contextPath + ns.core.web.analytics.getParamString(ns.app.layout, 'xml', true), '_blank');
							}
						}
					},
					{
						text: 'Microsoft Excel',
						iconCls: 'ns-menu-item-datasource',
						handler: function() {
							if (ns.core.init.contextPath && ns.app.paramString) {
								window.open(ns.core.init.contextPath + ns.core.web.analytics.getParamString(ns.app.layout, 'xls', true), '_blank');
							}
						}
					},
					{
						text: 'CSV',
						iconCls: 'ns-menu-item-datasource',
						handler: function() {
							if (ns.core.init.contextPath && ns.app.paramString) {
								window.open(ns.core.init.contextPath + ns.core.web.analytics.getParamString(ns.app.layout, 'csv', true), '_blank');
							}
						}
					}
				],
				listeners: {
					added: function() {
						ns.app.downloadButton = this;
					},
					afterrender: function() {
						this.getEl().addCls('ns-toolbar-btn-menu');
					}
				}
			},
			listeners: {
				added: function() {
					ns.app.downloadButton = this;
				}
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
                var url = ns.core.init.contextPath + '/dhis-web-event-reports/index.html?id=' + ns.app.layout.id,
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
                var url = ns.core.init.contextPath + '/api/eventReports/' + ns.app.layout.id + '/data',
                    textField,
                    window;

                textField = Ext.create('Ext.form.field.Text', {
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>'
                });

				window = Ext.create('Ext.window.Window', {
					title: 'API link',
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
				//interpretationItem.xable();
				//pluginItem.xable();
				favoriteUrlItem.xable();
				//apiUrlItem.xable();
			},
			menu: {
				cls: 'ns-menu',
				shadow: false,
				showSeparator: false,
				items: [
					//interpretationItem,
					//pluginItem,
                    favoriteUrlItem
                    //apiUrlItem
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

        statusBar = Ext.create('Ext.ux.toolbar.StatusBar', {
            height: 27,
            listeners: {
                render: function() {
                    ns.app.statusBar = this;

                    this.reset();
                }
            }
        });

		defaultButton = Ext.create('Ext.button.Button', {
			text: NS.i18n.table,
			iconCls: 'ns-button-icon-table',
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
                            text: NS.i18n.clear_event_report + '&nbsp;&nbsp;', //i18n
                            cls: 'ns-menu-item-noicon',
                            handler: function() {
                                window.location.href = ns.core.init.contextPath + '/dhis-web-event-reports';
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
					defaultButton,
					{
						text: NS.i18n.chart,
						iconCls: 'ns-button-icon-chart',
						toggleGroup: 'module',
						menu: {},
						handler: function(b) {
							b.menu = Ext.create('Ext.menu.Menu', {
								closeAction: 'destroy',
								shadow: false,
								showSeparator: false,
								items: [
									{
										text: NS.i18n.go_to_event_charts + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-event-visualizer';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-event-visualizer', '_blank');
														}
													}
												});
											}
										}
									},
									'-',
									{
										text: NS.i18n.open_this_table_as_chart + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										disabled: !(NS.isSessionStorage && ns.app.layout),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled && NS.isSessionStorage) {
														ns.app.layout.parentGraphMap = ns.app.accordion.treePanel.getParentGraphMap();
														ns.core.web.storage.session.set(ns.app.layout, 'eventanalytical');

														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-event-visualizer/index.html?s=eventanalytical';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-event-visualizer/index.html?s=eventanalytical', '_blank');
														}
													}
												});
											}
										}
									},
									{
										text: NS.i18n.open_last_chart + '&nbsp;&nbsp;',
										cls: 'ns-menu-item-noicon',
										disabled: !(NS.isSessionStorage && JSON.parse(sessionStorage.getItem('dhis2')) && JSON.parse(sessionStorage.getItem('dhis2'))['eventchart']),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = ns.core.init.contextPath + '/dhis-web-event-visualizer/index.html?s=eventchart';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(ns.core.init.contextPath + '/dhis-web-event-visualizer/index.html?s=eventchart', '_blank');
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
            bbar: statusBar,
			listeners: {
				added: function() {
					ns.app.centerRegion = this;
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
				},
				resize: function() {
					var width = this.getWidth();

					if (width < 748 && this.fullSize) {
						this.toggleCmp(false);
						this.fullSize = false;
					}
					else if (width >= 748 && !this.fullSize) {
						this.toggleCmp(true);
						this.fullSize = true;
					}
				}
			}
		});

        getLayoutWindow = function(dataType) {
            dataType = dataType || typeToolbar.getType();

            if (dataType === finalsDataTypeConf.aggregated_values) {
                return ns.app.aggregateLayoutWindow;
            }

            if (dataType === finalsDataTypeConf.individual_cases) {
                return ns.app.queryLayoutWindow;
            }

            return null;
        };

        getOptionsWindow = function(dataType) {
            dataType = dataType || typeToolbar.getType();

            if (dataType === finalsDataTypeConf.aggregated_values) {
                return ns.app.aggregateOptionsWindow;
            }

            if (dataType === finalsDataTypeConf.individual_cases) {
                return ns.app.queryOptionsWindow;
            }

            return null;
        };

		viewport = Ext.create('Ext.container.Viewport', {
			layout: 'border',
            getLayoutWindow: getLayoutWindow,
			items: [
				westRegion,
				centerRegion
			],
			listeners: {
				render: function() {
					ns.app.viewport = this;

					ns.app.aggregateLayoutWindow = AggregateLayoutWindow();
					ns.app.aggregateLayoutWindow.hide();
					ns.app.queryLayoutWindow = QueryLayoutWindow();
					ns.app.queryLayoutWindow.hide();
					ns.app.aggregateOptionsWindow = AggregateOptionsWindow();
					ns.app.aggregateOptionsWindow.hide();
					ns.app.queryOptionsWindow = QueryOptionsWindow();
					ns.app.queryOptionsWindow.hide();
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
						settingsHeight = 41;

					if (viewportHeight > numberOfTabs * tabHeight + minPeriodHeight + settingsHeight) {
						if (!Ext.isIE) {
							accordion.setAutoScroll(false);
							westRegion.setWidth(ns.core.conf.layout.west_width);
							accordion.doLayout();
						}
					}
					else {
						westRegion.hasScrollbar = true;

                        caseButton.setWidth(caseButton.getWidth() + scrollbarWidth);
					}

					// expand init panels
					accordion.expandInitPanels();

					// look for url params
					var id = ns.core.web.url.getParam('id'),
						session = ns.core.web.url.getParam('s'),
						layout;

					if (id) {
						ns.core.web.report.loadReport(id);
					}
					else if (Ext.isString(session) && NS.isSessionStorage && Ext.isObject(JSON.parse(sessionStorage.getItem('dhis2'))) && session in JSON.parse(sessionStorage.getItem('dhis2'))) {
						layout = ns.core.api.layout.Layout(JSON.parse(sessionStorage.getItem('dhis2'))[session]);

						if (layout) {
							ns.core.web.report.getData(layout, true);
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

		return viewport;
	};

	// initialize
	(function() {
		var requests = [],
			callbacks = 0,
			init = {},
            fn;

		fn = function() {
			if (++callbacks === requests.length) {

				ns.core = NS.getCore(init);
                ns.alert = ns.core.webAlert;
				extendCore(ns.core);

				dimConf = ns.core.conf.finals.dimension;
                finalsStyleConf = ns.core.conf.finals.style;
                styleConf = ns.core.conf.style;
                finalsDataTypeConf = ns.core.conf.finals.dataType;

				ns.app.viewport = createViewport();

                ns.core.app.getViewportWidth = function() { return ns.app.viewport.getWidth(); };
                ns.core.app.getViewportHeight = function() { return ns.app.viewport.getHeight(); };
                ns.core.app.getCenterRegionWidth = function() { return ns.app.viewport.centerRegion.getWidth(); };
                ns.core.app.getCenterRegionHeight = function() { return ns.app.viewport.centerRegion.getHeight(); };

                NS.instances.push(ns);
			}
		};

        // dhis2
        dhis2.util.namespace('dhis2.er');

        dhis2.er.store = dhis2.er.store || new dhis2.storage.Store({
            name: 'dhis2',
            adapters: [dhis2.storage.IndexedDBAdapter, dhis2.storage.DomSessionStorageAdapter, dhis2.storage.InMemoryAdapter],
            objectStores: ['optionSets']
        });

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
                            url: init.contextPath + '/api/systemSettings.json?key=keyCalendar&key=keyDateFormat',
                            success: function(r) {
                                var systemSettings = Ext.decode(r.responseText);
                                init.systemInfo.dateFormat = Ext.isString(systemSettings.keyDateFormat) ? systemSettings.keyDateFormat.toLowerCase() : 'yyyy-mm-dd';
                                init.systemInfo.calendar = systemSettings.keyCalendar;

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

                                        // legend sets
                                        requests.push({
                                            url: contextPath + '/api/legendSets.json?fields=id,displayName|rename(name),legends[id,displayName|rename(name),startValue,endValue,color]&paging=false',
                                            success: function(r) {
                                                init.legendSets = Ext.decode(r.responseText).legendSets || [];
                                                fn();
                                            }
                                        });

                                        // dimensions
                                        requests.push({
                                            url: contextPath + '/api/dimensions.json?fields=id,' + namePropertyUrl + '&filter=dimensionType:eq:ORGANISATION_UNIT_GROUP_SET&paging=false',
                                            success: function(r) {
                                                init.dimensions = Ext.decode(r.responseText).dimensions || [];
                                                fn();
                                            }
                                        });

                                        // option sets
                                        requests.push({
                                            url: '.',
                                            disableCaching: false,
                                            success: function() {
                                                var store = dhis2.er.store;

                                                store.open().done( function() {

                                                    // check if idb has any option sets
                                                    store.getKeys('optionSets').done( function(keys) {
                                                        if (keys.length === 0) {
                                                            Ext.Ajax.request({
                                                                url: contextPath + '/api/optionSets.json?fields=id,name,version,options[code,name]&paging=false',
                                                                success: function(r) {
                                                                    var sets = Ext.decode(r.responseText).optionSets;

                                                                    if (sets.length) {
                                                                        store.setAll('optionSets', sets).done(fn);
                                                                    }
                                                                    else {
                                                                        fn();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else {
                                                            Ext.Ajax.request({
                                                                url: contextPath + '/api/optionSets.json?fields=id,version&paging=false',
                                                                disableCaching: false,
                                                                success: function(r) {
                                                                    var optionSets = Ext.decode(r.responseText).optionSets || [],
                                                                        ids = [],
                                                                        url = '',
                                                                        callbacks = 0,
                                                                        checkOptionSet,
                                                                        updateStore;

                                                                    updateStore = function() {
                                                                        if (++callbacks === optionSets.length) {
                                                                            if (!ids.length) {
                                                                                fn();
                                                                                return;
                                                                            }

                                                                            for (var i = 0; i < ids.length; i++) {
                                                                                url += '&filter=id:eq:' + ids[i];
                                                                            }

                                                                            Ext.Ajax.request({
                                                                                url: contextPath + '/api/optionSets.json?fields=id,name,version,options[code,name]&paging=false' + url,
                                                                                success: function(r) {
                                                                                    var sets = Ext.decode(r.responseText).optionSets;

                                                                                    store.setAll('optionSets', sets).done(fn);
                                                                                }
                                                                            });
                                                                        }
                                                                    };

                                                                    registerOptionSet = function(optionSet) {
                                                                        store.get('optionSets', optionSet.id).done( function(obj) {
                                                                            if (!Ext.isObject(obj) || obj.version !== optionSet.version) {
                                                                                ids.push(optionSet.id);
                                                                            }

                                                                            updateStore();
                                                                        });
                                                                    };

                                                                    if (optionSets.length) {
                                                                        for (var i = 0; i < optionSets.length; i++) {
                                                                            registerOptionSet(optionSets[i]);
                                                                        }
                                                                    }
                                                                    else {
                                                                        fn();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                });
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
