import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.log4j.BasicConfigurator;

import javax.imageio.ImageIO;
import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MIDIVisualizer can read, play and export MIDI files.
 * <p>
 * Leon Bartmann 2021
 * <p>
 * This file is part of MIDIVisualizer.
 * <p>
 * MIDIVisualizer is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either
 * version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * MIDIVisualizer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR
 * PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with MIDIVisualizer. If not, see <<html>http://www.gnu.org/licenses/</html>>.
 * <p>
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES
 * PROVIDE
 * THE PROGRAM “AS IS”
 * WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE
 * ENTIRE RISK AS TO THE
 * QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 * <p>
 * IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS
 * PERMITTED
 * ABOVE, BE LIABLE TO YOU FOR
 * DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO
 * LOSS OF DATA
 * OR DATA BEING RENDERED
 * INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN
 * ADVISED OF
 * THE POSSIBILITY OF
 * SUCH DAMAGES.
 * <p>
 * <p>
 * MIDIVisualizer is the main class of this package and contains the whole GUI.
 */
public class MIDIVisualizer extends JPanel {
    /** displayed text in the 'About' window */
    public static final String infoText = "Shortcuts:\nFullscreen: F\nReset zoom: ESC\nExport video: Ctrl + E\nOpen MIDI: Ctrl + O\nRestart sequence: W\nMute: M\n\n\n\n"
            + "Leon Bartmann 2021\nReleased under the GNU GPL3 License\n<www.gnu.org/licenses/gpl-3.0>";

    /** number of notes supported by midi */
    public static final int MIDI_NOTES = 128;
    /** number of notes supported by this program */
    public static final int NOTE_COUNT = 88;
    /** number of white keys of the {@link #NOTE_COUNT} notes */
    private static final int WHITE_KEYS = 52;
    /** Determines the speed at which the notes move down at playback. Can be configured in the config file. */
    private static int TICKS_PER_PIXEL = 10;
    /** size of the original keyboard */
    private static final int KEYBOARD_WIDTH = 8827, KEYBOARD_HEIGHT = 866;
    /** Offset of the notes, added when read from the midi file. Can be configured in the config file. */
    public static int NOTE_OFFSET = 0;
    /** number of midi channels */
    public static final int CHANNELS = 16;
    /** saves the display color for each midi channel */
    public static final Color[] channelColors = new Color[CHANNELS];

    /** most left and most right note, range from 0 to {@link #WHITE_KEYS} */
    private int leftNote = 0, rightNote = WHITE_KEYS;

    /** file-chooser of this frame */
    private static JFileChooser fileChooser;
    /** the player playing the midi files */
    private static MidiPlayer player;
    /** main frame */
    private final JFrame frame;
    /** Images for background and the black key. Can be configured by putting a background.png next to the executable. */
    private static BufferedImage background, blackKey;
    /** image icon for the window decorations */
    public static BufferedImage icon;
    /** property map */
    private static Properties properties;
    /** background color, only used if no background image exists in the execution directory */
    public static Color backgroundColor;
    /** x position for the zoom range. Uses {@link Integer} to allow null values. */
    private Integer dragStart, mouseDragPos;
    /**
     * file extension filters for export and open dialogs
     */
    private final FileNameExtensionFilter exportFilter = new FileNameExtensionFilter("Video", "mp4"),
            openFilter = new FileNameExtensionFilter("Midi", "mid");

