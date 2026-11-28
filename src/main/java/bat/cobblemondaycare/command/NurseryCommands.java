package bat.cobblemondaycare.command;

import bat.cobblemondaycare.nursery.model.ParentSnapshot;
import bat.cobblemondaycare.nursery.service.NurseryService;
import bat.cobblemondaycare.network.NurseryNetworking;
import bat.cobblemondaycare.text.NurseryMessages;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class NurseryCommands {
    private NurseryCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            Commands.literal("nursery")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("status")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        NurseryService nurseryService = NurseryService.getInstance();
                        int cost = nurseryService.getStartCost();
                        boolean canAfford = nurseryService.canAffordStart(player);
                        NurseryService.DaycareStateView daycareState = nurseryService.getDaycareState(player);
                        context.getSource().sendSuccess(
                            () -> tr("cmd_status", cost, tr(canAfford ? "yes" : "no"))
                                .withStyle(ChatFormatting.AQUA),
                            false
                        );

                        for (NurseryService.ProcessSlotView slotView : nurseryService.getProcessSlots(player)) {
                            Component message = switch (slotView.status()) {
                                case EMPTY -> tr("slot", slotView.slotIndex() + 1, tr("empty"))
                                    .withStyle(ChatFormatting.DARK_GRAY);
                                case BREEDING -> tr("cmd_breeding", slotView.slotIndex() + 1, parentSummary(slotView.parentSummary()), slotView.remainingMinutes()).withStyle(ChatFormatting.YELLOW);
                                case READY -> tr("cmd_ready", slotView.slotIndex() + 1, parentSummary(slotView.parentSummary())).withStyle(ChatFormatting.GREEN);
                                case CLAIMED -> tr("slot", slotView.slotIndex() + 1, tr("claimed"))
                                    .withStyle(ChatFormatting.GRAY);
                                case CANCELLED -> tr("slot", slotView.slotIndex() + 1, tr("cancelled"))
                                    .withStyle(ChatFormatting.GRAY);
                            };
                            context.getSource().sendSuccess(() -> message, false);
                        }

                        context.getSource().sendSuccess(
                            () -> tr("cmd_daycare", daycareState.selectedYardIndex() + 1, daycareState.eggTray().size(), daycareState.eggTrayCapacity()).withStyle(ChatFormatting.GOLD),
                            false
                        );

                        for (NurseryService.DaycareYardView yard : daycareState.yards()) {
                            Component pair = shortPair(yard.parentA(), yard.parentB());
                            String detail = yard.remainingMs() > 0 ? " (" + ((yard.remainingMs() + 59_999L) / 60_000L) + "m)" : "";
                            context.getSource().sendSuccess(
                                () -> tr("cmd_yard", yard.yardIndex() + 1, tr(yard.unlocked() ? "yes" : "no"), yardState(yard.visualState().name()), yard.eggCount(), yard.eggCapacity()).append(yard.parentA() == null && yard.parentB() == null ? Component.empty() : tr("cmd_pair", pair)).append(detail).withStyle(ChatFormatting.YELLOW),
                                false
                            );
                        }
                        return 1;
                    }))
                .then(Commands.literal("preview")
                    .then(Commands.argument("slot_a", IntegerArgumentType.integer(1, 6))
                        .then(Commands.argument("slot_b", IntegerArgumentType.integer(1, 6))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int slotA = IntegerArgumentType.getInteger(context, "slot_a") - 1;
                                int slotB = IntegerArgumentType.getInteger(context, "slot_b") - 1;

                                var preview = NurseryService.getInstance().preview(player, slotA, slotB).orElse(null);
                                if (preview == null) {
                                    context.getSource().sendFailure(tr("cmd_invalid"));
                                    return 0;
                                }

                                context.getSource().sendSuccess(
                                    () -> tr("compat", NurseryMessages.component(preview.compatibility().reason()))
                                        .withStyle(preview.compatibility().compatible() ? ChatFormatting.GREEN : ChatFormatting.RED),
                                    false
                                );

                                if (preview.generatedEgg() != null) {
                                    context.getSource().sendSuccess(
                                        () -> tr("cmd_child", species(preview.generatedEgg().childSpeciesId()), nature(preview.generatedEgg().natureId()), preview.generatedEgg().ivs().toString()).withStyle(ChatFormatting.YELLOW),
                                        false
                                    );
                                }
                                return 1;
                            }))))
                .then(Commands.literal("start")
                    .then(Commands.argument("slot_a", IntegerArgumentType.integer(1, 6))
                        .then(Commands.argument("slot_b", IntegerArgumentType.integer(1, 6))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int slotA = IntegerArgumentType.getInteger(context, "slot_a") - 1;
                                int slotB = IntegerArgumentType.getInteger(context, "slot_b") - 1;
                                NurseryService.StartResult result = NurseryService.getInstance().startProcess(player, slotA, slotB);

                                if (!result.success()) {
                                    context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                    return 0;
                                }

                                context.getSource().sendSuccess(
                                    () -> tr("cmd_started", result.process().slotIndex() + 1, species(result.process().parentA().speciesId()), species(result.process().parentB().speciesId())).withStyle(ChatFormatting.GREEN),
                                    false
                                );
                                return 1;
                            }))))
                .then(Commands.literal("daycare")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        NurseryNetworking.openCommandScreen(player);
                        context.getSource().sendSuccess(
                            () -> tr("cmd_open")
                                .withStyle(ChatFormatting.GREEN),
                            false
                        );
                        return 1;
                    })
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            NurseryNetworking.openCommandScreen(target);
                            context.getSource().sendSuccess(
                                () -> tr("cmd_open_for", target.getGameProfile().getName())
                                    .withStyle(ChatFormatting.GREEN),
                                false
                            );
                            return 1;
                        })))
                .then(Commands.literal("selectyard")
                    .then(Commands.argument("yard", IntegerArgumentType.integer(1, 4))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int yardIndex = IntegerArgumentType.getInteger(context, "yard") - 1;
                            NurseryService.ActionResult result = NurseryService.getInstance().selectYard(player, yardIndex);
                            if (!result.success()) {
                                context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        })))
                .then(Commands.literal("assign")
                    .then(Commands.argument("yard", IntegerArgumentType.integer(1, 4))
                        .then(Commands.argument("party_slot", IntegerArgumentType.integer(1, 6))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int yardIndex = IntegerArgumentType.getInteger(context, "yard") - 1;
                                int partySlot = IntegerArgumentType.getInteger(context, "party_slot") - 1;
                                NurseryService.ActionResult result = NurseryService.getInstance().assignPartyPokemonToYard(player, yardIndex, partySlot);
                                if (!result.success()) {
                                    context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                    return 0;
                                }
                                context.getSource().sendSuccess(() -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.GREEN), false);
                                return 1;
                            }))))
                .then(Commands.literal("yardstart")
                    .then(Commands.argument("yard", IntegerArgumentType.integer(1, 4))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int yardIndex = IntegerArgumentType.getInteger(context, "yard") - 1;
                            NurseryService.ActionResult result = NurseryService.getInstance().startYard(player, yardIndex);
                            if (!result.success()) {
                                context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.GREEN), false);
                            return 1;
                        })))
                .then(Commands.literal("takeyard")
                    .then(Commands.argument("yard", IntegerArgumentType.integer(1, 4))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int yardIndex = IntegerArgumentType.getInteger(context, "yard") - 1;
                            NurseryService.ActionResult result = NurseryService.getInstance().takeYard(player, yardIndex);
                            if (!result.success()) {
                                context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.YELLOW), false);
                            return 1;
                        })))
                .then(Commands.literal("claimeggs")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        int yardIndex = NurseryService.getInstance().getDaycareState(player).selectedYardIndex();
                        NurseryService.ClaimEggsResult result = NurseryService.getInstance().claimEggTray(player, yardIndex);
                        if (!result.success()) {
                            context.getSource().sendFailure(NurseryMessages.component(result.message()));
                            return 0;
                        }
                        context.getSource().sendSuccess(() -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.GREEN), false);
                        return 1;
                    }))
                .then(Commands.literal("claim")
                    .then(Commands.argument("process_slot", IntegerArgumentType.integer(1, 6))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int processSlot = IntegerArgumentType.getInteger(context, "process_slot") - 1;
                            NurseryService.ClaimResult result = NurseryService.getInstance().claimProcess(player, processSlot);

                            if (!result.success()) {
                                context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                return 0;
                            }

                            context.getSource().sendSuccess(
                                () -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.GREEN),
                                false
                            );
                            return 1;
                        })))
                .then(Commands.literal("cancel")
                    .then(Commands.argument("process_slot", IntegerArgumentType.integer(1, 6))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int processSlot = IntegerArgumentType.getInteger(context, "process_slot") - 1;
                            NurseryService.ActionResult result = NurseryService.getInstance().cancelProcess(player, processSlot);

                            if (!result.success()) {
                                context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                return 0;
                            }

                            context.getSource().sendSuccess(
                                () -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.YELLOW),
                                false
                            );
                            return 1;
                        })))
                .then(Commands.literal("finish")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("process_slot", IntegerArgumentType.integer(1, 6))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int processSlot = IntegerArgumentType.getInteger(context, "process_slot") - 1;
                            NurseryService.ActionResult result = NurseryService.getInstance().forceReadyProcess(player, processSlot);

                            if (!result.success()) {
                                context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                return 0;
                            }

                            context.getSource().sendSuccess(
                                () -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.AQUA),
                                false
                            );
                            return 1;
                        })))
                .then(Commands.literal("finishyard")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("yard", IntegerArgumentType.integer(1, 4))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int yardIndex = IntegerArgumentType.getInteger(context, "yard") - 1;
                            NurseryService.ActionResult result = NurseryService.getInstance().forceReadyYard(player, yardIndex);
                            if (!result.success()) {
                                context.getSource().sendFailure(NurseryMessages.component(result.message()));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> NurseryMessages.component(result.message()).withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        })))
        ));
    }

    private static net.minecraft.network.chat.MutableComponent tr(String key, Object... args) {
        return Component.translatable("gui.cobblemondaycare.legacy." + key, args);
    }

    private static Component species(String id) {
        return Component.translatableWithFallback("cobblemon.species." + shortSpecies(id) + ".name", shortSpecies(id));
    }

    private static Component nature(String id) {
        return Component.translatableWithFallback("cobblemon.nature." + shortSpecies(id), shortSpecies(id));
    }

    private static Component yardState(String state) {
        String key = switch (state) {
            case "LOCKED" -> "locked_info";
            case "EMPTY" -> "empty";
            case "INCOMPLETE" -> "needs_parent";
            case "READY_TO_START" -> "ready";
            case "BREEDING" -> "breeding";
            case "PAUSED_NO_SPACE" -> "full";
            default -> "incompatible";
        };
        return Component.translatable("gui.cobblemondaycare.pc." + key);
    }

    private static Component parentSummary(String raw) {
        var parts = raw.split(" \\+ ", -1);
        return parts.length == 2 ? species(parts[0]).copy().append(" + ").append(species(parts[1])) : Component.literal(raw);
    }

    private static Component shortPair(ParentSnapshot parentA, ParentSnapshot parentB) {
        if (parentA == null) return parentB == null ? Component.empty() : species(parentB.speciesId());
        var left = species(parentA.speciesId()).copy();
        return parentB == null ? left : left.append(" + ").append(species(parentB.speciesId()));
    }

    private static String shortSpecies(String speciesId) {
        int separator = speciesId.indexOf(':');
        return separator >= 0 ? speciesId.substring(separator + 1) : speciesId;
    }
}
