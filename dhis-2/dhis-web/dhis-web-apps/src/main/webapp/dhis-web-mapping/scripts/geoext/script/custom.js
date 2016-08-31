/*
 * Copyright (c) 2008-2012 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/**
 * The layer model class used by the stores.
 */
Ext.define('GeoExt.data.LayerModel',{
    alternateClassName: 'GeoExt.data.LayerRecord',
    extend: 'Ext.data.Model',
    requires: ['Ext.data.proxy.Memory', 'Ext.data.reader.Json'],
    alias: 'model.gx_layer',
    statics: {
        /**
         * Convenience function for creating new layer model instance object
         * using a layer object.
         * @param {OpenLayers.Layer} layer
         * @return {GeoExt.data.LayerModel}
         * @static
         */
        createFromLayer: function(layer) {
            return this.proxy.reader.readRecords([layer]).records[0];
        }
    },
    fields: [
        'id',
        {name: 'title', type: 'string', mapping: 'name'},
        {name: 'legendURL', type: 'string', mapping: 'metadata.legendURL'},
        {name: 'hideTitle', type: 'bool', mapping: 'metadata.hideTitle'},
        {name: 'hideInLegend', type: 'bool', mapping: 'metadata.hideInLegend'}
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    /**
     * Returns the {OpenLayers.Layer} layer object used in this model instance
     */
    getLayer: function() {
        return this.raw;
    }
});


/*
 * Copyright (c) 2008-2012 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/LayerModel.js
 */

/**
 * @class GeoExt.data.LayerStore
 * A store that synchronizes a layers array of an OpenLayers.Map with a
 * layer store holding {@link GeoExt.data.LayerModel} instances.
 */
Ext.define('GeoExt.data.LayerStore', {
    requires: ['GeoExt.data.LayerModel'],
    extend: 'Ext.data.Store',
    model: 'GeoExt.data.LayerModel',

    statics: {
        /**
         * @static
         * @property {Number}
         * Direction: Map to store
         */
        MAP_TO_STORE: 1,
        /**
         * @static
         * @property {Number}
         * Direction: Store to map
         */
        STORE_TO_MAP: 2
    },

    /**
     * @event bind
     * Fires when the store is bound to a map.
     *
     * @param {GeoExt.data.LayerStore} store
     * @param {OpenLayers.Map} map
     */

    /**
     * @cfg {OpenLayers.Map/GeoExt.panel.Map/Object} map
     * Map that this store will be in sync with. If not provided, the
     * store will not be bound to a map.
     */

    /**
     * @property {OpenLayers.Map/Object} map
     * Map that the store is synchronized with, if any.
     */
    map: null,

    /**
     * @cfg {OpenLayers.Layer/Array} layers
     * Layers that will be added to the store (and the map, depending on the
     * value of the ``initDir`` option.
     */

    /**
     * @cfg {Number} initDir
     * Bitfields specifying the direction to use for the initial sync between
     * the map and the store, if set to 0 then no initial sync is done.
     * Defaults to {@link #MAP_TO_STORE} |
     * {@link #STORE_TO_MAP}.
     */

    /**
     * @config {Object} Creation parameters
     * @private
     */
    constructor: function(config) {
        var me = this;

        config = Ext.apply({}, config);

        // "map" option
        var map = (GeoExt.MapPanel && config.map instanceof GeoExt.MapPanel) ?
            config.map.map : config.map;
        delete config.map;

        // "layers" option - is an alias to "data" option
        if(config.layers) {
            config.data = config.layers;
        }
        delete config.layers;

        // "initDir" option
        var options = {initDir: config.initDir};
        delete config.initDir;

        me.callParent([config]);

        if(map) {
            this.bind(map, options);
        }
    },

    /**
     * Bind this store to a map instance, once bound the store
     * is synchronized with the map and vice-versa.
     *
     * @param {OpenLayers.Map} map The map instance.
     * @param {Object} options
     */
    bind: function(map, options) {
        var me = this;

        if(me.map) {
            // already bound
            return;
        }
        me.map = map;
        options = Ext.apply({}, options);

        var initDir = options.initDir;
        if(options.initDir == undefined) {
            initDir = GeoExt.data.LayerStore.MAP_TO_STORE |
                GeoExt.data.LayerStore.STORE_TO_MAP;
        }

        // create a snapshot of the map's layers
        var layers = map.layers.slice(0);

        if(initDir & GeoExt.data.LayerStore.STORE_TO_MAP) {
            me.each(function(record) {
                me.map.addLayer(record.getLayer());
            }, me);
        }
        if(initDir & GeoExt.data.LayerStore.MAP_TO_STORE) {
            me.loadRawData(layers, true);
        }

        map.events.on({
            "changelayer": me.onChangeLayer,
            "addlayer": me.onAddLayer,
            "removelayer": me.onRemoveLayer,
            scope: me
        });
        me.on({
            "load": me.onLoad,
            "clear": me.onClear,
            "add": me.onAdd,
            "remove": me.onRemove,
            "update": me.onUpdate,
            scope: me
        });
        me.data.on({
            "replace" : me.onReplace,
            scope: me
        });
        me.fireEvent("bind", me, map);
    },

    /**
     * Unbind this store from the map it is currently bound.
     */
    unbind: function() {
        var me = this;

        if(me.map) {
            me.map.events.un({
                "changelayer": me.onChangeLayer,
                "addlayer": me.onAddLayer,
                "removelayer": me.onRemoveLayer,
                scope: me
            });
            me.un("load", me.onLoad, me);
            me.un("clear", me.onClear, me);
            me.un("add", me.onAdd, me);
            me.un("remove", me.onRemove, me);

            me.data.un("replace", me.onReplace, me);

            me.map = null;
        }
    },

    /**
     * Handler for layer changes.  When layer order changes, this moves the
     * appropriate record within the store.
     * @private
     * @param {Object} evt
     */
    onChangeLayer: function(evt) {
        var layer = evt.layer;
        var recordIndex = this.findBy(function(rec, id) {
            return rec.getLayer() === layer;
        });
        if(recordIndex > -1) {
            var record = this.getAt(recordIndex);
            if(evt.property === "order") {
                if(!this._adding && !this._removing) {
                    var layerIndex = this.map.getLayerIndex(layer);
                    if(layerIndex !== recordIndex) {
                        this._removing = true;
                        this.remove(record);
                        delete this._removing;
                        this._adding = true;
                        this.insert(layerIndex, [record]);
                        delete this._adding;
                    }
                }
            } else if(evt.property === "name") {
                record.set("title", layer.name);
            } else {
                this.fireEvent("update", this, record, Ext.data.Record.EDIT);
            }
        }
    },

    /**
     * Handler for a map's addlayer event
     * @private
     * @param {Object} evt
     */
    onAddLayer: function(evt) {
        var me = this;
        if(!me._adding) {
            me._adding = true;
            var result  = me.proxy.reader.read(evt.layer);
            me.add(result.records);
            delete me._adding;
        }
    },

    /**
     * Handler for a map's removelayer event
     * @private
     * @param {Object} evt
     */
    onRemoveLayer: function(evt){
        //TODO replace the check for undloadDestroy with a listener for the
        // map's beforedestroy event, doing unbind(). This can be done as soon
        // as http://trac.openlayers.org/ticket/2136 is fixed.
        if(this.map.unloadDestroy) {
            if(!this._removing) {
                var layer = evt.layer;
                this._removing = true;
                this.remove(this.getByLayer(layer));
                delete this._removing;
            }
        } else {
            this.unbind();
        }
    },

    /**
     * Handler for a store's load event
     * @private
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model[]} records
     * @param {Boolean} successful
     */
    onLoad: function(store, records, successful) {
        if (successful) {
            if (!Ext.isArray(records)) {
                records = [records];
            }
            if(!this._addRecords) {
                this._removing = true;
                for (var i = this.map.layers.length - 1; i >= 0; i--) {
                    this.map.removeLayer(this.map.layers[i]);
                }
                delete this._removing;
            }
            var len = records.length;
            if (len > 0) {
                var layers = new Array(len);
                for (var j = 0; j < len; j++) {
                    layers[j] = records[j].getLayer();
                }
                this._adding = true;
                this.map.addLayers(layers);
                delete this._adding;
            }
        }
        delete this._addRecords;
    },

    /**
     * Handler for a store's clear event
     * @private
     * @param {Ext.data.Store} store
     */
    onClear: function(store) {
        this._removing = true;
        for (var i = this.map.layers.length - 1; i >= 0; i--) {
            this.map.removeLayer(this.map.layers[i]);
        }
        delete this._removing;
    },

    /**
     * Handler for a store's add event
     * @private
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model[]} records
     * @param {Number} index
     */
    onAdd: function(store, records, index) {
        if(!this._adding) {
            this._adding = true;
            var layer;
            for(var i=records.length-1; i>=0; --i) {
                layer = records[i].getLayer();
                this.map.addLayer(layer);
                if(index !== this.map.layers.length-1) {
                    this.map.setLayerIndex(layer, index);
                }
            }
            delete this._adding;
        }
    },

    /**
     * Handler for a store's remove event
     * @private
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model} record
     * @param {Number} index
     */
    onRemove: function(store, record, index){
        if(!this._removing) {
            var layer = record.getLayer();
            if (this.map.getLayer(layer.id) != null) {
                this._removing = true;
                this.removeMapLayer(record);
                delete this._removing;
            }
        }
    },

    /**
     * Handler for a store's update event
     * @private
     * @param {Ext.data.Store} store
     * @param {Ext.data.Model} record
     * @param {Number} operation
     */
    onUpdate: function(store, record, operation) {
        if(operation === Ext.data.Record.EDIT) {
            if (record.modified && record.modified.title) {
                var layer = record.getLayer();
                var title = record.get("title");
                if(title !== layer.name) {
                    layer.setName(title);
                }
            }
        }
    },

    /**
     * Removes a record's layer from the bound map.
     * @private
     * @param {Ext.data.Record} record
     */
    removeMapLayer: function(record){
        this.map.removeLayer(record.getLayer());
    },

    /**
     * Handler for a store's data collections' replace event
     * @private
     * @param {String} key
     * @param {Ext.data.Model} oldRecord In this case, a record that has
     *     been replaced.
     * @param {Ext.data.Model} newRecord In this case, a record that is
     *     replacing oldRecord.
     */
    onReplace: function(key, oldRecord, newRecord){
        this.removeMapLayer(oldRecord);
    },

    /**
     * Get the record for the specified layer
     * @param {OpenLayers.Layer} layer
     * @returns {Ext.data.Model} or undefined if not found
     */
    getByLayer: function(layer) {
        var index = this.findBy(function(r) {
            return r.getLayer() === layer;
        });
        if(index > -1) {
            return this.getAt(index);
        }
    },

    /**
     * @private
     */
    destroy: function() {
        var me = this;
        me.unbind();
        me.callParent();
    },

    /**
     * Overload loadRecords to set a flag if `addRecords` is `true`
     * in the load options. Ext JS does not pass the load options to
     * "load" callbacks, so this is how we provide that information
     * to `onLoad`.
     * @private
     */
    loadRecords: function(records, options) {
        if(options && options.addRecords) {
            this._addRecords = true;
        }
        this.callParent(arguments);
    }
});


/*
 * Copyright (c) 2008-2012 The Open Source Geospatial Foundation
 *
 * Published under the BSD license.
 * See https://github.com/geoext/geoext2/blob/master/license.txt for the full
 * text of the license.
 */

/*
 * @include GeoExt/data/LayerStore.js
 * @include OpenLayers/Map.js
 */

/**
 * Create a panel container for a map. The map contained by this panel
 * will initially be zoomed to either the center and zoom level configured
 * by the ``center`` and ``zoom`` configuration options, or the configured
 * ``extent``, or - if neither are provided - the extent returned by the
 * map's ``getExtent()`` method.
 *
 * Example:
<pre><code>
var mappanel = Ext.create('GeoExt.panel.Map', {
    title: 'A sample Map',
    map: {
        // ...
        // optional, can be either
        //   - a valid OpenLayers.Map configuration or
        //   - an instance of OpenLayers.Map
    },
    center: '12.31,51.48',
    zoom: 6
});
</code></pre>
 *
 * A Map created with code like above is then ready to use as any other panel.
 * To have a fullscrteen map application, you could e.g. add it to a viewport:
 *
 * Example:
<pre><code>
Ext.create('Ext.container.Viewport', {
    layout: 'fit',
    items: [
        mappanel // our variable from above
    ]
 });
</code></pre>
 */
Ext.define('GeoExt.panel.Map', {
    extend: 'Ext.panel.Panel',
    requires: ['GeoExt.data.LayerStore'],
    alias: 'widget.gx_mappanel',
    alternateClassName: 'GeoExt.MapPanel',

    statics: {
        /**
         * The first map panel found via an the Ext.ComponentQuery.query
         * manager.
         *
         * Convenience function for guessing the map panel of an application.
         * This can reliably be used for all applications that just have one map
         * panel in the viewport.
         *
         * @return {GeoExt.panel.Map}
         * @static
         */
        guess : function() {
            var candidates = Ext.ComponentQuery.query("gx_mappanel");
            return ((candidates && candidates.length > 0)
                ? candidates[0]
                : null);
        }
    },

    /** @cfg {OpenLayers.LonLat/Number[]/String} center
     * A location for the initial map center.  If an array is provided, the
     * first two items should represent x & y coordinates. If a string is
     * provided, it should consist of a x & y coordinate seperated by a
     * comma.
     */
    center: null,

    /**
     * @cfg {Number} zoom
     * An initial zoom level for the map.
     */
    zoom: null,

    /**
     * @cfg {OpenLayers.Bounds/Number[]} extent
     * An initial extent for the map (used if center and zoom are not
     * provided.  If an array, the first four items should be minx, miny,
     * maxx, maxy.
     */
    extent: null,

    /**
     * @cfg {Boolean} prettyStateKeys
     * Set this to true if you want pretty strings in the MapPanel's state
     * keys. More specifically, layer.name instead of layer.id will be used
     * in the state keys if this option is set to true. But in that case
     * you have to make sure you don't have two layers with the same name.
     * Defaults to false.
     */
    /**
     * @property {Boolean} prettyStateKeys
     * Whether we want the state key to be pretty. See
     * {@link #cfg-prettyStateKeys the config option prettyStateKeys} for
     * details.
     */
    prettyStateKeys: false,

    /**
     * @cfg {OpenLayers.Map/Object} map
     * A configured map or a configuration object for the map constructor.
     * A configured map will be available after construction through the
     * {@link GeoExt.panel.Map#property-map} property.
     */
    /**
     * @property {OpenLayers.Map/Object} map
     * A map or map configuration.
     */
    map: null,

    /**
     * @cfg {GeoExt.data.LayerStore/OpenLayers.Layer[]} layers
     * The layers provided here will be added to this Map's
     * {@link #property-map}.
     */
    /**
     * @property {GeoExt.data.LayerStore} layers
     * A store containing {@link GeoExt.data.LayerModel gx_layer-model}
     * instances.
     */
    layers: null,

    /**
     * @property {String[]} stateEvents
     * @private
     * Array of state events
     */
    stateEvents: [
        "aftermapmove",
        "afterlayervisibilitychange",
        "afterlayeropacitychange",
        "afterlayerorderchange",
        "afterlayernamechange",
        "afterlayeradd",
        "afterlayerremove"],

    /**
     * Initializes the map panel. Creates an OpenLayers map if
     * none was provided in the config options passed to the
     * constructor.
     * @private
     */
    initComponent: function(){
        if(!(this.map instanceof OpenLayers.Map)) {
            this.map = new OpenLayers.Map(
                Ext.applyIf(this.map || {}, {allOverlays: true})
            );
        }

        var layers  = this.layers;
        if(!layers || layers instanceof Array) {
            this.layers = Ext.create('GeoExt.data.LayerStore', {
                layers: layers,
                map: this.map.layers.length > 0 ? this.map : null
            });
        }

        if (Ext.isString(this.center)) {
            this.center = OpenLayers.LonLat.fromString(this.center);
        } else if(Ext.isArray(this.center)) {
            this.center = new OpenLayers.LonLat(this.center[0], this.center[1]);
        }
        if (Ext.isString(this.extent)) {
            this.extent = OpenLayers.Bounds.fromString(this.extent);
        } else if(Ext.isArray(this.extent)) {
            this.extent = OpenLayers.Bounds.fromArray(this.extent);
        }

        this.callParent(arguments);

        // The map is renderer and its size is updated when we receive
        // "resize" events.
        this.on('resize', this.onResize, this);

        //TODO This should be handled by a LayoutManager
        this.on("afterlayout", function() {
            //TODO remove function check when we require OpenLayers > 2.11
            if (typeof this.map.getViewport === "function") {
                this.items.each(function(cmp) {
                    if (typeof cmp.addToMapPanel === "function") {
                        cmp.getEl().appendTo(this.map.getViewport());
                    }
                }, this);
            }
        }, this);

        /**
         * @event aftermapmove
         * Fires after the map is moved.
         */
        /**
         * @event afterlayervisibilitychange
         * Fires after a layer changed visibility.
         */
        /**
         * @event afterlayeropacitychange
         * Fires after a layer changed opacity.
         */
        /**
         * @event afterlayerorderchange
         * Fires after a layer order changed.
         */
        /**
         * @event afterlayernamechange
         * Fires after a layer name changed.
         */
        /**
         * @event afterlayeradd
         * Fires after a layer added to the map.
         */
        /**
         * @event afterlayerremove
         * Fires after a layer removed from the map.
         */

        // bind various listeners to the corresponding OpenLayers.Map-events
        this.map.events.on({
            "moveend": this.onMoveend,
            "changelayer": this.onChangelayer,
            "addlayer": this.onAddlayer,
            "removelayer": this.onRemovelayer,
            scope: this
        });
    },

    /**
     * The "moveend" listener bound to the
     * {@link GeoExt.panel.Map#property-map}.
     * @param {Object} e
     * @private
     */
    onMoveend: function(e) {
        this.fireEvent("aftermapmove", this, this.map, e);
    },

    /**
     * The "changelayer" listener bound to the
     * {@link GeoExt.panel.Map#property-map}.
     * @param {Object} e
     * @private
     */
    onChangelayer: function(e) {
        var map = this.map;
        if (e.property) {
            if (e.property === "visibility") {
                this.fireEvent("afterlayervisibilitychange", this, map, e);
            } else if (e.property === "order") {
                this.fireEvent("afterlayerorderchange", this, map, e);
            } else if (e.property === "nathis") {
                this.fireEvent("afterlayernathischange", this, map, e);
            } else if (e.property === "opacity") {
                this.fireEvent("afterlayeropacitychange", this, map, e);
            }
        }
    },

    /**
     * The "addlayer" listener bound to the
     * {@link GeoExt.panel.Map#property-map}.
     * @param {Object} e
     * @private
     */
    onAddlayer: function() {
        this.fireEvent("afterlayeradd");
    },

    /**
     * The "removelayer" listener bound to the
     * {@link GeoExt.panel.Map#property-map}.
     * @param {Object} e
     * @private
     */
    onRemovelayer: function() {
        this.fireEvent("afterlayerremove");
    },

    /**
     * Private method called after the panel has been rendered or after it
     * has been laid out by its parent's layout.
     * @private
     */
    onResize: function() {
        var map = this.map;
        if(this.body.dom !== map.div) {
            // the map has not been rendered yet
            map.render(this.body.dom);

            this.layers.bind(map);

            if (map.layers.length > 0) {
                this.setInitialExtent();
            } else {
                this.layers.on("add", this.setInitialExtent, this,
                               {single: true});
            }
        } else {
            map.updateSize();
        }
    },

    /**
     * Set the initial extent of this panel's map.
     * @private
     */
    setInitialExtent: function() {
        var map = this.map;
        if (!map.getCenter()) {
            if (this.center || this.zoom ) {
                // center and/or zoom?
                map.setCenter(this.center, this.zoom);
            } else if (this.extent instanceof OpenLayers.Bounds) {
                // extent
                map.zoomToExtent(this.extent, true);
            }else {
                map.zoomToMaxExtent();
            }
        }
    },

    /**
     * Returns a state of the Map as keyed Object. Depending on the point in
     * time this methoid is being called, the following keys will be available:
     *
     * * `x`
     * * `y`
     * * `zoom`
     *
     * And for all layers present in the map the object will contain the
     * following keys
     *
     * * `visibility_<XXX>`
     * * `opacity_<XXX>`
     *
     * The <XXX> suffix is either the title or id of the layer record, it can be
     * influenced by setting #prettyStateKeys to `true` or `false`.
     * @private
     * @return {Object}
     */
    getState: function() {
        var me = this,
            map = me.map,
            state = me.callParent(arguments) || {},
            layer;

        // Ext delays the call to getState when a state event
        // occurs, so the MapPanel may have been destroyed
        // between the time the event occurred and the time
        // getState is called
        if(!map) {
            return;
        }

        // record location and zoom level
        var center = map.getCenter();
        // map may not be centered yet, because it may still have zero
        // dimensions or no layers
        center && Ext.applyIf(state, {
            "x": center.lon,
            "y": center.lat,
            "zoom": map.getZoom()
        });

        me.layers.each(function(modelInstance) {
            layer = modelInstance.getLayer();
            layerId = this.prettyStateKeys
                   ? modelInstance.get('title')
                   : modelInstance.get('id');
            state = me.addPropertyToState(state, "visibility_" + layerId,
                layer.getVisibility());
            state = me.addPropertyToState(state, "opacity_" + layerId,
                (layer.opacity === null) ? 1 : layer.opacity);
        }, me);

        return state;
    },

    /**
     * Apply the state provided as an argument.
     * @private
     * @param {Object} state The state to apply.
     */
    applyState: function(state) {
        var me = this;
            map = me.map;
        // if we get strings for state.x, state.y or state.zoom
        // OpenLayers will take care of converting them to the
        // appropriate types so we don't bother with that
        me.center = new OpenLayers.LonLat(state.x, state.y);
        me.zoom = state.zoom;

        // TODO refactor with me.layers.each
        // set layer visibility and opacity
        var i, l, layer, layerId, visibility, opacity;
        var layers = map.layers;
        for(i=0, l=layers.length; i<l; i++) {
            layer = layers[i];
            layerId = me.prettyStateKeys ? layer.name : layer.id;
            visibility = state["visibility_" + layerId];
            if(visibility !== undefined) {
                // convert to boolean
                visibility = (/^true$/i).test(visibility);
                if(layer.isBaseLayer) {
                    if(visibility) {
                        map.setBaseLayer(layer);
                    }
                } else {
                    layer.setVisibility(visibility);
                }
            }
            opacity = state["opacity_" + layerId];
            if(opacity !== undefined) {
                layer.setOpacity(opacity);
            }
        }
    },

    /**
     * Check if an added item has to take separate actions
     * to be added to the map.
     * See e.g. the GeoExt.slider.Zoom or GeoExt.slider.LayerOpacity
     * @private
     */
    onBeforeAdd: function(item) {
        if(Ext.isFunction(item.addToMapPanel)) {
            item.addToMapPanel(this);
        }
        this.callParent(arguments);
    },

    /**
     * Private method called during the destroy sequence.
     * @private
     */
    beforeDestroy: function() {
        if(this.map && this.map.events) {
            this.map.events.un({
                "moveend": this.onMoveend,
                "changelayer": this.onChangelayer,
                scope: this
            });
        }
        // if the map panel was passed a map instance, this map instance
        // is under the user's responsibility
        if(!this.initialConfig.map ||
           !(this.initialConfig.map instanceof OpenLayers.Map)) {
            // we created the map, we destroy it
            if(this.map && this.map.destroy) {
                this.map.destroy();
            }
        }
        delete this.map;
        this.callParent(arguments);
    }
});



Ext.define('GeoExt.tree.Column', {
    extend: 'Ext.tree.Column',
    alias: 'widget.gx_treecolumn',

    initComponent: function() {
        var me = this;

        me.callParent();

        var parentRenderer = me.renderer;

        this.renderer = function(value, metaData, record, rowIdx, colIdx, store, view) {

            var buf   = [parentRenderer(value, metaData, record, rowIdx, colIdx, store, view)];

            // Replace all base layers from checkbox to radio
            if(record.get('checkedGroup')) {
                buf[0] = buf[0].replace(/class="([^-]+)-tree-checkbox([^"]+)?"/, 'class="$1-tree-checkbox$2 gx-tree-radio"'); //"
            }

            // Addd a hook to add other components in the tree like legend icons
            buf.push('<div class="gx-tree-component gx-tree-component-off" id="tree-record-'+record.id+'"></div>');


            if(record.uiProvider && record.uiProvider instanceof 'string') {
                
            }

            return buf.join('');
        };

    },

    defaultRenderer: function(value) {
        return value;
    }
});

