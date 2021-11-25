import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MidiPlayer implements NotePlayer {
    /** true if and only if the player is not playing */
    private boolean paused;
    /** list of notes of all midi tracks, sorted by the end ticks */
    private final ArrayList<Note> notes;
    /** the timer which is used to repaint the parent frame while playback is running */
    private Timer timer;
    /** midi sequence */
    private final Sequence sequence;
    /** midi player */
    private final Sequencer sequencer;
    /** saves for each note (index 0-87) the channel on which this note is played or {@link #NO_CHANNEL} if the note is not played */
    private final byte[] playing;
    /** parent frame */
    private final MIDIVisualizer parent;

    /**
     * create a midi player
     *
     * @param parent   parent frame
     * @param sequence midi sequence to play
     * @throws MidiUnavailableException at midi error
     * @throws InvalidMidiDataException at midi error
     */
    public MidiPlayer(MIDIVisualizer parent, Sequence sequence) throws MidiUnavailableException, InvalidMidiDataException {
        this.parent = parent;
        this.sequence = sequence;
        playing = new byte[MIDIVisualizer.NOTE_COUNT];
        Arrays.fill(playing, NO_CHANNEL);
        notes = new ArrayList<>();

        final long[][] startTicks = new long[MIDIVisualizer.CHANNELS][MIDIVisualizer.MIDI_NOTES];
        for (Track track : sequence.getTracks()) {
            for (long[] arr: startTicks)
                Arrays.fill(arr, -1);
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof ShortMessage) {
                    ShortMessage message = (ShortMessage) event.getMessage();
                    message.setMessage(message.getCommand(), message.getChannel(), message.getData1() + MIDIVisualizer.NOTE_OFFSET, message.getData2());
                    if (message.getCommand() == ShortMessage.NOTE_ON) {
                        if (startTicks[message.getChannel()][message.getData1()] == -1) {
                            startTicks[message.getChannel()][message.getData1()] = event.getTick();
                        } else {
                            try {
                                message.setMessage(ShortMessage.NOTE_OFF, message.getChannel(), message.getData1(), message.getData2());
                                notes.add(new Note(message.getChannel(), message.getData1(), startTicks[message.getChannel()][message.getData1()], event.getTick()));
                                startTicks[message.getChannel()][message.getData1()] = -1;
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (message.getCommand() == ShortMessage.NOTE_OFF) {
                        try {
                            notes.add(new Note(message.getChannel(), message.getData1(), startTicks[message.getChannel()][message.getData1()], event.getTick()));
                            startTicks[message.getChannel()][message.getData1()] = -1;
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        paused = true;
        notes.sort((note1, note2) -> (int) Math.signum(note1.getEnd() - note2.getEnd()));

        sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.getTransmitter().setReceiver(new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (!(message instanceof ShortMessage))
                    return;
                ShortMessage sm = (ShortMessage) message;
                if (sm.getCommand() == ShortMessage.NOTE_ON) {
                    playing[sm.getData1() - 1] = (byte) sm.getChannel();
                } else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
                    playing[sm.getData1() - 1] = NO_CHANNEL;
//                    if (sequencer.getTickPosition() >= notes.get(notes.size() - 1).getEnd())
//                        stop();
                }
            }

            @Override
            public void close() {
            }
        });
    }

    /**
     * if paused, starts the sequencer and timer to update the ui
     */
    public synchronized void start() {
        if (!paused || sequencer.getTickPosition() > notes.get(notes.size() - 1).getEnd())
            return;
        paused = false;
        sequencer.start();
        parent.repaint();
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                parent.repaint();
                if (sequencer.getTickPosition() > notes.get(notes.size() - 1).getEnd())
                    stop();
            }
        }, 0, 4);
    }

    /**
     * calls {@link #stop()} if not paused
     */
    public synchronized void pause() {
        if (!paused)
            stop();
    }

    /**
     * stops the timer and sequencer
     */
    public synchronized void stop() {
        paused = true;
        sequencer.stop();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        parent.repaint();
    }

    /**
     * calls {@link #stop()} and sets the sequencer position to 0
     */
    public void restart() {
        stop();
        sequencer.setTickPosition(0);
        Arrays.fill(playing, NO_CHANNEL);
        parent.repaint();
    }

    /**
     * getter for {@link #notes}
     *
     * @return note list
     */
    @Override
    public ArrayList<Note> getNotes() {
        return notes;
    }

    /**
     * @return the played time in microseconds
     */
    @Override
    public long getTicks() {
        return sequencer.getTickPosition();
    }

    /**
     * get the channel currently playing the given note
     *
     * @param note note number (0-87)
     * @return the channel (0-15) or {@link #NO_CHANNEL} if no channel is currently playing this note
     */
    @Override
    public byte getChannel(int note) {
        try {
            return playing[note]; // - MIDIVisualizer.NOTE_OFFSET];
        } catch (ArrayIndexOutOfBoundsException e) {
            return NO_CHANNEL;
        }
    }

    /**
     * getter for {@link #paused}
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * getter for {@link #sequence}
     *
     * @return the sequence
     */
    public Sequence getSequence() {
        return sequence;
    }
}
