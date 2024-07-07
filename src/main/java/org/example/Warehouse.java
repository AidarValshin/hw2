package org.example;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Log4j2
@Getter
public class Warehouse extends Thread {

    private final List<Block> storage = new ArrayList<>();
    private final UnboundedQueue<Truck> trucks = new UnboundedQueue<>(); // выбрал данную реализацию, так как не считаю, что будет высокая борьба за ресурсы

    public Warehouse(String name) {
        super(name);
    }

    public Warehouse(String name, Collection<Block> initialStorage) {
        this(name);
        storage.addAll(initialStorage);
    }

    @Override
    public void run() {
        Truck truck;
        while (!currentThread().isInterrupted()) {
            truck = getNextArrivedTruck();
            if (truck == null) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    if (currentThread().isInterrupted()) {

                        break;
                    }
                }
                continue;
            }
            try {

                if (truck.getBlocks().isEmpty()) {
                    loadTruck(truck);
                } else {
                    unloadTruck(truck);
                }
            } finally {
                truck.getIsPerforming().compareAndSet(true, false);
            }
        }
        log.info("Warehouse thread interrupted");

    }

    private void loadTruck(Truck truck) {
        log.info("Loading truck {}", truck.getName());
        Collection<Block> blocksToLoad = getFreeBlocks(truck.getCapacity());
        try {
            sleep(10L * blocksToLoad.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while loading truck", e);
        }
        truck.getBlocks().addAll(blocksToLoad);
        log.info("Truck loaded {}", truck.getName());
    }

    /**
     * Принято, что у склада 1 дверь и надо все грузовики впускать по очереди
     */
    private Collection<Block> getFreeBlocks(int maxItems) {
        //TODO необходимо реализовать потокобезопасную логику по получению свободных блоков
        //TODO 1 блок грузится в 1 грузовик, нельзя клонировать блоки во время загрузки
        List<Block> blocks = new ArrayList<>();
        synchronized (storage) {
            int maxIndex = storage.size() - 1;
            for (int i = maxIndex; i > maxIndex - maxItems; i--) {
                blocks.add(storage.get(i));
                storage.remove(i);
            }
        }
        return blocks;
    }

    /**
     * Принято, что у склада 1 дверь и надо все завозить по очереди
     *
     * @param returnedBlocks
     */
    private void returnBlocksToStorage(List<Block> returnedBlocks) {
        synchronized (storage) {
            storage.addAll(returnedBlocks);
        }
        // реализовать потокобезопасную логику по возврату блоков на склад
    }

    private void unloadTruck(Truck truck) {

        log.info("Unloading truck {}", truck.getName());
        List<Block> arrivedBlocks = truck.getBlocks();
        try {
            sleep(100L * arrivedBlocks.size());
        } catch (InterruptedException e) {
            log.error("Interrupted while unloading truck", e);
        }
        returnBlocksToStorage(arrivedBlocks);
        truck.getBlocks().clear();
        log.info("Truck unloaded {}", truck.getName());

    }

    private Truck getNextArrivedTruck() {
        //TODO необходимо реализовать логику по получению следующего прибывшего грузовика внутри потока склада
        Truck truck = trucks.deq();
        return truck;
    }


    public void arrive(Truck truck) {
        //TODO необходимо реализовать логику по сообщению потоку склада о том, что грузовик приехал
        //TODO так же дождаться разгрузки блоков, при возврате из этого метода - грузовик покинет склад
        if (!truck.getIsPerforming().compareAndSet(false, true)) {
            log.error("truck is not prformed, it means it is performing in another thread");
            return;
        }
        trucks.enq(truck);
        log.debug("Truck {} arrived to warehouse {}", truck, this);
    }
}
