import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * small dialog that allows the user to change colors at runtime
 */
public class ColorsDialog extends JDialog {
    /** text fields for color hex codes */
    private final JTextField[] textFields;
    /** text field for background color */
    private final JTextField tfBackground;
    /** general size for text fields */
    private static final Dimension tfSize = new Dimension(80, 22);
    /** parent frame */
    private final MIDIVisualizer parent;

    public ColorsDialog(MIDIVisualizer parent) {
        super();
        this.parent = parent;
        setTitle("Set colors");
        setResizable(false);
        setUndecorated(true);
        setIconImage(MIDIVisualizer.icon);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = gbc.weighty = 1;
        gbc.insets.top = 7;
        gbc.insets.left = 20;
        textFields = new JTextField[MIDIVisualizer.CHANNELS];
        for (int i = 0; i < MIDIVisualizer.CHANNELS; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.insets.right = 0;
            JLabel label = new JLabel("Channel " + (i + 1));
            panel.add(label, gbc);
            gbc.gridx = 1;
            gbc.insets.right = 20;
            textFields[i] = new JTextField(toHex(MIDIVisualizer.channelColors[i]));
            textFields[i].setPreferredSize(tfSize);
            textFields[i].setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(textFields[i], gbc);
        }

        gbc.gridx = 0;
        gbc.gridy = MIDIVisualizer.CHANNELS;
        gbc.insets.right = 0;
        JLabel label = new JLabel("Background:");
        panel.add(label, gbc);
        gbc.gridx = 1;
        gbc.insets.right = 20;
        tfBackground = new JTextField(toHex(MIDIVisualizer.backgroundColor));
        tfBackground.setPreferredSize(tfSize);
        tfBackground.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(tfBackground, gbc);

        gbc.gridy = MIDIVisualizer.CHANNELS + 1;
        gbc.insets.bottom = gbc.insets.top;
        gbc.insets.top = 10;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JButton ok = new JButton("OK");
        ok.addActionListener(event -> exit());
        ok.setPreferredSize(new Dimension(70, 22));
        panel.add(ok, gbc);

        setContentPane(panel);
        getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESC");
        getRootPane().getActionMap().put("ESC", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * checks the text in all text fields and changes the channel colors if possible, then disposes this dialog
     */
    public void exit() {
        Color col;
        for (int i = 0; i < MIDIVisualizer.CHANNELS; i++) {
            if ((col = toColor(textFields[i].getText())) != null)
                MIDIVisualizer.channelColors[i] = col;
        }
        col = toColor(tfBackground.getText());
        if (col != null)
            MIDIVisualizer.backgroundColor = col;
        dispose();
        parent.repaint();
    }

    /**
     * create hex code from color
     *
     * @param color awt color
     * @return the hex-string describing the given color, contains a leading #, respects alpha values or null if color is null
     */
    private static String toHex(Color color) {
        if (color == null)
            return null;
        if (color.getAlpha() == 0xFF)
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()).toUpperCase();
        else
            return String.format("#%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).toUpperCase();
    }

    /**
     * create awt color from hex code
     *
     * @param hex hex-string, either 7 or 9 characters in length, has to contain a leading '#'
     * @return the color corresponding to the specified hex code or null if the input is wrong
     */
    public static Color toColor(String hex) {
        if (hex == null || hex.charAt(0) != '#')
            return null;
        try {
            if (hex.length() == 7)
                return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16));
            else if (hex.length() == 9)
                return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16),
                        Integer.parseInt(hex.substring(7, 9), 16));
            else
                return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
