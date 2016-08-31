/*----------------------------------------------------------------------------
 * EXTJS UX custom
 *--------------------------------------------------------------------------*/

/* ColorButton */
Ext.define('Ext.ux.button.ColorButton', {
	extend: 'Ext.button.Button',
	alias: 'widget.colorbutton',
	width: 109,
	height: 22,
	defaultValue: null,
	value: 'f1f1f1',
	getValue: function() {
		return this.value;
	},
	setValue: function(color) {
		this.value = color;
		if (Ext.isDefined(this.getEl())) {
			this.getEl().dom.style.background = '#' + color;
		}
	},
	reset: function() {
		this.setValue(this.defaultValue);
	},
	menu: {},
	menuHandler: function() {},
	initComponent: function() {
		var that = this;
		this.defaultValue = this.value;
		this.menu = Ext.create('Ext.menu.Menu', {
			showSeparator: false,
			items: {
				xtype: 'colorpicker',
				closeAction: 'hide',
                width: 163,
                height: 179,
                colors: [
                    'FFFFFF', 'FFDADA', 'FFEEDA', 'FFFFDA', 'DAFFDA', 'DAFFFF', 'DAEEFF', 'DADAFF', 'F7DAFF',
                    'E2E2E2', 'FFB6B6', 'FFDDB6', 'FFFFB6', 'B6FFB6', 'B6FFFF', 'B6DDFF', 'B6B6FF', 'F0B6FF',
                    'C6C6C6', 'FF9191', 'FFCC91', 'FFFF91', '91FF91', '91FFFF', '91CCFF', '9191FF', 'E991FF',
                    'AAAAAA', 'FF6D6D', 'FFBB6D', 'FFFF6D', '6DFF6D', '6DFFFF', '6DBBFF', '6D6DFF', 'E16DFF',
                    '8D8D8D', 'FF4848', 'FFAA48', 'FFFF48', '48FF48', '48FFFF', '48AAFF', '4848FF', 'DA48FF',
                    '717171', 'FF2424', 'FF9924', 'FFFF24', '24FF24', '24FFFF', '2499FF', '2424FF', 'D324FF',
                    '555555', 'FF0000', 'FF8800', 'FFFF00', '00FF00', '00FFFF', '0088FF', '0000FF', 'CC00FF',
                    '383838', 'BF0000', 'BF6600', 'E2E200', '00BF00', '00CCCC', '006CCC', '0000CC', 'A300CC',
                    '1C1C1C', '7F0000', '7F4400', 'C6C600', '007F00', '007F7F', '00447F', '00007F', '66007F',
                    '000000', '3F0000', '3F2200', '8D8D00', '003F00', '003F3F', '00223F', '00003F', '33003F'

                ],
				listeners: {
					select: function(cp, color) {
						that.setValue(color);
						that.menu.hide();
						that.menuHandler(cp, color);
					}
				}
			}
		});
		this.callParent();
	},
	listeners: {
		render: function() {
			this.setValue(this.value);
		}
	}
});

/* MultiSelect */
Ext.define("Ext.ux.layout.component.form.MultiSelect",{extend:"Ext.layout.component.field.Field",alias:["layout.multiselectfield"],type:"multiselectfield",defaultHeight:200,sizeBodyContents:function(a,b){var c=this;if(!Ext.isNumber(b)){b=c.defaultHeight}c.owner.panel.setSize(a,b)}});

