package com.lahvacek.freight_trains.util;

import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class StationAwareList extends ArrayList<StructurePoolElement> {
    public StationAwareList(List<StructurePoolElement> elements) {
        super(elements);
    }

    @Override
    public StructurePoolElement get(int index) {
        StructurePoolElement element = super.get(index);
        StationCandidateTracker.setStationCandidate(isStation(element));
        return element;
    }

    @Override
    public Iterator<StructurePoolElement> iterator() {
        Iterator<StructurePoolElement> delegate = super.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public StructurePoolElement next() {
                StructurePoolElement element = delegate.next();
                StationCandidateTracker.setStationCandidate(isStation(element));
                return element;
            }

            @Override
            public void remove() {
                delegate.remove();
            }
        };
    }

    private static boolean isStation(StructurePoolElement element) {
        return element.toString().contains("createfreighttrains:train_station");
    }
}
