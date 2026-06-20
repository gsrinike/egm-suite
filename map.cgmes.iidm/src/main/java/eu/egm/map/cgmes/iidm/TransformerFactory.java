package eu.egm.map.cgmes.iidm;

/**
 * Generic factory for creating transformer implementations.
 */
public interface TransformerFactory {
    <T extends Transformer<?>> T createTransformer(Class<T> transformerType);
}
