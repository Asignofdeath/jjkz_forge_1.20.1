package com.darkness.jujutsukaisenzone.blackhole;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 黑洞_A —— 全屏引力透镜后处理器（移植自 Soul's Black Holes）。
 *
 * 管线：把主帧缓冲 blit 到临时纹理 → 全屏三角形按 Schwarzschild 偏折公式
 * 重采样 → 结果直接画回主帧缓冲。与黑闪二值化滤镜同一套「拷贝主帧缓冲
 * 防读写同缓冲」的自定义 GL 管线，不与 Forge / 光影后处理链混用。
 */
public class LensingPostProcessor
{
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(LensingPostProcessor.class);
    private static final int MAX_BLACK_HOLES = 8;
    private static final String VERT_PATH = "/assets/jujutsukaisenzone/shaders/post/blackhole_lensing.vsh";
    private static final String FRAG_PATH = "/assets/jujutsukaisenzone/shaders/post/blackhole_lensing.fsh";

    private int shaderProgram = -1;
    private int sceneCopyTexture = -1;
    private int quadVAO = -1;
    private int quadVBO = -1;
    private int lastTextureWidth;
    private int lastTextureHeight;
    private boolean initialized = false;
    private boolean shaderFailed = false;
    private int uSceneTexture;
    private int uDepthTexture;
    private int uScreenSize;
    private int uNumBlackHoles;
    private int uBlackHoleScreenPos;
    private int uBlackHoleScreenRadius;
    private int uBlackHoleDepth;
    private int uLensingStrength;
    private int uNearPlane;
    private int uFarPlane;
    private final float[] screenPositions = new float[MAX_BLACK_HOLES * 2];
    private final float[] screenRadii = new float[MAX_BLACK_HOLES];
    private final float[] linearDepths = new float[MAX_BLACK_HOLES];
    private int activeBlackHoles = 0;
    private int blitFboId = -1;

    public void init()
    {
        if (this.initialized || this.shaderFailed)
        {
            return;
        }
        try
        {
            String vertSrc = this.loadShaderSource(VERT_PATH);
            String fragSrc = this.loadShaderSource(FRAG_PATH);
            int vertShader = this.compileShader(GL20.GL_VERTEX_SHADER, vertSrc);
            int fragShader = this.compileShader(GL20.GL_FRAGMENT_SHADER, fragSrc);
            this.shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(this.shaderProgram, vertShader);
            GL20.glAttachShader(this.shaderProgram, fragShader);
            GL20.glBindAttribLocation(this.shaderProgram, 0, "Position");
            GL20.glLinkProgram(this.shaderProgram);
            if (GL20.glGetProgrami(this.shaderProgram, GL20.GL_LINK_STATUS) == 0)
            {
                String log = GL20.glGetProgramInfoLog(this.shaderProgram);
                LOGGER.error("黑洞_A 透镜着色器链接失败: {}", log);
                this.cleanup();
                this.shaderFailed = true;
                return;
            }
            GL20.glDeleteShader(vertShader);
            GL20.glDeleteShader(fragShader);

            this.uSceneTexture = GL20.glGetUniformLocation(this.shaderProgram, "SceneTexture");
            this.uDepthTexture = GL20.glGetUniformLocation(this.shaderProgram, "DepthTexture");
            this.uScreenSize = GL20.glGetUniformLocation(this.shaderProgram, "ScreenSize");
            this.uNumBlackHoles = GL20.glGetUniformLocation(this.shaderProgram, "NumBlackHoles");
            this.uBlackHoleScreenPos = GL20.glGetUniformLocation(this.shaderProgram, "BlackHoleScreenPos");
            this.uBlackHoleScreenRadius = GL20.glGetUniformLocation(this.shaderProgram, "BlackHoleScreenRadius");
            this.uBlackHoleDepth = GL20.glGetUniformLocation(this.shaderProgram, "BlackHoleDepth");
            this.uLensingStrength = GL20.glGetUniformLocation(this.shaderProgram, "LensingStrength");
            this.uNearPlane = GL20.glGetUniformLocation(this.shaderProgram, "NearPlane");
            this.uFarPlane = GL20.glGetUniformLocation(this.shaderProgram, "FarPlane");

            this.sceneCopyTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.sceneCopyTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            this.initQuadGeometry();
            this.initialized = true;
            LOGGER.info("黑洞_A 引力透镜后处理器初始化成功");
        }
        catch (Exception e)
        {
            LOGGER.error("黑洞_A 引力透镜后处理器初始化失败", e);
            this.cleanup();
            this.shaderFailed = true;
        }
    }

    public void setBlackHoleData(int index, float screenX, float screenY, float screenRadius, float linearDepth)
    {
        if (index >= MAX_BLACK_HOLES)
        {
            return;
        }
        this.screenPositions[index * 2] = screenX;
        this.screenPositions[index * 2 + 1] = screenY;
        this.screenRadii[index] = screenRadius;
        this.linearDepths[index] = linearDepth;
    }

    public void setActiveBlackHoles(int count)
    {
        this.activeBlackHoles = Math.min(count, MAX_BLACK_HOLES);
    }