Ext.define('Ext.ux.form.MultiSelect', {
    extend: 'Ext.form.field.Base',
    alternateClassName: 'Ext.ux.Multiselect',
    alias: ['widget.multiselect', 'widget.multiselectfield'],
    uses: ['Ext.view.BoundList', 'Ext.form.FieldSet', 'Ext.ux.layout.component.form.MultiSelect', 'Ext.view.DragZone', 'Ext.view.DropZone'],
    /**
     * @cfg {String} listTitle An optional title to be displayed at the top of the selection list.
     */
    /**
     * @cfg {String/Array} dragGroup The ddgroup name(s) for the MultiSelect DragZone (defaults to undefined).
     */
    /**
     * @cfg {String/Array} dropGroup The ddgroup name(s) for the MultiSelect DropZone (defaults to undefined).
     */
    /**
     * @cfg {Boolean} ddReorder Whether the items in the MultiSelect list are drag/drop reorderable (defaults to false).
     */
    ddReorder: false,
    /**
     * @cfg {Object/Array} tbar An optional toolbar to be inserted at the top of the control's selection list.
     * This can be a {@link Ext.toolbar.Toolbar} object, a toolbar config, or an array of buttons/button configs
     * to be added to the toolbar. See {@link Ext.panel.Panel#tbar}.
     */
    /**
     * @cfg {String} appendOnly True if the list should only allow append drops when drag/drop is enabled
     * (use for lists which are sorted, defaults to false).
     */
    appendOnly: false,
    /**
     * @cfg {String} displayField Name of the desired display field in the dataset (defaults to 'text').
     */
    displayField: 'text',
    /**
     * @cfg {String} valueField Name of the desired value field in the dataset (defaults to the
     * value of {@link #displayField}).
     */
    /**
     * @cfg {Boolean} allowBlank False to require at least one item in the list to be selected, true to allow no
     * selection (defaults to true).
     */
    allowBlank: true,
    /**
     * @cfg {Number} minSelections Minimum number of selections allowed (defaults to 0).
     */
    minSelections: 0,
    /**
     * @cfg {Number} maxSelections Maximum number of selections allowed (defaults to Number.MAX_VALUE).
     */
    maxSelections: Number.MAX_VALUE,
    /**
     * @cfg {String} blankText Default text displayed when the control contains no items (defaults to 'This field is required')
     */
    blankText: 'This field is required',
    /**
     * @cfg {String} minSelectionsText Validation message displayed when {@link #minSelections} is not met (defaults to 'Minimum {0}
     * item(s) required'). The {0} token will be replaced by the value of {@link #minSelections}.
     */
    minSelectionsText: 'Minimum {0} item(s) required',
    /**
     * @cfg {String} maxSelectionsText Validation message displayed when {@link #maxSelections} is not met (defaults to 'Maximum {0}
     * item(s) allowed'). The {0} token will be replaced by the value of {@link #maxSelections}.
     */
    maxSelectionsText: 'Maximum {0} item(s) allowed',
    /**
     * @cfg {String} delimiter The string used to delimit the selected values when {@link #getSubmitValue submitting}
     * the field as part of a form. Defaults to ','. If you wish to have the selected values submitted as separate
     * parameters rather than a single delimited parameter, set this to <tt>null</tt>.
     */
    delimiter: ',',
    /**
     * @cfg {Ext.data.Store/Array} store The data source to which this MultiSelect is bound (defaults to <tt>undefined</tt>).
     * Acceptable values for this property are:
     * <div class="mdetail-params"><ul>
     * <li><b>any {@link Ext.data.Store Store} subclass</b></li>
     * <li><b>an Array</b> : Arrays will be converted to a {@link Ext.data.ArrayStore} internally.
     * <div class="mdetail-params"><ul>
     * <li><b>1-dimensional array</b> : (e.g., <tt>['Foo','Bar']</tt>)<div class="sub-desc">
     * A 1-dimensional array will automatically be expanded (each array item will be the combo
     * {@link #valueField value} and {@link #displayField text})</div></li>
     * <li><b>2-dimensional array</b> : (e.g., <tt>[['f','Foo'],['b','Bar']]</tt>)<div class="sub-desc">
     * For a multi-dimensional array, the value in index 0 of each item will be assumed to be the combo
     * {@link #valueField value}, while the value at index 1 is assumed to be the combo {@link #displayField text}.
     * </div></li></ul></div></li></ul></div>
     */
    componentLayout: 'multiselectfield',
    fieldBodyCls: Ext.baseCSSPrefix + 'form-multiselect-body',
    // private
    initComponent: function () {
        var me = this;
        me.bindStore(me.store, true);
        if (me.store.autoCreated) {
            me.valueField = me.displayField = 'field1';
            if (!me.store.expanded) {
                me.displayField = 'field2';
            }
        }
        if (!Ext.isDefined(me.valueField)) {
            me.valueField = me.displayField;
        }
        me.callParent();
    },
    bindStore: function (store, initial) {
        var me = this,
            oldStore = me.store,
            boundList = me.boundList;
        if (oldStore && !initial && oldStore !== store && oldStore.autoDestroy) {
            oldStore.destroy();
        }
        me.store = store ? Ext.data.StoreManager.lookup(store) : null;
        if (boundList) {
            boundList.bindStore(store || null);
        }
    },
    // private
    onRender: function (ct, position) {
        var me = this,
            panel, boundList, selModel;
        me.callParent(arguments);
        boundList = me.boundList = Ext.create('Ext.view.BoundList', {
            multiSelect: true,
            store: me.store,
            displayField: me.displayField,
            border: false
        });
        selModel = boundList.getSelectionModel();
        me.mon(selModel, {
            selectionChange: me.onSelectionChange,
            scope: me
        });
        panel = me.panel = Ext.create('Ext.panel.Panel', {
            title: me.listTitle,
            tbar: me.tbar,
            items: [boundList],
            renderTo: me.bodyEl,
            layout: 'fit'
        });
        // Must set upward link after first render
        panel.ownerCt = me;
        // Set selection to current value
        me.setRawValue(me.rawValue);
    },
    // No content generated via template, it's all added components
    getSubTplMarkup: function () {
        return '';
    },
    // private
    afterRender: function () {
        var me = this;
        me.callParent();
        if (me.ddReorder && !me.dragGroup && !me.dropGroup) {
            me.dragGroup = me.dropGroup = 'MultiselectDD-' + Ext.id();
        }
        if (me.draggable || me.dragGroup) {
            me.dragZone = Ext.create('Ext.view.DragZone', {
                view: me.boundList,
                ddGroup: me.dragGroup,
                dragText: '{0} Item{1}'
            });
        }
        if (me.droppable || me.dropGroup) {
            me.dropZone = Ext.create('Ext.view.DropZone', {
                view: me.boundList,
                ddGroup: me.dropGroup,
                handleNodeDrop: function (data, dropRecord, position) {
                    var view = this.view,
                        store = view.getStore(),
                        records = data.records,
                        index;
                    // remove the Models from the source Store
                    data.view.store.remove(records);
                    index = store.indexOf(dropRecord);
                    if (position === 'after') {
                        index++;
                    }
                    store.insert(index, records);
                    view.getSelectionModel().select(records);
                }
            });
        }
    },
    onSelectionChange: function () {
        this.checkChange();
    },
    /**
     * Clears any values currently selected.
     */
    clearValue: function () {
        this.setValue([]);
    },
    /**
     * Return the value(s) to be submitted for this field. The returned value depends on the {@link #delimiter}
     * config: If it is set to a String value (like the default ',') then this will return the selected values
     * joined by the delimiter. If it is set to <tt>null</tt> then the values will be returned as an Array.
     */
    getSubmitValue: function () {
        var me = this,
            delimiter = me.delimiter,
            val = me.getValue();
        return Ext.isString(delimiter) ? val.join(delimiter) : val;
    },
    // inherit docs
    getRawValue: function () {
        var me = this,
            boundList = me.boundList;
        if (boundList) {
            me.rawValue = Ext.Array.map(boundList.getSelectionModel().getSelection(), function (model) {
                return model.get(me.valueField);
            });
        }
        return me.rawValue;
    },
    // inherit docs
    setRawValue: function (value) {
        var me = this,
            boundList = me.boundList,
            models;
        value = Ext.Array.from(value);
        me.rawValue = value;
        if (boundList) {
            models = [];
            Ext.Array.forEach(value, function (val) {
                var undef, model = me.store.findRecord(me.valueField, val, undef, undef, true, true);
                if (model) {
                    models.push(model);
                }
            });
            boundList.getSelectionModel().select(models, false, true);
        }
        return value;
    },
    // no conversion
    valueToRaw: function (value) {
        return value;
    },
    // compare array values
    isEqual: function (v1, v2) {
        var fromArray = Ext.Array.from,
            i, len;
        v1 = fromArray(v1);
        v2 = fromArray(v2);
        len = v1.length;
        if (len !== v2.length) {
            return false;
        }
        for (i = 0; i < len; i++) {
            if (v2[i] !== v1[i]) {
                return false;
            }
        }
        return true;
    },
    getErrors: function (value) {
        var me = this,
            format = Ext.String.format,
            errors = me.callParent(arguments),
            numSelected;
        value = Ext.Array.from(value || me.getValue());
        numSelected = value.length;
        if (!me.allowBlank && numSelected < 1) {
            errors.push(me.blankText);
        }
        if (numSelected < this.minSelections) {
            errors.push(format(me.minSelectionsText, me.minSelections));
        }
        if (numSelected > this.maxSelections) {
            errors.push(format(me.maxSelectionsText, me.maxSelections));
        }
        return errors;
    },
    onDisable: function () {
        this.callParent();
        this.disabled = true;
        this.updateReadOnly();
    },
    onEnable: function () {
        this.callParent();
        this.disabled = false;
        this.updateReadOnly();
    },
    setReadOnly: function (readOnly) {
        this.readOnly = readOnly;
        this.updateReadOnly();
    },
    /**
     * @private Lock or unlock the BoundList's selection model to match the current disabled/readonly state
     */
    updateReadOnly: function () {
        var me = this,
            boundList = me.boundList,
            readOnly = me.readOnly || me.disabled;
        if (boundList) {
            boundList.getSelectionModel().setLocked(readOnly);
        }
    },
    onDestroy: function () {
        Ext.destroyMembers(this, 'panel', 'boundList', 'dragZone', 'dropZone');
        this.callParent();
    }
});


