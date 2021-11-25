import javax.swing.*;
import java.awt.*;

/**
 * progress dialog for video rendering
 */
public class ProgressDialog extends JDialog {
    /** progress bar */
    private final JProgressBar progressBar;

    /**
     * create progress dialog
     * @param parent parent frame
     * @param max maximum value
     * @param onCancel called when the cancel button is pressed, does not have to dispose this dialog
     */
    public ProgressDialog(JFrame parent, int max, Runnable onCancel) {
        super(parent);
        setResizable(false);
        setTitle("Rendering video");
        setUndecorated(true);
        setIconImage(MIDIVisualizer.icon);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        progressBar = new JProgressBar(0, max);
        progressBar.setPreferredSize(new Dimension(300, 30));
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(15,20,15,20);
        panel.add(progressBar, gbc);

        JButton bCancel = new JButton("Cancel");
        bCancel.setPreferredSize(new Dimension(80, 25));
        bCancel.addActionListener(event -> {
            if (onCancel != null)
                onCancel.run();
            dispose();
        });
        gbc.gridy = 1;
        gbc.insets.top = 0;
        panel.add(bCancel, gbc);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * set value of the progress bar
     * @param value new value
     */
    public void update(int value) {
        progressBar.setValue(value);
    }
}
