package com.tianscar.jamplayer;

import java.util.EventListener;

public interface MusicListener extends EventListener {

    void update(MusicEvent event);

}