    public void apply()
    {
        if (!this.initialized || this.shaderFailed || this.activeBlackHoles == 0)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int prevTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevVAO = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        this.copyFramebufferToTexture(mainTarget, width, height);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL20.glUseProgram(this.shaderProgram);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.sceneCopyTexture);
        GL20.glUniform1i(this.uSceneTexture, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainTarget.getColorTextureId());
        GL20.glUniform1i(this.uDepthTexture, 1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUniform2f(this.uScreenSize, (float) width, (float) height);
        GL20.glUniform1i(this.uNumBlackHoles, this.activeBlackHoles);
        GL20.glUniform1f(this.uLensingStrength, BlackHoleVisualConfig.LENSING_STRENGTH);
        GL20.glUniform1f(this.uNearPlane, 0.05F);
        GL20.glUniform1f(this.uFarPlane, mc.options.renderDistance().get() * 16.0F);
        if (this.uBlackHoleScreenPos >= 0)
        {
            GL20.glUniform2fv(this.uBlackHoleScreenPos, this.screenPositions);
        }
        if (this.uBlackHoleScreenRadius >= 0)
        {
            GL20.glUniform1fv(this.uBlackHoleScreenRadius, this.screenRadii);
        }
        if (this.uBlackHoleDepth >= 0)
        {
            GL20.glUniform1fv(this.uBlackHoleDepth, this.linearDepths);
        }

        this.drawFullscreenQuad();

        GL30.glBindVertexArray(prevVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
        GL13.glActiveTexture(prevActiveTexture);
        GL20.glUseProgram(prevProgram);
        if (prevBlend)
        {
            GL11.glEnable(GL11.GL_BLEND);
        }
        else
        {
            GL11.glDisable(GL11.GL_BLEND);
        }
        if (prevDepthTest)
        {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        else
        {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        if (prevCullFace)
        {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        else
        {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void copyFramebufferToTexture(RenderTarget target, int width, int height)
    {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.sceneCopyTexture);
        if (width != this.lastTextureWidth || height != this.lastTextureHeight)
        {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            this.lastTextureWidth = width;
            this.lastTextureHeight = height;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        if (this.blitFboId < 0)
        {
            this.blitFboId = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.blitFboId);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, this.sceneCopyTexture, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        else if (width != this.lastTextureWidth || height != this.lastTextureHeight)
        {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.blitFboId);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, this.sceneCopyTexture, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }

        int prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.blitFboId);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    private void initQuadGeometry()
    {
        // 两个三角形覆盖全屏（[0,1]² 位置）
        float[] vertices = new float[] {
                0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F,
                0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F
        };
        this.quadVAO = GL30.glGenVertexArrays();
        this.quadVBO = GL15.glGenBuffers();
        GL30.glBindVertexArray(this.quadVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.quadVBO);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0L);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private void drawFullscreenQuad()
    {
        GL30.glBindVertexArray(this.quadVAO);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    /** 世界坐标 → 屏幕坐标（UV 0~1），返回 [screenX, screenY, ndcZ]；在相机背后返回 null */
    public static float[] projectToScreen(Vector3f worldPos, Matrix4f modelViewMat, Matrix4f projMat)
    {
        Vector4f clipPos = new Vector4f(worldPos.x, worldPos.y, worldPos.z, 1.0F);
        clipPos.mul(modelViewMat);
        clipPos.mul(projMat);
        if (clipPos.w <= 0.0F)
        {
            return null;
        }
        float ndcX = clipPos.x / clipPos.w;
        float ndcY = clipPos.y / clipPos.w;
        float ndcZ = clipPos.z / clipPos.w;
        return new float[] {(ndcX + 1.0F) * 0.5F, (ndcY + 1.0F) * 0.5F, ndcZ};
    }

    /** 世界半径 → 屏幕空间半径（UV 0~1 的一半为半个视场） */
    public static float screenSpaceRadius(float radiusWorld, float distToCamera, float fov)
    {
        if (distToCamera <= 0.0F)
        {
            return 1.0F;
        }
        float angularSize = (float) Math.atan(radiusWorld / distToCamera);
        float halfFovRad = (float) Math.toRadians(fov * 0.5);
        return angularSize / halfFovRad * 0.5F;
    }

    public void cleanup()
    {
        if (this.shaderProgram > 0)
        {
            GL20.glDeleteProgram(this.shaderProgram);
            this.shaderProgram = -1;
        }
        if (this.sceneCopyTexture > 0)
        {
            GL11.glDeleteTextures(this.sceneCopyTexture);
            this.sceneCopyTexture = -1;
        }
        if (this.quadVAO > 0)
        {
            GL30.glDeleteVertexArrays(this.quadVAO);
            this.quadVAO = -1;
        }
        if (this.quadVBO > 0)
        {
            GL15.glDeleteBuffers(this.quadVBO);
            this.quadVBO = -1;
        }
        if (this.blitFboId > 0)
        {
            GL30.glDeleteFramebuffers(this.blitFboId);
            this.blitFboId = -1;
        }
        this.initialized = false;
    }

    public boolean isInitialized()
    {
        return this.initialized;
    }

    public boolean hasFailed()
    {
        return this.shaderFailed;
    }

    private int compileShader(int type, String source)
    {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0)
        {
            String log = GL20.glGetShaderInfoLog(shader);
            String typeName = type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment";
            GL20.glDeleteShader(shader);
            throw new RuntimeException("Failed to compile " + typeName + " shader: " + log);
        }
        return shader;
    }

    private String loadShaderSource(String path) throws IOException
    {
        try (InputStream is = LensingPostProcessor.class.getResourceAsStream(path))
        {
            if (is == null)
            {
                throw new IOException("Shader resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
