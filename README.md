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

O bot está hospedado no [Fly.io](https://fly.io/), uma plataforma de deploy global que oferece um plano gratuito generoso e execução de containers Docker.

### Por que Fly.io?

- ✅ Plano gratuito com 3 VMs compartilhadas
- ✅ Deploy via Docker
- ✅ Gerenciamento seguro de secrets
- ✅ Logs e monitoramento em tempo real
- ✅ Regiões globais (incluindo São Paulo - GRU)
- ✅ CLI poderosa e fácil de usar
- ✅ Uptime 24/7

### 🔧 Como fazer o deploy no Fly.io

#### 1. Pré-requisitos

- Conta no [Fly.io](https://fly.io/) (gratuita)
- [Fly CLI](https://fly.io/docs/hands-on/install-flyctl/) instalada
- Token do bot do Telegram (via [@BotFather](https://t.me/botfather))

#### 2. Instalar o Fly CLI

```bash
# macOS/Linux
curl -L https://fly.io/install.sh | sh

# Windows (PowerShell)
iwr https://fly.io/install.ps1 -useb | iex
```

#### 3. Fazer login no Fly.io

```bash
fly auth login
```

#### 4. Lançar a aplicação

Na pasta do projeto, execute:

```bash
fly launch
```

O Fly.io irá:
- Detectar o `Dockerfile`
- Sugerir um nome para a app (ou você pode escolher)
- Selecionar a região (recomendado: `gru` - São Paulo)

Quando perguntar **"Would you like to deploy now?"**, responda **não** (vamos configurar os secrets primeiro).

#### 5. Configurar os secrets (variáveis de ambiente)

```bash
fly secrets set BOT_TOKEN="seu_token_aqui"
fly secrets set BOT_USERNAME="seu_username_aqui"
```

#### 6. Fazer o deploy

```bash
fly deploy
```

#### 7. Verificar o status

```bash
fly status
fly logs
```

### 📋 Comandos úteis do Fly.io

```bash
# Ver logs em tempo real
fly logs

# Ver status da aplicação
fly status

# Abrir dashboard da app
fly dashboard

# Atualizar após mudanças no código
fly deploy

# Reiniciar a aplicação
fly apps restart

# Ver secrets configurados (não mostra os valores)
fly secrets list
```

### 💰 Sobre o plano gratuito

O Fly.io oferece gratuitamente:
- **3 VMs compartilhadas** (shared-cpu-1x)
- **256MB de RAM** por VM
- **3GB de storage persistente**
- **160GB de tráfego outbound/mês**

Isso é mais do que suficiente para rodar um bot do Telegram 24/7!

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
├── Dockerfile                             # Configuração do Docker
├── .dockerignore                          # Arquivos ignorados no build
├── fly.toml                               # Configuração do Fly.io
├── LICENSE                                # Licença MIT
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
