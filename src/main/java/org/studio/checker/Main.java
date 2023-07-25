package org.studio.checker;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.studio.checker.processor.AudioProcessor;
import org.studio.checker.processor.ImageProcessor;
import picocli.CommandLine;
import studio.core.v1.exception.StoryTellerException;
import studio.core.v1.model.StoryPack;
import studio.core.v1.model.asset.MediaAsset;
import studio.core.v1.model.asset.MediaAssetType;
import studio.core.v1.model.metadata.StoryPackMetadata;
import studio.core.v1.service.PackFormat;
import studio.core.v1.utils.stream.StoppingConsumer;
import studio.core.v1.utils.stream.ThrowingFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

@CommandLine.Command(name = "studio-pack-checker", mixinStandardHelpOptions = true, version = "studio-pack-checker 0.1",
        description = "Check and compress archive packs for STUdio.", defaultValueProvider = CommandLine.PropertiesDefaultProvider.class)
@Slf4j
public class Main implements Callable<Integer> {

    private static final String CONVERTED_SPC_ZIP = "_converted_spc.zip";

    @CommandLine.Parameters(index = "0", description = "The folder where are packs or the filepath of a unique pack.")
    private Path inputPath;

    @CommandLine.Option(names = {"-r", "--repair"}, description = "Repair if needed the pack file")
    private boolean repair;

    @CommandLine.Option(names = {"-c", "--compress"}, description = "Compress (and repair) if needed the pack file")
    private boolean compress;

    @CommandLine.Option(names = {"-o", "--overwrite"}, description = "Overwrite the pack")
    private boolean overwrite;

    @CommandLine.Option(names = {"-p", "--parallel"}, description = "Parallel process")
    private boolean parallel;

    @CommandLine.Option(names = {"-i", "--inc"}, description = "Increase version pack number if modification")
    private boolean increaseVersion;

    private enum MediaGroup {
        AUDIO, IMAGE;
    }

    // this example implements Callable, so parsing, error handling and handling user
    // requests for usage help or version help can be done with one line of code.
    public static void main(String... args) {
        CommandLine cmd = new CommandLine(new Main());
        File defaultsFile = new File("command.properties");
        cmd.setDefaultValueProvider(new CommandLine.PropertiesDefaultProvider(defaultsFile));
        int exitCode = cmd.execute(args);

        log.info("End of studio-pack-checker");
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        log.info("Starting studio-pack-checker");
        repair = compress || repair; // If compress is activated, so repair too

        if (Files.isDirectory(inputPath)) {
            try (Stream<Path> list = Files.list(inputPath)) {
                list.filter(path -> ThrowingFunction.unchecked(internPath -> Files.isRegularFile(path) && !Files.isHidden(path)).apply(path)).forEach(this::checkPackWithResult);
            }
        } else {
            this.checkPackWithResult(inputPath);
        }
        return 0;
    }

    private void checkPackWithResult(Path path) {
        if (!path.toString().endsWith(CONVERTED_SPC_ZIP)) {
            log.info("Checking {} pack", path.getFileName());
            CheckResult result = checkPack(path);
            result.showResult();
            log.info("------------------------");
        }
    }

    public CheckResult checkPack(Path inputPath) {
        PackFormat format = PackFormat.fromPath(inputPath);
        if (!PackFormat.ARCHIVE.equals(format)) {
            return CheckResult.ko(inputPath, "Wrong extension, only check zip files");
        }

        try {
            StoryPackMetadata metadata = format.getReader().readMetadata(inputPath);

            if (metadata == null) {
                return CheckResult.ko(inputPath, "No story.json found");
            }

            if (metadata.getThumbnail() == null) {
                log.warn("No thumbnail found");
            }
        } catch (IOException e) {
            return CheckResult.ko(inputPath, e);
        }

        try {
            StoryPack pack = format.getReader().read(inputPath);
            Path outputPath;
            if (repair && !overwrite) {
                outputPath = Path.of(inputPath.getParent() + "/" + inputPath.getFileName() + CONVERTED_SPC_ZIP);
                if (Files.exists(outputPath)) {
                    return CheckResult.ko(inputPath, "Target file already exists");
                }
            } else {
                outputPath = inputPath;
            }

            try {
                if (recompress(pack)) {
                    if (increaseVersion) {
                        pack.setVersion((short) (pack.getVersion() + 1));
                    }
                    format.getWriter().write(pack, outputPath, true);
                    return compress ? CheckResult.compress(outputPath, pack) : CheckResult.repair(outputPath, pack);
                }
            } catch (StoryTellerException e) {
                // TODO Need to handle these errors
                return CheckResult.ko(inputPath, e);
            }

            return CheckResult.ok(inputPath, pack);
        } catch (IOException e) {
            return CheckResult.ko(inputPath, e);
        }
    }

    public boolean recompress(StoryPack storyPack) {
        // Image
        boolean modification = processAssets(storyPack, MediaGroup.IMAGE, MediaAssetType.BMP, ThrowingFunction.unchecked(ia -> ImageProcessor.getProcessor(compress).apply(ia)));
        // Audio
        modification |= processAssets(storyPack, MediaGroup.AUDIO, MediaAssetType.MP3, ThrowingFunction.unchecked(aa -> AudioProcessor.getProcessor(repair, compress).apply(aa)));

        return modification;
    }

    private boolean processAssets(StoryPack storyPack, MediaGroup mg, MediaAssetType targetType,
                                  Function<MediaAsset, byte[]> processor) {
        // Cache prepared assets bytes
        Map<String, byte[]> assets = new ConcurrentHashMap<>();
        List<MediaAsset> medias = storyPack.assets(mg == MediaGroup.IMAGE);
        AtomicBoolean modification = new AtomicBoolean(false);

        try (ProgressBar pb = new ProgressBarBuilder()
                .setInitialMax(medias.size())
                .setUpdateIntervalMillis(100)
                .setTaskName("Converting " + mg)
                .setMaxRenderedLength(125)
                //.setConsumer(new DelegatingProgressBarConsumer(log::info))
                .build()) {
            var mediasStream = parallel ? medias.parallelStream() : medias.stream();
            mediasStream.forEach(StoppingConsumer.stopped(a -> {
                pb.setExtraMessage(a.getName()); // Set extra message to display at the end of the bar

                String assetHash = a.findHash();
                // Update data (converted if needed)
                byte[] rawData = assets.computeIfAbsent(assetHash, s -> processor.apply(a));
                if (!Arrays.equals(rawData, a.getRawData())) {
                    a.setRawData(rawData);
                    // force type
                    a.changeType(targetType);
                    modification.set(true);
                }

                pb.step();
            }));
        }
        // Clean cache
        assets.clear();

        return modification.get();
    }

}
