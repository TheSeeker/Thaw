
LICENSE
=======

Thaw is distributed under the GPLv2 license. You can find it in a file
called "gpl.txt" and joined with every copy of Thaw.


COMPILATION
===========

<!-- forget what follow, Hsqldb is not required for the moment -->
<!-- it will be when I will start to deal with indexes -->
<!--
In order to compile Thaw, you need to obtain the latest version of hsqldb.jar.

Here is a link to the current (06/11/2006) version of hsqldb:
http://switch.dl.sourceforge.net/sourceforge/hsqldb/hsqldb_1_8_0_4.zip

Extract the zip, and copy "hsqldb/lib/hsqldb.jar" to "Thaw/lib".
-->


To compile:
  $ ant

To build the jar file:
  $ ant jar

To build the javadoc:
  $ ant javadoc


RUNNING
=======

On Unix / Linux / etc:
$ cd lib ; java -jar Thaw.jar
or
$ ant run


On Windows:
err ... we will see that later ...

