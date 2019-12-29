package com.jsmatos.timesheets.gui;

import com.jsmatos.timesheets.model.RegistrationHandler;
import com.jsmatos.timesheets.model.VisibilityChangedHandler;
import com.jsmatos.timesheets.model.*;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
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

    private Screen(InteractionAPI interactionAPI, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.interactionAPI = interactionAPI;
        init();
    }

    public static void startInstance(InteractionAPI interactionAPI) {
        java.awt.EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setType(Type.NORMAL);
            frame.setAlwaysOnTop(true);
            frame.setUndecorated(true);
            Screen dialog = new Screen(interactionAPI, frame, true);
            dialog.setSize(new Dimension(700, 200));
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    interactionAPI.publish(new CloseEvent());
                }
            });
            dialog.setTitle("Timeshiit");
            dialog.setVisible(true);
        });
    }

    private void init() {
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
    }

    private JPanel getCurrentWorkPanel() {
        JPanel currentWorkPanel = new JPanel();
        currentWorkPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("What have you been doing since %s ago?"));
        currentWorkPanel.add(currentWorkTextElement);
        currentWorkTextElement.setWrapStyleWord(true);
        currentWorkTextElement.setColumns(50);
        currentWorkTextElement.setRows(2);
        return currentWorkPanel;
    }

    private HorizontalPanel getButtonsPanel() {
        HorizontalPanel buttonsPanel = new HorizontalPanel();
        buttonsPanel.add(logButton);
        return buttonsPanel;
    }

    JPanel getHistoryPanel() {
        VerticalPanel historyPanel = new VerticalPanel();
        historyPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("What you did previously [%s]"));
        historyPanel.add(getSearchPanel());
        historyPanel.add(previousWorkListElement);
//        previousWorkListElement.setCellRenderer(new DefaultListCellRenderer());
        previousWorkListElement.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
                    if (evt.getSource() == previousWorkListElement) {
                        int index = previousWorkListElement.locationToIndex(evt.getPoint());
                        handlePreviousWorkSelected(index);
                    }
                }
            }
        });
        return historyPanel;
    }

    void handlePreviousWorkSelected(int index) {
        LogEntry logEntry = previousWorkListElement.getSelectedValue();
        if (logEntry != null) {
            this.currentWorkTextElement.setText(logEntry.getWhat());
            handleTextChanges();
        }
    }

    JPanel getSearchPanel() {
        HorizontalPanel searchFields = new HorizontalPanel();
        searchLabel.setText("Search:");
        searchFields.add(searchLabel);
        searchFields.add(filterPreviousWorkElement);
        filterPreviousWorkElement.setMaximumSize(new Dimension(Integer.MAX_VALUE,17));
        filterPreviousWorkElement.setColumns(50);
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
            List<LogEntry> filteredLogs = interactionAPI.getLogEntries(filterPreviousWorkElement.getText());
            LogEntry[] listData = filteredLogs.toArray(new LogEntry[0]);
            previousWorkListElement.setListData(listData);
        });
    }

    private void handleClipboard() {
        Toolkit aToolkit = Toolkit.getDefaultToolkit();
        aToolkit.beep();
        Clipboard systemClipboard = aToolkit.getSystemClipboard();
        DataFlavor[] availableDataFlavors = systemClipboard.getAvailableDataFlavors();
        for (DataFlavor dataFlavor : availableDataFlavors) {
            try {
                Object data = systemClipboard.getData(dataFlavor);
                System.out.println(dataFlavor.toString() + " ---> " + data);
                System.out.println();
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        System.out.println(String.format("[%s] - Screen.onVisibilityChanged( %s )", new Date(), visible));
        this.setVisible(visible);
    }
}
