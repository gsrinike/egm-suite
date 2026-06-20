package eu.egm.com.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link MappingService} implementation.
 *
 * It supports Java records through their canonical constructor and simple
 * mutable classes through a no-argument constructor plus field assignment. This
 * keeps mapping configuration useful for both DTO styles without introducing a
 * heavy mapping framework dependency.
 */
public class ReflectionMappingService implements MappingService {
    @Override
    public <S, T> T map(S source, Class<T> targetType, MappingDefinition definition) {
        validate(source, targetType, definition);
        if (targetType.isRecord()) {
            return mapRecord(source, targetType, definition);
        }
        return mapMutableObject(source, targetType, definition);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, T> T map(S source, T target, MappingDefinition definition) {
        if (target == null) {
            throw new MappingException("target instance is required");
        }
        return (T) map(source, target.getClass(), definition);
    }

    private <S, T> T mapRecord(S source, Class<T> targetType, MappingDefinition definition) {
        try {
            RecordComponent[] components = targetType.getRecordComponents();
            Class<?>[] parameterTypes = Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new);
            Object[] values = new Object[components.length];
            for (int index = 0; index < components.length; index++) {
                RecordComponent component = components[index];
                Optional<FieldMapping> mapping = fieldForTarget(definition, component.getName());
                if (mapping.isPresent()) {
                    Object rawValue = readMappedValue(source, mapping.get());
                    values[index] = convert(rawValue, component.getType(), mapping.get());
                } else {
                    values[index] = primitiveDefault(component.getType());
                }
            }
            Constructor<T> constructor = targetType.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(values);
        } catch (ReflectiveOperationException exception) {
            throw new MappingException("Unable to map " + definition.name() + " to record " + targetType.getName(), exception);
        }
    }

    private <S, T> T mapMutableObject(S source, Class<T> targetType, MappingDefinition definition) {
        try {
            Constructor<T> constructor = targetType.getDeclaredConstructor();
            constructor.setAccessible(true);
            T target = constructor.newInstance();
            for (FieldMapping mapping : definition.fields()) {
                Field field = findField(targetType, mapping.targetPath())
                        .orElseThrow(() -> new MappingException("No target field " + mapping.targetPath()));
                if (Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                field.set(target, convert(readMappedValue(source, mapping), field.getType(), mapping));
            }
            return target;
        } catch (ReflectiveOperationException exception) {
            throw new MappingException("Unable to map " + definition.name() + " to " + targetType.getName(), exception);
        }
    }

    private Optional<FieldMapping> fieldForTarget(MappingDefinition definition, String targetName) {
        return definition.fields().stream()
                .filter(field -> targetName.equals(field.targetPath()))
                .findFirst();
    }

    private Object readMappedValue(Object source, FieldMapping mapping) {
        Object value = readPath(source, mapping.sourcePath());
        if (value == null || mapping.valueMappings().isEmpty()) {
            return value;
        }
        String key = value instanceof Enum<?> enumValue ? enumValue.name() : String.valueOf(value);
        return mapping.valueMappings().getOrDefault(key, key);
    }

    private Object readPath(Object source, String path) {
        Object current = source;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = readProperty(current, segment);
        }
        return current;
    }

    private Object readProperty(Object source, String name) {
        Class<?> type = source.getClass();
        try {
            Method accessor = type.getMethod(name);
            return accessor.invoke(source);
        } catch (ReflectiveOperationException ignored) {
            // Fall through to JavaBean getter and field access.
        }
        String getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            Method getter = type.getMethod(getterName);
            return getter.invoke(source);
        } catch (ReflectiveOperationException ignored) {
            // Fall through to direct field access.
        }
        return findField(type, name)
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(source);
                    } catch (IllegalAccessException exception) {
                        throw new MappingException("Unable to read field " + name, exception);
                    }
                })
                .orElseThrow(() -> new MappingException("No source property " + name + " on " + type.getName()));
    }

    private Optional<Field> findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return Optional.of(current.getDeclaredField(name));
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convert(Object value, Class<?> targetType, FieldMapping mapping) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                return primitiveDefault(targetType);
            }
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType, String.valueOf(value));
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if ((targetType == double.class || targetType == Double.class) && value instanceof Number number) {
            return number.doubleValue();
        }
        if ((targetType == int.class || targetType == Integer.class) && value instanceof Number number) {
            return number.intValue();
        }
        if ((targetType == long.class || targetType == Long.class) && value instanceof Number number) {
            return number.longValue();
        }
        if (Map.class.isAssignableFrom(targetType) && value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>(map);
        }
        throw new MappingException("Cannot map " + mapping.sourcePath() + " value to " + targetType.getName());
    }

    private Object primitiveDefault(Class<?> targetType) {
        if (!targetType.isPrimitive()) {
            return null;
        }
        if (targetType == boolean.class) {
            return false;
        }
        if (targetType == char.class) {
            return '\0';
        }
        return 0;
    }

    private void validate(Object source, Class<?> targetType, MappingDefinition definition) {
        if (source == null) {
            throw new MappingException("source is required");
        }
        if (targetType == null) {
            throw new MappingException("targetType is required");
        }
        if (definition == null) {
            throw new MappingException("mapping definition is required");
        }
        if (!definition.sourceType().isInstance(source)) {
            throw new MappingException("Source is " + source.getClass().getName() + " but mapping expects " + definition.sourceType().getName());
        }
        if (!targetType.equals(definition.targetType())) {
            throw new MappingException("Target is " + targetType.getName() + " but mapping expects " + definition.targetType().getName());
        }
    }
}
