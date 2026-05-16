package com.hibernateide.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibernateide.model.ConnectionProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private static final String FILE = "hibernate-ide-profiles.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private List<ConnectionProfile> profiles = new ArrayList<>();

    @PostConstruct
    public void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try {
            profiles = mapper.readValue(f, new TypeReference<List<ConnectionProfile>>() {});
            log.info("Loaded {} profile(s) from {}", profiles.size(), FILE);
        } catch (Exception e) {
            log.warn("Could not load profiles from {}: {}", FILE, e.getMessage());
        }
    }

    public List<ConnectionProfile> getAll() {
        return new ArrayList<>(profiles);
    }

    public void save(ConnectionProfile incoming) {
        profiles.removeIf(p -> p.getName().equals(incoming.getName()));
        profiles.add(incoming);
        persist();
    }

    public void updatePaths(String name, List<String> hbmPaths, List<String> classesPaths) {
        profiles.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .ifPresent(p -> {
                    p.setHbmPaths(hbmPaths);
                    p.setClassesPaths(classesPaths);
                    persist();
                });
    }

    public Optional<ConnectionProfile> findByName(String name) {
        return profiles.stream().filter(p -> p.getName().equals(name)).findFirst();
    }

    public boolean delete(String name) {
        boolean removed = profiles.removeIf(p -> p.getName().equals(name));
        if (removed) persist();
        return removed;
    }

    private void persist() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), profiles);
        } catch (Exception e) {
            log.error("Could not save profiles to {}: {}", FILE, e.getMessage());
        }
    }
}
