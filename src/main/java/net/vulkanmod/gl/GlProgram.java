package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.compat.gl.GlIntegerState;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.shader.parser.LevititeShaderConverter;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

public final class GlProgram {
    private static int nextShaderId = 1;
    private static int nextProgramId = 1;
    private static int nextUniformLocation = 1;

    private static final Int2ReferenceOpenHashMap<Shader> SHADERS = new Int2ReferenceOpenHashMap<>();
    private static final Int2ReferenceOpenHashMap<Program> PROGRAMS = new Int2ReferenceOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<UniformLocation> UNIFORM_LOCATIONS = new Int2ObjectOpenHashMap<>();

    private static int currentProgramId;
    private static final CopyOnWriteArrayList<IntConsumer> LIFECYCLE_LISTENERS = new CopyOnWriteArrayList<>();

    public static AutoCloseable addLifecycleListener(IntConsumer listener) {
        java.util.Objects.requireNonNull(listener, "listener");
        IntConsumer subscription = listener::accept;
        LIFECYCLE_LISTENERS.add(subscription);
        return () -> LIFECYCLE_LISTENERS.remove(subscription);
    }

    private static void notifyLifecycleListeners(int program) {
        RuntimeException failure = null;
        for (IntConsumer listener : LIFECYCLE_LISTENERS) {
            try { listener.accept(program); }
            catch (RuntimeException error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    private GlProgram() {
    }

    public static int createShader(int type) {
        int id = nextShaderId++;
        SHADERS.put(id, new Shader(type));
        return id;
    }

    public static void shaderSource(int shader, CharSequence source) {
        Shader shaderObj = SHADERS.get(shader);
        if (shaderObj != null) {
            shaderObj.source = source != null ? source.toString() : "";
        }
    }

    public static void compileShader(int shader) {
        Shader shaderObj = SHADERS.get(shader);
        if (shaderObj != null) {
            shaderObj.compiledSource = shaderObj.source;
            shaderObj.compiled = true;
        }
    }

    public static int getShaderi(int shader, int pname) {
        return GL20.GL_TRUE;
    }

    public static String getShaderInfoLog(int shader) {
        return "";
    }

    public static void deleteShader(int shader) {
        Shader owner = SHADERS.get(shader);
        if (owner == null) return;
        owner.deleteRequested = true;
        if (owner.attachmentCount == 0) SHADERS.remove(shader);
    }

    public static boolean isShader(int shader) {
        return SHADERS.containsKey(shader);
    }

    public static int createProgram() {
        int id = nextProgramId++;
        PROGRAMS.put(id, new Program());
        return id;
    }

    public static void attachShader(int program, int shader) {
        Program programObj = PROGRAMS.get(program);
        Shader source = SHADERS.get(shader);
        if (programObj != null && source != null && !programObj.attachedShaders.containsKey(shader)) {
            programObj.attachedShaders.put(shader, source);
            source.attachmentCount++;
        }
    }

    public static void detachShader(int program, int shader) {
        Program owner = PROGRAMS.get(program);
        if (owner != null) {
            Shader detached = owner.attachedShaders.remove(shader);
            if (detached != null) releaseShader(shader, detached);
        }
    }

    public static LinkedProgram linkedProgram(int program) {
        Program owner = PROGRAMS.get(program);
        return owner != null ? owner.linkedProgram : null;
    }

                                                                                                  
    public record ShaderSource(int shaderId, int type, String source, boolean compiled) {}

                                                                                             
    public record LinkedProgram(long generation, List<ShaderSource> shaders,
                                Map<String, Integer> attributeBindings) {
        public LinkedProgram {
            shaders = List.copyOf(shaders);
            attributeBindings = Map.copyOf(attributeBindings);
        }
    }

    public static void bindAttribLocation(int program, int index, CharSequence name) {
        Program programObj = PROGRAMS.get(program);
        if (programObj != null && name != null) {
            programObj.explicitAttributeBindings.put(name.toString(), index);
            programObj.attributeLocations.put(name.toString(), index);
        }
    }

    public static void linkProgram(int program) {
        Program programObj = PROGRAMS.get(program);
        if (programObj != null) {
            int[] ids = programObj.attachedShaders.keySet().toIntArray();
            Arrays.sort(ids);
            List<ShaderSource> sources = new ArrayList<>(ids.length);
            for (int id : ids) {
                Shader shader = programObj.attachedShaders.get(id);
                sources.add(new ShaderSource(id, shader.type, shader.compiledSource, shader.compiled));
            }
            LinkedProgram captured = new LinkedProgram(programObj.linkGeneration + 1, sources,
                    programObj.explicitAttributeBindings);
            LevititeProgramReflection reflection = LevititeProgramReflection.tryCreate(captured);
            clearUniforms(programObj);
            programObj.linkGeneration = captured.generation();
            programObj.linkedProgram = captured;
            programObj.linked = true;
            programObj.reflection = reflection;
            initializeReflectedUniforms(programObj);
            notifyLifecycleListeners(program);
        }
    }

    public static int getProgrami(int program, int pname) {
        Program owner = PROGRAMS.get(program);
        LevititeProgramReflection reflection = owner != null ? owner.reflection : null;
        if (reflection != null) {
            switch (pname) {
                case GL20.GL_ACTIVE_UNIFORMS: return reflection.uniforms().size();
                case GL20.GL_ACTIVE_ATTRIBUTES: return reflection.attributes().size();
                case GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH: return reflection.uniformMaxNameLength();
                case GL20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH: return reflection.attributeMaxNameLength();
            }
        }
        return switch (pname) {

            case GL20.GL_ACTIVE_UNIFORMS, GL20.GL_ACTIVE_ATTRIBUTES,
                    GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH, GL20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH,
                    GL20.GL_INFO_LOG_LENGTH, GL20.GL_ATTACHED_SHADERS,
                    GL_ACTIVE_UNIFORM_BLOCKS, GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH,
                    GL_PROGRAM_BINARY_LENGTH -> 0;
            case GL20.GL_DELETE_STATUS -> GL20.GL_FALSE;

            default -> GL20.GL_TRUE;
        };
    }

    private static final int GL_ACTIVE_UNIFORM_BLOCKS = 0x8A36;
    private static final int GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH = 0x8A35;
    private static final int GL_PROGRAM_BINARY_LENGTH = 0x8741;

    public static String getProgramInfoLog(int program) {
        return "";
    }

                                                                                       
    public record ActiveVariable(String name, int size, int type) {}

    public static ActiveVariable activeUniform(int program, int index) {
        Program owner = PROGRAMS.get(program);
        return owner != null && index >= 0 && index < owner.activeUniforms.size()
                ? owner.activeUniforms.get(index) : null;
    }

    public static ActiveVariable activeAttribute(int program, int index) {
        Program owner = PROGRAMS.get(program);
        return owner != null && index >= 0 && index < owner.activeAttributes.size()
                ? owner.activeAttributes.get(index) : null;
    }

    public static LevititeShaderConverter.Result convertedProgram(int program) {
        Program owner = PROGRAMS.get(program);
        return owner != null && owner.reflection != null ? owner.reflection.conversion() : null;
    }

    public static void useProgram(int program) {
        currentProgramId = isProgram(program) ? program : 0;
    }

    public static boolean isProgram(int program) {
        return program == 0 || PROGRAMS.containsKey(program);
    }

    public static void deleteProgram(int program) {
        Program removed = PROGRAMS.remove(program);
        if (removed != null) {
            for (var entry : removed.attachedShaders.int2ReferenceEntrySet()) {
                releaseShader(entry.getIntKey(), entry.getValue());
            }
            removed.attachedShaders.clear();
            clearUniforms(removed);
        }
        if (currentProgramId == program) {
            currentProgramId = 0;
        }
        if (removed != null) {
            notifyLifecycleListeners(program);
        }
    }

    public static int currentProgramId() {
        return currentProgramId;
    }

    public static int getAttribLocation(int program, CharSequence name) {
        Program programObj = PROGRAMS.get(program);
        if (programObj == null || name == null) {
            return -1;
        }

        if (programObj.reflection != null) {
            return programObj.reflectedAttributeLocations.getOrDefault(name.toString(), -1);
        }

        return programObj.attributeLocations.computeIfAbsent(name.toString(), ignored -> {
            String lowerName = name.toString().toLowerCase(Locale.ROOT);
            if (lowerName.contains("position")) {
                return 0;
            }
            if (lowerName.contains("color")) {
                return 1;
            }
            return programObj.attributeLocations.size();
        });
    }

    public static int getUniformLocation(int program, CharSequence name) {
        Program programObj = PROGRAMS.get(program);
        if (programObj == null || name == null) {
            return -1;
        }

        if (programObj.reflection != null) {
            return programObj.uniformLocations.getOrDefault(name.toString(), -1);
        }

        return programObj.uniformLocations.computeIfAbsent(name.toString(), uniformName -> {
            int location = nextUniformLocation++;
            UNIFORM_LOCATIONS.put(location, new UniformLocation(programObj, uniformName));
            return location;
        });
    }

    public static void uniform1i(int location, int value) {
        UniformLocation target = prepareWrite(location, GL20.GL_INT, 1, 1);
        if (target != null) {
            if (target.type == GL20.GL_SAMPLER_2D && (value < 0
                    || value >= GlIntegerState.getInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS))) return;
            target.value.floats[target.offset] = value;
            target.value.ints[target.offset] = value;
            target.value.syncMapped();
        }
    }

    public static void uniform1f(int location, float value) {
        UniformLocation target = prepareWrite(location, GL20.GL_FLOAT, 1, 1);
        if (target != null) {
            target.value.floats[target.offset] = value;
            target.value.syncMapped();
        }
    }

    public static void uniform2f(int location, float x, float y) {
        UniformLocation target = prepareWrite(location, GL20.GL_FLOAT_VEC2, 2, 1);
        if (target != null) {
            target.value.floats[target.offset] = x;
            target.value.floats[target.offset + 1] = y;
            target.value.syncMapped();
        }
    }

    public static void uniform3f(int location, float x, float y, float z) {
        UniformLocation target = prepareWrite(location, GL20.GL_FLOAT_VEC3, 3, 1);
        if (target != null) {
            target.value.floats[target.offset] = x;
            target.value.floats[target.offset + 1] = y;
            target.value.floats[target.offset + 2] = z;
            target.value.syncMapped();
        }
    }

    public static void uniform3i(int location, int x, int y, int z) {
        UniformLocation target = prepareWrite(location, GL20.GL_INT_VEC3, 3, 1);
        if (target != null) {
            target.value.floats[target.offset] = x;
            target.value.floats[target.offset + 1] = y;
            target.value.floats[target.offset + 2] = z;
            target.value.ints[target.offset] = x;
            target.value.ints[target.offset + 1] = y;
            target.value.ints[target.offset + 2] = z;
            target.value.syncMapped();
        }
    }

    public static void uniform4f(int location, float x, float y, float z, float w) {
        UniformLocation target = prepareWrite(location, GL20.GL_FLOAT_VEC4, 4, 1);
        if (target != null) {
            target.value.floats[target.offset] = x;
            target.value.floats[target.offset + 1] = y;
            target.value.floats[target.offset + 2] = z;
            target.value.floats[target.offset + 3] = w;
            target.value.syncMapped();
        }
    }

    public static void uniform1fv(int location, FloatBuffer value) {
        if (value == null || !value.hasRemaining()) return;
        UniformLocation target = prepareWrite(location, GL20.GL_FLOAT, 1, value.remaining());
        if (target == null) return;
        value.get(value.position(), target.value.floats, target.offset, target.writeCount(value.remaining()));
        target.value.syncMapped();
    }

    public static void uniform1fv(int location, float[] value) {
        if (value == null || value.length == 0) return;
        UniformLocation target = prepareWrite(location, GL20.GL_FLOAT, 1, value.length);
        if (target != null) {
            System.arraycopy(value, 0, target.value.floats, target.offset, target.writeCount(value.length));
            target.value.syncMapped();
        }
    }

    public static void uniform1fv(int location, int count, long value) {
        if (value == 0 || count <= 0) return;
        UniformLocation target = prepareWrite(location, GL20.GL_FLOAT, 1, count);
        if (target == null) return;
        int writableCount = target.writeCount(count);
        for (int i = 0; i < writableCount; i++) {
            target.value.floats[target.offset + i] = MemoryUtil.memGetFloat(value + i * (long) Float.BYTES);
        }
        target.value.syncMapped();
    }

    public static void uniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        uniformMatrix(location, transpose, 3, value);
    }

    public static void uniformMatrix3fv(int location, boolean transpose, float[] value) {
        uniformMatrix(location, transpose, 3, value);
    }

    public static void uniformMatrix3fv(int location, int count, boolean transpose, long value) {
        uniformMatrix(location, transpose, 3, count, value);
    }

    public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        uniformMatrix(location, transpose, 4, value);
    }

