#version 330
#extension GL_ARB_separate_shader_objects : enable

#CreateConstant

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <fog.glsl>
#endif

#if SHADER_VERSION >= 2
#moj_import <dynamictransforms.glsl>
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#else
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
layout(location = 0) in float vertexDistance;
#endif

uniform sampler2D Sampler0;

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

#GenerateOtherDefinedMethod

void main() {
#ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
#else
    vec4 texColor = texture(Sampler0, texCoord0);
#endif
#ifdef IS_SEE_THROUGH
    vec4 color = texColor * vertexColor;
#else
    vec4 color = texColor * vertexColor * ColorModulator;
#endif

    #GenerateOtherMainMethod

    if (color.a < 0.1) {
        discard;
    }
#ifdef IS_SEE_THROUGH
    fragColor = color * ColorModulator;
#elif defined(IS_GUI)
    fragColor = color;
#elif SHADER_VERSION >= 2
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
#else
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
#endif
}
