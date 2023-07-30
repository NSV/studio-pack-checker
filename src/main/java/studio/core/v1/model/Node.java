/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package studio.core.v1.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import studio.core.v1.model.enriched.EnrichedNodeMetadata;

import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uuid")
@Getter
@Setter
@EqualsAndHashCode(exclude = "enriched")
@NoArgsConstructor
public abstract class Node {

    @JsonAlias("id")
    private UUID uuid;

    @JsonUnwrapped
    private EnrichedNodeMetadata enriched;

    protected Node(UUID uuid, EnrichedNodeMetadata enriched) {
        this.uuid = uuid;
        this.enriched = enriched;
    }

    public UUID getId() {
        return uuid;
    }
}
