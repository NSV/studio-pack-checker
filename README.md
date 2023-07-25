[![Release](https://img.shields.io/github/v/release/NSV/studio-pack-checker)](https://github.com/NSV/studio-pack-checker/releases/latest)

SPC - STUdio Pack Checker
===============================

Verify, repair and compress your story packs for Lunii[^1] story teller device.

DISCLAIMER
----------

This software relies on reverse engineering research, which is limited to gathering the information necessary to ensure
interoperability with the Lunii[^1] story teller device, and does not distribute any protected content.

**USE AT YOUR OWN RISK**. Be advised that despite the best efforts to keep this software safe, it comes with
**NO WARRANTY** and may brick your device.

[^1]: Lunii and Luniistore are registered trademarks of Lunii SAS. This work is in no way affiliated with Lunii SAS.

USAGE
-----

### Prerequisite

* Java JRE 11+
* FFmpeg

### Installation

* **Download** [the latest release](https://github.com/NSV/studio-pack-checker/releases/latest) (
  or [build the application](#for-developers)).
* **Unzip** the distribution archive
* **Run the launcher script**: either `studio-pack-checker.sh` or `studio-pack-checker.bat` according to your platform.
  You may need to make them
  executable first.

### Configuration

Studio is portable by default: everything (except JRE and FFmpeg) is relative to current directory.

### Using the application

The application will check all the story packs in a folder (or just the specificied story pack)

##### Check

The application will verify that :

- The story.json is present and well-formed
- Image dimensions are 320x240
- Audio files are readable for STUdio

##### Repair

The application will repair only if needed :

- Convert audio files if not in MP3/OGG/WAVE formats. FFmpeg will be used.

##### Compresss

The application will compress only if needed :

- Compress image files to 4-bits depth / RLE encoding BMP
- Compress audio files to MP3 (mono, 44100Hz, 32kbits/s)

FOR DEVELOPERS
--------------

#### Prerequisite

* Maven 4+
* Java JDK 11+

#### Local build

* Clone this repository
* Run the application: `mvn exec:java -Dexec.mainClass="org.studio.checker.Main"`
* Package the application: `mvn package`
  This will produce the **distribution archive** in `target/studio-pack-checker-bin.zip`.

THIRD-PARTY APPLICATIONS
------------------------

If you liked STUdio, you will also like:

* [STUdio](https://github.com/kairoh/studio) create and transfer your own story packs to and from the Lunii[^1] story
  teller device.
* [Studio-Pack-Generator](https://github.com/jersou/studio-pack-generator) convert a folder or a RSS URL to Studio pack
  zip for Lunii device, see file structure below.
* [Studio-Pack-Generator](https://github.com/Seph29/LuniiKit_App) is a launcher for STUdio.

LICENSE
-------

This project is licensed under the terms of the **Mozilla Public License 2.0**. The terms of the license are in
the [LICENSE.md](LICENSE.md) file.

The `jvorbis`library, as well as the `VorbisEncoder` class are licensed by the Xiph.org Foundation. The terms of the
license can be found in the [LICENSE-jvorbis.md](LICENSE-jvorbis.md) file.

The `com.jhlabs.image`package is licensed by Jerry Huxtable under the terms of the Apache License 2.0. The terms of the
license can be found in the [LICENSE-jhlabs.md](LICENSE-jhlabs.md) file.
