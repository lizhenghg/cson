package com.cracker.code.cson.internal;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

import static com.cracker.code.cson.internal.CsonPreconditions.checkArgument;


/**
 *
 * CsonTypes: 对泛型的处理
 * @author lizhg<2486479615@qq.com>
 * <br/>=================================
 * <br/>公司：myself
 * <br/>版本：1.1.0
 * <br/>创建时间：2021-03-18
 * <br/>jdk版本：1.8
 * <br/>=================================
 */
public final class CsonTypes {

    static final Type[] EMPTY_TYPE_ARRAY = new Type[] {};

    private CsonTypes() {}

    public static Type canonicalize(Type type) {
        checkArgument(type != null);
        if (type instanceof Class) {
            Class<?> c = (Class<?>) type;
            return c.isArray() ? new GenericArrayTypeImpl(canonicalize(c.getComponentType())) : c;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) type;
            return new ParameterizedTypeImpl(p.getOwnerType(), p.getRawType(), p.getActualTypeArguments());
        } else if (type instanceof GenericArrayType) {
            GenericArrayType g = (GenericArrayType) type;
            return new GenericArrayTypeImpl(g.getGenericComponentType());
        } else if (type instanceof WildcardType) {
            WildcardType w = (WildcardType) type;
            return new WildcardTypeImpl(w.getUpperBounds(), w.getLowerBounds());
        } else {
            // 不处理TypeVariable，因为得到的K,V,T,E...都属于Object
            return type;
        }
    }

    /**
     * 类的引用，简单粗暴，不考虑设计模式
     * 创建一个与JDK1.8一模一样的GenericArrayTypeImpl，改造里面所有方法。
     */
    private static final class GenericArrayTypeImpl implements GenericArrayType, Serializable {
        private final Type componentType;

        public GenericArrayTypeImpl(Type componentType) {
            this.componentType = canonicalize(componentType);
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }

        /**
         * @param src 比较源
         * @return returns true means they are equal.
         */
        @Override
        public boolean equals(Object src) {
            return src instanceof GenericArrayType
                    && CsonTypes.equals(this, (GenericArrayType) src);
        }

        @Override
        public int hashCode() {
            return componentType.hashCode();
        }

        @Override
        public String toString() {
            return typeToString(componentType) + "[]";
        }

        /**
         * 序列化版本控制，需要显式加上
         */
        private static final long serialVersionUID = 1L;

    }


    /**
     * 类的引用，简单粗暴，不考虑设计模式
     * 创建一个与JDK1.8一模一样的ParameterizedTypeImpl，改造里面所有方法。
     */
    private static final class ParameterizedTypeImpl implements ParameterizedType, Serializable {
        private final Type ownerType;
        private final Type rawType;
        // 类型变量
        private final Type[] typeArguments;

        public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
            if (rawType instanceof Class<?>) {
                Class<?> rawTypeAsClass = (Class<?>) rawType;
                // 疑问疑问！如果ownerType为null，那么rawTypeAsClass.getEnclosingClass() == null
                // 成立，但是isStatic是不能为true的！因为任何一个没有包裹内部类的类，都不能使用static修饰
                boolean isStaticOrTopLevelClass = Modifier.isStatic(rawTypeAsClass.getModifiers())
                        || rawTypeAsClass.getEnclosingClass() == null;
                checkArgument(ownerType != null || isStaticOrTopLevelClass);
            }

            this.ownerType = ownerType == null ? null : canonicalize(ownerType);
            this.rawType = canonicalize(rawType);
            this.typeArguments = typeArguments.clone();
            for (int t = 0; t < this.typeArguments.length; t++) {
                CsonPreconditions.checkNotNull(this.typeArguments[t]);
                // 类型变量，instanceof TypeVariable，常见的有T、K、V
                checkNotPrimitive(this.typeArguments[t]);
                this.typeArguments[t] = canonicalize(this.typeArguments[t]);
            }
        }


        @Override
        public Type[] getActualTypeArguments() {
            return typeArguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        /**
         * 位运算
         * 二进制数1 二进制数2 与操作(&) 或操作(|) 异或操作(^)
         * 0 0 0 0 0
         * 0 1 0 1 1
         * 1 0 0 1 1
         * 1 1 1 1 0
         * @return int
         */
        @Override
        public int hashCode() {
            /*
            * int x = 3;
            * int y = 6;
            *
            * System.out.println(x & y); // 2
            * System.out.println(x | y); // 7
            * System.out.println(x ^ y); // 5
            */
            return Arrays.hashCode(typeArguments)
                    ^ rawType.hashCode()
                    ^ hashCodeOrZero(ownerType);

        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ParameterizedType
                    && CsonTypes.equals(this, (ParameterizedType) other);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(30 * (typeArguments.length + 1));
            stringBuilder.append(typeToString(rawType));

            if (typeArguments.length == 0) {
                return stringBuilder.toString();
            }

            stringBuilder.append("<").append(typeToString(typeArguments[0]));
            for (int i = 1; i < typeArguments.length; i++) {
                stringBuilder.append(", ").append(typeToString(typeArguments[i]));
            }
            return stringBuilder.append(">").toString();
        }

        /**
         * 序列化版本控制，需要显式加上
         */
        private static final long serialVersionUID = 1L;
    }


    /**
     * 类的引用，简单粗暴，不考虑设计模式
     * 创建一个与JDK1.8一模一样的WildcardTypeImpl，改造里面所有方法。
     */
    private static final class WildcardTypeImpl implements WildcardType, Serializable {
        private final Type upperBound;
        private final Type lowerBound;

        public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            checkArgument(lowerBounds.length <= 1);
            checkArgument(upperBounds.length == 1);

            if (lowerBounds.length == 1) {
                CsonPreconditions.checkNotNull(lowerBounds[0]);
                checkNotPrimitive(lowerBounds[0]);
                checkArgument(upperBounds[0] == Object.class);
                this.lowerBound = canonicalize(lowerBounds[0]);
                this.upperBound = Object.class;

            } else {
                CsonPreconditions.checkNotNull(upperBounds[0]);
                checkNotPrimitive(upperBounds[0]);
                this.lowerBound = null;
                this.upperBound = canonicalize(upperBounds[0]);
            }
        }

        @Override
        public Type[] getUpperBounds() {
            return new Type[] {upperBound};
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBound != null ? new Type[] {lowerBound} : EMPTY_TYPE_ARRAY;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof WildcardType
                    && CsonTypes.equals(this, (WildcardType) other);
        }

        @Override
        public int hashCode() {
            // this equals Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds());
            return (lowerBound != null ? 31 + lowerBound.hashCode() : 1)
                    ^ (31 + upperBound.hashCode());
        }

        @Override
        public String toString() {
            if (lowerBound != null) {
                return "? super " + typeToString(lowerBound);
            } else if (upperBound == Object.class) {
                // 或者"? extends Object"也行，只是所有类默认都继承了Object，故忽略extends Object
                return "?";
            } else {
                return "? extends " + typeToString(upperBound);
            }
        }

        /**
         * 序列化版本控制，需要显式加上
         */
        private static final long serialVersionUID = 1L;
    }


    /**
     * Returns true if {@code a} and {@code b} are equal.
     * @param a type
     * @param b type
     * @return boolean
     */
    public static boolean equals(Type a, Type b) {
        
        if (a == b) {
            // 同时处理了 (a == null && b == null)
            return true;
        } else if (a == null || b == null) {
            // 其中一个 == null，直接返回false
            return false;
        } else if (a instanceof Class) {
            // Class的equals已存在，直接调用即可，eg: 对于相同的Class<? extends String>，equals永远相等
            return a.equals(b);
        } else if (a instanceof ParameterizedType) {
            // ParameterizedType：参数化类型，如下举例均为参数化类型：
            /*
            * Map<String, Person> map;
            * Set<String> set;
            * Class<?> clazz;
            * List<String> list;
            * static class Holder<V> {}
            * */
            if (!(b instanceof ParameterizedType)) {
                return false;
            }
            
            ParameterizedType pa = (ParameterizedType) a;
            ParameterizedType pb = (ParameterizedType) b;
            return equal(pa.getOwnerType(), pb.getOwnerType())
                    && pa.getRawType().equals(pb.getRawType())
                    && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());
        } else if (a instanceof GenericArrayType) {
            // GenericArrayType：参数化数组类型，如下列举均为参数化数组类型：
            /*
            * List<String>[] sTypeArray
            * T[] vTypeArray
            * */
            if (!(b instanceof GenericArrayType)) {
                return false;
            }
            
            GenericArrayType ga = (GenericArrayType) a;
            GenericArrayType gb = (GenericArrayType) b;
            return equal(ga.getGenericComponentType(), gb.getGenericComponentType());
            
        } else if (a instanceof WildcardType) {
            // WildcardType：通配符类型，如下举例均为通配符类型：
            /*
            * <? super String>：下边界通配符
            * <? extends String>：上边界通配符
            * 
            * 表示包括String和它的父类Object，就是即可以add(null)，也可以add("xxx");在通配符类型中，有上下边界。
            * ps：就算下面的下边界通配符为? super java.lang.Object，一样有上下边界，都是class java.lang.Object
            * 上边界：class java.lang.Object
            * 下边界：class java.lang.String
            * List<? super String> superString;
            * 
            * 
            * 因为String是final类，无法被继承，所以只能包括null，就是只能add(null);在通配符类型中，只有上边界。
            * ps：就算下面的上边界通配符为? extends Object，也只有上边界，为class java.lang.Object
            * 上边界：class java.lang.String
            * List<? extends String> extendsString;
            * 
            * */
            if (!(b instanceof WildcardType)) {
                return false;
            }
            
            WildcardType wa = (WildcardType) a;
            WildcardType wb = (WildcardType) b;
            return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds()) 
                    && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());
            
        } else if (a instanceof TypeVariable) {
            // TypeVariable：类型变量，如下举例均为类型变量：
            /*
            * public class TypeVariableBean<K extends InputStream & Runnable> {}
            * 
            * */
            if (!(b instanceof TypeVariable)) {
                return false;
            }

            TypeVariable<?> ta = (TypeVariable<?>) a;
            TypeVariable<?> tb = (TypeVariable<?>) b;
            return ta.getGenericDeclaration() == tb.getGenericDeclaration() 
                    && ta.getName().equals(tb.getName());
            
        } else {
            // we don't support undefined type
            return false;
        }

    }

    public static Class<?> getRawType(Type type) {

        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Type rawType = parameterized.getRawType();
            checkArgument(rawType instanceof Class);
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            return Object.class;

        } else if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);

        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType or "
                    + "GenericArrayType, but <" + type + "> is of type " + className);
        }
    }


    /**
     * 不能instanceof Class，又或者不能isPrimitive
     * @param type Type
     */
    private static void checkNotPrimitive(Type type) {
        checkArgument(!(type instanceof Class<?>) || !((Class<?>) type).isPrimitive());
    }

    public static String typeToString(Type type) {
        return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
    }

    private static int hashCodeOrZero(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    static boolean equal(Object a, Object b) {
        // Objects.equals(a, b)为jdk1.7开始使用。这里不使用jdk1.8的Objects.equals(a, b);目的是为了兼容低版本jdk。
        return (a == b) || (a != null && a.equals(b));
        
    }

    /**
     * 返回此数组类型的组件类型
     * @param array Type
     * @return Type
     */
    public static Type getArrayComponentType(Type array) {
        return array instanceof GenericArrayType
                ? ((GenericArrayType) array).getGenericComponentType()
                : ((Class<?>) array).getComponentType();
    }


    public static Type resolve(Type context, Class<?> contextRawType, Type toResolve) {
        // this implementation is made a little more complicated in an attempt to avoid object-creation
        while (true) {
            if (toResolve instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) toResolve;
                toResolve = resolveTypeVariable(context, contextRawType, typeVariable);
                if (toResolve == typeVariable) {
                    return toResolve;
                }

            } else if (toResolve instanceof Class && ((Class<?>) toResolve).isArray()) {
                Class<?> original = (Class<?>) toResolve;
                Type componentType = original.getComponentType();
                Type newComponentType = resolve(context, contextRawType, componentType);
                return componentType == newComponentType
                        ? original
                        : arrayOf(newComponentType);

            } else if (toResolve instanceof GenericArrayType) {
                GenericArrayType original = (GenericArrayType) toResolve;
                Type componentType = original.getGenericComponentType();
                Type newComponentType = resolve(context, contextRawType, componentType);
                return componentType == newComponentType
                        ? original
                        : arrayOf(newComponentType);

            } else if (toResolve instanceof ParameterizedType) {
                ParameterizedType original = (ParameterizedType) toResolve;
                Type ownerType = original.getOwnerType();
                Type newOwnerType = resolve(context, contextRawType, ownerType);
                boolean changed = newOwnerType != ownerType;

                Type[] args = original.getActualTypeArguments();
                for (int t = 0, length = args.length; t < length; t++) {
                    Type resolvedTypeArgument = resolve(context, contextRawType, args[t]);
                    if (resolvedTypeArgument != args[t]) {
                        if (!changed) {
                            args = args.clone();
                            changed = true;
                        }
                        args[t] = resolvedTypeArgument;
                    }
                }

                return changed
                        ? newParameterizedTypeWithOwner(newOwnerType, original.getRawType(), args)
                        : original;

            } else if (toResolve instanceof WildcardType) {
                WildcardType original = (WildcardType) toResolve;
                Type[] originalLowerBound = original.getLowerBounds();
                Type[] originalUpperBound = original.getUpperBounds();

                if (originalLowerBound.length == 1) {
                    Type lowerBound = resolve(context, contextRawType, originalLowerBound[0]);
                    if (lowerBound != originalLowerBound[0]) {
                        return supertypeOf(lowerBound);
                    }
                } else if (originalUpperBound.length == 1) {
                    Type upperBound = resolve(context, contextRawType, originalUpperBound[0]);
                    if (upperBound != originalUpperBound[0]) {
                        return subtypeOf(upperBound);
                    }
                }
                return original;

            } else {
                return toResolve;
            }
        }
    }



    private static Class<?> declaringClassOf(TypeVariable<?> typeVariable) {
        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
        return genericDeclaration instanceof Class
                ? (Class<?>) genericDeclaration
                : null;
    }

    static Type resolveTypeVariable(Type context, Class<?> contextRawType, TypeVariable<?> unknown) {
        Class<?> declaredByRaw = declaringClassOf(unknown);

        // we can't reduce this further
        if (declaredByRaw == null) {
            return unknown;
        }

        Type declaredBy = getGenericSupertype(context, contextRawType, declaredByRaw);
        if (declaredBy instanceof ParameterizedType) {
            int index = indexOf(declaredByRaw.getTypeParameters(), unknown);
            return ((ParameterizedType) declaredBy).getActualTypeArguments()[index];
        }

        return unknown;
    }

    static Type getGenericSupertype(Type context, Class<?> rawType, Class<?> toResolve) {
        if (toResolve == rawType) {
            return context;
        }

        // we skip searching through interfaces if unknown is an interface
        if (toResolve.isInterface()) {
            Class<?>[] interfaces = rawType.getInterfaces();
            for (int i = 0, length = interfaces.length; i < length; i++) {
                if (interfaces[i] == toResolve) {
                    return rawType.getGenericInterfaces()[i];
                } else if (toResolve.isAssignableFrom(interfaces[i])) {
                    return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
                }
            }
        }

        // check our supertypes
        if (!rawType.isInterface()) {
            while (rawType != Object.class) {
                Class<?> rawSupertype = rawType.getSuperclass();
                if (rawSupertype == toResolve) {
                    return rawType.getGenericSuperclass();
                } else if (toResolve.isAssignableFrom(rawSupertype)) {
                    return getGenericSupertype(rawType.getGenericSuperclass(), rawSupertype, toResolve);
                }
                rawType = rawSupertype;
            }
        }

        // we can't resolve this further
        return toResolve;
    }

    private static int indexOf(Object[] array, Object toFind) {
        for (int i = 0; i < array.length; i++) {
            if (toFind.equals(array[i])) {
                return i;
            }
        }
        throw new NoSuchElementException();
    }

    public static WildcardType supertypeOf(Type bound) {
        return new WildcardTypeImpl(new Type[] { Object.class }, new Type[] { bound });
    }

    public static WildcardType subtypeOf(Type bound) {
        return new WildcardTypeImpl(new Type[] { bound }, EMPTY_TYPE_ARRAY);
    }

    public static GenericArrayType arrayOf(Type componentType) {
        return new GenericArrayTypeImpl(componentType);
    }

    public static ParameterizedType newParameterizedTypeWithOwner(
            Type ownerType, Type rawType, Type... typeArguments) {
        return new ParameterizedTypeImpl(ownerType, rawType, typeArguments);
    }

    public static Type getCollectionElementType(Type context, Class<?> contextRawType) {
        Type collectionType = getSupertype(context, contextRawType, Collection.class);

        if (collectionType instanceof WildcardType) {
            collectionType = ((WildcardType)collectionType).getUpperBounds()[0];
        }
        if (collectionType instanceof ParameterizedType) {
            return ((ParameterizedType) collectionType).getActualTypeArguments()[0];
        }
        return Object.class;
    }

    static Type getSupertype(Type context, Class<?> contextRawType, Class<?> supertype) {
        checkArgument(supertype.isAssignableFrom(contextRawType));
        return resolve(context, contextRawType,
                CsonTypes.getGenericSupertype(context, contextRawType, supertype));
    }

    public static Type[] getMapKeyAndValueTypes(Type context, Class<?> contextRawType) {
        if (context == Properties.class) {
            return new Type[] { String.class, String.class }; // TODO: test subclasses of Properties!
        }

        Type mapType = getSupertype(context, contextRawType, Map.class);
        // TODO: strip wildcards?
        if (mapType instanceof ParameterizedType) {
            ParameterizedType mapParameterizedType = (ParameterizedType) mapType;
            return mapParameterizedType.getActualTypeArguments();
        }
        return new Type[] { Object.class, Object.class };
    }
}