/*----------------------------------------------------------------------------
 * OpenStreetMap
 *--------------------------------------------------------------------------*/

OpenLayers.Util.OSM={};OpenLayers.Util.OSM.MISSING_TILE_URL="http://www.openstreetmap.org/openlayers/img/404.png";OpenLayers.Util.OSM.originalOnImageLoadError=OpenLayers.Util.onImageLoadError;OpenLayers.Util.onImageLoadError=function(){if(this.src.match(/^http:\/\/[abc]\.[a-z]+\.openstreetmap\.org\//)){this.src=OpenLayers.Util.OSM.MISSING_TILE_URL}else{if(this.src.match(/^http:\/\/[def]\.tah\.openstreetmap\.org\//)){}else{OpenLayers.Util.OSM.originalOnImageLoadError}}};OpenLayers.Layer.OSM.Mapnik=OpenLayers.Class(OpenLayers.Layer.OSM,{initialize:function(d,c){var b=["http://a.tile.openstreetmap.org/${z}/${x}/${y}.png","http://b.tile.openstreetmap.org/${z}/${x}/${y}.png","http://c.tile.openstreetmap.org/${z}/${x}/${y}.png"];c=OpenLayers.Util.extend({numZoomLevels:19,buffer:0,transitionEffect:"resize"},c);var a=[d,b,c];OpenLayers.Layer.OSM.prototype.initialize.apply(this,a)},CLASS_NAME:"OpenLayers.Layer.OSM.Mapnik"});OpenLayers.Layer.OSM.Osmarender=OpenLayers.Class(OpenLayers.Layer.OSM,{initialize:function(d,c){var b=["http://a.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png","http://b.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png","http://c.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png"];c=OpenLayers.Util.extend({numZoomLevels:18,buffer:0,transitionEffect:"resize"},c);var a=[d,b,c];OpenLayers.Layer.OSM.prototype.initialize.apply(this,a)},CLASS_NAME:"OpenLayers.Layer.OSM.Osmarender"});OpenLayers.Layer.OSM.CycleMap=OpenLayers.Class(OpenLayers.Layer.OSM,{initialize:function(d,c){var b=["http://a.tile.opencyclemap.org/cycle/${z}/${x}/${y}.png","http://b.tile.opencyclemap.org/cycle/${z}/${x}/${y}.png","http://c.tile.opencyclemap.org/cycle/${z}/${x}/${y}.png"];c=OpenLayers.Util.extend({numZoomLevels:19,buffer:0,transitionEffect:"resize"},c);var a=[d,b,c];OpenLayers.Layer.OSM.prototype.initialize.apply(this,a)},CLASS_NAME:"OpenLayers.Layer.OSM.CycleMap"});


/*----------------------------------------------------------------------------
 * OpenLayers - Cirle
 *--------------------------------------------------------------------------*/

OpenLayers.Control.Circle=OpenLayers.Class(OpenLayers.Control,{feature:null,layer:null,radius:5,origin:null,sides:40,angle:null,snapAngle:null,dragControl:null,initialize:function(a){OpenLayers.Control.prototype.initialize.apply(this,arguments)},activate:function(){var a=OpenLayers.Control.prototype.activate.call(this);if(a){var b={displayInLayerSwitcher:false,calculateInRange:function(){return true}};this.map.addLayer(this.layer)}return a},deactivate:function(){var a=OpenLayers.Control.prototype.deactivate.call(this);if(a){if(this.layer.map!=null){this.layer.destroy(false);if(this.feature){this.feature.destroy()}}this.layer=null;this.feature=null}return a},createGeometry:function(){this.angle=Math.PI*((1/this.sides)-(1/2));if(this.snapAngle){this.angle+=this.snapAngle*(Math.PI/180)}this.feature.geometry=OpenLayers.Geometry.Polygon.createRegularPolygon(this.origin,this.radius,this.sides,this.snapAngle)},modifyGeometry:function(){var f,c,b,a;var d=this.feature.geometry.components[0];if(d.components.length!=(this.sides+1)){this.createGeometry();d=this.feature.geometry.components[0]}for(var e=0;e<this.sides;++e){a=d.components[e];f=this.angle+(e*2*Math.PI/this.sides);a.x=this.origin.x+(this.radius*Math.cos(f));a.y=this.origin.y+(this.radius*Math.sin(f));a.clearBounds()}},updateCircle:function(b,a){this.origin=new OpenLayers.Geometry.Point(b.lon,b.lat);this.radius=a*1;if(!this.feature){this.feature=new OpenLayers.Feature.Vector();this.createGeometry();this.layer.addFeatures([this.feature],{silent:true})}else{this.modifyGeometry()}this.layer.drawFeature(this.feature,this.style)},CLASS_NAME:"Meteorage.Circle"});


/*----------------------------------------------------------------------------
 * OpenLayers - newSelectFeature
 *--------------------------------------------------------------------------*/

OpenLayers.Control.newSelectFeature=OpenLayers.Class(OpenLayers.Control,{multipleKey:null,toggleKey:null,multiple:false,clickout:true,toggle:false,hover:false,onSelect:function(){},onUnselect:function(){},onHoverSelect:function(){},onHoverUnselect:function(){},onClickSelect:function(){},onClickUnselect:function(){},geometryTypes:null,layer:null,callbacks:null,selectStyle:null,renderIntent:"select",handler:null,initialize:function(layer,options){OpenLayers.Control.prototype.initialize.apply(this,[options]);this.layer=layer;this.callbacks=OpenLayers.Util.extend({click:this.clickFeature,clickout:this.clickoutFeature,over:this.overFeature,out:this.outFeature},this.callbacks);var handlerOptions={geometryTypes:this.geometryTypes};this.handler=new OpenLayers.Handler.Feature(this,layer,this.callbacks,handlerOptions);},unselectAll:function(options){var feature;for(var i=this.layer.selectedFeatures.length-1;i>=0;--i){feature=this.layer.selectedFeatures[i];if(!options||options.except!=feature){this.unselect(feature,"click");}}},clickFeature:function(feature){if((this.onSelect.name!=""||this.onClickSelect.name!="")&&!this.hover){var selected=(OpenLayers.Util.indexOf(this.layer.selectedFeatures,feature)>-1);if(selected){if(this.toggleSelect()){this.unselect(feature);}else if(!this.multipleSelect()){this.unselectAll({except:feature});}}else{if(!this.multipleSelect()){this.unselectAll({except:feature});}}
this.select(feature,"click");}},multipleSelect:function(){return this.multiple||this.handler.evt[this.multipleKey];},toggleSelect:function(){return this.toggle||this.handler.evt[this.toggleKey];},clickoutFeature:function(feature){if(((this.onClickUnselect.name!=""||this.onHoverSelect.name=="")&&!this.hover)&&this.clickout){this.unselectAll();}},overFeature:function(feature){if((this.onHoverSelect.name!=""||this.hover)&&(OpenLayers.Util.indexOf(this.layer.selectedFeatures,feature)==-1)){this.select(feature,"hover");}},outFeature:function(feature){if(this.onHoverUnselect.name!=""||this.hover){this.unselect(feature,"hover");}},select:function(feature,evt){this.layer.selectedFeatures.push(feature);var selectStyle=this.selectStyle||this.renderIntent;this.layer.drawFeature(feature,selectStyle);this.layer.events.triggerEvent("featureselected",{feature:feature});switch(evt){case"hover":this.onHoverSelect(feature);break;case"click":if(this.onClickSelect.name!=""){this.onClickSelect(feature);}else if(this.onSelect.name!=""){this.onSelect(feature);}
break;default:this.onSelect(feature);break;}},unselect:function(feature,evt){this.layer.drawFeature(feature,"default");OpenLayers.Util.removeItem(this.layer.selectedFeatures,feature);this.layer.events.triggerEvent("featureunselected",{feature:feature});switch(evt){case"hover":this.onHoverUnselect(feature);break;case"click":if(this.onClickUnselect.name!=""){this.onClickUnselect(feature);}else if(this.onUnselect.name!=""){this.onUnselect(feature);}
break;default:this.onUnselect(feature);break;}},setMap:function(map){this.handler.setMap(map);OpenLayers.Control.prototype.setMap.apply(this,arguments);},CLASS_NAME:"OpenLayers.Control.newSelectFeature"});


/*----------------------------------------------------------------------------
 * GeoExt - custom
 *--------------------------------------------------------------------------*/

Ext.define("GeoExt.data.LayerModel",{alternateClassName:"GeoExt.data.LayerRecord",extend:"Ext.data.Model",requires:["Ext.data.proxy.Memory","Ext.data.reader.Json"],alias:"model.gx_layer",statics:{createFromLayer:function(a){return this.proxy.reader.readRecords([a]).records[0]}},fields:["id",{name:"title",type:"string",mapping:"name"},{name:"legendURL",type:"string",mapping:"metadata.legendURL"},{name:"hideTitle",type:"bool",mapping:"metadata.hideTitle"},{name:"hideInLegend",type:"bool",mapping:"metadata.hideInLegend"}],proxy:{type:"memory",reader:{type:"json"}},getLayer:function(){return this.raw}});Ext.define("GeoExt.data.LayerStore",{requires:["GeoExt.data.LayerModel"],extend:"Ext.data.Store",model:"GeoExt.data.LayerModel",statics:{MAP_TO_STORE:1,STORE_TO_MAP:2},map:null,constructor:function(b){var c=this;b=Ext.apply({},b);var d=(GeoExt.MapPanel&&b.map instanceof GeoExt.MapPanel)?b.map.map:b.map;delete b.map;if(b.layers){b.data=b.layers}delete b.layers;var a={initDir:b.initDir};delete b.initDir;c.callParent([b]);if(d){this.bind(d,a)}},bind:function(e,a){var b=this;if(b.map){return}b.map=e;a=Ext.apply({},a);var c=a.initDir;if(a.initDir==undefined){c=GeoExt.data.LayerStore.MAP_TO_STORE|GeoExt.data.LayerStore.STORE_TO_MAP}var d=e.layers.slice(0);if(c&GeoExt.data.LayerStore.STORE_TO_MAP){b.each(function(f){b.map.addLayer(f.getLayer())},b)}if(c&GeoExt.data.LayerStore.MAP_TO_STORE){b.loadRawData(d,true)}e.events.on({changelayer:b.onChangeLayer,addlayer:b.onAddLayer,removelayer:b.onRemoveLayer,scope:b});b.on({load:b.onLoad,clear:b.onClear,add:b.onAdd,remove:b.onRemove,update:b.onUpdate,scope:b});b.data.on({replace:b.onReplace,scope:b});b.fireEvent("bind",b,e)},unbind:function(){var a=this;if(a.map){a.map.events.un({changelayer:a.onChangeLayer,addlayer:a.onAddLayer,removelayer:a.onRemoveLayer,scope:a});a.un("load",a.onLoad,a);a.un("clear",a.onClear,a);a.un("add",a.onAdd,a);a.un("remove",a.onRemove,a);a.data.un("replace",a.onReplace,a);a.map=null}},onChangeLayer:function(b){var e=b.layer;var c=this.findBy(function(f,g){return f.getLayer()===e});if(c>-1){var a=this.getAt(c);if(b.property==="order"){if(!this._adding&&!this._removing){var d=this.map.getLayerIndex(e);if(d!==c){this._removing=true;this.remove(a);delete this._removing;this._adding=true;this.insert(d,[a]);delete this._adding}}}else{if(b.property==="name"){a.set("title",e.name)}else{this.fireEvent("update",this,a,Ext.data.Record.EDIT)}}}},onAddLayer:function(b){var c=this;if(!c._adding){c._adding=true;var a=c.proxy.reader.read(b.layer);c.add(a.records);delete c._adding}},onRemoveLayer:function(a){if(this.map.unloadDestroy){if(!this._removing){var b=a.layer;this._removing=true;this.remove(this.getByLayer(b));delete this._removing}}else{this.unbind()}},onLoad:function(c,b,g){if(g){if(!Ext.isArray(b)){b=[b]}if(!this._addRecords){this._removing=true;for(var e=this.map.layers.length-1;e>=0;e--){this.map.removeLayer(this.map.layers[e])}delete this._removing}var a=b.length;if(a>0){var f=new Array(a);for(var d=0;d<a;d++){f[d]=b[d].getLayer()}this._adding=true;this.map.addLayers(f);delete this._adding}}delete this._addRecords},onClear:function(a){this._removing=true;for(var b=this.map.layers.length-1;b>=0;b--){this.map.removeLayer(this.map.layers[b])}delete this._removing},onAdd:function(b,a,c){if(!this._adding){this._adding=true;var e;for(var d=a.length-1;d>=0;--d){e=a[d].getLayer();this.map.addLayer(e);if(c!==this.map.layers.length-1){this.map.setLayerIndex(e,c)}}delete this._adding}},onRemove:function(b,a,c){if(!this._removing){var d=a.getLayer();if(this.map.getLayer(d.id)!=null){this._removing=true;this.removeMapLayer(a);delete this._removing}}},onUpdate:function(c,a,b){if(b===Ext.data.Record.EDIT){if(a.modified&&a.modified.title){var d=a.getLayer();var e=a.get("title");if(e!==d.name){d.setName(e)}}}},removeMapLayer:function(a){this.map.removeLayer(a.getLayer())},onReplace:function(c,a,b){this.removeMapLayer(a)},getByLayer:function(b){var a=this.findBy(function(c){return c.getLayer()===b});if(a>-1){return this.getAt(a)}},destroy:function(){var a=this;a.unbind();a.callParent()},loadRecords:function(a,b){if(b&&b.addRecords){this._addRecords=true}this.callParent(arguments)}});Ext.define("GeoExt.panel.Map",{extend:"Ext.panel.Panel",requires:["GeoExt.data.LayerStore"],alias:"widget.gx_mappanel",alternateClassName:"GeoExt.MapPanel",statics:{guess:function(){var a=Ext.ComponentQuery.query("gx_mappanel");return((a&&a.length>0)?a[0]:null)}},center:null,zoom:null,extent:null,prettyStateKeys:false,map:null,layers:null,stateEvents:["aftermapmove","afterlayervisibilitychange","afterlayeropacitychange","afterlayerorderchange","afterlayernamechange","afterlayeradd","afterlayerremove"],initComponent:function(){if(!(this.map instanceof OpenLayers.Map)){this.map=new OpenLayers.Map(Ext.applyIf(this.map||{},{allOverlays:true}))}var a=this.layers;if(!a||a instanceof Array){this.layers=Ext.create("GeoExt.data.LayerStore",{layers:a,map:this.map.layers.length>0?this.map:null})}if(Ext.isString(this.center)){this.center=OpenLayers.LonLat.fromString(this.center)}else{if(Ext.isArray(this.center)){this.center=new OpenLayers.LonLat(this.center[0],this.center[1])}}if(Ext.isString(this.extent)){this.extent=OpenLayers.Bounds.fromString(this.extent)}else{if(Ext.isArray(this.extent)){this.extent=OpenLayers.Bounds.fromArray(this.extent)}}this.callParent(arguments);this.on("resize",this.onResize,this);this.on("afterlayout",function(){if(typeof this.map.getViewport==="function"){this.items.each(function(b){if(typeof b.addToMapPanel==="function"){b.getEl().appendTo(this.map.getViewport())}},this)}},this);this.map.events.on({moveend:this.onMoveend,changelayer:this.onChangelayer,addlayer:this.onAddlayer,removelayer:this.onRemovelayer,scope:this})},onMoveend:function(a){this.fireEvent("aftermapmove",this,this.map,a)},onChangelayer:function(b){var a=this.map;if(b.property){if(b.property==="visibility"){this.fireEvent("afterlayervisibilitychange",this,a,b)}else{if(b.property==="order"){this.fireEvent("afterlayerorderchange",this,a,b)}else{if(b.property==="nathis"){this.fireEvent("afterlayernathischange",this,a,b)}else{if(b.property==="opacity"){this.fireEvent("afterlayeropacitychange",this,a,b)}}}}}},onAddlayer:function(){this.fireEvent("afterlayeradd")},onRemovelayer:function(){this.fireEvent("afterlayerremove")},onResize:function(){var a=this.map;if(this.body.dom!==a.div){a.render(this.body.dom);this.layers.bind(a);if(a.layers.length>0){this.setInitialExtent()}else{this.layers.on("add",this.setInitialExtent,this,{single:true})}}else{a.updateSize()}},setInitialExtent:function(){var a=this.map;if(!a.getCenter()){if(this.center||this.zoom){a.setCenter(this.center,this.zoom)}else{if(this.extent instanceof OpenLayers.Bounds){a.zoomToExtent(this.extent,true)}else{a.zoomToMaxExtent()}}}},getState:function(){var c=this,e=c.map,d=c.callParent(arguments)||{},b;if(!e){return}var a=e.getCenter();a&&Ext.applyIf(d,{x:a.lon,y:a.lat,zoom:e.getZoom()});c.layers.each(function(f){b=f.getLayer();layerId=this.prettyStateKeys?f.get("title"):f.get("id");d=c.addPropertyToState(d,"visibility_"+layerId,b.getVisibility());d=c.addPropertyToState(d,"opacity_"+layerId,(b.opacity===null)?1:b.opacity)},c);return d},applyState:function(a){var j=this;map=j.map;j.center=new OpenLayers.LonLat(a.x,a.y);j.zoom=a.zoom;var f,c,g,d,b,h;var e=map.layers;for(f=0,c=e.length;f<c;f++){g=e[f];d=j.prettyStateKeys?g.name:g.id;b=a["visibility_"+d];if(b!==undefined){b=(/^true$/i).test(b);if(g.isBaseLayer){if(b){map.setBaseLayer(g)}}else{g.setVisibility(b)}}h=a["opacity_"+d];if(h!==undefined){g.setOpacity(h)}}},onBeforeAdd:function(a){if(Ext.isFunction(a.addToMapPanel)){a.addToMapPanel(this)}this.callParent(arguments)},beforeDestroy:function(){if(this.map&&this.map.events){this.map.events.un({moveend:this.onMoveend,changelayer:this.onChangelayer,scope:this})}if(!this.initialConfig.map||!(this.initialConfig.map instanceof OpenLayers.Map)){if(this.map&&this.map.destroy){this.map.destroy()}}delete this.map;this.callParent(arguments)}});Ext.define("GeoExt.tree.Column",{extend:"Ext.tree.Column",alias:"widget.gx_treecolumn",initComponent:function(){var b=this;b.callParent();var a=b.renderer;this.renderer=function(i,e,d,h,j,f,c){var g=[a(i,e,d,h,j,f,c)];if(d.get("checkedGroup")){g[0]=g[0].replace(/class="([^-]+)-tree-checkbox([^"]+)?"/,'class="$1-tree-checkbox$2 gx-tree-radio"')}g.push('<div class="gx-tree-component gx-tree-component-off" id="tree-record-'+d.id+'"></div>');if(d.uiProvider&&d.uiProvider instanceof "string"){}return g.join("")}},defaultRenderer:function(a){return a}});Ext.define("GeoExt.tree.View",{extend:"Ext.tree.View",alias:"widget.gx_treeview",initComponent:function(){var a=this;a.on("itemupdate",this.onItem,this);a.on("itemadd",this.onItem,this);a.on("createchild",this.createChild,this);return a.callParent(arguments)},onItem:function(a,c,f,b){var e=this;if(!(a instanceof Array)){a=[a]}for(var d=0;d<a.length;d++){this.onNodeRendered(a[d])}},onNodeRendered:function(c){var b=this;var a=Ext.get("tree-record-"+c.id);if(!a){return}if(c.get("layer")){b.fireEvent("createchild",a,c)}if(c.hasChildNodes()){c.eachChild(function(d){b.onNodeRendered(d)},b)}},createChild:function(b,c){var a=c.get("component");if(a){cmpObj=Ext.ComponentManager.create(a);cmpObj.render(b);b.removeCls("gx-tree-component-off")}}});Ext.define("GeoExt.tree.LayerNode",{extend:"Ext.AbstractPlugin",alias:"plugin.gx_layer",init:function(b){this.target=b;var a=b.get("layer");b.set("checked",a.getVisibility());if(!b.get("checkedGroup")&&a.isBaseLayer){b.set("checkedGroup","gx_baselayer")}b.set("fixedText",!!b.text);b.set("leaf",true);if(!b.get("iconCls")){b.set("iconCls","gx-tree-layer-icon")}b.on("afteredit",this.onAfterEdit,this);a.events.on({visibilitychanged:this.onLayerVisibilityChanged,scope:this})},onAfterEdit:function(c,a){var b=this;if(~Ext.Array.indexOf(a,"checked")){b.onCheckChange()}},onLayerVisibilityChanged:function(){if(!this._visibilityChanging){this.target.set("checked",this.target.get("layer").getVisibility())}},onCheckChange:function(){var c=this.target,b=this.target.get("checked");if(b!=c.get("layer").getVisibility()){c._visibilityChanging=true;var a=c.get("layer");if(b&&a.isBaseLayer&&a.map){a.map.setBaseLayer(a)}else{a.setVisibility(b)}delete c._visibilityChanging}}});Ext.define("GeoExt.tree.LayerLoader",{extend:"Ext.util.Observable",requires:["GeoExt.tree.LayerNode"],store:null,filter:function(a){return a.getLayer().displayInLayerSwitcher===true},baseAttrs:null,load:function(a){if(this.fireEvent("beforeload",this,a)){this.removeStoreHandlers();while(a.firstChild){a.removeChild(a.firstChild)}if(!this.store){this.store=GeoExt.MapPanel.guess().layers}this.store.each(function(b){this.addLayerNode(a,b)},this);this.addStoreHandlers(a);this.fireEvent("load",this,a)}},onStoreAdd:function(b,a,c,f){if(!this._reordering){var g=f.get("container").recordIndexToNodeIndex(c+a.length-1,f);for(var d=0,e=a.length;d<e;++d){this.addLayerNode(f,a[d],g)}}},onStoreRemove:function(a,b){if(!this._reordering){this.removeLayerNode(b,a)}},addLayerNode:function(d,a,b){b=b||0;if(this.filter(a)===true){var c=a.getLayer();var e=this.createNode({plugins:[{ptype:"gx_layer"}],layer:c,text:c.name,listeners:{move:this.onChildMove,scope:this}});if(b!==undefined){d.insertChild(b,e)}else{d.appendChild(e)}d.getChildAt(b).on("move",this.onChildMove,this)}},removeLayerNode:function(b,a){if(this.filter(a)===true){var c=b.findChildBy(function(d){return d.get("layer")==a.getLayer()});if(c){c.un("move",this.onChildMove,this);c.remove()}}},onChildMove:function(c,k,l,h){var i=this,g=i.store.getByLayer(c.get("layer")),b=l.get("container"),f=b.loader;i._reordering=true;if(f instanceof i.self&&i.store===f.store){f._reordering=true;i.store.remove(g);var a;if(l.childNodes.length>1){var j=(h===0)?h+1:h-1;a=i.store.findBy(function(m){return l.childNodes[j].get("layer")===m.getLayer()});if(h===0){a++}}else{if(k.parentNode===l.parentNode){var d=l;do{d=d.previousSibling}while(d&&!(d.get("container") instanceof b.self&&d.lastChild));if(d){a=i.store.findBy(function(m){return d.lastChild.get("layer")===m.getLayer()})}else{var e=l;do{e=e.nextSibling}while(e&&!(e.get("container") instanceof b.self&&e.firstChild));if(e){a=i.store.findBy(function(m){return e.firstChild.get("layer")===m.getLayer()})}a++}}}if(a!==undefined){i.store.insert(a,[g])}else{i.store.insert(oldRecordIndex,[g])}delete f._reordering}delete i._reordering},addStoreHandlers:function(b){if(!this._storeHandlers){this._storeHandlers={add:function(c,e,d){this.onStoreAdd(c,e,d,b)},remove:function(c,d){this.onStoreRemove(d,b)}};for(var a in this._storeHandlers){this.store.on(a,this._storeHandlers[a],this)}}},removeStoreHandlers:function(){if(this._storeHandlers){for(var a in this._storeHandlers){this.store.un(a,this._storeHandlers[a],this)}delete this._storeHandlers}},createNode:function(a){if(this.baseAttrs){Ext.apply(a,this.baseAttrs)}return a},destroy:function(){this.removeStoreHandlers()}});Ext.define("GeoExt.tree.LayerContainer",{extend:"Ext.AbstractPlugin",requires:["GeoExt.tree.LayerLoader"],alias:"plugin.gx_layercontainer",defaultText:"Layers",init:function(c){var b=this;var a=b.loader;b.loader=(a&&a instanceof GeoExt.tree.LayerLoader)?a:new GeoExt.tree.LayerLoader(a);c.set("container",b);if(!c.get("text")){c.set("text",b.defaultText);c.commit()}b.loader.load(c)},recordIndexToNodeIndex:function(c,g){var f=this;var b=f.loader.store;var e=b.getCount();var a=g.childNodes.length;var h=-1;for(var d=e-1;d>=0;--d){if(f.loader.filter(b.getAt(d))===true){++h;if(c===d||h>a-1){break}}}return h}});Ext.define("GeoExt.tree.BaseLayerContainer",{extend:"GeoExt.tree.LayerContainer",alias:"plugin.gx_baselayercontainer",defaultText:"Base Layers",init:function(c){var b=this;var a=b.loader;b.loader=Ext.applyIf(a||{},{baseAttrs:Ext.applyIf((a&&a.baseAttrs)||{},{iconCls:"gx-tree-baselayer-icon",checkedGroup:"baselayer"}),filter:function(d){var e=d.getLayer();return e.displayInLayerSwitcher===true&&e.isBaseLayer===true}});b.callParent(arguments)}});Ext.define("GeoExt.tree.Panel",{extend:"Ext.tree.Panel",alias:"widget.gx_treepanel",requires:["GeoExt.tree.Column","GeoExt.tree.View"],viewType:"gx_treeview",initComponent:function(){var a=this;if(!a.columns){if(a.initialConfig.hideHeaders===undefined){a.hideHeaders=true}a.addCls(Ext.baseCSSPrefix+"autowidth-table");a.columns=[{xtype:"gx_treecolumn",text:"Name",width:Ext.isIE6?null:10000,dataIndex:a.displayField}]}a.callParent()}});
























