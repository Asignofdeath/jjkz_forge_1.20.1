package com.darkness.jujutsukaisenzone;

import com.mojang.logging.LogUtils;
import com.darkness.jujutsukaisenzone.client.TechniqueDevelopmentKeyHandler;
import com.darkness.jujutsukaisenzone.client.TechniqueExpansionKeyHandler;
import com.darkness.jujutsukaisenzone.client.gui.SkillBarConfigScreen;
import com.darkness.jujutsukaisenzone.compat.EpicFightCompat;
import com.darkness.jujutsukaisenzone.event.TechniqueSkillKeyEvents;
import com.darkness.jujutsukaisenzone.network.ModNetwork;
import com.darkness.jujutsukaisenzone.registry.ModAttributes;
import com.darkness.jujutsukaisenzone.registry.ModBlockEntities;
import com.darkness.jujutsukaisenzone.registry.ModBlocks;
import com.darkness.jujutsukaisenzone.registry.ModCreativeTabs;
import com.darkness.jujutsukaisenzone.registry.ModEffects;
import com.darkness.jujutsukaisenzone.registry.ModEntities;
import com.darkness.jujutsukaisenzone.registry.ModItems;
import com.darkness.jujutsukaisenzone.registry.ModSounds;
import com.darkness.jujutsukaisenzone.technique.commonskill.CommonKeySkillRegistry;
import com.darkness.jujutsukaisenzone.technique.commonskill.BarrierTechniqueCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.commonskill.MimicryCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.commonskill.EmotionalReleaseCommonKeySkill;
import com.darkness.jujutsukaisenzone.technique.commonskill.ReverseTechniqueCommonKeySkill;
import com.darkness.jujutsukaisenzone.player.mimicry.MimicryVoiceLineDefaults;
import com.darkness.jujutsukaisenzone.technique.description.TechniqueSkillDescriptions;
import com.darkness.jujutsukaisenzone.technique.skillset.JudgmentTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.aerial.AerialCombatSkillRegistry;
import com.darkness.jujutsukaisenzone.technique.skillset.LimitlessTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.LoveRendezvousTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.ShrineTechniqueSkillSet;
import com.darkness.jujutsukaisenzone.technique.skillset.TechniqueSkillSetRegistry;
import com.darkness.jujutsukaisenzone.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(JuJutsuKaisenZone.MODID)
public class JuJutsuKaisenZone
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "jujutsukaisenzone";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 打开技能快捷栏配置 GUI 的按键（I 键）。 */
    private static final KeyMapping SKILL_BAR_CONFIG_KEY = new KeyMapping(
            "key.jujutsukaisenzone.skill_bar_config",
            GLFW.GLFW_KEY_I,
            "key.categories.jujutsukaisenzone"
    );


    public JuJutsuKaisenZone(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModAttributes.register(modEventBus);
        ModSounds.register(modEventBus);
        ModNetwork.register();

        TechniqueSkillSetRegistry.register(LimitlessTechniqueSkillSet.INSTANCE);
        TechniqueSkillSetRegistry.register(ShrineTechniqueSkillSet.INSTANCE);
        TechniqueSkillSetRegistry.register(JudgmentTechniqueSkillSet.INSTANCE);
        TechniqueSkillSetRegistry.register(LoveRendezvousTechniqueSkillSet.INSTANCE);
        AerialCombatSkillRegistry.bootstrap();
        TechniqueSkillDescriptions.bootstrap();
        CommonKeySkillRegistry.register(ReverseTechniqueCommonKeySkill.INSTANCE);
        CommonKeySkillRegistry.register(BarrierTechniqueCommonKeySkill.INSTANCE);
        CommonKeySkillRegistry.register(MimicryCommonKeySkill.INSTANCE);
        CommonKeySkillRegistry.register(EmotionalReleaseCommonKeySkill.INSTANCE);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> com.darkness.jujutsukaisenzone.client.gui.JjkzModConfigScreen.create(parent))));
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));

        event.enqueueWork(MimicryVoiceLineDefaults::register);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        // Disable hunger-based natural regeneration globally.
        MinecraftServer server = event.getServer();
        server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, server);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
        {
            CommonKeySkillRegistry.registerKeyMappings(event);
            event.register(SKILL_BAR_CONFIG_KEY);
            event.register(TechniqueDevelopmentKeyHandler.KEY_MAPPING);
            event.register(TechniqueExpansionKeyHandler.KEY_MAPPING);
            event.register(TechniqueSkillKeyEvents.KEY_ADJUST_MODE);
            event.register(TechniqueSkillKeyEvents.KEY_ADJUST_TIER_DOWN);
            event.register(TechniqueSkillKeyEvents.KEY_ADJUST_TIER_UP);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            /* 注册自定义天空盒 BlockEntityRenderer */
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                    com.darkness.jujutsukaisenzone.registry.ModBlockEntities.JJKZ_SKYBLOCK.get(),
                    com.darkness.jujutsukaisenzone.client.renderer.skyblock.JJKZSkyblockRenderer::new
            );

            /* 帐方块贴图含半透明 alpha，需 translucent 渲染层（cutout 会显示为纯黑不透明） */
            event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ModBlocks.SHROUD_BLOCK.get(), RenderType.translucent()));

            /* 注册自定义天空盒 RenderPass */
            event.enqueueWork(() -> {
                com.noodlegamer76.shadered.client.renderer.ComplexPassRenderer renderer =
                        com.noodlegamer76.shadered.client.renderer.ComplexPassRenderer.getInstance();
                renderer.add(
                        com.noodlegamer76.shadered.client.util.RenderStage.AFTER_BLOCK_ENTITIES,
                        new com.darkness.jujutsukaisenzone.client.renderer.skyblock.JJKZSkyblockRenderPass()
                );
            });
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        private static boolean wasSkillBarConfigKeyDown;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            CommonKeySkillRegistry.onClientTick(event);
            handleSkillBarConfigKey(event);
        }

        private static void handleSkillBarConfigKey(TickEvent.ClientTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            boolean isDown = SKILL_BAR_CONFIG_KEY.isDown();
            if (isDown && !wasSkillBarConfigKeyDown)
            {
                mc.setScreen(new SkillBarConfigScreen());
            }
            wasSkillBarConfigKeyDown = isDown;
        }
    }

    public static boolean isCombatModeEnabled()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && EpicFightCompat.isBattleMode(minecraft.player);
    }
}
