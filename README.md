# 🌾 ChatBot Quitutes da Granja

Bot para Telegram desenvolvido em Java para automatizar o atendimento da loja **Quitutes da Granja**, permitindo que clientes consultem catálogos, ofertas, façam pedidos e obtenham informações da loja de forma interativa.

## 📋 Descrição

O ChatBot Quitutes da Granja é um assistente virtual que funciona no Telegram, proporcionando uma experiência completa de atendimento ao cliente. O bot gerencia todo o fluxo de pedidos, desde a escolha dos produtos até a confirmação do endereço de entrega e forma de pagamento.

## ✨ Funcionalidades

- **📄 Ver Catálogo**: Envio do catálogo completo de produtos em formato PDF
- **🎉 Ofertas do Dia**: Consulta de promoções e ofertas especiais
- **📦 Fazer Pedido**: Sistema completo de pedidos com:
  - Registro do pedido
  - Validação automática de CEP via [Brasil API](https://brasilapi.com.br/)
  - Coleta de endereço completo (rua, número, complemento, bairro, cidade, estado)
  - Seleção de forma de pagamento (PIX ou Cartão)
  - Confirmação do pedido com todos os detalhes
- **ℹ️ Informações da Loja**: Endereço, horário de funcionamento e formas de pagamento
- **⏱️ Timeout de Inatividade**: Encerramento automático após 5 minutos de inatividade
- **🔄 Gerenciamento de Conversas**: Sistema de estados para controlar o fluxo da conversa

## 🛠️ Tecnologias Utilizadas

- **Java 11**: Linguagem de programação
- **Maven**: Gerenciador de dependências e build
- **Telegram Bots API (v6.9.7.1)**: Integração com o Telegram
- **Dotenv Java (v3.0.0)**: Gerenciamento de variáveis de ambiente
- **Brasil API**: Validação e busca de endereços por CEP

## 🚀 Como Usar

O bot está disponível publicamente no Telegram! Não é necessário instalar nada na sua máquina.

### Acesse o bot

1. Abra o Telegram
2. Procure por **@QuitutesGranjaBot** (ou o username configurado)
3. Clique em "Iniciar" ou digite `/start`
4. Pronto! O bot está pronto para atender você

## ☁️ Deploy

O bot está hospedado no [Railway](https://railway.com/), uma plataforma de deploy que simplifica a infraestrutura e permite escalabilidade automática.

### Por que Railway?

- ✅ Deploy automático a partir do GitHub
- ✅ Escalabilidade automática
- ✅ Gerenciamento simples de variáveis de ambiente
- ✅ Logs e monitoramento em tempo real
- ✅ Suporte nativo para Java/Maven
- ✅ Uptime 24/7

### Configuração no Railway

O bot utiliza as seguintes variáveis de ambiente:

- `BOT_TOKEN`: Token fornecido pelo [@BotFather](https://t.me/botfather)
- `BOT_USERNAME`: Username do bot no Telegram

O Railway detecta automaticamente o `pom.xml` e compila o projeto usando Maven, executando a classe principal automaticamente.

### 🔧 Faça seu próprio deploy (Opcional)

Se você quiser fazer seu próprio deploy do bot:

1. Faça um fork deste repositório
2. Crie uma conta no [Railway](https://railway.com/)
3. Crie um novo projeto no Railway
4. Conecte seu repositório do GitHub
5. Configure as variáveis de ambiente `BOT_TOKEN` e `BOT_USERNAME`
6. O Railway fará o deploy automaticamente!

O Railway oferece um plano gratuito generoso para começar, perfeito para projetos pequenos e médios.

## 📱 Como Usar o Bot

1. **Inicie a conversa**: Procure pelo bot no Telegram usando o username configurado
2. **Comando inicial**: Digite `/start` para iniciar
3. **Menu principal**: Escolha uma das opções digitando o número correspondente:
   - `1` - Ver catálogo
   - `2` - Ofertas do dia
   - `3` - Fazer pedido
   - `4` - Informações da loja

### Fluxo de Pedido

1. Digite `3` no menu principal
2. Digite o seu pedido
3. Informe o CEP (8 dígitos)
4. Confirme o endereço encontrado
5. Digite o número da residência
6. Digite o complemento (ou `.` se não houver)
7. Escolha a forma de pagamento: `PIX` ou `CARTAO`
8. Receba a confirmação do pedido

## 📁 Estrutura do Projeto

```
ChatBotQuitutesGranja/
├── src/
│   └── main/
│       └── java/
│           └── org/
│               └── example/
│                   └── Main.java          # Classe principal do bot
├── menu-quitutes.pdf                      # Catálogo da loja
├── pom.xml                                # Configuração do Maven
└── README.md                              # Este arquivo
```

## 🐛 Solução de Problemas

### O bot não responde

- Verifique se você iniciou a conversa com `/start`
- Certifique-se de estar usando o username correto do bot
- O bot pode estar temporariamente em manutenção

### CEP não é validado

- Verifique se digitou corretamente os 8 números do CEP
- A Brasil API pode estar temporariamente indisponível
- Tente novamente em alguns segundos

### PDF não é enviado

- O arquivo pode estar sendo processado
- Tente novamente usando o menu principal

## 🤝 Contribuindo

Contribuições são sempre bem-vindas! Sinta-se à vontade para:

1. Fazer um fork do projeto
2. Criar uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abrir um Pull Request

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

## 👥 Autor

Desenvolvido com ❤️ para a Quitutes da Granja

---

**Nota**: O bot está atualmente em produção no Railway. Para melhorias futuras, considere adicionar:
- Banco de dados para persistência de pedidos
- Sistema de autenticação e autorização para administradores
- Painel administrativo para gerenciar pedidos
- Testes automatizados
- Webhook ao invés de long polling para melhor performance
- Integração com sistema de pagamento