Ext.define('GeoExt.tree.View', {
    extend: 'Ext.tree.View',
    alias: 'widget.gx_treeview',

    initComponent : function() {
        var me = this;

        me.on('itemupdate', this.onItem, this);
        me.on('itemadd', this.onItem, this);
        me.on('createchild', this.createChild, this);

        return me.callParent(arguments);
    },

    onItem: function(records, index, node, options) {
        var me = this;

        if(!(records instanceof Array)) {
            records = [records]
        }

        for(var i=0; i<records.length; i++) {
            this.onNodeRendered(records[i]);
        }
    },

    onNodeRendered: function(node) {
        var me = this;

        var el = Ext.get('tree-record-'+node.id);
        if(!el) {
            return;
        }

        if(node.get('layer'))
            me.fireEvent('createchild', el, node);

        if(node.hasChildNodes()) {
            node.eachChild(function(node) {
                me.onNodeRendered(node);
            }, me);
        }
    },

    createChild: function(el, node) {

        var component = node.get('component');

        if(component) {
            cmpObj = Ext.ComponentManager.create(component);

            cmpObj.render(el);

            el.removeCls('gx-tree-component-off');
        }
    }

});

Ext.define('GeoExt.tree.LayerNode', {
    extend: 'Ext.AbstractPlugin',
    alias: 'plugin.gx_layer',

    /**
     * @private
     * The init method is invoked after initComponent method has been run for
     * the client Component.
     *
     * It perform plugin initialization.
     * @param {Ext.Component} target The client Component which owns this plugin.
     */
    init: function(target) {

        this.target = target;
        var layer = target.get('layer');

        target.set('checked', layer.getVisibility());
        if (!target.get('checkedGroup') && layer.isBaseLayer) {
            target.set('checkedGroup', 'gx_baselayer');
        }
        target.set('fixedText', !!target.text);
        
        target.set('leaf', true);
        
        if(!target.get('iconCls')) {
            target.set('iconCls', "gx-tree-layer-icon");
        }

        target.on('afteredit', this.onAfterEdit, this);
        layer.events.on({
            "visibilitychanged": this.onLayerVisibilityChanged,
            scope: this
        });
    },

    /**
     * @private
     * Handler for the node's afteredit event.
     *
     * @param {GeoExt.data.LayerTreeModel} node
     * @param {String[]} modifiedFields
     */
    onAfterEdit: function(node, modifiedFields) {
        var me = this;

        if(~Ext.Array.indexOf(modifiedFields, 'checked')) {
            me.onCheckChange();
        }
    },
    
    /**
     * @private
     * Handler for visibilitychanged events on the layer
     */
    onLayerVisibilityChanged: function() {
        if(!this._visibilityChanging) {
            this.target.set('checked', this.target.get('layer').getVisibility());
        }
    },
    
    /**
     * @private
     * Updates the visibility of the layer that is connected to the target
     * node. 
     */
    onCheckChange: function() {
        var node = this.target,
            checked = this.target.get('checked');

        if(checked != node.get('layer').getVisibility()) {
            node._visibilityChanging = true;
            var layer = node.get('layer');
            if(checked && layer.isBaseLayer && layer.map) {
                layer.map.setBaseLayer(layer);
            } else {
                layer.setVisibility(checked);
            }
            delete node._visibilityChanging;
        }
    }

});

