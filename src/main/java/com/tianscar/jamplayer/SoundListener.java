package com.tianscar.jamplayer;

import java.util.EventListener;

public interface SoundListener extends EventListener {

    void update(SoundEvent event);

}
