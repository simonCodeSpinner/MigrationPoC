# MigrationPoC

To Build:
  mvn clean package

To Run (three command line args): 

  java -jar target/AzureB2CMigration-0.0.1-SNAPSHOT.jar
    [path to user file (in json format)]
    [path to properties file]
    [client secret for application in b2c directory]
