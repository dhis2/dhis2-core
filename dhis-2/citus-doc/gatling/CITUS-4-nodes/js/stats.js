var stats = {
    type: "GROUP",
name: "All Requests",
path: "",
pathFormatted: "group_missing-name--1146707516",
stats: {
    "name": "All Requests",
    "numberOfRequests": {
        "total": "77",
        "ok": "77",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "606",
        "ok": "606",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "2600",
        "ok": "2600",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1414",
        "ok": "1414",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "369",
        "ok": "369",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1388",
        "ok": "1388",
        "ko": "-"
    },
    "percentiles2": {
        "total": "1623",
        "ok": "1623",
        "ko": "-"
    },
    "percentiles3": {
        "total": "2143",
        "ok": "2143",
        "ko": "-"
    },
    "percentiles4": {
        "total": "2355",
        "ok": "2355",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 3,
    "percentage": 3.896103896103896
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 21,
    "percentage": 27.27272727272727
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 53,
    "percentage": 68.83116883116884
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "4.53",
        "ok": "4.53",
        "ko": "-"
    }
},
contents: {
"req_-api-analytics--690187672": {
        type: "REQUEST",
        name: "/api/analytics/enrollments/query/SSLpOM0r1U7?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX:IN:1;NV;0,s53RFfXA75f[0].LauCl9aicLX:IN:1;NV;0,HI9Y7BKVNnC,JQC3DLdCWK8:GE:0,dkaVGV1WUCR,lXDpHyE8wgb&headers=ouname,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX,s53RFfXA75f[0].LauCl9aicLX,HI9Y7BKVNnC,JQC3DLdCWK8,dkaVGV1WUCR,lXDpHyE8wgb&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
path: "/api/analytics/enrollments/query/SSLpOM0r1U7?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX:IN:1;NV;0,s53RFfXA75f[0].LauCl9aicLX:IN:1;NV;0,HI9Y7BKVNnC,JQC3DLdCWK8:GE:0,dkaVGV1WUCR,lXDpHyE8wgb&headers=ouname,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX,s53RFfXA75f[0].LauCl9aicLX,HI9Y7BKVNnC,JQC3DLdCWK8,dkaVGV1WUCR,lXDpHyE8wgb&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
pathFormatted: "req_-api-analytics--690187672",
stats: {
    "name": "/api/analytics/enrollments/query/SSLpOM0r1U7?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX:IN:1;NV;0,s53RFfXA75f[0].LauCl9aicLX:IN:1;NV;0,HI9Y7BKVNnC,JQC3DLdCWK8:GE:0,dkaVGV1WUCR,lXDpHyE8wgb&headers=ouname,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX,s53RFfXA75f[0].LauCl9aicLX,HI9Y7BKVNnC,JQC3DLdCWK8,dkaVGV1WUCR,lXDpHyE8wgb&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
    "numberOfRequests": {
        "total": "10",
        "ok": "10",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "1151",
        "ok": "1151",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "2278",
        "ok": "2278",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1585",
        "ok": "1585",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "289",
        "ok": "289",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1578",
        "ok": "1578",
        "ko": "-"
    },
    "percentiles2": {
        "total": "1658",
        "ok": "1658",
        "ko": "-"
    },
    "percentiles3": {
        "total": "2043",
        "ok": "2043",
        "ko": "-"
    },
    "percentiles4": {
        "total": "2231",
        "ok": "2231",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 0,
    "percentage": 0.0
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 1,
    "percentage": 10.0
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 9,
    "percentage": 90.0
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "0.59",
        "ok": "0.59",
        "ko": "-"
    }
}
    },"req_-api-analytics--1703293855": {
        type: "REQUEST",
        name: "/api/analytics/enrollments/query/M3xtLkYBlKI?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,cl2RC5MLQYO:GE:0,gDgZ5oXCyWm,DishKl0ppXK,nO7nsEjYcp5,zyTL3AMIkf2,OHeZKzifNYE,d6Sr0B2NJYv,yZmG3RbbBKG,uvMKOn1oWvd.yhX7ljWZV9q:IN:NV,uvMKOn1oWvd.JhpYDsTUfi2:IN:1,CWaAcQYKVpq[1].dbMsAGvictz,CWaAcQYKVpq[2].dbMsAGvictz,CWaAcQYKVpq[0].dbMsAGvictz,CWaAcQYKVpq.ehBd9cR5bq4:EQ:NV,CWaAcQYKVpq.VNM6zoPECqd:GT:0,CWaAcQYKVpq.SaHE38QFFwZ:IN:HILLY_AND_PLATUE;PLATUE;HILLY&headers=ouname,cl2RC5MLQYO,gDgZ5oXCyWm,DishKl0ppXK,nO7nsEjYcp5,zyTL3AMIkf2,OHeZKzifNYE,d6Sr0B2NJYv,yZmG3RbbBKG,uvMKOn1oWvd.yhX7ljWZV9q,uvMKOn1oWvd.JhpYDsTUfi2,CWaAcQYKVpq[1].dbMsAGvictz,CWaAcQYKVpq[2].dbMsAGvictz,CWaAcQYKVpq[0].dbMsAGvictz,CWaAcQYKVpq.ehBd9cR5bq4,CWaAcQYKVpq.VNM6zoPECqd,CWaAcQYKVpq.SaHE38QFFwZ,createdbydisplayname&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&programStatus=COMPLETED,ACTIVE&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
path: "/api/analytics/enrollments/query/M3xtLkYBlKI?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,cl2RC5MLQYO:GE:0,gDgZ5oXCyWm,DishKl0ppXK,nO7nsEjYcp5,zyTL3AMIkf2,OHeZKzifNYE,d6Sr0B2NJYv,yZmG3RbbBKG,uvMKOn1oWvd.yhX7ljWZV9q:IN:NV,uvMKOn1oWvd.JhpYDsTUfi2:IN:1,CWaAcQYKVpq[1].dbMsAGvictz,CWaAcQYKVpq[2].dbMsAGvictz,CWaAcQYKVpq[0].dbMsAGvictz,CWaAcQYKVpq.ehBd9cR5bq4:EQ:NV,CWaAcQYKVpq.VNM6zoPECqd:GT:0,CWaAcQYKVpq.SaHE38QFFwZ:IN:HILLY_AND_PLATUE;PLATUE;HILLY&headers=ouname,cl2RC5MLQYO,gDgZ5oXCyWm,DishKl0ppXK,nO7nsEjYcp5,zyTL3AMIkf2,OHeZKzifNYE,d6Sr0B2NJYv,yZmG3RbbBKG,uvMKOn1oWvd.yhX7ljWZV9q,uvMKOn1oWvd.JhpYDsTUfi2,CWaAcQYKVpq[1].dbMsAGvictz,CWaAcQYKVpq[2].dbMsAGvictz,CWaAcQYKVpq[0].dbMsAGvictz,CWaAcQYKVpq.ehBd9cR5bq4,CWaAcQYKVpq.VNM6zoPECqd,CWaAcQYKVpq.SaHE38QFFwZ,createdbydisplayname&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&programStatus=COMPLETED,ACTIVE&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
pathFormatted: "req_-api-analytics--1703293855",
stats: {
    "name": "/api/analytics/enrollments/query/M3xtLkYBlKI?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,cl2RC5MLQYO:GE:0,gDgZ5oXCyWm,DishKl0ppXK,nO7nsEjYcp5,zyTL3AMIkf2,OHeZKzifNYE,d6Sr0B2NJYv,yZmG3RbbBKG,uvMKOn1oWvd.yhX7ljWZV9q:IN:NV,uvMKOn1oWvd.JhpYDsTUfi2:IN:1,CWaAcQYKVpq[1].dbMsAGvictz,CWaAcQYKVpq[2].dbMsAGvictz,CWaAcQYKVpq[0].dbMsAGvictz,CWaAcQYKVpq.ehBd9cR5bq4:EQ:NV,CWaAcQYKVpq.VNM6zoPECqd:GT:0,CWaAcQYKVpq.SaHE38QFFwZ:IN:HILLY_AND_PLATUE;PLATUE;HILLY&headers=ouname,cl2RC5MLQYO,gDgZ5oXCyWm,DishKl0ppXK,nO7nsEjYcp5,zyTL3AMIkf2,OHeZKzifNYE,d6Sr0B2NJYv,yZmG3RbbBKG,uvMKOn1oWvd.yhX7ljWZV9q,uvMKOn1oWvd.JhpYDsTUfi2,CWaAcQYKVpq[1].dbMsAGvictz,CWaAcQYKVpq[2].dbMsAGvictz,CWaAcQYKVpq[0].dbMsAGvictz,CWaAcQYKVpq.ehBd9cR5bq4,CWaAcQYKVpq.VNM6zoPECqd,CWaAcQYKVpq.SaHE38QFFwZ,createdbydisplayname&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&programStatus=COMPLETED,ACTIVE&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
    "numberOfRequests": {
        "total": "11",
        "ok": "11",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "914",
        "ok": "914",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "1688",
        "ok": "1688",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1371",
        "ok": "1371",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "227",
        "ok": "227",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1448",
        "ok": "1448",
        "ko": "-"
    },
    "percentiles2": {
        "total": "1522",
        "ok": "1522",
        "ko": "-"
    },
    "percentiles3": {
        "total": "1671",
        "ok": "1671",
        "ko": "-"
    },
    "percentiles4": {
        "total": "1685",
        "ok": "1685",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 0,
    "percentage": 0.0
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 3,
    "percentage": 27.27272727272727
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 8,
    "percentage": 72.72727272727273
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "0.65",
        "ok": "0.65",
        "ko": "-"
    }
}
    },"req_-api-analytics---46546211": {
        type: "REQUEST",
        name: "/api/analytics/enrollments/query/pMIglSEqPGS?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,JxX12764mmB,ENRjVGxVL6l:ILIKE:a,sB1IHYu2xQT:!ILIKE:r,D917mo9Whvn:IN:1;NV&filter=qDVxzO0Ilkq:GT:0&headers=ouname,JxX12764mmB,ENRjVGxVL6l,sB1IHYu2xQT,D917mo9Whvn&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
path: "/api/analytics/enrollments/query/pMIglSEqPGS?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,JxX12764mmB,ENRjVGxVL6l:ILIKE:a,sB1IHYu2xQT:!ILIKE:r,D917mo9Whvn:IN:1;NV&filter=qDVxzO0Ilkq:GT:0&headers=ouname,JxX12764mmB,ENRjVGxVL6l,sB1IHYu2xQT,D917mo9Whvn&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
pathFormatted: "req_-api-analytics---46546211",
stats: {
    "name": "/api/analytics/enrollments/query/pMIglSEqPGS?dimension=ou:IWp9dQGM0bS;W6sNfkJcXGC;LEVEL-b5jE033nBqM;LEVEL-vFr4zVw6Avn;OU_GROUP-YXlxwXEWex6;OU_GROUP-roGQQw4l3dW;OU_GROUP-VePuVPFoyJ2,JxX12764mmB,ENRjVGxVL6l:ILIKE:a,sB1IHYu2xQT:!ILIKE:r,D917mo9Whvn:IN:1;NV&filter=qDVxzO0Ilkq:GT:0&headers=ouname,JxX12764mmB,ENRjVGxVL6l,sB1IHYu2xQT,D917mo9Whvn&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
    "numberOfRequests": {
        "total": "13",
        "ok": "13",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "735",
        "ok": "735",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "1472",
        "ok": "1472",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1176",
        "ok": "1176",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "204",
        "ok": "204",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1190",
        "ok": "1190",
        "ko": "-"
    },
    "percentiles2": {
        "total": "1285",
        "ok": "1285",
        "ko": "-"
    },
    "percentiles3": {
        "total": "1467",
        "ok": "1467",
        "ko": "-"
    },
    "percentiles4": {
        "total": "1471",
        "ok": "1471",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 1,
    "percentage": 7.6923076923076925
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 7,
    "percentage": 53.84615384615385
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 5,
    "percentage": 38.46153846153847
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "0.76",
        "ok": "0.76",
        "ko": "-"
    }
}
    },"req_-api-analytics--351119387": {
        type: "REQUEST",
        name: "/api/analytics/enrollments/query/Lt6P15ps7f6?dimension=ou:USER_ORGUNIT,ntelZthDPpR,WlgSirxU9bG,oindugucx72,OrvVU0I9wTT:EQ:1&headers=enrollmentdate,ouname,ntelZthDPpR,WlgSirxU9bG,oindugucx72,OrvVU0I9wTT&totalPages=false&enrollmentDate=THIS_YEAR,LAST_5_YEARS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
path: "/api/analytics/enrollments/query/Lt6P15ps7f6?dimension=ou:USER_ORGUNIT,ntelZthDPpR,WlgSirxU9bG,oindugucx72,OrvVU0I9wTT:EQ:1&headers=enrollmentdate,ouname,ntelZthDPpR,WlgSirxU9bG,oindugucx72,OrvVU0I9wTT&totalPages=false&enrollmentDate=THIS_YEAR,LAST_5_YEARS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
pathFormatted: "req_-api-analytics--351119387",
stats: {
    "name": "/api/analytics/enrollments/query/Lt6P15ps7f6?dimension=ou:USER_ORGUNIT,ntelZthDPpR,WlgSirxU9bG,oindugucx72,OrvVU0I9wTT:EQ:1&headers=enrollmentdate,ouname,ntelZthDPpR,WlgSirxU9bG,oindugucx72,OrvVU0I9wTT&totalPages=false&enrollmentDate=THIS_YEAR,LAST_5_YEARS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
    "numberOfRequests": {
        "total": "12",
        "ok": "12",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "802",
        "ok": "802",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "1685",
        "ok": "1685",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1318",
        "ok": "1318",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "277",
        "ok": "277",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1361",
        "ok": "1361",
        "ko": "-"
    },
    "percentiles2": {
        "total": "1551",
        "ok": "1551",
        "ko": "-"
    },
    "percentiles3": {
        "total": "1658",
        "ok": "1658",
        "ko": "-"
    },
    "percentiles4": {
        "total": "1680",
        "ok": "1680",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 0,
    "percentage": 0.0
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 5,
    "percentage": 41.66666666666667
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 7,
    "percentage": 58.333333333333336
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "0.71",
        "ok": "0.71",
        "ko": "-"
    }
}
    },"req_-api-analytics---741163252": {
        type: "REQUEST",
        name: "/api/analytics/enrollments/query/KYzHf1Ta6C4?dimension=ou:USER_ORGUNIT,BdvE9shT6GX,bE3YcdNxA3g:GT:1&headers=enrollmentdate,ouname,BdvE9shT6GX,bE3YcdNxA3g&totalPages=false&enrollmentDate=THIS_YEAR,LAST_YEAR&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
path: "/api/analytics/enrollments/query/KYzHf1Ta6C4?dimension=ou:USER_ORGUNIT,BdvE9shT6GX,bE3YcdNxA3g:GT:1&headers=enrollmentdate,ouname,BdvE9shT6GX,bE3YcdNxA3g&totalPages=false&enrollmentDate=THIS_YEAR,LAST_YEAR&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
pathFormatted: "req_-api-analytics---741163252",
stats: {
    "name": "/api/analytics/enrollments/query/KYzHf1Ta6C4?dimension=ou:USER_ORGUNIT,BdvE9shT6GX,bE3YcdNxA3g:GT:1&headers=enrollmentdate,ouname,BdvE9shT6GX,bE3YcdNxA3g&totalPages=false&enrollmentDate=THIS_YEAR,LAST_YEAR&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
    "numberOfRequests": {
        "total": "10",
        "ok": "10",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "1175",
        "ok": "1175",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "1744",
        "ok": "1744",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1501",
        "ok": "1501",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "195",
        "ok": "195",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1548",
        "ok": "1548",
        "ko": "-"
    },
    "percentiles2": {
        "total": "1648",
        "ok": "1648",
        "ko": "-"
    },
    "percentiles3": {
        "total": "1742",
        "ok": "1742",
        "ko": "-"
    },
    "percentiles4": {
        "total": "1744",
        "ok": "1744",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 0,
    "percentage": 0.0
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 1,
    "percentage": 10.0
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 9,
    "percentage": 90.0
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "0.59",
        "ok": "0.59",
        "ko": "-"
    }
}
    },"req_-api-analytics--238133834": {
        type: "REQUEST",
        name: "/api/analytics/enrollments/query/SSLpOM0r1U7?dimension=ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN;USER_ORGUNIT_GRANDCHILDREN,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX:IN:1;NV;0,s53RFfXA75f[0].LauCl9aicLX:IN:1;NV;0,HI9Y7BKVNnC,JQC3DLdCWK8:GE:0,dkaVGV1WUCR,lXDpHyE8wgb&headers=ouname,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX,s53RFfXA75f[0].LauCl9aicLX,HI9Y7BKVNnC,JQC3DLdCWK8,dkaVGV1WUCR,lXDpHyE8wgb&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
path: "/api/analytics/enrollments/query/SSLpOM0r1U7?dimension=ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN;USER_ORGUNIT_GRANDCHILDREN,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX:IN:1;NV;0,s53RFfXA75f[0].LauCl9aicLX:IN:1;NV;0,HI9Y7BKVNnC,JQC3DLdCWK8:GE:0,dkaVGV1WUCR,lXDpHyE8wgb&headers=ouname,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX,s53RFfXA75f[0].LauCl9aicLX,HI9Y7BKVNnC,JQC3DLdCWK8,dkaVGV1WUCR,lXDpHyE8wgb&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
pathFormatted: "req_-api-analytics--238133834",
stats: {
    "name": "/api/analytics/enrollments/query/SSLpOM0r1U7?dimension=ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN;USER_ORGUNIT_GRANDCHILDREN,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX:IN:1;NV;0,s53RFfXA75f[0].LauCl9aicLX:IN:1;NV;0,HI9Y7BKVNnC,JQC3DLdCWK8:GE:0,dkaVGV1WUCR,lXDpHyE8wgb&headers=ouname,itJqasg1QiC,jMotFa52JAq,s53RFfXA75f.LTdvha8zapG,s53RFfXA75f[-1].LauCl9aicLX,s53RFfXA75f[0].LauCl9aicLX,HI9Y7BKVNnC,JQC3DLdCWK8,dkaVGV1WUCR,lXDpHyE8wgb&totalPages=false&lastUpdated=2021-08-01_2024-11-23&enrollmentDate=LAST_12_MONTHS&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
    "numberOfRequests": {
        "total": "13",
        "ok": "13",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "606",
        "ok": "606",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "2139",
        "ok": "2139",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1217",
        "ok": "1217",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "371",
        "ok": "371",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1218",
        "ok": "1218",
        "ko": "-"
    },
    "percentiles2": {
        "total": "1331",
        "ok": "1331",
        "ko": "-"
    },
    "percentiles3": {
        "total": "1749",
        "ok": "1749",
        "ko": "-"
    },
    "percentiles4": {
        "total": "2061",
        "ok": "2061",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 2,
    "percentage": 15.384615384615385
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 4,
    "percentage": 30.76923076923077
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 7,
    "percentage": 53.84615384615385
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "0.76",
        "ok": "0.76",
        "ko": "-"
    }
}
    },"req_-api-analytics---2094250091": {
        type: "REQUEST",
        name: "/api/analytics/enrollments/query/YcxRnVWkbQ1?dimension=ou:IWp9dQGM0bS,sB1IHYu2xQT,ENRjVGxVL6l,oindugucx72,GIb6Ge9PCc4.pXGhYbf5cAP,GIb6Ge9PCc4.LNRDN39NSfY,G0ePNuYPT87.fkqcoDLvzED,cSkxPgrdUKE.kBBrPq9mMEw,F9CrPrvrtb1.bX9jmncMHLC:IN:MEASLES,r9R6DTQdVgR.BLiSlEmoEQH&headers=enrollmentdate,ouname,sB1IHYu2xQT,ENRjVGxVL6l,oindugucx72,GIb6Ge9PCc4.pXGhYbf5cAP,GIb6Ge9PCc4.LNRDN39NSfY,G0ePNuYPT87.fkqcoDLvzED,cSkxPgrdUKE.kBBrPq9mMEw,F9CrPrvrtb1.bX9jmncMHLC,r9R6DTQdVgR.BLiSlEmoEQH&totalPages=false&enrollmentDate=LAST_YEAR&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
path: "/api/analytics/enrollments/query/YcxRnVWkbQ1?dimension=ou:IWp9dQGM0bS,sB1IHYu2xQT,ENRjVGxVL6l,oindugucx72,GIb6Ge9PCc4.pXGhYbf5cAP,GIb6Ge9PCc4.LNRDN39NSfY,G0ePNuYPT87.fkqcoDLvzED,cSkxPgrdUKE.kBBrPq9mMEw,F9CrPrvrtb1.bX9jmncMHLC:IN:MEASLES,r9R6DTQdVgR.BLiSlEmoEQH&headers=enrollmentdate,ouname,sB1IHYu2xQT,ENRjVGxVL6l,oindugucx72,GIb6Ge9PCc4.pXGhYbf5cAP,GIb6Ge9PCc4.LNRDN39NSfY,G0ePNuYPT87.fkqcoDLvzED,cSkxPgrdUKE.kBBrPq9mMEw,F9CrPrvrtb1.bX9jmncMHLC,r9R6DTQdVgR.BLiSlEmoEQH&totalPages=false&enrollmentDate=LAST_YEAR&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
pathFormatted: "req_-api-analytics---2094250091",
stats: {
    "name": "/api/analytics/enrollments/query/YcxRnVWkbQ1?dimension=ou:IWp9dQGM0bS,sB1IHYu2xQT,ENRjVGxVL6l,oindugucx72,GIb6Ge9PCc4.pXGhYbf5cAP,GIb6Ge9PCc4.LNRDN39NSfY,G0ePNuYPT87.fkqcoDLvzED,cSkxPgrdUKE.kBBrPq9mMEw,F9CrPrvrtb1.bX9jmncMHLC:IN:MEASLES,r9R6DTQdVgR.BLiSlEmoEQH&headers=enrollmentdate,ouname,sB1IHYu2xQT,ENRjVGxVL6l,oindugucx72,GIb6Ge9PCc4.pXGhYbf5cAP,GIb6Ge9PCc4.LNRDN39NSfY,G0ePNuYPT87.fkqcoDLvzED,cSkxPgrdUKE.kBBrPq9mMEw,F9CrPrvrtb1.bX9jmncMHLC,r9R6DTQdVgR.BLiSlEmoEQH&totalPages=false&enrollmentDate=LAST_YEAR&displayProperty=NAME&outputType=ENROLLMENT&pageSize=100&page=1&includeMetadataDetails=true&relativePeriodDate=2023-11-14",
    "numberOfRequests": {
        "total": "8",
        "ok": "8",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "1315",
        "ok": "1315",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "2600",
        "ok": "2600",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1998",
        "ok": "1998",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "351",
        "ok": "351",
        "ko": "-"
    },
    "percentiles1": {
        "total": "1992",
        "ok": "1992",
        "ko": "-"
    },
    "percentiles2": {
        "total": "2179",
        "ok": "2179",
        "ko": "-"
    },
    "percentiles3": {
        "total": "2471",
        "ok": "2471",
        "ko": "-"
    },
    "percentiles4": {
        "total": "2574",
        "ok": "2574",
        "ko": "-"
    },
    "group1": {
    "name": "t < 800 ms",
    "htmlName": "t < 800 ms",
    "count": 0,
    "percentage": 0.0
},
    "group2": {
    "name": "800 ms <= t < 1200 ms",
    "htmlName": "t >= 800 ms <br> t < 1200 ms",
    "count": 0,
    "percentage": 0.0
},
    "group3": {
    "name": "t >= 1200 ms",
    "htmlName": "t >= 1200 ms",
    "count": 8,
    "percentage": 100.0
},
    "group4": {
    "name": "failed",
    "htmlName": "failed",
    "count": 0,
    "percentage": 0.0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "0.47",
        "ok": "0.47",
        "ko": "-"
    }
}
    }
}

}

