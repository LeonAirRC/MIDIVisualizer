import java.util.ArrayList;

public interface NotePlayer {
    byte NO_CHANNEL = 16;

    default boolean isPlaying(int note) {
        return getChannel(note) != NO_CHANNEL;
    }

    byte getChannel(int note);

    ArrayList<Note> getNotes();

    long getTicks();

    boolean isPaused();
}