Ext.define('GeoExt.tree.LayerLoader', {
    extend: 'Ext.util.Observable',
    requires: [
        'GeoExt.tree.LayerNode'
    ],

    /**
     * Triggered before loading children. Return false to avoid
     * loading children.
     * @event beforeload
     * @param {GeoExt.tree.LayerLoader} this This loader.
     * @param {Ext.data.NodeInterface} node The node that this loader is
     * configured with.
     */

    /**
     * Triggered after children were loaded.
     * @event load
     * @param {GeoExt.tree.LayerLoader} loader This loader.
     * @param {Ext.data.NodeInterface} node The node that this loader is
     * configured with.
     */

    /**
     * @cfg {GeoExt.data.LayerStore} store
     * The layer store containing layers to be added by this loader.
     */
    /**
     * @property {GeoExt.data.LayerStore} store
     * The layer store containing layers to be added by this loader.
     */
     store: null,
    
    /**
     * @property {Function} filter
     * A function, called in the scope of this loader, with a layer record
     * as argument. Is expected to return true for layers to be loaded, false
     * otherwise. By default, the filter checks for displayInLayerSwitcher:
     *  
     *     filter: function(record) {
     *         return record.getLayer().displayInLayerSwitcher === true
     *     }
     */
    filter: function(record) {
        return record.getLayer().displayInLayerSwitcher === true;
    },
    
    /**
     * @cfg
     * An object containing attributes to be added to all nodes created by
     * this loader.
     */
    baseAttrs: null,
    
    /**
     * @param {GeoExt.data.LayerTreeModel} node The node to add children to.
     * @private
     */
    load: function(node) {
        if (this.fireEvent("beforeload", this, node)) {
            this.removeStoreHandlers();
            while (node.firstChild) {
                node.removeChild(node.firstChild);
            }
            
            if (!this.store) {
                this.store = GeoExt.MapPanel.guess().layers;
            }
            this.store.each(function(record) {
                this.addLayerNode(node, record);
            }, this);
            this.addStoreHandlers(node);
    
            this.fireEvent("load", this, node);
        }
    },
    
    /**
     * Listener for the store's add event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Record[]} records
     * @param {Number} index
     * @param {GeoExt.data.LayerTreeModel} node
     *  
     * @private
     */
    onStoreAdd: function(store, records, index, node) {
        if (!this._reordering) {
            var nodeIndex = node.get('container')
                .recordIndexToNodeIndex(index+records.length-1, node);
            for (var i=0, ii=records.length; i<ii; ++i) {
                this.addLayerNode(node, records[i], nodeIndex);
            }
        }
    },
    
    /**
     * Listener for the store's remove event.
     *
     * @param {Ext.data.Store} store
     * @param {Ext.data.Record} record
     * @param {Integer} index
     * @param {GeoExt.data.LayerTreeModel} node
     *
     * @private
     */
    onStoreRemove: function(layerRecord, node) {
        if (!this._reordering) {
            this.removeLayerNode(node, layerRecord);
        }
    },

    /**
     * Adds a child node representing a layer of the map
     *
     * @param {GeoExt.data.LayerTreeModel} node The node that the layer node
     * will be added to as child.
     * @param {GeoExt.data.LayerModel} layerRecord The layer record containing
     * the layer to be added.
     * @param {Integer} index Optional index for the new layer.  Default is 0.
     *
     * @private
     */
    addLayerNode: function(node, layerRecord, index) {
        index = index || 0;
        if (this.filter(layerRecord) === true) {
            var layer = layerRecord.getLayer();
            var child = this.createNode({
                plugins: [{
                    ptype: 'gx_layer'
                }],
                layer: layer,
                text: layer.name,
                listeners: {
                    move: this.onChildMove,
                    scope: this
                }
            });
            if (index !== undefined) {
                node.insertChild(index, child);
            } else {
                node.appendChild(child);
            }
            node.getChildAt(index).on("move", this.onChildMove, this);
        }
    },

    /**
     * Removes a child node representing a layer of the map
     * @param {GeoExt.data.LayerTreeModel} node The node that the layer node
     * will be removed from as child.
     * @param {GeoExt.data.LayerModel} layerRecord The layer record containing
     * the layer to be removed.
     *
     * @private
     */
    removeLayerNode: function(node, layerRecord) {
        if (this.filter(layerRecord) === true) {
            var child = node.findChildBy(function(node) {
                return node.get('layer') == layerRecord.getLayer();
            });
            if (child) {
                child.un("move", this.onChildMove, this);
                child.remove();
            }
        }
    },
    
    /**
     * Listener for child node "move" events.  This updates the order of
     * records in the store based on new node order if the node has not
     * changed parents.
     *
     * @param {GeoExt.data.LayerTreeModel} node
     * @param {GeoExt.data.LayerTreeModel} oldParent
     * @param {GeoExt.data.LayerTreeModel} newParent
     * @param {Integer} index
     *
     * @private
     */
    onChildMove: function(node, oldParent, newParent, index) {
        var me = this,
            record = me.store.getByLayer(node.get('layer')),
            container = newParent.get('container'),
            parentLoader = container.loader;

        // remove the record and re-insert it at the correct index
        me._reordering = true;
        if (parentLoader instanceof me.self && me.store === parentLoader.store) {
            parentLoader._reordering = true;
            me.store.remove(record);
            var newRecordIndex;
            if (newParent.childNodes.length > 1) {
                // find index by neighboring node in the same container
                var searchIndex = (index === 0) ? index + 1 : index - 1;
                newRecordIndex = me.store.findBy(function(r) {
                    return newParent.childNodes[searchIndex]
                        .get('layer') === r.getLayer();
                });
                if (index === 0) {
                    newRecordIndex++;
                }
            } else if (oldParent.parentNode === newParent.parentNode) {
                // find index by last node of a container above
                var prev = newParent;
                do {
                    prev = prev.previousSibling;
                } while (prev &&
                    !(prev.get('container') instanceof container.self &&
                    prev.lastChild));
                if (prev) {
                    newRecordIndex = me.store.findBy(function(r) {
                        return prev.lastChild.get('layer') === r.getLayer();
                    });
                } else {
                    // find indext by first node of a container below
                    var next = newParent;
                    do {
                        next = next.nextSibling;
                    } while (next &&
                        !(next.get('container') instanceof container.self &&
                        next.firstChild));
                    if (next) {
                        newRecordIndex = me.store.findBy(function(r) {
                            return next.firstChild.get('layer') === r.getLayer();
                        });
                    }
                    newRecordIndex++;
                }
            }
            if (newRecordIndex !== undefined) {
                me.store.insert(newRecordIndex, [record]);
            } else {
                me.store.insert(oldRecordIndex, [record]);
            }
            delete parentLoader._reordering;
        }
        delete me._reordering;
    },
    
    /**
     * @param {GeoExt.data.LayerTreeModel} node
     *
     * @private
     */
    addStoreHandlers: function(node) {
        if (!this._storeHandlers) {
            this._storeHandlers = {
                "add": function(store, layerRecords, index) {
                    this.onStoreAdd(store, layerRecords, index, node);
                },
                "remove": function(parent, removedRecord) {
                    this.onStoreRemove(removedRecord, node);
                }
            };
            for (var evt in this._storeHandlers) {
                this.store.on(evt, this._storeHandlers[evt], this);
            }
        }
    },
    
    /**
     * @private
     */
    removeStoreHandlers: function() {
        if (this._storeHandlers) {
            for (var evt in this._storeHandlers) {
                this.store.un(evt, this._storeHandlers[evt], this);
            }
            delete this._storeHandlers;
        }
    },

    /**
     * Extend this function to modify the node attributes at creation time.
     *
     * @param {Object} attr attributes for the new node
     */
    createNode: function(attr) {
        if (this.baseAttrs){
            Ext.apply(attr, this.baseAttrs);
        }
        
        return attr;
    },

    /**
     * @private
     */
    destroy: function() {
        this.removeStoreHandlers();
    }
});

