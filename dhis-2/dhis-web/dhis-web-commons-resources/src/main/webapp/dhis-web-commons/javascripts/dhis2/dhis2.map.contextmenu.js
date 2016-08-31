/*
	MapContextMenu v1.0
	
	A context menu for Google Maps API v3
	http://code.martinpearman.co.uk/googlemapsapi/contextmenu/
	
	Copyright Martin Pearman
	Last updated 21st November 2011
	
	developer@martinpearman.co.uk
	
	This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

function MapContextMenu(map, options){
	options=options || {};
	
	this.setMap(map);
	
	this.classNames_=options.classNames || {};
	this.map_=map;
	this.mapDiv_=map.getDiv();
	this.menuItems_=options.menuItems || [];
	this.pixelOffset=options.pixelOffset || new google.maps.Point(10, -5);
}

MapContextMenu.prototype=new google.maps.OverlayView();

MapContextMenu.prototype.draw=function(){
	if(this.isVisible_){
		var mapSize=new google.maps.Size(this.mapDiv_.offsetWidth, this.mapDiv_.offsetHeight);
		var menuSize=new google.maps.Size(this.menu_.offsetWidth, this.menu_.offsetHeight);
		var mousePosition=this.getProjection().fromLatLngToDivPixel(this.position_);
		
		var left=mousePosition.x;
		var top=mousePosition.y;
		
		if(mousePosition.x>mapSize.width-menuSize.width-this.pixelOffset.x){
			left=left-menuSize.width-this.pixelOffset.x;
		} else {
			left+=this.pixelOffset.x;
		}
		
		if(mousePosition.y>mapSize.height-menuSize.height-this.pixelOffset.y){
			top=top-menuSize.height-this.pixelOffset.y;
		} else {
			top+=this.pixelOffset.y;
		}
		
		this.menu_.style.left=left+'px';
		this.menu_.style.top=top+'px';
	}
};

MapContextMenu.prototype.getVisible=function(){
	return this.isVisible_;
};

MapContextMenu.prototype.hide=function(){
	if(this.isVisible_){
		this.menu_.style.display='none';
		this.isVisible_=false;
	}
};

MapContextMenu.prototype.onAdd=function(){
	function createMenuItem(values){        
		var menuItem=document.createElement('div');
		menuItem.innerHTML=values.label;
		if(values.className){
			menuItem.className=values.className;
		}
		if(values.id){
			menuItem.id=values.id;
		}
		menuItem.style.cssText='white-space:nowrap; font-size: 12pt;';
		menuItem.onclick=function(){
			google.maps.event.trigger($this, 'menu_item_selected', $this.event_, $this.position_, values.eventName);
		};
		return menuItem;
	}
	function createMenuSeparator(){
		var menuSeparator=document.createElement('div');
		if($this.classNames_.menuSeparator){
			menuSeparator.className=$this.classNames_.menuSeparator;
		}
		return menuSeparator;
	}
	var $this=this;	//	used for closures
	
	var menu=document.createElement('div');
	if(this.classNames_.menu){
		menu.className=this.classNames_.menu;
	}
	menu.style.cssText='display:none; position:absolute';
	
	for(var i=0, j=this.menuItems_.length; i<j; i++){
		if(this.menuItems_[i].label && this.menuItems_[i].eventName){
			menu.appendChild(createMenuItem(this.menuItems_[i]));
		} else {
			menu.appendChild(createMenuSeparator());
		}
	}
	
	delete this.classNames_;
	delete this.menuItems_;
	
	this.isVisible_=false;
	this.menu_=menu;
	this.position_=new google.maps.LatLng(0, 0);
	
	google.maps.event.addListener(this.map_, 'click', function(mouseEvent){
		$this.hide();
	});
	
	this.getPanes().floatPane.appendChild(menu);
};

MapContextMenu.prototype.onRemove=function(){
	this.menu_.parentNode.removeChild(this.menu_);
	delete this.mapDiv_;
	delete this.menu_;
	delete this.position_;
};

MapContextMenu.prototype.show=function(e){
	if(!this.isVisible_){
		this.menu_.style.display='block';
		this.isVisible_=true;
	}
	this.position_=e.latLng;
    this.event_=e;
	this.draw();
};