var validationRules = {
    "user" : {
        "username" : {
            "required" : true,
            "rangelength" : [ 2, 140 ]
        },
        "inviteUsername" : {
            "rangelength" : [ 2, 140 ]
        },
        "firstName" : {
            "required" : true,
            "rangelength" : [ 2, 140 ]
        },
        "surname" : {
            "required" : true,
            "rangelength" : [ 2, 140 ]
        },
        "password" : {
            "required" : true,
            "password" : true,
            "notequalto" : "#username",
            "rangelength" : [ 8, 35 ]
        },
        "retypePassword" : {
            "required" : true,
            "equalTo" : "#rawPassword"
        },
        "email" : {
            "email" : true,
            "rangelength" : [ 0, 160 ]
        },
        "inviteEmail" : {
            "required" : true,
            "email" : true,
            "rangelength" : [ 4, 160 ]
        },
        "phoneNumber" : {
            "rangelength" : [ 0, 80 ]
        },
        "urValidator" : {
            "required" : true
        }
    },
    "profile" : {
        "email" : {
            "email" : true,
            "rangelength" : [ 0, 160 ]
        },
        "phoneNumber" : {
            "rangelength" : [ 0, 80 ]
        }
    },
    "role" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 140 ]
        }
    },
    "userGroup" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ],
            "alphanumericwithbasicpuncspaces" : true
        },
        "usersSelected" : {
            "required" : true
        }
    },
    "organisationUnit" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "shortName" : {
            "required" : true,
            "rangelength" : [ 2, 50 ]
        },
        "code" : {
            "rangelength" : [ 0, 50 ],
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : false
        },
        "openingDate" : {
            "required" : true
        },
        "longitude" : {
            "number" : true,
            "min": -180,
            "max": 180
        },
        "latitude" : {
            "number" : true,
            "min": -90,
            "max": 90
        },
        "url" : {
            "url" : true,
            "rangelength" : [ 0, 255 ]
        },
        "contactPerson" : {
            "rangelength" : [ 0, 255 ]
        },
        "address" : {
            "rangelength" : [ 0, 255 ]
        },
        "email" : {
            "email" : true,
            "rangelength" : [ 0, 250 ]
        },
        "phoneNumber" : {
            "rangelength" : [ 0, 255 ]
        }
    },
    "organisationUnitGroup" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "shortName" : {
            "required" : true,
            "alphanumericwithbasicpuncspaces" : true,
            "rangelength" : [ 2, 50 ]
        },
        "code" : {
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : false,
            "rangelength" : [ 0, 50 ]
        }
    },
    "organisationUnitGroupSet" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        }
    },
    "dataEntry" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 100 ]
        }
    },
    "section" : {
        "sectionName" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        }
    },
    "dataSet" : {
        "name" : {
            "required" : true,
            "alphanumericwithbasicpuncspaces" : true,
            "rangelength" : [ 2, 230 ]
        },
        "shortName" : {
            "required" : true,
            "alphanumericwithbasicpuncspaces" : true,
            "rangelength" : [ 2, 50 ]
        },
        "code" : {
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : false,
            "rangelength" : [ 0, 50 ]
        },
        "expiryDays": {
            "digits" : true
        },
        "frequencySelect" : {
            "required" : true
        }
    },
    "sqlView" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "sqlquery" : {
            "required" : true
        }
    },
    "dataLocking" : {
        "selectedPeriods" : {
            "required" : true
        },
        "selectedDataSets" : {
            "required" : true
        }
    },
    "dataBrowser" : {
        "periodTypeId" : {
            "required" : true
        },
        "mode" : {
            "required" : true
        }
    },
    "minMax" : {
        "dataSetIds" : {
            "required" : true
        }
    },
    "concept" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 3, 10 ]
        }
    },
    "dataElement" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ],
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : true
        },
        "shortName" : {
            "required" : true,
            "rangelength" : [ 2, 50 ],
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : true
        },
        "code" : {
            "rangelength" : [ 0, 50 ],
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : false
        },
        "formName" : {
            "rangelength" : [ 2, 160 ],
            "alphanumericwithbasicpuncspaces" : false,
            "notOnlyDigits" : false
        },
        "url" : {
            "url" : true,
            "rangelength" : [ 0, 255 ]
        }
    },
    "dateElementCategoryCombo" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "selectedList" : {
            "required" : true
        }
    },
    "dateElementCategory" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "memberValidator" : {
            "required" : true
        }
    },
    "dataElementGroup" : {
        "name" : {
            "required" : true,
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : true,
            "rangelength" : [ 2, 230 ]
        },
        "shortName" : {
            "required" : true,
            "rangelength" : [ 2, 50 ],
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : true
        },
        "code" : {
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : false,
            "rangelength" : [ 0, 50 ]
        }
    },
    "dataElementGroupSet" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        }
    },
    "indicator" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ],
            "alphanumericwithbasicpuncspaces" : true,
            "nostartwhitespace" : true
        },
        "shortName" : {
            "required" : true,
            "rangelength" : [ 2, 50 ],
            "alphanumericwithbasicpuncspaces" : true
        },
        "code" : {
            "rangelength" : [ 0, 50 ],
            "alphanumericwithbasicpuncspaces" : true,
            "notOnlyDigits" : false
        },
        "url" : {
            "url" : true,
            "rangelength" : [ 0, 255 ]
        },
        "indicatorTypeId" : {
            "required" : true
        },
        "denominator" : {
            "required" : true
        }
    },
    "indicatorGroup" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ],
            "alphanumericwithbasicpuncspaces" : true
        }
    },
    "indicatorGroupSet" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        }
    },
    "indicatorType" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ],
            "alphanumericwithbasicpuncspaces" : true
        },
        "factor" : {
            "required" : true,
            "rangelength" : [ 1, 10 ],
            "digits" : true
        }
    },
    "validationRule" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "importance" : {
            "required" : true
        },
        "ruleType" : {
            "required" : true
        },
        "organisationUnitLevel" : {
        	"number" : true,
        	"min": 1,
        	"max": 999
        },
        "periodTypeName" : {
            "required" : true
        },
        "sequentialSampleCount" : {
        	"number" : true,
        	"min": 0,
        	"max": 10
        },
        "annualSampleCount" : {
        	"number" : true,
        	"min": 0,
        	"max": 10
        },
        "sequentialSkipCount" : {
        	"number" : true,
        	"min": 0,
        	"max": 10
        },
        "operator" : {
            "required" : true
        },
        "leftSideExpression" : {
            "required" : true
        },
        "rightSideExpression" : {
            "required" : true
        }
    },
    "validationRuleGroup" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        }
    },
    "constant" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "value" : {
            "required" : true
        }
    },
    "attribute" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ],
            "alphanumericwithbasicpuncspaces" : true
        }
    },
    "attributeOption" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ],
            "alphanumericwithbasicpuncspaces" : true
        }
    },
    "trackedEntityAttribute" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "shortName" : {
            "required" : true,
            "rangelength" : [ 2, 50 ]
        }
    },
    "relationshipType" : {
        "aIsToB" : {
            "required" : true,
            "rangelength" : [ 2, 160 ]
        },
        "bIsToA" : {
            "required" : true,
            "rangelength" : [ 2, 160 ]
        },
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 160 ]
        }
    },
    "trackedEntity" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2,160 ]
        }
    },
    "program" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        },
        "shortName" : {
            "required" : true,
            "rangelength" : [ 2, 50 ]
        },
        "trackedEntityId" : {
            "required" : true
        },
        "dateOfIncidentDescription" : {
            "required" : true,
            "rangelength" : [ 2,160 ]
        },
        "dateOfEnrollmentDescription" : {
            "required" : true,
            "rangelength" : [ 2,160 ]
        },
        "compulsaryIdentifier" : {
            "required" : true
        },
        "version" : {
            "required" : true,
            "integer":true
        }
    },
    "programStage" : {
        "name" : {
            "required" : true,
            "rangelength" : [2,230]
        },
        "description" : {
            "required" : false
        },
        "reportDateDescription" : {
            "required" : true,
            "rangelength" : [2,160]
        },
        "minDaysFromStart" : {
            "number" : true,
            "min" : 0
        },
        "reportDateToUse" : {
            "required" : true
        },
        "standardInterval" : {
            "number" : true
        }
    },
    "programRule" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2,230 ]
        },
        "description" : {
            "required" : true,
            "rangelength" : [ 2,160 ]
        },
        "condition" : {
            "required" : true,
            "rangelength" : [ 2,255 ]
        }
    },
    "programIndicator" : {
        "name" : {
            "required" : true,
            "rangelength" : [2,230]
        },
        "shortName" : {
            "required" : true,
            "rangelength" : [2,50]
        },
        "code" : {
            "rangelength" : [2,160]
        },
        "rootDate" : {
            "required" : true
        },
        "expression" : {
            "required" : true
        },
        "valueType" : {
            "required" : true
        }
    },
    "programIndicatorGroup" : {
        "name" : {
            "required" : true,
            "rangelength" : [ 2, 230 ]
        }
    },
    "programStageSection" : {
        "name" : {
            "required" : true,
            "rangelength" : [2,230]
        },
        "dataElementList" : {
            "required" : true
        }
    },
    "validationCriteria" : {
        "name" : {
            "required" : true,
            "rangelength" : [2,230]
        },
        "description" : {
            "required" : true,
            "rangelength" : [2,254]
        },
        "property" : {
            "required" : true
        },
        "value" : {
            "required" : true
        }
    },
    "SMSConfig" : {
        "pollingInterval" : {
            "required" : true,
            "digits" : true
        }
    },
    "autoUpdateClient" : {
        "version" : {
            "required" : true,
            "number" : true
        }
    },
    "dataApprovalLevel" : {
        "organisationUnitLevel" : {
            "required" : true
        }
    }
};
