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

O Modelo de faltas permite a existência de faltas silenciosas, ou seja cada réplica pode simplesmente parar. Este modelo não enquadra a existência de faltas bizantinas, visto que o quórum (definido semanticamente como mais de metade) não prevê a existência de falsos positivos.    


## Solução

<img src=Figura.jpeg>

Para cada comando é criado um sender, que começa por enviar o comando para todos os recs, e para cada um deste cria um observer responsável por apanhar o conteúdo devolvido pelo rec. Aquando receção deste conteúdo guarda esta informação no response collector. Após recebimento de um número suficiente de recs, o response collector será responsável por comunicar as respostas à função que processa o comando no frontend. Desta forma devolvendo ao cliente o resultado do comando.


## Protocolo de replicação

Neste projeto foi utilizado um protocolo de coêrencia forte, dito isto e considerando o  Teorema CAP, este protocolo peca pela perda de Availability, por outro lado beneficia da existência de partições, bem como um regime de coêrencia forte.

O protocolo em questão é composto por *N* réplicas, cada uma com peso <img src="https://latex.codecogs.com/png.image?\dpi{150}&space;\bg_white&space;p_{i}" title="\bg_white p_{i}" />. Para cada comando o cliente envia *N* mensagens assincronamente e após receber  *t* mensagens com peso total associado a <img src="https://latex.codecogs.com/png.image?\dpi{150}&space;\bg_white&space;>\tfrac{\sum_{}^{}&space;p_{i}}{2}" title="\bg_white >\tfrac{\sum_{}^{} p_{i}}{2}" /> , 
conclui a execução deste comando.  


Os clientes enviam 3 comandos distintos:

`ctrl_ping` -> apenas envia uma mensagem e recebe a mesma mensagem sob o formato de string.

`write` -> envia não só o identificador e o valor que deseja guardar, como também o número de versão deste valor, bem como o identificador de cliente. Recebe apenas uma confirmação.

`read` -> envia o identificador e recebe o valor que se encontra guardado, bem como o número de versão deste valor e o número de cliente. 

Nota: O `read` e o `ctrl_ping` podem ser comandos executados normalmente, no entanto o `write` pressupõe sempre que o read também seja efetuado, embora não seja vísivel para o utilizador, tal acontece para assegurar o uso correto do número de versão. 

## Medições de desempenho

_(Tabela-resumo)_

_(explicação)_

## Opções de implementação

_(Descrição de opções de implementação, incluindo otimizações e melhorias introduzidas)_

_(Justificar as otimizações com as medições efetuadas -- antes e depois)_

## Notas finais

_(Algo mais a dizer?)_
