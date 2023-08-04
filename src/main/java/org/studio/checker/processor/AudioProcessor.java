package org.studio.checker.processor;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.studio.checker.FFmpegCreator;
import studio.core.v1.model.asset.MediaAsset;
import studio.core.v1.model.asset.MediaAssetType;
import studio.core.v1.utils.audio.AudioConversion;
import studio.core.v1.utils.audio.ID3Tags;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class AudioProcessor implements Function<MediaAsset, byte[]> {

    private static AudioProcessor singleton;

    private final boolean repair;
    private final boolean compress;

    public static AudioProcessor getProcessor(boolean repair, boolean compress) {
        if (singleton == null) {
            singleton = new AudioProcessor(repair, compress);
        }
        return singleton;
    }

    @SneakyThrows
    public byte[] apply(MediaAsset mediaAsset) {
        byte[] rawData = mediaAsset.getRawData();
        try {
            if (MediaAssetType.MP3 != mediaAsset.getType()) {
                if (compress) {
                    log.debug("Converting audio asset into MP3");
                    rawData = anyToMp3(rawData);
                    
                    // Remove potential ID3 tags
                    rawData = ID3Tags.removeID3v1Tag(rawData);
                    rawData = ID3Tags.removeID3v2Tag(rawData);
                }
            } else {
                // Check that the file is MONO / 44100Hz
                try (ByteArrayInputStream bais = new ByteArrayInputStream(rawData)) {
                    AudioFormat audioFormat = AudioSystem.getAudioFileFormat(bais).getFormat();
                    if (compress) {
                        if (audioFormat.getChannels() != AudioConversion.CHANNELS
                                || audioFormat.getSampleRate() != AudioConversion.MP3_SAMPLE_RATE) {
                            log.debug("Re-encoding MP3 audio asset");
                            rawData = anyToMp3(rawData);
                        }
                    }
                } finally {
                    if (compress) {
                        // Remove potential ID3 tags
                        rawData = ID3Tags.removeID3v1Tag(rawData);
                        rawData = ID3Tags.removeID3v2Tag(rawData);
                    }
                }
            }
        } catch (Exception e) {
            if (repair) {
                log.debug("Audio format not supported by javax.sound, use FFmpeg as failback");
                // Need to flush the data in a file in order to provide it to ffmpeg
                rawData = anyToMp3(rawData);
            } else {
                throw e;
            }
        }
        return rawData;
    }

    private static byte[] anyToMp3(byte[] rawData) throws IOException, UnsupportedAudioFileException {
        File inputTempFile = File.createTempFile("spc_audio_inputTempFile", ".audio");
        try (FileOutputStream fos = new FileOutputStream(inputTempFile)) {
            fos.write(rawData);
            fos.flush();

            File outputTempFile = File.createTempFile("spc_audio_outputTempFile", MediaAssetType.MP3.firstExtension());
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputTempFile.getAbsolutePath())
                    .addOutput(outputTempFile.getAbsolutePath())
                    .setAudioChannels(AudioConversion.CHANNELS)
                    .setAudioSampleRate(Math.round(AudioConversion.MP3_SAMPLE_RATE))
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .done();

            FFmpegJob job = FFmpegCreator.getFFmpegExecutor().createJob(builder);
            job.run();
            if (FFmpegJob.State.FINISHED.equals(job.getState())) {
                return Files.readAllBytes(outputTempFile.toPath());
            } else {
                return AudioConversion.anyToMp3(rawData);
            }
        }
    }

}
