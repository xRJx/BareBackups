package simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import backups.BareBackups;

public class FXMLController implements Initializable {
	public String projectAuthor = "Jonathan Droogh (aka RJ)";
	public String projectRelease = "June 1st, 2015";
	public String projectUpdate = "June 1st, 2015";
	public String projectVersion = "1.00";
	public String projectWebsite = "http://barebackups.jonathandroogh.com";
	
	private BareBackups backup;
	
	private ArrayList<Node> autoBackupList = new ArrayList<Node>();
	
	@FXML private CheckBox autoBackup;
	@FXML private CheckBox forceUnchangedBackups;
	
	@FXML private ComboBox<String> backupIntervalTime;
	
	@FXML private Button backupCopiesIncrement;
	@FXML private Button backupCopiesDecrement;
	@FXML private Button backupIntervalDecrement;
	@FXML private Button backupIntervalIncrement;
	@FXML private Button beginBackup;
	@FXML private Button inputDirectoryBrowse;
	@FXML private Button outputDirectoryBrowse;
	@FXML private Button startAutoBackup;

	@FXML private Label backupCopiesLabel;
	@FXML private Label backupIntervalLabel;
	
	@FXML private MenuItem fileOpenProfile;
	@FXML private MenuItem fileSaveProfileAs;
	@FXML private MenuItem helpAbout;
	@FXML private MenuItem helpGettingStarted;
	
	@FXML private Stage primaryStage;
	
	@FXML private ProgressBar progress;
	
	@FXML private TextArea console;
	
	@FXML private TextField backupCopies;
	@FXML private TextField backupInterval;
	@FXML private TextField inputDirectory;
	@FXML private TextField outputDirectory;
	@FXML private TextField saveName;
	
	@FXML private Label author;
	@FXML private Label release;
	@FXML private Label update;
	@FXML private Label version;
	@FXML private Hyperlink website;
	
	@Override
	public void initialize(URL url, ResourceBundle rb){
		
	}
	
	public void addConsoleText(String text){
		Platform.runLater(() -> {
			this.console.appendText(String.format("%s\n", text));
		});
	}
	
	public void addLog(String message){
		this.addConsoleText(message);
	}
	
	public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.setBackup((BareBackups) in.readObject());
		this.getBackup().setController(this);
		
