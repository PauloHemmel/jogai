package br.ufms.jogai.data.firebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

/**
 * Creates an immutable snapshot of the mutable Firestore containers supported by the repositories.
 */
final class FirestoreDataSnapshot {

    private FirestoreDataSnapshot() {
    }

    static Map<String, Object> copy(Map<String, ?> source) {
        return copyMap(source, new IdentityHashMap<>());
    }

    static Object copyValue(Object value) {
        return copyValue(value, new IdentityHashMap<>());
    }

    private static Object copyValue(Object value, IdentityHashMap<Object, Boolean> active) {
        switch (value) {
            case null -> {
                return null;
            }
            case Map<?, ?> map -> {
                return copyMap(map, active);
            }
            case List<?> list -> {
                enter(list, active);
                try {
                    List<Object> copy = new ArrayList<>(list.size());
                    for (Object element : list) copy.add(copyValue(element, active));
                    return Collections.unmodifiableList(copy);
                } finally {
                    active.remove(list);
                }
            }
            case Record record -> {
                return copyRecord(record, active);
            }
            case byte[] bytes -> {
                return bytes.clone();
            }
            case Date date -> {
                return new Date(date.getTime());
            }
            default -> {
                return value;
            }
        }
    }

    private static Object copyRecord(Record record, IdentityHashMap<Object, Boolean> active) {
        enter(record, active);
        try {
            RecordComponent[] components = record.getClass().getRecordComponents();
            Class<?>[] parameterTypes = new Class<?>[components.length];
            Object[] values = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
                Method accessor = components[i].getAccessor();
                if (!accessor.canAccess(record) && !accessor.trySetAccessible()) {
                    throw new IllegalAccessException("Record component accessor is not accessible");
                }
                values[i] = copyValue(accessor.invoke(record), active);
            }
            Constructor<?> constructor = record.getClass().getDeclaredConstructor(parameterTypes);
            if (!constructor.canAccess(null) && !constructor.trySetAccessible()) {
                throw new IllegalAccessException("Record constructor is not accessible");
            }
            return constructor.newInstance(values);
        } catch (ReflectiveOperationException error) {
            Throwable cause = error instanceof InvocationTargetException invocation && invocation.getCause() != null
                    ? invocation.getCause() : error;
            throw new IllegalArgumentException("Could not snapshot Firestore record " + record.getClass().getName(), cause);
        } finally {
            active.remove(record);
        }
    }

    private static Map<String, Object> copyMap(Map<?, ?> source, IdentityHashMap<Object, Boolean> active) {
        enter(source, active);
        try {
            Map<String, Object> copy = new HashMap<>(source.size());
            source.forEach((key, value) -> {
                if (!(key instanceof String field)) {
                    throw new IllegalArgumentException("Firestore map keys must be strings");
                }
                copy.put(field, copyValue(value, active));
            });
            return Collections.unmodifiableMap(copy);
        } finally {
            active.remove(source);
        }
    }

    private static void enter(Object value, IdentityHashMap<Object, Boolean> active) {
        if (active.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Firestore data must not contain cyclic maps, lists, or records");
        }
    }
}
