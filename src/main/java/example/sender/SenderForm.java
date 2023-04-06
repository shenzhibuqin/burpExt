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

public class SenderForm {

    private MontoyaApi api;
    public HttpRequestEditor requestViewer;
    public HttpResponseEditor responseViewer;
    public JPanel panel1;
    private JTextField textFieldUrl;
    private JCheckBox GETCheckBox;
    private JCheckBox POSTCheckBox;
    private JCheckBox otherCheckBox;
    private JTextField textFieldMethod;
    private JButton sendButton;
    private JTextArea textAreaHeaders;
    private JTextArea textAreaBody;

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
            if (textFieldUrl.getText().equals("")) {
                logging.logToError("url is empty");
                return;
            }

            String url = textFieldUrl.getText();
            int index = url.indexOf("/", 8);
            String serviceAddress = index == -1 ? url : url.substring(0, index);
            logging.logToOutput("service = " + serviceAddress);
            HttpService service = HttpService.httpService(serviceAddress);

            String Host = serviceAddress.replace("https://", "").replace("http://", "");
            logging.logToOutput("Host = " + Host);

            String path = index == -1 ? "/" : url.substring(index);
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
    }

}
