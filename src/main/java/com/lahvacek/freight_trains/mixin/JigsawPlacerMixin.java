package com.lahvacek.freight_trains.mixin;

import com.lahvacek.freight_trains.CreateFreightTrains;
import com.lahvacek.freight_trains.util.StationAwareList;
import com.lahvacek.freight_trains.util.StationCandidateTracker;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(JigsawPlacement.Placer.class)
public class JigsawPlacerMixin {

    @Shadow @Final
    private List<? super PoolElementStructurePiece> pieces;

    @Shadow @Final
    private int maxDepth;

    @Redirect(
        method = "tryPlacingChildren",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getShuffledTemplates(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"
        )
    )
    private List<StructurePoolElement> filterUniqueVillageBuildings(
            StructureTemplatePool pool,
            RandomSource random,
            PoolElementStructurePiece currentPiece,
            MutableObject<?> free,
            int depth
    ) {
        List<StructurePoolElement> originalList = new ArrayList<>(pool.getShuffledTemplates(random));

        boolean hasStation = alreadyPlaced("createfreighttrains:train_station");
        boolean hasPlot = alreadyPlaced("createfreighttrains:plot_empty");
        boolean containsStation = originalList.stream()
                .anyMatch(element -> element.toString().contains("createfreighttrains:train_station"));

        if (containsStation) {
            CreateFreightTrains.LOGGER.info(
                    "Village jigsaw candidate: train station, depth={}, existingStation={}, candidates={}",
                    depth, hasStation, originalList.size());
        }

        originalList.removeIf(element -> {
            String name = element.toString();

            // Nádraží: Max 1x ve vesnici. Je v houses poolu, který se vybírá už
            // při prvním rozšíření vesnice, proto zde nesmí být depth limit.
            if (name.contains("createfreighttrains:train_station")) {
                return hasStation || depth < maxDepth - 1;
            }

            // Parcela (připraveno na další krok): Max 1x ve vesnici
            if (name.contains("createfreighttrains:plot_empty")) {
                return hasPlot || depth < 2;
            }

            return false;
        });

        if (!hasStation && depth >= maxDepth - 1) {
            StructurePoolElement station = originalList.stream()
                    .filter(element -> element.toString().contains("createfreighttrains:train_station"))
                    .findFirst()
                    .orElse(null);
            if (station != null) {
                originalList.remove(station);
                originalList.add(0, station);
                CreateFreightTrains.LOGGER.info(
                        "Prioritized train station village candidate at depth {}", depth);
            }
        }

        return new StationAwareList(originalList);
    }

    @Inject(method = "tryPlacingChildren", at = @At("RETURN"))
    private void logStationPlacementResult(
            PoolElementStructurePiece parent,
            MutableObject<?> free,
            int depth,
            boolean keepJigsaws,
            net.minecraft.world.level.LevelHeightAccessor heightAccessor,
            net.minecraft.world.level.levelgen.RandomState randomState,
            net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup aliasLookup,
            net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings liquidSettings,
            CallbackInfo callbackInfo
    ) {
        boolean stationPlaced = alreadyPlaced("createfreighttrains:train_station");
        if (stationPlaced) {
            CreateFreightTrains.LOGGER.info(
                    "Train station placement result: placed=true, depth={}, pieces={}",
                    depth, pieces.size());
        }
    }

    @Redirect(
        method = "tryPlacingChildren",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/JigsawBlock;canAttach(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo;)Z"
        )
    )
    private boolean logStationAttachment(
            StructureTemplate.StructureBlockInfo parent,
            StructureTemplate.StructureBlockInfo candidate
    ) {
        boolean canAttach = net.minecraft.world.level.block.JigsawBlock.canAttach(parent, candidate);
        boolean stationConnector = isStationConnector(candidate);
        StationCandidateTracker.setStationCandidate(stationConnector && canAttach);
        if (stationConnector) {
            CreateFreightTrains.LOGGER.warn(
                    "Train station jigsaw attachment check: result={}, parent={}, candidate={}",
                    canAttach, describeJigsaw(parent), describeJigsaw(candidate));
        }
        return canAttach;
    }

    @Redirect(
        method = "tryPlacingChildren",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/Shapes;joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z"
        )
    )
    private boolean allowStationCollision(VoxelShape first, VoxelShape second, BooleanOp operation) {
        boolean intersects = Shapes.joinIsNotEmpty(first, second, operation);
        boolean stationCandidate = StationCandidateTracker.isStationCandidate();
        StationCandidateTracker.setStationCandidate(false);
        if (intersects && operation == BooleanOp.ONLY_SECOND && stationCandidate) {
            CreateFreightTrains.LOGGER.warn(
                    "Train station collision ignored at the village edge");
            return false;
        }
        return intersects;
    }

    private boolean isStationConnector(StructureTemplate.StructureBlockInfo info) {
        String name = info.nbt().getString("name");
        String target = info.nbt().getString("target");
        return "minecraft:building_entrance".equals(name) && "minecraft:empty".equals(target);
    }

    private String describeJigsaw(StructureTemplate.StructureBlockInfo info) {
        return "{pos=" + info.pos()
                + ", name=" + info.nbt().getString("name")
                + ", target=" + info.nbt().getString("target")
                + ", pool=" + info.nbt().getString("pool")
                + ", facing=" + net.minecraft.world.level.block.JigsawBlock.getFrontFacing(info.state())
                + "}";
    }

    private boolean alreadyPlaced(String idSubstring) {
        for (Object obj : this.pieces) {
            if (obj instanceof PoolElementStructurePiece piece) {
                if (piece.getElement().toString().contains(idSubstring)) {
                    return true;
                }
            }
        }
        return false;
    }
}