Ext.define('GeoExt.tree.LayerContainer', {
    extend: 'Ext.AbstractPlugin',
    requires: [
        'GeoExt.tree.LayerLoader'
    ],
    alias: 'plugin.gx_layercontainer',
    
    /**
     * @cfg {GeoExt.tree.LayerLoader/Object} loader
     * The loader to use with this container. If an Object is provided, a
     * GeoExt.tree.LayerLoader, configured with the the properties from
     * the provided object, will be created. By default, a LayerLoader for
     * all layers of the first MapPanel found by the ComponentManager will be
     * created.
     */
    
    /**
     * @private
     * The default text for the target node.
     */
    defaultText: 'Layers',
    
    /**
     * @private
     */
    init: function(target) {
        var me = this;

        var loader = me.loader;
        
        me.loader = (loader && loader instanceof GeoExt.tree.LayerLoader) ?
            loader : new GeoExt.tree.LayerLoader(loader);

        target.set('container', me);
        if (!target.get('text')) {
            target.set('text', me.defaultText);
            target.commit();
        }
        me.loader.load(target);
        
    },
    
    /**
     * @private
     * @param {Number} index  The record index in the layer store.
     * @returns {Number} The appropriate child node index for the record.
     */
    recordIndexToNodeIndex: function(index, node) {
        var me = this;
        var store = me.loader.store;
        var count = store.getCount();
        var nodeCount = node.childNodes.length;
        var nodeIndex = -1;
        for(var i=count-1; i>=0; --i) {
            if(me.loader.filter(store.getAt(i)) === true) {
                ++nodeIndex;
                if(index === i || nodeIndex > nodeCount-1) {
                    break;
                }
            }
        }
        return nodeIndex;
    }
});

