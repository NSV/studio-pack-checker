package org.studio.checker;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import studio.core.v1.model.StoryPack;

import java.nio.file.Path;

@Data
@RequiredArgsConstructor
@Slf4j
public class CheckResult {

    private final Path path;
    private final CheckResultEnum result;
    private StoryPack storyPack;
    private Exception exception;
    private String message;

    public static CheckResult ok(Path path, StoryPack storyPack) {
        CheckResult checkResult = new CheckResult(path, CheckResultEnum.OK);
        checkResult.setStoryPack(storyPack);
        return checkResult;
    }

    public static CheckResult ko(Path path, Exception exception) {
        CheckResult checkResult = new CheckResult(path, CheckResultEnum.KO);
        checkResult.setException(exception);
        return checkResult;
    }

    public static CheckResult ko(Path path, String message) {
        CheckResult checkResult = new CheckResult(path, CheckResultEnum.KO);
        checkResult.setMessage(message);
        return checkResult;
    }

    public static CheckResult repair(Path path, StoryPack storyPack) {
        CheckResult checkResult = new CheckResult(path, CheckResultEnum.REPAIR);
        checkResult.setStoryPack(storyPack);
        return checkResult;
    }

    public static CheckResult compress(Path path, StoryPack storyPack) {
        CheckResult checkResult = new CheckResult(path, CheckResultEnum.COMPRESS);
        checkResult.setStoryPack(storyPack);
        return checkResult;
    }

    public void showResult() {
        switch (result) {
            case KO:
                if (exception != null) {
                    log.error("KO : " + path.getFileName() + " pack checked with errors", exception);
                } else {
                    log.error("KO : {} pack checked with errors : {}", path.getFileName(), message);
                }
                break;
            case OK:
                log.info("OK : {} pack checked with no error", path.getFileName());
                break;
            case REPAIR:
                log.info("REPAIR : {} pack checked with errors and repaired", path.getFileName());
                break;
            case COMPRESS:
                log.info("REPAIR AND COMPRESS : {} pack checked with errors, repaired and compressed", path.getFileName());
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (storyPack != null) {
            log.info("Title : {}", storyPack.getEnriched().getTitle());
            log.info("Version : {}", storyPack.getVersion());
            log.info("UUID : {}", storyPack.getUuid());
            log.info("Thumbnail : {}", storyPack.getEnriched().getThumbnail() != null);
        }
    }

    public enum CheckResultEnum {

        OK, REPAIR, COMPRESS, KO

    }


}