    public static void uniformMatrix4fv(int location, boolean transpose, float[] value) {
        uniformMatrix(location, transpose, 4, value);
    }

    public static void uniformMatrix4fv(int location, int count, boolean transpose, long value) {
        uniformMatrix(location, transpose, 4, count, value);
    }

    private static void uniformMatrix(int location, boolean transpose, int dimension, FloatBuffer value) {
        if (value == null) return;
        int width = dimension * dimension;
        int floatCount = value.remaining() / width * width;
        if (floatCount == 0) return;
        UniformLocation target = prepareWrite(location, matrixType(dimension), width, floatCount / width);
        if (target == null) return;
        floatCount = target.writeCount(floatCount / width) * width;
        value.get(value.position(), target.value.floats, target.offset, floatCount);
        if (transpose) transposeMatrices(target.value.floats, target.offset, dimension, floatCount);
        target.value.syncMapped();
    }

    private static void uniformMatrix(int location, boolean transpose, int dimension, float[] value) {
        if (value == null) return;
        int width = dimension * dimension;
        int floatCount = value.length / width * width;
        if (floatCount == 0) return;
        UniformLocation target = prepareWrite(location, matrixType(dimension), width, floatCount / width);
        if (target == null) return;
        floatCount = target.writeCount(floatCount / width) * width;
        System.arraycopy(value, 0, target.value.floats, target.offset, floatCount);
        if (transpose) transposeMatrices(target.value.floats, target.offset, dimension, floatCount);
        target.value.syncMapped();
    }

