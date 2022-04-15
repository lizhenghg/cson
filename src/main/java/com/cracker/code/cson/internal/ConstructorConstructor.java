package com.cracker.code.cson.internal;

import com.cracker.code.cson.InstanceCreator;
import com.cracker.code.cson.JsonIoException;
import com.cracker.code.cson.reflect.TypeToken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 *
 * 自定义构造器，支持任意的class，均可通过class获取到instance
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-04-03
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public class ConstructorConstructor {

    private final Map<Type, InstanceCreator<?>> instanceCreators;

    public ConstructorConstructor(Map<Type, InstanceCreator<?>> instanceCreators) {
        this.instanceCreators = instanceCreators;
    }

    public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
        Type type = typeToken.getType();
        Class<? super T> rawType = typeToken.getRawType();

        @SuppressWarnings("unchecked")
        InstanceCreator<T> typeCreator = (InstanceCreator<T>) instanceCreators.get(type);
        if (typeCreator != null) {
            return new ObjectConstructor<T>() {
                @Override
                public T construct() {
                    return typeCreator.createInstance(type);
                }
            };
        }

        @SuppressWarnings("unchecked")
        InstanceCreator<T> rawTypeCreator = (InstanceCreator<T>) instanceCreators.get(rawType);
        if (rawTypeCreator != null) {
            return new ObjectConstructor<T>() {
                @Override
                public T construct() {
                    return rawTypeCreator.createInstance(type);
                }
            };
        }

        ObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType);
        if (defaultConstructor != null) {
            return defaultConstructor;
        }

        ObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(type, rawType);
        if (defaultImplementation != null) {
            return defaultImplementation;
        }

        return newUnsafeAllocator(type, rawType);
    }

    private <T> ObjectConstructor<T> newDefaultConstructor(Class<? super T> rawType) {

        try {
            Constructor<? super T> constructor = rawType.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return new ObjectConstructor<T>() {
                @Override
                @SuppressWarnings("unchecked")
                public T construct() {
                    try {
                        return (T) constructor.newInstance((Object) null);
                    } catch (InstantiationException e) {
                        throw new RuntimeException("Failed to invoke " + constructor + " with no args", e);
                    } catch (InvocationTargetException  e) {
                        throw new RuntimeException("Failed to invoke " + constructor + " with no args",
                                e.getTargetException());
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                }
            };
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectConstructor<T> newDefaultImplementationConstructor(
            final Type type, Class<? super T> rawType) {
        if (Collection.class.isAssignableFrom(rawType)) {
            if (SortedSet.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new TreeSet<Object>();
                    }
                };
            } else if (EnumSet.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    @SuppressWarnings("rawtypes")
                    public T construct() {
                        if (type instanceof ParameterizedType) {
                            Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                            if (elementType instanceof Class) {
                                return (T) EnumSet.noneOf((Class) elementType);
                            } else {
                                throw new JsonIoException("Invalid EnumSet type: " + type.toString());
                            }
                        } else {
                            throw new JsonIoException("Invalid EnumSet type: " + type.toString());
                        }
                    }
                };
            } else if (Set.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new LinkedHashSet<Object>();
                    }
                };
            } else if (Queue.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new LinkedList<Object>();
                    }
                };
            } else {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new ArrayList<Object>();
                    }
                };
            }
        }

        if (Map.class.isAssignableFrom(rawType)) {
            if (SortedMap.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new TreeMap<Object, Object>();
                    }
                };
            } else if (type instanceof ParameterizedType && !(String.class.isAssignableFrom(
                    TypeToken.get(((ParameterizedType) type).getActualTypeArguments()[0]).getRawType()))) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new LinkedHashMap<Object, Object>();
                    }
                };
            } else {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new HashMap<>();
                    }
                };
            }
        }

        return null;
    }

    private <T> ObjectConstructor<T> newUnsafeAllocator(final Type type, final Class<? super T> rawType) {
        // 匿名类有时候使用起来很方便好用
        return new ObjectConstructor<T>() {

            private final UnsafeAllocator unsafeAllocator = UnsafeAllocator.create();

            @Override
            @SuppressWarnings("unchecked")
            public T construct() {
                try {
                    Object newInstance = unsafeAllocator.newInstance(rawType);
                    return (T) newInstance;
                } catch (Exception exception) {
                    throw new RuntimeException("Unable to invoke no-args constructor for " + type + ". "
                    + "Register an InstanceCreator with Cson for this type may fix this problem.", exception);
                }
            }
        };
    }

    @Override
    public String toString() {
        return this.instanceCreators.toString();
    }

}
