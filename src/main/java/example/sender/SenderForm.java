package example.sender;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SenderForm {

    public HttpRequestEditor requestViewer;
    public HttpResponseEditor responseViewer;
    public JPanel panel1;
    private final MontoyaApi api;
    private JTextField textFieldUrl;
    private JCheckBox GETCheckBox;
    private JCheckBox POSTCheckBox;
    private JCheckBox otherCheckBox;
    private JTextField textFieldMethod;
    private JButton sendButton;
    private JTextArea textAreaHeaders;
    private JTextArea textAreaBody;
    private JButton loadUrlsButton;
    private JTextArea textAreaHosts;
    private JTextField textPath;
    private JCheckBox pathCheckBox;

    public SenderForm(HttpRequestEditor requestViewer, HttpResponseEditor responseViewer, MontoyaApi api) {
        this.requestViewer = requestViewer;
        this.responseViewer = responseViewer;
        this.api = api;
        sendButton.addActionListener(e -> {
            Logging logging = api.logging();

            logging.logToOutput("-----prepare req-----");
            if (otherCheckBox.isSelected() && textFieldMethod.getText().equals("")) {
                logging.logToError("method is empty");
                return;
            }
            String method;
            if (GETCheckBox.isSelected()) {
                method = "GET";
            } else if (POSTCheckBox.isSelected()) {
                method = "POST";
            } else {
                method = textFieldMethod.getText();
            }
            if (textAreaHosts.getText().equals("")) {
                logging.logToError("url is empty");
                return;
            }
            logging.logToOutput("method = " + method);

            String url = textAreaHosts.getText().strip();
            int index = url.indexOf("/", 8);
            String serviceAddress = index == -1 ? url : url.substring(0, index);
            HttpService service = HttpService.httpService(serviceAddress);
            logging.logToOutput("service = " + serviceAddress);

            String Host = serviceAddress.replace("https://", "").replace("http://", "");
            logging.logToOutput("Host = " + Host);

            String path;
            if (pathCheckBox.isSelected() && !Objects.equals(pathCheckBox.getText().strip(), "")) {
                path = index == -1 ? textPath.getText().strip() : url.substring(index) + textPath.getText().strip();
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
            if (!headerString.equals("")) {
                for (String header : headerString.split("\n")) {
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
            }).start();
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
        loadUrlsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
                        textAreaHosts.setText(urls.toString().strip());
                    } catch (Exception exception) {
                    }
                }

            }
        });
    }

}
