package example.sender;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SenderForm {

    public HttpRequestEditor requestViewer;
    public HttpResponseEditor responseViewer;
    public JPanel panel1;
    private final MontoyaApi api;
    private JCheckBox GETCheckBox;
    private JCheckBox POSTCheckBox;
    private JCheckBox otherCheckBox;
    private JTextField textFieldMethod;
    private JButton sendButton;
    private JTextArea textAreaHeaders;
    private JTextArea textAreaBody;
    private JButton loadUrlsButton;
    private JTextArea textAreaUrls;
    private JTextField textPath;
    private JCheckBox pathCheckBox;
    private JTextField textFieldThread;
    private JLabel labelDone;
    private JTextField textFieldTimeout;

    public SenderForm(HttpRequestEditor requestViewer, HttpResponseEditor responseViewer, MontoyaApi api) {
        this.requestViewer = requestViewer;
        this.responseViewer = responseViewer;
        this.api = api;
        sendButton.addActionListener(e -> {
            Logging logging = api.logging();
            labelDone.setText("0");

            if (textAreaUrls.getText().isBlank()){
                logging.logToError("Hosts is empty");
            }else if (textAreaUrls.getText().strip().contains("\n")){
                logging.logToOutput("Batch req");
                batchReq();
            }else {
                logging.logToOutput("Single req");
                singleReq();
            }
        });
        GETCheckBox.addActionListener(e -> {
            POSTCheckBox.setSelected(false);
            otherCheckBox.setSelected(false);
        });
        POSTCheckBox.addActionListener(e -> {
            GETCheckBox.setSelected(false);
            otherCheckBox.setSelected(false);
        });
        otherCheckBox.addActionListener(e -> {
            GETCheckBox.setSelected(false);
            POSTCheckBox.setSelected(false);
        });
        loadUrlsButton.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            // JFileChooser.FILES_ONLY
            // JFileChooser.DIRECTORIES_ONLY
            int returnVal = fc.showOpenDialog(panel1);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                try {
                    InputStreamReader read = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(read);
                    StringBuilder urls = new StringBuilder();
                    String lineText;
                    while ((lineText = bufferedReader.readLine()) != null) {
                        urls.append(lineText + "\n");
                    }
                    read.close();
                    textAreaUrls.setText(urls.toString().strip());
                } catch (Exception exception) {
                }
            }

        });
    }

    private void singleReq() {
        Logging logging = api.logging();
        logging.logToOutput("-----prepare req-----");
        String method = getReqMethod();
        if (method.isBlank()) {
            logging.logToError("method is empty");
            return;
        }
        logging.logToOutput("method = " + method);

        String url = textAreaUrls.getText().strip();
        int index = url.indexOf("/", 8);
        String serviceAddress = index == -1 ? url : url.substring(0, index);
        HttpService service = HttpService.httpService(serviceAddress);
        logging.logToOutput("service = " + serviceAddress);

        String Host = serviceAddress.replace("https://", "").replace("http://", "");
        logging.logToOutput("Host = " + Host);

        String path;
        if (pathCheckBox.isSelected() && !Objects.equals(pathCheckBox.getText().strip(), "")) {
            path = textPath.getText().strip();
        } else {
            path = index == -1 ? "/" : url.substring(index);
        }
        logging.logToOutput("path = " + path);

        HttpRequest req = HttpRequest.httpRequest()
                .withMethod(method)
                .withPath(path)
                .withService(service)
                .withAddedHeader("Host", Host)
                .withBody(textAreaBody.getText());

        String headerString = textAreaHeaders.getText();
        if (!headerString.isEmpty()) {
            for (String header : headerString.strip().split("\n")) {
                header = header.strip();
                index = header.indexOf(":");
                if (index == -1) {
                    logging.logToError("invalid header: " + header);
                    continue;
                }
                String k = header.substring(0, index).strip();
                String v = header.substring(index + 1).strip();

                if (!k.equals("Host")) {
                    logging.logToOutput("add header " + k + ": " + v);
                    req = req.withAddedHeader(k, v);
                } else {
                    logging.logToOutput("update header " + k + ": " + v);
                    req = req.withUpdatedHeader(k, v);
                }

            }
        }

        logging.logToOutput("-----req prepared-----");
        HttpRequest finalReq = req;
        new Thread(() -> {
            HttpRequestResponse res = api.http().sendRequest(finalReq);
            requestViewer.setRequest(finalReq);
            responseViewer.setResponse(res.response());
            labelDone.setText("1");
        }).start();
    }

    private void batchReq() {
        new Thread(() -> {
            Logging logging = api.logging();
            requestViewer.setRequest(null);
            responseViewer.setResponse(null);
            sendButton.setEnabled(false);
            logging.logToOutput("Start! send button disable");

            String method = getReqMethod();
            if (method.isBlank()) {
                logging.logToError("method is empty");
                sendButton.setEnabled(true);
                logging.logToOutput("Done! send button enable");
                return;
            }

            ExecutorService pool = Executors.newFixedThreadPool(getThread());
            String[] urls = textAreaUrls.getText().strip().split("\n");
            for (String url : urls){
                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() {
                        int index = url.indexOf("/", 8);
                        String serviceAddress = index == -1 ? url : url.substring(0, index);
                        HttpService service = HttpService.httpService(serviceAddress);

                        String Host = serviceAddress.replace("https://", "").replace("http://", "");

                        String path;
                        if (pathCheckBox.isSelected() && !Objects.equals(pathCheckBox.getText().strip(), "")) {
                            path = textPath.getText().strip();
                        } else {
                            path = index == -1 ? "/" : url.substring(index);
                        }

                        HttpRequest req = HttpRequest.httpRequest()
                                .withMethod(method)
                                .withPath(path)
                                .withService(service)
                                .withAddedHeader("Host", Host)
                                .withBody(textAreaBody.getText());

                        String headerString = textAreaHeaders.getText();

                        if (!headerString.isEmpty()) {
                            for (String header : headerString.strip().split("\n")) {
                                header = header.strip();
                                index = header.indexOf(":");
                                if (index == -1) {
                                    logging.logToError("invalid header: " + header);
                                    continue;
                                }
                                String k = header.substring(0, index).strip();
                                String v = header.substring(index + 1).strip();

                                if (!k.equals("Host")) {
                                    req = req.withAddedHeader(k, v);
                                } else {
                                    req = req.withUpdatedHeader(k, v);
                                }

                            }
                        }
                        api.http().sendRequest(req);
                        return null;
                    }

                    @Override
                    protected void done() {
                        int n = Integer.parseInt(labelDone.getText())+1;
                        labelDone.setText(String.valueOf(n));
                    }
                };
                //线程执行
                pool.execute(worker);
            }
            pool.shutdown();
            try {
                if (!pool.awaitTermination(getTimeout(), TimeUnit.SECONDS)){
                    logging.logToOutput("timeout");
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }
            sendButton.setEnabled(true);
            logging.logToOutput("Done! send button enable");
        }).start();
    }

    private String getReqMethod() {
        if (GETCheckBox.isSelected()) {
            return  "GET";
        } else if (POSTCheckBox.isSelected()) {
            return  "POST";
        } else {
            return textFieldMethod.getText();
        }
    }

    private  int getThread(){
        try {
            int Thread = Integer.parseInt(textFieldThread.getText());
            if (Thread > 0){
                return Thread;
            }else {
                return 10;
            }
        }
        catch (Exception e){
            textFieldThread.setText("10");
            return 10;
        }

    }

    private  int getTimeout(){
        try {
            int Thread = Integer.parseInt(textFieldTimeout.getText());
            if (Thread > 0){
                return Thread;
            }else {
                return 60;
            }
        }
        catch (Exception e){
            textFieldTimeout.setText("60");
            return 60;
        }

    }

}
