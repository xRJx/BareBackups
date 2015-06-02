package backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import simulator.FXMLController;

/**
 * This class provides backup functionality (compressing, auto backup, etc.). It also manages underlying functionality of the GUI element.
 * @author Jonathan Droogh (aka RJ)
 * @version 1.0
 * @see simulator.FXMLController
 */
public class BareBackups implements Serializable {
	private ArrayList<String> inputFiles = new ArrayList<String>();
	
	//private boolean shouldForceUnchangedBackups; // TODO Implement auto backup skipping if files haven't changed.
	private boolean shouldAutoBackup;
	
	private double progressComplete = 0;
	
	private FXMLController controller;
	
	private int autoBackupCopies;
	private int autoBackupID;
	private int autoBackupInterval;
	
	private static final long serialVersionUID = 1L; // TODO Increment for each new backup feature that can be saved/loaded.
	
	private String autoBackupIntervalTime;
	private String inputDirectory;
	private String outputDirectory;
	private String saveName;
	
	public BareBackups(){
		this.setAutoBackupID(1);
		this.setAutoBackupIntervalTime("");
		this.setInputDirectory("");
		this.setOutputDirectory("");
		this.setSaveName("");
	}
	
	/**
	 * Initial constructor. Sets up values so they aren't null and assigns the controller.
	 * @param controller The object used for interacting with the GUI elements.
	 */
	public BareBackups(FXMLController controller){
		this();
		this.setController(controller);
	}
	
	/**
	 * Adds all files/folders under 'directory' to the 'inputFiles' ArrayList.
	 * @param directory The directory to find files to be added.
	 * @return Whether the operation of adding files failed or not.
	 */
	public Boolean addInputFiles(File directory){
		if (!directory.exists()) // Terminates if missing critical info or the directory to add files from doesn't exist.
			return false;
		
		try {
			File[] files = directory.listFiles(); // Gets files from directory.
			
			for (File file : files){ // Loops through each file in the top-level of the directory.
				if (file.isDirectory() && file.list().length > 0) // Check if directory isn't empty
					this.addInputFiles(file); // Recursively adds files inside directory if it has more files.
				else
					this.addInputFile(file.getCanonicalPath());
				
			}
		} catch (IOException exception) {
			this.addLog(String.format("[ERROR] %s", exception));
		}
		
		return true;
	}
	
	/**
	 * Adds a path+file to the 'backupFile' ArrayList.
	 * @param backupFile The path+file added to the ArrayList (the file that will be backed up).
	 */
	public void addInputFile(String backupFile){
		this.getInputFiles().add(backupFile);
	}
	
	/**
	 * Adds a message to the GUI's console and system's console (if running in command line).
	 * @param message What will be printed to the console.
	 */
	public void addLog(String message){
		if (this.getController() != null)
			this.getController().addLog(message);
	}
	
	/**
	 * Various checks to make sure essential values aren't empty.
	 * @param shouldLog Whether a log should be made if a check doesn't pass.
	 * @return True if there's enough information to attempt a backup, false otherwise.
	 */
	public boolean isReadyToZip(boolean shouldLog){
		boolean isReady = true;
		
		if (this.getOutputDirectory().equals("")){
			if (shouldLog) this.addLog("[WARNING] Missing output directory to save backup to.");
			isReady = false;
		}
		
		if (this.getInputDirectory().equals("")){
			if (shouldLog) this.addLog("[WARNING] Missing input directory to get files from.");
			isReady = false;
		}
		
		if (this.getSaveName().equals("")){
			if (shouldLog) this.addLog("[WARNING] Missing save name to save backup as.");
			isReady = false;
		}
		
		return isReady;
	}
	
	/**
	 * Reloads serialized elements to their according fields.
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.setInputDirectory((String) in.readObject());
		this.setOutputDirectory((String) in.readObject());
		this.setSaveName((String) in.readObject());
	}
	
	/**
	 * Clears the ArrayList containing which files to backup so unnecessary files aren't backed up.
	 */
	public void resetInputFiles(){
		this.getInputFiles().clear();
	}
	
