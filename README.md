# krautBOT-discord
Runs a cron that collects data from Google Analytics and publishes it to a specific Discord channel.

## Required properties
The properties can be provided in various ways that [Quarkus](https://quarkus.io/) supports.

```properties
cron.expression=

analytics.application.name=
analytics.key.file.location=
analytics.view.id=

discord.token=
```