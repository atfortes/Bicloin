# Relatório do projeto *Bicloin*

Sistemas Distribuídos 2020-2021, segundo semestre

## Autores


**Grupo A43**


| Número | Nome              | Utilizador                       | Correio eletrónico                  |
| -------|-------------------|----------------------------------| ------------------------------------|
| 92428  | Armando Fortes | <https://git.rnl.tecnico.ulisboa.pt/ist192428>   | <armando.teles.fortes@tecnico.ulisboa.pt>   |
| 92455  | Diogo Soares       | <https://git.rnl.tecnico.ulisboa.pt/ist192455>     | <diogo.sousa.soares@tecnico.ulisboa.pt>     |
| 92478  | Hugo Pitorro     | <https://git.rnl.tecnico.ulisboa.pt/ist192478> | <hugo.pitorro@tecnico.ulisboa.pt> |

<img src=Armando_Fortes.jpg height="150"> <img src=Diogo_Soares.jpeg height="150"> <img src=Hugo_Pitorro.jpg height="150">


## Melhorias da primeira parte

- [Corrected Hub-Tester function names](https://git.rnl.tecnico.ulisboa.pt/SD-20-21-2/A43-Bicloin/commit/dc86d06c55784d4df5d64f170c75b6f7594643d9)

- [Added Hub-Test for code coverage](https://git.rnl.tecnico.ulisboa.pt/SD-20-21-2/A43-Bicloin/commit/dc86d06c55784d4df5d64f170c75b6f7594643d9)
 

- [Corrected Rec Synchronization](https://git.rnl.tecnico.ulisboa.pt/SD-20-21-2/A43-Bicloin/commit/dc86d06c55784d4df5d64f170c75b6f7594643d9)

- [Added User and Station locks](https://git.rnl.tecnico.ulisboa.pt/SD-20-21-2/A43-Bicloin/commit/dc86d06c55784d4df5d64f170c75b6f7594643d9)


## Modelo de faltas

O modelo de faltas permite a existência de faltas silenciosas e transientes, ou seja cada réplica pode simplesmente parar. No entanto este modelo não enquadra a existência de faltas bizantinas, visto que o quórum (definido semanticamente como mais de metade) não prevê a existência de falsos positivos.    

Numericamente o modelo de faltas para um sistema com pesos iguais permite para um sistema com 2f réplicas aceitar até f - 1 faltas, isto porque são necessárias f + 1 respostas para obter quórum. Caso o sistema seja obtido por réplicas com pesos diferenciados o número de faltas não pode ser calculado de forma tão linear, para este cenário podemos definir o número aceitável de faltas o min(#F), com F definido como o conjunto de todos os subconjuntos de réplicas que falham, onde sum(C) é quórum e deixa de o ser quando removido qualquer um dos seus elementos, F é composto pelos conjuntos complementares aos conjuntos de C.

Nota: A aceitação de falhas bizantinas teria uma grande perda de performance associada, uma vez que para aceitar t faltas bizantinas precisariamos de extender a cardinalidade do quórum em t réplicas, isto porque para garantirmos que f + 1 respostas estão certas, temos de aceitar a possibilidade de t destas respostas estarem corretas devido a processos arbitrários, ou seja o equivalente a um falso positivo, como tal apenas t + f + 1 garante que temos f + 1 respostas certas, na presença de t faltas.

A definição de resposta certa acima mencionada também teria de ser extremamente mais restrita que a mesma no protocolo sem a presença de faltas bizantinas, visto que no cenário normal apenas é necessário que as mensagens contenham os campos expectáveis, enquanto que neste cenário temos de garantir um consenso nas variáveis retornadas (o conceito de resposta parcialmente correta também é interessante, em casos em que as faltas são identificáveis, por exemplo sistemas de comunicação com estados quânticos).  

<br />


## Solução

<img src=Figura.jpeg>

Para cada comando é criado um sender, que começa por enviar o comando para todos os recs, e para cada um deste cria um observer responsável por apanhar o conteúdo devolvido pelo rec. 

Aquando receção deste conteúdo guarda esta informação no response collector. 

Após recebimento de um número suficiente de recs, o response collector será responsável por comunicar as respostas à função que processa o comando no frontend. 

Desta forma devolvendo ao cliente o resultado do comando.


## Protocolo de replicação

Neste projeto foi utilizado um protocolo de coêrencia forte, dito isto e considerando o  Teorema CAP, este protocolo peca pela perda de Availability, por outro lado beneficia da existência de partições, bem como um regime de coêrencia forte.

O protocolo em questão é composto por *N* réplicas, cada uma com peso *p*i. Para cada comando o cliente envia *N* mensagens assincronamente e após receber  *t* mensagens com peso total associado a mais de sum(*p*i)/2  (quórum), 
conclui a execução deste comando.  


Os clientes enviam 3 comandos distintos:

`ctrl_ping` -> apenas envia uma mensagem e recebe a mesma mensagem sob o formato de string.

`write` -> envia não só o identificador e o valor que deseja guardar, como também o número de versão deste valor, bem como o identificador de cliente. Recebe apenas uma confirmação.

`read` -> envia o identificador e recebe o valor que se encontra guardado, bem como o número de versão deste valor e o número de cliente. 

Nota: O `read` e o `ctrl_ping` podem ser comandos executados normalmente, no entanto o `write` pressupõe sempre que o `read` também seja efetuado, embora não seja vísivel para o utilizador, tal acontece para assegurar o uso correto do número de versão. 

<br />


Procedimentos no Cliente:
Comando | Cliente  
--- | --- 
`Ctrl_Ping` | Define Mensagem; Envia Pedido para todos os Servidores; Recebe Respostas Corretas até ter quórum; Escolhe a primeira 
`Read` | Define Identificador; Envia Pedido para todos os Servidores; Recebe Respostas Corretas até ter quórum; Escolhe a resposta com maior tag 
`Write` | Define Identificador; Faz read; Define tag como tag do read +1; Recebe Respostas Corretas até ter quórum; Escolhe a primeira

<br />


Procedimentos no Servidor:

Comando | Servidor  
--- | --- 
`Ctrl_Ping` | Devolve Mensagem 
`Read` | Acede ao dado em questão; Devolve Conteúdo do Identificador e Tag 
`Write` | Verifica se a tag do request é maior que a tag atual; Altera o dado em questão; Devolve mensagem

<br />


## Medições de desempenho
<br />

### Estado Inicial:

Medição | Cliente (Hub)
--- | --- 
ReadTime | 134 ms
WriteTime | 91 ms
Reads | 427
Writes | 652
ReadWriteRatio | 0.6549

<br />

### Estado Final (após otimizações):
Medição | Cliente (Hub)
--- | --- 
ReadTime | 26 ms
WriteTime | 39 ms
Reads | 239
Writes | 233
ReadWriteRatio | ~1

<br />


O tempo de read e write foi considerado como o tempo desde que o cliente faz a chamada ao método do comando no frontend, até ao momento em que a resposta é devolvida ao cliente.  

As condições de teste escolhidas para as medições, consistem na execução de 3 conjuntos de instruções realizadas em paralelo por 3 clientes na app. 

Os ficheiros de teste utilizados encontram-se na pasta da app.

As intuições associadas à variação de tempo encontram-se descritas na secção imediatamente posterior.

## Opções de implementação

Na implementação do protocolo um dos nossos principais focos foi a escalibilidade e legibilidade dos mecanismos que estavam a ser codificados. 

Como tal, decidimos utilizar uma relação wait notify entre a thread que aguardava pelo quórum de respostas e os observadores subsequentes, esta opção permite conceptualizar o envio de múltiplas mensagem como um problema isolado, desde que a informação esteja no collector aquando o notify é executado.

Outro dos nossos focos foi escalar com facilidade a criação de novos comandos, para tal foi necessário facilitar o mecanismo de envio de mensagens para todos os recs, embora aparentemente simples optamos por usar funções lambda como parâmetros para representar a ação de envio a ser executada em cada stub. Devido à natureza modular da solução que apresentámos é necessário apenas explicitar qual o comando e o conteúdo do request a ser enviado, consequentemente aumentando substancialmente a escalabilidade.

No processo de análise e criação de melhorias à solução original, inicialmente decidimos elencar possíveis bottlenecks associados, rapidamente identificamos o write-back para todas réplicas mesmo aquelas atualizadas como uma possível ineficiência, assim como o pedido recorrente dos mesmos valores ao servidor quando estes podem estar armazenados numa cache local, por fim também observamos que o processo de identificar mudanças de ip (uma função responsável por isto estava a ser chamada para todos os comandos), era desnecessário quando consideramos a probabilidade do mesmo acontecer.

Após longa consideração decidimos resolver os problemas mencionados das seguintes formas:

- Implementar uma cache local, onde cada valor desejado só será exigido ao rec, caso não esteja na cache (garantimos que o valor não está alterado por se tratar de um só hub)
- Utilizar Write-Back apenas quando a tag do request associado a um rec é inferior à tag máxima do read efetuado, desta forma garantimos que só enviamos Write-Back para recs desatualizados
- Apenas verificar se existe mudança de ip de 10 em 10 chamadas, considerámos o número 10 como um número interessante para o tradeoff entre garantir que os ips se encontram certos e a superior velocidade do sistema.

Como conjeturado teoricamente, verificamos que o tempo médio de cada leitura e escrita é entre 3 a 4 vezes inferior no estado final quando comparado com o estado inicial. 

Outra observação interessante provém do também menor número de reads e writes devido à existência de uma cache associada.

## Notas finais

Uma possível simplificação no que toca à interação entre a aplicação e o hub seria a implementação de uma cache, de forma a armazenar distâncias previamente conhecidas entre o utilizador e várias estações evitando uma remote procedure call adicional no comando Hub-Info.

Outra das ponderações efetuadas no desenvolvimento do projeto foi a utilização de pesos diferenciados com alterações dinâmicas mediante a performance dos mesmos, devido a restrições inerentes ao desenvolvimento do projeto tal acabou por não ser feito, no entanto planificamos o seguinte roadmap:

- Modelar a performance do sistema com uma função F(x1,x2, ... xn, p1, p2, ... pn), onde xi corresponde à latência média (para uma certa epoch) e pi corresponde ao peso (um exemplo que respeita a monotonia que desejariamos seria sum(-xi*pi))

- Calcular as derivadas parciais em função dos pesos de forma a minimizar o custo da função acima definida

- Somar a cada peso a derivada parcial a multiplicar pela learning rate (valor definido manualmente) 

- Aplicar o protocolo normalmente, mas com os pesos atualizados

Mesmo no que toca a pesos diferenciados por rec, consideramos que não seria uma otimização interessante dadas as condições como os testes estavam a ser executados (recs definidos internamente na mesma máquina , visto que implica o  mesmo tempo de processamento e a mesma latência)