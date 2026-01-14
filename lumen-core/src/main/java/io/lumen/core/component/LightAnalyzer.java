package io.lumen.core.component;

/**
 * Interface for analyzing a LightDefinition and producing LightMetadata.
 * This is the extension point for annotation-based metadata extraction.
 */
public interface LightAnalyzer {
    /**
     * Analyze a light definition and produce metadata about how to create it.
     *
     * @param definition the light definition to analyze
     * @return metadata containing constructor, dependencies, etc.
     */
    LightMetadata analyze(LightDefinition definition);
}
