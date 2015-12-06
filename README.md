Tired of waiting long for the versions page of your project? Send $ 14.90 to mock@tober.com and use the Jira Version Cleaner right now.

**config.json**
```
{ 
    "jiraUrl": "http://our.jira.net",
    "username": "john",
    "password": "secr3t"
}
```

**Compile jira-rest-standalone**
```
cd jira-rest-client-standalone
mvn clean package
cp target/jira-rest-client-standalone-1.0.0-SNAPSHOT.jar ../
```

**Compile jira-version-cleaner**
```
cd ..
mvn clean package
java -jar target/jira-rest-client-standalone-1.0.0-SNAPSHOT.jar
java -jar target/jira-version-cleaner-0.1.0-SNAPSHOT.jar server jira-version-cleaner.yaml
```

**Frontend**

`http://localhost:2000/versions`
