/**
 * a note represents a single midi-note, determined by the note-on and note-off events
 */
public class Note {
    private final int channel, note;
    private final long start, end;

    public Note(int channel, int note, long start, long end) {
        // if (note + MIDIVisualizer.NOTE_OFFSET < 1 || note + MIDIVisualizer.NOTE_OFFSET > MIDIVisualizer.NOTE_COUNT)
        if (note < 1 || note > MIDIVisualizer.NOTE_COUNT)
            throw new IllegalArgumentException("note value not allowed");
        this.channel = channel;
        this.note = note;
        this.start = start;
        this.end = end;
    }

    /**
     * getter for {@link #start}
     *
     * @return start time in ticks
     */
    public long getStart() {
        return start;
    }

    /**
     * getter for {@link #end}
     *
     * @return end time in ticks
     */
    public long getEnd() {
        return end;
    }

    /**
     * getter for {@link #channel}
     *
     * @return midi channel
     */
    public int getChannel() {
        return channel;
    }

    /**
     * get note ranging from 0 to 87 respecting {@link MIDIVisualizer#NOTE_OFFSET}
     *
     * @return note value + OFFSET
     */
    public int getNote() {
        return note - 1; //+ MIDIVisualizer.NOTE_OFFSET - 1;
    }

    /**
     * duration of this note in ticks
     *
     * @return duration
     */
    public int getDuration() {
        return (int) (end - start);
    }

    @Override
    public String toString() {
        return "Note [ " + start + " ; " + end + " ]";
    }
}