    private static void uniformMatrix(int location, boolean transpose, int dimension, int count, long value) {
        int width = dimension * dimension;
        if (value == 0 || count <= 0 || count > Integer.MAX_VALUE / width) return;
        int floatCount = count * width;
        UniformLocation target = prepareWrite(location, matrixType(dimension), width, count);
        if (target == null) return;
        floatCount = target.writeCount(count) * width;
        for (int i = 0; i < floatCount; i++) {
            target.value.floats[target.offset + i] = MemoryUtil.memGetFloat(value + i * (long) Float.BYTES);
        }
        if (transpose) transposeMatrices(target.value.floats, target.offset, dimension, floatCount);
        target.value.syncMapped();
    }

    private static int matrixType(int dimension) {
        return dimension == 3 ? GL20.GL_FLOAT_MAT3 : GL20.GL_FLOAT_MAT4;
    }

    private static void transposeMatrices(float[] values, int offset, int dimension, int floatCount) {
        int width = dimension * dimension;
                                                                                        
        for (int base = offset; base < offset + floatCount; base += width) {
            for (int column = 0; column < dimension; column++) {
                for (int row = column + 1; row < dimension; row++) {
                    int first = base + column * dimension + row;
                    int second = base + row * dimension + column;
                    float saved = values[first];
                    values[first] = values[second];
                    values[second] = saved;
                }
            }
        }
    }

