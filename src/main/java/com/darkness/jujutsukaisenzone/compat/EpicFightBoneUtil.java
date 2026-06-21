package com.darkness.jujutsukaisenzone.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * 从 Epic Fight 骨骼姿态读取世界空间位置与朝向，供特效实体跟随挂点。
 */
public final class EpicFightBoneUtil
{
    /** 右小臂挂点（EF Biped {@code Arm_R}）。 */
    public static final String RIGHT_FOREARM_JOINT_NAME = "Arm_R";

    private EpicFightBoneUtil()
    {
    }

    public record BoneTransform(Vec3 position, float yRot, float xRot)
    {
    }

    @Nullable
    public static Joint resolveRightForearmJoint(LivingEntity owner)
    {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(owner, LivingEntityPatch.class);
        if (patch == null || !(patch.getArmature() instanceof HumanoidArmature humanoid))
        {
            return null;
        }
        return humanoid.armR;
    }

    @Nullable
    public static BoneTransform getRightForearmWorldTransform(LivingEntity owner, float partialTick)
    {
        Joint joint = resolveRightForearmJoint(owner);
        if (joint == null)
        {
            return null;
        }
        return getJointWorldTransform(owner, joint, partialTick);
    }

    @Nullable
    public static BoneTransform getJointWorldTransform(LivingEntity owner, Joint joint, float partialTick)
    {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(owner, LivingEntityPatch.class);
        if (patch == null)
        {
            return null;
        }

        Pose pose = patch.getAnimator().getPose(partialTick);
        Vec3 position = owner.position();
        OpenMatrix4f worldTransform = patch.getArmature().getBoundTransformFor(pose, joint)
                .mulFront(
                        OpenMatrix4f.createTranslation((float) position.x, (float) position.y, (float) position.z)
                                .mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS)
                                        .mulBack(patch.getModelMatrix(partialTick))));

        Vec3 worldPos = OpenMatrix4f.transform(worldTransform, Vec3.ZERO);
        Vec3 forward = OpenMatrix4f.transform(worldTransform, new Vec3(0.0D, 0.0D, -1.0D)).subtract(worldPos);
        if (forward.lengthSqr() < 1.0E-8D)
        {
            forward = owner.getLookAngle();
        }
        else
        {
            forward = forward.normalize();
        }

        float yRot = (float) MathUtils.getYRotOfVector(forward);
        float xRot = (float) MathUtils.getXRotOfVector(forward);
        return new BoneTransform(worldPos, yRot, xRot);
    }
}
