package UI;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.simple.JSONObject;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import Backend.Global;
import Backend.ServerData;

public class inputNumber implements Initializable {
    public boolean isOk = false;
    public Button cancelButton;
    public Button okButton;
    public TextField tf;
    public String id;
    public Label msg;
    public ProgressIndicator pi;
    public void setOk(){
        isOk = true;
    }
    public void cancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        isOk = false;
        stage.close();
    }

    private void close(boolean state) {
        if (state) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            isOk = true;
            stage.close();
        } else {
            msg.setText("Cannot connect to server");
            okButton.setDisable(false);
            tf.setDisable(false);
            pi.setVisible(false);
        }
    }

    boolean output = false;

    public void ok() {

        isOk = true;
        this.id = tf.getText();
        pi.setVisible(true);
        tf.setDisable(true);
        okButton.setDisable(true);
        msg.setText("Contacting Server...");
        new Thread(new Runnable() {
            public void run() {
                JSONObject out = ServerData.getNow(id);
                if (out != null) {
                    output = (boolean) out.get(ServerData.ISACTIVE);
                    if (output)
                        Global.v.data = out;
                } else
                    output = false;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        close(output);
                    }
                });

            }
        }).start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        tf.textProperty().addListener(new ChangeListener<String>() {
            private boolean state = false;

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*")) {
                    msg.setText("Only Number allowed");
                    tf.setText(newValue.replaceAll("[^\\d]", ""));
                } else if (newValue.length() < 4 && state) {
                    state = false;
                    okButton.setDisable(true);
                    msg.setText("Provide ServerID bellow");
                } else if (newValue.length() == 4 && !state) {
                    msg.setText("Looks good, click OK");
                    state = true;
                    okButton.setDisable(false);
                    okButton.requestFocus();
                } else if (newValue.length() > 4) {
                    msg.setText("Maximum length is reached");
                    tf.setText(oldValue);
                }
            }
        });
    }

}
