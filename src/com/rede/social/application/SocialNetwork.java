package com.rede.social.application;

import com.rede.social.exception.database.DBException;
import com.rede.social.exception.global.AlreadyExistsError;
import com.rede.social.exception.global.NotFoundError;
import com.rede.social.exception.interactionException.InteractionDuplicatedError;
import com.rede.social.exception.interactionException.PostUnauthorizedError;
import com.rede.social.exception.requestException.FriendshipAlreadyExistsError;
import com.rede.social.exception.profileException.ProfileAlreadyActivatedError;
import com.rede.social.exception.profileException.ProfileAlreadyDeactivatedError;
import com.rede.social.exception.profileException.ProfileUnauthorizedError;
import com.rede.social.exception.requestException.RequestNotFoundError;
import com.rede.social.model.*;
import com.rede.social.model.enums.InteractionType;
import com.rede.social.repository.IPostRepository;
import com.rede.social.repository.IProfileRepository;
import com.rede.social.util.JsonFileHandler;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class SocialNetwork {
    private Map<Profile, Profile> pendingFriendRequests;
    private List<Interaction> interactions;
    private IPostRepository postRepository;
    private IProfileRepository profileRepository;

    public SocialNetwork(IPostRepository postRepository, IProfileRepository profileRepository) {
        this.postRepository = postRepository;
        this.profileRepository = profileRepository;
        this.pendingFriendRequests = new HashMap<>();
        this.interactions = new ArrayList<>();
    }

    public void saveProfiles() throws IOException, DBException {
        JsonFileHandler.saveProfilesToFile(profileRepository.getAllProfiles(), "profiles.json");
    }

    public void savePosts() throws IOException, DBException {
        JsonFileHandler.savePostsToFile(postRepository.listPosts(), "posts.json");
    }

    /**
     * Método responsável por executar a lógica de criar um post
     * @param content o conteúdo do post a ser criado
     * @param owner a instância de perfil que representa o dono do post
     * @return uma nova instância de Post, com o id gerado baseado na quantidade de posts existentes
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public Post createPost(String content, Profile owner) throws DBException {
        int lastId = this.getLastPostId();
        Post post = new Post(lastId+1, content, "PN", owner);
        owner.addPost(post);
        return post;
    }

    /**
     * Método responsável por executar a lógica de criar um post avançado
     * @param content o conteúdo do post a ser criado
     * @param owner a instância de perfil que representa o dono do post
     * @return uma nova instância de AdvancedPost, com o id gerado baseado na quantidade de posts existentes
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public AdvancedPost createAdvancedPost(String content, Profile owner) throws DBException {
        int lastId = this.getLastPostId();
        AdvancedPost advancedPost = new AdvancedPost(lastId+1, content, "PA", owner);
        owner.addPost(advancedPost);
        return advancedPost;
    }

    public int getLastPostId() throws DBException {
        List<Post> posts = this.postRepository.listPosts();
        return posts.stream()
                .mapToInt(Post::getId)  // Converte a stream para uma de inteiros
                .max()                  // Pega o maior ID
                .orElse(0);       // Retorna 0 se a lista estiver vazia
    }

    /**
     * Método que encapsula a lógica de adicionar um post no repositório de posts
     * @param post uma instância de Post a ser adicionada no repositório
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public void addPost(Post post) throws DBException {
        this.postRepository.addPost(post);
    }

    /**
     * Método que encapsula a lógica de recuperar todos os posts através do repositório de posts
     * @return retorna uma lista com todos os posts cadastrados
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public List<Post> listPosts() throws DBException {
        return this.postRepository.listPosts();
    }

    /**
     * Método que encapsula a lógica de recuperar todos os posts de um dado Perfil, através do repositório de posts
     * @param usernameOwner uma String que representa o nome do dono dos posts
     * @return uma lista de posts pertencentes ao dono do perfil
     * @throws NotFoundError no caso do perfil não ser encontrado
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public List<Post> listPostsByProfile(String usernameOwner) throws NotFoundError, DBException {
        return this.postRepository.listPostsByProfile(usernameOwner);
    }

    /**
     * Método responsável por executar a lógica de criar um perfil
     * @param username o nome de usuário do perfil a ser criado
     * @param photo o emoji do perfil a ser criado
     * @param email o email do perfil a ser criado
     * @return uma instância de perfil, com o id gerado baseado na quantidade de perfis existentes
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public Profile createProfile(String username, String photo, String email) throws DBException {
        int lastId = this.getLastProfileId();
        return new Profile(lastId+1, username, photo, email, "PN");
    }

    /**
     * Método responsável por executar a lógica de criar um perfil avançado
     * @param username o nome de usuário do perfil a ser criado
     * @param photo o emoji do perfil a ser criado
     * @param email o email do perfil a ser criado
     * @return uma instância de perfil avançado, com o id gerado baseado na quantidade de perfis existentes
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public AdvancedProfile createAdvancedProfile(String username, String photo, String email) throws DBException {
        int lastIndex = this.getLastProfileId();
        return new AdvancedProfile(lastIndex+1, username, photo, email, "PA");
    }

    public int getLastProfileId() throws DBException {
        List<Profile> posts = this.profileRepository.getAllProfiles();
        return posts.stream()
                .mapToInt(Profile::getId)   // Converte a stream para uma de inteiros
                .max()                      // Pega o maior ID
                .orElse(0);           // Retorna 0 se a lista estiver vazia
    }

    /**
     * Método que encapsula a lógica de adicionar um perfil no repositório de perfis
     * @param profile uma instância de perfil a ser adicionada no repositório
     * @throws AlreadyExistsError se o perfil já existe no repositório de perfis
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public void addProfile(Profile profile) throws AlreadyExistsError, DBException {
        profileRepository.addProfile(profile);
    }

    /**
     * Método que encapsula a lógica de buscar perfil por id informado
     * @param id o id do perfil a ser buscado no repositório
     * @throws NotFoundError se o perfil não for encontrado
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public Profile findProfileById(Integer id) throws NotFoundError, DBException {
        Optional<Profile> founded = profileRepository.findProfileById(id);
        return founded.orElseThrow(() -> new NotFoundError("!Nao foi encontrado perfil de id: " + id));
    }

    /**
     * Método que encapsula a lógica de buscar perfil por email informado
     * @param email o email do perfil a ser buscado no repositório
     * @throws NotFoundError se o perfil não for encontrado
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public Profile findProfileByEmail(String email) throws NotFoundError, DBException {
        Optional<Profile> founded = profileRepository.findProfileByEmail(email);
        return founded.orElseThrow(() -> new NotFoundError("!Nao foi encontrado perfil de email: " + email));
    }

    /**
     * Método que encapsula a lógica de buscar perfil por nome de usuário informado
     * @param username o nome de usuário do perfil a ser buscado no repositório
     * @throws NotFoundError se o perfil não for encontrado
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public Profile findProfileByUsername(String username) throws NotFoundError, DBException {
        Optional<Profile> founded = profileRepository.findProfileByUsername(username);
        return founded.orElseThrow(() -> new NotFoundError("!Nao foi encontrado perfil de username: " + username));
    }

    // TODO: documentar este método
    public List<Profile> listProfile() throws DBException {
        return this.profileRepository.getAllProfiles();
    }

    /**
     * Método para ativar perfil se o perfil for instância de AdvancedProfile
     * @param username o nome de usuário do perfil a ser buscado no repositório
     * @throws NotFoundError se o perfil não for encontrado
     * @throws ProfileUnauthorizedError se o perfil encontrado não for instância de perfil avançado
     * @throws ProfileAlreadyActivatedError se o perfil encontrado já estiver ativo
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public void activateProfile(String username) throws NotFoundError, ProfileUnauthorizedError, ProfileAlreadyActivatedError, DBException {
        Optional<Profile> optionalProfile = profileRepository.findProfileByUsername(username);
        Profile profile = optionalProfile.get();
        if (!(profile instanceof AdvancedProfile)) {
            throw new ProfileUnauthorizedError("Somente perfis avançados podem ativar/desativar perfis.");
        }
        AdvancedProfile advancedProfile = (AdvancedProfile) profile;
        if (advancedProfile.getStatus()) throw new ProfileAlreadyActivatedError("O perfil do " + username + " ja esta ativo.");
        advancedProfile.setStatus(true);
    }

    /**
     * Método para desativar perfil se o perfil for instância de AdvancedProfile
     * @param username o nome de usuário do perfil a ser buscado no repositório
     * @throws NotFoundError se o perfil não for encontrado
     * @throws ProfileUnauthorizedError se o perfil encontrado não for instância de perfil avançado
     * @throws ProfileAlreadyDeactivatedError se o perfil encontrado já estiver inativo
    // TODO: adicionar throws DBException e atualizar documentação caso haja erro na comunicação com o banco de dados
     */
    public void unactivateProfile(String username) throws NotFoundError, ProfileUnauthorizedError, ProfileAlreadyDeactivatedError, DBException {
        Optional<Profile> optionalProfile = profileRepository.findProfileByUsername(username);
        Profile profile = optionalProfile.get();
        if (!(profile instanceof AdvancedProfile)) {
            throw new ProfileUnauthorizedError("Somente perfis avançados podem ativar/desativar perfis.");
        }
        AdvancedProfile advancedProfile = (AdvancedProfile) profile;
        if (!advancedProfile.getStatus()) throw new ProfileAlreadyDeactivatedError("O perfil do " + username + " ja esta inativo.");
        advancedProfile.setStatus(false);
        // TODO: chamar banco para atualizar lá o status
    }

    /**
     * Método responsável por enviar uma solicitação de amizade de um perfil para outro
     * @param usernameApplicant nome do usuário que está enviando a solicitação
     * @param usernameReceiver nome do usuário que irá receber a solicitação
     * @throws NotFoundError caso um dos perfis informados não seja encontrado
     * @throws AlreadyExistsError caso a solicitação ja existe nas solicitações pendentes
     * @throws FriendshipAlreadyExistsError caso os perfis já tenham amizade
     */
    public void sendRequest(String usernameApplicant, String usernameReceiver) throws NotFoundError, AlreadyExistsError, FriendshipAlreadyExistsError, DBException {
        Profile applicant = this.profileRepository.findProfileByUsername(usernameApplicant).get();
        Profile receiver = this.profileRepository.findProfileByUsername(usernameReceiver).get();
        if (pendingFriendRequests.containsKey(applicant) && pendingFriendRequests.get(applicant).equals(receiver) ||
                (pendingFriendRequests.containsValue(applicant) && pendingFriendRequests.containsKey(receiver) &&
                        pendingFriendRequests.get(receiver).equals(applicant))) {
            throw new AlreadyExistsError("solicitacao ja existe.");
        }
        if (applicant.getFriends().contains(receiver)){
            throw new FriendshipAlreadyExistsError("esses perfis ja sao amigos");
        }
        pendingFriendRequests.put(applicant, receiver);
    }

    /**
     * Método responsável por aceitar uma solicitação de amizade de um perfil para outro
     * @param usernameApplicant nome do usuário que está enviando a solicitação
     * @param usernameReceiver nome do usuário que irá receber a solicitação
     * @throws NotFoundError caso um dos perfis informados não seja encontrado
     * @throws RequestNotFoundError caso a solicitação que relacionado aos dois perfis não exista
     */
    public void acceptRequest(String usernameApplicant, String usernameReceiver) throws NotFoundError, RequestNotFoundError, DBException {
        Profile applicant = this.profileRepository.findProfileByUsername(usernameApplicant).get();
        Profile receiver = this.profileRepository.findProfileByUsername(usernameReceiver).get();
        if (!pendingFriendRequests.containsKey(applicant) || !pendingFriendRequests.get(applicant).equals(receiver)) {
            throw new RequestNotFoundError("solicitacao de amizade nao encontrada.");
        }
        applicant.addFriend(receiver);
        receiver.addFriend(applicant);
        pendingFriendRequests.remove(applicant);
    }

    /**
     * Método responsável por recusar uma solicitação de amizade de um perfil para outro
     * @param usernameApplicant nome do usuário que está enviando a solicitação
     * @param usernameReceiver nome do usuário que irá receber a solicitação
     * @throws NotFoundError caso um dos perfis informados não seja encontrado
     * @throws RequestNotFoundError caso a solicitação que relacionado aos dois perfis não exista
     */
    public void refuseRequest(String usernameApplicant, String usernameReceiver) throws NotFoundError, RequestNotFoundError, DBException {
        Profile applicant = this.profileRepository.findProfileByUsername(usernameApplicant).get();
        Profile receiver = this.profileRepository.findProfileByUsername(usernameReceiver).get();
        if (!pendingFriendRequests.containsKey(applicant) || !pendingFriendRequests.get(applicant).equals(receiver)){
            throw new RequestNotFoundError("solicitacao de amizade nao encontrada.");
        }
        pendingFriendRequests.remove(applicant);
    }

    // TODO: fazer documentação do seguinte método
    public boolean existsPendingFriendRequest() {
        return !this.pendingFriendRequests.isEmpty();
    }

    // TODO: fazer documentação do seguinte método
    public Map<Profile, Profile> getPendingFriendRequests() {
        return this.pendingFriendRequests;
    }

    /**
     * Método responsável criar uma instância de Interaction
     * @param type instância de InteractionType que representa o tipo de interação
     * @param owner a instância de perfil que representa o dono da interação
     * @return uma nova instância de Interaction, com o id gerado baseado na quantidade de interactions existentes
     */
    public Interaction createInteraction(InteractionType type, Profile owner) {
        if (interactions.isEmpty()) {
            return new Interaction(1, type, owner);
        }
        Integer id = interactions.get(interactions.size() - 1).getId() + 1;
        return new Interaction(id, type, owner);
    }

    /**
     * Método responsável por adicionar uma nova interação em um post avançado
     * @param idPost id do post que deve ser inserido a interação
     * @param interaction instância de Interaction que representa a interação que será inserida no post
     * @throws PostUnauthorizedError no caso de o post não ser um post avançado
     * @throws InteractionDuplicatedError no caso de uma inserção duplicada de uma mesma interação de um mesmo perfil
     * @throws NotFoundError no caso do post procurado não ser encontrado
     */
    public void addInteraction(Integer idPost, Interaction interaction) throws PostUnauthorizedError, InteractionDuplicatedError, NotFoundError, DBException {
        Post post = this.postRepository.findPostById(idPost).get();
        if (!(post instanceof AdvancedPost)){
            throw new PostUnauthorizedError("somente posts avancados podem realizar interacoes.");
        }
        AdvancedPost advancedPost = (AdvancedPost) post;
        if (this.interactionAlreadyExists(advancedPost, interaction)){
            throw new InteractionDuplicatedError("interacao ja existe");
        }
        advancedPost.addInteraction(interaction);
    }

    // TODO: fazer documentação dos métodos abaixo
    public boolean existsProfile() {
        try {
            return !profileRepository.getAllProfiles().isEmpty();
        } catch (DBException e) {
        }
        return false;
    }

    // TODO: documentar método
    public boolean existsPost() {
        try {
            return !postRepository.listPosts().isEmpty();
        } catch (DBException e) {
        }
        return false;
    }

    public boolean existsAdvancedProfiles() {
        List<AdvancedProfile> listAdvancedProfile = null;
        try {
            listAdvancedProfile = getAdvancedProfiles();
        } catch (DBException e) {
        }
        return !listAdvancedProfile.isEmpty();
    }

    public List<AdvancedProfile> getAdvancedProfiles() throws DBException {
        List<AdvancedProfile> advancedProfileList = new ArrayList<>();
        for (Profile p : listProfile()) {
            if (p instanceof AdvancedProfile) {
                advancedProfileList.add((AdvancedProfile) p);
            }
        }
        return advancedProfileList;
    }

    // TODO: documentar método
    public boolean existsAdvancedPost() {
        List<AdvancedPost> listAdvancedPosts = null;
        try {
            listAdvancedPosts = getAdvancedPosts();
            return !listAdvancedPosts.isEmpty();
        } catch (DBException e) {
            return false;
        }
    }

    public List<AdvancedPost> getAdvancedPosts() throws DBException {
        List<AdvancedPost> advancedPosts = new ArrayList<>();
        for (Post p : listPosts()) {
            if (p instanceof AdvancedPost) {
                advancedPosts.add((AdvancedPost) p);
            }
        }
        return advancedPosts;
    }

    /**
     * Método axuiliar com a lógica para verificar se uma interação já existe, excencial para evitar interações duplicadas
     * @param advancedPost instância de post avançado que permite interações
     * @param interaction instância da interação que deseja verificar se já existe
     * @return retorna true caso a interação já exista no post avançado ou false caso não exista
     */
    private boolean interactionAlreadyExists(AdvancedPost advancedPost, Interaction interaction) {
        Stream<Interaction> interactionsFromPost = advancedPost.listInteractions().stream();
        return interactionsFromPost.anyMatch( i -> i.getAuthor().equals(interaction.getAuthor()));
    }

    public int getQuantityProfiles() {
        try {
            return listProfile().size();
        } catch (DBException e) {
            return 0;
        }
    }
}