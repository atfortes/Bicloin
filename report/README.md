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

Medição | Cliente 1 | Cliente 2 | Cliente 3 | Média
--- | --- | --- | --- | ---
ReadTime |
WriteTime |
ReadFrequency |
WriteFrequency |
ReadWriteRatio |

<br />

### Estado Final (após otimizações):
Medição | Cliente 1 | Cliente 2 | Cliente 3 | Média
--- | --- | --- | --- | ---
ReadTime |
WriteTime |
ReadFrequency |
WriteFrequency |
ReadWriteRatio |

<br />


O tempo de read e write foi considerado como o tempo desde que o cliente faz a chamada ao método do comando no frontend, até ao momento em que a resposta é devolvida ao cliente.  

As condições de teste escolhidas para as medições, consistem na execução de 3 conjuntos de instruções realizadas em paralelo por 3 clientes na app. 

Os ficheiros de teste utilizados encontram-se na pasta  .

As intuições associadas à variação de tempo encontram-se descritas na secção imediatamente posterior.

## Opções de implementação

_(Descrição de opções de implementação, incluindo otimizações e melhorias introduzidas)_

_(Justificar as otimizações com as medições efetuadas -- antes e depois)_

## Notas finais

_(Algo mais a dizer?)_
