
# DHIS 2

DHIS 2 is a flexible information system for data capture, management, validation, analytics and visualization. It allows for data capture through clients ranging from Web browsers, Android devices, Java feature phones and SMS. DHIS 2 features data visualization apps for dashboards, pivot tables, charting and GIS. It provides metadata management and configuration. The data model and services are exposed through a RESTful Web API.

## Overview

Issues can be reported and browsed in [JIRA](https://jira.dhis2.org).

For documentation please visit the [doc page](https://www.dhis2.org/documentation/).

You can download pre-built WAR files from the [continuous integration server](http://ci.dhis2.org/).

You can explore various demos of DHIS 2 in the [play environment](https://play.dhis2.org/).

The software is open source and released under the [BSD license](https://opensource.org/licenses/BSD-2-Clause).

For general info please visit the [project web page](https://www.dhis2.org/).

For software development guides please visit the [project wiki](http://dhis2.github.io/).

## Build process

This repository contains the source code for the server-side component of DHIS 2, which is developed in [Java](https://www.java.com/en/) and built with [Maven](https://maven.apache.org/). 

To build it you must first install the root POM file, navigate to the dhis-web directory and then build the web POM file.

Check [contributing](https://github.com/dhis2/dhis2-core/blob/master/CONTRIBUTING.md) for the procedure to make it run locally.

[![Build Status](https://travis-ci.org/dhis2/dhis2-core.svg?branch=master)](https://travis-ci.org/dhis2/dhis2-core)