		this.postDeserialize();
	}
	
	private void load(File loadFile){
		
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(loadFile));
			this.deserialize(in);
			
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void postDeserialize(){
		BareBackups backup = this.getBackup();
		
		outputDirectory.setText(backup.getOutputDirectory());
		inputDirectory.setText(backup.getInputDirectory());
		saveName.setText(backup.getSaveName());
	}
	
	public void postInitialize(){
		this.setBackup(new BareBackups(this));
		this.getBackup().setAutoBackupInterval(1);
		this.getBackup().setAutoBackupIntervalTime("minutes");
		
		//autoBackupList.add(forceUnchangedBackups);
		autoBackupList.add(backupInterval);
		autoBackupList.add(backupIntervalLabel);
		autoBackupList.add(backupIntervalTime);
		autoBackupList.add(backupIntervalDecrement);
		autoBackupList.add(backupIntervalIncrement);

		autoBackupList.add(backupCopies);
		autoBackupList.add(backupCopiesLabel);
		autoBackupList.add(backupCopiesDecrement);
		autoBackupList.add(backupCopiesIncrement);
		
		this.setBackupIntervalText("1");
		this.setBackupCopiesText("1");
		
		autoBackup.setOnAction((e) -> {
			if (autoBackup.selectedProperty().getValue() == true){
				for (int i = 0; i < autoBackupList.size(); i++)
					autoBackupList.get(i).setDisable(false);
			} else {
				for (int i = 0; i < autoBackupList.size(); i++)
					autoBackupList.get(i).setDisable(true);
			}
		});
		
		backupIntervalTime.getItems().addAll(
			"seconds",
			"minutes",
			"hours",
			"days"
		);
		
		backupIntervalTime.getSelectionModel().select("minutes");
		
		// Credits to thatsIch (slightly revised), source: http://stackoverflow.com/a/19099928
		backupInterval.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		        if (!newValue.matches("\\d*"))
		        	setBackupIntervalText(oldValue);
		        else if (newValue.equals(""))
		        	setBackupIntervalText("1");
		        else if (Integer.parseInt(newValue) < 1)
		        	setBackupIntervalText(oldValue);
		    }
		});
		
		backupIntervalDecrement.setOnAction((e) -> {
			try {
				this.setBackupIntervalText(Integer.toString(Integer.parseInt(this.getBackupIntervalText()) - 1));
			} catch (NumberFormatException exception) {
				exception.printStackTrace();
			}
		});
		
		backupIntervalIncrement.setOnAction((e) -> {
			try {
				this.setBackupIntervalText(Integer.toString(Integer.parseInt(this.getBackupIntervalText()) + 1));
			} catch (NumberFormatException exception) {
				exception.printStackTrace();
			}
		});
		
		// Credits to thatsIch (slightly revised), source: http://stackoverflow.com/a/19099928
		backupCopies.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		        if (!newValue.matches("\\d*"))
		        	setBackupCopiesText(oldValue);
		        else if (newValue.equals(""))
		        	setBackupCopiesText("1");
		        else if (Integer.parseInt(newValue) < 1)
		        	setBackupCopiesText(oldValue);
		    }
		});
		
		backupCopiesDecrement.setOnAction((e) -> {
			try {
				this.setBackupCopiesText(Integer.toString(Integer.parseInt(this.getBackupCopiesText()) - 1));
			} catch (NumberFormatException exception) {
				exception.printStackTrace();
			}
		});
		
		backupCopiesIncrement.setOnAction((e) -> {
			try {
				this.setBackupCopiesText(Integer.toString(Integer.parseInt(this.getBackupCopiesText()) + 1));
			} catch (NumberFormatException exception) {
				exception.printStackTrace();
			}
		});
		
		beginBackup.setOnAction((e) -> {
			this.saveFields();
			
			this.getBackup().saveBackup();
		});
		
		fileOpenProfile.setOnAction((e) -> {
			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter fileFilter = new FileChooser.ExtensionFilter("SER files (*.ser)", "*.ser");
			fileChooser.getExtensionFilters().add(fileFilter);
			
			File loadFile = fileChooser.showOpenDialog(primaryStage);
			
			if (loadFile != null)
				this.load(loadFile);
		});
		
		fileSaveProfileAs.setOnAction((e) -> {
			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter fileFilter = new FileChooser.ExtensionFilter("SER files (*.ser)", "*.ser");
			fileChooser.getExtensionFilters().add(fileFilter);
			
			File saveFile = fileChooser.showSaveDialog(primaryStage);
			
			if (saveFile != null)
				this.save(saveFile);
		});
		
		helpAbout.setOnAction((e) -> {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("BareBackupsAbout.fxml"));
				Parent root = loader.load();
				FXMLController controller = (FXMLController) loader.getController();
				Stage secondaryStage = new Stage();
				Scene scene = new Scene(root);
				
				secondaryStage.setTitle("About");
				secondaryStage.initOwner(primaryStage);
				secondaryStage.setScene(scene);
				secondaryStage.show();
				controller.setPrimaryStage(secondaryStage);
				
				controller.setAuthor(projectAuthor);
				controller.setRelease(projectRelease);
				controller.setUpdate(projectUpdate);
				controller.setVersion(projectVersion);
				controller.setWebsite(projectWebsite);
				
				URI website = new URI(projectWebsite);
				
				controller.website.setOnAction((e1) ->{
					try {
						java.awt.Desktop.getDesktop().browse(website);
					} catch (Exception e2) {
						this.addLog(e2.toString());
					}
				});
			} catch (Exception e1) {
				this.addLog(e1.toString());
				e1.printStackTrace();
			}
		});
		
		helpGettingStarted.setOnAction((e) -> {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("BareBackupsGettingStarted.fxml"));
				Parent root = loader.load();
				FXMLController controller = (FXMLController) loader.getController();
				Stage secondaryStage = new Stage();
				Scene scene = new Scene(root);
				
				secondaryStage.setTitle("Getting Started");
				secondaryStage.initOwner(primaryStage);
				secondaryStage.setScene(scene);
				secondaryStage.show();
				controller.setPrimaryStage(secondaryStage);
			} catch (Exception e1) {
				this.addLog(e1.toString());
				e1.printStackTrace();
			}
		});
		
		inputDirectoryBrowse.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			File selectedDirectory = directoryChooser.showDialog(primaryStage);
			
			if (selectedDirectory != null)
				this.setInputDirectoryText(selectedDirectory.getAbsolutePath());
		});
		
		outputDirectoryBrowse.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			File selectedDirectory = directoryChooser.showDialog(primaryStage);
			
			if (selectedDirectory != null)
				this.setOutputDirectoryText(selectedDirectory.getAbsolutePath());
		});

		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Runs a schedule on one thread.
		
		Runnable toRun = new Runnable(){
			public void run(){
				if (!backup.getShouldAutoBackup()){
					addLog("[ERROR] Auto backup ran when it shouldn't have.");
					return;
				}
				
				saveFields();
				
				BareBackups backup = getBackup();
				String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
				
				backup.setSaveName(String.format("%s_%s_%s", date, saveName.getText(), backup.getAutoBackupID()));
				backup.saveBackup();
				backup.setAutoBackupID(backup.getAutoBackupID() + 1);
			}
		};
		
		ArrayList<ScheduledFuture<?>> handlerList = new ArrayList<ScheduledFuture<?>>();
		
		startAutoBackup.setOnAction((e) -> {
			boolean shouldAutoBackup = !this.getBackup().getShouldAutoBackup();

			if (shouldAutoBackup){
				if (autoBackup.selectedProperty().getValue() == false){
					return;
				}
				
				this.getBackup().setShouldAutoBackup(true);
				startAutoBackup.setText("Stop Auto Backup");
			} else {
				this.getBackup().setShouldAutoBackup(false);
				startAutoBackup.setText("Start Auto Backup");
			}
			
			this.saveFields();
			shouldAutoBackup = this.getBackup().getShouldAutoBackup();
			
			int scheduleDelay = (int) this.getBackup().getAutoBackupInterval();
			
			ScheduledFuture<?> handler = scheduler.scheduleAtFixedRate(toRun, scheduleDelay, scheduleDelay, TimeUnit.SECONDS);
			handlerList.add(handler);
			
			if (shouldAutoBackup){
				primaryStage.setOnCloseRequest((e1) ->{
					handler.cancel(true);
					scheduler.shutdown();
				});
			} else {
				for (ScheduledFuture<?> currentHandler : handlerList){
					currentHandler.cancel(true);
				}
			}
		});
	}
	
	private void save(File saveFile){
		this.saveFields();
		
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile));
			this.serialize(out);

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void saveFields(){
		this.getBackup().setAutoBackupIntervalTime(backupIntervalTime.getValue());
		this.getBackup().setAutoBackupCopies(Integer.parseInt(backupCopies.getText()));
		this.getBackup().setAutoBackupInterval(Integer.parseInt(backupInterval.getText()));
		this.getBackup().setInputDirectory(inputDirectory.getText());
		this.getBackup().setOutputDirectory(outputDirectory.getText());
		this.getBackup().setSaveName(saveName.getText());
		//this.getBackup().setShouldAutoBackup(autoBackup.isSelected());
		//this.getBackup().setShouldForceUnchangedBackups(forceUnchangedBackups.isSelected());
	}
	
	private void serialize(ObjectOutputStream out) throws IOException {
		out.writeObject(this.getBackup());
	}
	
	// GETTERS
	
	public BareBackups getBackup(){
		return this.backup;
	}
	
	public String getBackupCopiesText(){
		return this.backupCopies.getText();
	}
	
	public String getBackupIntervalText(){
		return this.backupInterval.getText();
	}
	
	public String getConsoleText(){
		return this.console.getText();
	}
	
	public FXMLController getController(){
		return this;
	}
	
	public MenuItem getHelpAbout(){
		return this.helpAbout;
	}
	
	public String getSaveNameText(){
		return this.saveName.getText();
	}
	
	public Hyperlink getWebsite(){
		return this.website;
	}
	
	// SETTERS
	
	public void setAuthor(String author){
		this.author.setText(author);
	}
	
	public void setBackup(BareBackups backup){
		this.backup = backup;
	}
	
	public void setBackupCopiesText(String text){
		this.backupCopies.setText(text);
	}
	
	public void setBackupIntervalText(String text){
		this.backupInterval.setText(text);
	}
	
	public void setInputDirectoryText(String text){
		this.inputDirectory.setText(text);
	}
	
	public void setOutputDirectoryText(String text){
		this.outputDirectory.setText(text);
	}
	
	public void setPrimaryStage(Stage primaryStage){
		this.primaryStage = primaryStage;
	}
	
	public void setRelease(String release){
		this.release.setText(release);
	}
	
	public void setUpdate(String update){
		this.update.setText(update);
	}
	
	public void setVersion(String version){
		this.version.setText(version);
	}
	
	public void setWebsite(String website){
		this.website.setText(website);
	}
	
	public void setProgressAmount(double progress){
		this.progress.setProgress(progress);
	}
}