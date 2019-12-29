package com.jsmatos.timesheets.gui;

import javax.swing.*;

public class HorizontalPanel extends JPanel {
    private final BoxLayout layout;

    public HorizontalPanel() {
        layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);
    }
}