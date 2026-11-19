package com.eyad.cleanmirror.window;

import com.eyad.cleanmirror.config.CleanMirrorConfig.ScalingMode;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Paints one texture across the clean window. Lives entirely in the clean window's own context.
 *
 * Two details that matter:
 *  - The little scaffolding objects (VAO, program, sampler) are created HERE, in the second
 *    window's context. Textures are shared between the two windows; VAOs are not.
 *  - We read the picture through our own "sampler" object instead of changing the texture's
 *    settings. The texture belongs to Minecraft's graphics layer, which keeps its own record of
 *    those settings - changing them behind its back would make that record wrong.
 */
final class Blitter {

    private static final String VERTEX = """
            #version 330 core
            out vec2 uv;
            uniform vec2 uScale;
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                uv = p;
                gl_Position = vec4((p * 2.0 - 1.0) * uScale, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT = """
            #version 330 core
            in vec2 uv;
            out vec4 fragColor;
            uniform sampler2D uTex;
            void main() {
                fragColor = vec4(texture(uTex, uv).rgb, 1.0);
            }
            """;

    private int program, vao, sampler, uScale, uTex;

    void init() {
        int vs = compile(GL_VERTEX_SHADER, VERTEX);
        int fs = compile(GL_FRAGMENT_SHADER, FRAGMENT);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Blitter link failed: " + glGetProgramInfoLog(program));
        }
        glDetachShader(program, vs);
        glDetachShader(program, fs);
        glDeleteShader(vs);
        glDeleteShader(fs);
        uScale = glGetUniformLocation(program, "uScale");
        uTex = glGetUniformLocation(program, "uTex");
        vao = glGenVertexArrays();

        sampler = glGenSamplers();
        glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    private static int compile(int type, String src) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Blitter compile failed: " + glGetShaderInfoLog(s));
        }
        return s;
    }

    void blit(int texture, int srcW, int srcH, int dstW, int dstH, ScalingMode scaling) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, dstW, dstH);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDisable(GL_SCISSOR_TEST);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        float sx = 1f, sy = 1f;
        if (srcW > 0 && srcH > 0 && scaling != ScalingMode.STRETCH) {
            float src = (float) srcW / srcH;
            float dst = (float) dstW / dstH;
            if (scaling == ScalingMode.LETTERBOX) {
                if (src > dst) sy = dst / src; else sx = src / dst;
            } else if (scaling == ScalingMode.CROP) {
                if (src > dst) sx = src / dst; else sy = dst / src;
            }
        }

        glUseProgram(program);
        glUniform2f(uScale, sx, sy);
        glUniform1i(uTex, 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glBindSampler(0, sampler);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
        glBindSampler(0, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glUseProgram(0);
    }

    void destroy() {
        if (sampler != 0) { glDeleteSamplers(sampler); sampler = 0; }
        if (vao != 0) { glDeleteVertexArrays(vao); vao = 0; }
        if (program != 0) { glDeleteProgram(program); program = 0; }
    }
}
