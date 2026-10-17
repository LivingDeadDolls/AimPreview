package aimpreview.sim;

import aimpreview.config.AimPreviewConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Works out what the player would shoot right now and how it leaves the hand,
 * with the same spawn points and speeds as the vanilla items.
 */
public final class Aim {
    /** Spread between multishot arrows (the vanilla Multishot enchantment). */
    private static final float MULTISHOT_SPREAD = 10.0f;

    private Aim() {
    }

    /** What would be shot and with which item; no launches when nothing would be. */
    public record Aimed(ItemStack stack, List<Launch> launches) {
        static final Aimed NONE = new Aimed(ItemStack.EMPTY, List.of());
    }

    public static List<Launch> launches(Player player, float partialTick, AimPreviewConfig config) {
        return aim(player, partialTick, config).launches();
    }

    public static Aimed aim(Player player, float partialTick, AimPreviewConfig config) {
        boolean holding = config.timing == AimPreviewConfig.Timing.HOLDING;
        if (player.isUsingItem()) {
            ItemStack stack = player.getUseItem();
            List<Launch> using = forItem(player, stack, true, holding, partialTick, config);
            if (!using.isEmpty()) return new Aimed(stack, using);
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            List<Launch> held = forItem(player, stack, false, holding, partialTick, config);
            if (!held.isEmpty()) return new Aimed(stack, held);
        }
        return Aimed.NONE;
    }

