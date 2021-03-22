package Backend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
//import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ViHttp {
    private boolean running = false;
    private File file;
    private String dataOut;

    public class HttpThread implements Runnable {
        private StringBuffer response;
        private long contentSize = 0;
        private long contentBufferCounter = 0;
        private boolean error = false;
        private boolean connection = false;
        private boolean stopRequest = false;
        private boolean finish = false;
        private Response resp;

        public synchronized Response getResponse() {
            return resp;
        }

        public synchronized boolean isConnected() {
            return connection;
        }

        public synchronized DataRate getContentBufferCounter() {
            return new DataRate(contentBufferCounter);
        }

        public synchronized DataRate getContentSize() {
            return new DataRate(contentSize);
        }

        public synchronized void stop() {
            stopRequest = true;
        }

        public synchronized boolean isFinished() {
            return finish;
        }

        public synchronized boolean isStop() {
            return stopRequest;
        }

        public synchronized boolean isError() {
            return error;
        }

        public synchronized StringBuffer getStream() {
            return response;
        }

        @Override
        public void run() {
            String state = Thread.currentThread().getName();
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("text/plain");
            if (state.equals("0")) {// get data
                Global.pl("GET HTTP Request");
                Request request = new Request.Builder().url(url).method("GET", null).build();
                connection = true;
                try {
                    resp = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    error = true;
                    finish = true;
                }
                finish = true;
            }else if (state.equals("5")) {// delete data
                Global.pl("GET HTTP Request");
                Request request = new Request.Builder().url(url).method("DELETE", null).build();
                connection = true;
                try {
                    resp = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    error = true;
                    finish = true;
                }
                finish = true;
            } else if (state.equals("1")) {// put data
                Global.pl("PUT HTTP Request");
                RequestBody body = RequestBody.create(dataOut, mediaType);
                Request request = new Request.Builder().url(url).method("PUT", body)
                        .addHeader("Content-Type", "text/plain").build();
                connection = true;
                try {
                    resp = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    finish = true;
                    error = true;
                }
                finish = true;
            } else if (state.equals("2")) {// post data
                Global.pl("POST HTTP Request");
                RequestBody body = RequestBody.create(dataOut, mediaType);
                Request request = new Request.Builder().url(url).method("POST", body)
                        .addHeader("Content-Type", "text/plain").build();
                connection = true;
                try {
                    resp = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    finish = true;
                    error = true;
                }
                finish = true;
            } else if (state.equals("3")) {// upload file
                Global.pl("Upload File HTTP Request");
                RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("reqtype", "fileupload")
                        .addFormDataPart("fileToUpload", file.getAbsolutePath(),
                                RequestBody.create(file, MediaType.parse("application/octet-stream")))
                        .build();
                Request request = new Request.Builder().url(url).method("POST", body).build();
                connection = true;
                try {
                    resp = client.newCall(request).execute();
                } catch (IOException e) {
                    error = true;
                    finish = true;
                    e.printStackTrace();
                }
                finish = true;
            } else if (state.equals("4")) {// download file
                Global.pl("Download File");
                try {
                    URL obj = new URL(url);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setRequestProperty("User-Agent", "Mozilla");
                    int responseCode = httpURLConnection.getResponseCode();
                    System.out.println("GET Response Code : " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) { // success
                        contentSize = httpURLConnection.getContentLengthLong();
                        connection = true;
                        BufferedInputStream bis = new BufferedInputStream(httpURLConnection.getInputStream());
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                        int inByte;
                        while ((inByte = bis.read()) != -1) {
                            bos.write(inByte);
                            contentBufferCounter++;
                            if (isStop()) {
                                error = true;
                                finish = true;
                                bis.close();
                                bos.close();
                                return;
                            }
                        }
                        bis.close();
                        bos.close();

                        System.out.println("GET FINISH");
                        finish = true;
                    } else {
                        System.out.println("GET request not worked");
                        error = true;
                        finish = true;
                    }
                } catch (Exception e) {
                    e.getStackTrace();
                    error = true;
                    finish = true;
                }
            }
        }
    }

    private class TimerThread implements Runnable {
        private long last = 0;
        private long differ = 0;
        private int derrivate = 1;
        private float timeRemaining = 0;

        public synchronized void setDerrivate(int derrivate) {
            this.derrivate = derrivate;
        }

        public synchronized long getDifference() {
            return differ;
        }

        public synchronized float getTimeRemaining() {
            return this.timeRemaining;
        }

        public void run() {
            while (true) {
                try {
                    last = httpThread.getContentBufferCounter().inByte();
                    Thread.sleep(1000 / derrivate);
                    long newSize = httpThread.getContentBufferCounter().inByte();
                    timeRemaining = (float) (httpThread.getContentSize().inByte() - newSize)
                            / (float) timerThread.getDifference();
                    differ = (newSize - last) * (long) derrivate;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public  HttpThread httpThread = new HttpThread();
    private TimerThread timerThread = new TimerThread();
    private Thread tThread = new Thread(timerThread);
    private String url;

    public HttpThread control() {
        return httpThread;
    }

    public static final int GET = 0;
    public static final int PUT = 1;
    public static final int POST = 2;
    public static final int UPLOAD = 3;
    public static final int DOWNLOAD = 4;
    public static final int DELETE = 5;
    private int state = 0;

    public ViHttp(String url, File file, int state) {
        this.file = file;
        this.url = url;
        this.state = state;
    }

    public ViHttp(String url, File file, boolean startState, int state) {
        this.file = file;
        this.url = url;
        this.state = state;
    }

    public ViHttp(String url, int state) {
        this.url = url;
        this.state = state;
    }

    public ViHttp(String url) {
        this.url = url;
        this.state = ViHttp.GET;
    }

    public ViHttp(String url, String data, int state) {
        this.dataOut = data;
        this.url = url;
        this.state = state;
    }

    public ViHttp start() {
        new Thread(httpThread, String.valueOf(this.state)).start();
        tThread.setDaemon(true);
        tThread.start();
        running = true;
        return this;
    }

    public boolean stop() {
        httpThread.stop();
        while (!httpThread.isFinished())
            ;
        running = false;
        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public File getFileContent() {
        return this.file;
    }

    public float getPercentage() {
        return ((float) httpThread.getContentBufferCounter().inByte() / (float) httpThread.getContentSize().inByte())
                * 100;
    }

    public static class DataRate {
        long difference;

        public DataRate(long data) {
            this.difference = data;
        }

        public long inByte() {
            return difference;
        }

        public float inKByte() {
            return (float) inByte() / 1024;
        }

        public float inMByte() {
            return (float) inKByte() / 1024;
        }

        public float inGByte() {
            return (float) inMByte() / 1024;
        }

        public class AutoValue {
            private int state;

            public float getValue() {
                float val = 0;
                if ((val = inGByte()) > 1) {
                    state = 3;
                    return val;

                } else if ((val = inMByte()) > 1) {
                    state = 2;
                    return val;
                } else if ((val = inKByte()) > 1) {
                    state = 1;
                    return val;
                }
                state = 0;
                return inKByte();
            }

            /**
             * @return 0-inByte 1-inKByte 2-inMByte 3-inGByte
             */
            public int getState() {
                return this.state;
            }

            /**
             * @param format 0-KB 1-KBytes 2-KiloBytes 3(default)-Kilo Bytes
             */
            public String AutotoString(int format) {
                float value = getValue();
                String str = "";
                switch (this.state) {
                case 0: {
                    switch (format) {
                    case 0: {
                        str = String.format("%.2f B", value);
                        break;
                    }
                    default: {
                        str = String.format("%.2f Bytes", value);
                        break;
                    }
                    }
                }
                    break;

                case 1: {
                    switch (format) {
                    case 0: {
                        str = String.format("%.2f KB", value);
                        break;
                    }
                    case 1: {
                        str = String.format("%.2f KBytes", value);
                        break;
                    }
                    case 2: {
                        str = String.format("%.2f KiloBytes", value);
                        break;
                    }
                    default: {
                        str = String.format("%.2f Kilo Bytes", value);
                        break;
                    }
                    }
                }
                    break;
                case 2: {
                    switch (format) {
                    case 0: {
                        str = String.format("%.2f MB", value);
                        break;
                    }
                    case 1: {
                        str = String.format("%.2f MBytes", value);
                        break;
                    }
                    case 2: {
                        str = String.format("%.2f MegaBytes", value);
                        break;
                    }
                    default: {
                        str = String.format("%.2f Mega Bytes", value);
                        break;
                    }
                    }
                }
                    break;
                case 3: {
                    switch (format) {
                    case 0: {
                        str = String.format("%.2f GB", value);
                        break;
                    }
                    case 1: {
                        str = String.format("%.2f GBytes", value);
                        break;
                    }
                    case 2: {
                        str = String.format("%.2f GigaBytes", value);
                        break;
                    }
                    default: {
                        str = String.format("%.2f Giga Bytes", value);
                        break;
                    }
                    }
                }
                    break;
                }
                return str;
            }
        }

        public AutoValue autoValue = new AutoValue();
    }

    public DataRate getCurrentSpeed() {
        return new DataRate(timerThread.getDifference());
    }

    public float getTimeRemaining() {
        return timerThread.getTimeRemaining();
    }

    /**
     * 
     * @param derrivate int default = 1 (Timer speed update 1000/derrivate)
     * @return DataRate
     */
    public DataRate getCurrentSpeed(int derrivate) {
        timerThread.setDerrivate(derrivate);
        return new DataRate(timerThread.getDifference());
    }

    public String getStringResponse() throws IOException{
        return control().getResponse().body().string();
    }
}