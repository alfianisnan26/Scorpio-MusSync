package UI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Scanner;

import org.json.simple.JSONObject;

import Backend.Global;
import Backend.NTPClient;
import Backend.ServerData;
import Backend.ViHttp;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class mainWindow implements Initializable {
    public RadioMenuItem beClient;
    public RadioMenuItem beServer;
    public MenuItem disconnect;
    public MenuItem exit;
    public MenuItem start;
    public MenuItem pause;
    public MenuItem stop;
    public MenuItem sync;
    public Menu idLabel;
    public CheckMenuItem isync;
    public Label message;
    public Label time;
    public Label percentage;
    public ProgressBar progressBar;
    public Menu player;
    public FileChooser fileChooser = new FileChooser();
    private boolean firstSync = false;
    private long prec = 0;
    private Media media;
    private MediaPlayer mediaPlayer;
    private boolean clientThread;
    private int sendLoop = 0;

    private void setTime(long val) {
        if (Global.v.data != null
                && (Integer.parseInt(Global.v.data.get(ServerData.PLAYER).toString()) == ServerData.ISREADY)) {
            val = ((long) Global.v.data.get(ServerData.TIMER) / 1000 - val / 1000);
            Global.pl("Count Down : " + val);
            if (val > 0)
                time.setText("Start in " + val + "...");
            else {
                time.setText("Playing");
                mediaPlayer.play();
                pause.setDisable(false);
                stop.setDisable(false);
                progressBar.setDisable(false);
            }
            if (val <= 0) {
                Global.v.data.replace(ServerData.PLAYER, ServerData.ISPLAY);
                if (whoami == 2) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ServerData.putNow(id, Global.v.data);
                        }
                    }).start();
                    ;
                }
            }
        } else {

            if (Global.v.data != null
                    && (Integer.parseInt(Global.v.data.get(ServerData.PLAYER).toString()) == ServerData.ISPLAY)) {
                if (mediaPlayer != null) {
                    sendLoop++;
                    Duration time = mediaPlayer.getCurrentTime();
                    progressBar.setProgress(time.toMillis() / mediaPlayer.getTotalDuration().toMillis());
                    percentage.setText(String.format("%02d:%02d", (int) time.toMinutes(), (int) time.toSeconds() % 60,
                            (int) time.toMillis() % 1000));
                    if (sendLoop > 5 && whoami == 2) {
                        sendLoop = 0;
                        final long finalVal = val;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Global.v.data.put(ServerData.TIMER, finalVal + 5000);
                                Global.v.data.put(ServerData.SEEKER, time.toMillis() + 5000);
                                ServerData.putNow(id, Global.v.data);
                            }
                        }).start();
                    }
                }
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            LocalTime now = LocalTime.now().plus(java.time.Duration.ofMillis(prec));
            time.setText(dtf.format(now));
        }

    }

    private class clientThread implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (clientThread) {
                    JSONObject raw = ServerData.getNow(id);
                    long player = (long) raw.get(ServerData.PLAYER);
                    if (!raw.get(ServerData.LINK).equals(Global.v.data.get(ServerData.LINK))) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Global.v.data = raw;
                                reDownload = true;
                                clientActivity();
                            }
                        });
                    } else if (player != (long) Global.v.data.get(ServerData.PLAYER)) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Global.v.data = raw;
                                if (player == ServerData.ISSTOP)
                                    toStop();
                                else if (player == ServerData.ISPAUSE)
                                    toPause();
                                else if (player == ServerData.ISREADY)
                                    toStart();
                            }
                        });
                    }
                }
            }
        }
    }

    private class timeThread implements Runnable {
        private long last;

        @Override
        public void run() {
            while (true) {
                long val = System.currentTimeMillis() + prec;
                if ((val) % 1000 == 0 && val != last) {
                    last = val;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            setTime(val);
                        }
                    });
                }
            }
        }
    }

    private class AutoThread implements Runnable {
        private boolean stopState = false;

        public synchronized void stop() {
            stopState = true;
        }

        @Override
        public void run() {
            Global.pl("Automatic Sync Started");
            while (!stopState) {
                for (int a = 0; a < 60; a++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (stopState) {
                        stopState = false;
                        return;
                    }
                    Global.pl("Auto on : " + a);
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        getPrec();
                    }
                });
            }
            Global.pl("Automatic Sync Stopped");
            stopState = false;
        }

    }

    AutoThread autoThread = new AutoThread();

    public void autoSync() {
        Global.pl("AutoSync : " + isync.isSelected());
        if (isync.isSelected())
            new Thread(autoThread).start();
        else
            autoThread.stop();
    }

    public void onExit() {
        System.exit(0);
    }

    public void updatePrecs(long newPrecs) {

        this.prec = newPrecs;
        sync.setDisable(false);
        sync.setText("Synchronize Time");
        if (newPrecs == 0)
            message.setText("Synchronizing Time Out, please re-sync");
        else {
            message.setText("Offset +" + newPrecs + "ms");
            if (!firstSync) {
                firstSync = true;
                progressBar.setDisable(true);
                progressBar.setProgress(0);
                beServer.setDisable(false);
                beClient.setDisable(false);
                idLabel.setDisable(false);
            }
        }

    }

    public void getPrec() {
        message.setText("Synchronizing Time...");
        sync.setText("Synchronizing...");
        sync.setDisable(true);
        new Thread(new syncThread()).start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        start.setText("Play in " + Global.v.playOffset + "ms");
        new Thread(new timeThread()).start();
        new Thread(new clientThread()).start();
        // setTime(System.currentTimeMillis());
        getPrec();
    }

    private String idGenerator() {
        int max = 9999;
        int min = 1000;
        return String.valueOf(new Random().nextInt((max - min) + 1) + min);
    }

    /**
     * 0-No Connection 1-Client 2-Server
     */
    private int whoami = 0;
    private String linkFile = null;

    public void openingFile() {
        try {
            fileChooser.setTitle("Open File");
            fileChooser.setTitle("Select media file to stream");
            fileChooser.getExtensionFilters().setAll(
                    new ExtensionFilter("Supported Audio Files", "*.mp3", "*.m4a", "*.ogg", "*.wav"),
                    new ExtensionFilter("MP3 Files", "*.mp3"), new ExtensionFilter("M4A Files", "*.m4a"),
                    new ExtensionFilter("OGG Files", "*.ogg"), new ExtensionFilter("WAV Files", "*.wav"));
            File thisFile = fileChooser.showOpenDialog(message.getScene().getWindow());
            if (thisFile == null) {
                Global.pl("No File Selected");
                if (disconnect.isDisable())
                    beServer.setSelected(false);
                if (whoami == 1)
                    beClient.setSelected(true);
            } else {
                // filechecking uploaded
                File cache = new File("./cache/fileServer.csv");
                file = thisFile;
                if (cache.exists()) {
                    Scanner sc = new Scanner(cache);
                    while (sc.hasNext()) {
                        String str = sc.nextLine();
                        String fn = file.getName();
                        String[] arStr = str.split(",");
                        Global.pl("Scan: " + arStr[0] + " | Ref: " + fn);
                        Global.pl();
                        if (arStr[0].equals(fn)) {
                            linkFile = arStr[1];
                            Global.pl("File found in cache list");
                            break;
                        }
                    }
                    if (linkFile == null) {
                        Global.pl("File not found in cache list");
                    }
                    sc.close();
                } else {
                    cache.createNewFile();
                }
                percentage.setVisible(false);
                idLabel.setDisable(true);
                message.setText("Uploading files...");
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                progressBar.setDisable(false);
                new Thread(new UploadFiles()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void isUploaded(boolean state, String generated) {
        if (state) {
            Global.pl("Iam is Server");
            message.setText("Server Listen on : " + generated);
            idLabel.setText("SERVER : " + generated);
            disconnect.setDisable(false);
            player.setDisable(false);
            whoami = 2;
            percentage.setVisible(true);
            percentage.setText((String) Global.v.data.get(ServerData.FILENAME));
            loadPlayer();
            clientThread = false;
        } else {
            Global.pl("Iam Not Server");
            message.setText("Cannot Upload File");
            if (disconnect.isDisable())
                beServer.setSelected(false);
            if (whoami == 1)
                beClient.setSelected(true);
        }
        progressBar.setDisable(true);
        progressBar.setProgress(0);
        idLabel.setDisable(false);
    }

    private File file;
    private String id;
    private boolean isReupload = false;

    private class UploadFiles implements Runnable {

        private boolean stout = true;

        @Override
        public void run() {
            Global.pl("Creating Server");
            String outLink = "";
            if (!isReupload) {
                JSONObject server = null;
                boolean tf = false;
                try {
                    do {
                        id = idGenerator();
                        if (!tf) {
                            tf = true;
                            id = "7103";
                        }
                        Global.pl("ID Generated : " + id);
                        server = new ServerData().withID(id).get().getData();
                        if (server == null)
                            break;
                        else if (!(boolean) server.get(ServerData.ISACTIVE)) {
                            server = null;
                            Global.pl("ID Generated because ServerID is Deactive");
                        }
                    } while (server != null);
                    Global.pl("ID Generated Accepted");
                } catch (Exception e) {
                    e.printStackTrace();
                    stout = false;
                }
            }
            isReupload = false;
            ViHttp handler = null;
            if (linkFile == null) {
                Global.pl("Uploading File");
                handler = new ViHttp(Global.f.url, file, ViHttp.UPLOAD).start();
                while (!handler.control().isFinished())
                    ;
                if (handler.control().isError()) {
                    stout = false;
                    System.out.println("Upload Error");
                }
            } else {
                Global.pl("File Cache Available");
            }
            if (stout) {
                try {
                    if (linkFile == null) {
                        outLink = handler.getStringResponse();
                        FileWriter pw = new FileWriter("./cache/fileServer.csv", true);
                        pw.append(file.getName());
                        pw.append(',');
                        pw.append(outLink);
                        pw.append('\n');
                        pw.flush();
                        pw.close();
                        ;
                    } else
                        outLink = linkFile;
                    System.out.println("Upload Successful\nURL: " + outLink);
                } catch (IOException e) {
                    e.printStackTrace();
                    stout = false;
                }
                try {
                    Global.pl("Writing to database");
                    JSONObject data = new JSONObject();
                    data.put(ServerData.TIMER, (System.currentTimeMillis() + prec - 5000));
                    data.put(ServerData.FILENAME, file.getName());
                    data.put(ServerData.LINK, outLink);
                    data.put(ServerData.PLAYER, 0);
                    stout = ServerData.putNow(id, data);
                    if (stout) {
                        Global.v.data = data;
                        Global.pl("Success on ID : " + id + "\nLinks : " + outLink);
                    } else
                        Global.pl("Error on Writing database");
                } catch (Exception e) {
                    System.out.println("Error");
                    e.printStackTrace();
                    stout = false;
                }
            }
            linkFile = null;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    isUploaded(stout, id);
                }
            });
        }

    }

    private boolean reDownload = false;

    public void clientActivity() {
        inputNumber valReturn;
        if (!reDownload) {
            FXMLLoader fxmll = new FXMLLoader(getClass().getResource("inputNumber.fxml"));
            Parent root = null;
            idLabel.setDisable(true);
            try {

                root = fxmll.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.setAlwaysOnTop(true);
            valReturn = fxmll.getController();
            stage.showAndWait();
        } else {
            valReturn = new inputNumber();
            valReturn.setOk();
        }
        if (valReturn.isOk || reDownload) {
            reDownload = false;
            // cari file
            percentage.setVisible(false);
            File cache = new File("cache/" + Global.v.data.get(ServerData.FILENAME));
            Global.pl("File : " + cache.getAbsolutePath() + " | isFound : " + cache.isFile());
            if (!cache.isFile()) {
                progressBar.setDisable(false);
                percentage.setText("Connecting...");
                percentage.setVisible(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ViHttp handlerHttp = new ViHttp((String) Global.v.data.get(ServerData.LINK), cache,
                                ViHttp.DOWNLOAD).start();
                        float lastPercent = 0;
                        while (!handlerHttp.control().isFinished()) {
                            if (handlerHttp.control().isConnected()) {
                                if (lastPercent != handlerHttp.getPercentage()) {
                                    lastPercent = handlerHttp.getPercentage();
                                    Global.pl(String.format("%.2f", handlerHttp.getPercentage()) + "%");
                                    Platform.runLater(new Runnable() {

                                        @Override
                                        public void run() {
                                            setProgressValue(handlerHttp.getCurrentSpeed(2).autoValue.AutotoString(0),
                                                    (float) handlerHttp.getPercentage());
                                        }

                                    });
                                }
                            }
                        }
                        if (handlerHttp.control().isError())
                            Platform.runLater(new Runnable() {

                                @Override
                                public void run() {
                                    setClientOff();
                                }

                            });
                        else
                            file = cache;
                        Platform.runLater(new Runnable() {

                            @Override
                            public void run() {
                                setClientOn(valReturn.id);
                            }

                        });

                    }
                }).start();
            } else
                file = cache;
            setClientOn(valReturn.id);
        } else {
            setClientOff();
        }

    }

    public void setProgressValue(String speed, float value) {
        message.setText("Speed : " + speed + "/second");
        progressBar.setProgress(value / 100);
        percentage.setText(String.format("%.2f", value) + "%");
    }

    public void setClientOn(String id) {
        this.id = id;
        disconnect.setDisable(false);
        progressBar.setDisable(true);
        progressBar.setProgress(0);
        Global.pl("Be Client");
        message.setText("Connected to Server : " + id);
        idLabel.setText("CLIENT : " + id);
        idLabel.setDisable(false);
        percentage.setVisible(true);
        percentage.setText((String) Global.v.data.get(ServerData.FILENAME));
        loadPlayer();
        whoami = 1;
        clientThread = true;
    }

    public void setClientOff() {
        Global.pl("Failed");
        if (whoami == 2) {
            beServer.setSelected(true);
        } else if (whoami == 1) {
            beClient.setSelected(true);
        } else
            beClient.setSelected(false);
        idLabel.setDisable(false);
        progressBar.setDisable(true);
        progressBar.setProgress(0);
        percentage.setVisible(false);
        clientThread = false;
    }

    public void disconnecting() {
        if (whoami == 1) {
            beClient.setSelected(false);
        } else if (whoami == 2) {
            beServer.setSelected(false);
        }
        disconnect.setDisable(true);
        message.setText("Disconnected");
        idLabel.setText("No Connection");
        player.setDisable(true);
        percentage.setVisible(true);
        whoami = 0;
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        clientThread = false;
    }

    public MenuItem snf;

    public void selectNewFile() {
        isReupload = true;
        openingFile();
    }

    private class syncThread implements Runnable {
        @Override
        public void run() {
            System.out.println("Getting offset");
            Long precs = NTPClient.getOffset();
            System.out.println("Offset : " + precs + "ms");
            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    updatePrecs(precs);
                }

            });
        }
    }

    public void setTimeOffset() {
        FXMLLoader fxmll = new FXMLLoader(getClass().getResource("startIn.fxml"));
        Parent root = null;
        idLabel.setDisable(true);
        try {

            root = fxmll.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setTitle("Set play offset");
        stage.setScene(new Scene(root));
        stage.setAlwaysOnTop(true);
        stage.showAndWait();
        start.setText("Play in " + Global.v.playOffset + "ms");
    }

    public void loadPlayer() {
        try {
            media = new Media(file.toURI().toURL().toExternalForm());
            if (mediaPlayer != null)
                mediaPlayer.dispose();
            mediaPlayer = new MediaPlayer(media);
            progressBar.setProgress(0);
            mediaPlayer.setOnEndOfMedia(new Runnable() {
                @Override
                public void run() {
                    Platform.runLater(new Runnable() {
                        public void run() {
                            toStop();
                        }
                    });
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isPaused = false;

    public void toStart() {
        boolean state = false;
        if (mediaPlayer == null)
            loadPlayer();
        if (isPaused) {
            isPaused = false;
            Double seeker = (double) Global.v.data.get(ServerData.SEEKER);
            Global.pl(seeker);
            mediaPlayer.seek(Duration.millis(seeker));
        }
        if (whoami == 2) {
            Global.v.data.replace(ServerData.TIMER, System.currentTimeMillis() + prec + Global.v.playOffset);
            Global.v.data.replace(ServerData.PLAYER, ServerData.ISREADY);
            state = ServerData.putNow(id, Global.v.data);
            if (state) {
                start.setDisable(true);
            }
        }
    }

    public void toStop() {
        isPaused = false;
        boolean state = true;
        if (whoami == 2) {
            Global.v.data.replace(ServerData.PLAYER, ServerData.ISSTOP);
            state = ServerData.putNow(id, Global.v.data);
        }
        if (state) {
            mediaPlayer.stop();
            start.setDisable(false);
            stop.setDisable(true);
            pause.setDisable(true);
            progressBar.setProgress(0);
            progressBar.setDisable(true);
            percentage.setText((String) Global.v.data.get(ServerData.FILENAME));
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    public void toPause() {
        /*
        isPaused = true;
        boolean state = true;
        if (whoami == 2) {
            Global.v.data.replace(ServerData.PLAYER, ServerData.ISPAUSE);
            Global.v.data.put(ServerData.SEEKER, mediaPlayer.getCurrentTime().toMillis());
            state = ServerData.putNow(id, Global.v.data);
        }
        if (state) {
            mediaPlayer.pause();
            start.setDisable(false);
            stop.setDisable(false);
            pause.setDisable(true);
            progressBar.setDisable(true);
        }
        */
    }

}