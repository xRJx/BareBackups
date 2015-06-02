package simulator;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class FXLauncher extends Application {
	public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) throws Exception {
    	FXMLLoader loader = new FXMLLoader(getClass().getResource("BareBackups.fxml"));
		Parent root = loader.load();
		
		FXMLController controller = (FXMLController)loader.getController();
		controller.setPrimaryStage(primaryStage);
		controller.postInitialize();
		
		Scene scene = new Scene(root);
		primaryStage.setTitle("Bare Backups");
		primaryStage.setScene(scene);
		primaryStage.getIcons().add(new Image("icon.png"));
		primaryStage.show();
    }
}