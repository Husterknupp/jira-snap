Tired of waiting long for the versions page of your project? Send $ 14.90 to mock@tober.com and use the Jira Version Cleaner right now.

`cd jira-rest-client-standalone`

`mvn clean package`

`cp target/jira-rest-client-standalone-1.0.0-SNAPSHOT.jar ../`

---

`cd ..`

`mvn clean package`

`java -jar target/jira-rest-client-standalone-1.0.0-SNAPSHOT.jar`

`java -jar target/jira-version-cleaner-0.1.0-SNAPSHOT.jar server jira-version-cleaner.yaml`

`http://localhost:2000/versions` <- see a website
