Tired of waiting long for the versions page of your project? Send $ 14.90 to mock@tober.com and use the Jira Version Cleaner right now. [![Build Status](https://travis-ci.org/Husterknupp/jira-snap.svg)](https://travis-ci.org/Husterknupp/jira-snap)

**config.json**
```
{
  "componentsToPollFor": [
    "component-a",
    "component-b"
  ],
  "jiraUrl": "http://our.jira.net",
  "username": "john",
  ...
}
```

**Compile jira-rest-standalone (or download from release section)**
```
cd jira-rest-client-standalone
mvn clean package
cp target/jira-rest-client-standalone-1.2.0-SNAPSHOT.jar ../
```

**Compile jira-version-cleaner**
```
cd ..
mvn clean package
java -jar jira-rest-client-standalone-1.2.0-SNAPSHOT.jar
java -jar target/jira-version-cleaner-0.1.0-SNAPSHOT.jar server config.json
```

**Frontend**

`http://localhost:2000/versions`