	/**
	 * Adds selected input folder (and its files) to a backup file saved to the selected output directory.
	 */
	public void saveBackup(){
		boolean hasFailed = false;
		this.resetInputFiles();
		
		if (!this.isReadyToZip(true)){
			hasFailed = true;
		} else if (this.addInputFiles(new File(this.getInputDirectory())) == false){
			hasFailed = true;
			this.addLog("[ERROR] Invalid input directory.");
		} else if (this.getInputFiles().isEmpty()){
			hasFailed = true;
			this.addLog("[ERROR] No files to backup.");
		}
		
		if (hasFailed){
			this.addLog("[INFO] Unable to create backup file.");
			return;
		}
		
		// Checks if amount of auto backups exceeds defined limits, removes oldest backup if true.
		if (this.getShouldAutoBackup()){
			String tempSaveName = this.getController().getSaveNameText(); // TODO Rework to avoid consistency errors.
			File directory = new File(this.getOutputDirectory());
			File[] fileList = directory.listFiles();
			ArrayList<File> backupList = new ArrayList<File>();
			
			for (File file : fileList){
				String matchRegex = String.format("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]-[0-9][0-9]_%s_.*\\.zip", tempSaveName);
				
				if (file.getName().matches(matchRegex)){
					backupList.add(file);
				}
			}
			
			if (backupList.size() >= this.getAutoBackupCopies()){
				File toRemove = backupList.get(0);
				
				this.addLog(String.format("[INFO] Backup copies exceeded, removed backup: %s", toRemove.getAbsolutePath()));
				toRemove.delete();
			}
		}
		
		try {
			double totalProgress = this.getInputFiles().size();
			byte[] buffer = new byte[1024];
			String saveLocation = String.format("%s\\%s.zip", this.getOutputDirectory(), this.getSaveName());
			
			FileOutputStream fileOutput = new FileOutputStream(saveLocation); // Writes files to a zip.
			ZipOutputStream zipOutput = new ZipOutputStream(fileOutput); // Zipping stuff.
			
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Runs a schedule on one thread.
			
			Runnable toRun = new Runnable(){
				public void run(){
					getController().setProgressAmount(progressComplete/totalProgress);
				}
			};
			
			// Loops through each file in input directory and adds it to backup.
			for (String backupFile : this.getInputFiles()){
				File sourceFile = new File(backupFile);
				String rootDirectory = sourceFile.getParent().replace(this.getInputDirectory(), "");
				String fileName = sourceFile.getName();
				
				if (!rootDirectory.equals("")){
					fileName = String.format("%s\\%s", rootDirectory, sourceFile.getName()).substring(1); // Maintains file structure for files not in top-level.
				}
				
				if (!sourceFile.isDirectory()){
					FileInputStream fileInput = new FileInputStream(sourceFile); // Reads file to be backed up.
					
					zipOutput.putNextEntry(new ZipEntry(fileName)); // Prepares zip for adding file.
					
					int fileSize;
					
					while ((fileSize = fileInput.read(buffer)) > 0){ // Writes the file data in to the zip (if data exists).
						zipOutput.write(buffer, 0, fileSize);
					}
	
					fileInput.close();
				} else {
					fileName += "\\";
					zipOutput.putNextEntry(new ZipEntry(fileName));
				}
				
				zipOutput.closeEntry();
				this.progressComplete++;
				scheduler.schedule(toRun, 0, TimeUnit.SECONDS);
				
				if (!sourceFile.isDirectory())
					this.addLog(String.format("[INFO] Added file to backup: %s", fileName));
				else
					this.addLog(String.format("[INFO] Added folder to backup: %s", fileName));
			}
			
			zipOutput.close();
			fileOutput.close();
			this.progressComplete = 0;
			this.getController().setProgressAmount(0);
			this.addLog(String.format("[INFO] Backup successfully created and saved: %s", saveLocation));
		} catch (IOException exception){
			this.addLog(String.format("[ERROR] Unable to create backup file: %s", exception));
		}
	}
	