    public static void main(String[] args) {
        BasicConfigurator.configure();
        try {
            String executionDirectory = new File(MIDIVisualizer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            System.out.println("exec dir: " + executionDirectory);
            blackKey = ImageIO.read(Objects.requireNonNull(MIDIVisualizer.class.getResourceAsStream("blackKey.png")));
            icon = ImageIO.read(Objects.requireNonNull(MIDIVisualizer.class.getResourceAsStream("icon.png")));

            try (BufferedReader reader = new BufferedReader(new FileReader(executionDirectory + File.separator + "properties.config"))) {
                properties = new Properties();
                properties.load(reader);
                backgroundColor = ColorsDialog.toColor((String) properties.get("BACKGROUND_COLOR"));
                try {
                    background = ImageIO.read(new File(executionDirectory + File.separator + properties.get("BACKGROUND_IMAGE")));
                } catch (Exception ignored) {
                }
                for (int i = 0; i < CHANNELS; i++) {
                    channelColors[i] = ColorsDialog.toColor((String) properties.get("Channel_" + (i + 1)));
                    if (channelColors[i] == null)
                        throw new NullPointerException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Arrays.fill(channelColors, Color.BLUE);
                channelColors[0] = ColorsDialog.toColor("#41FCFD");
                channelColors[1] = ColorsDialog.toColor("#BE0302");
            }

            if (backgroundColor == null)
                backgroundColor = new Color(44, 44, 44);

            try {
                TICKS_PER_PIXEL = Integer.parseInt((String) properties.get("TICKS_PER_PIXEL"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                NOTE_OFFSET = Integer.parseInt((String) properties.get("NOTE_OFFSET"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            new MIDIVisualizer();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public MIDIVisualizer() throws IOException {
        super();
        FlatDarkLaf.setup();
        JPopupMenu menu = new JPopupMenu();
        JMenuItem menuOpen = new JMenuItem("Open midi");
        menuOpen.addActionListener(event -> loadFile());
        menu.add(menuOpen);
        JMenuItem menuExport = new JMenuItem("Export video");
        menuExport.addActionListener(event -> new Thread(MIDIVisualizer.this::exportVideo).start());
        menu.add(menuExport);
        JMenuItem menuColors = new JMenuItem("Color settings");
        menuColors.addActionListener(event -> new ColorsDialog(MIDIVisualizer.this));
        menu.add(menuColors);
        JMenuItem menuInfo = new JMenuItem("About");
        menuInfo.addActionListener(event -> JOptionPane.showMessageDialog(MIDIVisualizer.this, infoText, "About", JOptionPane.PLAIN_MESSAGE));
        menu.add(menuInfo);

        setComponentPopupMenu(menu);
        setDoubleBuffered(true);

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(this);
        frame.setIconImage(icon);
        getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "space");
        getActionMap().put("space", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (player == null)
                    return;
                if (player.isPaused())
                    player.start();
                else
                    player.pause();
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("control O"), "CTRLO");
        getActionMap().put("CTRLO", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menuOpen.doClick();
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("control E"), "CTRLE");
        getActionMap().put("CTRLE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menuExport.doClick();
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("W"), "W");
        getActionMap().put("W", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (player != null)
                    player.restart();
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("F"), "F");
        getActionMap().put("F", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                frame.setUndecorated(!frame.isUndecorated());
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESC");
        getActionMap().put("ESC", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftNote = 0;
                rightNote = WHITE_KEYS;
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e))
                    dragStart = e.getX();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e))
                    return;
                if (dragStart != null && mouseDragPos != null) {
                    zoom();
                    repaint();
                }
                dragStart = null;
                mouseDragPos = null;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseDragPos = e.getX();
                    repaint();
                }
            }
        });

        fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(800, 450));
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        SwingUtilities.updateComponentTreeUI(frame);
        SwingUtilities.updateComponentTreeUI(fileChooser);
    }

    /**
     * Displays a file-chooser to select the export file and format and starts rendering the video.
     * Blocks the executing thread until rendering is finished or cancelled.
     */
    private synchronized void exportVideo() {
        if (player == null)
            return;
        player.stop();
        ProgressDialog progressDialog = null;
        VideoRenderer renderer = null;
        File file = null;
        try {
            fileChooser.setFileFilter(exportFilter);
            fileChooser.setSelectedFile(new File(""));
            int opt = fileChooser.showSaveDialog(this);
            if (opt == JFileChooser.CANCEL_OPTION)
                return;
            file = fileChooser.getSelectedFile();
            if (!file.isDirectory() && !file.getPath().toLowerCase().endsWith(".mp4"))
                file = new File(file + ".mp4");
            if (file.exists()
                    && JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                return;
            VideoRenderer.init();
            int width = Integer.parseInt((String) properties.get("EXPORT_WIDTH")), height = Integer.parseInt((String) properties.get("EXPORT_HEIGHT"));
            int fps = Integer.parseInt((String) properties.get("EXPORT_FPS"));
            AtomicBoolean cancelled = new AtomicBoolean(false);
            progressDialog = new ProgressDialog(frame, (int) (player.getSequence().getMicrosecondLength() * fps / 1000000),
                    () -> cancelled.set(true));
            RenderingPlayer renderingPlayer = new RenderingPlayer(player.getSequence(), player.getNotes());
            frame.setEnabled(false);
            renderer = new VideoRenderer(file.getPath(), "mp4", null, fps, width, height);
            int frame = 0;
            do {
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                Graphics g = img.getGraphics();
                paintMidiPlayer(g, renderingPlayer, width, height, null);
                g.dispose();
                renderer.addFrame(img);
                frame++;
                renderingPlayer.nextTime(frame * 1000000L / fps);
                progressDialog.update(frame);
            } while (!cancelled.get() && !renderingPlayer.isAtEnd());
            renderer.finish();
            if (!cancelled.get())
                progressDialog.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            if (progressDialog != null)
                progressDialog.dispose();
            if (renderer != null)
                renderer.finish();
            if (file != null)
                file.delete();
            JOptionPane.showMessageDialog(this, "Export of the video failed", "Export error", JOptionPane.ERROR_MESSAGE);
        }
        frame.setEnabled(true);
        frame.toFront();
    }

    /**
     * Checks the zooming bounds {@link #dragStart} and {@link #mouseDragPos}. The minimum range of visible notes is two octaves.
     * If both values are correct, new zooming bounds are set.
     */
    private void zoom() {
        int note1 = dragStart * (rightNote - leftNote) / this.getWidth() + leftNote;
        int note2 = mouseDragPos * (rightNote - leftNote) / this.getWidth() + leftNote + 1;
        if (Math.abs(note2 - note1) > 13) {
            leftNote = Math.min(note1, note2);
            rightNote = Math.max(note1, note2);
        }
    }

    /**
     * Allows the user to import a new midi file. Does not consider if another midi file is currently loaded.
     */
    private synchronized void loadFile() {
        fileChooser.setFileFilter(openFilter);
        fileChooser.setSelectedFile(new File(""));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                MidiPlayer newPlayer = new MidiPlayer(this, MidiSystem.getSequence(fileChooser.getSelectedFile()));
                if (player != null) { // only stop current player if no exception occurred while loading the new sequence
                    player.stop();
                }
                player = newPlayer;
                repaint();
            } catch (InvalidMidiDataException | MidiUnavailableException e) {
                JOptionPane.showMessageDialog(this, "The loaded file does not point to valid MIDI file data recognized by the system", "Error loading file",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "The selected file could not be read", "Error loading file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * determine if a note is a white key
     *
     * @param note note ranging from 0 to 87
     * @return true if the note with the specified number is a white key
     */
    private static boolean isWhiteKey(int note) {
        note = note % 12;
        return !(note == 1 || note == 4 || note == 6 || note == 9 || note == 11);
    }

    /**
     * If the key with the given number is a white key, the number of the corresponding white key is returned, ranging from 0 to {@link #WHITE_KEYS} - 1.
     * Otherwise the number of the corresponding black key is returned, ranging from 0 to {@link #WHITE_KEYS} - 2. Thus there are numbers for black keys that don't
     * exist, but
     * they grow linearly.
     *
     * @param note note ranging from 0 to 87
     * @return the number of the key
     */
    private static int noteToColoredKey(int note) {
        int octave = note / 12;
        int key;
        switch (note % 12) {
            case 0:
            case 1:
                key = 0;
                break;
            case 2:
                key = 1;
                break;
            case 3:
            case 4:
                key = 2;
                break;
            case 5:
            case 6:
                key = 3;
                break;
            case 7:
                key = 4;
                break;
            case 8:
            case 9:
                key = 5;
                break;
            case 10:
            case 11:
                key = 6;
                break;
            default:
                key = -1;
                break;
        }
        return octave * 7 + key;
    }

    /**
     * Returns the midi note number (0-87) for the given white key
     *
     * @param key white key number
     * @return the number of the corresponding note
     */
    private static int whiteKeyToNote(int key) {
        int octave = key / 7;
        int note;
        switch (key % 7) {
            case 0:
                note = 0;
                break;
            case 1:
                note = 2;
                break;
            case 2:
                note = 3;
                break;
            case 3:
                note = 5;
                break;
            case 4:
                note = 7;
                break;
            case 5:
                note = 8;
                break;
            case 6:
                note = 10;
                break;
            default:
                note = -1;
                break;
        }
        return octave * 12 + note;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        paintMidiPlayer(g, player, this.getWidth(), this.getHeight(), this);
    }

    /**
     * Paints a {@link NotePlayer} with the given graphics object in the given size.
     * The keyboard-section that is printed is determined by {@link #leftNote} and {@link #rightNote}.
     * The keyboard always spans the whole width and is aligned on the bottom.
     * The background is scaled to match the remaining space above the keyboard and is painted centered.
     *
     * @param g          graphics object
     * @param player     the midi player, provides the notes and key-press states
     * @param areaWidth  paint area width
     * @param areaHeight paint area height
     * @param observer   image observer for images, may be null
     */
    private void paintMidiPlayer(Graphics g, NotePlayer player, int areaWidth, int areaHeight, ImageObserver observer) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float kbScale = 1f * areaWidth * WHITE_KEYS / (rightNote - leftNote) / KEYBOARD_WIDTH;
        int kbHeight = Math.round(KEYBOARD_HEIGHT * kbScale);

        if (background == null) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, areaWidth, areaHeight);
        } else {
            float bgScale = Math.max((float) areaWidth / background.getWidth(), (float) (areaHeight - kbHeight) / background.getHeight());
            g2d.drawImage(background, Math.round((areaWidth - bgScale * background.getWidth()) / 2),
                    Math.round((areaHeight - kbHeight - bgScale * background.getHeight()) / 2),
                    (int) Math.ceil(background.getWidth() * bgScale), (int) Math.ceil(background.getHeight() * bgScale), observer);
        }

        if (player != null) {
            for (Note note : player.getNotes()) {
                g2d.setColor(channelColors[note.getChannel()]);
                int x = isWhiteKey(note.getNote()) ?
                        ((noteToColoredKey(note.getNote()) - leftNote) * areaWidth / (rightNote - leftNote) + (areaWidth / (rightNote - leftNote) - whiteNoteWidth(areaWidth)) / 2)
                        : ((noteToColoredKey(note.getNote()) + 1 - leftNote) * areaWidth / (rightNote - leftNote) - blackNoteWidth(areaWidth) / 2);
                float y = (areaHeight - kbHeight) + (float) (player.getTicks() - note.getEnd()) / TICKS_PER_PIXEL;
                g2d.fill(new RoundRectangle2D.Float(x, y, widthForNote(note.getNote(), areaWidth), (float) note.getDuration() / TICKS_PER_PIXEL, 5, 5));
            }
        }

        g.setColor(new Color(191, 191, 191));
        g.fillRect(0, areaHeight - kbHeight, areaWidth, kbHeight);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(kbScale * 3));
        for (int i = 0; i <= rightNote - leftNote; i++) {
            float x = 1f * i * areaWidth / (rightNote - leftNote);
            g2d.draw(new Line2D.Float(x, areaHeight - kbHeight + 1.5f * kbScale/* remove tip */, x, areaHeight));
            int note = whiteKeyToNote(i + leftNote);
            if (player != null && player.isPlaying(note)) {
                g2d.setColor(channelColors[player.getChannel(note)]);
                g2d.fill(new Rectangle2D.Float(x + 1.5f * kbScale, areaHeight - kbHeight, kbScale * KEYBOARD_WIDTH / WHITE_KEYS - 3 * kbScale, kbHeight));
                g2d.setColor(Color.BLACK);
            }
        }

        for (int i = 0; i < NOTE_COUNT; i++)
            if (!isWhiteKey(i)) {
                int x = (noteToColoredKey(i) + 1 - leftNote) * areaWidth / (rightNote - leftNote) - Math.round(kbScale * blackKey.getWidth() / 2);
                int width = Math.round(kbScale * blackKey.getWidth()), height = Math.round(kbScale * blackKey.getHeight());
                g.drawImage(blackKey, x, areaHeight - kbHeight, width, height, observer);
                if (player != null && player.isPlaying(i)) {
                    g.setColor(channelColors[player.getChannel(i)]);
                    g.fillRect(x, areaHeight - kbHeight, width, height);
                }
            }

        if (this == observer && dragStart != null && mouseDragPos != null) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(dragStart, 0, dragStart, areaHeight - kbHeight);
            g.drawLine(mouseDragPos, 0, mouseDragPos, areaHeight - kbHeight);
            g.setColor(new Color(0, 0, 0, 50));
            g.fillRect(dragStart, 0, mouseDragPos - dragStart, areaHeight - kbHeight);
        }
    }

    /**
     * get the display width for white-key notes
     *
     * @param areaWidth width of the whole paint area in pixels
     * @return the display width in pixels for the note blocks corresponding to white keys
     */
    private int whiteNoteWidth(int areaWidth) {
        return areaWidth / (rightNote - leftNote) * 3 / 5;
    }

    /**
     * get the display width for black-key notes
     *
     * @param areaWidth width of the whole paint area in pixels
     * @return the display width in pixels for the note blocks corresponding to black keys
     */
    private int blackNoteWidth(int areaWidth) {
        return areaWidth / (rightNote - leftNote) * 2 / 5;
    }

    /**
     * get the display width for any note
     *
     * @param note      note number, ranging from 0 to 87
     * @param areaWidth width of the whole paint area in pixels
     * @return the display width in pixels for the note blocks, depending on whether this note corresponds to a white or black key
     */
    private int widthForNote(int note, int areaWidth) {
        return isWhiteKey(note) ? whiteNoteWidth(areaWidth) : blackNoteWidth(areaWidth);
    }
}
