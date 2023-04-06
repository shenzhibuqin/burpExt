/*
 * Copyright (c) 2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package example.sender;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import java.awt.*;

import static burp.api.montoya.ui.editor.EditorOptions.READ_ONLY;

public class Sender implements BurpExtension
{
    private MontoyaApi api;

    @Override
    public void initialize(MontoyaApi api)
    {
        this.api = api;
        api.extension().setName("Sender");
        api.userInterface().registerSuiteTab("Sender", createComponent());

    }

    private Component createComponent()
    {
        // main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // tabs with request/response viewers
//        JTabbedPane tabs = new JTabbedPane();
        JSplitPane splitPaneReq = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        UserInterface userInterface = api.userInterface();

        HttpRequestEditor requestViewer = userInterface.createHttpRequestEditor(READ_ONLY);
        HttpResponseEditor responseViewer = userInterface.createHttpResponseEditor(READ_ONLY);

//        tabs.addTab("Request", requestViewer.uiComponent());
//        tabs.addTab("Response", responseViewer.uiComponent());
        splitPaneReq.setLeftComponent(requestViewer.uiComponent());
        splitPaneReq.setRightComponent(responseViewer.uiComponent());
        splitPane.setRightComponent(splitPaneReq);

        SenderForm senderForm = new SenderForm(requestViewer,responseViewer,api);


        splitPane.setLeftComponent(senderForm.panel1);

        return splitPane;
    }
}