	/**
	 * Serializes select elements so irrelevant ones aren't serialized.
	 * @param out
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(this.getInputDirectory());
		out.writeObject(this.getOutputDirectory());
		out.writeObject(this.getSaveName());
	}
	
	// GETTERS
	
	public int getAutoBackupCopies(){
		return this.autoBackupCopies;
	}
	
	public int getAutoBackupID(){
		return this.autoBackupID;
	}
	
	/**
	 * Gets the save interval in seconds.
	 * @return Integer containing how often an auto save should be run in seconds.
	 */
	public double getAutoBackupInterval(){
		String intervalTime = this.getAutoBackupIntervalTime();
		double modifier = 1; // Base, assumes time is in seconds (unchanged).
		
		if (intervalTime.equals("minutes"))
			modifier = 60;
		else if (intervalTime.equals("hours"))
			modifier = 3600;
		else if (intervalTime.equals("days"))
			modifier = 86400;
		
		return this.autoBackupInterval*modifier;
	}
	
	public String getAutoBackupIntervalTime(){
		return this.autoBackupIntervalTime;
	}
	
	/**
	 * Gets the controller to interact with the GUI.
	 * @return Controller object stored in BareBackups.
	 */
	public FXMLController getController(){
		return this.controller;
	}
	
	/**
	 * Gets the input directory to be backed up.
	 * @return The directory that's to be backed up.
	 */
	public String getInputDirectory(){
		return this.inputDirectory;
	}
	
	/**
	 * Gets a list of files in the input directory.
	 * @return List of files to be backed up.
	 */
	public ArrayList<String> getInputFiles(){
		return this.inputFiles;
	}
	
	/**
	 * Gets the output directory on where to save the backup file.
	 * @return Directory to where the backup file is saved.
	 */
	public String getOutputDirectory(){
		return this.outputDirectory;
	}
	
	/**
	 * Gets the save name of the backup.
	 * @return The name the backup file will be saved as.
	 */
	public String getSaveName(){
		return this.saveName;
	}
	
	public boolean getShouldAutoBackup(){
		return this.shouldAutoBackup;
	}
	
	// SETTERS
	
	public void setAutoBackupCopies(int backupCopies){
		this.autoBackupCopies = backupCopies;
	}
	
	public void setAutoBackupID(int ID){
		this.autoBackupID = ID;
	}
	
	/**
	 * Sets the auto save interval.
	 * @param interval What to set the save interval to.
	 */
	public void setAutoBackupInterval(int interval){
		this.autoBackupInterval = interval;
	}
	
	public void setAutoBackupIntervalTime(String autoBackupIntervalTime){
		this.autoBackupIntervalTime = autoBackupIntervalTime;
	}
	
	/**
	 * Sets the controller.
	 * @param controller What the controller is set to.
	 */
	public void setController(FXMLController controller){
		this.controller = controller;
	}
	
	 // TODO Implement auto backup skipping if files haven't changed.
	/*public void setShouldForceUnchangedBackups(boolean shouldForceUnchangedBackups){
		this.shouldForceUnchangedBackups = shouldForceUnchangedBackups;
	}*/
	
	/**
	 * Sets the input directory.
	 * @param directory What the input directory is set to.
	 */
	public void setInputDirectory(String directory){
		this.inputDirectory = directory;
	}
	
	/**
	 * Sets the input files.
	 * @param inputFiles What to set the input files list to.
	 */
	public void setInputFiles(ArrayList<String> inputFiles){
		this.inputFiles = inputFiles;
	}
	
	/**
	 * Sets the output directory.
	 * @param directory What to set the output directory to.
	 */
	public void setOutputDirectory(String directory){
		this.outputDirectory = directory;
	}
	
	/**
	 * Sets the save name
	 * @param name What to set the save name to.
	 */
	public void setSaveName(String name){
		this.saveName = name;
	}
	
	public void setShouldAutoBackup(boolean shouldAuotBackup){
		this.shouldAutoBackup = shouldAuotBackup;
	}
}