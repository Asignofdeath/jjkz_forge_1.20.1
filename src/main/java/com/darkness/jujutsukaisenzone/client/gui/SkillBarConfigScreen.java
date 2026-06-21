package com.darkness.jujutsukaisenzone.client.gui;

import com.darkness.jujutsukaisenzone.client.TechniqueSkillTooltipHelper;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.player.PlayerTechniqueData;
import com.darkness.jujutsukaisenzone.technique.development.SkillUnlockLogic;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 技能快捷栏配置 GUI（按 I 键打开）。
 * <p>
 * 黑色背景，上方显示当前术式的可用技能图标（可拖拽），下方 9 个快捷栏位分 3 组。
 * 将上方技能图标拖到下方栏位完成配置；右键点击已配置栏位可清空；拖拽栏位间的技能可交换位置。
 * <p>
 * 配置按术式独立保存；关闭时自动发送到服务端持久化。
 * <p>
 * 栏位编号与游戏内按键的对应关系：
 * Tab+1→组1→键1/2/3 触发栏位 1/2/3；Tab+2→组2→键1/2/3 触发栏位 4/5/6；
 * Tab+3→组3→键1/2/3 触发栏位 7/8/9。
 */
public final class SkillBarConfigScreen extends Screen
{
    private static final int SCREEN_WIDTH = 300;
    private static final int SCREEN_HEIGHT = 200;
    private static final int ICON_SIZE = 20;
    private static final int ICON_GAP = 6;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 4;
    private static final int GROUP_GAP = 12;

    // --- 本地缓存：术式 → 9栏位配置 ---
    private static final Map<String, String[]> LOCAL_CACHE = new HashMap<>();

    // --- 实例字段 ---
    private final String techniqueId;
    private final List<TechniqueSkill> availableSkills;
    private final String[] slotConfig; // 索引0~8对应槽位1~9

    // 拖拽状态
    private int draggedSkillIndex = -1;
    private int draggedSkillSlot = -1; // 从槽位拖出时记录槽位，-1=从技能区拖出

    // 布局缓存
    private int guiLeft;
    private int guiTop;

    public SkillBarConfigScreen()
    {
        super(Component.literal("技能快捷栏配置"));

        Minecraft mc = Minecraft.getInstance();
        String techId = "";
        List<TechniqueSkill> skills = Collections.emptyList();

        if (mc.player != null)
        {
            techId = PlayerTechniqueData.getInnateTechnique(mc.player);
            if (!techId.isBlank())
            {
                skills = SkillUnlockLogic.getUnlockedConfigurableSkills(mc.player, techId);
            }
        }

        this.techniqueId = techId;
        this.availableSkills = skills;
        this.slotConfig = loadLocalConfig(techId);
        purgeLockedFromSlots();
    }

    // ======================== 静态方法 ========================