    public static float[] getFloatUniform(String name) {
        Program programObj = PROGRAMS.get(currentProgramId);
        if (programObj == null) {
            return null;
        }

        UniformValue value = programObj.uniformValues.get(name);
        if (value == null && programObj.reflection != null) {
            UniformLocation alias = ownedLocation(programObj, name);
            return alias != null ? Arrays.copyOfRange(alias.value.floats, alias.offset, alias.offset + alias.components) : null;
        }
        return value != null ? value.floats : null;
    }

    public static int[] getIntUniform(String name) {
        Program programObj = PROGRAMS.get(currentProgramId);
        if (programObj == null) {
            return null;
        }

        UniformValue value = programObj.uniformValues.get(name);
        if (value == null && programObj.reflection != null) {
            UniformLocation alias = ownedLocation(programObj, name);
            return alias != null && alias.value.ints != null
                    ? Arrays.copyOfRange(alias.value.ints, alias.offset, alias.offset + alias.components) : null;
        }
        return value != null ? value.ints : null;
    }

    public static MappedBuffer uniformBuffer(int program, String name) {
        Program owner = PROGRAMS.get(program);
        if (owner == null || !owner.linked || name == null) return null;
        UniformValue value = owner.uniformValues.get(name);
        if (value == null && owner.reflection != null) {
            UniformLocation alias = ownedLocation(owner, name);
            if (alias != null) value = alias.value;
        }
                                                                                 
                                                                                   
        return value != null ? value.mappedBuffer() : null;
    }

