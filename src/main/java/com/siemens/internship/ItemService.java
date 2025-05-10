package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private final List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Issues of the original version :
     * - it returned the processedItems list before tasks completed
     * - it used processedItems without synchronization
     * - mixed @Async with executor management
     * <p>
     * The updated version :
     * - processes all items asynchronously
     * - uses CompletableFuture.allOf() to ensure all items are processed before returning the result
     * - uses thread-safe collections
     * - handles exceptions
     * - uses @Async properly
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        processedItems.clear(); // clears the list before processing

        List<Long> itemIds = itemRepository.findAllIds();
        List<CompletableFuture<Item>> futures = new ArrayList<>();

        // for each task id launch an asynchronous task to process it
        for (Long id : itemIds) {
            futures.add(CompletableFuture.supplyAsync(() -> processItem(id)));
        }

        // waits for all the tasks to be completed
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(voided -> {
                    List<Item> result = new ArrayList<>();
                    for (CompletableFuture<Item> future : futures) {
                        try {
                            Item item = future.join();
                            if (item != null) {
                                processedItems.add(item);
                                result.add(item);
                            }
                        } catch (Exception e) {
                            System.err.println("Error joining future: " + e.getMessage());
                        }
                    }
                    return result;
                });
    }

    private Item processItem(Long id) {
        try {
            Thread.sleep(100); // simulate processing time

            Optional<Item> optional = itemRepository.findById(id);
            if (optional.isPresent()) {
                Item item = optional.get();
                item.setStatus("PROCESSED");
                return itemRepository.save(item);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Processing interrupted for item id " + id);
        } catch (Exception e) {
            System.err.println("Failed to process item with id " + id + ": " + e.getMessage());
        }
        return null;
    }

}

