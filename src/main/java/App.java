import java.io.File;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
 
public class App extends Application{
    

    public static void main(String[] args) {
        launch(args);
    }

    

    @Override
    public void start(Stage stage) throws Exception  {
        FXMLLoader root = new FXMLLoader(getClass().getResource("UI/mainWindow.fxml"));
        Scene scene = new Scene(root.load());
        new File("cache/").mkdir();
        stage.setResizable(false);
        stage.setTitle("Scorpio MusSync");
        stage.setScene(scene);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>(){

            @Override
            public void handle(WindowEvent arg0) {
                System.exit(0);
            }
            
        });
        stage.show();
    }
}