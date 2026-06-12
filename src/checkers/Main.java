package checkers;

import checkers.controller.GameController;
import checkers.controller.NetworkManager;
import checkers.view.ConnectionDialog;
import checkers.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        MainFrame mainFrame = new MainFrame();
        ConnectionDialog dialog = new ConnectionDialog(mainFrame);
        ConnectionDialog.ConnectionConfig config = dialog.showDialog();
        if (!config.isConfirmed()) {
            return;
        }

        NetworkManager networkManager = new NetworkManager(config, null);
        GameController gameController = new GameController(mainFrame, networkManager);
        networkManager.setListener(gameController);
        mainFrame.setRestartAction(e -> gameController.requestRestart());

        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                gameController.shutdown();
            }
        });

        mainFrame.setVisible(true);
        gameController.startNetwork();
    }
}
