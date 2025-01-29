package com.rede.social.application;
import com.rede.social.exception.global.AlreadyExistsError;
import com.rede.social.exception.global.NotFoundError;
import com.rede.social.exception.interactionException.InteractionDuplicatedError;
import com.rede.social.exception.interactionException.PostUnauthorizedError;
import com.rede.social.exception.profileException.ProfileAlreadyActivatedError;
import com.rede.social.exception.profileException.ProfileAlreadyDeactivatedError;
import com.rede.social.exception.profileException.ProfileUnauthorizedError;
import com.rede.social.exception.requestException.FriendshipAlreadyExistsError;
import com.rede.social.exception.requestException.RequestNotFoundError;
import com.rede.social.model.AdvancedPost;
import com.rede.social.model.Interaction;
import com.rede.social.model.Post;
import com.rede.social.model.Profile;
import com.rede.social.model.enums.InteractionType;
import com.rede.social.util.IOUtil;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public class App {

    Stack<Runnable> viewStack = new Stack<>();
    private SocialNetwork socialNetwork;
    private IOUtil ioUtil;

    public App(SocialNetwork socialNetwork) {
        this.socialNetwork = socialNetwork;
        this.ioUtil = new IOUtil();
    }

    /**
     * Classe utilizada para encapsular a lógica de uma opção do menu
     */
    private class Option {
        String title;
        Runnable callback;
        Supplier<Boolean> canShow;

        /**
         * @param title nome que que representará a opção
         * @param callback função que será executada ao chamar a opção
         * @param canShow função booleana que controla quando essa opção poderá ser exibida
         */
        Option(String title, Runnable callback, Supplier<Boolean> canShow) {
            this.title = title;
            this.callback = callback;
            this.canShow = canShow;
        }

        @Override
        public String toString() {
            return title;
        }

    }

    // criando um menu dinâmico
    private List<Option> options = List.of(
            new Option("adicionar perfil", this::createProfile, () -> true),
            new Option("buscar perfil", this::findProfile, () -> socialNetwork.existsProfile()),
            new Option("listar perfis", this::listAllProfile, () -> socialNetwork.existsProfile()),
            new Option("ativar perfil", this::enableProfile, () -> socialNetwork.existsProfile()),
            new Option("desativar perfil", this::disableProfile, () -> socialNetwork.existsProfile()),
            new Option("adicionar post", this::createPost, () -> socialNetwork.existsProfile()),
            new Option("listar todos os posts", this::listAllPosts, () -> socialNetwork.existsPost()),
            new Option("listar todos os posts por perfil", this::listPostByProfile, () -> socialNetwork.existsPost()),
            new Option("solicitar amizade", this::sendRequest, () -> socialNetwork.existsProfile()),
            new Option("aceitar solicitacao", this::acceptRequest, () -> socialNetwork.existsPendingFriendRequest()),
            new Option("recusar solicitacao", this::refuseRequest, () -> socialNetwork.existsPendingFriendRequest()),
            new Option("adicionar interacao em post", this::addInteraction, () -> socialNetwork.existsAdvancedPost())
    );

    // TODO: documentar métodos

    /**
     * Método que retorna o menu atualizado, com todas as opções possíveis no momento.
     * @param options é uma lista de objetos do tipo {@link Option} e mostra ao usuário as opções no menu
     * */
    public void showMenu(List<Option> options) {
        int numberOption = 0;
        for (Option o : options) {
            System.out.printf("-> %-2d - %s%n", ++numberOption, o.title);
        }
        System.out.printf("-> %-2d - Sair%n", 0);
    }

    /**
     * Método que chama a função presente na opção escolhida pelo usuário, exibindo uma mensagem de erro em caso de opção inválida.
     */
    public void mainMenu() {
        List<Option> optionsToShow = options.stream()
                .filter(op -> op.canShow.get()).toList();
        showMenu(optionsToShow);
        int chosen = ioUtil.getInt("\n> opcao: ");
        if (chosen > optionsToShow.size() || chosen < 0) {
            ioUtil.showMessage("! Informe uma opcao válida !");
            return;
        }

        if (chosen == 0) {
            try {
                socialNetwork.saveProfiles();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            viewStack.pop();
            return;
        }

        // executa a função callback da opção escolhida
        optionsToShow.get(chosen-1).callback.run();
    }

    /**
     * Método que mantém o menu ativo enquanto ele ainda possui opções que podem ser feitas.
     *  A função {@link #mainMenu()} é adicionada logo no início do método, inserindo-a na pilha.
     *  {@code viewStack} é uma pilha que o controla o menu, executando-a por meio do método {@link Runnable#run()}}
     */
    public void run() {
        viewStack.push(this::mainMenu);

        // loop principal do programa
        while (!viewStack.isEmpty()) {
            viewStack.peek().run();
            ioUtil.clearScreen();
        }

        ioUtil.closeScanner();
    }

    // métodos relacionados ao gerenciamento de perfis

    /**
     * Método para criação de perfis, adicionando perfis normais ou avançados na classe {@link #socialNetwork}.
     * Além de solicitar nome, email e foto de perfil, permite a escolha do tipo de perfil (social ou avançado).
     * @throws AlreadyExistsError lançada em caso de já existir um perfil igual ao que está sendo criado.
     */
    public void createProfile() {
        String username = ioUtil.getText("> Insira o seu nome de usuario: ");
        String email = ioUtil.getText("> Insira o seu email: ");
        int chosenPhoto = ioUtil.getInt("> escolha uma foto (1-\uD83D\uDC69\uD83C\uDFFB\u200D\uD83E\uDDB0 2-\uD83D\uDC68\uD83C\uDFFB\u200D\uD83E\uDDB0): ");
        String photo = chosenPhoto == 1? "\uD83D\uDC69\uD83C\uDFFB\u200D\uD83E\uDDB0" : "\uD83D\uDC68\uD83C\uDFFB\u200D\uD83E\uDDB0";
        int typeProfile = ioUtil.getInt("> tipo de perfil: (1-normal, 2-avançado): ");
        Profile newProfile = typeProfile == 1? socialNetwork.createProfile(username, photo, email) :
                socialNetwork.createAdvancedProfile(username, photo, email);
        try {
            socialNetwork.addProfile(newProfile);
        } catch (AlreadyExistsError e) {
            ioUtil.showError("!Ja existe perfil com este nome ou email!");
        }

        ioUtil.showMessage("-> perfil criado com sucesso!");
    }

    /**
     * Método para procurar um perfil na classe {@link #socialNetwork} por username ou email, localizando-o nessa sequência.
     * @throws NotFoundError  Em caso do usuário não ser achado.
     */
    public void findProfile() {
        ioUtil.showMessage(" -> Voce pode fazer a busca por: username ou email <- ");
        String searchTerm = ioUtil.getText("> Insira o username ou email: ");
        Profile profile;
        // tentando encontrar pelo username
        try {
            profile = socialNetwork.findProfileByUsername(searchTerm);
            ioUtil.showMessage("-> usuario encontrado: \n" + profile.toString());
        } catch (NotFoundError e) {
            // tentando encontrar pelo email
            try {
                profile = socialNetwork.findProfileByEmail(searchTerm);
                ioUtil.showMessage("-> usuario encontrado: \n" + profile.toString());
            } catch (NotFoundError ex) {
                ioUtil.showError("!Nao foi encontrado o usuario com esta informacao: " + searchTerm);
            }
        }
    }

    /**
     * Método que lista todos os perfis cadastrados na classe {@link #socialNetwork}.
     * Verifica se existem perfis cadastrados e, em caso de não existir, exibe uma mensagem informando.
     * Caso exista, exibe a lista de perfis presentes.
     */
    public void listAllProfile() {
        List<Profile> profiles = socialNetwork.listProfile();
        if (profiles.isEmpty()) {
            ioUtil.showError("!Nao existe perfis cadastrados!");
            return;
        }
        ioUtil.showMessage("-> Lista de perfis:");
        profiles.forEach(System.out::print);
    }

    /**
     * Método para ativar um perfil avançado, verificando a presença de perfis cadastrados e recebendo o username de um deles
     * @throws NotFoundError caso o username recebido não tenha sido encontrado em nenhum perfil.
     * @throws ProfileUnauthorizedError caso o perfil não seja do tipo avançado.
     * @throws ProfileAlreadyActivatedError caso o perfil já esteja ativo.
     */
    public void enableProfile() {

        // verificando se existe perfis salvos
        if (socialNetwork.listProfile().isEmpty()) {
            ioUtil.showError("!Nao ha perfil cadastrado para poder ativar!");
            return;
        }
        listAllProfile();       // exibe lista de perfis
        String username = ioUtil.getText("> informe o username do perfil a ser ativado: ");
        try {
            socialNetwork.activateProfile(username);
        } catch (NotFoundError e) {
            ioUtil.showError("!Nao foi encontrado perfil com username: " + username);
            return;
        } catch (ProfileUnauthorizedError e) {
            ioUtil.showError("O perfil nao e do tipo avancado, por isso nao sera ativado!");
            return;
        } catch (ProfileAlreadyActivatedError e) {
            ioUtil.showError("O perfil ja esta ativo!");
            return;
        }

        ioUtil.showMessage("-> perfil ativo com sucesso <-");
    }

    /**
     * Método para desativar um perfil da classe {@link #socialNetwork}, recebendo o username escolhido de uma lista de perfis salvos.
     * @throws NotFoundError caso o username recebido não tenha sido encontrado em nenhum perfil.
     * @throws ProfileUnauthorizedError caso o perfil não seja do tipo avançado.
     * @throws ProfileAlreadyActivatedError caso o perfil já esteja desativado.
     */
    public void disableProfile() {

        // verificando se existe perfis salvos
        if (socialNetwork.listProfile().isEmpty()) {
            ioUtil.showError("!Nao ha perfil cadastrado para poder ativar!");
            return;
        }
        listAllProfile();       // exibe lista de perfis
        String username = ioUtil.getText("> informe o username do perfil a ser desativado: ");
        try {
            socialNetwork.unactivateProfile(username);
        } catch (NotFoundError e) {
            ioUtil.showError("!Nao foi encontrado perfil com username: " + username);
            return;
        } catch (ProfileUnauthorizedError e) {
            ioUtil.showError("O perfil nao e do tipo avancado, por isso nao sera desativado!");
            return;
        } catch (ProfileAlreadyDeactivatedError e) {
            ioUtil.showError("!O perfil ja esta desativado!");
            return;
        }

        ioUtil.showMessage("-> perfil desativado com sucesso <-");
    }

    // métodos relacionado ao gerenciamento de publicações

    /**
     * Método para criar um novo post em um perfil, recebendo conteúdo e informações do post.
     * @throws NotFoundError caso o perfil não seja encontrado.
     */
    public void createPost() {
        ioUtil.showMessage("-> informações do perfil <-");
        String username = ioUtil.getText("> insira o username: ");

        try {
            Profile foundByUsername = socialNetwork.findProfileByUsername(username);
            String contentPost = ioUtil.getText("> conteudo do post: ");
            int typePost = ioUtil.getInt("> tipo do post: (1-normal, 2-avançado): ");
            Post newPost = typePost == 1 ? socialNetwork.createPost(contentPost, foundByUsername):
                    socialNetwork.createAdvancedPost(contentPost, foundByUsername);

            socialNetwork.addPost(newPost);
            ioUtil.showMessage("-> novo post adicionado com sucesso ao perfil de " + foundByUsername.getUsername());

        } catch (NotFoundError e) {
            ioUtil.showError(e.getMessage());
        }
    }

    /**
     * Método para listar todas os post na classe {@link #socialNetwork}.
     * Verifica se existem posts e exibe-os, caso haja post cadastrados.
     */
    public void listAllPosts() {
        List<Post> posts = socialNetwork.listPosts();
        if (posts.isEmpty()) {
            ioUtil.showMessage("!Nao ha posts cadastrados!");
            return;
        }

        ioUtil.showMessage("-> FEED com todos os posts <-");
        posts.forEach(this::showPost);
    }

    /**
     * Método para listar todos os posts de um perfil.
     * Solicita o username do perfil, verifica-o na {@link #socialNetwork} e verifica se possui posts cadastrados, retornando-os caso haja.
     * @throws NotFoundError caso o username do perfil não seja encontrado.
     */
    public void listPostByProfile() {
        ioUtil.showMessage("-> informações do perfil <-");
        String username = ioUtil.getText("> insira o username: ");

        try {
            Profile foundByUsername = socialNetwork.findProfileByUsername(username);
            List<Post> postsFromProfile = socialNetwork.listPostsByProfile(username);
            if (postsFromProfile.isEmpty()) {
                ioUtil.showMessage("!O perfil de " + username + " não possui nenhum post!");
                return;
            }
            ioUtil.showMessage("-> posts de " + username + ":");
            postsFromProfile.forEach(this::showPost);

        } catch (NotFoundError e) {
            ioUtil.showError(e.getMessage());
        }
    }

    /**
     * Método que exibe as informações de um post, verificando se o post é uma instância da classe {@link AdvancedPost} e exibindo as interações caso seja.
     * @param post pode ser do tipo {@link Post} ou {@link AdvancedPost} e é o post a ser exibido.
     */
    private void showPost(Post post) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String postFormated;
        if (post instanceof AdvancedPost) {
            Map<InteractionType, Integer> interactionTypeIntegerMap = this.getQuantityInteractionType((AdvancedPost) post);
            postFormated = String.format("""
                ╔═════════╦══════════════════╦══════════════════╦═══════════════════════════════════════════╦════════════════════════════╗
                ║ <ID> %-2d ║ @%-15s ║ %-16s ║ %-40s  ║  %-2d-👍 %-2d-👎 %-2d-😂 %-2d-😲  ║
                ╚═════════╩══════════════════╩══════════════════╩═══════════════════════════════════════════╩════════════════════════════╝
                """, post.getId(), post.getOwner().getUsername(),
                    post.getCreatedAt().format(fmt), post.getContent(),
                    interactionTypeIntegerMap.get(InteractionType.LIKE),
                    interactionTypeIntegerMap.get(InteractionType.DISLIKE),
                    interactionTypeIntegerMap.get(InteractionType.LAUGH),
                    interactionTypeIntegerMap.get(InteractionType.SURPRISE));
            System.out.print(postFormated);
            return;
        }

        postFormated = String.format("""
                ╔═════════╦══════════════════╦══════════════════╦═══════════════════════════════════════════╗
                ║ <ID> %-2d ║ @%-15s ║ %-16s ║ %-40s  ║
                ╚═════════╩══════════════════╩══════════════════╩═══════════════════════════════════════════╝
                """, post.getId(), post.getOwner().getUsername(),
                post.getCreatedAt().format(fmt), post.getContent());
        System.out.print(postFormated);
    }

    /**
     * Método que busca e retorna o número de cada interação em um post do tipo {@link AdvancedPost}.
     * Conta a quantidade de interações( curtida, descurtida, risos e surpresa) e armazena elas em um {@link java.util.Map} do tipo {@link InteractionType}.
     *
     * @param post o {@link AdvancedPost} que terá suas interações contadas.
     * @return um {@link java.util.Map} contendo o número de cada interação feita no post
     */
    private Map<InteractionType, Integer> getQuantityInteractionType(AdvancedPost post) {
        List<Interaction> interactions = post.listInteractions();
        int like = 0, dislike = 0, laugh = 0, surprise = 0;
        for (Interaction interaction : interactions) {
            if (interaction.getType() == InteractionType.LIKE) {
                like++;
                continue;
            }
            if (interaction.getType() == InteractionType.DISLIKE) {
                dislike++;
                continue;
            }
            if (interaction.getType() == InteractionType.LAUGH) {
                laugh++;
                continue;
            }
            if (interaction.getType() == InteractionType.SURPRISE) {
                surprise++;
            }
        }
        Map<InteractionType, Integer> interactionTypeIntegerMap = Map.of(
                InteractionType.LIKE, like,
                InteractionType.DISLIKE, dislike,
                InteractionType.LAUGH, laugh,
                InteractionType.SURPRISE, surprise
        );
        return interactionTypeIntegerMap;
    }

    // métodos relacionado ao gerenciamento de solicitações

    /**
     * Método para enviar solicitações de amizade entre dois usuários, recebendo username de ambos
     * @throws NotFoundError caso um dos usuários não exista.
     * @throws AlreadyExistsError caso a solicitação de amizade já tenha sido feita antes.
     * @throws FriendshipAlreadyExistsError caso os usuários já sejam amigos.
     */
    public void sendRequest() {
        ioUtil.showMessage("-> solicitar amizade <-");
        ioUtil.showMessage(" -- informações do solicitante --");
        String applicantUsername = ioUtil.getText("> username: ");
        ioUtil.showMessage("-- informações do recebedor --");
        String receiverUsername = ioUtil.getText("> username: ");

        try {
            socialNetwork.sendRequest(applicantUsername, receiverUsername);
            ioUtil.showMessage("-> solicitação enviada de " + applicantUsername + " para " + receiverUsername);
        } catch (NotFoundError | AlreadyExistsError | FriendshipAlreadyExistsError e) {
            ioUtil.showError(e.getMessage());
        }
    }

    /**
     * Método que aceita solicitações de amizade entre usuários, exibindo lista de solicitações pendentes.
     * @throws NotFoundError caso algum dos usuários não exista na rede social.
     * @throws RequestNotFoundError caso a solicitação de amizade não tenha sido feita.
     */
    public void acceptRequest() {
        if (!socialNetwork.existsPendingFriendRequest()) {
            ioUtil.showMessage("!Não existe solicitações pendentes!");
            return;
        }

        Map<Profile, Profile> pendingRequests = socialNetwork.getPendingFriendRequests();
        ioUtil.showMessage("-> lista de solicitacoes <-");
        this.showFriendRequests(pendingRequests);

        ioUtil.showMessage("-> informe solicitacao para ser aceita <-");
        String applicantUsername = ioUtil.getText("> username solicitante: ");
        String receiverUsername = ioUtil.getText("> username recebedor: ");

        try {
            socialNetwork.acceptRequest(applicantUsername, receiverUsername);
            ioUtil.showMessage("-> solicitacao aceita, agora " + applicantUsername + " e " + receiverUsername + " sao amigos!");
        } catch (NotFoundError | RequestNotFoundError e) {
            ioUtil.showError(e.getMessage());
        }
    }

    /**
     *  Método que recusa uma solicitação de amizade usuários, exibindo lista de solicitações pendentes.
     *  @throws NotFoundError caso algum dos usuários não exista na rede social.
     *  @throws RequestNotFoundError caso a solicitação de amizade não tenha sido feita.
     */
    public void refuseRequest() {
        if (!socialNetwork.existsPendingFriendRequest()) {
            ioUtil.showMessage("!Não existe solicitações pendentes!");
            return;
        }

        Map<Profile, Profile> pendingRequests = socialNetwork.getPendingFriendRequests();
        ioUtil.showMessage("-> lista de solicitacoes <-");
        this.showFriendRequests(pendingRequests);

        ioUtil.showMessage("-> informe solicitacao para ser recusada <-");
        String applicantUsername = ioUtil.getText("> username solicitante: ");
        String receiverUsername = ioUtil.getText("> username recebedor: ");

        try {
            socialNetwork.refuseRequest(applicantUsername, receiverUsername);
            ioUtil.showMessage("-> solicitacao recusada com sucesso!");
        } catch (NotFoundError | RequestNotFoundError e) {
            ioUtil.showError(e.getMessage());
        }
    }

    /**
     * Método que exibe as solicitações de amizade pendentes, com base num id de solicitação, nome do solicitante e recebedor.
     * @param pendingRequests É um mapa que contém as solicitações pendentes, onde a chave = solicitante e o valor = recebedor.
     */
    private void showFriendRequests(Map<Profile, Profile> pendingRequests) {
        Set<Profile> keys = pendingRequests.keySet();
        int idRequest = 0;
        ioUtil.showMessage("      id        solicitante         recebedor");
        for (Profile applicant : keys) {
            Profile receiver = pendingRequests.get(applicant);
            String profilesFormated = String.format("""
                ╔═══════════╦══════════════════╦══════════════════╗
                ║ <ID> %-4d ║ @%-15s ║ @%-15s ║ 
                ╚═══════════╩══════════════════╩══════════════════╝
                    """, ++idRequest, applicant.getUsername(), receiver.getUsername());
            System.out.print(profilesFormated);
        }
    }


    // métodos relacionado ao gerenciamento de interações

    /**
     * Método que adiciona uma interação a um {@link AdvancedPost}, permitindo escolher post específico de um perfil.
     * @throws NotFoundError caso o perfil ou post não exista.
     * @throws PostUnauthorizedError caso o usuário não tenha permissão para interagir com o post.
     * @throws InteractionDuplicatedError caso a interação já tenha sido realizada no post.
     *
     */
    public void addInteraction() {
        if (!socialNetwork.existsAdvancedPost()) {
            ioUtil.showError("!Nao existe posts avançados para poder interagir!");
        }

        // informações do perfil de quem deseja fazer a interação
        ioUtil.showMessage("-> informacao do perfil que deseja interagir com post <-");
        String username = ioUtil.getText("> username: ");
        Profile owner;
        try {
            owner = socialNetwork.findProfileByUsername(username);
        } catch (NotFoundError e) {
            ioUtil.showError(e.getMessage());
            return;
        }

        // exibir posts avançados que podem receber interações
        List<AdvancedPost> advancedPostList = socialNetwork.getAdvancedPosts();
        ioUtil.showMessage("-> lista de posts avançados <-");
        advancedPostList.forEach(this::showPost);

        ioUtil.showMessage("-> informaçao do post que deseja interagir <-");
        int idPost = ioUtil.getInt("> id do post: ");
        InteractionType interactionType = this.getInteractionType();
        Interaction interaction = socialNetwork.createInteraction(interactionType, owner);

        // tentando criar e adiconar interação
        try {
            socialNetwork.addInteraction(idPost, interaction);
            ioUtil.showMessage("-> interação adiconada com sucesso✅!");
        } catch (PostUnauthorizedError | InteractionDuplicatedError | NotFoundError e) {
            ioUtil.showError(e.getMessage());
        }
    }

    /**
     * Método para escolher um tipo de interação a ser usada.
     * @return o tipo de interação escolhido pelo usuário.
     */
    private InteractionType getInteractionType() {
        ioUtil.showMessage("-> escolha um tipo de interacao:");
        int chosen = ioUtil.getInt("> (1-👍 2-👎 3-😂 4-😲): ");
        if (chosen == 1) return InteractionType.LIKE;
        if (chosen == 2) return InteractionType.DISLIKE;
        if (chosen == 3) return InteractionType.LAUGH;
        return InteractionType.SURPRISE;
    }
}