    private static UniformLocation prepareWrite(int location, int type, int components, int count) {
        if (count <= 0 || count > Integer.MAX_VALUE / components) return null;
        UniformLocation uniformLocation = UNIFORM_LOCATIONS.get(location);
        Program programObj = PROGRAMS.get(currentProgramId);
        if (uniformLocation == null || programObj == null || uniformLocation.owner != programObj) {
            return null;
        }

        if (uniformLocation.type != 0) {
            boolean matches = uniformLocation.type == type
                    || type == GL20.GL_INT && uniformLocation.type == GL20.GL_SAMPLER_2D;
            if (!matches || uniformLocation.components != components
                    || uniformLocation.arraySize == 1 && count != 1) return null;
            return uniformLocation;
        }

        int floatCount = components * count;
        int intCount = type == GL20.GL_INT || type == GL20.GL_INT_VEC3 ? floatCount : 0;
        String name = uniformLocation.name;
        UniformValue value = programObj.uniformValues.get(name);
        if (value == null || !hasCapacity(value, floatCount, intCount)) {
            value = new UniformValue(new float[floatCount], intCount > 0 ? new int[intCount] : null);
            programObj.uniformValues.put(name, value);
        }

        uniformLocation.value = value;
        uniformLocation.components = components;
        return uniformLocation;
    }

    private static UniformLocation ownedLocation(Program owner, String name) {
        Integer location = owner.uniformLocations.get(name);
        return location != null ? UNIFORM_LOCATIONS.get(location.intValue()) : null;
    }

    private static void initializeReflectedUniforms(Program owner) {
        owner.activeUniforms = List.of();
        owner.activeAttributes = List.of();
        owner.reflectedAttributeLocations.clear();
        if (owner.reflection == null) return;
        List<ActiveVariable> uniforms = new ArrayList<>();
        for (LevititeProgramReflection.Variable field : owner.reflection.uniforms()) {
            String name = field.size() > 1 ? field.name().substring(0, field.name().length() - 3) : field.name();
            int count = Math.multiplyExact(field.size(), field.components());
            UniformValue value = new UniformValue(new float[count], field.integer() ? new int[count] : null);
            owner.uniformValues.put(name, value);
            for (int index = 0; index < field.size(); index++) {
                int location = nextUniformLocation++;
                UniformLocation alias = new UniformLocation(owner, name, index * field.components(),
                        field.type(), field.size(), field.components(), value);
                UNIFORM_LOCATIONS.put(location, alias);
                if (index == 0) owner.uniformLocations.put(name, location);
                if (field.size() > 1) owner.uniformLocations.put(name + "[" + index + "]", location);
            }
            uniforms.add(new ActiveVariable(field.name(), field.size(), field.type()));
        }
        owner.activeUniforms = List.copyOf(uniforms);
        List<ActiveVariable> attributes = new ArrayList<>();
        for (LevititeProgramReflection.Variable input : owner.reflection.attributes()) {
            attributes.add(new ActiveVariable(input.name(), input.size(), input.type()));
            owner.reflectedAttributeLocations.put(input.name(), input.location());
        }
        owner.activeAttributes = List.copyOf(attributes);
    }

    private static UniformLocation readableUniform(int program, int location) {
        Program owner = PROGRAMS.get(program);
        UniformLocation entry = UNIFORM_LOCATIONS.get(location);
        if (owner == null || !owner.linked || entry == null || entry.owner != owner || entry.value == null) return null;
        return entry;
    }

    public static float getUniformf(int program, int location) {
        UniformLocation entry = readableUniform(program, location);
        return entry != null ? readFloat(entry, 0) : 0;
    }

    public static int getUniformi(int program, int location) {
        UniformLocation entry = readableUniform(program, location);
        return entry != null ? readInt(entry, 0) : 0;
    }

    public static void getUniformfv(int program, int location, FloatBuffer output) {
        UniformLocation entry = readableUniform(program, location);
        if (entry == null || output == null) return;
        int count = Math.min(entry.components, output.remaining());
        for (int i = 0; i < count; i++) output.put(output.position() + i, readFloat(entry, i));
    }

    public static void getUniformfv(int program, int location, float[] output) {
        UniformLocation entry = readableUniform(program, location);
        if (entry == null || output == null) return;
        int count = Math.min(entry.components, output.length);
        for (int i = 0; i < count; i++) output[i] = readFloat(entry, i);
    }

    public static void getUniformfv(int program, int location, long output) {
        UniformLocation entry = readableUniform(program, location);
        if (entry == null || output == 0) return;
        for (int i = 0; i < entry.components; i++) MemoryUtil.memPutFloat(output + (long) i * Float.BYTES, readFloat(entry, i));
    }

