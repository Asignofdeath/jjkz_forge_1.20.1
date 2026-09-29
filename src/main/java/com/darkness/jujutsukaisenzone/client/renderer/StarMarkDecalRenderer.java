package com.darkness.jujutsukaisenzone.client.renderer;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.darkness.jujutsukaisenzone.client.StarMarkDecalClient;
import com.darkness.jujutsukaisenzone.player.StarMark;
import com.darkness.jujutsukaisenzone.technique.skill.StarMarkSkillLogic;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 星星标记表现：
 * <ul>
 *   <li>喷漆贴花——贴在方块命中面 / 实体碰撞箱面上，<b>所有人可见</b>；</li>
 *   <li>头顶标记——目标头顶上方，始终面朝玩家（billboard），<b>仅施法者自己可见</b>。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StarMarkDecalRenderer
{
    /** 贴花相对面的外偏，避免 z-fight。 */
    private static final float SURFACE_EPS = 0.012F;
    /** 喷漆贴花基础高度（格）；宽度按贴图宽高比拉伸。 */
    private static final float BASE_HEIGHT = 0.22F;
    private static final float[] TEXTURE_ASPECT = {32f / 16f, 32f / 16f, 40f / 16f, 40f / 16f, 42f / 16f};

    /** 头顶标记基础高度（格）；贴图约 28×25。 */
    private static final float HEAD_BASE_HEIGHT = 0.32F;
    private static final float HEAD_ASPECT = 28f / 25f;
    /** 相对「头顶」再上抬一点，避免与名牌 / 碰撞箱重叠。 */
    private static final float HEAD_LIFT = 0.35F;
    /** 方块头顶标记：方块顶面之上。 */
    private static final float BLOCK_HEAD_LIFT = 0.45F;

    private StarMarkDecalRenderer()
    {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        StarMarkDecalClient.clear();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null)
        {
            return;
        }
        Map<UUID, List<StarMark>> snapshot = StarMarkDecalClient.snapshot();
        if (snapshot.isEmpty())
        {
            return;
        }

        Level level = minecraft.level;
        ResourceKey<Level> dimension = level.dimension();
        Vec3 camera = event.getCamera().getPosition();
        Quaternionf cameraRotation = event.getCamera().rotation();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        UUID selfId = minecraft.player.getUUID();
        for (Map.Entry<UUID, List<StarMark>> entry : snapshot.entrySet())
        {
            boolean ownMarks = selfId.equals(entry.getKey());
            for (StarMark mark : entry.getValue())
            {
                if (!mark.hasDecal() || !mark.dimension().equals(dimension))
                {
                    continue;
                }
                if (mark.kind() == StarMark.Kind.BLOCK)
                {
                    renderBlockDecal(poseStack, buffer, tesselator, camera, mark);
                    if (ownMarks)
                    {
                        Vec3 head = new Vec3(
                                mark.pos().getX() + 0.5D,
                                mark.pos().getY() + 1.0D + BLOCK_HEAD_LIFT,
                                mark.pos().getZ() + 0.5D);
                        renderHeadBillboard(poseStack, buffer, tesselator, camera, cameraRotation, mark, head);
                    }
                }
                else if (mark.kind() == StarMark.Kind.ENTITY && mark.entityId() != null)
                {
                    Entity entity = findEntity(level, mark.entityId());
                    if (entity == null || !entity.isAlive())
                    {
                        continue;
                    }
                    renderEntityDecal(poseStack, buffer, tesselator, camera, mark, entity, partialTick);
                    if (ownMarks)
                    {
                        Vec3 renderPos = entity.getPosition(partialTick);
                        Vec3 head = new Vec3(
                                renderPos.x,
                                renderPos.y + entity.getBbHeight() + HEAD_LIFT,
                                renderPos.z);
                        renderHeadBillboard(poseStack, buffer, tesselator, camera, cameraRotation, mark, head);
                    }
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static Entity findEntity(Level level, UUID id)
    {
        if (level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)
        {
            for (Entity entity : clientLevel.entitiesForRendering())
            {
                if (id.equals(entity.getUUID()))
                {
                    return entity;
                }
            }
        }
        return null;
    }

    private static void renderBlockDecal(PoseStack poseStack, BufferBuilder buffer, Tesselator tesselator,
                                         Vec3 camera, StarMark mark)
    {
        double minX = mark.pos().getX();
        double minY = mark.pos().getY();
        double minZ = mark.pos().getZ();
        drawFaceQuad(poseStack, buffer, tesselator, camera, mark,
                minX, minY, minZ, minX + 1.0D, minY + 1.0D, minZ + 1.0D,
                mark.face(), mark.u(), mark.v());
    }

    private static void renderEntityDecal(PoseStack poseStack, BufferBuilder buffer, Tesselator tesselator,
                                          Vec3 camera, StarMark mark, Entity entity, float partialTick)
    {
        AABB box = entity.getBoundingBox();
        Vec3 renderPos = entity.getPosition(partialTick);
        Vec3 currentPos = entity.position();
        double dx = renderPos.x - currentPos.x;
        double dy = renderPos.y - currentPos.y;
        double dz = renderPos.z - currentPos.z;
        Direction worldFace = StarMarkSkillLogic.localHorizontalToWorld(mark.face(), entity.getYRot());
        drawFaceQuad(poseStack, buffer, tesselator, camera, mark,
                box.minX + dx, box.minY + dy, box.minZ + dz,
                box.maxX + dx, box.maxY + dy, box.maxZ + dz,
                worldFace, mark.u(), mark.v());
    }

    /** 头顶标记：相机朝向 billboard。 */
    private static void renderHeadBillboard(PoseStack poseStack, BufferBuilder buffer, Tesselator tesselator,
                                            Vec3 camera, Quaternionf cameraRotation, StarMark mark, Vec3 worldPos)
    {
        float height = HEAD_BASE_HEIGHT * (1.0F + 0.06F * (mark.stacks() - 1));
        float width = height * HEAD_ASPECT;
        float halfW = width * 0.5F;
        float halfH = height * 0.5F;

        RenderSystem.setShaderTexture(0, mark.type().headTexture());
        poseStack.pushPose();
        poseStack.translate(worldPos.x - camera.x, worldPos.y - camera.y, worldPos.z - camera.z);
        poseStack.mulPose(cameraRotation);
        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int a = 255;
        // 相机朝向本地 -Z；四边形画在 XY 平面，面向玩家
        vertexLocal(buffer, matrix, -halfW, -halfH, 0.0F, 0.0F, 1.0F, a);
        vertexLocal(buffer, matrix, halfW, -halfH, 0.0F, 1.0F, 1.0F, a);
        vertexLocal(buffer, matrix, halfW, halfH, 0.0F, 1.0F, 0.0F, a);
        vertexLocal(buffer, matrix, -halfW, halfH, 0.0F, 0.0F, 0.0F, a);
        tesselator.end();
        poseStack.popPose();
    }

    private static void drawFaceQuad(PoseStack poseStack, BufferBuilder buffer, Tesselator tesselator,
                                     Vec3 camera, StarMark mark,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     Direction face, float u, float v)
    {
        float aspect = TEXTURE_ASPECT[Math.min(mark.type().ordinal(), TEXTURE_ASPECT.length - 1)];
        float height = BASE_HEIGHT * (1.0F + 0.08F * (mark.stacks() - 1));
        float width = height * aspect;
        float halfW = width * 0.5F;
        float halfH = height * 0.5F;

        Vec3 center = pointOnFace(minX, minY, minZ, maxX, maxY, maxZ, face, u, v);
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 right;
        Vec3 up;
        switch (face)
        {
            case UP ->
            {
                right = new Vec3(1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 0.0D, 1.0D);
            }
            case DOWN ->
            {
                right = new Vec3(1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 0.0D, -1.0D);
            }
            case NORTH ->
            {
                right = new Vec3(-1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
            case SOUTH ->
            {
                right = new Vec3(1.0D, 0.0D, 0.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
            case WEST ->
            {
                right = new Vec3(0.0D, 0.0D, 1.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
            default ->
            { // EAST
                right = new Vec3(0.0D, 0.0D, -1.0D);
                up = new Vec3(0.0D, 1.0D, 0.0D);
            }
        }

        Vec3 origin = center.add(normal.scale(SURFACE_EPS));
        Vec3 c0 = origin.add(right.scale(-halfW)).add(up.scale(-halfH));
        Vec3 c1 = origin.add(right.scale(halfW)).add(up.scale(-halfH));
        Vec3 c2 = origin.add(right.scale(halfW)).add(up.scale(halfH));
        Vec3 c3 = origin.add(right.scale(-halfW)).add(up.scale(halfH));

        RenderSystem.setShaderTexture(0, mark.type().decalTexture());
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int a = 230;
        vertex(buffer, matrix, c0, 0.0F, 1.0F, a);
        vertex(buffer, matrix, c1, 1.0F, 1.0F, a);
        vertex(buffer, matrix, c2, 1.0F, 0.0F, a);
        vertex(buffer, matrix, c3, 0.0F, 0.0F, a);
        vertex(buffer, matrix, c0, 0.0F, 1.0F, a);
        vertex(buffer, matrix, c3, 0.0F, 0.0F, a);
        vertex(buffer, matrix, c2, 1.0F, 0.0F, a);
        vertex(buffer, matrix, c1, 1.0F, 1.0F, a);
        tesselator.end();
        poseStack.popPose();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, float u, float v, int alpha)
    {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .uv(u, v)
                .color(255, 255, 255, alpha)
                .endVertex();
    }

    private static void vertexLocal(BufferBuilder buffer, Matrix4f matrix,
                                    float x, float y, float z, float u, float v, int alpha)
    {
        buffer.vertex(matrix, x, y, z)
                .uv(u, v)
                .color(255, 255, 255, alpha)
                .endVertex();
    }

    private static Vec3 pointOnFace(double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ,
                                    Direction face, float u, float v)
    {
        float cu = clamp01(u);
        float cv = clamp01(v);
        return switch (face)
        {
            case UP -> new Vec3(lerp(minX, maxX, cu), maxY, lerp(minZ, maxZ, cv));
            case DOWN -> new Vec3(lerp(minX, maxX, cu), minY, lerp(minZ, maxZ, cv));
            case NORTH -> new Vec3(lerp(minX, maxX, cu), lerp(minY, maxY, cv), minZ);
            case SOUTH -> new Vec3(lerp(minX, maxX, cu), lerp(minY, maxY, cv), maxZ);
            case WEST -> new Vec3(minX, lerp(minY, maxY, cv), lerp(minZ, maxZ, cu));
            case EAST -> new Vec3(maxX, lerp(minY, maxY, cv), lerp(minZ, maxZ, cu));
        };
    }

    private static double lerp(double a, double b, float t)
    {
        return a + (b - a) * t;
    }

    private static float clamp01(float value)
    {
        if (value < 0.0F)
        {
            return 0.0F;
        }
        if (value > 1.0F)
        {
            return 1.0F;
        }
        return value;
    }
}