    private static List<Launch> forItem(Player player, ItemStack stack, boolean using, boolean holding, float pt, AimPreviewConfig config) {
        if (stack.isEmpty()) return List.of();
        Shooter s = new Shooter(player, pt);

        if (stack.getItem() instanceof BowItem) {
            if (!config.bow || player.getProjectile(stack).isEmpty()) return List.of();
            float power;
            if (using) {
                power = BowItem.getPowerForTime(player.getTicksUsingItem());
                if (power < 0.1f) return List.of();
            } else if (holding) {
                power = 1.0f;
            } else {
                return List.of();
            }
            return List.of(s.fromRotation(Physics.ARROW, s.eyeY - 0.1, 0.0f, power * 3.0f));
        }
        if (stack.getItem() instanceof CrossbowItem) {
            if (!config.crossbow) return List.of();
            ChargedProjectiles charged = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            if (!charged.isEmpty()) {
                boolean firework = charged.contains(Items.FIREWORK_ROCKET);
                if (firework && !config.crossbowFirework) return List.of();
                float power = firework ? 1.6f : 3.15f;
                // itemCopies() is a Stream in 26.3 and a List before; forEach works on both.
                List<ItemStack> items = new ArrayList<>();
                charged.itemCopies().forEach(items::add);
                List<Launch> launches = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0 && !config.multishotAll) break;
                    ItemStack item = items.get(i);
                    float angle = multishotAngle(i, items.size());
                    if (item.is(Items.FIREWORK_ROCKET)) {
                        Fireworks fireworks = item.get(DataComponents.FIREWORKS);
                        int flights = 1 + (fireworks == null ? 0 : fireworks.flightDuration());
                        // Vanilla adds 0-5 and 0-6 random ticks; the middle of that is used.
                        launches.add(s.crossbow(Physics.FIREWORK, s.eyeY - 0.15, angle, power).withLifetime(10 * flights + 5));
                    } else {
                        launches.add(s.crossbow(Physics.ARROW, s.eyeY - 0.1, angle, power));
                    }
                }
                return launches;
            }
            if (!holding) return List.of();
            int count = multishotLevel(player, stack) > 0 ? 3 : 1;
            List<Launch> launches = new ArrayList<>();
            for (int i = 0; i < (config.multishotAll ? count : 1); i++) {
                launches.add(s.crossbow(Physics.ARROW, s.eyeY - 0.1, multishotAngle(i, count), 3.15f));
            }
            return launches;
        }
        if (stack.getItem() instanceof TridentItem) {
            if (!config.trident || !(using || holding)) return List.of();
            // A riptide trident is never thrown.
            if (EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0.0f) return List.of();
            return List.of(s.fromRotation(Physics.TRIDENT, s.eyeY - 0.1, 0.0f, 2.5f));
        }
        if (stack.getItem() instanceof SnowballItem) {
            return config.snowball ? List.of(s.fromRotation(Physics.THROWABLE, s.eyeY - 0.1, 0.0f, 1.5f)) : List.of();
        }
        if (stack.getItem() instanceof EggItem) {
            return config.egg ? List.of(s.fromRotation(Physics.THROWABLE, s.eyeY - 0.1, 0.0f, 1.5f)) : List.of();
        }
        if (stack.getItem() instanceof EnderpearlItem) {
            return config.enderPearl ? List.of(s.fromRotation(Physics.THROWABLE, s.eyeY - 0.1, 0.0f, 1.5f)) : List.of();
        }
        if (stack.getItem() instanceof SplashPotionItem) {
            return config.splashPotion ? List.of(s.fromRotation(Physics.POTION, s.eyeY - 0.1, -20.0f, 0.5f)) : List.of();
        }
        if (stack.getItem() instanceof LingeringPotionItem) {
            return config.lingeringPotion ? List.of(s.fromRotation(Physics.POTION, s.eyeY - 0.1, -20.0f, 0.5f)) : List.of();
        }
        if (stack.getItem() instanceof ExperienceBottleItem) {
            return config.experienceBottle ? List.of(s.fromRotation(Physics.EXPERIENCE_BOTTLE, s.eyeY - 0.1, -20.0f, 0.7f)) : List.of();
        }
        if (stack.getItem() instanceof WindChargeItem) {
            return config.windCharge ? List.of(s.fromRotation(Physics.STRAIGHT, s.eyeY, 0.0f, 1.5f)) : List.of();
        }
        if (stack.getItem() instanceof FishingRodItem) {
            if (!config.fishingRod || player.fishing != null) return List.of();
            return List.of(s.fishingHook());
        }
        return List.of();
    }

    /** Vanilla ProjectileWeaponItem.shoot: 0, then alternating -step, +step, ... */
    private static float multishotAngle(int index, int count) {
        if (count <= 1) return 0.0f;
        float step = 2.0f * MULTISHOT_SPREAD / (count - 1);
        float offset = (count - 1) % 2 * step / 2.0f;
        float direction = index % 2 == 1 ? -1.0f : 1.0f;
        return offset + direction * ((index + 1) / 2) * step;
    }

    private static int multishotLevel(Player player, ItemStack stack) {
        return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.MULTISHOT)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel((Holder<Enchantment>) holder, stack))
                .orElse(0);
    }

    /** The player as seen at this frame. */
    private static final class Shooter {
        final Player player;
        final float pt;
        final double x;
        final double z;
        final double eyeY;
        final float xRot;
        final float yRot;

        Shooter(Player player, float pt) {
            this.player = player;
            this.pt = pt;
            this.x = Mth.lerp(pt, player.xo, player.getX());
            this.z = Mth.lerp(pt, player.zo, player.getZ());
            this.eyeY = Mth.lerp(pt, player.yo, player.getY()) + player.getEyeHeight();
            this.xRot = player.getViewXRot(pt);
            this.yRot = player.getViewYRot(pt);
        }

        /** The movement the server adds to thrown things (ServerPlayer.getKnownMovement: the last client step). */
        Vec3 movement() {
            return new Vec3(player.getX() - player.xo, player.onGround() ? 0.0 : player.getY() - player.yo, player.getZ() - player.zo);
        }

        /** Projectile.shootFromRotation. */
        Launch fromRotation(Physics physics, double y, float yOffset, float power) {
            float rad = Mth.DEG_TO_RAD;
            double dx = -Mth.sin(yRot * rad) * Mth.cos(xRot * rad);
            double dy = -Mth.sin((xRot + yOffset) * rad);
            double dz = Mth.cos(yRot * rad) * Mth.cos(xRot * rad);
            return new Launch(physics, new Vec3(x, y, z), Spread.shot(new Vec3(dx, dy, dz), power, movement()), player);
        }

        /** CrossbowItem.shootProjectile: the view turned around the player's up axis; no added movement. */
        Launch crossbow(Physics physics, double y, float angle, float power) {
            Vec3 up = player.getUpVector(pt);
            Quaternionf turn = new Quaternionf().setAngleAxis(angle * Mth.DEG_TO_RAD, up.x, up.y, up.z);
            Vector3f dir = player.getViewVector(pt).toVector3f().rotate(turn);
            return new Launch(physics, new Vec3(x, y, z), Spread.shot(new Vec3(dir.x(), dir.y(), dir.z()), power, Vec3.ZERO), player);
        }

        /** FishingHook(Player, ...): starts beside the eye; the speed without randomness is the middle of vanilla's. */
        Launch fishingHook() {
            float yCos = Mth.cos(-yRot * Mth.DEG_TO_RAD - Mth.PI);
            float ySin = Mth.sin(-yRot * Mth.DEG_TO_RAD - Mth.PI);
            float xCos = -Mth.cos(-xRot * Mth.DEG_TO_RAD);
            float xSin = Mth.sin(-xRot * Mth.DEG_TO_RAD);
            Vec3 start = new Vec3(x - ySin * 0.3, eyeY, z - yCos * 0.3);
            Vec3 dir = new Vec3(-ySin, Mth.clamp(-(xSin / xCos), -5.0f, 5.0f), -yCos);
            return new Launch(Physics.FISHING, start, Spread.fishing(dir, 0.6 / dir.length() + 0.5), player);
        }
    }
}
