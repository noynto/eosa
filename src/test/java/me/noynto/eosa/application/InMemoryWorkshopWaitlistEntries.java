package me.noynto.eosa.application;

import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopWaitlistEntryId;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;
import me.noynto.eosa.workshop.WorkshopWaitlistEntryProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Liste d'attente en mémoire pour les tests, avec les mêmes filtres et le même ordre que la persistance JDBC.
 */
class InMemoryWorkshopWaitlistEntries implements WorkshopWaitlistEntryProvider {

    private final List<WorkshopWaitlistEntry> entries = new ArrayList<>();

    @Override
    public Stream<WorkshopWaitlistEntryId> readIds(WorkshopSessionId sessionId, String email, String token, Boolean waiting) {
        return entries.stream()
                .filter(entry -> sessionId == null || sessionId.equals(entry.getSessionId()))
                .filter(entry -> email == null || email.equalsIgnoreCase(entry.getEmail()))
                .filter(entry -> token == null || token.equals(entry.getToken()))
                .filter(entry -> waiting == null || waiting == entry.isWaiting())
                .sorted(Comparator.comparing(WorkshopWaitlistEntry::getCreatedAt).thenComparing(entry -> entry.getId().value()))
                .map(WorkshopWaitlistEntry::getId)
                .toList()
                .stream();
    }

    @Override
    public Optional<WorkshopWaitlistEntry> read(WorkshopWaitlistEntryId workshopWaitlistEntryId) {
        return entries.stream().filter(entry -> entry.getId().equals(workshopWaitlistEntryId)).findFirst();
    }

    @Override
    public WorkshopWaitlistEntry write(WorkshopWaitlistEntry workshopWaitlistEntry) {
        if (workshopWaitlistEntry.getId() == null) {
            workshopWaitlistEntry.setId(new WorkshopWaitlistEntryId(UUID.randomUUID().toString()));
        }
        entries.removeIf(entry -> entry.getId().equals(workshopWaitlistEntry.getId()));
        entries.add(workshopWaitlistEntry);
        return workshopWaitlistEntry;
    }

    List<WorkshopWaitlistEntry> all() {
        return entries;
    }

}
