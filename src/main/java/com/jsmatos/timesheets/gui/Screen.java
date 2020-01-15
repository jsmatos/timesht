package com.jsmatos.timesheets.gui;

import com.jsmatos.timesheets.model.*;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Screen extends JDialog implements LogEntryCreatedHandler, VisibilityChangedHandler {
    private JButton logButton;
    private JTextArea currentWorkTextElement;
    private JList<LogEntry> previousWorkListElement;
    private JTextField filterPreviousWorkElement;
    private JLabel searchLabel;
    private final InteractionAPI interactionAPI;
    private final List<RegistrationHandler> registrationHandlers = new ArrayList<>();

    private Screen(InteractionAPI interactionAPI) {
        super((JFrame) null, true);
        this.interactionAPI = interactionAPI;
        init();
    }

    public static Screen startInstance(InteractionAPI interactionAPI) {
        Screen dialog = new Screen(interactionAPI);
        dialog.setAlwaysOnTop(true);
        dialog.setSize(new Dimension(700, 200));
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                interactionAPI.publish(new CloseEvent());
            }
        });
        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension size = e.getComponent().getSize();
                System.out.println("Screen.componentResized: " + size);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                Point locationOnScreen = e.getComponent().getLocationOnScreen();
                System.out.println("Screen.componentMoved: " + locationOnScreen);
            }
        });
        dialog.setTitle("Timeshiit");
        java.awt.EventQueue.invokeLater(() -> {
            dialog.pack();
            dialog.setVisible(true);
            dialog.pack();
        });
        return dialog;
    }

    private void init() {
        setLayout(new BorderLayout());
        logButton = new JButton();
        JLabel durationLabel = new JLabel();
        currentWorkTextElement = new JTextArea();
        searchLabel = new JLabel();
        filterPreviousWorkElement = new JTextField();
        previousWorkListElement = new JList<>();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        durationLabel.setText("Duration:");
        logButton.setText("Log");
        logButton.setEnabled(false);
        logButton.addActionListener(this::actionPerformed);
        currentWorkTextElement.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                handleTextChanges();
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
                    if (StringUtils.isNotBlank(currentWorkTextElement.getText())){
                        logWork();
                    }
                }
            }
        });
        HorizontalPanel buttonsPanel = getButtonsPanel();

        VerticalPanel panel = new VerticalPanel();
        panel.add(getHistoryPanel());
        panel.add(getCurrentWorkPanel());
        panel.add(buttonsPanel);
        add(panel);
        registrationHandlers.add(this.interactionAPI.registerLogEntryCreatedHandler(this));
        registrationHandlers.add(this.interactionAPI.registerVisibilityChangedHandler(this));
        pack();
    }

    private JPanel getCurrentWorkPanel() {
        JPanel currentWorkPanel = new JPanel();
        currentWorkPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("What have you been doing since %s ago?"));
        currentWorkPanel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(currentWorkTextElement);
        currentWorkPanel.add(scrollPane);
        currentWorkTextElement.setWrapStyleWord(true);
        currentWorkTextElement.setAutoscrolls(true);
        return currentWorkPanel;
    }

    private HorizontalPanel getButtonsPanel() {
        HorizontalPanel buttonsPanel = new HorizontalPanel();
        buttonsPanel.add(logButton);
        return buttonsPanel;
    }

    private JPanel getHistoryPanel() {
        VerticalPanel historyPanel = new VerticalPanel();
        historyPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("What you did previously [%s]"));
        historyPanel.add(getSearchPanel());
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(previousWorkListElement);

        historyPanel.add(scrollPane);
        previousWorkListElement.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        previousWorkListElement.setAutoscrolls(true);
        previousWorkListElement.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
                    if (evt.getSource() == previousWorkListElement) {
                        int index = previousWorkListElement.locationToIndex(evt.getPoint());
                        handlePreviousWorkSelected();
                    }
                }
            }
        });
        return historyPanel;
    }

    void handlePreviousWorkSelected() {
        LogEntry logEntry = previousWorkListElement.getSelectedValue();
        if (logEntry != null) {
            this.currentWorkTextElement.setText(logEntry.getWhat());
            handleTextChanges();
        }
    }

    private JPanel getSearchPanel() {
        HorizontalPanel searchFields = new HorizontalPanel();
        searchLabel.setText("Search:");
        searchFields.add(searchLabel);
        searchFields.add(filterPreviousWorkElement);
        filterPreviousWorkElement.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    updatePreviousWork();
                }
            }
        });
        JButton searchButton = new JButton("search");
        searchButton.addActionListener(e -> {
            updatePreviousWork();
        });
        searchFields.add(searchButton);
        filterPreviousWorkElement.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));
        return searchFields;
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (logButton == source) {
            logWork();
        }
    }

    void handleTextChanges() {
        String text = currentWorkTextElement.getText();
        logButton.setEnabled(StringUtils.isNotBlank(text));
    }

    private void logWork() {
        try {
            String text = currentWorkTextElement.getText();
            Date now = new Date();
            LogEntry logEntry = new LogEntry(now, text);
            interactionAPI.publish(logEntry);
            setVisible(false);
            currentWorkTextElement.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, String.format("An error occurred while saving the log: :%s", ex.getMessage()));
            ex.printStackTrace();
        }
    }

    @Override
    public void onLogEntryCreated(LogEntry logEntry) {

    }

    @Override
    public void setVisible(final boolean visible) {
        EventQueue.invokeLater(() -> {
            if (visible) {
                if (Screen.super.isVisible()) {
                    return;
                }
                updatePreviousWork();
            }
            Screen.super.setVisible(visible);
            currentWorkTextElement.grabFocus();
        });
    }

    private void updatePreviousWork() {
        EventQueue.invokeLater(() -> {
            List<LogEntry> filteredLogs = interactionAPI.getLogEntries(filterPreviousWorkElement.getText(), true, 5);
            LogEntry[] listData = filteredLogs.toArray(new LogEntry[0]);
            previousWorkListElement.setListData(listData);
        });
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        System.out.println(String.format("[%s] - Screen.onVisibilityChanged( %s )", new Date(), visible));
        this.setVisible(visible);
    }
}
