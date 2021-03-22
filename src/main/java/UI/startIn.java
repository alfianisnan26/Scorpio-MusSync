package UI;

import java.net.URL;
import java.util.ResourceBundle;

import Backend.Global;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class startIn implements Initializable{
    public Slider slider;
    public TextField tf;
    public Button apply;
    public Button reset;
    public Button bdefault;
    private int lastValue;

    public void mdefault(){
        slider.setValue(5000);
        tf.setText(String.valueOf(5000));
    }
    public void mreset(){
        slider.setValue(lastValue);
        tf.setText(String.valueOf(lastValue));
    }

    public void mapply(){
        Global.v.playOffset = (int) slider.getValue();
        Stage thisStage = ((Stage) apply.getScene().getWindow());
        thisStage.close();
    }
    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        lastValue = Global.v.playOffset;
        slider.setMin(1000);
        slider.setMax(60000);
        slider.setValue(lastValue);
        tf.setText(String.valueOf(Global.v.playOffset));
        slider.valueProperty().addListener(new ChangeListener(){

            @Override
            public void changed(ObservableValue arg0, Object arg1, Object arg2) {
                tf.setText(String.valueOf((int)slider.getValue()));
            }
        });
        
    }
}
