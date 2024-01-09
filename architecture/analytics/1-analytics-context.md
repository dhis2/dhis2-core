** Analytics context **

Analytics context serves the functionality for analysing data. The context contanis web apps for showing aggregated analysis to end users, as well as apps used by admin users to produce reports for themselves or sharing with other users. The container also serves an API on the DHIS2 core that is used by these web apps as well as externally developed apps and scripts.

The data in question here is entered in the _platform context_, _tracker context_ and the _android context_, and entering such data is not in scope for the analytics context. The analytics context in DHIS2 contains tools for combining and visualising data in a way that makes a user capable of seeing trends and convey the story behind the numbers entered. 

There is 2 main data models that is visualised by the analytics context:
- Aggregate data model
  - The aggregate data model contains data that is already aggregated(usually counted) and entered in a context(often from one clinic for one month).
  - The tools in analtyics allows hirearchical aggregation, as well as slicing and recombining of these data.
- Tracker data model
  - Line listing of individual records or queries for the purposes


Analysing data offline in Android is out of scope for the analytics context, as this is described in the _android context_.
