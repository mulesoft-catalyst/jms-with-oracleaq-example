# Oracle AQ JMS integration example with Mule 4

## Overview

Oracle Database Advanced Queuing provides database-integrated message queuing functionality. It is built on top of Oracle Streams and leverages the functions of Oracle Database so that messages can be stored persistently, propagated between queues on different computers and databases, and transmitted using Oracle Net Services and HTTP(S). [Oracle AQ Documentation](https://docs.oracle.com/database/121/ADQUE/aq_intro.htm#ADQUE2418)

![Oracle AQ Queue Architecture](https://docs.oracle.com/database/121/ADQUE/img/adque250.gif)

### Supported JMS Types

Oracle AQ support all available JMS Message Types below:
* TextMessage
* MapMessage
* BytesMessage
* StreamMessage
* ObjectMessage
* Message

[Oracle PL/SQL JMS Type Documentation](https://docs.oracle.com/database/121/ARPLS/t_jms.htm#ARPLS74857)

[Oracle AQ JMS message type example](https://docs.oracle.com/database/121/ADQUE/jm_exmpl.htm#ADQUE1600)


## Pre-requisites

### Grant AQ package to target oracle db user

Create users in the database and grant them AQ JMS permissions. Use a database user with administrator privileges to perform the following task:

Using the Oracle SQL*Plus environment, log in with an administrator login.

`connect / as sysdba;`

Create the JMS user schema. For the following example, the user name is jmsuser and the password is jmsuserpwd.

```SQL
Grant connect, resource TO jmsuser IDENTIFIED BY jmsuserpwd;

Grant the AQ user role to jmsuser.

Grant aq_user_role TO jmsuser;

Grant execute privileges to AQ packages.

Grant execute ON sys.dbms_aqadm TO jmsuser;

Grant execute ON sys.dbms_aq TO jmsuser;

Grant execute ON sys.dbms_aqin TO jmsuser;

Grant execute ON sys.dbms_aqjms TO jmsuser;
```

[Reference](https://docs.oracle.com/cd/E15523_01/web.1111/e13738/aq_jms.htm#JMSAD576)

### Setting up oracle AQ queues

Execute the following SQL commands using sqlplus, Oracle SQL Developer or an SQL client application of your choice:

```SQL
/*Create Text message queue*/
exec DBMS_AQADM.CREATE_QUEUE_TABLE (queue_table => 'mule_text_queue_table', queue_payload_type => 'sys.aq$_jms_text_message');
exec DBMS_AQADM.CREATE_QUEUE (queue_name => 'mule_text_queue', queue_table => 'mule_text_queue_table');
exec DBMS_AQADM.START_QUEUE('mule_text_queue');

/*Create Object message queue*/
exec DBMS_AQADM.CREATE_QUEUE_TABLE (queue_table => 'mule_object_queue_table', queue_payload_type => 'sys.aq$_jms_object_message');
exec DBMS_AQADM.CREATE_QUEUE (queue_name => 'mule_object_queue', queue_table => 'mule_object_queue_table');
exec DBMS_AQADM.START_QUEUE('mule_object_queue');

/*Create MAP message queue*/
exec DBMS_AQADM.CREATE_QUEUE_TABLE (queue_table => 'mule_map_queue_table', queue_payload_type => 'sys.aq$_jms_map_message');
exec DBMS_AQADM.CREATE_QUEUE (queue_name => 'mule_map_queue', queue_table => 'mule_map_queue_table');
exec DBMS_AQADM.START_QUEUE('mule_map_queue')
```

### Configured required libraries

| Library | Type        |Link/Location          |
|---------|:-----------:|-------------:|
|Oracle JDBC Driver| JDBC Driver|[link](https://www.oracle.com/database/technologies/jdbc-ucp-122-downloads.html)|
|Oracle Universal connection pool (UCP)|Connection Pool Management|[link](https://www.oracle.com/database/technologies/jdbc-ucp-122-downloads.html)|
|Oracle AQ Client|JMS Client Lib| {ORACLE_DB_HOME}/rdbms/jlib/aqapi.jar|


Install libraries to local maven repo

```bash
$ mvn install:install-file -Dfile=aqapi.jar -DgroupId=com.oracle.jms -DartifactId=aqapi -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

$ mvn install:install-file -Dfile=ucp.jar -DgroupId=com.oracle.jdbc -DartifactId=ucp -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

$ mvn install:install-file -Dfile=ojdbc8-12.2.0.1.jar -DgroupId=com.oracle.jdbc -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar -DgeneratePom=true
```

 After that those libs can be refered at the pom.xml:
 ```xml
 ...
 <sharedLibraries>
                        <sharedLibrary>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-beans</artifactId>
                        </sharedLibrary>
                    <sharedLibrary>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-core</artifactId>
                        </sharedLibrary>
                    <sharedLibrary>
                            <groupId>org.springframework.security</groupId>
                            <artifactId>spring-security-config</artifactId>
                        </sharedLibrary>
                    <sharedLibrary>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-context</artifactId>
                        </sharedLibrary>
                    <sharedLibrary>
                            <groupId>org.springframework.security</groupId>
                            <artifactId>spring-security-core</artifactId>
                        </sharedLibrary>
                    <sharedLibrary>
				            <groupId>com.oracle.jms</groupId>
				            <artifactId>aqapi</artifactId>
                        </sharedLibrary>
                    <sharedLibrary>
				            <groupId>com.oracle.jdbc</groupId>
				            <artifactId>ojdbc8</artifactId>
                        </sharedLibrary>
                    <sharedLibrary>
            				<groupId>com.oracle.jdbc</groupId>
            				<artifactId>ucp</artifactId>
                        </sharedLibrary>
                    </sharedLibraries>
                </configuration>
 ...
   <dependency>
            <groupId>com.oracle.jms</groupId>
            <artifactId>aqapi</artifactId>
            <version>1.0</version>
        </dependency>
    	<dependency>
            <groupId>com.oracle.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
            <version>12.2.0.1</version>
        </dependency>
    	<dependency>
            <groupId>com.oracle.jdbc</groupId>
            <artifactId>ucp</artifactId>
            <version>1.0</version>
        </dependency>
 ```

## Example Application

### Application Configuration

The example application provides a simple implementation of JMS producer and consumer that targeted Oracle AQ as JMS Broker using Mule 4 JMS and Spring Module. The spring module is required to configure the JMS connection resource to be referred by JMS Plugin as shown below:

```xml
<bean id="oraAQDataSource" class="oracle.ucp.jdbc.PoolDataSourceImpl">
			<property name="URL" value="jdbc:oracle:thin:@//${oracle.aq.host}:${oracle.aq.port}/${oracle.aq.sid}" />
			<property name="user" value="${oracle.aq.username}" />
			<property name="password" value="${oracle.aq.password}" />
			<property name="connectionFactoryClassName" value="oracle.jdbc.pool.OracleDataSource" />
			<property name="minPoolSize" value="1" />
			<property name="maxPoolSize" value="20" />
			<property name="connectionWaitTimeout" value="5" />
			<property name="validateConnectionOnBorrow" value="true" />
		</bean>
<bean id="oraAQConnectionFactory" class="oracle.jms.AQjmsConnectionFactory">
			<property name="datasource" ref="oraAQDataSource" />
		</bean>
```

JMS module configuration:

```xml
<spring:config name="Spring_Config" doc:name="Spring Config" doc:id="36cb9e37-cfa0-4b7f-b648-11e4ada2a26b" files="beans.xml" />
<jms:config name="jms_oracle_aq" doc:name="JMS Config" doc:id="930496da-dea2-43c7-ad16-4e53571c7235" >
		<jms:generic-connection connectionFactory="oraAQConnectionFactory" />
	</jms:config>
```
_PLease ignore xml validation tag error ("One of the following elements: [jndi-connection-factory] is required and must be wrapped in connection-factory") at Studio_

### Sending to MAPmessage type Queue 

JMS message type MapMessage only able to receive JAVA MAP as part of the message body (other message body type like string, bytes, will be rejected). Thus, the payload need to be transformed to Map as follow:

```
%dw 2.0
output application/java
---
{
	"Message":"OracleAQ",
	"JMSType":"Map"
}
```

### Sending to ObjectMessage type Queue 

ObjectMessage JMS message type expecting an instance of JAVA Objects to be sent as part of JMS Message body ((other message body type like string, maps will be rejected)). To do so the application required instantiate any Java object from the pre-defined POJO class as follows:

```
%dw 2.0
import java!com::test::jms::Message
output application/java
---
Message::new("10000824439","70327557510","a2P1s000000UmLTEA0")]
```


# Contribution
Yohanes S

Just fork the repo, make your updates and open a pull request!