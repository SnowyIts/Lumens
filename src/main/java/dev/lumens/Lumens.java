package dev.lumens;

import java.io.File;
import lombok.Generated;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import dev.lumens.base.autobuy.AutoBuyManager;
import dev.lumens.base.comand.CommandManager;
import dev.lumens.base.config.ConfigManager;
import dev.lumens.base.discord.DiscordManager;
import dev.lumens.base.filemanager.impl.FriendManager;
import dev.lumens.base.filemanager.impl.StaffManager;
import dev.lumens.base.license.LumensAuth;
import dev.lumens.base.macro.MacroManager;
import dev.lumens.base.modules.ModuleManager;
import dev.lumens.base.notify.NotifyManager;
import dev.lumens.base.repository.RCTRepository;
import dev.lumens.base.request.ScriptManager;
import dev.lumens.base.theme.ThemeManager;
import dev.lumens.base.waypoint.WaypointManager;
import dev.lumens.client.screens.menu.MenuScreen;
import dev.lumens.utility.game.server.ServerHandler;
import dev.lumens.utility.render.display.shader.DrawUtil;
import dev.lumens.utility.render.display.shader.GlProgram;

public enum Lumens implements ClientModInitializer {
   INSTANCE;

   public static final String NAME = "Lumens";
   public static final String VER = "";
   public static final String TYPE = "DEV";
   private static final String MOD_ID = "Lumens".toLowerCase();
   public static File DIRECTORY;
   private ModuleManager moduleManager;
   private ThemeManager themeManager;
   private MenuScreen menuScreen;
   private ScriptManager scriptManager;
   private ServerHandler serverHandler;
   private FriendManager friendManager;
   private MacroManager macroManager;
   private StaffManager staffManager;
   private AutoBuyManager autoBuyManager;
   private WaypointManager waypointManager;
   private NotifyManager notifyManager;
   private CommandManager commandManager;
   private ConfigManager configManager;
   private RCTRepository rctRepository;
   private DiscordManager discordManager;
   private boolean initialized = false;

   @Override
   public void onInitializeClient() {
      try {
         init();
      } catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
   }

   @Native
   public void init() {
      if (initialized) {
         return;
      }
      initialized = true;
      
      try {
         DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, "Lumens");
         if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
         }
         
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getInstance().shutdown();
         }));
         
         this.friendManager = new FriendManager();
         this.macroManager = new MacroManager();
         this.staffManager = new StaffManager();
         this.notifyManager = new NotifyManager();
         this.serverHandler = new ServerHandler();
         this.rctRepository = new RCTRepository();
         this.themeManager = new ThemeManager();
         this.moduleManager = new ModuleManager();
         this.configManager = new ConfigManager();
         this.autoBuyManager = new AutoBuyManager();
         this.commandManager = new CommandManager();
         this.scriptManager = new ScriptManager();
         try {
            this.discordManager = new DiscordManager();
         } catch (Throwable e) {
            this.discordManager = null;
         }
         this.waypointManager = new WaypointManager();
         this.menuScreen = new MenuScreen();
         LumensAuth.get().initAsync();
         ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
               return Lumens.id("after_shader_load");
            }

            @Override
            public void reload(ResourceManager manager) {
               GlProgram.loadAndSetupPrograms();
            }
         });
         DrawUtil.initializeShaders();
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException("Lumens initialization failed", e);
      }
   }

   @Native
   public void shutdown() {
      this.friendManager.save();
      this.staffManager.save();
      this.configManager.save();
      this.macroManager.save();
      if (this.discordManager != null) {
         this.discordManager.stopRPC();
      }

   }

   public static Identifier id(String path) {
      return Identifier.of("javelin", path);
   }

   public static Lumens getInstance() {
      return INSTANCE;
   }

   public RCTRepository getRCTRepository() {
      return this.rctRepository;
   }

   @Generated
   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   @Generated
   public ThemeManager getThemeManager() {
      return this.themeManager;
   }

   @Generated
   public MenuScreen getMenuScreen() {
      return this.menuScreen;
   }

   @Generated
   public ScriptManager getScriptManager() {
      return this.scriptManager;
   }

   @Generated
   public ServerHandler getServerHandler() {
      return this.serverHandler;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public MacroManager getMacroManager() {
      return this.macroManager;
   }

   @Generated
   public StaffManager getStaffManager() {
      return this.staffManager;
   }

   @Generated
   public AutoBuyManager getAutoBuyManager() {
      return this.autoBuyManager;
   }

   @Generated
   public WaypointManager getWaypointManager() {
      return this.waypointManager;
   }

   @Generated
   public NotifyManager getNotifyManager() {
      return this.notifyManager;
   }

   @Generated
   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public DiscordManager getDiscordManager() {
      return this.discordManager;
   }


   private static Lumens[] $values() {
      return new Lumens[]{INSTANCE};
   }
}
