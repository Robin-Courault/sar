# Designs

On considère N clients connaissant nom/ip d'un serveur S.

Hypothèses :

- Nom / IP serveur fixe
- Pas de faute OS, faute processus

## Design Stateless

> Rien n'est enregistré sur disque persistent sur le serveur.

### Le Client

Pour effectuer une tâche, une application sur l'un des clients peut demander la récupération d'un jeton unique auprès du serveur.
Pour se faire, l'application utilise une bibliothèque fournie, cette bibliothèque connait l'adresse du serveur S et possède les comportements suivants :

- Un détecteur de faute parfait (DFP) tourne en permanence et est capable de détecter lorsque le serveur meurt dès la première connection de la bibliothèque au serveur. Le DFP est également capable de détecter lorsque le serveur ressuscite.
  - Lorsque le serveur meurt, le DFP lève un événement. Cet événement note la mort du serveur et bloque toute communication avec le serveur
  - Lorsque le serveur ressuscite, le DFP lève un événement. Cet événement, si et seulement si le serveur était noté comme mort, se reconnecte puis envoie au serveur son état parmi : `idle`, `waiting` et `token`. Si l'état est `waiting`, envoie également sa position dans la file.
- Lorsqu'une demande de jeton survient depuis l'application, la bibliothèque bloque l'application puis envoie une requête au serveur qui peut lui répondre de deux façons :
  - Soit il répond avec le jeton, la bibliothèque mémorise alors qu'elle possède le jeton (état `token`) puis débloque l'application en attendant que l'application l'informe de la libération du jeton (fin de son traitement en section critique). La bibliothèque retourne alors le jeton au serveur puis repasse dans un état `idle` signifiant qu'elle ne possède plus le jeton.
  - Soit le serveur répond la position dans la file, la bibliothèque passe alors dans l'état `waiting` et sauvegarde sa position. (Le serveur actualise la position lorsque la file change.)
- La bibliothèque établit la connection (`connect`) avec le serveur la première fois qu'elle a besoin du jeton, si le serveur est mort, la connection attend qu'il ressuscite.

### Le Serveur

Le serveur écoute en attendant des connections, il possède 1 file ainsi qu'un jeton. Le serveur possède les comportements suivants :

- Un détecteur de faute parfait (DFP) tourne en permanence et contrôle les connections avec les clients. Le DFP détecte la mort d'un client et lève un événement fonctionnant comme suit :
  - Si le client était détenteur du jeton, le serveur recrée le jeton, défile la file de clients en attente et passe le jeton au client défilé. Le serveur prévient ensuite les éventuels clients encore en attente de leur changement de position dans la file.
  - Si le client était en attente dans la file, le serveur le supprime de la file puis prévient les clients de leur changement de position si nécessaire.
  - Si le client n'était ni détenteur du jeton, ni en attente, le serveur ne fait rien.
- À la connection d'un client, le serveur accepte la connection (`accept`) et lui associe un identifiant (peut être le numéro de port associé à la connection).
- Lorsqu'une demande de jeton survient depuis un client, le serveur regarde si le jeton est disponible (== si personne ne possède le jeton, variable du serveur) :
  - Si le jeton est disponible et que personne n'est en attente dans la file, alors le serveur envoie le jeton au client et retient que ce client possède le jeton.
  - Si le jeton n'est pas disponible, alors le serveur place le client à la fin de la file puis envoie sa position dans la file au client.
- Lors du démarrage (ou redémarrage) du serveur, ce dernier attend un certain temps (qu'il faudrait évaluer avec le temps moyen d'aller retour sur le réseau plus une marge d'erreur mais disons 5 minutes pour le moment). Pendant ce laps de temps, le serveur va recevoir des messages de tous les clients surveillant l'état du serveur. Plusieurs cas se posent :
  - Si le serveur reçoit une demande de jeton, il la bloque le temps que son initialisation soit terminée.
  - Si le serveur reçoit un message avec un état `waiting`, alors il reçoit également une position, il assigne donc le client correspondant au message à cette position dans la file. Si la position n'est pas précisé, le client est placé à la fin.
  - Si le serveur reçoit un message avec un état `idle`, il ne fait rien.
  - Si le serveur reçoit un message avec un état `token`, alors il retient que c'est le client correspondant qui possède le jeton.
  - Lorsque le serveur arrive au terme de son initialisation (l'attente de 5 minutes) :
    - Si aucun message `token` n'a été reçu, le serveur crée le jeton puis défile si possible, si c'est le cas, il envoie le jeton au client, et informe les autres de leur changement de position. Il se trouve alors dans son fonctionnement normal.
    - Si un message `token` a été reçu, le serveur attend de recevoir le jeton, il se trouve dans son fonctionnement normal.
  - Si le serveur reçoit un message avec un état `token` après son initialisation, alors il déconnecte le client et le considère mort afin de conserver un état cohérent. Dans ce cas la bibliothèque côté client réamorce une procédure de connection et prévient éventuellement l'application.
  - Si le serveur reçoit un message avec un état `waiting` après son initialisation, alors il place simplement le client dans la file et lui retourne sa position. Le client doit dont mettre à jour sa position dans la liste.

## Cas d'ordre dans la file non conservé

> Permet des famines, mais plus économe en messages et donc plus efficace.

- Les messages d'état `waiting` ne contiennent plus la position.
- Le serveur n'informe plus de leur position les clients, les mises à jour des clients au changement d'une file sont également supprimées.

## Design Statefull (discuté en cours)

### Panne Client

Le client a tout perdu, le serveur surveille les clients connectés, en cas de mort client :

- Le serveur qui le surveillait libère les mutex (jetons) possédés par le client et continue son travail normal (répond aux éventuels clients en attente).

### Panne Serveur

- Toutes les files (queues) sont enregistrées dans un SGBD.
- Tous les jetons (mutex) sont enregistrés dans un SGBD.
- Chaque détenteur de mutex est également retenu et enregistré dans un SGBD.
- Écriture et mise à jour avant envoie de messages, et avec des transactions sur le SGBD.
- Le serveur assigne un numéro unique à chaque client, le numéro du client lui est envoyé dans la réponse de connection.
- Lors de l'initialisation : 
  - On attend un temps choisi, pendant ce temps les clients se reconnectent en informant le serveur de leur identifiant et (si en attente) de leur position dans chaque file. Les nouvelles connections (celles ne contenant pas d'id) sont traitées à la fin de l'initialisation.
  - Le serveur envoie à tous les clients existants dans les sauvegardes leur état sauvegardé.