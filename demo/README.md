# Guião de Demonstração


## 1. Preparação do sistema

Para testar o sistema e todos os seus componentes, é necessário preparar um ambiente com dados para proceder à verificação dos testes.

### 1.1. Lançar o *registry*

Para lançar o *ZooKeeper*, ir à pasta `zookeeper/bin` e correr o comando  
`./zkServer.sh start` (Linux) ou `zkServer.cmd` (Windows).

É possível também lançar a consola de interação com o *ZooKeeper*, novamente na pasta `zookeeper/bin` e correr `./zkCli.sh` (Linux) ou `zkCli.cmd` (Windows).

### 1.2. Compilar o projeto

Primeiramente, é necessário compilar e instalar todos os módulos e suas dependências --  *rec*, *hub*, *app*, etc.
Para isso, basta ir à pasta *root* do projeto e correr o seguinte comando:

```sh
$ mvn clean install -DskipTests
```

### 1.3. Lançar e testar o *rec*

Para proceder aos testes, é preciso em primeiro lugar lançar os vários servidores *rec* .
Para isso basta ir à pasta *rec* e executar:

```sh
$ mvn compile exec:java -Drec.i="n"
```

Este comando vai colocar o *rec* no endereço *localhost* e no porto *809n* onde o argumento n é o número da instância.

Para confirmar o funcionamento do servidor com um *ping*, fazer:

```sh
$ cd rec-tester
$ mvn compile exec:java
```

Para executar toda a bateria de testes de integração, fazer:

```sh
$ mvn integration-test
```

Todos os testes devem ser executados sem erros.


### 1.4. Lançar e testar o *hub*

Para proceder aos testes, é preciso em primeiro lugar lançar o servidor *rec* (ver 1.3).

Em segundo lugar lançar o servidor *hub* .
Para isso basta ir à pasta *hub* e executar:

```sh
$ mvn compile exec:java
```

Este comando vai colocar o *hub* no endereço *localhost* e na porto *8081*.

**Nota:** ficheiros stations.csv e users.csv têm de estar na diretoria *hub*.

Para confirmar o funcionamento do servidor com um *ping*, fazer:

```sh
$ cd hub-tester
$ mvn compile exec:java
```

Para executar toda a bateria de testes de integração, fazer:

```sh
$ mvn integration-test
```

Todos os testes devem ser executados sem erros.

### 1.5. *App*

Iniciar a aplicação com a utilizadora alice:

```sh
$ app localhost 2181 alice +35191102030 38.7380 -9.3000
```

**Nota:** Para poder correr o script *app* diretamente é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na pasta `target/appassembler/bin/`.

Abrir outra consola, e iniciar a aplicação com o utilizador bruno.

Depois de lançar todos os componentes, tal como descrito acima, já temos o que é necessário para usar o sistema através dos comandos.

## 2. Teste dos comandos

Nesta secção vamos correr os comandos necessários para testar todas as operações do sistema.
Cada subsecção é respetiva a cada operação presente no *hub*.

### 2.1. *balance*
Consultar o saldo de *bicloins*:

    > balance
    alice 0 BIC

Consultar o saldo de um utilizador desconhecido:

    > balance
    ERRO utilizador não encontrado

### 2.2 *top-up*

Carregar 15 Euros em *bicloins*:

    > top-up 15
    alice 150 BIC

Carregar 15 Euros com um utilizador desconhecido:

    > top-up 15
    ERRO utilizador não encontrado

Carregar 15 Euros com o numero de telemóvel errado:

    > top-up 15
    ERRO número de telemóvel incorreto

Carregar 30 Euros em *bicloins*:

    > top-up 30
    ERRO quantia fora do intervalo 1-20

### 2.3 *info_station*

Listagem da informação de uma estação:
    
    > info istt
    IST Taguspark, lat 38,737202, long -9,302300, 20 docas, 4 BIC prémio, 12 bicicletas, 0 levantamentos, 0 devoluções, https://www.google.com/maps/place/38,737202,-9,302300


Listagem da informação de uma estação desconhecida:
    
    > info sdis
    ERRO estação desconhecida

### 2.4 *locate_station*

Localizar 3 estações:
    
    > scan 3
    istt, lat 38,737202, long -9,302300, 20 docas, 4 BIC prémio, 12 bicicletas, a 218 metros
    stao, lat 38,686699, long -9,312400, 30 docas, 3 BIC prémio, 20 bicicletas, a 5804 metros
    jero, lat 38,697201, long -9,206400, 30 docas, 3 BIC prémio, 20 bicicletas, a 9301 metros

Localizar um número inválido de estações:

    > scan -1
    ERRO número de estações inválido

### 2.5 *bike_up*

Levantar uma bicicleta:

    > bike-up ocea
    OK

Levantar uma bicicleta com um utilizador desconhecido:

    > bike-up ocea
    ERRO utilizador não encontrado

Levantar uma bicicleta numa estação desconhecida:

    > bike-up sdis
    ERRO estação desconhecida

Levantar uma bicicleta sem dinheiro:

    > bike-up ocea
    ERRO sem dinheiro

Levantar uma bicicleta a mais de 200 metros:

    > bike-up ocea
    ERRO fora de alcance

Levantar uma bicicleta sem bicicletas na estação:

    > bike-up cate
    ERRO estação sem biclicletas

Levantar uma bicicleta já com uma bicicleta:

    > bike-up ocea
    ERRO já tem bicicleta

Levantar uma bicicleta com uma estação inválida:

    > bike-up mystation
    ERRO estação desconhecida

### 2.6 *bike_down*

Entregar uma bicicleta:

    > bike-down cate
    OK

Entregar uma bicicleta com um utilizador desconhecido:

    > bike-down cate
    ERRO utilizador não encontrado

Entregar uma bicicleta numa estação desconhecida:

    > bike-down sdis
    ERRO estação desconhecida

Entregar uma bicicleta a mais de 200 metros:
    
    > bike-down ocea
    ERRO fora de alcance

Entregar uma bicicleta sem bicicleta:
    
    > bike-down ocea
    ERRO não tem bicicleta

Entregar uma bicicleta numa estação cheia:
    
    > bike-down gulb
    ERRO estação sem capacidade

Entregar uma bicicleta com uma estação inválida:
    
    > bike-down ocea
    ERRO estação desconhecida
 
### 2.7 *ping*

Ping do hub:

    > ping
    Recebido: hello

### 2.8 *sys_status*

Ver servidores ativos:

    > sys_status 
    Server /grpc/bicloin/hub/1 contactado com estado: UP
    Server /grpc/bicloin/rec/1 contactado com estado: UP


----

## 3. Considerações Finais

Estes testes não cobrem tudo, pelo que devem ter sempre em conta os testes de integração e o código.
