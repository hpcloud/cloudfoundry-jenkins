Debugging:
```
$ export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
$ mvn hpi:run -Djetty.port=8090
```

Packaging:
```
mvn package
```
