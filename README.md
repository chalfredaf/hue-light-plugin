# hue-light plugin for Jenkins CI

Use the awesome [Philips hue lights](https://www.meethue.com) to show the state of your builds.

The following states are implemented:

* building => blue
* success => green
* fatal errors => red
* no fatal errors ("unstable") => yellow


## Configuration

1. Create a new user one the hue bridge (http://developers.meethue.com/gettingstarted.html)
2. Open Global Setting and set the
  * IP address of the hue bridge
  * Authorized username of the hue bridge
3. Create a new job or modify an existing job
  * Add post-build action **Colorize Hue-Light**
  * Set the id of the light you want to control


## ÅF fork

Implementing support for multiple bridges. Global settings of bridge IP and
user are now defaults. Each individual job has settings for bridge IP and user
in post-build action.

Implementing support for pipeline scripts. Format is presented in the Snippet
Generator.

### Usage
#### Freestyle Projects
Normal usage with added fields for bridge IP and bridge user.
#### Pipeline Projects
A pipeline step can be called with the command 'huelight'.
The pipeline command has two uses, the prebuild indication and the build
result indication.

The following parameters are required.
* bridgeIp  - Ip and port of the bridges (required)
* bridgeUsername  - Bridge bridgeUsername (required)
* lightId - Id of a light or a comma separeted list of id's (required)
* notifierType  - set to 'PreBuild' for prebuild notification (only required for prebuild)
* result  - set to ${currentBuild.currentResult} (only required for build result indication)

The following colorsetting parameters are optional and uses four standard colors ('blue','green', 'yellow', 'red').
* preBuild
* goodBuild
* badBuild
* unstableBuild

Example:
```
huelight bridgeIp: '<IP:port>', bridgeUsername: '<username>', lightId:'<id>', notifierType:'PreBuild'
<step>...
<step>...
huelight bridgeIp: '<IP:port>', bridgeUsername: '<username>', lightId:'<id>', result:'${currentBuild.currentResult}'
 ```

## License

This plugin has been released under the MIT License. It uses

Copyright (c) 2013 Mathias Nestler

Also included is a copy of the [Jue library](https://github.com/Q42/Jue), licensed under the MIT License too.
