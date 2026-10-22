package net.vulkanmod.compat.sable.veil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


public final class VeilShaderProgramAccess {
    private static final String WRAPPER = "foundry.veil.impl.client.render.shader.program.ShaderProgramImpl$Wrapper";
    private static final Access ORDINARY = new Access(false, null);
    private static final Access UNAVAILABLE_WRAPPER = new Access(true, null);

    private static final ClassValue<Access> ACCESS = new ClassValue<>() {
        @Override
        protected Access computeValue(Class<?> type) {
            boolean wrapper = false;
            for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
                if (WRAPPER.equals(parent.getName())) {
                    wrapper = true;
                    break;
                }
            }
            if (!wrapper) return ORDINARY;

            try {
                Method program = type.getMethod("program");
                if (Modifier.isStatic(program.getModifiers())) return UNAVAILABLE_WRAPPER;
                Class<?> declaringType = program.getDeclaringClass();
                MethodHandles.Lookup lookup = Modifier.isPublic(declaringType.getModifiers())
                        ? MethodHandles.publicLookup()
                        : MethodHandles.privateLookupIn(declaringType, MethodHandles.lookup());
                MethodHandle owner = lookup.unreflect(program);
                Class<?> programInterface = programInterface(program.getReturnType());
                if (programInterface == null) return UNAVAILABLE_WRAPPER;
                MethodHandle id = MethodHandles.publicLookup().findVirtual(programInterface,
                        "getProgram", MethodType.methodType(int.class));
                MethodHandle reader = MethodHandles.filterReturnValue(owner,
                        id.asType(MethodType.methodType(int.class, program.getReturnType())))
                        .asType(MethodType.methodType(int.class, Object.class));
                return new Access(true, reader);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                return UNAVAILABLE_WRAPPER;
            }
        }
    };

    private VeilShaderProgramAccess() {}

    public static boolean isWrapper(Object shader) {
        return shader != null && ACCESS.get(shader.getClass()).wrapper();
    }

    @SuppressWarnings("removal")
    public static int programId(Object shader) {
        if (shader == null) return 0;
        MethodHandle reader = ACCESS.get(shader.getClass()).reader();
        if (reader == null) return 0;
        try {
            return (int) reader.invokeExact(shader);
        } catch (VirtualMachineError | ThreadDeath fatal) {
            throw fatal;
        } catch (Throwable unavailable) {
            return 0;
        }
    }


    private static Class<?> programInterface(Class<?> type) {
        if (type.isInterface() && Modifier.isPublic(type.getModifiers())) {
            try {
                Method getter = type.getMethod("getProgram");
                if (getter.getReturnType() == int.class && !Modifier.isStatic(getter.getModifiers())) return type;
            } catch (NoSuchMethodException ignored) {

            }
        }
        for (Class<?> parent : type.getInterfaces()) {
            Class<?> candidate = programInterface(parent);
            if (candidate != null) return candidate;
        }
        Class<?> parent = type.getSuperclass();
        return parent == null ? null : programInterface(parent);
    }

    private record Access(boolean wrapper, MethodHandle reader) {}
}
