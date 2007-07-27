COPYRIGHT
=========

Thaw copyright is held by Freenet Project Incorporated.  Thaw is
distributed under the GPLv2 license. You can find it in a file called
"gpl.txt" in the folder "licenses". This file is included in every
.jar files of Thaw.

----------------

Thaw, Freenet coffe machine
Copyright (C) 2007 Freenet Project Incorporated

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

---------------

COMPILATION
===========

In order to compile Thaw, you need to obtain the latest version of hsqldb.jar.

Here is a link to the current (06/11/2006) version of hsqldb:
http://switch.dl.sourceforge.net/sourceforge/hsqldb/hsqldb_1_8_0_4.zip

Extract the zip, and copy "hsqldb/lib/hsqldb.jar" to "Thaw/lib".

You need also BouncyCastle:
* Download the provider for the JDK 1.4
* Rename the file to BouncyCastle.jar
* Put this .jar in lib/
* You will also need the "unrestricted policy files" for the JCE (see http://bouncycastle.org/fr/documentation.html )

and jmDNS:
* download the jar and put it in lib/
* (expected filename : jmdns.jar)

To compile:
  $ ant

To build the jar file:
  $ ant jar

To build the javadoc:
  $ ant javadoc


RUNNING
=======

With Unix / Linux / etc:
$ cd lib ; java -jar Thaw.jar
or
$ ant run


With Windows:
err ... we will see that later ...

