package dev.lumens.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.lumens.base.events.impl.other.EventCloseScreen;
import dev.lumens.base.events.impl.player.EventMotion;
import dev.lumens.base.events.impl.player.EventMove;
import dev.lumens.base.events.impl.player.EventSlowWalking;
import dev.lumens.base.events.impl.player.EventSprintUpdate;
import dev.lumens.base.events.impl.player.EventUpdate;
import dev.lumens.client.modules.impl.player.NoPush;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
   @Shadow
   private float lastYaw;
   @Shadow
   @Final
   protected MinecraftClient client;
   @Final
   @Shadow
   public ClientPlayNetworkHandler networkHandler;
   @Shadow
   private double lastX;
   @Shadow
   private double lastBaseY;
   @Shadow
   private double lastZ;
   @Shadow
   private float lastPitch;
   @Shadow
   private boolean lastOnGround;
   @Shadow
   private boolean lastHorizontalCollision;
   @Shadow
   private boolean autoJumpEnabled;
   @Shadow
   private int ticksSinceLastPositionPacketSent;
   @Unique
   private final EventMotion event = new EventMotion(0.0F, 0.0F);

   public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Shadow
   private void sendSprintingPacket() {
      throw new AssertionError();
   }

   @Shadow
   protected boolean isCamera() {
      return false;
   }

   @Shadow
   protected abstract void autoJump(float var1, float var2);

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      EventManager.call(new EventUpdate());
   }

   @Redirect(
      method = {"sendMovementPackets"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendSprintingPacket()V"
)
   )
   public void invokeSprintUpdate(ClientPlayerEntity instance) {
      EventSprintUpdate eventSprintUpdate = new EventSprintUpdate();
      EventManager.call(eventSprintUpdate);
      if (!eventSprintUpdate.isCancelled()) {
         this.sendSprintingPacket();
      }

   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
      if (NoPush.INSTANCE.isEnabled() && NoPush.INSTANCE.getObjects().isEnable("Блоки")) {
         ci.cancel();
      }

   }

   @Redirect(
      method = {"tickMovement"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
),
      require = 0
   )
   private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
      if (player.isUsingItem()) {
         EventSlowWalking slowDownEvent = new EventSlowWalking();
         EventManager.call(slowDownEvent);
         return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
      } else {
         return player.isUsingItem() && player.getVehicle() == null;
      }
   }

   @Inject(
      method = {"closeHandledScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void closeHandledScreenHook(CallbackInfo info) {
      EventCloseScreen event = new EventCloseScreen(this.client.currentScreen);
      EventManager.call(event);
      if (event.isCancelled()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"move"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
)},
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      EventMove event = new EventMove(movement);
      EventManager.call(event);
      double d = this.getX();
      double e = this.getZ();
      super.move(movementType, event.getMovePos());
      this.autoJump((float)(this.getX() - d), (float)(this.getZ() - e));
      ci.cancel();
   }

   /**
    * @author javelin
    * @reason rotation hook
    */
   @Overwrite
   private void sendMovementPackets() {
      this.sendSprintingPacket();
      if (this.isCamera()) {
         this.event.setYaw(this.getYaw());
         this.event.setPitch(this.getPitch());
         EventManager.call(this.event);
         if (this.event.isCancelled()) {
            this.event.setCancelled(false);
            return;
         }

         double d = this.getX() - this.lastX;
         double e = this.getY() - this.lastBaseY;
         double f = this.getZ() - this.lastZ;
         double g = (double)(this.event.getYaw() - this.lastYaw);
         double h = (double)(this.event.getPitch() - this.lastPitch);
         ++this.ticksSinceLastPositionPacketSent;
         boolean bl = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4D) || this.ticksSinceLastPositionPacketSent >= 20;
         boolean bl2 = g != 0.0D || h != 0.0D;
         if (bl && bl2) {
            this.networkHandler.sendPacket(new Full(this.getX(), this.getY(), this.getZ(), this.event.getYaw(), this.event.getPitch(), this.isOnGround(), this.horizontalCollision));
         } else if (bl) {
            this.networkHandler.sendPacket(new PositionAndOnGround(this.getX(), this.getY(), this.getZ(), this.isOnGround(), this.horizontalCollision));
         } else if (bl2) {
            this.networkHandler.sendPacket(new LookAndOnGround(this.event.getYaw(), this.event.getPitch(), this.isOnGround(), this.horizontalCollision));
         } else if (this.lastOnGround != this.isOnGround() || this.lastHorizontalCollision != this.horizontalCollision) {
            this.networkHandler.sendPacket(new OnGroundOnly(this.isOnGround(), this.horizontalCollision));
         }

         if (bl) {
            this.lastX = this.getX();
            this.lastBaseY = this.getY();
            this.lastZ = this.getZ();
            this.ticksSinceLastPositionPacketSent = 0;
         }

         if (bl2) {
            this.lastYaw = this.event.getYaw();
            this.lastPitch = this.event.getPitch();
         }

         this.lastOnGround = this.isOnGround();
         this.lastHorizontalCollision = this.horizontalCollision;
         this.autoJumpEnabled = (Boolean)this.client.options.getAutoJump().getValue();
      }

   }
}
