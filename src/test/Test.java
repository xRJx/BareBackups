package test;

import java.io.File;
import java.util.ArrayList;

public class Test {

	public static void main(String[] args) {
		//BareBackups backup = new BareBackups();
		
		ArrayList<String> backups = new ArrayList<String>();
		File directory = new File("C:\\Users\\jonny\\Desktop");
		File[] filesList = directory.listFiles();
		
		boolean match = false;
		
		for (File file : filesList){
			
			if (file.getName().matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]-[0-9][0-9]_.*_[0-9]\\.zip")){
				backups.add(file.toString());
				System.out.println(file.getName());
				
				match = true;
			}
		}

		
		if (!match)
			System.out.println("No go");
	}
}