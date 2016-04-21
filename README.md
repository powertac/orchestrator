# Power TAC Experiment Scheduler

## Introduction

The Power TAC Experiment Scheduler is a dynamic jsf web application for scheduling and managing experiments between brokers. For more information, see http://www.powertac.org.

## Getting Started 

* Download a copy of apache tomcat 7 (http://tomcat.apache.org/download-70.cgi).

* Edit the tomcat-users.xml file and add the following markup in the tomcat-users tag:

`<role rolename="manager-gui"/>`

`<role rolename="admin-gui"/>`

`<role rolename="manager-script"/>`

`<role rolename="admin-script"/>`

`<user username="admin" password="admin" roles="admin-gui,manager-gui,admin-script,manager-script"/>`

* Run the ./startup.sh script for apache tomcat

* Copy the example config files, and edit the properties if needed.
  Usually only db user/pass and the file locations are needed
  $cp src/main/resources/experiment.properties.template src/main/resources/experiment.properties
  $cp src/main/resources/hibernate.cfg.xml.template     src/main/resources/hibernate.cfg.xml
  $cp src/main/resources/log4j.cfg.xml.template         src/main/resources/log4j.cfg.xml

* Navigate to the experiment scheduler project and run `mvn compile tomcat7:deploy`

