package com.rede.social.repository;

import com.rede.social.exception.database.DBException;
import com.rede.social.exception.global.AlreadyExistsError;
import com.rede.social.exception.global.NotFoundError;
import com.rede.social.model.Profile;

import java.util.List;
import java.util.Optional;

public interface IProfileRepository {
    /**
     * Método que adiciona uma instância de Profile no repositório de Profiles
     * @param profile perfil a ser adicionado
     * @throws AlreadyExistsError no caso de ja existir perfil com o mesmo id, username ou email
     * @throws DBException caso ocorra falha na comunicaçao com a base de dados
     */
    void addProfile(Profile profile) throws AlreadyExistsError, DBException;

    /**
     * Método que busca e retorna um perfil baseado na string que representa o email do perfil
     * @param email o email do perfil a ser buscado
     * @return o perfil buscado, se encontrado
     * @throws NotFoundError no caso de não existir um perfil com este email
     * @throws DBException caso ocorra falha na comunicaçao com a base de dados
     */
    Optional<Profile> findProfileByEmail(String email) throws NotFoundError, DBException;

    /**
     * Método que busca e retorna um perfil baseado na string que representa o username do perfil
     * @param username o username do perfil a ser buscado
     * @return o perfil buscado, se encontrado
     * @throws NotFoundError no caso de não existir um perfil com este username
     * @throws DBException caso ocorra falha na comunicaçao com a base de dados
     */
    Optional<Profile> findProfileByUsername(String username) throws NotFoundError, DBException;

    /**
     * Método que busca e retorna um perfil baseado no int que representa o id do perfil
     * @param id o id do perfil a ser buscado
     * @return o perfil buscado, se encontrado
     * @throws NotFoundError no caso de não existir um perfil com este id
     * @throws DBException caso ocorra falha na comunicaçao com a base de dados
     */
    Optional<Profile> findProfileById(Integer id) throws NotFoundError, DBException;

    /**
     * Esse método é utilizado para retornar todos os perfis armazenados
     * @return todos os perfis criados
     * @throws DBException caso ocorra falha na comunicaçao com a base de dados
     */
    List<Profile> getAllProfiles() throws DBException;
}
