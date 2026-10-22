package com.lahvacek.freight_trains.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(JigsawPlacement.Placer.class)
public class JigsawPlacerMixin {

    @Shadow @Final
    private List<? super PoolElementStructurePiece> pieces;

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

        originalList.removeIf(element -> {
            String name = element.toString();

            // Nádraží: Max 1x ve vesnici. Je v houses poolu, který se vybírá už
            // při prvním rozšíření vesnice, proto zde nesmí být depth limit.
            if (name.contains("createfreighttrains:train_station")) {
                return hasStation;
            }

            // Parcela (připraveno na další krok): Max 1x ve vesnici
            if (name.contains("createfreighttrains:plot_empty")) {
                return hasPlot || depth < 2;
            }

            return false;
        });

        if (!hasStation) {
            StructurePoolElement station = originalList.stream()
                    .filter(element -> element.toString().contains("createfreighttrains:train_station"))
                    .findFirst()
                    .orElse(null);
            if (station != null) {
                originalList.remove(station);
                originalList.add(0, station);
            }
        }

        return originalList;
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