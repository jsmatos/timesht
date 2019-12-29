package com.jsmatos.timesheets.gui;

import javax.swing.*;

class VerticalPanel extends JPanel {
    private final BoxLayout layout;

    public VerticalPanel() {
        layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);
    }
}