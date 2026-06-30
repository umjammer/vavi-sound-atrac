[![Release](https://jitpack.io/v/umjammer/vavi-sound-atrac.svg)](https://jitpack.io/#umjammer/vavi-sound-atrac)
[![Java CI](https://github.com/umjammer/vavi-sound-atrac/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-atrac/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-atrac/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-sound-atrac/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-sound-atrac

<img alt="logo" src="src/test/resources/duke_md.png" width="160" />

Pure Java ATRAC series decoder (Java Sound SPI) powered by [Jpcsp](https://github.com/jpcsp/jpcsp) (atrac3+) and [libatrac9](https://github.com/Thealexbarney/LibAtrac9/tree/master/CSharp/LibAtrac9) (atrac9)

## install

 * [maven](https://jitpack.io/#umjammer/vavi-sound-atrac)

## Usage

```java
AudioInputStream atracAis = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(atrac)));
AudioFormat inFormat = sourceAis.getFormat();
AudioFormat outFormat = new AudioFormat(44100, 16, 2, true, false);
AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outFormat, atracAis);
SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, pcmAis.getFormat()));
line.open(pcmAis.getFormat());
line.start();
byte[] buffer = new byte[line.getBufferSize()];
int bytesRead;
while ((bytesRead = pcmAis.read(buffer)) != -1) {
  line.write(buffer, 0, bytesRead);
}
line.drain();
```

## References

 * https://github.com/korlibs-archive/korau-atrac3plus ... sample doesn't work, ~~same result as mine~~
 * https://github.com/Vincit/ffmpeg/blob/master/libavcodec/atrac3plus.c
 * atrac9
   * https://github.com/FFmpeg/FFmpeg/blob/master/libavcodec/atrac9dec.c
   * https://github.com/Thealexbarney/LibAtrac9/tree/master/CSharp/LibAtrac9
   * https://github.com/xXxTheDarkprogramerxXx/PS4_Tools
   * https://www.psdevwiki.com/ps4/Snd0.at9
   * [sony at9tool](https://mega.nz/file/6xlxGBAT#y7XR9u5bmS-qvu2Hd8DM0k_1aSmxaheTdN_5smKcfRc)
     * `at9tool -e -br 144 [-wholeloop] in.wav out.at9`

## TODO

 * ~~spi~~
 * file extension is `aa3`?
 * ~~atrac9~~
 * ~~project name vavi-sound-atrac3plus -> vavi-sound-atrac~~
 * ~~package name vavi.sound.sampled.atrac3 -> vavi.sound.sampled.atrac~~ 

---

<sub>image designed by @umjammer, drawn by nano banana</sub>
