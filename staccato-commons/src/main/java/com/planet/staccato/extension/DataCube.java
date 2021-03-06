package com.planet.staccato.extension;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collection;
import java.util.Map;

/**
 * Defines the fields and Jackson property values for fields in the data cube extension.
 * @see <a href="https://github.com/radiantearth/stac-spec/tree/master/extensions/datacube">Data Cube Extension</a>
 * @author joshfix
 * Created on 2019-05-15
 */
public interface DataCube {

    String EXTENSION_PREFIX = "cube";

    @JsonProperty(EXTENSION_PREFIX + ":dimensions")
    Map<String, ? extends Dimension> getDimensions();
    void setDimensions(Map<String, ? extends Dimension> dimensions);

    @Data
    abstract class Dimension {
        private String type;
        private Collection extent;
        private Collection values;
    }

    @Data
    class HorizontalSpatialDimension extends Dimension {
        private String axis;
        private Double step;
        @JsonProperty("reference_system")
        private Object referenceSystem;
    }

    @Data
    class VerticalSpatialDimension extends Dimension {
        private String axis;
        private Double step;
        private String unit;
        @JsonProperty("reference_system")
        private Object referenceSystem;
    }

    @Data
    class TemporalDimension extends Dimension {
        private String step;
    }

    @Data
    class AdditionalDimension extends Dimension {
        private Double step;
        private String unit;
        @JsonProperty("reference_system")
        private String referenceSystem;
    }

}
