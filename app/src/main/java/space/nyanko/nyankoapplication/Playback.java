package space.nyanko.nyankoapplication;

/**
 * \brief Stors the playback information
 *
 * - Maps to each tab.
 * - There are as many playback instanes as the number of tabs.
 *
 */
public class Playback {

    private static final String TAG = "Playback";

    // Play position in milliseconds
    private int position = 0;

    public String filePath;

    // 0 stopped
    // 1 playing
    // 2 paused
    int state = 0;
}
