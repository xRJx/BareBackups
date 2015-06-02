package test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import backups.BareBackups;

public class BareBackupsTest {
	@Test public void testAddInputFile(){
		BareBackups backup = new BareBackups();
		
		backup.addInputFile("test");
		
		assertTrue(backup.getInputFiles().get(0).equals("test")); // Checks if "test" was added to the inputFiles ArrayList.
	}
	
	@Test
	public void testAddInputFiles() throws IOException{
		BareBackups backup = new BareBackups();
		File directory = new File("barebackupstestdirectory001");
		File tempFile = new File(directory, "temp.txt");

		if (tempFile.exists())
			tempFile.delete();
		if (directory.exists())
			directory.delete();
		
		directory.mkdirs();
		tempFile.createNewFile();
		backup.addInputFiles(directory);
		
		assertTrue(backup.getInputFiles().get(0).equals(tempFile.getCanonicalPath())); // Checks if file was properly added.
		
		tempFile.delete();
		directory.delete();
	}
	
	@Test
	public void testResetInputFiles(){
		BareBackups backup = new BareBackups();
		
		//backup.setInputFiles(new ArrayList<String>());
		backup.getInputFiles().add("test");
		backup.resetInputFiles();
		
		assertTrue(backup.getInputFiles().isEmpty()); // Checks if the ArrayList was emptied.
	}
}