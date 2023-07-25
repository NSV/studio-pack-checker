package org.studio.checker;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;

import java.io.IOException;

public class FFmpegCreator {

    private static FFmpeg ffmpeg;
    private static FFprobe ffprobe;
    private static FFmpegExecutor fFmpegExecutor;

    public static FFmpegExecutor getFFmpegExecutor() throws IOException {
        if (fFmpegExecutor == null) {
            fFmpegExecutor = new FFmpegExecutor(getFFmpeg(), getFFProbe());
        }
        return fFmpegExecutor;
    }

    public static FFmpeg getFFmpeg() throws IOException {
        if (ffmpeg == null) {
            ffmpeg = new FFmpeg();
        }
        return ffmpeg;
    }

    public static FFprobe getFFProbe() throws IOException {
        if (ffprobe == null) {
            ffprobe = new FFprobe();
        }
        return ffprobe;
    }

}
