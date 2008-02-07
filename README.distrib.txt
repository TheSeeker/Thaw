Notes for the people wanting to bundle Thaw in a distribution
=============================================================

You can specify to ant where it must look for Thaw dependencies:

% ant \
   -Djmdns.location=[pathToJmdns]/jmdns.jar \
   -Dhsqldb.location=[pathToHsqldb]/hsqldb.jar \
   -Dbouncycastle.location=[pathToBouncyCastle]/BouncyCastle.jar


And you can compile a version of Thaw not including all its dependencies
by specifying the target 'jar-nodeps'.

So in the end, you can use the following line to compile Thaw:


% ant \
   -Djmdns.location=[pathToJmdns]/jmdns.jar \
   -Dhsqldb.location=[pathToHsqldb]/hsqldb.jar \
   -Dbouncycastle.location=[pathToBouncyCastle]/BouncyCastle.jar \
   jar-nodeps

If you use the target "jar-nodeps", the final .jar will be bin/Thaw-light.jar

To start Thaw-light, you will have to specify where each dependency is located:

% java \
  -cp [pathToJmdns]/jmdns.jar:[pathToHsqldb]/hsqldb.jar:[pathToBouncyCastle]/BouncyCastle.jar \
  -jar Thaw.jar

