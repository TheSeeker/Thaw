/*
  FileAccess.java / File Access
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License as
  published by the Free Software Foundation; either version 2 of
  the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package frost.util;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import thaw.core.Logger;

public class FileAccess {

	//private static final Logger logger = Logger.getLogger(FileAccess.class.getName());

	public static File createTempFile(String prefix, String suffix) {
		File tmpFile = null;
		try {
			tmpFile = File.createTempFile(prefix, suffix, new File(System.getProperty("java.io.tmpdir")));
		} catch( Throwable ex ) {
		}
		if( tmpFile == null ) {
			do {
				tmpFile = new File(
						   System.getProperty("java.io.tmpdir")+
						   prefix+
						   System.currentTimeMillis()+
						   suffix);
			} while(tmpFile.isFile());
		}
		return tmpFile;
	}


    /**
     * Reads a file and returns it's content in a byte[]
     * @param file the file to read
     * @return byte[] with the files content
     */
    public static byte[] readByteArray(String filename) {
        return readByteArray(new File(filename));
    }

    public static byte[] readByteArray(File file) {
        try {
            byte[] data = new byte[(int)file.length()];
            FileInputStream fileIn = new FileInputStream(file);
            DataInputStream din = new DataInputStream(fileIn);
            din.readFully(data);
            fileIn.close();
            return data;
        } catch( IOException e ) {
            Logger.error(e, "frost.util.FileAccess: Exception thrown in readByteArray(File file)"+ e.toString());
        }
        return null;
    }

    /**
     * Returns all files starting from given directory/file that have a given extension.
     */
    public static ArrayList getAllEntries(File file, final String extension) {
        ArrayList files = new ArrayList();
        getAllFiles(file, extension, files);
        return files;
    }

    /**
     * Returns all files starting from given directory/file that have a given extension.
     */
    private static void getAllFiles(File file, String extension, ArrayList filesLst) {
        if( file != null ) {
            if( file.isDirectory() ) {
                File[] dirfiles = file.listFiles();
                if( dirfiles != null ) {
                    for( int i = 0; i < dirfiles.length; i++ ) {
                        getAllFiles(dirfiles[i], extension, filesLst); // process recursive
                    }
                }
            }
            if( extension.length() == 0 || file.getName().endsWith(extension) ) {
                filesLst.add(file);
            }
        }
    }
    
    /**
     * Compresses a file into a gzip file.
     * 
     * @param inputFile   file to compress
     * @param outputFile  gzip file
     * @return   true if OK
     */
    public static boolean compressFileGZip(File inputFile, File outputFile) {
        
        final int bufferSize = 4096;

        GZIPOutputStream out = null;
        FileInputStream in = null;

        try {
            in = new FileInputStream(inputFile);
            out = new GZIPOutputStream(new FileOutputStream(outputFile));
        
            byte[] buf = new byte[bufferSize];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch(Throwable t) {
            Logger.error(t, "frost.util.FileAccess : Exception catched : "+ t.toString());
            try { if( in != null) in.close(); } catch(Throwable tt) { }
            try { if( out != null) out.close(); } catch(Throwable tt) { }
            return false;
        }
    }

    /**
     * Decompresses a gzip file.
     * 
     * @param inputFile   gzip file
     * @param outputFile  unzipped file
     * @return   true if OK
     */
    public static boolean decompressFileGZip(File inputFile, File outputFile) {
        
        final int bufferSize = 4096;
        
        GZIPInputStream in = null;
        OutputStream out = null;

        try {
            in = new GZIPInputStream(new FileInputStream(inputFile));
            out = new FileOutputStream(outputFile);
        
            byte[] buf = new byte[bufferSize];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch(Throwable t) {
		Logger.error(t, "frost.util.FileAccess : Exception catched : "+ t.toString());
            try { if( in != null) in.close(); } catch(Throwable tt) { }
            try { if( out != null) out.close(); } catch(Throwable tt) { }
            return false;
        }
    }
    
    /**
     * Writes zip file
     */
    public static boolean writeZipFile(byte[] content, String entry, File file) {
        if (content == null || content.length == 0) {
            Exception e = new Exception();
            e.fillInStackTrace();
            Logger.error(e, "Tried to zip an empty file!  Send this output to a dev"+
			 " and describe what you were doing : "+ e.toString());
            return false;
        }
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
            zos.setLevel(9); // maximum compression
            ZipEntry ze = new ZipEntry(entry);
            ze.setSize(content.length);
            zos.putNextEntry(ze);
            zos.write(content);
            zos.flush(); //do this before closeEntry()
            zos.closeEntry();
            zos.close();
            return true;
        } catch( Throwable e ) {
		Logger.error(e, "frost.util.FileAccess : Exception catched : "+ e.toString());
            return false;
        }
    }

    /**
     * Reads first zip file entry and returns content in a byte[].
     */
    public static byte[] readZipFileBinary(File file) {
        if( !file.isFile() || file.length() == 0 ) {
            return null;
        }

        final int bufferSize = 4096;
        ZipInputStream zis = null;
        ByteArrayOutputStream out = null;
        try {
            zis = new ZipInputStream(new FileInputStream(file));
            out = new ByteArrayOutputStream();
            zis.getNextEntry();

            byte[] zipData = new byte[bufferSize];
            while( true ) {
                int len = zis.read(zipData);
                if( len < 0 ) {
                    break;
                }
                out.write(zipData, 0, len);
            }
            zis.close();
            return out.toByteArray();

        } catch( FileNotFoundException e ) {
		Logger.error(e, "frost.util.FileAccess : Exception catched : "+ e.toString());
        } catch( IOException e ) {
            try { if( zis != null) zis.close(); } catch(Throwable t) { }
            try { if( out != null) out.close(); } catch(Throwable t) { }
	    Logger.error(e, "FileAccess : Exception thrown in readZipFile(String path) \n" +
			 "Offending file saved as badfile.zip, send to a dev for analysis : "+e.toString());
            copyFile(file.getPath(), "badfile.zip");
        }
        return null;
    }

    /**
     * Reads file and returns a List of lines.
     * Encoding "ISO-8859-1" is used.
     */
    public static List readLines(File file) {
        return readLines(file, "ISO-8859-1");
    }
    /**
     * Reads a File and returns a List of lines
     */
    public static List readLines(File file, String encoding) {
        List result = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            result = readLines(fis, encoding);
            fis.close();
        } catch (IOException e) {
            Logger.error(e, "Exception thrown in readLines(File file, String encoding):"+e.toString());
        }
        return result;
    }
    
    /**
     * Reads an InputStream and returns a List of lines.
     */
    public static ArrayList readLines(InputStream is, String encoding) {
        String line;
        ArrayList data = new ArrayList();
        try {
            InputStreamReader iSReader = new InputStreamReader(is, encoding);
            BufferedReader reader = new BufferedReader(iSReader);
            while( (line = reader.readLine()) != null ) {
                data.add(line.trim());
            }
            reader.close();
        } catch (IOException e) {
            Logger.error(e, "Exception thrown in readLines(InputStream is, String encoding):"+e.toString());
        }
        return data;
    }


    /**
     * Reads a file and returns its contents in a String
     */
    public static String readFile(File file) {
	    String data = null;
	    try {
		    FileInputStream stream = new FileInputStream(file);
		    DataInputStream dis = new DataInputStream(stream);

		    data = dis.readUTF();

		    dis.close();
		    stream.close();

	    } catch(java.io.FileNotFoundException e) {
		    Logger.warning(e, "frost.util.FileAcces: Unable to read file : "+e.toString());
		    return null;
	    } catch(java.io.IOException e) {
		    Logger.warning(e, "frost.util.FileAcces: Unable to read file : "+e.toString());
		    return null;
	    }

	    return data;
    }

    /**
     * Reads a file, line by line, and adds a \n after each one. You can specify the encoding to use when reading.
     *
     * @param path
     * @param encoding
     * @return the contents of the file
     */
    public static String readFile(File file, String encoding) {
	    /* Jflesch> No really, I don't care of the encoding. */
	    return readFile(file);
    }

    /**
     * Writes a file "file" to "path"
     */
    public static boolean writeFile(String content, String filename) {
        return writeFile(content, new File(filename));
    }

    /**
     * Writes a file "file" to "path", being able to specify the encoding
     */
    public static boolean writeFile(String content, String filename, String encoding) {
        return writeFile(content, new File(filename), encoding);
    }

    /**
     * Writes a text file in ISO-8859-1 encoding.
     */
    public static boolean writeFile(String content, File file) {
        OutputStreamWriter out = null;
        try {
            // write the file in single byte codepage (default could be a DBCS codepage)
            try {
                out = new OutputStreamWriter(new FileOutputStream(file), "ISO-8859-1");
            } catch(UnsupportedEncodingException e) {
                out = new FileWriter(file);
            }
            out.write(content);
            out.close();
            return true;
        } catch( Throwable e ) {
            Logger.error(e, "Exception thrown in writeFile(String content, File file):"+ e.toString());
            try { if( out != null) out.close(); } catch(Throwable tt) { }
            return false;
        }
    }

    public static boolean writeFile(byte[] content, File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(content);
            out.close();
            return true;
        } catch( Throwable e ) {
            Logger.error(e, "Exception thrown in writeFile(byte[] content, File file)"+e.toString());
            try { if( out != null) out.close(); } catch(Throwable tt) { }
            return false;
        }
    }

    /**
     * Writes a text file in specified encoding. Converts line separators to target platform.
     */
    public static boolean writeFile(String content, File file, String encoding) {
        BufferedReader inputReader = null;
        OutputStreamWriter outputWriter = null;
        try {
            outputWriter = new OutputStreamWriter(new FileOutputStream(file), encoding);

            inputReader = new BufferedReader(new StringReader(content));
            String lineSeparator = System.getProperty("line.separator");
            String line = inputReader.readLine();

            while (line != null) {
                outputWriter.write(line + lineSeparator);
                line = inputReader.readLine();
            }

            outputWriter.close();
            inputReader.close();
            return true;
        } catch (Throwable e) {
            Logger.error(e, "Exception thrown in writeFile(String content, File file, String encoding): "+e.toString());
            try { if( inputReader != null) inputReader.close(); } catch(Throwable tt) { }
            try { if( outputWriter != null) outputWriter.close(); } catch(Throwable tt) { }
            return false;
        }
    }

    /**
     * Deletes the given directory and ALL FILES/DIRS IN IT !!!
     * USE CAREFUL !!!
     */
    public static boolean deleteDir(File dir) {
        if( dir.isDirectory() ) {
            String[] children = dir.list();
            for( int i = 0; i < children.length; i++ ) {
                boolean success = deleteDir(new File(dir, children[i]));
                if( !success ) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * This method copies the contents of one file to another. If the destination file didn't exist, it is created. If
     * it did exist, its contents are overwritten.
     *
     * @param sourceName
     *            name of the source file
     * @param destName
     *            name of the destination file
     */
    public static boolean copyFile(String sourceName, String destName) {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        boolean wasOk = false;
        try {
            sourceChannel = new FileInputStream(sourceName).getChannel();
            destChannel = new FileOutputStream(destName).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            wasOk = true;
        } catch (Throwable exception) {
            Logger.error(exception, "Exception in copyFile: "+ exception.toString());
        } finally {
            try { if( sourceChannel != null) sourceChannel.close(); } catch(Throwable tt) { }
            try { if( destChannel != null) destChannel.close(); } catch(Throwable tt) { }
        }
        return wasOk;
    }

    /**
     * This method compares 2 file byte by byte.
     * Returns true if they are equals, or false.
     */
    public static boolean compareFiles(File f1, File f2) {
        BufferedInputStream s1 = null;
        BufferedInputStream s2 = null;
        try {
            s1 = new BufferedInputStream(new FileInputStream(f1));
            s2 = new BufferedInputStream(new FileInputStream(f2));
            int i1, i2;
            boolean equals = false;
            while(true) {
                i1 = s1.read();
                i2 = s2.read();
                if( i1 != i2 ) {
                    equals = false;
                    break;
                }
                if( i1 < 0 && i2 < 0 ) {
                    equals = true; // both at EOF
                    break;
                }
            }
            s1.close();
            s2.close();
            return equals;
        } catch(Throwable e) {
            try { if( s1 != null) s1.close(); } catch(Throwable tt) { }
            try { if( s2 != null) s2.close(); } catch(Throwable tt) { }
            return false;
        }
    }

    /**
     * Appends a line to the specified text file in UTF-8 encoding.
     */
    public static boolean appendLineToTextfile(File file, String line) {
        BufferedWriter out = null;
        boolean wasOk = false;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
            out.write(line);
            out.write("\n");
            wasOk = true;
        } catch (Throwable e) {
            Logger.error(e, "Exception catched: "+e.toString());
        } finally {
            try { if( out != null) out.close(); } catch(Throwable tt) { }
        }
        return wasOk;
    }
}
