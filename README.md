
# DHIS 2

DHIS 2 is a flexible information system for data capture, management, validation, analytics and visualization. It allows for data capture through clients ranging from Web browsers, Android devices, Java feature phones and SMS. DHIS 2 features data visualization apps for dashboards, pivot tables, charting and GIS. It provides metadata management and configuration. The data model and services are exposed through a RESTful Web API.

## Overview

Issues can be reported and browsed in [JIRA](https://jira.dhis2.org).

For documentation visit the [documentation portal](https://docs.dhis2.org/).

You can download pre-built WAR files from the [continuous integration server](https://ci.dhis2.org/).

You can explore various demos of DHIS 2 in the [play environment](https://play.dhis2.org/).

For support and discussions visit the [community forum](https://community.dhis2.org/).

For general info visit the [project web page](https://www.dhis2.org/).

For software developer resources visit the [developer portal](https://developers.dhis2.org/).

To contribute to the software read the [contributor guidelines](https://developers.dhis2.org/docs/conceptual/contribute).

The software is open source and released under the [BSD license](https://opensource.org/licenses/BSD-2-Clause).

## Build process

This repository contains the source code for the server-side component of DHIS 2, which is developed in [Java](https://www.java.com/en/) and built with [Maven](https://maven.apache.org/). 

To build it you must first install the root `POM` file, navigate to the `dhis-web` directory and then build the web `POM` file.

See the [contributing](https://github.com/dhis2/dhis2-core/blob/master/CONTRIBUTING.md) page to learn how to run locally.

[![Build Status](https://travis-ci.com/dhis2/dhis2-core.svg?branch=master)](https://travis-ci.org/dhis2/dhis2-core)
