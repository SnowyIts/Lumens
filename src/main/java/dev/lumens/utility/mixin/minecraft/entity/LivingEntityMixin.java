package dev.lumens.utility.mixin.minecraft.entity;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import dev.lumens.utility.interfaces.IMinecraft;

@Mixin({LivingEntity.class})
public class LivingEntityMixin implements IMinecraft {
}