    public static void getUniformiv(int program, int location, IntBuffer output) {
        UniformLocation entry = readableUniform(program, location);
        if (entry == null || output == null) return;
        int count = Math.min(entry.components, output.remaining());
        for (int i = 0; i < count; i++) output.put(output.position() + i, readInt(entry, i));
    }

    public static void getUniformiv(int program, int location, int[] output) {
        UniformLocation entry = readableUniform(program, location);
        if (entry == null || output == null) return;
        int count = Math.min(entry.components, output.length);
        for (int i = 0; i < count; i++) output[i] = readInt(entry, i);
    }

    public static void getUniformiv(int program, int location, long output) {
        UniformLocation entry = readableUniform(program, location);
        if (entry == null || output == 0) return;
        for (int i = 0; i < entry.components; i++) MemoryUtil.memPutInt(output + (long) i * Integer.BYTES, readInt(entry, i));
    }

    private static float readFloat(UniformLocation entry, int component) {
        int index = entry.offset + component;
        return entry.value.ints != null ? entry.value.ints[index] : entry.value.floats[index];
    }

    private static int readInt(UniformLocation entry, int component) {
        int index = entry.offset + component;
        return entry.value.ints != null ? entry.value.ints[index] : Math.round(entry.value.floats[index]);
    }

    private static void clearUniforms(Program program) {
        for (int location : program.uniformLocations.values()) {
            UNIFORM_LOCATIONS.remove(location);
        }
        program.uniformLocations.clear();
        program.uniformValues.clear();
    }

    private static void releaseShader(int id, Shader shader) {
        shader.attachmentCount--;
        if (shader.attachmentCount == 0 && shader.deleteRequested) SHADERS.remove(id);
    }

    private static boolean hasCapacity(UniformValue value, int floatCount, int intCount) {
        return value.floats.length >= floatCount
                && (intCount == 0 ? value.ints == null : value.ints != null && value.ints.length >= intCount);
    }

    private static final class Shader {
        final int type;
        String source = "";
        String compiledSource = "";
        boolean compiled;
        int attachmentCount;
        boolean deleteRequested;

        Shader(int type) {
            this.type = type;
        }
    }

    private static final class Program {
        final Int2ReferenceOpenHashMap<Shader> attachedShaders = new Int2ReferenceOpenHashMap<>();
        final Map<String, Integer> explicitAttributeBindings = new HashMap<>();
        final Map<String, Integer> attributeLocations = new HashMap<>();
        final Map<String, Integer> uniformLocations = new HashMap<>();
        final Map<String, UniformValue> uniformValues = new HashMap<>();
        final Map<String, Integer> reflectedAttributeLocations = new HashMap<>();
        List<ActiveVariable> activeUniforms = List.of();
        List<ActiveVariable> activeAttributes = List.of();
        LevititeProgramReflection reflection;
        long linkGeneration;
        LinkedProgram linkedProgram;
        boolean linked;
    }

    private static final class UniformValue {
        final float[] floats;
        final int[] ints;
        private MappedBuffer mapped;

        UniformValue(float[] floats, int[] ints) {
            this.floats = floats;
            this.ints = ints;
        }

        MappedBuffer mappedBuffer() {
            if (mapped == null) {
                                                                                    
                                                                                   
                ByteBuffer storage = ByteBuffer.allocateDirect(Math.multiplyExact(floats.length, Float.BYTES))
                        .order(ByteOrder.nativeOrder());
                mapped = MappedBuffer.createFromBuffer(storage);
                syncMapped();
            }
            return mapped;
        }

        void syncMapped() {
            if (mapped == null) return;
            if (ints != null) {
                for (int i = 0; i < ints.length; i++) mapped.putInt(i * Integer.BYTES, ints[i]);
            } else {
                for (int i = 0; i < floats.length; i++) mapped.putFloat(i * Float.BYTES, floats[i]);
            }
        }
    }

    private static final class UniformLocation {
        final Program owner;
        final String name;
        final int offset;
        final int type;
        final int arraySize;
        int components;
        UniformValue value;

        UniformLocation(Program owner, String name) {
            this(owner, name, 0, 0, 1, 1, null);
        }

        UniformLocation(Program owner, String name, int offset, int type, int arraySize,
                        int components, UniformValue value) {
            this.owner = owner;
            this.name = name;
            this.offset = offset;
            this.type = type;
            this.arraySize = arraySize;
            this.components = components;
            this.value = value;
        }

        int writeCount(int requested) {
            return type != 0 ? Math.min(requested, arraySize - offset / components) : requested;
        }
    }
}
