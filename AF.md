# ÅF fork

Implementing support for multiple bridges and pipeline. Global settings of bridge IP and
user are now defaults. Each individual job has selectable settings for bridge IP and user
in post-build action.


## Usage
### Freestyle Projects
Normal usage with added fields for bridge IP and bridge user for individual jobs.
If not specified the global settings will be used as default.
### Pipeline Projects
A pipeline step can be called with the command 'huelight'.
The pipeline command has two uses, the prebuild indication and the build
result indication. The build result indication is placed in a 'post'-block.

The following parameters are required.
* bridgeIp  - Ip address of the hue bridge (required)
* bridgeUsername  - Authorized username of the hue bridge (required)
* lightId - Id of a light or a comma separeted list of id's (required)
* notifierType  - set to 'PreBuild' for prebuild notification (only required for prebuild)
* result  - set to ${currentBuild.currentResult} (only required for build result indication)


The following parameters are optional and use four standard colors ('blue','green', 'yellow', 'red').
* preBuild
* goodBuild
* badBuild
* unstableBuild

Basic Example:
```
steps{
  huelight bridgeIp: '<IP:port>', bridgeUsername: '<username>', lightId:'<id>', notifierType:'PreBuild'
  <step>...
  ...
}
post{
  always{
    huelight bridgeIp: '<IP:port>', bridgeUsername: '<username>', lightId:'<id>', result:'${currentBuild.currentResult}'
  }
}
 ```

Example with optional parameter 'preBuild':
 ```
 huelight bridgeIp: '<IP:port>', bridgeUsername: '<username>', lightId:'<id>', notifierType:'PreBuild', preBuild: 'yellow'
 ```
