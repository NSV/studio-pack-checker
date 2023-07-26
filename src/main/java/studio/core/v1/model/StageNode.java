/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package studio.core.v1.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import studio.core.v1.model.asset.MediaAsset;
import studio.core.v1.model.enriched.EnrichedNodeMetadata;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = { "okTransition", "homeTransition" })
public class StageNode extends Node {

    private Boolean squareOne; // first node only
    private MediaAsset image;
    private MediaAsset audio;
    private Transition okTransition;
    private Transition homeTransition;
    private ControlSettings controlSettings;

    public StageNode(UUID uuid, MediaAsset image, MediaAsset audio, Transition okTransition,
            Transition homeTransition, ControlSettings controlSettings, EnrichedNodeMetadata enriched) {
        super(uuid, enriched);
        this.image = image;
        this.audio = audio;
        this.okTransition = okTransition;
        this.homeTransition = homeTransition;
        this.controlSettings = controlSettings;
    }

    public Stream<MediaAsset> assets() {
        return Stream.of(getImage(), getAudio()).filter(Objects::nonNull);
    }
}
