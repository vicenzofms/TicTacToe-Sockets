# TicTacToe-Sockets

> Jogo da Velha para dois jogadores via rede, feito em Java com sockets TCP.  
> Desenvolvido como trabalho prático da matéria de **Redes de Computadores**.

![Java](https://img.shields.io/badge/Java-8+-007ec6?style=for-the-badge&logo=openjdk&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-opcional-007ec6?style=for-the-badge&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/Licença-MIT-007ec6?style=for-the-badge&logo=opensourceinitiative&logoColor=white)

---

## Sobre o Projeto

Aplicação cliente-servidor onde dois jogadores se conectam a um servidor central e jogam Jogo da Velha em tempo real. O servidor é o árbitro da partida: recebe jogadas, valida o tabuleiro, controla os turnos e informa o resultado aos clientes via protocolo de texto puro sobre TCP.

---

## Tecnologias

- **Java** (JDK 8+) — linguagem principal, sockets TCP nativos
- **Docker** — containerização do servidor (opcional)

---

## Estrutura

```text
.
├── Dockerfile
├── README.md
└── src
    ├── client
    │   └── Client.java
    ├── common
    │   └── Protocol.java
    └── server
        ├── Board.java
        ├── GameSession.java
        └── Server.java
```

---

## Pré-requisitos

- Java JDK 8 ou superior
- Docker (opcional, apenas para rodar o servidor em container)

---

## Compilar

Na raiz do projeto:

```bash
javac -d out src/common/Protocol.java src/server/Board.java src/server/GameSession.java src/server/Server.java src/client/Client.java
```

---

## Executar

Abra três terminais na raiz do projeto.

**Terminal 1 — Servidor:**

```bash
java -cp out server.Server
```

A porta padrão é `5000`. Para usar outra:

```bash
java -cp out server.Server 6000
```

**Terminal 2 — Jogador 1:**

```bash
java -cp out client.Client localhost 5000
```

**Terminal 3 — Jogador 2:**

```bash
java -cp out client.Client localhost 5000
```

O primeiro cliente conectado joga com `X`, o segundo com `O`.

---

## Docker

Para rodar o servidor em container:

```bash
docker build -t tictactoe-server .
docker run --rm -p 5000:5000 tictactoe-server
```

Os clientes rodam normalmente no host:

```bash
java -cp out client.Client localhost 5000
```

---

## Como Jogar

Use as posições de `0` a `8`:

```text
 0 | 1 | 2
---+---+---
 3 | 4 | 5
---+---+---
 6 | 7 | 8
```

Quando for sua vez, digite a posição e pressione Enter:

```text
Sua vez. Escolha uma posicao (0-8): 4
```

Para sair, digite `sair` ou `quit`.

---

## Arquitetura

Arquitetura cliente-servidor centralizada com threads por jogador.

- `Server` abre uma porta TCP e aguarda conexões.
- A cada dois clientes conectados, uma `GameSession` é criada em uma thread dedicada.
- Cada jogador é atendido por uma thread própria no servidor.
- O cliente envia comandos simples (`MOVE:4`); o servidor valida, atualiza o `Board` e transmite o novo estado para ambos.
- O servidor detecta vitória, empate e desconexão.

---

## Protocolo

Mensagens de texto puro, uma por linha.

**Cliente → Servidor:**

```text
MOVE:4
QUIT
```

**Servidor → Cliente:**

```text
ASSIGN:X
BOARD:____X____
STATUS:YOUR_TURN
STATUS:WAIT
STATUS:WIN
STATUS:LOSE
STATUS:DRAW
STATUS:OPPONENT_LEFT
MESSAGE:Partida iniciada. Jogador X começa.
ERROR:Posição invalida ou ocupada.
```

No campo `BOARD`, `_` representa casa vazia:

```text
BOARD:XO_X_O__X
```

Equivale a:

```text
 X | O | 2
---+---+---
 X | 4 | O
---+---+---
 6 | 7 | X
```

---

## Contribuintes

| Nome | GitHub |
|------|--------|
| Henrique Carvalho | [@henriquegdc](https://github.com/henriquegdc) |
| Renato Douglas | [@RenatoDNS](https://github.com/RenatoDNS) |
| Vicenzo Fonseca | [@vicenzofms](https://github.com/vicenzofms) |

---

## Licença

Distribuído sob a licença MIT.
