package com.rede.social.repository.impl;

import com.rede.social.exception.global.AlreadyExistsError;
import com.rede.social.exception.global.NotFoundError;
import com.rede.social.model.AdvancedProfile;
import com.rede.social.model.Profile;
import com.rede.social.repository.IProfileRepository;
import com.rede.social.util.JsonFileHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileRepositoryImplFile implements IProfileRepository {
    private final List<Profile> profiles;

    public ProfileRepositoryImplFile() {
        this.profiles = new ArrayList<>();
        loadProfiles();
    }

    // Carregar perfis do arquivo JSON
    private void loadProfiles() {
        try {
            List<Profile> loadedProfiles = JsonFileHandler.loadProfilesFromFile("profiles.json");
            List<Profile> listToSave = loadedProfiles.stream()
                    .map(p -> p.getType().equals("PN") ? new Profile(p.getId(), p.getUsername(),
                            p.getPhoto(), p.getEmail(), p.getType()) : new AdvancedProfile(p.getId(), p.getUsername(),
                            p.getPhoto(), p.getEmail(), p.getType()))
                    .toList();
            if (loadedProfiles != null) {
                profiles.addAll(listToSave);
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void addProfile(Profile profile) throws AlreadyExistsError {
        Boolean exists = profiles.stream().anyMatch(p -> p.getId() == profile.getId() || p.getEmail().equals(profile.getEmail()) || p.getUsername().equals(profile.getUsername()));
        if (exists) throw new AlreadyExistsError("Ja existe um perfil com este username, email ou id");
        profiles.add(profile);
    }

    @Override
    public Optional<Profile> findProfileByEmail(String email) throws NotFoundError {
        for (Profile profile : profiles) {
            if (profile.getEmail().equals(email)) {
                return Optional.of(profile);
            }
        }
        throw new NotFoundError("nao foi encontrado perfil com email: " + email);
    }

    @Override
    public Optional<Profile> findProfileByUsername(String username) throws NotFoundError {
        for (Profile profile : profiles) {
            if (profile.getUsername().equals(username)) {
                return Optional.of(profile);
            }
        }
        throw new NotFoundError("nao foi encontrado perfil com username: " + username);
    }

    public Optional<Profile> findProfileById(Integer id) throws NotFoundError {
        for (Profile profile : profiles) {
            if (profile.getId() == id) {
                return Optional.of(profile);
            }
        }
        throw new NotFoundError("nao foi encontrado perfil com id: " + id);
    }

    @Override
    public List<Profile> getAllProfiles() {
        return profiles;
    }
}