    /** 服务端同步配置到达时调用。 */
    public static void onConfigSync(String techniqueId, String[] skillIds)
    {
        LOCAL_CACHE.put(techniqueId, Arrays.copyOf(skillIds, 9));
        // 若当前正在打开配置界面，刷新
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SkillBarConfigScreen screen)
        {
            screen.refreshFromCache();
        }
    }

    /** 请求服务端同步指定术式的配置。 */
    public static void requestConfigFromServer(String techniqueId)
    {
        // 空包触发服务端回传配置
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                new com.darkness.jujutsukaisenzone.network.packet.SyncSkillBarConfigRequestC2SPacket(techniqueId)
        );
    }

    // ======================== Screen 生命周期 ========================

    @Override
    protected void init()
    {
        this.guiLeft = (this.width - SCREEN_WIDTH) / 2;
        this.guiTop = (this.height - SCREEN_HEIGHT) / 2;

        if (!techniqueId.isBlank())
        {
            requestConfigFromServer(techniqueId);
        }
    }

    void refreshFromCache()
    {
        String[] cached = LOCAL_CACHE.get(techniqueId);
        if (cached != null)
        {
            System.arraycopy(cached, 0, slotConfig, 0, 9);
            purgeLockedFromSlots();
        }
    }

    @Override
    public void onClose()
    {
        saveConfig();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    // ======================== 渲染 ========================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        // 全屏半透明黑色背景
        this.renderBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xDD_000000);

        // 顶部标题
        graphics.drawCenteredString(font, "技能快捷栏配置", this.width / 2, 8, 0xFF_FFFFFF);

        renderSkillIcons(graphics, mouseX, mouseY);
        renderSlots(graphics, mouseX, mouseY);
        renderDraggedIcon(graphics, mouseX, mouseY);

        // 底部操作提示
        String hint = "左键拖拽技能到栏位  |  右键清空栏位  |  I 键关闭";
        graphics.drawCenteredString(font, hint, this.width / 2, this.height - 14, 0xFF_888888);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSkillIcons(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (availableSkills.isEmpty()) return;

        int totalWidth = availableSkills.size() * ICON_SIZE + (availableSkills.size() - 1) * ICON_GAP;
        int startX = guiLeft + (SCREEN_WIDTH - totalWidth) / 2;
        int iconY = guiTop + 12;

        for (int i = 0; i < availableSkills.size(); i++)
        {
            int x = startX + i * (ICON_SIZE + ICON_GAP);
            renderSkillIcon(graphics, availableSkills.get(i), x, iconY, mouseX, mouseY);
        }
    }

    private void renderSlots(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int totalWidth = 3 * (3 * SLOT_SIZE + 2 * SLOT_GAP) + 2 * GROUP_GAP;
        int groupStartX = guiLeft + (SCREEN_WIDTH - totalWidth) / 2;
        int labelY = guiTop + 70;
        int slotY = guiTop + 82;

        for (int g = 0; g < 3; g++)
        {
            int gx = groupStartX + g * (3 * SLOT_SIZE + 2 * SLOT_GAP + GROUP_GAP);

            // 组标签
            graphics.drawCenteredString(font, "组" + (g + 1), gx + (3 * SLOT_SIZE + 2 * SLOT_GAP) / 2, labelY, 0xFF_CCCCCC);

            for (int s = 0; s < 3; s++)
            {
                int slotIndex = g * 3 + s; // 0..8
                int sx = gx + s * (SLOT_SIZE + SLOT_GAP);

                // 槽位背景
                boolean hovered = mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
                int bgColor = hovered ? 0xFF_444444 : 0xFF_222222;
                graphics.fill(sx, slotY, sx + SLOT_SIZE, slotY + SLOT_SIZE, bgColor);

                // 已配置技能图标
                String skillId = slotConfig[slotIndex];
                if (skillId != null && !skillId.isEmpty())
                {
                    TechniqueSkill skill = findSkillById(skillId);
                    if (skill != null)
                    {
                        ResourceLocation icon = iconResource(skill.iconPath());
                        RenderSystem.enableBlend();
                        graphics.blit(icon, sx + 2, slotY + 2, 0, 0, SLOT_SIZE - 4, SLOT_SIZE - 4, SLOT_SIZE - 4, SLOT_SIZE - 4);
                        RenderSystem.disableBlend();
                    }
                }

                // 槽位边框（用 4 条线画框）
                int borderColor = 0xFF_888888;
                graphics.fill(sx, slotY, sx + SLOT_SIZE, slotY + 1, borderColor);         // top
                graphics.fill(sx, slotY + SLOT_SIZE - 1, sx + SLOT_SIZE, slotY + SLOT_SIZE, borderColor); // bottom
                graphics.fill(sx, slotY, sx + 1, slotY + SLOT_SIZE, borderColor);         // left
                graphics.fill(sx + SLOT_SIZE - 1, slotY, sx + SLOT_SIZE, slotY + SLOT_SIZE, borderColor); // right

                if (hovered)
                {
                    TechniqueSkill slotSkill = findSkillForTooltip(skillId);
                    if (slotSkill != null)
                    {
                        renderSkillTooltip(graphics, slotSkill, mouseX, mouseY);
                    }
                }
            }
        }
    }

    private void renderDraggedIcon(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (draggedSkillIndex < 0) return;

        TechniqueSkill skill = (draggedSkillSlot >= 0)
                ? findSkillById(slotConfig[draggedSkillSlot])
                : availableSkills.get(Math.min(draggedSkillIndex, availableSkills.size() - 1));

        if (skill == null) return;

        int drawX = mouseX - ICON_SIZE / 2;
        int drawY = mouseY - ICON_SIZE / 2;

        ResourceLocation icon = iconResource(skill.iconPath());
        RenderSystem.enableBlend();
        graphics.blit(icon, drawX, drawY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }

    private void renderSkillIcon(GuiGraphics graphics, TechniqueSkill skill, int x, int y, int mouseX, int mouseY)
    {
        boolean hovered = mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
        if (hovered)
        {
            graphics.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0xFF_666666);
        }

        ResourceLocation icon = iconResource(skill.iconPath());
        RenderSystem.enableBlend();
        graphics.blit(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();

        if (hovered)
        {
            renderSkillTooltip(graphics, skill, mouseX, mouseY);
        }
    }

    private void renderSkillTooltip(GuiGraphics graphics, TechniqueSkill skill, int mouseX, int mouseY)
    {
        if (minecraft.player == null || skill == null || techniqueId.isBlank())
        {
            return;
        }
        List<Component> tooltip = TechniqueSkillTooltipHelper.build(minecraft.player, techniqueId, skill);
        if (!tooltip.isEmpty())
        {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    // ======================== 鼠标交互 ========================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        // 右键：不处理拖拽，让下面逻辑处理清空
        if (button == 1)
        {
            int slot = findSlotAt((int) mouseX, (int) mouseY);
            if (slot >= 0)
            {
                slotConfig[slot] = "";
                return true;
            }
        }

        // 左键：检查技能图标区域
        if (button == 0)
        {
            int skillIdx = findSkillIconAt((int) mouseX, (int) mouseY);
            if (skillIdx >= 0)
            {
                draggedSkillIndex = skillIdx;
                draggedSkillSlot = -1;
                return true;
            }

            // 检查槽位（从槽位拖出已有技能）
            int slot = findSlotAt((int) mouseX, (int) mouseY);
            if (slot >= 0 && slotConfig[slot] != null && !slotConfig[slot].isEmpty())
            {
                draggedSkillIndex = 0; // dummy, won't be used from availableSkills
                draggedSkillSlot = slot;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0 && draggedSkillIndex >= 0)
        {
            int slot = findSlotAt((int) mouseX, (int) mouseY);

            if (draggedSkillSlot >= 0)
            {
                // 从槽位拖出
                String movedSkill = slotConfig[draggedSkillSlot];
                slotConfig[draggedSkillSlot] = "";

                if (slot >= 0 && slot != draggedSkillSlot)
                {
                    // 放到另一个槽位上（交换）
                    String targetSkill = slotConfig[slot];
                    slotConfig[draggedSkillSlot] = targetSkill;
                    slotConfig[slot] = movedSkill;
                }
                // 否则丢弃（已清空）
            }
            else if (slot >= 0)
            {
                // 从技能区拖到槽位
                if (draggedSkillIndex < availableSkills.size())
                {
                    slotConfig[slot] = availableSkills.get(draggedSkillIndex).skillId();
                }
            }
            // 否则丢弃

            draggedSkillIndex = -1;
            draggedSkillSlot = -1;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ======================== 工具方法 ========================

    private int findSkillIconAt(int mouseX, int mouseY)
    {
        if (availableSkills.isEmpty()) return -1;
        int totalWidth = availableSkills.size() * ICON_SIZE + (availableSkills.size() - 1) * ICON_GAP;
        int startX = guiLeft + (SCREEN_WIDTH - totalWidth) / 2;
        int iconY = guiTop + 12;

        for (int i = 0; i < availableSkills.size(); i++)
        {
            int x = startX + i * (ICON_SIZE + ICON_GAP);
            if (mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= iconY && mouseY < iconY + ICON_SIZE)
            {
                return i;
            }
        }
        return -1;
    }

    private int findSlotAt(int mouseX, int mouseY)
    {
        int totalWidth = 3 * (3 * SLOT_SIZE + 2 * SLOT_GAP) + 2 * GROUP_GAP;
        int groupStartX = guiLeft + (SCREEN_WIDTH - totalWidth) / 2;
        int slotY = guiTop + 82;

        for (int g = 0; g < 3; g++)
        {
            int gx = groupStartX + g * (3 * SLOT_SIZE + 2 * SLOT_GAP + GROUP_GAP);
            for (int s = 0; s < 3; s++)
            {
                int sx = gx + s * (SLOT_SIZE + SLOT_GAP);
                if (mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE)
                {
                    return g * 3 + s;
                }
            }
        }
        return -1;
    }

    private TechniqueSkill findSkillById(String skillId)
    {
        for (TechniqueSkill s : availableSkills)
        {
            if (s.skillId().equals(skillId)) return s;
        }
        return null;
    }

    @Nullable
    private TechniqueSkill findSkillForTooltip(@Nullable String skillId)
    {
        if (skillId == null || skillId.isEmpty())
        {
            return null;
        }
        TechniqueSkill skill = findSkillById(skillId);
        if (skill != null)
        {
            return skill;
        }
        return SkillUnlockLogic.findSkillDefinition(techniqueId, skillId).orElse(null);
    }

    private static ResourceLocation iconResource(String path)
    {
        if (path.contains(":"))
        {
            return ResourceLocation.parse(path);
        }
        return ResourceLocation.fromNamespaceAndPath("jujutsukaisenzone", path);
    }

    // ======================== 保存与加载 ========================

    private void saveConfig()
    {
        if (techniqueId.isBlank()) return;
        purgeLockedFromSlots();
        ModNetwork.sendSkillBarConfigToServer(techniqueId, slotConfig);
    }

    private void purgeLockedFromSlots()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || techniqueId.isBlank())
        {
            return;
        }
        for (int i = 0; i < 9; i++)
        {
            slotConfig[i] = SkillUnlockLogic.sanitizeSkillBarSlot(mc.player, techniqueId, slotConfig[i]);
        }
    }

    private static String[] loadLocalConfig(String techniqueId)
    {
        String[] cached = LOCAL_CACHE.get(techniqueId);
        if (cached != null)
        {
            return Arrays.copyOf(cached, 9);
        }
        // 新建全空配置
        String[] empty = new String[9];
        Arrays.fill(empty, "");
        return empty;
    }
}
