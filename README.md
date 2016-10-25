# qaf-perfecto-support
Support project that provides ready to use steps for perfecto mobile cloud and report integration
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
