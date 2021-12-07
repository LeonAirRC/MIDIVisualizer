import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * the midi player used for rendering
 */
public class RenderingPlayer implements NotePlayer {
    /** used to convert microseconds to ticks */
    private final Sequencer sequencer;
    /** notes of the rendered sequence */
    private final ArrayList<Note> notes;
    /** saves for each note (index 0-87) the channel on which this note is played or NO_CHANNEL if the note is not played */
    private final byte[] playing;

    /**
     * create rendering player
     *
     * @param sequence sequence to play
     * @param notes    list of notes
     */
    public RenderingPlayer(Sequence sequence, ArrayList<Note> notes) throws MidiUnavailableException, InvalidMidiDataException {
        this.notes = notes;
        playing = new byte[MIDIVisualizer.NOTE_COUNT];
        Arrays.fill(playing, NO_CHANNEL);
        sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.setSequence(sequence);
    }

    /**
     * updates the current time
     *
     * @param newTime new time in microseconds
     */
    public void nextTime(long newTime) {
        long oldTicks = sequencer.getTickPosition();
        if (oldTicks == 0)
            for (Note note : notes)
                if (note.getStart() == 0 && note.getEnd() > 0)
                    playing[note.getNote()] = (byte) note.getChannel();
        sequencer.setMicrosecondPosition(newTime);
        long newTicks = sequencer.getTickPosition();
        for (Note note : notes) {
            if (oldTicks < note.getStart() && note.getStart() <= newTicks && note.getEnd() > newTicks) {
                playing[note.getNote()] = (byte) note.getChannel();
            } else if (oldTicks <= note.getEnd() && note.getEnd() < newTicks) {
                playing[note.getNote()] = NO_CHANNEL;
            }
        }
    }

    /**
     * check if the player is at the end of the track
     *
     * @return true if the player is at the end of the track, false otherwise
     */
    public boolean isAtEnd() {
        return sequencer.getTickPosition() > notes.get(notes.size() - 1).getEnd() + 1000000 * sequencer.getTickLength() / sequencer.getMicrosecondLength();
    }

    /**
     * get the channel currently playing the given note
     *
     * @param note note number
     * @return the channel (0-15) or {@link #NO_CHANNEL} if no channel is currently playing this note
     */
    @Override
    public byte getChannel(int note) {
        try {
            return playing[note];
        } catch (ArrayIndexOutOfBoundsException e) {
            return NO_CHANNEL;
        }
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

    public boolean isPaused() {
        return false;
    }
}
