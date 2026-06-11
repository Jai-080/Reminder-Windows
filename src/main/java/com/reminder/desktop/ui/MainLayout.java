package com.reminder.desktop.ui;

import com.reminder.desktop.MainApplication;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class MainLayout extends BorderPane {
    private final MainApplication app;
    private final Sidebar sidebar;

    public MainLayout(MainApplication app) {
        this.app = app;
        this.sidebar = new Sidebar(this, app);

        this.setLeft(sidebar);
        ThemeManager.registerRoot(this);

        // Show default view
        showView("Dashboard");
    }

    public void showView(String viewName) {
        sidebar.setActiveButton(viewName);
        Node centerView;
        
        switch (viewName) {
            case "Notes":
                centerView = new NotesView();
                break;
            case "Reminders":
                centerView = new ReminderView();
                break;
            case "Payments":
                centerView = new PaymentView();
                break;
            case "Dashboard":
            default:
                centerView = new DashboardView(this);
                break;
        }
        
        this.setCenter(centerView);
    }
}
