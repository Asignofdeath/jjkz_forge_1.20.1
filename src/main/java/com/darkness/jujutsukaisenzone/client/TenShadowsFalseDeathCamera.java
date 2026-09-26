package com.darkness.jujutsukaisenzone.client;

import com.darkness.jujutsukaisenzone.JuJutsuKaisenZone;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 假死离体相机（学 CMDCam outside：镜头离开身体，身体仍留在场上）。
 * <p>
 * 用客户端 Marker + {@link Minecraft#setCameraEntity} 驱动视角（不碰 protected 的 Camera#setPosition）。
 * WASD 平移，鼠标转向；不改服务端坐标。假死移动锁仍由 {@link PlayerInputLockController} 清零身体输入。
 */
@Mod.EventBusSubscriber(modid = JuJutsuKaisenZone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TenShadowsFalseDeathCamera
{
    private static final double MOVE_SPEED = 0.45D;
    private static final double FAST_MULTIPLIER = 2.0D;

    private static boolean active;
    private static Marker cameraEntity;
    private static float camYaw;
    private static float camPitch;
    private static float bodyYaw;
    private static float bodyPitch;
    private static double lastMouseX;
    private static double lastMouseY;
    private static boolean mouseInitialized;

    private TenShadowsFalseDeathCamera()
    {
    }

    public static boolean isActive()
    {
        return active;
    }

    public static void activateAt(Entity entity)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || entity == null)
        {
            return;
        }
        deactivate();
        Marker marker = EntityType.MARKER.create(mc.level);
        if (marker == null)
        {
            return;
        }
        Vec3 eye = entity.getEyePosition();
        marker.setPos(eye.x, eye.y + 1.0D, eye.z);
        camYaw = entity.getYRot();
        camPitch = 30.0F;
        bodyYaw = entity.getYRot();
        bodyPitch = entity.getXRot();
        marker.setYRot(camYaw);
        marker.setXRot(camPitch);
        marker.yRotO = camYaw;
        marker.xRotO = camPitch;
        cameraEntity = marker;
        mouseInitialized = false;
        active = true;
        mc.setCameraEntity(marker);
    }

    public static void deactivate()
    {
        Minecraft mc = Minecraft.getInstance();
        if (cameraEntity != null && mc.getCameraEntity() == cameraEntity)
        {
            mc.setCameraEntity(mc.player);
        }
        cameraEntity = null;
        active = false;
        mouseInitialized = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !active || cameraEntity == null)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            deactivate();
            return;
        }
        /* 身体朝向锁回进入假死时，避免鼠标仍转动躯干 */
        mc.player.setYRot(bodyYaw);
        mc.player.setXRot(bodyPitch);
        mc.player.yRotO = bodyYaw;
        mc.player.xRotO = bodyPitch;

        if (mc.screen != null)
        {
            return;
        }
        long window = mc.getWindow().getWindow();
        float yawRad = camYaw * ((float) Math.PI / 180.0F);
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        double rightX = forwardZ;
        double rightZ = -forwardX;

        double speed = MOVE_SPEED;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL))
        {
            speed *= FAST_MULTIPLIER;
        }

        double x = cameraEntity.getX();
        double y = cameraEntity.getY();
        double z = cameraEntity.getZ();

        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W))
        {
            x += forwardX * speed;
            z += forwardZ * speed;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S))
        {
            x -= forwardX * speed;
            z -= forwardZ * speed;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A))
        {
            x -= rightX * speed;
            z -= rightZ * speed;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D))
        {
            x += rightX * speed;
            z += rightZ * speed;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_SPACE))
        {
            y += speed;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT))
        {
            y -= speed;
        }

        double mx = mc.mouseHandler.xpos();
        double my = mc.mouseHandler.ypos();
        if (!mouseInitialized)
        {
            lastMouseX = mx;
            lastMouseY = my;
            mouseInitialized = true;
        }
        else
        {
            double dx = mx - lastMouseX;
            double dy = my - lastMouseY;
            lastMouseX = mx;
            lastMouseY = my;
            double sens = mc.options.sensitivity().get() * 0.6D + 0.2D;
            double scale = sens * sens * sens * 8.0D * 0.15D;
            camYaw += (float) (dx * scale);
            camPitch = Mth.clamp(camPitch + (float) (dy * scale), -90.0F, 90.0F);
        }

        cameraEntity.xo = cameraEntity.getX();
        cameraEntity.yo = cameraEntity.getY();
        cameraEntity.zo = cameraEntity.getZ();
        cameraEntity.yRotO = cameraEntity.getYRot();
        cameraEntity.xRotO = cameraEntity.getXRot();
        cameraEntity.setPos(x, y, z);
        cameraEntity.setYRot(camYaw);
        cameraEntity.setXRot(camPitch);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        if (!active)
        {
            return;
        }
        event.setYaw(camYaw);
        event.setPitch(camPitch);
        event.setRoll(0.0F);
    }
}
