# Semantic Turkey #

Semantic Turkey is a platform for Knowledge Acquisition and Management realized by the [ART Research Group](http://art.uniroma2.it) at the University of Rome Tor Vergata.

Everything about Semantic Turkey (user manual, build information etc..) is in its [official site](http://semanticturkey.uniroma2.it/)

## Building the project ##
### Requirements ###
1. a properly installed JDK version 8
2. a recent version of Apache Maven, preferably version 3.3.3 or superior (see section troubleshooting below)

### Building ###
just a plain:

```
#!cmd

mvn clean install
```

should suffice. In any case, more detailed instructions can be found here:

http://semanticturkey.uniroma2.it/documentation/dev/building.jsf

### Troubleshooting ###
If ST is built with an old version of Maven, it may happen that the build fails with an error like the one below:

```
[ERROR] Failed to execute goal org.apache.karaf.tooling:features-maven-plugin:2.3.3:add-features-to-repo (add-features-to-repo) on project st-builder: Error populating repository: /home/user/semantic-turkey/st-builder/target/classes (Is a directory) -> [Help 1]
```
The solution is to upgrade Maven to the minimum version advertised in the section on build requirements.

## Notes for previous releases ##

### Installing the old ST 0.9.1 on a recent Firefox (v. >= 29) ###

Due to changes in latest firefox UI (addon bar has been removed from FF), the pencil for annotations is not visible anymore. However, you can easily patch it for 0.9.1 by installing this further extension in Firefox
https://addons.mozilla.org/it/firefox/addon/the-addon-bar/