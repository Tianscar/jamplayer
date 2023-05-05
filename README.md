# Java Audio Media Player
A quite simple and lightweight audio player for JavaSound, streaming playing to reduce memory usage.  
Supported audio formats: MP3, OGG Vorbis, FLAC, WAV, AIFF, AU, SND.

## Usage
[Examples](/src/test/java/com/tianscar/jamplayer/test/)

## TODO
- In-Memory audio player like Android's SoundPool, for short sound effects
- WavPack, Speex, OGG Speex, Opus, OGG Opus, OGG FLAC, AAC, MP4 AAC, Monkey's Audio, MP4 ALAC, CAF ALAC support
- Separate to several modules
- Swing widget and GUI

## License
[Apache-2.0](/LICENSE) (c) Tianscar  
[audios for test](/src/test/resources) originally created by [ProHonor](https://github.com/Aislandz), authorized [me](https://github.com/Tianscar) to use. 2023 (c) ProHonor, all rights reserved.

### This project currently uses the following libraries as dependencies:
[Apache-2.0](https://github.com/Gagravarr/VorbisJava/blob/master/LICENSE.txt) [VorbisJava](https://github.com/Gagravarr/VorbisJava)  
LGPL-2.1 [MP3SPI](https://mvnrepository.com/artifact/com.googlecode.soundlibs/mp3spi/1.9.5.4)  
LGPL-2.1 [VorbisSPI](https://mvnrepository.com/artifact/com.googlecode.soundlibs/vorbisspi/1.0.3.3)  
Apache-2.0 [jFLAC](https://jflac.sourceforge.net)