function fillStats(stat){
    $("#numberOfRequests").append(stat.numberOfRequests.total);
    $("#numberOfRequestsOK").append(stat.numberOfRequests.ok);
    $("#numberOfRequestsKO").append(stat.numberOfRequests.ko);

    $("#minResponseTime").append(stat.minResponseTime.total);
    $("#minResponseTimeOK").append(stat.minResponseTime.ok);
    $("#minResponseTimeKO").append(stat.minResponseTime.ko);

    $("#maxResponseTime").append(stat.maxResponseTime.total);
    $("#maxResponseTimeOK").append(stat.maxResponseTime.ok);
    $("#maxResponseTimeKO").append(stat.maxResponseTime.ko);

    $("#meanResponseTime").append(stat.meanResponseTime.total);
    $("#meanResponseTimeOK").append(stat.meanResponseTime.ok);
    $("#meanResponseTimeKO").append(stat.meanResponseTime.ko);

    $("#standardDeviation").append(stat.standardDeviation.total);
    $("#standardDeviationOK").append(stat.standardDeviation.ok);
    $("#standardDeviationKO").append(stat.standardDeviation.ko);

    $("#percentiles1").append(stat.percentiles1.total);
    $("#percentiles1OK").append(stat.percentiles1.ok);
    $("#percentiles1KO").append(stat.percentiles1.ko);

    $("#percentiles2").append(stat.percentiles2.total);
    $("#percentiles2OK").append(stat.percentiles2.ok);
    $("#percentiles2KO").append(stat.percentiles2.ko);

    $("#percentiles3").append(stat.percentiles3.total);
    $("#percentiles3OK").append(stat.percentiles3.ok);
    $("#percentiles3KO").append(stat.percentiles3.ko);

    $("#percentiles4").append(stat.percentiles4.total);
    $("#percentiles4OK").append(stat.percentiles4.ok);
    $("#percentiles4KO").append(stat.percentiles4.ko);

    $("#meanNumberOfRequestsPerSecond").append(stat.meanNumberOfRequestsPerSecond.total);
    $("#meanNumberOfRequestsPerSecondOK").append(stat.meanNumberOfRequestsPerSecond.ok);
    $("#meanNumberOfRequestsPerSecondKO").append(stat.meanNumberOfRequestsPerSecond.ko);
}
