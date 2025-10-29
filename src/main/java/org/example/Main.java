package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends TelegramLongPollingBot {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");
    private static final String BOT_USERNAME = dotenv.get("BOT_USERNAME");
    
    private static Long GRUPO_ATENDIMENTO_ID = null;

    private static final String SENHA_ATENDENTE = dotenv.get("SENHA_ATENDENTE");
    private static final java.util.Set<Long> idsAtendentes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static final long TIMEOUT_INATIVIDADE = 5 * 60 * 1000;

    private final Map<Long, String> estadoUsuario = new HashMap<>();
    private final Map<Long, DadosPedido> pedidosUsuario = new HashMap<>();
    private final Map<Long, Timer> timersInatividade = new ConcurrentHashMap<>();
    private final Map<Long, Long> clientesEmAtendimento = new ConcurrentHashMap<>();
    private final Map<Long, String> nomesClientes = new ConcurrentHashMap<>();

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String texto = update.getMessage().getText();
            
            String nomeUsuario = "";
            if (update.getMessage().getFrom().getFirstName() != null) {
                nomeUsuario = update.getMessage().getFrom().getFirstName();
                if (update.getMessage().getFrom().getLastName() != null) {
                    nomeUsuario += " " + update.getMessage().getFrom().getLastName();
                }
            }
            
            if (nomeUsuario != null && !nomeUsuario.isEmpty()) {
                nomesClientes.put(chatId, nomeUsuario);
            }

            cancelarTimerInatividade(chatId);

            if (texto.equals("/id_grupo")) {
                enviarMensagem(chatId, "🆔 ID deste chat/grupo: `" + chatId + "`\n\n" +
                        "Para configurar como grupo de atendimento, use:\n" +
                        "`/configurar_grupo " + chatId + " SENHA`");
                return;
            }
            
            if (texto.startsWith("/configurar_grupo")) {
                configurarGrupoAtendimento(chatId, texto);
                return;
            }

            if (texto.equals("/meu_id")) {
                enviarMensagem(chatId, "🆔 Seu ID do Telegram é: `" + chatId + "`");
                return;
            }

            if (texto.startsWith("/cadastrar_atendente")) {
                cadastrarAtendente(chatId, texto);
                return;
            }

            if (texto.equals("/listar_atendentes") && isAtendente(chatId)) {
                listarAtendentes(chatId);
                return;
            }

            if (texto.equals("/start")) {
                enviarMenuPrincipal(chatId);
                estadoUsuario.put(chatId, "MENU");
                return;
            }

            if (texto.equalsIgnoreCase("/finalizar")) {
                finalizarAtendimentoHumano(chatId);
                return;
            }

            if (GRUPO_ATENDIMENTO_ID != null && chatId.equals(GRUPO_ATENDIMENTO_ID) && 
                texto.startsWith("/responder ")) {
                responderClienteDoGrupo(chatId, texto, nomeUsuario);
                return;
            }

            if (clientesEmAtendimento.containsKey(chatId)) {
                encaminharMensagemParaGrupo(chatId, texto, nomeUsuario);
                return;
            }

            String estado = estadoUsuario.getOrDefault(chatId, "MENU");
            processarMensagem(chatId, texto, estado);
        }
    }

    private void processarMensagem(Long chatId, String texto, String estado) {
        switch (estado) {
            case "MENU":
                processarMenu(chatId, texto);
                break;
            case "AGUARDANDO_PEDIDO":
                receberPedido(chatId, texto);
                break;
            case "AGUARDANDO_CEP":
                receberCEP(chatId, texto);
                break;
            case "AGUARDANDO_NUMERO":
                receberNumero(chatId, texto);
                break;
            case "AGUARDANDO_COMPLEMENTO":
                receberComplemento(chatId, texto);
                break;
            case "AGUARDANDO_PAGAMENTO":
                receberPagamento(chatId, texto);
                break;
            case "AGUARDANDO_CONTINUAR":
                receberContinuar(chatId, texto);
                break;
        }
    }

    private void enviarMenuPrincipal(Long chatId) {
        String menu = "🌾 *Bem-vindo ao Quitutes da Granja!*\n\n" +
                "Como posso ajudar?\n\n" +
                "1. Ver catálogo\n" +
                "2. Ofertas do dia\n" +
                "3. Fazer pedido\n" +
                "4. Informações da loja\n" +
                "5. Falar com atendente\n\n" +
                "Digite a opção desejada:\n\n";
        enviarMensagem(chatId, menu);
    }

    private void processarMenu(Long chatId, String opcao) {
        switch (opcao) {
            case "1":
                enviarCatalogo(chatId);
                break;
            case "2":
                enviarOfertas(chatId);
                break;
            case "3":
                enviarMensagem(chatId, "📝 Digite seu pedido:");
                estadoUsuario.put(chatId, "AGUARDANDO_PEDIDO");
                pedidosUsuario.put(chatId, new DadosPedido());
                break;
            case "4":
                enviarInformacoes(chatId);
                perguntarContinuar(chatId);
                break;
            case "5":
                falarComAtendente(chatId);
                break;
            default:
                enviarMensagem(chatId, "❌ Opção inválida. Digite 1, 2, 3, 4 ou 5:");
        }
    }

    private void configurarGrupoAtendimento(Long chatId, String comando) {
        // Formato: /configurar_grupo -123456789 SENHA
        String[] partes = comando.split(" ", 3);
        
        if (partes.length < 3) {
            enviarMensagem(chatId, "❌ Formato incorreto!\n\n" +
                    "Use: /configurar_grupo ID_DO_GRUPO SENHA\n\n" +
                    "Para descobrir o ID do grupo:\n" +
                    "1. Adicione o bot ao grupo\n" +
                    "2. No grupo, envie /id_grupo");
            return;
        }
        
        try {
            Long grupoId = Long.parseLong(partes[1]);
            String senhaInformada = partes[2];
            
            if (senhaInformada.equals(SENHA_ATENDENTE)) {
                GRUPO_ATENDIMENTO_ID = grupoId;
                enviarMensagem(chatId, "✅ *Grupo de atendimento configurado!*\n\n" +
                        "🆔 ID do grupo: `" + grupoId + "`\n\n" +
                        "Agora todas as mensagens dos clientes serão enviadas para este grupo!");
                
                // Envia confirmação também no grupo
                enviarMensagem(grupoId, "✅ *Este grupo foi configurado como grupo de atendimento!*\n\n" +
                        "📋 Para responder um cliente, use:\n" +
                        "`/responder ID_CLIENTE mensagem`");
                
                System.out.println("✅ Grupo de atendimento configurado: " + grupoId);
            } else {
                enviarMensagem(chatId, "❌ Senha incorreta! Tente novamente.");
            }
        } catch (NumberFormatException e) {
            enviarMensagem(chatId, "❌ ID do grupo inválido! Deve ser um número.");
        }
    }

    private void cadastrarAtendente(Long chatId, String comando) {
        // Formato: /cadastrar_atendente SENHA
        String[] partes = comando.split(" ", 2);
        
        if (partes.length < 2) {
            enviarMensagem(chatId, "❌ Formato incorreto!\n\nUse: /cadastrar_atendente SENHA");
            return;
        }
        
        String senhaInformada = partes[1];
        
        if (senhaInformada.equals(SENHA_ATENDENTE)) {
            idsAtendentes.add(chatId);
            enviarMensagem(chatId, "✅ *Atendente cadastrado com sucesso!*\n\n" +
                    "🆔 Seu ID: `" + chatId + "`\n\n" +
                    "📋 *Comandos disponíveis:*\n" +
                    "• `/listar_atendentes` - Ver todos os atendentes\n\n" +
                    "Você receberá notificações quando houver atendimentos!");
            
            System.out.println("✅ Novo atendente cadastrado: " + chatId);
        } else {
            enviarMensagem(chatId, "❌ Senha incorreta! Tente novamente.");
            System.out.println("⚠️ Tentativa de cadastro com senha incorreta: " + chatId);
        }
    }

    private void listarAtendentes(Long chatId) {
        if (idsAtendentes.isEmpty()) {
            enviarMensagem(chatId, "ℹ️ Nenhum atendente cadastrado ainda.");
            return;
        }
        
        StringBuilder lista = new StringBuilder();
        lista.append("👥 *ATENDENTES CADASTRADOS*\n\n");
        
        int contador = 1;
        for (Long id : idsAtendentes) {
            lista.append(contador++).append(". ID: `").append(id).append("`\n");
        }
        
        lista.append("\n_Total: ").append(idsAtendentes.size()).append(" atendente(s)_");
        
        enviarMensagem(chatId, lista.toString());
    }

    private void falarComAtendente(Long chatId) {
        if (GRUPO_ATENDIMENTO_ID == null) {
            enviarMensagem(chatId, "⚠️ *Desculpe, o atendimento humano não está disponível no momento.*\n\n" +
                    "O grupo de atendimento ainda não foi configurado. Por favor, utilize as outras opções do menu.");
            perguntarContinuar(chatId);
            return;
        }
        
        enviarMensagem(chatId, "👤 *Conectando você com um atendente...*\n\n" +
                "Por favor, aguarde. Um atendente irá respondê-lo em breve.\n\n" +
                "Para encerrar o atendimento, digite: /finalizar");
        
        // Marca o cliente como em atendimento humano
        estadoUsuario.put(chatId, "ATENDIMENTO_HUMANO");
        clientesEmAtendimento.put(chatId, GRUPO_ATENDIMENTO_ID);
        
        // Notifica o grupo de atendimento
        notificarGrupoAtendimento(chatId);
    }

    private void notificarGrupoAtendimento(Long clienteChatId) {
        String nomeCliente = nomesClientes.getOrDefault(clienteChatId, "Cliente");
        
        String mensagemGrupo = "🔔 *NOVO ATENDIMENTO SOLICITADO*\n\n" +
                "👤 Cliente: *" + nomeCliente + "*\n" +
                "🆔 ID: `" + clienteChatId + "`\n" +
                "⏰ Horário: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "\n\n" +
                "💬 Aguardando mensagem do cliente...\n\n" +
                "📋 Para responder:\n" +
                "`/responder " + clienteChatId + " sua mensagem aqui`";
        
        enviarMensagem(GRUPO_ATENDIMENTO_ID, mensagemGrupo);
        System.out.println("📢 Notificação enviada ao grupo sobre cliente " + clienteChatId);
    }

    private void encaminharMensagemParaGrupo(Long clienteChatId, String mensagem, String nomeCliente) {
        // Salva o nome do cliente
        if (nomeCliente != null && !nomeCliente.isEmpty()) {
            nomesClientes.put(clienteChatId, nomeCliente);
        }
        
        String nomeExibir = nomesClientes.getOrDefault(clienteChatId, "Cliente");
        
        String mensagemFormatada = "💬 *" + nomeExibir + "* (ID: `" + clienteChatId + "`):\n\n" +
                "\"" + mensagem + "\"\n\n" +
                "_Para responder: /responder " + clienteChatId + " sua resposta_";
        
        enviarMensagem(GRUPO_ATENDIMENTO_ID, mensagemFormatada);
    }

    private void responderClienteDoGrupo(Long grupoChatId, String comando, String nomeAtendente) {
        String[] partes = comando.split(" ", 3);
        
        if (partes.length < 3) {
            enviarMensagem(grupoChatId, "❌ Formato incorreto!\n\nUse: /responder ID_CLIENTE mensagem");
            return;
        }
        
        try {
            Long clienteChatId = Long.parseLong(partes[1]);
            String resposta = partes[2];
            
            if (!clientesEmAtendimento.containsKey(clienteChatId)) {
                enviarMensagem(grupoChatId, "⚠️ Este cliente não está em atendimento no momento.");
                return;
            }
            
            String nomeExibir = (nomeAtendente != null && !nomeAtendente.isEmpty()) ? nomeAtendente : "Atendente";
            
            enviarMensagem(clienteChatId, "👤 *" + nomeExibir + ":* " + resposta);
            
            String nomeCliente = nomesClientes.getOrDefault(clienteChatId, "Cliente");
            enviarMensagem(grupoChatId, "✅ Mensagem enviada para *" + nomeCliente + "* (ID: " + clienteChatId + ")");
            
        } catch (NumberFormatException e) {
            enviarMensagem(grupoChatId, "❌ ID do cliente inválido!");
        }
    }

    private void finalizarAtendimentoHumano(Long chatId) {
        if (clientesEmAtendimento.containsKey(chatId)) {
            String nomeCliente = nomesClientes.getOrDefault(chatId, "Cliente");
            
            clientesEmAtendimento.remove(chatId);
            nomesClientes.remove(chatId);
            
            enviarMensagem(chatId, "✅ *Atendimento finalizado!*\n\nObrigado por entrar em contato.");
            
            if (GRUPO_ATENDIMENTO_ID != null) {
                enviarMensagem(GRUPO_ATENDIMENTO_ID, "ℹ️ *" + nomeCliente + "* (ID: `" + chatId + "`) finalizou o atendimento.");
            }
            
            perguntarContinuar(chatId);
            
        } else {
            enviarMensagem(chatId, "ℹ️ Você não está em atendimento no momento.");
        }
    }

    private boolean isAtendente(Long chatId) {
        return idsAtendentes.contains(chatId);
    }

    private void enviarCatalogo(Long chatId) {
        enviarMensagem(chatId, "📄 Enviando catálogo...");

        File catalogo = new File("menu-quitutes.pdf");

        if (catalogo.exists()) {
            enviarPDF(chatId, catalogo, "📄 Catálogo - Quitutes da Granja");
        } else {
            enviarMensagem(chatId, "❌ Desculpe, o catálogo não esta disponível no momento, estamos atualizando os itens.\n\n");
        }

        perguntarContinuar(chatId);
    }

    private void enviarOfertas(Long chatId) {
        enviarMensagem(chatId, "🎉 Enviando ofertas do dia...");

        File ofertas = new File("menu-quitutes.pdf");

        if (ofertas.exists()) {
            enviarPDF(chatId, ofertas, "🎉 Ofertas do Dia - Quitutes da Granja");
        } else {
            enviarMensagem(chatId, "❌ Desculpe, as ofertas não estão disponíveis no momento.\n\n");
        }

        perguntarContinuar(chatId);
    }

    private void receberPedido(Long chatId, String pedido) {
        DadosPedido dados = pedidosUsuario.get(chatId);
        dados.pedido = pedido;
        enviarMensagem(chatId, "✅ Pedido anotado!\n\n📮 Digite seu CEP:");
        estadoUsuario.put(chatId, "AGUARDANDO_CEP");
    }

    private void receberCEP(Long chatId, String cep) {
        cep = cep.replaceAll("[^0-9]", "");
        if (cep.length() == 8) {
            DadosPedido dados = pedidosUsuario.get(chatId);
            dados.cep = cep;

            if (validarEBuscarCEP(chatId, dados)) {
                enviarMensagem(chatId, "🏠 Digite o número:");
                estadoUsuario.put(chatId, "AGUARDANDO_NUMERO");
            } else {
                enviarMensagem(chatId, "❌ CEP não encontrado! Digite novamente:");
            }
        } else {
            enviarMensagem(chatId, "❌ CEP inválido! Digite 8 números:");
        }
    }

    private boolean validarEBuscarCEP(Long chatId, DadosPedido dados) {
        try {
            String urlString = "https://brasilapi.com.br/api/cep/v2/" + dados.cep;
            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String jsonResponse = response.toString();
                dados.rua = extrairValorJSON(jsonResponse, "street");
                dados.bairro = extrairValorJSON(jsonResponse, "neighborhood");
                dados.cidade = extrairValorJSON(jsonResponse, "city");
                dados.estado = extrairValorJSON(jsonResponse, "state");

                String mensagemCEP = "✅ *CEP válido!*\n\n";
                if (!dados.rua.isEmpty()) {
                    mensagemCEP += "📍 *Endereço:* " + dados.rua + "\n";
                }
                if (!dados.bairro.isEmpty()) {
                    mensagemCEP += "🏘️ *Bairro:* " + dados.bairro + "\n";
                }
                mensagemCEP += "🏙️ *Cidade:* " + dados.cidade + " - " + dados.estado;

                enviarMensagem(chatId, mensagemCEP);
                return true;
            }

            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Erro ao validar o CEP: " + e.getMessage());
        }
        return false;
    }

    private String extrairValorJSON(String json, String chave) {
        try {
            String busca = "\"" + chave + "\":\"";
            int inicio = json.indexOf(busca);
            if (inicio == -1) return "";
            inicio += busca.length();
            int fim = json.indexOf("\"", inicio);
            return json.substring(inicio, fim);
        } catch (Exception e) {
            return "";
        }
    }

    private void receberNumero(Long chatId, String numero) {
        DadosPedido dados = pedidosUsuario.get(chatId);
        dados.numero = numero;
        enviarMensagem(chatId, "🏠 Digite o complemento\n(ou '.' se não houver):");
        estadoUsuario.put(chatId, "AGUARDANDO_COMPLEMENTO");
    }

    private void receberComplemento(Long chatId, String complemento) {
        DadosPedido dados = pedidosUsuario.get(chatId);
        if (!complemento.equals(".")) {
            dados.complemento = complemento;
        }
        enviarMensagem(chatId, "💳 Forma de pagamento?\nDigite: PIX ou CARTAO");
        estadoUsuario.put(chatId, "AGUARDANDO_PAGAMENTO");
    }

    private void receberPagamento(Long chatId, String pagamento) {
        pagamento = pagamento.toUpperCase();
        if (pagamento.equals("PIX") || pagamento.equals("CARTÃO")) {
            DadosPedido dados = pedidosUsuario.get(chatId);
            dados.pagamento = pagamento;
            confirmarPedido(chatId, dados);
        } else {
            enviarMensagem(chatId, "❌ Digite apenas: PIX ou CARTÃO");
        }
    }

    private void confirmarPedido(Long chatId, DadosPedido dados) {
        StringBuilder confirmacao = new StringBuilder();
        confirmacao.append("✅ *PEDIDO CONFIRMADO!*\n\n");
        confirmacao.append("📦 *Pedido:* ").append(dados.pedido).append("\n\n");
        confirmacao.append("📍 *Endereço de entrega:*\n");

        if (dados.rua != null && !dados.rua.isEmpty()) {
            confirmacao.append(dados.rua).append(", ").append(dados.numero).append("\n");
        } else {
            confirmacao.append("Número: ").append(dados.numero).append("\n");
        }

        if (dados.complemento != null && !dados.complemento.isEmpty()) {
            confirmacao.append("Complemento: ").append(dados.complemento).append("\n");
        }

        if (dados.bairro != null && !dados.bairro.isEmpty()) {
            confirmacao.append("Bairro: ").append(dados.bairro).append("\n");
        }

        confirmacao.append(dados.cidade).append(" - ").append(dados.estado).append("\n");

        confirmacao.append("CEP: ").append(dados.cep).append("\n");

        confirmacao.append("\n💳 *Pagamento:* ").append(dados.pagamento);

        enviarMensagem(chatId, confirmacao.toString());
        perguntarContinuar(chatId);
    }

    private void enviarInformacoes(Long chatId) {
        String info = "ℹ️ *INFORMAÇÕES DA LOJA*\n\n" +
                "📍 *Endereço:*\n" +
                "Rua tanana, 288, Jd Tanana\n" +
                "Sao Paulo - SP, CEP: 00000-000\n\n" +
                "🕐 *Horário:*\n" +
                "Segunda à Sabado\n09h às 18h\n\n" +
                "💳 *Pagamentos:*\n" +
                "• Cartões (crédito/débito)\n" +
                "• Vale alimentação\n" +
                "• PIX\n" +
                "• Dinheiro";
        enviarMensagem(chatId, info);
    }

    private void perguntarContinuar(Long chatId) {
        enviarMensagem(chatId, "\n❓ Posso ajudar em algo mais?\nDigite: SIM ou NÃO");
        estadoUsuario.put(chatId, "AGUARDANDO_CONTINUAR");

        iniciarTimerInatividade(chatId);
    }

    private void receberContinuar(Long chatId, String resposta) {
        resposta = resposta.toUpperCase();
        if (resposta.equals("SIM") || resposta.equals("S")) {
            cancelarTimerInatividade(chatId);
            enviarMenuPrincipal(chatId);
            estadoUsuario.put(chatId, "MENU");
        } else {
            enviarMensagem(chatId, "Obrigado. Até logo! 👋");
            estadoUsuario.remove(chatId);
            pedidosUsuario.remove(chatId);
            encerrarConversa(chatId);
        }
    }

    private void enviarPDF(Long chatId, File arquivo, String legenda) {
        SendDocument document = new SendDocument();
        document.setChatId(chatId.toString());
        document.setDocument(new InputFile(arquivo));
        document.setCaption(legenda);

        try {
            execute(document);
            System.out.println("✅ PDF enviado: " + arquivo.getName() + " para chat: " + chatId);
        } catch (TelegramApiException e) {
            System.err.println("❌ Erro ao enviar PDF: " + e.getMessage());
            enviarMensagem(chatId, "❌ Erro ao enviar o documento. Tente novamente.");
        }
    }

    private void enviarMensagem(Long chatId, String texto) {
        SendMessage mensagem = new SendMessage();
        mensagem.setChatId(chatId.toString());
        mensagem.setText(texto);
        mensagem.enableMarkdown(true);

        try {
            execute(mensagem);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void iniciarTimerInatividade(Long chatId) {
        cancelarTimerInatividade(chatId);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                encerrarPorInatividade(chatId);
            }
        }, TIMEOUT_INATIVIDADE);

        timersInatividade.put(chatId, timer);
    }

    private void cancelarTimerInatividade(Long chatId) {
        Timer timer = timersInatividade.get(chatId);
        if (timer != null) {
            timer.cancel();
            timersInatividade.remove(chatId);
        }
    }

    private void encerrarPorInatividade(Long chatId) {
        String estado = estadoUsuario.get(chatId);
        if (estado != null && !estado.equals("MENU")) {
            enviarMensagem(chatId, "⏱️ *Conversa encerrada por inatividade*\n\n" +
                    "Você ficou inativo por mais de 5 minutos.\n" +
                    "Para continuar, envie /start a qualquer momento! 👋");
            limparDadosUsuario(chatId);
        }
    }

    private void encerrarConversa(Long chatId) {
        cancelarTimerInatividade(chatId);
        enviarMensagem(chatId,
                "Obrigado por usar o Quitutes da Granja.\n" +
                "Para iniciar novamente, envie /start");
        limparDadosUsuario(chatId);
    }

    private void limparDadosUsuario(Long chatId) {
        estadoUsuario.remove(chatId);
        pedidosUsuario.remove(chatId);
        cancelarTimerInatividade(chatId);
    }

    private static class DadosPedido {
        String pedido;
        String cep;
        String rua;
        String bairro;
        String cidade;
        String estado;
        String numero;
        String complemento;
        String pagamento;
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new Main());
            System.out.println("✅ Bot Telegram iniciado com sucesso!");
            System.out.println("🤖 Procure por @" + BOT_USERNAME + " no Telegram");
        } catch (TelegramApiException e) {
            System.err.println("❌ Erro ao iniciar bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}