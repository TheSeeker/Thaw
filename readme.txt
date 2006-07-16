COPYRIGHT
=========

Thaw copyright is held by the Freenet project ( http://www.freenetproject.org/ ).
Thaw is distributed under the GPLv2 license. You
can find it in a file called "gpl.txt" and joined with every copy of
Thaw.

----------------
    Copyright (C) 2006  Freenet project

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
---------------

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

