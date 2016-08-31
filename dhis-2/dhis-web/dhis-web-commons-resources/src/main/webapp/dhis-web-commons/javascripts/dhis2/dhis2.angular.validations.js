d2Directives.directive('d2NumberValidator', function() {
    
    return {
        require: 'ngModel',
        restrict: 'A',
        link: function (scope, element, attrs, ngModel) {
            
            function setValidity(numberType, isRequired){
                if(numberType === 'NUMBER'){
                    ngModel.$validators.number = function(value) {
                    	value = value === 0 ? value.toString(): value; 
                        return value === 'null' || !value ? !isRequired : dhis2.validation.isNumber(value);
                    };
                }
                else if(numberType === 'INTEGER_POSITIVE'){
                    ngModel.$validators.posInt = function(value) {
                    	value = value === 0 ? value.toString(): value; 
                        return value === 'null' || !value ? !isRequired : dhis2.validation.isPositiveInt(value);
                    };
                }
                else if(numberType === 'INTEGER_NEGATIVE'){
                    ngModel.$validators.negInt = function(value) {
                    	value = value === 0 ? value.toString(): value;
                        return value === 'null' || !value ? !isRequired : dhis2.validation.isNegativeInt(value);
                    };
                }
                else if(numberType === 'INTEGER_ZERO_OR_POSITIVE'){
                    ngModel.$validators.zeroPositiveInt = function(value) {
                    	value = value === 0 ? value.toString(): value; 
                        return value === 'null' || !value ? !isRequired : dhis2.validation.isZeroOrPositiveInt(value);
                    };
                }
                else if(numberType === 'INTEGER'){
                    ngModel.$validators.int = function(value) {
                    	value = value === 0 ? value.toString(): value;
                        return value === 'null' || !value ? !isRequired : dhis2.validation.isInt(value);
                    };
                }
            }

            var numberType = attrs.numberType;
            var isRequired = attrs.ngRequired === 'true';            
            setValidity(numberType, isRequired);
        }
    };
})

.directive("d2DateValidator", function(DateUtils, CalendarService, $parse) {
    return {
        restrict: "A",         
        require: "ngModel",         
        link: function(scope, element, attrs, ngModel) {
        	
            var isRequired = attrs.ngRequired === 'true';
        	
            ngModel.$validators.dateValidator = function(value) {
                if(!value){
                    return !isRequired;
                }                
                var convertedDate = DateUtils.format(angular.copy(value));
                var isValid = value === convertedDate;
                return isValid;
            };
            
            ngModel.$validators.futureDateValidator = function(value) {
                if(!value){
                    return !isRequired;
                }
                var maxDate = $parse(attrs.maxDate)(scope);
                var convertedDate = DateUtils.format(angular.copy(value));
                var isValid = value === convertedDate;                
                if(isValid){
                    isValid = maxDate === 0 ? !moment(convertedDate).isAfter(DateUtils.getToday()) : isValid;
                }
                return isValid;
            };
        }
    };
})

.directive("d2CustomCoordinateValidator", function() {
    return {
        restrict: "A",         
        require: "ngModel",         
        link: function(scope, element, attrs, ngModel) {
            
            var isRequired = attrs.ngRequired === 'true';
            
            ngModel.$validators.customCoordinateValidator = function(value) {
                if(!value){
                    return !isRequired;
                }
                
                var coordinate = value.split(",");
                
                if( !coordinate || coordinate.length !== 2 ){
                    return false;
                }

                if( !dhis2.validation.isNumber(coordinate[0]) ){
                    return false;
                }
                
                if( !dhis2.validation.isNumber(coordinate[1]) ){
                    return false;
                }
                
                return coordinate[0] >= -180 && coordinate[0] <= 180 && coordinate[1] >= -90 && coordinate[1] <= 90;
            };           
        }
    };
})

.directive("d2CoordinateValidator", function() {
    return {
        restrict: "A",         
        require: "ngModel",         
        link: function(scope, element, attrs, ngModel) {
            
            var isRequired = attrs.ngRequired === 'true';
            
            if(attrs.name === 'latitude'){
                ngModel.$validators.latitudeValidator = function(value) {
                    if(!value){
                        return !isRequired;
                    }

                    if(!dhis2.validation.isNumber(value)){
                        return false;
                    }
                    return value >= -90 && value <= 90;
                };
            }
            
            if(attrs.name === 'longitude'){
                ngModel.$validators.longitudeValidator = function(value) {
                    if(!value){
                        return !isRequired;
                    }

                    if(!dhis2.validation.isNumber(value)){
                        return false;
                    }
                    return value >= -180 && value <= 180;
                };
            }            
        }
    };
})

.directive("d2OptionValidator", function($translate, DialogService) {
    return {
        restrict: "A",         
        require: "ngModel",         
        link: function(scope, element, attrs, ngModel) {
        	
            var isRequired = attrs.ngRequired === 'true';
            
            ngModel.$validators.optionValidator = function(value) {               
                
                var res = !value ? !isRequired : true;
                
                if(!res){
                	var dialogOptions = {
		                headerText: 'validation_error',
		                bodyText: 'option_required'
		            };		
		            DialogService.showDialog({}, dialogOptions);
                }
                
                return res;
            };
        }
    };
})

.directive("d2AttributeValidator", function($q, TEIService, SessionStorageService) {
    return {
        restrict: "A",         
        require: "ngModel",
        link: function(scope, element, attrs, ngModel) {            
            
            function uniqunessValidatior(attributeData){
                
                ngModel.$asyncValidators.uniqunessValidator = function (modelValue, viewValue) {
                    var pager = {pageSize: 1, page: 1, toolBarDisplay: 5};
                    var deferred = $q.defer(), currentValue = modelValue || viewValue, programUrl = null, ouMode = 'ALL';
                    
                    if (currentValue) {
                        
                        attributeData.value = currentValue;                        
                        var attUrl = 'filter=' + attributeData.id + ':EQ:' + attributeData.value;
                        var ouId = SessionStorageService.get('ouSelected');
                        
                        if(attrs.selectedProgram && attributeData.programScope){
                            programUrl = 'program=' + attrs.selectedProgram;
                        }
                        
                        if(attributeData.orgUnitScope){
                            ouMode = 'SELECTED';
                        }                        

                        TEIService.search(ouId, ouMode, null, programUrl, attUrl, pager, true).then(function(data) {
                            if(attrs.selectedTeiId){
                                if(data && data.rows && data.rows.length && data.rows[0] && data.rows[0].length && data.rows[0][0] !== attrs.selectedTeiId){
                                    deferred.reject();
                                }
                            }
                            else{
                                if (data.rows.length > 0) {    
                                    deferred.reject();
                                }
                            }                            
                            deferred.resolve();
                        });
                    }
                    else {
                        deferred.resolve();
                    }

                    return deferred.promise;
                };
            }                      
            
            scope.$watch(attrs.ngDisabled, function(value){
                var attributeData = scope.$eval(attrs.attributeData);
                if(!value){
                    if( attributeData && attributeData.unique && !value ){
                        uniqunessValidatior(attributeData);
                    }
                }              
            });     
        }
    };
});