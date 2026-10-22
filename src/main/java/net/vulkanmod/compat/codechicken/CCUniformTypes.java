package net.vulkanmod.compat.codechicken;

import net.vulkanmod.vulkan.shader.CustomUniformSource.Descriptor;

import static net.vulkanmod.vulkan.shader.CustomUniformSource.ScalarKind.*;


public final class CCUniformTypes {
    private CCUniformTypes() {
    }


    public static Descriptor descriptor(String enumName, int totalComponentCount) {
        Descriptor element = switch (enumName) {
            case "INT" -> new Descriptor(I32, 1, 1, 1);
            case "I_VEC2" -> new Descriptor(I32, 1, 2, 1);
            case "I_VEC3" -> new Descriptor(I32, 1, 3, 1);
            case "I_VEC4" -> new Descriptor(I32, 1, 4, 1);
            case "U_INT" -> new Descriptor(U32, 1, 1, 1);
            case "U_VEC2" -> new Descriptor(U32, 1, 2, 1);
            case "U_VEC3" -> new Descriptor(U32, 1, 3, 1);
            case "U_VEC4" -> new Descriptor(U32, 1, 4, 1);
            case "B_VEC2" -> new Descriptor(B32, 1, 2, 1);
            case "B_VEC3" -> new Descriptor(B32, 1, 3, 1);
            case "B_VEC4" -> new Descriptor(B32, 1, 4, 1);
            case "FLOAT" -> new Descriptor(F32, 1, 1, 1);
            case "VEC2" -> new Descriptor(F32, 1, 2, 1);
            case "VEC3" -> new Descriptor(F32, 1, 3, 1);
            case "VEC4" -> new Descriptor(F32, 1, 4, 1);
            case "MAT2" -> new Descriptor(F32, 2, 2, 1);
            case "MAT2x3" -> new Descriptor(F32, 2, 3, 1);
            case "MAT2x4" -> new Descriptor(F32, 2, 4, 1);
            case "MAT3" -> new Descriptor(F32, 3, 3, 1);
            case "MAT3x2" -> new Descriptor(F32, 3, 2, 1);
            case "MAT3x4" -> new Descriptor(F32, 3, 4, 1);
            case "MAT4" -> new Descriptor(F32, 4, 4, 1);
            case "MAT4x2" -> new Descriptor(F32, 4, 2, 1);
            case "MAT4x3" -> new Descriptor(F32, 4, 3, 1);
            case "DOUBLE" -> new Descriptor(F64, 1, 1, 1);
            case "D_VEC2" -> new Descriptor(F64, 1, 2, 1);
            case "D_VEC3" -> new Descriptor(F64, 1, 3, 1);
            case "D_VEC4" -> new Descriptor(F64, 1, 4, 1);
            case "D_MAT2" -> new Descriptor(F64, 2, 2, 1);
            case "D_MAT2x3" -> new Descriptor(F64, 2, 3, 1);
            case "D_MAT2x4" -> new Descriptor(F64, 2, 4, 1);
            case "D_MAT3" -> new Descriptor(F64, 3, 3, 1);
            case "D_MAT3x2" -> new Descriptor(F64, 3, 2, 1);
            case "D_MAT3x4" -> new Descriptor(F64, 3, 4, 1);
            case "D_MAT4" -> new Descriptor(F64, 4, 4, 1);
            case "D_MAT4x2" -> new Descriptor(F64, 4, 2, 1);
            case "D_MAT4x3" -> new Descriptor(F64, 4, 3, 1);
            default -> throw new IllegalArgumentException("Unsupported CodeChickenLib uniform type: " + enumName);
        };
        int componentsPerElement = element.columns() * element.rows();
        if (totalComponentCount <= 0 || totalComponentCount % componentsPerElement != 0) {
            throw new IllegalArgumentException("CodeChickenLib uniform " + enumName
                    + " requires a positive component count divisible by " + componentsPerElement
                    + "; got " + totalComponentCount);
        }
        return new Descriptor(element.kind(), element.columns(), element.rows(),
                totalComponentCount / componentsPerElement);
    }
}