Ext.define('GeoExt.tree.BaseLayerContainer', {
    extend: 'GeoExt.tree.LayerContainer',
    alias: 'plugin.gx_baselayercontainer',
    
    /**
     * @private
     * The default text for the target node.
     */
    defaultText: 'Base Layers',
    
    /**
     * @private
     */
    init: function(target) {
        var me = this;
        
        var loader = me.loader;

        me.loader = Ext.applyIf(loader || {}, {
            baseAttrs: Ext.applyIf((loader && loader.baseAttrs) || {}, {
                iconCls: 'gx-tree-baselayer-icon',
                checkedGroup: 'baselayer'
            }),
            filter: function(record) {
                var layer = record.getLayer();
                return layer.displayInLayerSwitcher === true &&
                    layer.isBaseLayer === true;
            }
        });
        me.callParent(arguments);
    }
});

Ext.define('GeoExt.tree.Panel', {
    extend: 'Ext.tree.Panel',
    alias: 'widget.gx_treepanel',
    requires: ['GeoExt.tree.Column', 'GeoExt.tree.View'],
    viewType: 'gx_treeview',
    
    initComponent: function() {
        var me = this;

        if (!me.columns) {
            if (me.initialConfig.hideHeaders === undefined) {
                me.hideHeaders = true;
            }
            me.addCls(Ext.baseCSSPrefix + 'autowidth-table');
            me.columns = [{
                xtype    : 'gx_treecolumn',
                text     : 'Name',
                width    : Ext.isIE6 ? null : 10000,
                dataIndex: me.displayField         
            }];
        }

        me.callParent();
    }
});
