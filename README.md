# qaf-perfecto-support
Support project that provides ready to use steps for perfecto mobile cloud and report integration.
It will enable reportium inegration and will provide utility methods and ready to use stpes for behavior driven developement.

Properties:

 name             | posible value       | description 
 -----------------|---------------------|--------------
 project.name| your project name| project name to set in reportium default is xml suite name
 project.ver| version of the project| project version to set in reportium, default is 1.0
 JOB_NAME|CI job name|to set in reportium
 BUILD_NUMBER|CI build number|build number to set in reportium
 perfecto.default.driver.listener|`true`/`false`|enable or desable default driver listner (default is `true`}
 driver.pluginType | `eclipse` or `intellij` | set this property appropriate value to use device opened in IDE. Will work only if driver listener is enabled and provided same device id in capability

# Usage
## Maven

```xml
<repository>
    <id>perfectomobile</id>
    <name>Perfecto mobile</name>
    <url>http://repository-perfectomobile.forge.cloudbees.com/public</url>
</repository>

...

<!-- Perfecto support -->
<dependency>
    <groupId>com.qmetry</groupId>
    <artifactId>qaf-perfecto-support</artifactId>
    <version>latest.integration</version>
</dependency>

```

## Ivy

Add Perfecto’s repository to your `ivysettings.xml`:
```xml
<ivysettings>
    <settings defaultResolver="perfecto"/>
    <property name="perfecto-public"
              value="http://repository-perfectomobile.forge.cloudbees.com/public"/>
    <resolvers>
        <ibiblio name="perfecto" m2compatible="true" root="${perfecto-public}"/>
    </resolvers>
</ivysettings>
```


Add Perfecto support jar to `ivy.xml`:
```xml
<!-- Reportium SDK -->
<dependency org="com.qmetry" name="qaf-perfecto-support" rev="latest.integration" />

```
