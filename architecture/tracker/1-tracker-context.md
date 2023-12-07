**Tracker context**
This context serves functionality for individual level data. It contains a set of APIs that allows importing and exporting individual level data, as well as end user functionality for capturing and vewing individual level data through the Capture web app. The main end user for these apps is data officers and clinical management users - analytical use is covered by the _analtyics context_.
In addition to the end user functionality, the tracker context serves a set of APIs that is shared between the end user facing Capture app, the Android app described in the _Android context_ and externally written scripts and apps.

The tracker context holds the following containers:
- Capture web app - end user functionality for capturing individual level data(web)
- Tracker capture web app  - legacy app for end user functionality for capturing individual level data(web)
- /tracker - APIs for storing and retreiving individual level data
- various legacy endpoints for storing and retreiving individual level data