Stackato Jenkins plugin
=======================

This is a prototype that can push simple apps to Stackato at the end of a Jenkins build.

Installing:
-----------
Due to conflicts between the versions of Spring used by Jenkins and the CF Java client, this plugin uses a modified version of the CF Java client with shaded libraries.  
The jar file is included at the root of this project. To install it to your local Maven repository, run:

```
mvn org.apache.maven.plugins:maven-install-plugin:2.5.1:install-file -Dfile=cloudfoundry-client-lib-shaded-1.0.3.jar
```

Debugging:
----------
This will launch a Jenkins instance for you with the plugin pre-installed. The Jenkins files will be stored in the `work` folder.  
The Jenkins instance will be accessible at http://localhost:8090/jenkins. You can also remote debug the plugin at port 8000.

```
export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
mvn hpi:run -Djetty.port=8090
```

Packaging:
----------
If you already have a working Jenkins instance, use this command to create an .hpi file. You can then upload it to your Jenkins instance.

```
mvn install
```
