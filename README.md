# Jogo da Velha em Rede

Jogo da Velha para dois jogadores feito em Java com sockets TCP. O servidor é o árbitro da partida: recebe jogadas, valida o tabuleiro, controla os turnos e informa o resultado aos clientes.

## Estrutura

```text
.
├── Dockerfile
├── README.md
├── instructions.md
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

## Requisitos

- Java JDK 8 ou superior
- Terminal Linux, WSL2 ou equivalente
- Docker opcional, apenas para rodar o servidor em container

## Compilar

Na raiz do projeto:

```bash
javac -d out src/common/Protocol.java src/server/Board.java src/server/GameSession.java src/server/Server.java src/client/Client.java
```

## Rodar

Abra três terminais na raiz do projeto.

### 1. Servidor

```bash
java -cp out server.Server
```

A porta padrão é `5000`. Para usar outra porta:

```bash
java -cp out server.Server 6000
```

### 2. Jogador 1

```bash
java -cp out client.Client localhost 5000
```

### 3. Jogador 2

```bash
java -cp out client.Client localhost 5000
```

O primeiro cliente conectado joga com `X`. O segundo joga com `O`.

## Rodar o servidor com Docker

```bash
docker build -t jogo-da-velha-rede .
docker run --rm -p 5000:5000 jogo-da-velha-rede
```

Depois, rode os dois clientes normalmente no host:

```bash
java -cp out client.Client localhost 5000
```

## Como jogar

Use as posições de `0` a `8`:

```text
 0 | 1 | 2
---+---+---
 3 | 4 | 5
---+---+---
 6 | 7 | 8
```

Quando for sua vez, digite a posição desejada e pressione Enter.

```text
Sua vez. Escolha uma posicao (0-8): 4
```

Para sair:

```text
sair
```

ou:

```text
quit
```

## Funcionamento

A arquitetura é cliente-servidor centralizada.

- `Server` abre uma porta TCP e aguarda conexões.
- A cada dois clientes conectados, uma `GameSession` é criada.
- Cada jogador é atendido por uma thread própria no servidor.
- O cliente envia apenas comandos simples, como `MOVE:4`.
- O servidor valida a jogada, atualiza o `Board` e envia o novo estado para ambos.
- O turno alterna entre `X` e `O`.
- O servidor detecta vitória, empate e desconexão.

## Protocolo

Mensagens de texto puro, uma por linha.

Cliente para servidor:

```text
MOVE:4
QUIT
```

Servidor para cliente:

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

No `BOARD`, `_` representa casa vazia.

```text
BOARD:XO_X_O__X
```

Representa:

```text
 X | O | 2
---+---+---
 X | 4 | O
---+---+---
 6 | 7 | X
```
