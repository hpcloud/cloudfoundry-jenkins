Cloud Foundry Jenkins plugin
=======================

This plugin can push apps to a Cloud Foundry platform at the end of a Jenkins build. You can either use the 
configuration of a manifest.yml file, or write your settings in the Jenkins build's configuration page.

Installing:
-----------
Due to conflicts between the versions of Spring used by Jenkins and the CF Java client, this plugin uses a modified 
version of the CF Java client with shaded libraries.  
The jar file is included in the `lib` subfolder. To install it to your local Maven repository, run:

```
mvn org.apache.maven.plugins:maven-install-plugin:2.5.1:install-file -Dfile=lib/cloudfoundry-client-lib-shaded-1.0.3.jar
```

Debugging:
----------
This will launch a Jenkins instance for you with the plugin pre-installed. The Jenkins files will be stored in the 
`work` folder.  
The Jenkins instance will be accessible at http://localhost:8090/jenkins.

```
mvn hpi:run -Djetty.port=8090
```

You can also enable remote debugging at port 8000 by setting some Maven options before running the previous command:

```
export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
```

Packaging:
----------
If you already have a working Jenkins instance, use this command to create an .hpi file. You can then upload it to your 
Jenkins instance.

```
mvn install -Dmaven.test.skip=true
```

If you have a working Cloud Foundry platform and want to run the tests before building, you will need to specify some 
arguments for the tests:

```
mvn install -Dtarget=<target URL> -Dusername=<username> -Dpassword=<password> -Dorg=<org> -Dspace=<space>
```
