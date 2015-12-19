h1. JIRA Snap
Speed up release function of JIRA versions page by magnitudes. Configure your components, enter credentials and use simple but fast front end for that.

[![Build Status](https://travis-ci.org/Husterknupp/jira-snap.svg)](https://travis-ci.org/Husterknupp/jira-snap)

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
java -jar jira-rest-client-standalone-1.1.0.jar
java -jar target/jira-version-cleaner-0.1.0-SNAPSHOT.jar server config.json
```

**Frontend**

`http://localhost:2000/versions`
