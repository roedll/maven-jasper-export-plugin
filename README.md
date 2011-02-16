Introduction
============

Maven plugin to export jasper files to PDF, HTML or XML using MySQL as data source.

Usage and configuration
=======================

Include the plugin into your pom file

 <plugin>
     <groupId>com.abiquo.jasper</groupId>
     <artifactId>jasper-export-maven-plugin</artifactId>
     <configuration>
         <!-- see below -->
     </configuration>
     <executions>
         <execution>
             <goals>
                 <goal>jasper-export</goal>
             </goals>
         </execution>
     </executions>
    
     <dependencies>
         <dependency>
             <groupId>jasperreports</groupId>
             <artifactId>jasperreports</artifactId>
             <version>${jasperreports.version}</version>
         </dependency>
     </dependencies>
 </plugin>

There are required properties:

 * jasperDirectory: Directory where jasper files are located
 * jrxmlDirectory: Directory where the source of jasper files are located
 * outputDirectory: Where the reports will be generated
 * mysqlHost: The MySQL host
 * mysqlDatabase: The MySQL database
 * mysqlUser: The MySQL user
 * mysqlPassword: The MySQL password
        
Non required properties:

 * exportFormat: valid values are PDF, HTML and XML. The default value is PDF.